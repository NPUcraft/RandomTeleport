package com.npucraft.randomteleport.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Text {

    private Text() {
    }

    public static String colorize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public static void send(CommandSender to, String prefix, String message) {
        to.sendMessage(colorize(prefix + message));
    }

    /** Clears the bar when {@code raw} is null or blank. */
    public static void actionBar(Player player, String raw) {
        if (player == null) {
            return;
        }
        if (raw == null || raw.isBlank()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colorize(raw)));
    }
}
