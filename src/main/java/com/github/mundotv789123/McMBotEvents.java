package com.github.mundotv789123;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class McMBotEvents extends ListenerAdapter implements Listener {

    private final List<String> players = new ArrayList();
    private final McMBot plugin;

    public McMBotEvents(McMBot plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    protected void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String nick = p.getName().toLowerCase();

        if (players.contains(nick)) {
            return;
        }
        if (!p.hasPermission(plugin.getConfig().getString("config.permission"))) {
            return;
        }
        String dcId = getDiscordIdByNick(nick);
        if (dcId == null) {
            p.kickPlayer(plugin.getConfig().getString("kick_messages.discord_not_found").replace("&", "§"));
            String message = plugin.getConfig().getString("discord_messages.permission");
            message = message.replace("%player%", p.getName());
            message = message.replace("%ip%", p.getAddress().getHostString());
            sendDiscordMessage(message, null, null);
            return;
        }
        String message = plugin.getConfig().getString("discord_messages.login");
        message = message.replace("%player%", p.getName());
        message = message.replace("%ip%", p.getAddress().getHostString());
        message = message.replace("%discord%", "<@" + dcId + ">");
        sendDiscordMessage(message, dcId, nick);
    }

    @EventHandler
    protected void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if (cancelEvent(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    protected void onPlayerMove(PlayerMoveEvent e) {
        if (cancelEvent(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    protected void onPlayerChat(PlayerChatEvent e) {
        if (cancelEvent(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    void onPlayerInteractEvent(PlayerInteractEvent e) {
        if (cancelEvent(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    void onPlayerDropItemEvent(PlayerDropItemEvent e) {
        if (cancelEvent(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        String nick = p.getName().toLowerCase();
        if (players.contains(nick)) {
            players.remove(nick);
        }
    }

    @Override
    public void onButtonClick(ButtonClickEvent e) {
        String[] splited = e.getComponentId().split(":");
        if (splited.length != 3) {
            return;
        }
        if (!e.getUser().getId().equals(splited[1])) {
            e.deferEdit().queue();
            return;
        }
        if (players.contains(splited[2])) {
            e.getMessage().delete().queue();
            return;
        }
        if (!splited[1].equals(getDiscordIdByNick(splited[2]))) {
            e.deferEdit().queue();
            return;
        }
        switch (splited[0]) {
            case "mcmbot_confirm":
                players.add(splited[2]);
                e.getTextChannel().sendMessage(plugin.getConfig().getString("discord_messages.success").replace("%discord%", e.getUser().getAsMention())).queue();
                break;
            case "mcmbot_block":
                Player p = Bukkit.getPlayer(splited[2]);
                if (plugin.getConfig().isList("config.refused_commands")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            List<String> commands = plugin.getConfig().getStringList("config.refused_commands");
                            for (String command : commands) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", p.getName()).replace("%ip%", p.getAddress().getHostString()).replace("&", "§"));
                            }
                            p.kickPlayer("");
                        }
                    }.runTask(plugin);
                }

                break;
            default:
                return;
        }
        e.getMessage().delete().queue();
    }

    @Nullable
    private String getDiscordIdByNick(String nick) {
        List<String> accounts = plugin.getConfig().getStringList("accounts");
        for (String account : accounts) {
            String[] splited = account.split(":");
            if (splited.length != 2) {
                continue;
            }
            if (splited[0].toLowerCase().equals(nick)) {
                return splited[1];
            }
        }
        return null;
    }

    private void sendDiscordMessage(String message, String dcId, String nick) {
        TextChannel tc = plugin.getJda().getTextChannelById(plugin.getConfig().getString("config.login_chat"));
        if (tc != null) {
            MessageAction ma = tc.sendMessage(message);
            if (dcId != null) {
                ma = ma.setActionRow(Button.success("mcmbot_confirm:" + dcId + ":" + nick, "Sim"), Button.danger("mcmbot_block:" + dcId + ":" + nick, "Não"));
            }
            ma.queue();
        }
    }

    private boolean cancelEvent(Player p) {
        String nick = p.getName().toLowerCase();
        if (players.contains(nick)) {
            return false;
        }
        return p.hasPermission(plugin.getConfig().getString("config.permission"));
    }
}
