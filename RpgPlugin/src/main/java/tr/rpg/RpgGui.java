package tr.rpg;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class RpgGui implements Listener {
    enum Type { MAIN, CLASS, STATS, SKILLS }

    static class Holder implements InventoryHolder {
        final Type type;
        Inventory inv;
        Holder(Type type) { this.type = type; }
        @Override public Inventory getInventory() { return inv; }
    }

    private final RpgPlugin plugin;
    private final ProfileManager mgr;

    public RpgGui(RpgPlugin plugin) {
        this.plugin = plugin;
        this.mgr = plugin.profiles();
    }

    // ================= Yardımcılar =================

    private static ItemStack item(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(Msg.c("&r" + name));
        List<String> l = new ArrayList<>();
        for (String s : lore) l.add(Msg.c("&r" + s));
        m.setLore(l);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        it.setItemMeta(m);
        return it;
    }

    private Inventory create(Type t, int size, String title) {
        Holder h = new Holder(t);
        Inventory inv = Bukkit.createInventory(h, size, Msg.c(title));
        h.inv = inv;
        ItemStack glass = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < size; i++) inv.setItem(i, glass);
        return inv;
    }

    private static String bar(double frac) {
        int filled = (int) Math.round(Math.max(0, Math.min(1, frac)) * 10);
        return "&a" + "█".repeat(filled) + "&7" + "█".repeat(10 - filled);
    }

    // ================= Ana menü =================

    public void openMain(Player p) {
        PlayerProfile pr = mgr.get(p);
        if (pr.cls == null) { openClass(p); return; }
        Inventory inv = create(Type.MAIN, 27, "&8Karakter");

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(p);
        sm.setDisplayName(Msg.c("&r&e" + p.getName() + " &7- &fSeviye " + pr.level));
        double frac = pr.level >= ProfileManager.MAX_LEVEL ? 1 : (double) pr.xp / ProfileManager.xpNeeded(pr.level);
        List<String> lore = new ArrayList<>();
        lore.add(Msg.c("&r&7Sınıf: " + pr.cls.color + pr.cls.display));
        lore.add(Msg.c("&r" + bar(frac) + " &7" + (int) (frac * 100) + "%"));
        lore.add(Msg.c("&r&7XP: &f" + (pr.level >= ProfileManager.MAX_LEVEL ? "MAKS" : pr.xp + "/" + ProfileManager.xpNeeded(pr.level))));
        lore.add("");
        lore.add(Msg.c("&r&7Can: &c" + (int) p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
        lore.add(Msg.c("&r&7Mana: &b" + (int) pr.maxMana() + " &7(+" + String.format("%.1f", pr.manaRegen()) + "/sn)"));
        lore.add("");
        lore.add(Msg.c("&r&7Stat puanı: &e" + pr.statPoints + " &7| Skill puanı: &d" + pr.skillPoints));
        sm.setLore(lore);
        head.setItemMeta(sm);
        inv.setItem(10, head);

        inv.setItem(12, item(Material.IRON_SWORD, "&aStatlar", "&7Dağıtılacak puan: &e" + pr.statPoints, "", "&eTıkla: aç"));
        inv.setItem(14, item(Material.ENCHANTED_BOOK, "&dSkiller", "&7Skill puanı: &d" + pr.skillPoints, "", "&eTıkla: aç"));
        inv.setItem(16, item(Material.NETHER_STAR, "&6Sınıf Değiştir", "&7Tüm puanlar iade edilir ve", "&7stat/skill dağılımı sıfırlanır.", "", "&eTıkla: aç"));
        inv.setItem(22, item(Material.PAPER, "&bKontroller",
                "&7F: &fSeçili skill'i kullan",
                "&7Shift+F: &fSkill değiştir",
                "&7Mana zamanla yenilenir."));
        p.openInventory(inv);
    }

    // ================= Sınıf seçimi =================

    public void openClass(Player p) {
        PlayerProfile pr = mgr.get(p);
        Inventory inv = create(Type.CLASS, 27, "&8Sınıf Seç");
        int[] slots = {10, 12, 14, 16};
        ClassType[] all = ClassType.values();
        for (int i = 0; i < all.length; i++) {
            ClassType c = all[i];
            List<String> lore = new ArrayList<>();
            lore.add("&7" + c.desc);
            lore.add("");
            lore.add("&6Başlangıç Statları:");
            for (StatType s : StatType.values()) lore.add(" &7" + s.display + ": &f" + c.base(s));
            lore.add("");
            lore.add("&dSkiller:");
            for (SkillType s : SkillType.of(c)) lore.add(" &7" + s.display + " &8(Sv." + s.reqLevel + ")");
            lore.add("");
            if (pr.cls == c) lore.add("&aŞu anki sınıfın");
            else if (pr.cls == null) lore.add("&eTıkla: bu sınıfı seç");
            else lore.add("&cShift+Sol tık: sınıfı değiştir (sıfırlar)");
            inv.setItem(slots[i], item(c.icon, c.color + "&l" + c.display, lore.toArray(new String[0])));
        }
        if (pr.cls != null) inv.setItem(22, item(Material.ARROW, "&7« Geri"));
        p.openInventory(inv);
    }

    // ================= Statlar =================

    public void openStats(Player p) {
        PlayerProfile pr = mgr.get(p);
        Inventory inv = create(Type.STATS, 27, "&8Statlar");
        inv.setItem(4, item(Material.EXPERIENCE_BOTTLE, "&eDağıtılacak Puan: &f" + pr.statPoints,
                "&7Her seviyede +3 puan kazanırsın."));
        int[] slots = {10, 12, 14, 16};
        StatType[] all = StatType.values();
        for (int i = 0; i < all.length; i++) {
            StatType s = all[i];
            inv.setItem(slots[i], item(s.icon, "&a" + s.display,
                    "&7" + s.desc, "",
                    "&7Sınıf tabanı: &f" + pr.cls.base(s),
                    "&7Dağıtılan: &e" + pr.alloc.getOrDefault(s, 0),
                    "&7Toplam: &6" + pr.total(s), "",
                    "&aSol tık: +1", "&aShift+Sol: +5"));
        }
        inv.setItem(22, item(Material.ARROW, "&7« Geri"));
        p.openInventory(inv);
    }

    // ================= Skiller =================

    public void openSkills(Player p) {
        PlayerProfile pr = mgr.get(p);
        Inventory inv = create(Type.SKILLS, 27, "&8Skiller");
        inv.setItem(4, item(Material.EXPERIENCE_BOTTLE, "&dSkill Puanı: &f" + pr.skillPoints,
                "&7Her seviyede +1 puan kazanırsın."));
        List<SkillType> list = SkillType.of(pr.cls);
        int[] slots = {11, 13, 15};
        for (int i = 0; i < list.size() && i < slots.length; i++) {
            SkillType s = list.get(i);
            int lv = pr.skillLevel(s);
            double pw = 6 + Math.max(1, lv) * 3 + pr.total(s.scale) * 0.6;
            boolean locked = pr.level < s.reqLevel;
            boolean sel = (pr.selected % list.size()) == i;
            List<String> lore = new ArrayList<>();
            lore.add("&7" + s.desc);
            lore.add("");
            lore.add("&7Seviye: &e" + lv + "&7/" + SkillType.MAX_LEVEL);
            lore.add("&7Mana: &b" + s.mana + " &7| Bekleme: &f" + s.cooldown + "sn");
            lore.add("&7Güç: &c" + (int) pw + " &8(" + s.scale.display + " ile artar)");
            lore.add("&7Gereken seviye: " + (locked ? "&c" : "&a") + s.reqLevel);
            lore.add("");
            if (locked) lore.add("&cKilitli");
            else if (lv >= SkillType.MAX_LEVEL) lore.add("&6Maksimum seviye");
            else lore.add("&aSol tık: yükselt (1 puan)");
            lore.add(sel ? "&d✦ Aktif skill" : "&eSağ tık: aktif skill yap");
            ItemStack it = item(s.icon, (sel ? "&d" : "&f") + s.display, lore.toArray(new String[0]));
            if (sel) {
                ItemMeta m = it.getItemMeta();
                m.setEnchantmentGlintOverride(true);
                it.setItemMeta(m);
            }
            inv.setItem(slots[i], it);
        }
        inv.setItem(22, item(Material.ARROW, "&7« Geri"));
        p.openInventory(inv);
    }

    // ================= Tıklamalar =================

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getInventory()) return;
        PlayerProfile pr = mgr.get(p);
        int slot = e.getSlot();
        ClickType ct = e.getClick();

        switch (h.type) {
            case MAIN -> {
                if (slot == 12) openStats(p);
                else if (slot == 14) openSkills(p);
                else if (slot == 16) openClass(p);
            }
            case CLASS -> {
                if (slot == 22 && pr.cls != null) { openMain(p); return; }
                int idx = slot == 10 ? 0 : slot == 12 ? 1 : slot == 14 ? 2 : slot == 16 ? 3 : -1;
                if (idx < 0) return;
                ClassType c = ClassType.values()[idx];
                if (pr.cls == c) return;
                if (pr.cls != null && ct != ClickType.SHIFT_LEFT) {
                    Msg.send(p, "&cSınıf değiştirmek için &eShift+Sol tık &cyap. Tüm dağılımın sıfırlanır!");
                    return;
                }
                mgr.setClass(p, c);
                Msg.send(p, "&aArtık bir " + c.color + c.display + "&a! &7Stat ve skill puanlarını dağıtmak için &e/rpg");
                p.closeInventory();
                openMain(p);
            }
            case STATS -> {
                if (slot == 22) { openMain(p); return; }
                int idx = slot == 10 ? 0 : slot == 12 ? 1 : slot == 14 ? 2 : slot == 16 ? 3 : -1;
                if (idx < 0) return;
                if (pr.statPoints <= 0) { Msg.send(p, "&cDağıtılacak stat puanın yok."); return; }
                int amt = Math.min(pr.statPoints, ct.isShiftClick() ? 5 : 1);
                StatType s = StatType.values()[idx];
                pr.alloc.merge(s, amt, Integer::sum);
                pr.statPoints -= amt;
                if (s == StatType.VIT) mgr.applyAttributes(p);
                openStats(p);
            }
            case SKILLS -> {
                if (slot == 22) { openMain(p); return; }
                int idx = slot == 11 ? 0 : slot == 13 ? 1 : slot == 15 ? 2 : -1;
                List<SkillType> list = SkillType.of(pr.cls);
                if (idx < 0 || idx >= list.size()) return;
                SkillType s = list.get(idx);
                if (ct.isRightClick()) {
                    pr.selected = idx;
                    Msg.send(p, "&dAktif skill: &f" + s.display);
                } else {
                    if (pr.level < s.reqLevel) { Msg.send(p, "&cBu skill için seviye " + s.reqLevel + " gerekli."); return; }
                    if (pr.skillLevel(s) >= SkillType.MAX_LEVEL) { Msg.send(p, "&eBu skill zaten maksimum seviyede."); return; }
                    if (pr.skillPoints <= 0) { Msg.send(p, "&cSkill puanın yok."); return; }
                    pr.skills.merge(s, 1, Integer::sum);
                    pr.skillPoints--;
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
                }
                openSkills(p);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }
}
