package com.example.playergamblingfumino.manager;

import com.example.playergamblingfumino.PlayerGamblingFumino;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final PlayerGamblingFumino plugin;
    private final Map<UUID, Double> balances = new HashMap<>();
    private final Map<UUID, String> playerNames = new HashMap<>();
    private final double initialBalance;
    private File dataFile;
    private FileConfiguration dataConfig;

    public PlayerDataManager(PlayerGamblingFumino plugin) {
        this.plugin = plugin;
        this.initialBalance = plugin.getConfig().getDouble("settings.initial-balance", 100.0);
    }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 playerdata.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // 加载所有玩家数据
        if (dataConfig.contains("players")) {
            for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                double balance = dataConfig.getDouble("players." + key + ".balance", initialBalance);
                String name = dataConfig.getString("players." + key + ".name", "Unknown");
                balances.put(uuid, balance);
                playerNames.put(uuid, name);
            }
        }
        plugin.getLogger().info("已加载 " + balances.size() + " 名玩家数据");
    }

    public void save() {
        if (dataConfig == null) return;

        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            String path = "players." + entry.getKey().toString();
            dataConfig.set(path + ".balance", entry.getValue());
            dataConfig.set(path + ".name", playerNames.getOrDefault(entry.getKey(), "Unknown"));
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 playerdata.yml: " + e.getMessage());
        }
    }

    /**
     * 获取玩家余额，如果不存在则创建并返回初始余额
     */
    public double getBalance(Player player) {
        UUID uuid = player.getUniqueId();
        if (!balances.containsKey(uuid)) {
            balances.put(uuid, initialBalance);
            playerNames.put(uuid, player.getName());
        }
        return balances.get(uuid);
    }

    /**
     * 获取玩家余额（通过UUID）
     */
    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0.0);
    }

    /**
     * 设置玩家余额
     */
    public void setBalance(Player player, double amount) {
        balances.put(player.getUniqueId(), amount);
        playerNames.put(player.getUniqueId(), player.getName());
    }

    /**
     * 增加玩家余额
     */
    public void addBalance(Player player, double amount) {
        UUID uuid = player.getUniqueId();
        double current = getBalance(player);
        balances.put(uuid, current + amount);
        playerNames.put(uuid, player.getName());
    }

    /**
     * 减少玩家余额
     */
    public void removeBalance(Player player, double amount) {
        UUID uuid = player.getUniqueId();
        double current = getBalance(player);
        balances.put(uuid, Math.max(0, current - amount));
        playerNames.put(uuid, player.getName());
    }

    /**
     * 检查玩家是否有足够的余额
     */
    public boolean hasEnough(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    /**
     * 获取或创建玩家数据
     */
    public void ensurePlayerExists(Player player) {
        getBalance(player); // 会自动创建
    }
}
