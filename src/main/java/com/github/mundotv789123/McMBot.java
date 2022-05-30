package com.github.mundotv789123;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class McMBot extends JavaPlugin {

    private JDA jda;

    public void onEnable() {
        McMBotEvents listener = new McMBotEvents(this);
        Bukkit.getPluginManager().registerEvents(listener, this);
        
        JDABuilder jdab = JDABuilder.createDefault(getConfig().getString("config.token"));
        jdab.addEventListeners(listener);
        try {
            this.jda = jdab.build();
        } catch (LoginException ex) {
            Logger.getLogger(McMBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void onDisable() {
        this.jda.shutdownNow();
    }

    public JDA getJda() {
        return jda;
    }
}
