package com.github.mundotv789123;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.entities.Message;
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
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class McMBotEvents extends ListenerAdapter implements Listener {

    private final List<String> players = new ArrayList();
    private final Map<String, String> messages = new HashMap();
    private final McMBot plugin;

    public McMBotEvents(McMBot plugin) {
        this.plugin = plugin;
    }
    
    /* eventos do servidor de minecraft */
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
        String msgid = sendDiscordMessage(message, dcId, nick);
        if (msgid != null) {
            messages.put(nick, msgid);
        }
    }
    
    @EventHandler
    protected void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        String nick = p.getName().toLowerCase();
        if (players.contains(nick)) {
            players.remove(nick);
            return;
        }
        TextChannel tc = plugin.getJda().getTextChannelById(plugin.getConfig().getString("config.login_chat"));
        if (tc == null) {
            return;
        }
        if (messages.containsKey(nick)) {
            deleteMessage(nick);
        }
        sendDiscordMessage(plugin.getConfig().getString("discord_messages.logout").replace("%player%", p.getName()).replace("%ip%", p.getAddress().getHostString()), null, null);
    }

    /* eventos de proteção */
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
    protected void onPlayerInteractEvent(PlayerInteractEvent e) {
        if (cancelEvent(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    protected void onPlayerDropItemEvent(PlayerDropItemEvent e) {
        if (cancelEvent(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    protected void onPluginDisableEvent(PluginDisableEvent e) {
        if (e.getPlugin().equals(plugin)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission(plugin.getConfig().getString("config.permission"))) {
                    deleteMessage(p.getName().toLowerCase());
                    p.kickPlayer(plugin.getConfig().getString("kick_messages.restarting").replace("&", "§"));
                }
            }
        }
    }

    /* eventos do bot do discord */
    @Override
    public void onButtonClick(ButtonClickEvent e) {
        String[] splited = e.getComponentId().split(":");
        /* verificando comando */
        if (!splited[0].equals("mcmbot")) {
            return;
        }
        if (splited.length != 3) {
            return;
        }
        
        /* verificação de segurança */
        if (!e.getTextChannel().getId().equals(plugin.getConfig().getString("config.login_chat"))) {
            return;
        }
        if (!e.getUser().getId().equals(getDiscordIdByNick(splited[2]))) {
            e.deferEdit().queue();
            return;
        }
        
        if (players.contains(splited[2])) {
            deleteMessage(splited[2]);
            return;
        }
        
        /* executando comando */
        switch (splited[1]) {
            case "confirm":
                players.add(splited[2]);
                e.getTextChannel().sendMessage(plugin.getConfig().getString("discord_messages.success").replace("%discord%", e.getUser().getAsMention())).queue();
                break;
            case "block":
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
        deleteMessage(splited[2]);
    }

    /* funções úteis */
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

    private String sendDiscordMessage(String message, String dcId, String nick) {
        TextChannel tc = plugin.getJda().getTextChannelById(plugin.getConfig().getString("config.login_chat"));
        if (tc != null) {
            MessageAction ma = tc.sendMessage(message);
            if (nick != null) {
                ma = ma.setActionRow(Button.success("mcmbot:confirm:" + nick, "Sim"), Button.danger("mcmbot:block:" + nick, "Não"));
            }
            return ma.complete().getId();
        }
        return null;
    }

    private boolean cancelEvent(Player p) {
        String nick = p.getName().toLowerCase();
        if (players.contains(nick)) {
            return false;
        }
        return p.hasPermission(plugin.getConfig().getString("config.permission"));
    }

    public void deleteMessage(String nick) {
        TextChannel tc = plugin.getJda().getTextChannelById(plugin.getConfig().getString("config.login_chat"));
        if (tc == null) {
            return;
        }
        if (!messages.containsKey(nick)) {
            return;
        }
        Message msg = tc.retrieveMessageById(messages.get(nick)).complete();
        if (msg != null) {
            msg.delete().queue();
        }
        messages.remove(nick);
    }
}
