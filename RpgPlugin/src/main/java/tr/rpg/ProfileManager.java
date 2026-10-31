package tr.rpg;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ProfileManager {
    public static final int MAX_LEVEL = 50;

    private final RpgPlugin plugin;
    private final File dir;
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();
    public final NamespacedKey healthKey;
    public final NamespacedKey skillArrowKey;
    /** Skill hasarı verilirken true olur; böylece stat çarpanları ikinci kez uygulanmaz. */
    public boolean casting = false;

    public ProfileManager(RpgPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "data");
        this.healthKey = new NamespacedKey(plugin, "rpg_health");
        this.skillArrowKey = new NamespacedKey(plugin, "skill_arrow");
    }

    public static long xpNeeded(int level) {
        return 100 + (long) (level - 1) * 75;
    }

    // ---------- Veri ----------

    public PlayerProfile get(Player p) {
        return profiles.computeIfAbsent(p.getUniqueId(), this::load);
    }

    private PlayerProfile load(UUID id) {
        PlayerProfile pr = new PlayerProfile(id);
        File f = new File(dir, id + ".yml");
        if (!f.exists()) return pr;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        try {
            String c = cfg.getString("class");
            if (c != null) pr.cls = ClassType.valueOf(c);
        } catch (IllegalArgumentException ignored) {}
        pr.level = Math.max(1, Math.min(MAX_LEVEL, cfg.getInt("level", 1)));
        pr.xp = cfg.getLong("xp", 0);
        pr.statPoints = cfg.getInt("stat-points", 0);
        pr.skillPoints = cfg.getInt("skill-points", 0);
        pr.selected = cfg.getInt("selected", 0);
        ConfigurationSection st = cfg.getConfigurationSection("stats");
        if (st != null) for (String k : st.getKeys(false)) {
            try { pr.alloc.put(StatType.valueOf(k), st.getInt(k)); } catch (IllegalArgumentException ignored) {}
        }
        ConfigurationSection sk = cfg.getConfigurationSection("skills");
        if (sk != null) for (String k : sk.getKeys(false)) {
            try { pr.skills.put(SkillType.valueOf(k), sk.getInt(k)); } catch (IllegalArgumentException ignored) {}
        }
        pr.mana = pr.maxMana();
        return pr;
    }

    public void save(PlayerProfile pr) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("class", pr.cls == null ? null : pr.cls.name());
        cfg.set("level", pr.level);
        cfg.set("xp", pr.xp);
        cfg.set("stat-points", pr.statPoints);
        cfg.set("skill-points", pr.skillPoints);
        cfg.set("selected", pr.selected);
        for (Map.Entry<StatType, Integer> e : pr.alloc.entrySet()) cfg.set("stats." + e.getKey().name(), e.getValue());
        for (Map.Entry<SkillType, Integer> e : pr.skills.entrySet()) cfg.set("skills." + e.getKey().name(), e.getValue());
        try {
            dir.mkdirs();
            cfg.save(new File(dir, pr.uuid + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Profil kaydedilemedi: " + e.getMessage());
        }
    }

    public void unload(Player p) {
        PlayerProfile pr = profiles.remove(p.getUniqueId());
        if (pr != null) save(pr);
    }

    public void saveAll() {
        for (PlayerProfile pr : profiles.values()) save(pr);
    }

    // ---------- Oyun mantığı ----------

    public void applyAttributes(Player p) {
        PlayerProfile pr = get(p);
        AttributeInstance ai = p.getAttribute(Attribute.MAX_HEALTH);
        if (ai == null) return;
        for (AttributeModifier m : new ArrayList<>(ai.getModifiers())) {
            if (m.getKey().equals(healthKey)) ai.removeModifier(m);
        }
        if (pr.cls != null) {
            ai.addModifier(new AttributeModifier(healthKey, pr.healthBonus(), AttributeModifier.Operation.ADD_NUMBER));
        }
        if (p.getHealth() > ai.getValue()) p.setHealth(ai.getValue());
    }

    /** Sınıf seçer/değiştirir; tüm puanlar seviyeye göre yeniden verilir. */
    public void setClass(Player p, ClassType c) {
        PlayerProfile pr = get(p);
        pr.cls = c;
        pr.alloc.clear();
        pr.skills.clear();
        pr.cooldowns.clear();
        pr.selected = 0;
        pr.statPoints = 3 * pr.level;
        pr.skillPoints = pr.level;
        pr.mana = pr.maxMana();
        applyAttributes(p);
        save(pr);
    }

    public void addXp(Player p, long amount) {
        PlayerProfile pr = get(p);
        if (pr.cls == null || pr.level >= MAX_LEVEL || amount <= 0) return;
        pr.xp += amount;
        boolean up = false;
        while (pr.level < MAX_LEVEL && pr.xp >= xpNeeded(pr.level)) {
            pr.xp -= xpNeeded(pr.level);
            pr.level++;
            pr.statPoints += 3;
            pr.skillPoints += 1;
            up = true;
        }
        if (pr.level >= MAX_LEVEL) pr.xp = 0;
        if (up) {
            applyAttributes(p);
            p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
            pr.mana = pr.maxMana();
            p.sendTitle(Msg.c("&6&lSEVİYE ATLADIN!"), Msg.c("&eSeviye " + pr.level), 5, 50, 15);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            Msg.send(p, "&aYeni seviye: &e" + pr.level + " &7| +3 stat puanı, +1 skill puanı. &e/rpg");
            save(pr);
        }
    }

    // ---------- Görevler ----------

    private String hud(PlayerProfile pr) {
        if (pr.cls == null) return "&7/rpg yazarak sınıfını seç";
        List<SkillType> sk = SkillType.of(pr.cls);
        SkillType s = sk.get(pr.selected % sk.size());
        long left = pr.cooldowns.getOrDefault(s, 0L) - System.currentTimeMillis();
        String state;
        if (pr.skillLevel(s) < 1) state = "&8Öğrenilmedi";
        else if (left > 0) state = "&c" + (int) Math.ceil(left / 1000.0) + "sn";
        else state = "&aHazır";
        String xp = pr.level >= MAX_LEVEL ? "MAKS" : pr.xp + "/" + xpNeeded(pr.level);
        return "&bMana &f" + (int) pr.mana + "/" + (int) pr.maxMana()
                + "  &eSv." + pr.level + " &7(" + xp + ")"
                + "  &d✦ " + s.display + " " + state;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerProfile pr = profiles.get(p.getUniqueId());
                if (pr == null) continue;
                if (pr.cls != null) pr.mana = Math.min(pr.maxMana(), pr.mana + pr.manaRegen());
                Msg.bar(p, hud(pr));
            }
        }, 20L, 20L);

        Bukkit.getScheduler().runTaskTimer(plugin, this::saveAll, 6000L, 6000L);
    }
}
