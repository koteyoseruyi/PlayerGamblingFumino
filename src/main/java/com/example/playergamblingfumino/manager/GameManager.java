package com.example.playergamblingfumino.manager;

import com.example.playergamblingfumino.PlayerGamblingFumino;
import com.example.playergamblingfumino.gui.BetSetupGUI;
import com.example.playergamblingfumino.gui.RPSGUI;
import com.example.playergamblingfumino.model.Choice;
import com.example.playergamblingfumino.model.GamePhase;
import com.example.playergamblingfumino.model.GameSession;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GameManager {

    private final PlayerGamblingFumino plugin;
    private final EconomyManager economyManager;
    private final Map<UUID, GameSession> sessions = new HashMap<>();
    private final Set<UUID> playersInGUI = new HashSet<>();
    private final Set<UUID> transitioningPlayers = new HashSet<>();

    private double baseStep;
    private double gradientStep;
    private int resultDisplayTime;

    public GameManager(PlayerGamblingFumino plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        reloadConfig();
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public void reloadConfig() {
        baseStep = plugin.getConfig().getDouble("settings.base-step", 0.2);
        gradientStep = plugin.getConfig().getDouble("settings.gradient-step", 0.04);
        resultDisplayTime = plugin.getConfig().getInt("settings.result-display-time", 5);
    }

    // ========== 过渡标记 ==========

    public void startTransition(Player player) {
        transitioningPlayers.add(player.getUniqueId());
    }

    public void endTransition(Player player) {
        transitioningPlayers.remove(player.getUniqueId());
    }

    public boolean isTransitioning(Player player) {
        return transitioningPlayers.contains(player.getUniqueId());
    }

    public void saveAll() {}

    // ========== 游戏创建 ==========

    public boolean createGame(Player playerA, Player playerB) {
        if (isPlayerInGame(playerA) || isPlayerInGame(playerB)) {
            playerA.sendMessage("§c你或对方已在游戏中");
            return false;
        }

        if (!economyManager.isEnabled()) {
            playerA.sendMessage("§c§l❌ 经济系统未启用!");
            playerA.sendMessage("§7请确保安装了Vault和经济插件 (如EssentialsX)");
            return false;
        }

        double minRequired = 20000;
        if (!economyManager.hasBalance(playerA, minRequired)) {
            playerA.sendMessage("§c你的余额不足, 至少需要 §e" + economyManager.format(minRequired));
            return false;
        }
        if (!economyManager.hasBalance(playerB, minRequired)) {
            playerA.sendMessage("§c对方余额不足");
            return false;
        }

        GameSession session = new GameSession(playerA, playerB, baseStep, gradientStep);

        sessions.put(playerA.getUniqueId(), session);
        sessions.put(playerB.getUniqueId(), session);

        playersInGUI.add(playerA.getUniqueId());
        playersInGUI.add(playerB.getUniqueId());

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            BetSetupGUI.open(playerA, session, this);
            BetSetupGUI.open(playerB, session, this);
        });

        playerA.sendMessage("§a§l✦ 已与 §e" + playerB.getName() + " §a开始对赌!");
        playerB.sendMessage("§a§l✦ 已与 §e" + playerA.getName() + " §a开始对赌!");
        playerA.sendMessage("§6请在界面中选择 §e回合数 §6和 §e金额档位§6, 双方须一致");
        playerB.sendMessage("§6请在界面中选择 §e回合数 §6和 §e金额档位§6, 双方须一致");

        return true;
    }

    // ========== 回合选择 ==========

    public void handleRoundSelect(Player player, Integer rounds) {
        GameSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getPhase() != GamePhase.BET_SETUP) return;

        UUID uuid = player.getUniqueId();
        if (session.isConfirmed(uuid)) {
            player.sendMessage("§c你已确认, 无法修改");
            return;
        }

        session.setSelectedRounds(uuid, rounds);

        BetSetupGUI.refresh(player, session);
        Player opponent = plugin.getServer().getPlayer(session.getOpponent(uuid));
        if (opponent != null) BetSetupGUI.refresh(opponent, session);

        player.sendMessage(rounds != null ? "§a你选择了 §e" + rounds + " 回合" : "§7已取消回合选择");
    }

    // ========== 档位选择 ==========

    public void handleTierSelect(Player player, Integer tier) {
        GameSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getPhase() != GamePhase.BET_SETUP) return;

        UUID uuid = player.getUniqueId();
        if (session.isConfirmed(uuid)) {
            player.sendMessage("§c你已确认, 无法修改");
            return;
        }

        if (tier != null) {
            double required = tier * 2.0;
            if (!economyManager.hasBalance(player, required)) {
                player.sendMessage("§c❌ 余额不足! 需要 §e" + economyManager.format(required)
                        + " §7你有 §e" + economyManager.format(economyManager.getBalance(player)));
                return;
            }
        }

        session.setSelectedTier(uuid, tier);

        BetSetupGUI.refresh(player, session);
        Player opponent = plugin.getServer().getPlayer(session.getOpponent(uuid));
        if (opponent != null) BetSetupGUI.refresh(opponent, session);

        player.sendMessage(tier != null ? "§a你选择了 §e" + tier + " " + economyManager.getCurrencyName()
                : "§7已取消档位选择");
    }

    // ========== 确认押注（扣款发生在这里） ==========

    public void handleBetConfirm(Player player) {
        GameSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getPhase() != GamePhase.BET_SETUP) return;

        UUID uuid = player.getUniqueId();

        if (session.isConfirmed(uuid)) {
            player.sendMessage("§c你已经确认过了");
            return;
        }

        Integer myRounds = session.getSelectedRounds(uuid);
        Integer myTier = session.getSelectedTier(uuid);

        if (myRounds == null || myTier == null) {
            player.sendMessage("§c请先选择回合数和金额档位!");
            return;
        }

        if (!session.isRoundsMatched()) {
            player.sendMessage("§c回合数不一致! 对手选了 §e"
                    + session.getSelectedRounds(session.getOpponent(uuid)) + " 回合");
            return;
        }

        if (!session.isTiersMatched()) {
            player.sendMessage("§c档位不一致! 对手选了 §e"
                    + session.getSelectedTier(session.getOpponent(uuid)));
            return;
        }

        double required = myTier * 2.0;
        if (!economyManager.hasBalance(player, required)) {
            player.sendMessage("§c❌ 余额不足! 需要 §e" + economyManager.format(required));
            return;
        }

        Player opponent = plugin.getServer().getPlayer(session.getOpponent(uuid));
        if (opponent == null || !opponent.isOnline()) {
            player.sendMessage("§c对手已离线");
            return;
        }

        if (!economyManager.hasBalance(opponent, required)) {
            player.sendMessage("§c对手余额不足");
            return;
        }

        // 标记确认
        session.setConfirmed(uuid, true);
        player.sendMessage("§a✔ 已确认! §e" + myRounds + "回合 §e" + myTier + " " + economyManager.getCurrencyName());
        player.sendMessage("§7等待对手确认...");

        BetSetupGUI.refresh(player, session);
        if (opponent != null) {
            BetSetupGUI.refresh(opponent, session);
            opponent.sendMessage("§e" + player.getName() + " §a已确认!");
        }

        // 双方都确认后执行扣款
        if (session.isBothConfirmed()) {
            executeWagerWithdrawal(session);
        }
    }

    /**
     * 执行押注扣款（双方各扣wager，奖池 = 2 * wager）
     */
    private void executeWagerWithdrawal(GameSession session) {
        session.finalizeSettings();
        double wager = session.getWagerAmount();

        Player playerA = plugin.getServer().getPlayer(session.getPlayerA());
        Player playerB = plugin.getServer().getPlayer(session.getPlayerB());

        if (playerA == null || playerB == null) {
            String msg = "§c❌ 玩家离线, 游戏取消";
            if (playerA != null) playerA.sendMessage(msg);
            if (playerB != null) playerB.sendMessage(msg);
            cleanupSession(session);
            return;
        }

        plugin.getLogger().info("§6[对赌] 开始扣款: " + playerA.getName()
                + " & " + playerB.getName() + " 各 " + wager);

        // 扣A款
        boolean successA = economyManager.withdraw(playerA, wager);
        if (!successA) {
            playerA.sendMessage("§c❌ 扣款失败! 余额不足?");
            playerB.sendMessage("§c❌ " + playerA.getName() + " 扣款失败, 游戏取消");
            cleanupSession(session);
            return;
        }

        // 扣B款
        boolean successB = economyManager.withdraw(playerB, wager);
        if (!successB) {
            economyManager.deposit(playerA, wager); // 退A的钱
            playerB.sendMessage("§c❌ 扣款失败! 余额不足?");
            playerA.sendMessage("§c❌ " + playerB.getName() + " 扣款失败, 已退还 §e" + economyManager.format(wager));
            cleanupSession(session);
            return;
        }

        // 扣款成功!
        plugin.getLogger().info("§a[对赌] 扣款成功! 奖池: " + (wager * 2));
        playerA.sendMessage("§a§l✦ 已扣除 §e" + economyManager.format(wager) + " §a奖池: §e" + economyManager.format(wager * 2));
        playerB.sendMessage("§a§l✦ 已扣除 §e" + economyManager.format(wager) + " §a奖池: §e" + economyManager.format(wager * 2));

        startRPSPhase(session);
    }

    // ========== 猜拳阶段 ==========

    private void startRPSPhase(GameSession session) {
        session.setPhase(GamePhase.RPS_CHOICE);

        Player playerA = plugin.getServer().getPlayer(session.getPlayerA());
        Player playerB = plugin.getServer().getPlayer(session.getPlayerB());

        if (playerA == null || playerB == null) {
            refundWager(session);
            return;
        }

        startTransition(playerA);
        startTransition(playerB);
        playersInGUI.remove(session.getPlayerA());
        playersInGUI.remove(session.getPlayerB());

        playerA.closeInventory();
        playerB.closeInventory();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            playersInGUI.add(session.getPlayerA());
            playersInGUI.add(session.getPlayerB());

            RPSGUI.open(playerA, session, this);
            RPSGUI.open(playerB, session, this);

            endTransition(playerA);
            endTransition(playerB);

            String info = "\n§6§l══════ 第 " + session.getRound() + "/" + session.getTotalRounds() + " 轮 ══════\n" +
                    "§7甲方系数: §b" + String.format("%.3f", session.getCoefficientA()) +
                    " §7| §7乙方系数: §b" + String.format("%.3f", session.getCoefficientB()) + "\n" +
                    "§7赌注: §e" + economyManager.format(session.getWagerAmount()) +
                    " §7| §7奖池: §e" + economyManager.format(session.getWagerAmount() * 2) + "\n" +
                    "§6§l═══════════════════════════";

            playerA.sendMessage(info);
            playerB.sendMessage(info);
        });
    }

    // ========== 猜拳操作 ==========

    public void handleChoiceSelect(Player player, Choice choice) {
        GameSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getPhase() != GamePhase.RPS_CHOICE) return;

        UUID uuid = player.getUniqueId();
        if (uuid.equals(session.getPlayerA())) {
            session.setChoiceA(choice);
        } else {
            session.setChoiceB(choice);
        }

        RPSGUI.refresh(player, session);
        player.sendMessage("§a你选择了 §e" + getChineseChoice(choice) + "§a, 点击确认锁定");
    }

    public void handleChoiceConfirm(Player player) {
        GameSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getPhase() != GamePhase.RPS_CHOICE) return;

        UUID uuid = player.getUniqueId();
        Choice choice = uuid.equals(session.getPlayerA()) ? session.getChoiceA() : session.getChoiceB();

        if (choice == null) {
            player.sendMessage("§c请先选择石头、布或剪刀!");
            return;
        }

        if (uuid.equals(session.getPlayerA())) {
            session.setChoiceConfirmedA(true);
        } else {
            session.setChoiceConfirmedB(true);
        }

        player.sendMessage("§a✔ 选择已锁定! 等待对手...");
        RPSGUI.refresh(player, session);

        if (session.isBothChoicesConfirmed()) {
            processRound(session);
        }
    }

    // ========== 处理一轮结果 ==========

    private void processRound(GameSession session) {
        session.setPhase(GamePhase.RESULT);

        boolean ended = session.executeRound();
        Player playerA = plugin.getServer().getPlayer(session.getPlayerA());
        Player playerB = plugin.getServer().getPlayer(session.getPlayerB());

        if (playerA == null || playerB == null) {
            refundWager(session);
            return;
        }

        Choice choiceA = session.getChoiceA();
        Choice choiceB = session.getChoiceB();
        double coefA = session.getCoefficientA();
        double coefB = session.getCoefficientB();
        double step = session.getCurrentStep();
        int prevRound = session.getRound() - 1;

        RPSGUI.showResult(playerA, session, this);
        RPSGUI.showResult(playerB, session, this);

        Choice.Result result = choiceA.vs(choiceB);
        String stepStr = result == Choice.Result.DRAW
                ? "§e平局! §7步长 §b" + String.format("%.3f", step) + " §7累积至下回合"
                : "§7步长: §b" + String.format("%.3f", step);

        String resultMsg = "\n§6§l══════ 第 " + prevRound + " 轮结果 ══════\n" +
                "§e" + playerA.getName() + " §7: §f" + getChineseChoice(choiceA) +
                "  §cVS  §e" + playerB.getName() + " §7: §f" + getChineseChoice(choiceB) + "\n" +
                stepStr + "\n" +
                "§7甲方系数: §b" + String.format("%.3f", coefA) +
                " §7| §7乙方系数: §b" + String.format("%.3f", coefB) + "\n" +
                "§6§l═══════════════════════════";

        playerA.sendMessage(resultMsg);
        playerB.sendMessage(resultMsg);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (session.shouldEnd()) {
                    executePayout(session);
                } else {
                    session.resetChoices();
                    session.setPhase(GamePhase.RPS_CHOICE);

                    startTransition(playerA);
                    startTransition(playerB);
                    playersInGUI.remove(session.getPlayerA());
                    playersInGUI.remove(session.getPlayerB());

                    playerA.closeInventory();
                    playerB.closeInventory();

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        playersInGUI.add(session.getPlayerA());
                        playersInGUI.add(session.getPlayerB());

                        RPSGUI.open(playerA, session, GameManager.this);
                        RPSGUI.open(playerB, session, GameManager.this);

                        endTransition(playerA);
                        endTransition(playerB);

                        playerA.sendMessage("\n§a第 §e" + session.getRound() + " §a轮开始! 请出拳");
                        playerB.sendMessage("\n§a第 §e" + session.getRound() + " §a轮开始! 请出拳");
                    });
                }
            }
        }.runTaskLater(plugin, resultDisplayTime * 20L);
    }

    // ========== 分发奖金 ==========

    /**
     * 执行奖金分发
     * 甲方得 = 2M * (coefA / 2)
     * 乙方得 = 2M * (1 - coefA / 2)
     */
    private void executePayout(GameSession session) {
        session.setPhase(GamePhase.FINISHED);

        Player playerA = plugin.getServer().getPlayer(session.getPlayerA());
        Player playerB = plugin.getServer().getPlayer(session.getPlayerB());

        playersInGUI.remove(session.getPlayerA());
        playersInGUI.remove(session.getPlayerB());

        double coefA = session.getCoefficientA();
        double coefB = session.getCoefficientB();
        double payoutA = session.calculatePayoutA();
        double payoutB = session.calculatePayoutB();
        double wager = session.getWagerAmount();

        plugin.getLogger().info("§6[对赌] 结算: "
                + (playerA != null ? playerA.getName() : "?") + " coef=" + String.format("%.3f", coefA) + " 获得=" + payoutA
                + " | " + (playerB != null ? playerB.getName() : "?") + " coef=" + String.format("%.3f", coefB) + " 获得=" + payoutB);

        if (playerA != null) playerA.closeInventory();
        if (playerB != null) playerB.closeInventory();

        boolean successA = playerA != null && economyManager.deposit(playerA, payoutA);
        boolean successB = playerB != null && economyManager.deposit(playerB, payoutB);

        if (!successA || !successB) {
            plugin.getLogger().severe("§c§l[对赌] 发放异常! 启动紧急退款...");
            if (playerA != null && !successA) {
                economyManager.deposit(playerA, wager);
                playerA.sendMessage("§c❌ 发放失败, 已退还本金 §e" + economyManager.format(wager));
            }
            if (playerB != null && !successB) {
                economyManager.deposit(playerB, wager);
                playerB.sendMessage("§c❌ 发放失败, 已退还本金 §e" + economyManager.format(wager));
            }
        } else {
            if (playerA != null) {
                playerA.sendMessage("\n§6§l══════ 对局结束 ══════");
                playerA.sendMessage("§7甲方系数: §b" + String.format("%.3f", coefA));
                playerA.sendMessage("§7乙方系数: §b" + String.format("%.3f", coefB));
                playerA.sendMessage("§a你获得: §e" + economyManager.format(payoutA));
                playerA.sendMessage("§7对手获得: §e" + economyManager.format(payoutB));
                playerA.sendMessage("§6§l══════════════════════");
            }
            if (playerB != null) {
                playerB.sendMessage("\n§6§l══════ 对局结束 ══════");
                playerB.sendMessage("§7乙方系数: §b" + String.format("%.3f", coefB));
                playerB.sendMessage("§7甲方系数: §b" + String.format("%.3f", coefA));
                playerB.sendMessage("§a你获得: §e" + economyManager.format(payoutB));
                playerB.sendMessage("§7对手获得: §e" + economyManager.format(payoutA));
                playerB.sendMessage("§6§l══════════════════════");
            }
        }

        cleanupSession(session);
    }

    /**
     * 退款（用于异常情况）
     */
    private void refundWager(GameSession session) {
        Player playerA = plugin.getServer().getPlayer(session.getPlayerA());
        Player playerB = plugin.getServer().getPlayer(session.getPlayerB());
        double wager = session.getWagerAmount();

        plugin.getLogger().warning("§c[对赌] 异常退款: "
                + (playerA != null ? playerA.getName() : "?")
                + " & " + (playerB != null ? playerB.getName() : "?"));

        if (playerA != null && wager > 0) {
            economyManager.deposit(playerA, wager);
            playerA.sendMessage("§c❌ 游戏异常结束, 已退还 §e" + economyManager.format(wager));
        }
        if (playerB != null && wager > 0) {
            economyManager.deposit(playerB, wager);
            playerB.sendMessage("§c❌ 游戏异常结束, 已退还 §e" + economyManager.format(wager));
        }

        cleanupSession(session);
    }

    /**
     * 清理会话
     */
    private void cleanupSession(GameSession session) {
        playersInGUI.remove(session.getPlayerA());
        playersInGUI.remove(session.getPlayerB());
        sessions.remove(session.getPlayerA());
        sessions.remove(session.getPlayerB());
    }

    // ========== 玩家退出处理 ==========

    public void handlePlayerQuit(Player player) {
        GameSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        UUID quitter = player.getUniqueId();
        UUID winner = session.getOpponent(quitter);
        Player winnerPlayer = plugin.getServer().getPlayer(winner);

        playersInGUI.remove(session.getPlayerA());
        playersInGUI.remove(session.getPlayerB());

        double wager = session.getWagerAmount();

        if (winnerPlayer != null && winnerPlayer.isOnline()) {
            double totalPrize = wager * 2;
            plugin.getLogger().info("§c[对赌] " + player.getName() + " 退出! "
                    + winnerPlayer.getName() + " 自动获胜");

            boolean success = economyManager.deposit(winnerPlayer, totalPrize);
            if (success) {
                winnerPlayer.closeInventory();
                winnerPlayer.sendMessage("\n§c§l══════ 对手退出 ══════");
                winnerPlayer.sendMessage("§c" + player.getName() + " §7已退出游戏");
                winnerPlayer.sendMessage("§a你自动获胜! 获得: §e" + economyManager.format(totalPrize));
                winnerPlayer.sendMessage("§c§l══════════════════════");
            } else {
                economyManager.deposit(winnerPlayer, wager);
                winnerPlayer.sendMessage("§c❌ 发放失败, 已退还本金 §e" + economyManager.format(wager));
            }
        } else {
            plugin.getLogger().warning("§c[对赌] " + player.getName() + " 退出, 胜者不在线, 退款");
            if (winnerPlayer != null) {
                economyManager.deposit(winnerPlayer, wager);
            }
        }

        cleanupSession(session);
    }

    // ========== 查询方法 ==========

    public boolean isPlayerInGame(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public boolean isPlayerInGUI(Player player) {
        return playersInGUI.contains(player.getUniqueId());
    }

    public void removePlayerFromGUI(Player player) {
        playersInGUI.remove(player.getUniqueId());
    }

    public GameSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public static String getChineseChoice(Choice choice) {
        if (choice == null) return "未选择";
        return switch (choice) {
            case ROCK -> "§f石头";
            case PAPER -> "§f布";
            case SCISSORS -> "§f剪刀";
        };
    }
}
