package tr.rpg;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

public class RpgListener implements Listener {
    private final RpgPlugin plugin;
    private final ProfileManager mgr;

    public RpgListener(RpgPlugin plugin) {
        this.plugin = plugin;
        this.mgr = plugin.profiles();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerProfile pr = mgr.get(p);
        mgr.applyAttributes(p);
        if (pr.cls == null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    Msg.send(p, "&eHoş geldin! Maceraya başlamak için sınıfını seç.");
                    plugin.gui().openClass(p);
                }
            }, 40L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        mgr.unload(e.getPlayer());
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        Player killer = dead.getKiller();
        if (killer == null || !(dead instanceof Enemy)) return;
        double maxHp = dead.getAttribute(Attribute.MAX_HEALTH).getValue();
        long xp = Math.max(2, Math.round(maxHp * 0.6));
        PlayerProfile pr = mgr.get(killer);
        if (pr.cls == null) return;
        mgr.addXp(killer, xp);
        if (pr.level < ProfileManager.MAX_LEVEL) Msg.bar(killer, "&a+" + xp + " XP");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (mgr.casting) return;
        Player attacker = null;
        boolean ranged = false;
        if (e.getDamager() instanceof Player pl) {
            attacker = pl;
        } else if (e.getDamager() instanceof AbstractArrow a && a.getShooter() instanceof Player pl) {
            if (a.getPersistentDataContainer().has(mgr.skillArrowKey, PersistentDataType.BYTE)) return;
            attacker = pl;
            ranged = true;
        }
        if (attacker == null) return;
        PlayerProfile pr = mgr.get(attacker);
        if (pr.cls == null) return;

        double mult = 1 + pr.total(ranged ? StatType.DEX : StatType.STR) * 0.02;
        double critChance = Math.min(40, pr.total(StatType.DEX) * 0.4);
        if (ThreadLocalRandom.current().nextDouble() * 100 < critChance) {
            mult *= 1.5;
            Msg.bar(attacker, "&6&l✦ KRİTİK ✦");
        }
        e.setDamage(e.getDamage() * mult);
    }

    /** F tuşu: skill kullan. Shift+F: skill değiştir. */
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        PlayerProfile pr = mgr.get(p);
        if (pr.cls == null) return;
        e.setCancelled(true);
        if (p.isSneaking()) plugin.skills().cycle(p);
        else plugin.skills().cast(p);
    }
}
