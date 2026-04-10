package com.npucraft.randomteleport.teleport;

import com.npucraft.randomteleport.RandomTeleportPlugin;
import com.npucraft.randomteleport.config.PluginSettings;
import com.npucraft.randomteleport.config.SafetySettings;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class SafeLocationFinder {

    private static final Set<Material> BASE_UNSAFE = EnumSet.of(
            Material.LAVA,
            Material.FIRE,
            Material.MAGMA_BLOCK,
            Material.CACTUS,
            Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.SOUL_FIRE,
            Material.END_PORTAL,
            Material.NETHER_PORTAL,
            Material.POWDER_SNOW
    );

    private final RandomTeleportPlugin plugin;
    private final PluginSettings settings;
    private final Set<Material> unsafe;

    public SafeLocationFinder(RandomTeleportPlugin plugin, PluginSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.unsafe = buildUnsafeSet(settings.safety());
    }

    private static Set<Material> buildUnsafeSet(SafetySettings s) {
        EnumSet<Material> set = EnumSet.copyOf(BASE_UNSAFE);
        if (s.avoidWater()) {
            set.add(Material.WATER);
            set.add(Material.BUBBLE_COLUMN);
            set.add(Material.KELP);
            set.add(Material.KELP_PLANT);
            set.add(Material.SEAGRASS);
            set.add(Material.TALL_SEAGRASS);
        }
        if (s.avoidWebs()) {
            set.add(Material.COBWEB);
        }
        if (s.avoidCaveVines()) {
            set.add(Material.CAVE_VINES);
            set.add(Material.CAVE_VINES_PLANT);
        }
        if (s.avoidDripstone()) {
            set.add(Material.POINTED_DRIPSTONE);
        }
        return set;
    }

    /**
     * @param forPlayer player who will teleport (excluded from “too close” checks); must not be null for isolation rules
     * @return safe teleport location with yaw/pitch preserved from {@code base}, or null
     */
    public Location findSafe(World world, PluginSettings.AxisRange range, Location base, Player forPlayer) {
        if (range == null || world == null || base == null) {
            return null;
        }
        int attempts = settings.maxLocationAttempts();
        ChunkLoadBudget budget = ChunkLoadBudget.fromCap(settings.maxSyncChunkLoadsPerRtp());
        for (int i = 0; i < attempts; i++) {
            int x = randomInRange(range.minX(), range.maxX());
            int z = randomInRange(range.minZ(), range.maxZ());
            Location candidate = switch (world.getEnvironment()) {
                case NETHER -> findInNether(world, x, z, base, budget);
                default -> findSurface(world, x, z, base, budget);
            };
            if (candidate == null || !isColumnSafe(world, candidate)) {
                continue;
            }
            if (tooCloseToAnotherPlayer(world, candidate, forPlayer)) {
                plugin.debugLog("RTP attempt " + (i + 1) + "/" + attempts + ": rejected (too close to another player) at "
                        + candidate.getBlockX() + ", " + candidate.getBlockY() + ", " + candidate.getBlockZ()
                        + " in " + world.getName());
                continue;
            }
            plugin.debugLog("RTP picked candidate " + candidate.getBlockX() + ", " + candidate.getBlockY() + ", "
                    + candidate.getBlockZ() + " in " + world.getName() + " after " + (i + 1) + " outer tries");
            return candidate;
        }
        plugin.debugLog("RTP failed after " + attempts + " attempts in " + world.getName());
        return null;
    }

    private boolean tooCloseToAnotherPlayer(World world, Location candidate, Player self) {
        double min = settings.minBlocksFromOtherPlayers();
        if (min <= 0 || self == null) {
            return false;
        }
        double minSq = min * min;
        UUID id = self.getUniqueId();
        for (Player other : world.getPlayers()) {
            if (other.getUniqueId().equals(id)) {
                continue;
            }
            Location ol = other.getLocation();
            if (!ol.getWorld().equals(world)) {
                continue;
            }
            if (ol.distanceSquared(candidate) < minSq) {
                return true;
            }
        }
        return false;
    }

    private Location findSurface(World world, int x, int z, Location base, ChunkLoadBudget budget) {
        int cx = x >> 4;
        int cz = z >> 4;
        if (!ensureChunkLoaded(world, cx, cz, budget)) {
            return null;
        }
        int surfaceY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING);
        int feetY = surfaceY + 1;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        if (feetY >= maxY - 1 || feetY <= minY) {
            return null;
        }
        Block floor = world.getBlockAt(x, surfaceY, z);
        if (isUnsafe(floor.getType())) {
            return null;
        }
        if (Tag.BEDS.isTagged(floor.getType())) {
            return null;
        }
        return new Location(world, x + 0.5, feetY, z + 0.5, base.getYaw(), base.getPitch());
    }

    private Location findInNether(World world, int x, int z, Location base, ChunkLoadBudget budget) {
        int cx = x >> 4;
        int cz = z >> 4;
        if (!ensureChunkLoaded(world, cx, cz, budget)) {
            return null;
        }
        int minY = Math.max(world.getMinHeight() + 2, 4);
        int maxY = Math.min(world.getMaxHeight() - 2, world.getLogicalHeight() - 2);
        if (maxY <= minY) {
            return null;
        }
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int t = 0; t < 24; t++) {
            int y = minY + r.nextInt(maxY - minY + 1);
            Block ground = world.getBlockAt(x, y - 1, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            if (!ground.getType().isSolid()) {
                continue;
            }
            if (isUnsafe(ground.getType())) {
                continue;
            }
            if (!isPassable(feet) || !isPassable(head)) {
                continue;
            }
            return new Location(world, x + 0.5, y, z + 0.5, base.getYaw(), base.getPitch());
        }
        return null;
    }

    private static boolean ensureChunkLoaded(World world, int chunkX, int chunkZ, ChunkLoadBudget budget) {
        if (world.isChunkLoaded(chunkX, chunkZ)) {
            return true;
        }
        if (!budget.tryConsumeLoad()) {
            return false;
        }
        world.getChunkAt(chunkX, chunkZ).load(true);
        return true;
    }

    private boolean isColumnSafe(World world, Location feetCenter) {
        int x = feetCenter.getBlockX();
        int y = feetCenter.getBlockY();
        int z = feetCenter.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    Block b = world.getBlockAt(x + dx, y + dy, z + dz);
                    if (isUnsafe(b.getType())) {
                        return false;
                    }
                }
            }
        }
        Block below = world.getBlockAt(x, y - 1, z);
        if (!below.getType().isSolid() || isUnsafe(below.getType())) {
            return false;
        }
        if (world.getEnvironment() != World.Environment.NETHER) {
            Block twoBelow = world.getBlockAt(x, y - 2, z);
            if (twoBelow.getType() == Material.LAVA) {
                return false;
            }
        }
        return true;
    }

    private boolean isPassable(Block block) {
        return isPassable(block.getType());
    }

    private boolean isPassable(Material m) {
        if (m.isAir()) {
            return true;
        }
        return !m.isSolid() && !unsafe.contains(m);
    }

    private boolean isUnsafe(Material m) {
        return unsafe.contains(m);
    }

    private int randomInRange(int min, int max) {
        if (min == max) {
            return min;
        }
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        return lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
    }

    /**
     * Limits synchronous chunk loads per {@link #findSafe} to reduce main-thread stalls.
     * {@code cap == 0} means unlimited.
     */
    private static final class ChunkLoadBudget {
        private int remaining;
        private final boolean unlimited;

        private ChunkLoadBudget(boolean unlimited, int remaining) {
            this.unlimited = unlimited;
            this.remaining = remaining;
        }

        static ChunkLoadBudget fromCap(int cap) {
            if (cap <= 0) {
                return new ChunkLoadBudget(true, 0);
            }
            return new ChunkLoadBudget(false, cap);
        }

        boolean tryConsumeLoad() {
            if (unlimited) {
                return true;
            }
            if (remaining <= 0) {
                return false;
            }
            remaining--;
            return true;
        }
    }
}
