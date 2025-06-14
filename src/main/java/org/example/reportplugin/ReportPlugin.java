package org.example.reportplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReportPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private Map<UUID, List<String>> reports = new HashMap<>();
    private Inventory reportsInventory;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("report").setExecutor(this);
        getCommand("reports").setExecutor(this);
        getCommand("reportscheck").setExecutor(this);
        saveDefaultConfig();
        
        // Инициализация инвентаря для просмотра репортов
        reportsInventory = Bukkit.createInventory(null, 54, ChatColor.RED + "Система жалоб");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("report")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Использование: /report <ник> <причина>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Игрок не найден!");
                return true;
            }

            StringBuilder reason = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                reason.append(args[i]).append(" ");
            }

            UUID targetUUID = target.getUniqueId();
            if (!reports.containsKey(targetUUID)) {
                reports.put(targetUUID, new ArrayList<>());
            }

            reports.get(targetUUID).add(ChatColor.YELLOW + "От " + player.getName() + ": " + ChatColor.WHITE + reason.toString());
            player.sendMessage(ChatColor.GREEN + "Жалоба на " + target.getName() + " отправлена!");
            
            // Оповещение админов
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("reportplugin.reportscheck")) {
                    p.sendMessage(ChatColor.RED + "[Репорт] " + ChatColor.YELLOW + player.getName() + 
                                " пожаловался на " + target.getName() + ": " + ChatColor.WHITE + reason);
                }
            }
            
            // Звук при отправке репорта
            player.playSound(player.getLocation(), "ENTITY_PLAYER_LEVELUP", 1.0f, 1.0f);
            
        } else if (cmd.getName().equalsIgnoreCase("reports")) {
            if (!reports.containsKey(player.getUniqueId()) || reports.get(player.getUniqueId()).isEmpty()) {
                player.sendMessage(ChatColor.GREEN + "На вас нет жалоб!");
                return true;
            }

            player.sendMessage(ChatColor.RED + "=== Жалобы на вас ===");
            for (String report : reports.get(player.getUniqueId())) {
                player.sendMessage(report);
            }
            
        } else if (cmd.getName().equalsIgnoreCase("reportscheck")) {
            if (!player.hasPermission("reportplugin.reportscheck")) {
                player.sendMessage(ChatColor.RED + "У вас нет прав на эту команду!");
                return true;
            }

            openReportsMenu(player);
        }

        return true;
    }

    private void openReportsMenu(Player player) {
        reportsInventory.clear();
        
        int slot = 0;
        for (Map.Entry<UUID, List<String>> entry : reports.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            
            Player target = Bukkit.getPlayer(entry.getKey());
            if (target == null) continue;
            
            ItemStack head = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.setDisplayName(ChatColor.RED + target.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GOLD + "Количество жалоб: " + entry.getValue().size());
            lore.add(ChatColor.YELLOW + "Кликните для просмотра");
            meta.setLore(lore);
            
            head.setItemMeta(meta);
            reportsInventory.setItem(slot++, head);
        }
        
        player.openInventory(reportsInventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.RED + "Система жалоб")) return;
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        Player target = Bukkit.getPlayer(meta.getOwningPlayer().getName());
        if (target == null) return;
        
        player.closeInventory();
        player.sendMessage(ChatColor.RED + "=== Жалобы на " + target.getName() + " ===");
        for (String report : reports.get(target.getUniqueId())) {
            player.sendMessage(report);
        }
    }

    @Override
    public void onDisable() {
        // Сохранение репортов при выключении плагина
        getConfig().set("reports", reports);
        saveConfig();
    }
}
