package com.npucraft.randomteleport.feedback;

import com.npucraft.randomteleport.config.EffectEntry;
import com.npucraft.randomteleport.config.EffectsSettings;
import com.npucraft.randomteleport.util.Text;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class PlayerFeedback {

    private PlayerFeedback() {
    }

    public static void apply(
            EffectsSettings pack,
            EffectEntry entry,
            Player player,
            Location soundAt,
            Replacer replacer
    ) {
        if (!pack.masterEnabled() || !entry.enabled()) {
            return;
        }
        if (entry.sound() != null && soundAt != null) {
            player.playSound(soundAt, entry.sound(), entry.volume(), entry.pitch());
        }
        String title = Text.colorize(replacer.apply(entry.title() == null ? "" : entry.title()));
        String sub = Text.colorize(replacer.apply(entry.subtitle() == null ? "" : entry.subtitle()));
        if (!title.isEmpty() || !sub.isEmpty()) {
            player.sendTitle(title, sub, entry.fadeIn(), entry.stay(), entry.fadeOut());
        }
    }

    @FunctionalInterface
    public interface Replacer {
        String apply(String raw);
    }
}
