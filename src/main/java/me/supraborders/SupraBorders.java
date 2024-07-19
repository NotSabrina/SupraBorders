package me.supraborders;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SupraBorders extends JavaPlugin {

    private int borderSize;
    private List<String> enabledWorlds;
    private final Map<Player, Integer> playerLevels = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        borderSize = getConfig().getInt("started-border");
        enabledWorlds = getConfig().getStringList("enabled-worlds");

        for (World world : Bukkit.getWorlds()) {
            if (enabledWorlds.contains(world.getName())) {
                WorldBorder worldBorder = world.getWorldBorder();
                worldBorder.setSize(borderSize);
            }
        }

        registerEvents();
        startTimers();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("restartborder")) {
            if (sender instanceof Player && !sender.hasPermission("supraborders.restart")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }
            restartBorders();
            sender.sendMessage("Borders have been reset to the initial size.");
            return true;
        }
        return false;
    }

    private void restartBorders() {
        for (World world : Bukkit.getWorlds()) {
            if (enabledWorlds.contains(world.getName())) {
                WorldBorder worldBorder = world.getWorldBorder();
                worldBorder.setSize(borderSize);
            }
        }
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new PlayerEvents(this), this);
    }

    private void startTimers() {
        Bukkit.getScheduler().runTaskTimer(this, this::expandBorderSecond, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, this::expandBorderMinute, 1200L, 1200L);
        Bukkit.getScheduler().runTaskTimer(this, this::expandBorderHour, 72000L, 72000L);
    }

    private void expandBorder(World world, int amount) {
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setSize(worldBorder.getSize() + amount);
    }

    private void expandBorderSecond() {
        if (getConfig().getBoolean("events.second")) {
            for (World world : Bukkit.getWorlds()) {
                if (enabledWorlds.contains(world.getName())) {
                    expandBorder(world, 1);
                }
            }
        }
    }

    private void expandBorderMinute() {
        if (getConfig().getBoolean("events.minute")) {
            for (World world : Bukkit.getWorlds()) {
                if (enabledWorlds.contains(world.getName())) {
                    expandBorder(world, 1);
                }
            }
        }
    }

    private void expandBorderHour() {
        if (getConfig().getBoolean("events.hour")) {
            for (World world : Bukkit.getWorlds()) {
                if (enabledWorlds.contains(world.getName())) {
                    expandBorder(world, 1);
                }
            }
        }
    }

    public void expandForEvent(String eventName, World world, int amount) {
        if (enabledWorlds.contains(world.getName()) && getConfig().getBoolean("events." + eventName)) {
            expandBorder(world, amount);
        }
    }

    public Map<Player, Integer> getPlayerLevels() {
        return playerLevels;
    }
}

class PlayerEvents implements Listener {

    private final SupraBorders plugin;

    public PlayerEvents(SupraBorders plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        int newLevel = player.getLevel();
        int oldLevel = plugin.getPlayerLevels().getOrDefault(player, newLevel);

        if (newLevel > oldLevel) {
            int levelDifference = newLevel - oldLevel;
            plugin.expandForEvent("playerexp", player.getWorld(), levelDifference);
            plugin.getPlayerLevels().put(player, newLevel);
        }
    }

    @EventHandler
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        plugin.expandForEvent("playereat", event.getPlayer().getWorld(), 1);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            plugin.expandForEvent("playerdmg", event.getEntity().getWorld(), 1);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            plugin.expandForEvent("mobkill", event.getEntity().getWorld(), 1);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerLevels().putIfAbsent(player, player.getLevel());
        plugin.expandForEvent("playerjoin", player.getWorld(), 1);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        plugin.expandForEvent("blockbreak", event.getPlayer().getWorld(), 1);
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player) {
            plugin.expandForEvent("playerkill", event.getEntity().getKiller().getWorld(), 1);
        }
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        plugin.expandForEvent("advancement", event.getPlayer().getWorld(), 1);
    }
}


