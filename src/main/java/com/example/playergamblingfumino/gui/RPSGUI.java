package com.example.playergamblingfumino.gui;

import com.example.playergamblingfumino.manager.GameManager;
import com.example.playergamblingfumino.model.Choice;
import com.example.playergamblingfumino.model.GamePhase;
import com.example.playergamblingfumino.model.GameSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class RPSGUI {

    private static final int GUI_SIZE = 54;

    private static final int ROCK_SLOT = 20;
    private static final int PAPER_SLOT = 22;
    private static final int SCISSORS_SLOT = 24;
    private static final int CONFIRM_SLOT = 49;
    private static final int INFO_SLOT = 4;
    private static final int OPPONENT_SLOT = 40;

    private static final int YOUR_CHOICE_SLOT = 29;
    private static final int VS_SLOT = 31;
    private static final int OPPONENT_CHOICE_SLOT = 33;
    private static final int COEFFICIENT_SLOT = 13;

    private static final Map<Integer, Material> CHOICE_MATERIALS = Map.of(
            ROCK_SLOT, Material.STONE,
            PAPER_SLOT, Material.PAPER,
            SCISSORS_SLOT, Material.SHEARS
    );

    private static final Map<Integer, String> CHOICE_NAMES = Map.of(
            ROCK_SLOT, "§f石头",
            PAPER_SLOT, "§f布",
            SCISSORS_SLOT, "§f剪刀"
    );

    private static final Map<Choice, String> CHOICE_CHINESE = Map.of(
            Choice.ROCK, "石头",
            Choice.PAPER, "布",
            Choice.SCISSORS, "剪刀"
    );

    public static void open(Player player, GameSession session, GameManager manager) {
        RPSHolder holder = new RPSHolder(session, player.getUniqueId(), manager);
        Inventory inv = Bukkit.createInventory(holder, GUI_SIZE, "§6§l⚔ 猜拳 - 第" + session.getRound() + "轮");

        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, "§7 ", null);
        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, border.clone());
        }

        updateInfoDisplay(inv, session, player.getUniqueId());

        inv.setItem(ROCK_SLOT, createChoiceItem(Material.STONE, "§f石头", false));
        inv.setItem(PAPER_SLOT, createChoiceItem(Material.PAPER, "§f布", false));
        inv.setItem(SCISSORS_SLOT, createChoiceItem(Material.SHEARS, "§f剪刀", false));

        inv.setItem(CONFIRM_SLOT, createConfirmButton(false));

        inv.setItem(OPPONENT_SLOT, createItem(Material.PLAYER_HEAD,
                "§c对手: §e" + session.getOpponentName(player.getUniqueId()),
                new String[]{"§7等待对手选择..."}));

        updateCoefficientDisplay(inv, session, player.getUniqueId());

        player.openInventory(inv);
    }

    public static void refresh(Player player, GameSession session) {
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof RPSHolder)) return;

        Inventory inv = player.getOpenInventory().getTopInventory();
        UUID uuid = player.getUniqueId();

        Choice currentChoice = uuid.equals(session.getPlayerA()) ? session.getChoiceA() : session.getChoiceB();
        boolean confirmed = uuid.equals(session.getPlayerA()) ? session.isChoiceConfirmedA() : session.isChoiceConfirmedB();

        updateChoiceButton(inv, ROCK_SLOT, currentChoice == Choice.ROCK, confirmed);
        updateChoiceButton(inv, PAPER_SLOT, currentChoice == Choice.PAPER, confirmed);
        updateChoiceButton(inv, SCISSORS_SLOT, currentChoice == Choice.SCISSORS, confirmed);

        inv.setItem(CONFIRM_SLOT, createConfirmButton(confirmed));
        updateInfoDisplay(inv, session, uuid);
        updateCoefficientDisplay(inv, session, uuid);

        boolean opponentConfirmed = uuid.equals(session.getPlayerA()) ?
                session.isChoiceConfirmedB() : session.isChoiceConfirmedA();
        String status = opponentConfirmed ? "§a对手已确认" : "§7等待对手选择...";
        inv.setItem(OPPONENT_SLOT, createItem(Material.PLAYER_HEAD,
                "§c对手: §e" + session.getOpponentName(uuid),
                new String[]{status}));
    }

    public static void showResult(Player player, GameSession session, GameManager manager) {
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof RPSHolder)) return;

        Inventory inv = player.getOpenInventory().getTopInventory();
        UUID uuid = player.getUniqueId();

        Choice myChoice = uuid.equals(session.getPlayerA()) ? session.getChoiceA() : session.getChoiceB();
        Choice oppChoice = uuid.equals(session.getPlayerA()) ? session.getChoiceB() : session.getChoiceA();
        Choice.Result result = myChoice != null && oppChoice != null ? myChoice.vs(oppChoice) : null;

        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, "§7 ", null));
        }

        inv.setItem(YOUR_CHOICE_SLOT, createChoiceItem(
                getMaterialForChoice(myChoice),
                "§a你的选择: §e" + getChoiceName(myChoice),
                true));

        inv.setItem(VS_SLOT, createItem(Material.BARRIER, "§c§lVS", null));

        inv.setItem(OPPONENT_CHOICE_SLOT, createChoiceItem(
                getMaterialForChoice(oppChoice),
                "§c对手的选择: §e" + getChoiceName(oppChoice),
                true));

        double step = session.getCurrentStep();
        double coefA = session.getCoefficientA();
        double coefB = session.getCoefficientB();

        String resultStr;
        Material resultMat;
        if (result == Choice.Result.WIN) {
            resultStr = "§a§l你赢了!";
            resultMat = Material.EMERALD;
        } else if (result == Choice.Result.LOSE) {
            resultStr = "§c§l你输了...";
            resultMat = Material.REDSTONE;
        } else {
            resultStr = "§e§l平局! (步累积)";
            resultMat = Material.GRAY_DYE;
        }

        String[] resultLore = {
                "§7当前步长: §b" + String.format("%.3f", step),
                "§7甲方系数: §b" + String.format("%.3f", coefA),
                "§7乙方系数: §b" + String.format("%.3f", coefB),
                "§7赌注: " + BetSetupGUI.formatTier(session.getWagerAmount()) + " §7| 奖池: " + BetSetupGUI.formatTier(session.getWagerAmount() * 2)
        };

        inv.setItem(INFO_SLOT, createItem(resultMat, resultStr, resultLore));
        updateCoefficientDisplay(inv, session, uuid);
    }

    public static void handleClick(Player player, int slot, GameManager manager) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof RPSHolder holder)) return;
        if (!holder.getPlayerUUID().equals(player.getUniqueId())) return;

        GameSession session = holder.getSession();
        if (session.getPhase() != GamePhase.RPS_CHOICE) return;

        UUID uuid = player.getUniqueId();
        boolean confirmed = uuid.equals(session.getPlayerA()) ?
                session.isChoiceConfirmedA() : session.isChoiceConfirmedB();

        if (confirmed) {
            player.sendMessage("§c你已确认选择, 等待对手...");
            return;
        }

        if (slot == ROCK_SLOT) {
            manager.handleChoiceSelect(player, Choice.ROCK);
        } else if (slot == PAPER_SLOT) {
            manager.handleChoiceSelect(player, Choice.PAPER);
        } else if (slot == SCISSORS_SLOT) {
            manager.handleChoiceSelect(player, Choice.SCISSORS);
        } else if (slot == CONFIRM_SLOT) {
            manager.handleChoiceConfirm(player);
        }
    }

    // === 内部方法 ===

    private static void updateInfoDisplay(Inventory inv, GameSession session, UUID uuid) {
        int round = session.getRound();
        int total = session.getTotalRounds();
        double step = session.getCurrentStep();
        double normalStep = session.getStepForRound(round);

        String stepInfo = (Math.abs(step - normalStep) > 0.001)
                ? String.format("%.3f §7(含累积 §b%.3f§7)", step, step - normalStep)
                : String.format("%.3f", step);

        inv.setItem(INFO_SLOT, createItem(Material.KNOWLEDGE_BOOK,
                "§6§l第 " + round + "/" + total + " 轮",
                new String[]{
                        "§7当前步长: §b" + stepInfo,
                        "§7甲方系数: §b" + String.format("%.3f", session.getCoefficientA()),
                        "§7乙方系数: §b" + String.format("%.3f", session.getCoefficientB()),
                        "§7赌注: " + BetSetupGUI.formatTier(session.getWagerAmount()) + " §7| 奖池: " + BetSetupGUI.formatTier(session.getWagerAmount() * 2)
                }));
    }

    private static void updateCoefficientDisplay(Inventory inv, GameSession session, UUID uuid) {
        double coefA = session.getCoefficientA();
        double coefB = session.getCoefficientB();
        double payoutA = session.calculatePayoutA();
        double payoutB = session.calculatePayoutB();

        UUID playerA = session.getPlayerA();
        String myCoeffStr = uuid.equals(playerA)
                ? String.format("%.3f", coefA) + " §7(可获: " + BetSetupGUI.formatTier(payoutA) + "§7)"
                : String.format("%.3f", coefB) + " §7(可获: " + BetSetupGUI.formatTier(payoutB) + "§7)";

        inv.setItem(COEFFICIENT_SLOT, createItem(Material.GOLD_INGOT,
                "§6§l你的系数: §e" + myCoeffStr,
                new String[]{
                        "§7甲方: §b" + String.format("%.3f", coefA) + " §7→ " + BetSetupGUI.formatTier(payoutA),
                        "§7乙方: §b" + String.format("%.3f", coefB) + " §7→ " + BetSetupGUI.formatTier(payoutB),
                        "§7奖池: " + BetSetupGUI.formatTier(session.getWagerAmount() * 2)
                }));
    }

    private static void updateChoiceButton(Inventory inv, int slot, boolean selected, boolean confirmed) {
        Material originalMat = CHOICE_MATERIALS.get(slot);
        String name = CHOICE_NAMES.get(slot);
        Material mat;

        if (confirmed) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
        } else if (selected) {
            mat = Material.LIME_STAINED_GLASS_PANE;
        } else {
            mat = originalMat;
        }

        String[] lore;
        if (confirmed) {
            lore = new String[]{"§7已锁定"};
        } else if (selected) {
            lore = new String[]{"§a已选择", "§7点击确认锁定"};
        } else {
            lore = new String[]{"§7点击选择"};
        }

        inv.setItem(slot, createItem(mat, name, lore));
    }

    private static ItemStack createChoiceItem(Material material, String name, boolean glowing) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createConfirmButton(boolean confirmed) {
        if (confirmed) {
            return createItem(Material.LIME_STAINED_GLASS_PANE,
                    "§a§l✔ 已确认!",
                    new String[]{"§7等待对手确认..."});
        } else {
            return createItem(Material.EMERALD_BLOCK,
                    "§a§l★ 点击确认出拳",
                    new String[]{"§7确认后将无法修改", "§7双方确认后揭晓结果"});
        }
    }

    private static Material getMaterialForChoice(Choice choice) {
        if (choice == null) return Material.BARRIER;
        return switch (choice) {
            case ROCK -> Material.STONE;
            case PAPER -> Material.PAPER;
            case SCISSORS -> Material.SHEARS;
        };
    }

    private static String getChoiceName(Choice choice) {
        return CHOICE_CHINESE.getOrDefault(choice, "未选择");
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

    public static class RPSHolder implements InventoryHolder {
        private final GameSession session;
        private final UUID playerUUID;
        private final GameManager manager;

        public RPSHolder(GameSession session, UUID playerUUID, GameManager manager) {
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
