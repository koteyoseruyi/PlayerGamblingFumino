package com.example.playergamblingfumino.manager;

import com.example.playergamblingfumino.PlayerGamblingFumino;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final PlayerGamblingFumino plugin;
    private Economy economy;
    private boolean enabled;

    public EconomyManager(PlayerGamblingFumino plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("§c[经济] 未找到Vault插件! 请安装Vault");
            enabled = false;
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
                .getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("§c[经济] 未找到经济提供商! 请安装EssentialsX等经济插件");
            enabled = false;
            return false;
        }

        economy = rsp.getProvider();
        if (economy == null) {
            plugin.getLogger().warning("§c[经济] 经济提供商为null");
            enabled = false;
            return false;
        }

        enabled = economy.isEnabled();
        if (enabled) {
            plugin.getLogger().info("§a[经济] 已对接: " + economy.getName());
        } else {
            plugin.getLogger().warning("§c[经济] 经济系统未启用");
        }
        return enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCurrencyName() {
        if (!enabled || economy == null) return "金币";
        return economy.currencyNamePlural();
    }

    public double getBalance(Player player) {
        if (!enabled || economy == null) return 0;
        return economy.getBalance(player);
    }

    public boolean hasBalance(Player player, double amount) {
        if (!enabled || economy == null) return false;
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!enabled || economy == null) return false;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (response.transactionSuccess()) {
            plugin.getLogger().info("§a[经济] 扣款: " + player.getName() + " -" + amount
                    + " (余额: " + economy.getBalance(player) + ")");
            return true;
        } else {
            plugin.getLogger().warning("§c[经济] 扣款失败: " + player.getName()
                    + " -" + amount + " 原因: " + response.errorMessage);
            return false;
        }
    }

    public boolean deposit(Player player, double amount) {
        if (!enabled || economy == null) return false;
        EconomyResponse response = economy.depositPlayer(player, amount);
        if (response.transactionSuccess()) {
            plugin.getLogger().info("§a[经济] 存入: " + player.getName() + " +" + amount
                    + " (余额: " + economy.getBalance(player) + ")");
            return true;
        } else {
            plugin.getLogger().warning("§c[经济] 存入失败: " + player.getName()
                    + " +" + amount + " 原因: " + response.errorMessage);
            return false;
        }
    }

    public boolean transfer(Player from, Player to, double amount) {
        if (!enabled || economy == null) return false;

        EconomyResponse withdrawResponse = economy.withdrawPlayer(from, amount);
        if (!withdrawResponse.transactionSuccess()) {
            plugin.getLogger().warning("§c[经济] 转账扣款失败: " + from.getName()
                    + " 原因: " + withdrawResponse.errorMessage);
            return false;
        }

        EconomyResponse depositResponse = economy.depositPlayer(to, amount);
        if (!depositResponse.transactionSuccess()) {
            economy.depositPlayer(from, amount); // 回滚
            plugin.getLogger().warning("§c[经济] 转账存入失败, 已回滚: " + to.getName()
                    + " 原因: " + depositResponse.errorMessage);
            return false;
        }

        plugin.getLogger().info("§a[经济] 转账: " + from.getName()
                + " -> " + to.getName() + " " + amount);
        return true;
    }

    public String format(double amount) {
        return String.format("%,.2f", amount);
    }
}
