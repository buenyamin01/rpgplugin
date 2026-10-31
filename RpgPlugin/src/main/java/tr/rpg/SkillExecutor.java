package tr.rpg;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SkillExecutor {
    private final RpgPlugin plugin;
    private final ProfileManager mgr;

    public SkillExecutor(RpgPlugin plugin) {
        this.plugin = plugin;
        this.mgr = plugin.profiles();
    }

    // ================= Genel akış =================

    public void cast(Player p) {
        PlayerProfile pr = mgr.get(p);
        if (pr.cls == null) { Msg.bar(p, "&cÖnce /rpg ile sınıf seç!"); return; }
        List<SkillType> list = SkillType.of(pr.cls);
        SkillType s = list.get(pr.selected % list.size());

        int lv = pr.skillLevel(s);
        if (lv < 1) { Msg.bar(p, "&c" + s.display + " henüz öğrenilmedi! /rpg → Skiller"); return; }
        if (pr.level < s.reqLevel) { Msg.bar(p, "&cGereken seviye: " + s.reqLevel); return; }

        long now = System.currentTimeMillis();
        long until = pr.cooldowns.getOrDefault(s, 0L);
        if (now < until) { Msg.bar(p, "&cBekleme: " + (int) Math.ceil((until - now) / 1000.0) + " sn"); return; }
        if (pr.mana < s.mana) { Msg.bar(p, "&cYetersiz mana! (" + s.mana + " gerekli)"); return; }

        if (!execute(p, pr, s, lv)) return;

        pr.mana -= s.mana;
        double cd = s.cooldown * (1 - 0.05 * (lv - 1));
        pr.cooldowns.put(s, now + (long) (cd * 1000));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.6f, 1.3f);
    }

    public void cycle(Player p) {
        PlayerProfile pr = mgr.get(p);
        if (pr.cls == null) return;
        pr.selected = (pr.selected + 1) % SkillType.of(pr.cls).size();
        SkillType s = SkillType.of(pr.cls).get(pr.selected);
        Msg.bar(p, "&dSeçili skill: &f" + s.display);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
    }

    // ================= Yardımcılar =================

    private boolean valid(Player caster, Entity e) {
        if (!(e instanceof LivingEntity) || e == caster || e instanceof ArmorStand || e.isDead()) return false;
        if (e instanceof Tameable t && caster.equals(t.getOwner())) return false;
        if (e instanceof Player pl) {
            GameMode g = pl.getGameMode();
            if (g == GameMode.CREATIVE || g == GameMode.SPECTATOR) return false;
        }
        return true;
    }

    private List<LivingEntity> near(Player p, Location c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : c.getWorld().getNearbyEntities(c, r, r, r)) {
            if (valid(p, e)) out.add((LivingEntity) e);
        }
        return out;
    }

    private LivingEntity closest(Location from, List<LivingEntity> list) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            double d = e.getLocation().distanceSquared(from);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    private LivingEntity rayTarget(Player p, double range) {
        Location eye = p.getEyeLocation();
        RayTraceResult r = p.getWorld().rayTraceEntities(eye, eye.getDirection(), range, 0.6, e -> valid(p, e));
        return (r != null && r.getHitEntity() instanceof LivingEntity le) ? le : null;
    }

    private Location aimPoint(Player p, double range) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        RayTraceResult r = p.getWorld().rayTrace(eye, dir, range, FluidCollisionMode.NEVER, true, 0.3, e -> valid(p, e));
        if (r != null) return r.getHitPosition().toLocation(p.getWorld());
        return eye.clone().add(dir.clone().multiply(range));
    }

    private void hit(Player p, LivingEntity t, double dmg) {
        mgr.casting = true;
        try {
            t.damage(dmg, p);
        } finally {
            mgr.casting = false;
        }
    }

    private void line(Location from, Location to, Particle particle) {
        Vector d = to.toVector().subtract(from.toVector());
        double len = d.length();
        if (len < 0.1) return;
        d.normalize();
        for (double i = 0; i < len; i += 0.5) {
            from.getWorld().spawnParticle(particle, from.clone().add(d.clone().multiply(i)), 1, 0, 0, 0, 0);
        }
    }

    private static int ticks(double seconds) {
        return (int) (seconds * 20);
    }

    // ================= Skill'ler =================

    private boolean execute(Player p, PlayerProfile pr, SkillType s, int lv) {
        double pw = 6 + lv * 3 + pr.total(s.scale) * 0.6;
        World w = p.getWorld();
        Location loc = p.getLocation();

        switch (s) {
            case GUCLU_VURUS -> {
                Vector look = loc.getDirection().setY(0).normalize();
                for (LivingEntity e : near(p, loc, 4.5)) {
                    Vector to = e.getLocation().toVector().subtract(loc.toVector()).setY(0);
                    if (to.lengthSquared() > 0.01 && to.normalize().dot(look) < 0.3) continue;
                    hit(p, e, pw * 1.4);
                    e.setVelocity(look.clone().multiply(0.8).setY(0.3));
                }
                w.spawnParticle(Particle.SWEEP_ATTACK, p.getEyeLocation().add(look.clone().multiply(1.5)), 5, 0.8, 0.3, 0.8, 0);
                w.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.8f);
            }
            case SAVAS_NARASI -> {
                int dur = ticks(6 + 2 * lv);
                for (Entity e : w.getNearbyEntities(loc, 8, 8, 8)) {
                    if (e instanceof Player pl) {
                        pl.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, dur, 0));
                        pl.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, 0));
                    }
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, dur, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, 0));
                w.spawnParticle(Particle.ANGRY_VILLAGER, loc.clone().add(0, 2, 0), 15, 1, 0.5, 1, 0);
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1f, 1f);
            }
            case YER_SARSINTISI -> {
                for (LivingEntity e : near(p, loc, 6)) {
                    hit(p, e, pw * 1.2);
                    e.setVelocity(new Vector(0, 0.9, 0));
                    e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                }
                w.spawnParticle(Particle.CLOUD, loc, 80, 3, 0.2, 3, 0.05);
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.5f, 0.6f);
            }
            case COKLU_OK -> {
                Vector dir = p.getEyeLocation().getDirection();
                for (int i = -1; i <= 1; i++) {
                    Vector d = dir.clone().rotateAroundY(Math.toRadians(i * 8));
                    Arrow a = p.launchProjectile(Arrow.class, d.multiply(2.5));
                    a.setDamage(pw * 0.5);
                    a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    a.getPersistentDataContainer().set(mgr.skillArrowKey, PersistentDataType.BYTE, (byte) 1);
                }
                w.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1f, 1.2f);
            }
            case HIZLI_ADIM -> {
                Vector dir = loc.getDirection().setY(0);
                if (dir.lengthSquared() > 0.01) dir.normalize();
                p.setVelocity(dir.multiply(1.6).setY(0.25));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks(4 + lv), 1));
                w.spawnParticle(Particle.CLOUD, loc, 20, 0.3, 0.1, 0.3, 0.05);
                w.playSound(loc, Sound.ENTITY_BAT_TAKEOFF, 1f, 1.2f);
            }
            case OK_YAGMURU -> {
                final Location center = aimPoint(p, 30);
                final double dmg = pw * 0.5;
                new BukkitRunnable() {
                    int n = 0;
                    @Override public void run() {
                        if (!p.isOnline() || n++ >= 6) { cancel(); return; }
                        w.spawnParticle(Particle.CRIT, center.clone().add(0, 3, 0), 60, 2.5, 2, 2.5, 0.2);
                        for (LivingEntity e : near(p, center, 5)) hit(p, e, dmg);
                        w.playSound(center, Sound.ENTITY_ARROW_HIT, 1f, 1f);
                    }
                }.runTaskTimer(plugin, 0L, 10L);
            }
            case ATES_OKU -> {
                LivingEntity t = rayTarget(p, 24);
                Location from = p.getEyeLocation().subtract(0, 0.2, 0);
                Location to = t != null ? t.getEyeLocation() : from.clone().add(from.getDirection().multiply(24));
                line(from, to, Particle.FLAME);
                if (t != null) {
                    hit(p, t, pw * 1.3);
                    t.setFireTicks(ticks(3 + lv));
                }
                w.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
            }
            case BUZ_HALKASI -> {
                for (LivingEntity e : near(p, loc, 5)) {
                    hit(p, e, pw * 0.9);
                    e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(3 + lv), 2));
                    e.setFreezeTicks(140);
                }
                w.spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 0.5, 0), 100, 2.5, 0.3, 2.5, 0.02);
                w.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
            }
            case ZINCIRLEME_SIMSEK -> {
                LivingEntity first = rayTarget(p, 20);
                if (first == null) first = closest(loc, near(p, loc, 12));
                if (first == null) { Msg.bar(p, "&cHedef bulunamadı!"); return false; }
                int chains = 3 + lv / 2;
                Set<LivingEntity> done = new HashSet<>();
                LivingEntity cur = first;
                for (int i = 0; i < chains && cur != null; i++) {
                    done.add(cur);
                    hit(p, cur, pw * (1.2 - 0.1 * i));
                    w.strikeLightningEffect(cur.getLocation());
                    LivingEntity next = null;
                    double best = 36;
                    for (LivingEntity e : near(p, cur.getLocation(), 6)) {
                        if (done.contains(e)) continue;
                        double d = e.getLocation().distanceSquared(cur.getLocation());
                        if (d < best) { best = d; next = e; }
                    }
                    cur = next;
                }
            }
            case GOLGE_ADIMI -> {
                LivingEntity t = rayTarget(p, 10);
                if (t == null) t = closest(loc, near(p, loc, 8));
                if (t == null) { Msg.bar(p, "&cHedef bulunamadı!"); return false; }
                Vector dir = t.getLocation().getDirection().setY(0);
                if (dir.lengthSquared() < 0.01) dir = new Vector(0, 0, 1);
                dir.normalize();
                Location dest = t.getLocation().clone().subtract(dir.clone().multiply(1.5));
                if (!dest.getBlock().isPassable() || !dest.clone().add(0, 1, 0).getBlock().isPassable()) {
                    dest = t.getLocation().clone();
                }
                dest.setDirection(dir);
                w.spawnParticle(Particle.SMOKE, loc.clone().add(0, 1, 0), 30, 0.3, 0.6, 0.3, 0.02);
                p.teleport(dest);
                hit(p, t, pw * 1.8);
                w.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
            }
            case DUMAN_BOMBASI -> {
                int dur = ticks(3 + lv);
                for (LivingEntity e : near(p, loc, 6)) {
                    e.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, dur, 0));
                    e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, dur, 1));
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ticks(4 + lv), 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks(4 + lv), 1));
                w.spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0, 1, 0), 120, 2.5, 1, 2.5, 0.02);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.5f, 0.7f);
            }
            case OLUM_DANSI -> {
                final double dmg = pw * 0.6;
                new BukkitRunnable() {
                    int n = 0;
                    @Override public void run() {
                        if (!p.isOnline() || p.isDead() || n++ >= 4) { cancel(); return; }
                        Location l = p.getLocation();
                        w.spawnParticle(Particle.SWEEP_ATTACK, l.clone().add(0, 1, 0), 6, 1.5, 0.3, 1.5, 0);
                        for (LivingEntity e : near(p, l, 4)) hit(p, e, dmg);
                        w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.1f);
                    }
                }.runTaskTimer(plugin, 0L, 5L);
            }
        }
        return true;
    }
}
