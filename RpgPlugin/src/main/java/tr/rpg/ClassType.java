package tr.rpg;

import org.bukkit.Material;

public enum ClassType {
    //        ad         renk   ikon                   açıklama                                         GÜÇ DEX ZEKA DAY
    SAVASCI("Savaşçı", "&c", Material.IRON_SWORD,     "Yakın dövüşte güçlü ve dayanıklı ön saf.",       8, 4, 2, 8),
    OKCU("Okçu",       "&a", Material.BOW,            "Uzaktan yüksek hasar ve hareketlilik.",          4, 9, 3, 5),
    BUYUCU("Büyücü",   "&b", Material.BLAZE_ROD,      "Alan hasarı ve büyü ustası.",                    2, 3, 10, 4),
    HAYDUT("Haydut",   "&7", Material.DIAMOND_SWORD,  "Hızlı, ani saldırılar ve kaçış ustası.",         6, 8, 2, 4);

    public final String display;
    public final String color;
    public final Material icon;
    public final String desc;
    private final int[] base;

    ClassType(String display, String color, Material icon, String desc, int str, int dex, int intel, int vit) {
        this.display = display;
        this.color = color;
        this.icon = icon;
        this.desc = desc;
        this.base = new int[]{str, dex, intel, vit};
    }

    public int base(StatType s) {
        return base[s.ordinal()];
    }
}
