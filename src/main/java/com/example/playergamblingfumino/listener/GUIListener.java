package com.example.playergamblingfumino.listener;

import com.example.playergamblingfumino.PlayerGamblingFumino;
import com.example.playergamblingfumino.gui.BetSetupGUI;
import com.example.playergamblingfumino.gui.RPSGUI;
import com.example.playergamblingfumino.manager.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {

    private final PlayerGamblingFumino plugin;
    private final GameManager gameManager;

    public GUIListener(PlayerGamblingFumino plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInventory = player.getOpenInventory().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();

        if (holder instanceof BetSetupGUI.BetHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != topInventory) return;
            int slot = event.getSlot();
            BetSetupGUI.handleClick(player, slot, gameManager);

        } else if (holder instanceof RPSGUI.RPSHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != topInventory) return;
            int slot = event.getSlot();
            RPSGUI.handleClick(player, slot, gameManager);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BetSetupGUI.BetHolder ||
                holder instanceof RPSGUI.RPSHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (gameManager.isTransitioning(player)) return;

        if (gameManager.isPlayerInGUI(player)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (gameManager.isPlayerInGame(player) && gameManager.isPlayerInGUI(player)) {
                    player.sendMessage("你不能关闭对赌界面!");
                    var session = gameManager.getSession(player);
                    if (session != null) {
                        switch (session.getPhase()) {
                            case BET_SETUP -> BetSetupGUI.open(player, session, gameManager);
                            case RPS_CHOICE -> RPSGUI.open(player, session, gameManager);
                            case RESULT -> {}
                            case FINISHED -> gameManager.removePlayerFromGUI(player);
                        }
                    }
                }
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (gameManager.isPlayerInGame(player)) {
            gameManager.handlePlayerQuit(player);
        }
    }
}
