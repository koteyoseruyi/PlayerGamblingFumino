package com.example.playergamblingfumino.gui;

import com.example.playergamblingfumino.manager.GameManager;
import com.example.playergamblingfumino.model.GameSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.UUID;

public class BetSetupGUI {

    private static final int GUI_SIZE = 54;

    // 信息槽位
    private static final int INFO_SLOT = 4;

    // 回合按钮（第2行）
    private static final int ROUND_5_SLOT = 11;
    private static final int ROUND_7_SLOT = 13;
    private static final int ROUND_9_SLOT = 15;
    private static final int[] ROUND_SLOTS = {ROUND_5_SLOT, ROUND_7_SLOT, ROUND_9_SLOT};

    // 档位按钮（第3行）
    private static final int TIER_10K_SLOT = 20;
    private static final int TIER_25K_SLOT = 21;
    private static final int TIER_50K_SLOT = 22;
    private static final int TIER_250K_SLOT = 23;
    private static final int TIER_500K_SLOT = 24;
    private static final int[] TIER_SLOTS = {TIER_10K_SLOT, TIER_25K_SLOT, TIER_50K_SLOT, TIER_250K_SLOT, TIER_500K_SLOT};

    // 信息显示
    private static final int MY_SELECTION_SLOT = 29;
    private static final int OPPONENT_SELECTION_SLOT = 33;
    private static final int STATUS_SLOT = 40;
    private static final int CONFIRM_SLOT = 49;

    public static void open(Player player, GameSession session, GameManager manager) {
        BetHolder holder = new BetHolder(session, player.getUniqueId(), manager);
        Inventory inv = Bukkit.createInventory(holder, GUI_SIZE, "§6§l⚔ 对赌设定");

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, "§7 ", null);
        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, border.clone());
        }

        UUID uuid = player.getUniqueId();

        inv.setItem(INFO_SLOT, createItem(Material.KNOWLEDGE_BOOK,
                "§6§l★ 对赌设定",
                new String[]{
                        "§7对手: §e" + session.getOpponentName(uuid),
                        "§7选择回合数与金额档位",
                        "§7双方选择一致后方可确认"
                }));

        // 回合按钮
        for (int slot : ROUND_SLOTS) {
            int rounds = getRoundsForSlot(slot);
            boolean selected = session.getSelectedRounds(uuid) != null && session.getSelectedRounds(uuid) == rounds;
            boolean oppSelected = session.getSelectedRounds(session.getOpponent(uuid)) != null &&
                    session.getSelectedRounds(session.getOpponent(uuid)) == rounds;
            inv.setItem(slot, createRoundButton(rounds, selected, oppSelected));
        }

        // 档位按钮
        for (int slot : TIER_SLOTS) {
            int tier = getTierForSlot(slot);
            boolean selected = session.getSelectedTier(uuid) != null && session.getSelectedTier(uuid) == tier;
            boolean oppSelected = session.getSelectedTier(session.getOpponent(uuid)) != null &&
                    session.getSelectedTier(session.getOpponent(uuid)) == tier;
            inv.setItem(slot, createTierButton(tier, selected, oppSelected));
        }

        updateMySelection(inv, session, uuid);
        updateOpponentSelection(inv, session, uuid);
        updateStatus(inv, session, uuid, manager); // ✅ 传入 manager

        inv.setItem(CONFIRM_SLOT, createConfirmButton(false));

        player.openInventory(inv);
    }

    public static void refresh(Player player, GameSession session) {
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof BetHolder holder)) return;

        Inventory inv = player.getOpenInventory().getTopInventory();
        UUID uuid = player.getUniqueId();
        UUID oppUuid = session.getOpponent(uuid);
        GameManager manager = holder.getManager(); // ✅ 从 holder 获取 manager

        // 刷新回合按钮
        for (int slot : ROUND_SLOTS) {
            int rounds = getRoundsForSlot(slot);
            boolean selected = session.getSelectedRounds(uuid) != null && session.getSelectedRounds(uuid) == rounds;
            boolean oppSelected = session.getSelectedRounds(oppUuid) != null &&
                    session.getSelectedRounds(oppUuid) == rounds;
            inv.setItem(slot, createRoundButton(rounds, selected, oppSelected));
        }

        // 刷新档位按钮
        for (int slot : TIER_SLOTS) {
            int tier = getTierForSlot(slot);
            boolean selected = session.getSelectedTier(uuid) != null && session.getSelectedTier(uuid) == tier;
            boolean oppSelected = session.getSelectedTier(oppUuid) != null &&
                    session.getSelectedTier(oppUuid) == tier;
            inv.setItem(slot, createTierButton(tier, selected, oppSelected));
        }

        updateMySelection(inv, session, uuid);
        updateOpponentSelection(inv, session, uuid);
        updateStatus(inv, session, uuid, manager); // ✅ 传入 manager

        boolean confirmed = session.isConfirmed(uuid);
        inv.setItem(CONFIRM_SLOT, createConfirmButton(confirmed));
    }

    // ========== 按钮创建 ==========

    private static ItemStack createRoundButton(int rounds, boolean selected, boolean opponentSelected) {
        Material mat;
        String name;
        String[] lore;

        if (selected) {
            mat = Material.LIME_STAINED_GLASS_PANE;
            name = "§a§l✔ " + rounds + " 回合 (已选)";
            lore = new String[]{"§7点击取消选择"};
        } else if (opponentSelected) {
            mat = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            name = "§b§l" + rounds + " 回合 (对手已选)";
            lore = new String[]{"§7对手选择了此回合数", "§7点击选择相同回合数"};
        } else {
            mat = Material.GOLD_BLOCK;
            name = "§6§l" + rounds + " 回合";
            lore = new String[]{"§7点击选择", "§7共 " + rounds + " 轮猜拳"};
        }

        return createItem(mat, name, lore);
    }

    private static ItemStack createTierButton(int tier, boolean selected, boolean opponentSelected) {
        Material mat;
        String name;
        String[] lore;

        if (selected) {
            mat = Material.LIME_STAINED_GLASS_PANE;
            name = "§a§l✔ " + formatTier(tier) + " (已选)";
            lore = new String[]{"§7点击取消选择"};
        } else if (opponentSelected) {
            mat = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            name = "§b§l" + formatTier(tier) + " (对手已选)";
            lore = new String[]{"§7对手选择了此档位", "§7点击选择相同档位"};
        } else {
            mat = Material.EMERALD_BLOCK;
            name = "§a§l" + formatTier(tier);
            lore = new String[]{"§7押注: " + formatTier(tier), "§7奖池: " + formatTier(tier * 2)};
        }

        return createItem(mat, name, lore);
    }

    private static ItemStack createConfirmButton(boolean confirmed) {
        if (confirmed) {
            return createItem(Material.LIME_STAINED_GLASS_PANE,
                    "§a§l✔ 已确认!",
                    new String[]{"§7等待对手确认..."});
        } else {
            return createItem(Material.EMERALD_BLOCK,
                    "§a§l★ 点击确认开始",
                    new String[]{
                            "§7双方回合和档位一致后方可确认",
                            "§7确认后将扣除押注金额"
                    });
        }
    }

    // ========== 信息显示更新 ==========

    private static void updateMySelection(Inventory inv, GameSession session, UUID uuid) {
        Integer rounds = session.getSelectedRounds(uuid);
        Integer tier = session.getSelectedTier(uuid);
        boolean confirmed = session.isConfirmed(uuid);

        String roundStr = rounds != null ? "§e" + rounds + " 回合" : "§c未选择";
        String tierStr = tier != null ? "§e" + formatTier(tier) : "§c未选择";
        String confirmStr = confirmed ? "§a§l✔ 已确认" : "§c未确认";

        inv.setItem(MY_SELECTION_SLOT, createItem(Material.PLAYER_HEAD,
                "§a§l你的选择",
                new String[]{
                        "§7回合: " + roundStr,
                        "§7金额: " + tierStr,
                        "§7状态: " + confirmStr
                }));
    }

    private static void updateOpponentSelection(Inventory inv, GameSession session, UUID uuid) {
        UUID opp = session.getOpponent(uuid);
        Integer rounds = session.getSelectedRounds(opp);
        Integer tier = session.getSelectedTier(opp);
        boolean confirmed = session.isConfirmed(opp);

        String roundStr = rounds != null ? "§e" + rounds + " 回合" : "§c未选择";
        String tierStr = tier != null ? "§e" + formatTier(tier) : "§c未选择";
        String confirmStr = confirmed ? "§a§l✔ 已确认" : "§c未确认";

        inv.setItem(OPPONENT_SELECTION_SLOT, createItem(Material.PLAYER_HEAD,
                "§c§l对手选择 (" + session.getOpponentName(uuid) + ")",
                new String[]{
                        "§7回合: " + roundStr,
                        "§7金额: " + tierStr,
                        "§7状态: " + confirmStr
                }));
    }

    /** ✅ 修复：传入 GameManager 参数 */
    private static void updateStatus(Inventory inv, GameSession session, UUID uuid, GameManager manager) {
        boolean roundsSelected = session.isBothRoundsSelected();
        boolean tiersSelected = session.isBothTiersSelected();
        boolean roundsMatched = session.isRoundsMatched();
        boolean tiersMatched = session.isTiersMatched();

        String status;
        Material mat;

        if (!roundsSelected || !tiersSelected) {
            status = "§e⌛ 请选择回合数和金额档位";
            mat = Material.YELLOW_STAINED_GLASS_PANE;
        } else if (!roundsMatched) {
            status = "§c✘ 回合数不一致! 请选择相同回合数";
            mat = Material.RED_STAINED_GLASS_PANE;
        } else if (!tiersMatched) {
            status = "§c✘ 金额档位不一致! 请选择相同档位";
            mat = Material.RED_STAINED_GLASS_PANE;
        } else {
            Integer myTier = session.getSelectedTier(uuid);
            if (myTier != null) {
                Player player = Bukkit.getPlayer(uuid);
                Player opponent = Bukkit.getPlayer(session.getOpponent(uuid));
                double required = myTier * 2;

                if (player == null || opponent == null) {
                    status = "§c玩家离线";
                    mat = Material.RED_STAINED_GLASS_PANE;
                } else {
                    // ✅ 使用传入的 manager 获取经济信息
                    double myBalance = manager.getEconomyManager().getBalance(player);
                    double oppBalance = manager.getEconomyManager().getBalance(opponent);

                    if (myBalance < required) {
                        status = "§c✘ 你的余额不足! 需要: " + formatTier(required);
                        mat = Material.RED_STAINED_GLASS_PANE;
                    } else if (oppBalance < required) {
                        status = "§c✘ 对手余额不足!";
                        mat = Material.RED_STAINED_GLASS_PANE;
                    } else {
                        status = "§a✔ 所有条件满足! 点击确认开始对局";
                        mat = Material.LIME_STAINED_GLASS_PANE;
                    }
                }
            } else {
                status = "§e⌛ 请选择金额档位";
                mat = Material.YELLOW_STAINED_GLASS_PANE;
            }
        }

        inv.setItem(STATUS_SLOT, createItem(mat, "§6§l状态", new String[]{status}));
    }

    // ========== 点击处理 ==========

    public static void handleClick(Player player, int slot, GameManager manager) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof BetHolder holder)) return;
        if (!holder.getPlayerUUID().equals(player.getUniqueId())) return;

        GameSession session = holder.getSession();
        UUID uuid = player.getUniqueId();

        if (session.isConfirmed(uuid)) {
            player.sendMessage("§c你已确认, 等待对手...");
            return;
        }

        // 回合按钮
        for (int s : ROUND_SLOTS) {
            if (slot == s) {
                int rounds = getRoundsForSlot(s);
                Integer current = session.getSelectedRounds(uuid);
                if (current != null && current == rounds) {
                    manager.handleRoundSelect(player, null);
                } else {
                    manager.handleRoundSelect(player, rounds);
                }
                return;
            }
        }

        // 档位按钮
        for (int s : TIER_SLOTS) {
            if (slot == s) {
                int tier = getTierForSlot(s);
                Integer current = session.getSelectedTier(uuid);
                if (current != null && current == tier) {
                    manager.handleTierSelect(player, null);
                } else {
                    manager.handleTierSelect(player, tier);
                }
                return;
            }
        }

        // 确认按钮
        if (slot == CONFIRM_SLOT) {
            manager.handleBetConfirm(player);
        }
    }

    // ========== 工具方法 ==========

    private static int getRoundsForSlot(int slot) {
        if (slot == ROUND_5_SLOT) return 5;
        if (slot == ROUND_7_SLOT) return 7;
        return 9;
    }

    private static int getTierForSlot(int slot) {
        if (slot == TIER_10K_SLOT) return 10000;
        if (slot == TIER_25K_SLOT) return 25000;
        if (slot == TIER_50K_SLOT) return 50000;
        if (slot == TIER_250K_SLOT) return 250000;
        return 500000;
    }

    public static String formatTier(double amount) {
        return "§6" + String.format("%,.0f", amount);
    }

    private static ItemStack createItem(Material material, String name, String[] lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static class BetHolder implements InventoryHolder {
        private final GameSession session;
        private final UUID playerUUID;
        private final GameManager manager;

        public BetHolder(GameSession session, UUID playerUUID, GameManager manager) {
            this.session = session;
            this.playerUUID = playerUUID;
            this.manager = manager;
        }

        public GameSession getSession() { return session; }
        public UUID getPlayerUUID() { return playerUUID; }
        public GameManager getManager() { return manager; }

        @Override
        public Inventory getInventory() { return null; }
    }
}
