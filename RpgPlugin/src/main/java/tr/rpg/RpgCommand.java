package tr.rpg;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class RpgCommand implements CommandExecutor, TabCompleter {
    private final RpgPlugin plugin;

    public RpgCommand(RpgPlugin plugin) {
        this.plugin = plugin;
    }

    private void msg(CommandSender s, String t) {
        s.sendMessage(Msg.c(Msg.PREFIX + t));
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        ProfileManager mgr = plugin.profiles();

        if (a.length == 0 || !a[0].equalsIgnoreCase("admin")) {
            if (!(s instanceof Player p)) { msg(s, "&cOyun içinden kullan."); return true; }
            String sub = a.length == 0 ? "" : a[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "sinif" -> plugin.gui().openClass(p);
                case "stat" -> { if (mgr.get(p).cls == null) plugin.gui().openClass(p); else plugin.gui().openStats(p); }
                case "skill" -> { if (mgr.get(p).cls == null) plugin.gui().openClass(p); else plugin.gui().openSkills(p); }
                case "bilgi" -> {
                    PlayerProfile pr = mgr.get(p);
                    if (pr.cls == null) { msg(s, "&7Henüz sınıf seçmedin."); return true; }
                    msg(s, "&e" + pr.cls.display + " &7| Seviye &f" + pr.level + " &7| XP &f" + pr.xp + "/" + ProfileManager.xpNeeded(pr.level));
                    for (StatType st : StatType.values()) s.sendMessage(Msg.c(" &7" + st.display + ": &f" + pr.total(st)));
                }
                default -> plugin.gui().openMain(p);
            }
            return true;
        }

        // /rpg admin ...
        if (!s.hasPermission("rpg.admin")) { msg(s, "&cYetkin yok."); return true; }
        if (a.length < 3) {
            msg(s, "&cKullanım: /rpg admin <seviye|xp|sifirla> <oyuncu> [değer]");
            return true;
        }
        Player t = Bukkit.getPlayerExact(a[2]);
        if (t == null) { msg(s, "&cOyuncu çevrimdışı."); return true; }
        PlayerProfile pr = mgr.get(t);

        switch (a[1].toLowerCase(Locale.ROOT)) {
            case "seviye" -> {
                if (a.length < 4) { msg(s, "&cKullanım: /rpg admin seviye <oyuncu> <1-" + ProfileManager.MAX_LEVEL + ">"); return true; }
                int lv;
                try { lv = Integer.parseInt(a[3]); } catch (NumberFormatException ex) { msg(s, "&cGeçersiz sayı."); return true; }
                pr.level = Math.max(1, Math.min(ProfileManager.MAX_LEVEL, lv));
                pr.xp = 0;
                if (pr.cls != null) mgr.setClass(t, pr.cls); // puanları seviyeye göre yeniden verir
                msg(s, "&a" + t.getName() + " seviyesi " + pr.level + " yapıldı (dağılım sıfırlandı).");
            }
            case "xp" -> {
                if (a.length < 4) { msg(s, "&cKullanım: /rpg admin xp <oyuncu> <miktar>"); return true; }
                try { mgr.addXp(t, Long.parseLong(a[3])); } catch (NumberFormatException ex) { msg(s, "&cGeçersiz sayı."); return true; }
                msg(s, "&aXP verildi.");
            }
            case "sifirla" -> {
                pr.reset();
                mgr.applyAttributes(t);
                mgr.save(pr);
                msg(s, "&a" + t.getName() + " profili sıfırlandı.");
            }
            default -> msg(s, "&cBilinmeyen alt komut.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> opts = new ArrayList<>();
        if (a.length == 1) {
            opts.addAll(List.of("sinif", "stat", "skill", "bilgi"));
            if (s.hasPermission("rpg.admin")) opts.add("admin");
        } else if (a.length == 2 && a[0].equalsIgnoreCase("admin") && s.hasPermission("rpg.admin")) {
            opts.addAll(List.of("seviye", "xp", "sifirla"));
        } else if (a.length == 3 && a[0].equalsIgnoreCase("admin") && s.hasPermission("rpg.admin")) {
            for (Player p : Bukkit.getOnlinePlayers()) opts.add(p.getName());
        }
        List<String> out = new ArrayList<>();
        String cur = a[a.length - 1].toLowerCase(Locale.ROOT);
        for (String o : opts) if (o.toLowerCase(Locale.ROOT).startsWith(cur)) out.add(o);
        return out;
    }
}
