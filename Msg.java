package dev.virtualchunkloader.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Einheitliche Chat-Ausgabe mit Präfix. */
final class Msg {

    private static final Component PREFIX = Component.text("[VirtualChunkLoader] ", NamedTextColor.GOLD);

    private Msg() {
    }

    static void send(CommandSender s, NamedTextColor color, String text) {
        s.sendMessage(PREFIX.append(Component.text(text, color)));
    }

    static void info(CommandSender s, String text) { send(s, NamedTextColor.GRAY, text); }
    static void ok(CommandSender s, String text) { send(s, NamedTextColor.GREEN, text); }
    static void warn(CommandSender s, String text) { send(s, NamedTextColor.YELLOW, text); }
    static void err(CommandSender s, String text) { send(s, NamedTextColor.RED, text); }
}
