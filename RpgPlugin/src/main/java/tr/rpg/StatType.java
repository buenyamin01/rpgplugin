package tr.rpg;

import org.bukkit.Material;

public enum StatType {
    STR("Güç", Material.IRON_SWORD, "Yakın dövüş hasarını artırır (+%2 / puan)."),
    DEX("Çeviklik", Material.BOW, "Ok hasarını ve kritik şansını artırır."),
    INT("Zeka", Material.ENCHANTED_BOOK, "Maksimum mana ve mana yenilemeyi artırır."),
    VIT("Dayanıklılık", Material.GOLDEN_APPLE, "Maksimum canı artırır (+1 can / puan).");

    public final String display;
    public final Material icon;
    public final String desc;

    StatType(String display, Material icon, String desc) {
        this.display = display;
        this.icon = icon;
        this.desc = desc;
    }
}
