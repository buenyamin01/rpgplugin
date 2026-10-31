package tr.rpg;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerProfile {
    public final UUID uuid;
    public ClassType cls;
    public int level = 1;
    public long xp = 0;
    public int statPoints = 0;
    public int skillPoints = 0;
    public final EnumMap<StatType, Integer> alloc = new EnumMap<>(StatType.class);
    public final EnumMap<SkillType, Integer> skills = new EnumMap<>(SkillType.class);
    public int selected = 0;

    // çalışma anı verisi (kaydedilmez)
    public double mana = 0;
    public final Map<SkillType, Long> cooldowns = new HashMap<>();

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public int total(StatType s) {
        return (cls == null ? 0 : cls.base(s)) + alloc.getOrDefault(s, 0);
    }

    public int skillLevel(SkillType s) {
        return skills.getOrDefault(s, 0);
    }

    public double maxMana() {
        return 40 + total(StatType.INT) * 5 + level * 2;
    }

    public double manaRegen() {
        return 2 + total(StatType.INT) * 0.15;
    }

    public double healthBonus() {
        return total(StatType.VIT) + (level - 1) * 0.5 - 4;
    }

    public void reset() {
        cls = null;
        level = 1;
        xp = 0;
        statPoints = 0;
        skillPoints = 0;
        alloc.clear();
        skills.clear();
        selected = 0;
        mana = 0;
        cooldowns.clear();
    }
}
