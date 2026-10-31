package tr.rpg;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class Msg {
    public static final String PREFIX = "&5[RPG] &r";

    private Msg() {}

    public static String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void bar(Player p, String s) {
        p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(c(s)));
    }

    public static void send(Player p, String s) {
        p.sendMessage(c(PREFIX + s));
    }
}
