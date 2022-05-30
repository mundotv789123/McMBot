package com.github.mundotv789123;

import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class McMBot extends JavaPlugin {

    private JDA jda;

    public void onEnable() {
        saveDefaultConfig();
        McMBotEvents listener = new McMBotEvents(this);
        Bukkit.getPluginManager().registerEvents(listener, this);

        JDABuilder jdab = JDABuilder.createDefault(getConfig().getString("config.token"));
        jdab.addEventListeners(listener);
        try {
            this.jda = jdab.build();
            jda.setAutoReconnect(true);
        } catch (LoginException ex) {
            Bukkit.getConsoleSender().sendMessage("§c=================================================");
            Bukkit.getConsoleSender().sendMessage("§cErro ao fazer login no bot §e" + ex.getMessage());
            Bukkit.getConsoleSender().sendMessage("§c=================================================");
        }
    }

    public void onDisable() {
        if (jda != null) {
            this.jda.shutdownNow();
        }
    }

    public JDA getJda() {
        return jda;
    }
}
