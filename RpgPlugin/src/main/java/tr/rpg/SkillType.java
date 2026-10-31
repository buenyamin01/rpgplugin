package tr.rpg;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public enum SkillType {
    // Savaşçı
    GUCLU_VURUS("Güçlü Vuruş", ClassType.SAVASCI, 1, 15, 5, StatType.STR, Material.IRON_AXE,
            "Önündeki düşmanlara güçlü bir darbe indirir."),
    SAVAS_NARASI("Savaş Narası", ClassType.SAVASCI, 5, 25, 20, StatType.STR, Material.GOAT_HORN,
            "Kendine ve yakındaki oyunculara Güç ve Hız verir."),
    YER_SARSINTISI("Yer Sarsıntısı", ClassType.SAVASCI, 10, 40, 30, StatType.STR, Material.ANVIL,
            "Çevrendekilere hasar verir, havaya fırlatır ve yavaşlatır."),
    // Okçu
    COKLU_OK("Çoklu Ok", ClassType.OKCU, 1, 15, 5, StatType.DEX, Material.ARROW,
            "Aynı anda 3 ok fırlatır."),
    HIZLI_ADIM("Hızlı Adım", ClassType.OKCU, 5, 20, 12, StatType.DEX, Material.FEATHER,
            "İleri atılır ve kısa süre hız kazanırsın."),
    OK_YAGMURU("Ok Yağmuru", ClassType.OKCU, 10, 45, 30, StatType.DEX, Material.SPECTRAL_ARROW,
            "Baktığın noktaya 3 saniye boyunca ok yağdırır."),
    // Büyücü
    ATES_OKU("Ateş Oku", ClassType.BUYUCU, 1, 12, 3, StatType.INT, Material.FIRE_CHARGE,
            "Baktığın düşmana ateş ışını gönderir ve yakar."),
    BUZ_HALKASI("Buz Halkası", ClassType.BUYUCU, 5, 30, 15, StatType.INT, Material.BLUE_ICE,
            "Çevrendekilere hasar verir ve dondurup yavaşlatır."),
    ZINCIRLEME_SIMSEK("Zincirleme Şimşek", ClassType.BUYUCU, 10, 45, 25, StatType.INT, Material.LIGHTNING_ROD,
            "Şimşek düşmandan düşmana zıplar."),
    // Haydut
    GOLGE_ADIMI("Gölge Adımı", ClassType.HAYDUT, 1, 15, 6, StatType.DEX, Material.ENDER_PEARL,
            "En yakın düşmanın arkasına ışınlanıp vurur."),
    DUMAN_BOMBASI("Duman Bombası", ClassType.HAYDUT, 5, 25, 20, StatType.DEX, Material.GUNPOWDER,
            "Düşmanlara körlük ve yavaşlık verir, sen görünmez olursun."),
    OLUM_DANSI("Ölüm Dansı", ClassType.HAYDUT, 10, 40, 28, StatType.DEX, Material.NETHERITE_SWORD,
            "Etrafında dönerek 4 kez hasar verirsin.");

    public static final int MAX_LEVEL = 5;

    public final String display;
    public final ClassType cls;
    public final int reqLevel;
    public final int mana;
    public final int cooldown; // saniye
    public final StatType scale;
    public final Material icon;
    public final String desc;

    SkillType(String display, ClassType cls, int reqLevel, int mana, int cooldown, StatType scale,
              Material icon, String desc) {
        this.display = display;
        this.cls = cls;
        this.reqLevel = reqLevel;
        this.mana = mana;
        this.cooldown = cooldown;
        this.scale = scale;
        this.icon = icon;
        this.desc = desc;
    }

    public static List<SkillType> of(ClassType c) {
        List<SkillType> out = new ArrayList<>();
        for (SkillType s : values()) if (s.cls == c) out.add(s);
        return out;
    }
}
