package com.github.mundotv789123;

import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class McMBot extends JavaPlugin {

    private JDA jda;

    @Override
    public void onEnable() {
        McMBotEvents listener = new McMBotEvents(this);
        try {
            saveDefaultConfig();
            Bukkit.getPluginManager().registerEvents(listener, this);
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage("§c=================================================");
            Bukkit.getConsoleSender().sendMessage("§cError ao iniciar o plugin: §e" + ex.getMessage());
            Bukkit.getConsoleSender().sendMessage("§cDesligando servidor por segurança...");
            Bukkit.getConsoleSender().sendMessage("§c=================================================");
            ex.printStackTrace();
            Bukkit.getServer().shutdown();
            return;
        }

        try {
            JDABuilder jdab = JDABuilder.createDefault(getConfig().getString("config.token"));
            jdab.addEventListeners(listener);
            jda = jdab.build();
            jda.setAutoReconnect(true);
        } catch (LoginException ex) {
            Bukkit.getConsoleSender().sendMessage("§c=================================================");
            Bukkit.getConsoleSender().sendMessage("§cErro ao fazer login §e" + ex.getMessage());
            Bukkit.getConsoleSender().sendMessage("§c=================================================");
        }
    }

    @Override
    public void onDisable() {
        if (jda != null) {
            this.jda.shutdown();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equals("reload")) {
            reloadConfig();
            sender.sendMessage("§aconfig.yml reloaded!");
            return true;
        }
        sender.sendMessage("§cUsage: " + command.getUsage());
        return true;
    }

    public JDA getJda() {
        return jda;
    }
}
