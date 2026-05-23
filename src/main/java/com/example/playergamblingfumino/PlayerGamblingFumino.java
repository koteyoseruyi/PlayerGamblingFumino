package com.example.playergamblingfumino;

import com.example.playergamblingfumino.command.GamblingCommand;
import com.example.playergamblingfumino.listener.GUIListener;
import com.example.playergamblingfumino.manager.EconomyManager;
import com.example.playergamblingfumino.manager.GameManager;
import com.example.playergamblingfumino.manager.InvitationManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerGamblingFumino extends JavaPlugin {

    private static PlayerGamblingFumino instance;

    private InvitationManager invitationManager;
    private EconomyManager economyManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.invitationManager = new InvitationManager(this);
        this.economyManager = new EconomyManager(this);
        this.gameManager = new GameManager(this, economyManager);

        GamblingCommand gamblingCommand = new GamblingCommand(this, gameManager, invitationManager);
        getCommand("gamble").setExecutor(gamblingCommand);
        getCommand("gamble").setTabCompleter(gamblingCommand);

        getServer().getPluginManager().registerEvents(new GUIListener(this, gameManager), this);

        getLogger().info("§aPlayerGamblingFumino 已启用!");
        if (economyManager.isEnabled()) {
            getLogger().info("§a[经济] 已对接Vault, 经济可用");
        } else {
            getLogger().warning("§c[经济] 未对接Vault, 经济功能不可用");
        }
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.saveAll();
        getServer().getOnlinePlayers().forEach(player -> player.closeInventory());
        getLogger().info("PlayerGamblingFumino 已禁用");
    }

    public static PlayerGamblingFumino getInstance() { return instance; }

    public GameManager getGameManager() { return gameManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
}
