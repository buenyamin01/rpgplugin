package tr.rpg;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RpgPlugin extends JavaPlugin {
    private ProfileManager profiles;
    private SkillExecutor skills;
    private RpgGui gui;

    @Override
    public void onEnable() {
        profiles = new ProfileManager(this);
        skills = new SkillExecutor(this);
        gui = new RpgGui(this);

        getServer().getPluginManager().registerEvents(new RpgListener(this), this);
        getServer().getPluginManager().registerEvents(gui, this);

        RpgCommand cmd = new RpgCommand(this);
        PluginCommand pc = getCommand("rpg");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }

        // Eklenti /reload ile yeniden yüklenirse çevrimiçi oyuncuların profilini hazırla
        for (Player p : getServer().getOnlinePlayers()) {
            profiles.get(p);
            profiles.applyAttributes(p);
        }
        profiles.start();
        getLogger().info("RpgPlugin aktif.");
    }

    @Override
    public void onDisable() {
        if (profiles != null) profiles.saveAll();
    }

    public ProfileManager profiles() { return profiles; }
    public SkillExecutor skills() { return skills; }
    public RpgGui gui() { return gui; }
}
