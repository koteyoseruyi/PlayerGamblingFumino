package com.example.playergamblingfumino.model;

import org.bukkit.entity.Player;

import java.util.UUID;

public class GameSession {

    private final UUID playerA;
    private final UUID playerB;
    private String nameA;
    private String nameB;

    // === 回合选择（5/7/9）===
    private Integer selectedRoundsA;
    private Integer selectedRoundsB;

    // === 押注档位选择 ===
    private Integer selectedTierA;
    private Integer selectedTierB;
    private double wagerAmount;

    // === 确认状态 ===
    private boolean confirmedA;
    private boolean confirmedB;

    // === 系数拔河 ===
    private double coefficientA;
    private double accumulatedStep;
    private int totalRounds;

    // === 猜拳状态 ===
    private Choice choiceA;
    private Choice choiceB;
    private boolean choiceConfirmedA;
    private boolean choiceConfirmedB;

    private GamePhase phase;
    private int round;

    // === 配置参数 ===
    private final double baseStep;
    private final double gradientStep;
    private final double minCoefficient = 0.0;
    private final double maxCoefficient = 2.0;

    public static final int[] ROUND_OPTIONS = {5, 7, 9};
    public static final int[] TIER_OPTIONS = {10000, 25000, 50000, 250000, 500000};

    public GameSession(Player playerA, Player playerB,
                       double baseStep, double gradientStep) {
        this.playerA = playerA.getUniqueId();
        this.playerB = playerB.getUniqueId();
        this.nameA = playerA.getName();
        this.nameB = playerB.getName();

        this.coefficientA = 1.0;
        this.accumulatedStep = 0;
        this.totalRounds = 9;
        this.phase = GamePhase.BET_SETUP;
        this.round = 1;

        this.baseStep = baseStep;
        this.gradientStep = gradientStep;
    }

    // ========== Getter ==========

    public UUID getPlayerA() { return playerA; }
    public UUID getPlayerB() { return playerB; }
    public String getNameA() { return nameA; }
    public String getNameB() { return nameB; }

    public Integer getSelectedRoundsA() { return selectedRoundsA; }
    public Integer getSelectedRoundsB() { return selectedRoundsB; }
    public Integer getSelectedTierA() { return selectedTierA; }
    public Integer getSelectedTierB() { return selectedTierB; }
    public double getWagerAmount() { return wagerAmount; }
    public boolean isConfirmedA() { return confirmedA; }
    public boolean isConfirmedB() { return confirmedB; }

    public double getCoefficientA() { return coefficientA; }
    public double getCoefficientB() { return 2.0 - coefficientA; }

    public int getRound() { return round; }
    public int getTotalRounds() { return totalRounds; }
    public double getBaseStep() { return baseStep; }
    public double getGradientStep() { return gradientStep; }

    public Choice getChoiceA() { return choiceA; }
    public Choice getChoiceB() { return choiceB; }
    public boolean isChoiceConfirmedA() { return choiceConfirmedA; }
    public boolean isChoiceConfirmedB() { return choiceConfirmedB; }

    public GamePhase getPhase() { return phase; }

    // ========== Setter ==========

    public void setPhase(GamePhase phase) { this.phase = phase; }

    public void setConfirmedA(boolean v) { this.confirmedA = v; }
    public void setConfirmedB(boolean v) { this.confirmedB = v; }

    public void setChoiceA(Choice choiceA) { this.choiceA = choiceA; }
    public void setChoiceB(Choice choiceB) { this.choiceB = choiceB; }
    public void setChoiceConfirmedA(boolean v) { this.choiceConfirmedA = v; }
    public void setChoiceConfirmedB(boolean v) { this.choiceConfirmedB = v; }

    // ========== 基于UUID的Getter/Setter ==========

    public UUID getOpponent(UUID self) {
        return playerA.equals(self) ? playerB : playerA;
    }

    public String getOpponentName(UUID self) {
        return playerA.equals(self) ? nameB : nameA;
    }

    // ---- 回合选择 ----

    public Integer getSelectedRounds(UUID uuid) {
        return playerA.equals(uuid) ? selectedRoundsA : selectedRoundsB;
    }

    public void setSelectedRounds(UUID uuid, Integer rounds) {
        if (playerA.equals(uuid)) selectedRoundsA = rounds;
        else selectedRoundsB = rounds;
    }

    public boolean isBothRoundsSelected() {
        return selectedRoundsA != null && selectedRoundsB != null;
    }

    public boolean isRoundsMatched() {
        return isBothRoundsSelected() && selectedRoundsA.equals(selectedRoundsB);
    }

    // ---- 档位选择 ----

    public Integer getSelectedTier(UUID uuid) {
        return playerA.equals(uuid) ? selectedTierA : selectedTierB;
    }

    public void setSelectedTier(UUID uuid, Integer tier) {
        if (playerA.equals(uuid)) selectedTierA = tier;
        else selectedTierB = tier;
    }

    public boolean isBothTiersSelected() {
        return selectedTierA != null && selectedTierB != null;
    }

    public boolean isTiersMatched() {
        return isBothTiersSelected() && selectedTierA.equals(selectedTierB);
    }

    // ---- 确认 ----

    public boolean isConfirmed(UUID uuid) {
        return playerA.equals(uuid) ? confirmedA : confirmedB;
    }

    public void setConfirmed(UUID uuid, boolean v) {
        if (playerA.equals(uuid)) confirmedA = v;
        else confirmedB = v;
    }

    public boolean isBothConfirmed() {
        return confirmedA && confirmedB;
    }

    /** ✅ 关键方法：双方猜拳是否都已确认 */
    public boolean isBothChoicesConfirmed() {
        return choiceConfirmedA && choiceConfirmedB;
    }

    public boolean isAllMatched() {
        return isRoundsMatched() && isTiersMatched();
    }

    // ---- 最终确定 ----

    public void finalizeSettings() {
        if (isAllMatched()) {
            this.totalRounds = selectedRoundsA;
            this.wagerAmount = selectedTierA;
        }
    }

    // ========== 拔河核心逻辑 ==========

    public double getStepForRound(int k) {
        return baseStep + (k - 1) * gradientStep;
    }

    public double getCurrentStep() {
        return getStepForRound(round) + accumulatedStep;
    }

    public boolean executeRound() {
        if (choiceA == null || choiceB == null) return false;

        Choice.Result result = choiceA.vs(choiceB);
        double step = getCurrentStep();

        switch (result) {
            case WIN -> {
                coefficientA = Math.min(maxCoefficient, coefficientA + step);
                accumulatedStep = 0;
                round++;
            }
            case LOSE -> {
                coefficientA = Math.max(minCoefficient, coefficientA - step);
                accumulatedStep = 0;
                round++;
            }
            case DRAW -> {
                accumulatedStep += getStepForRound(round);
                round++;
            }
        }

        return coefficientA >= maxCoefficient || coefficientA <= minCoefficient;
    }

    public boolean shouldEnd() {
        return coefficientA >= maxCoefficient || coefficientA <= minCoefficient || round > totalRounds;
    }

    public double calculatePayoutA() {
        double totalPool = wagerAmount * 2;
        return totalPool * (coefficientA / 2.0);
    }

    public double calculatePayoutB() {
        double totalPool = wagerAmount * 2;
        return totalPool * (1.0 - coefficientA / 2.0);
    }

    public void resetChoices() {
        choiceA = null;
        choiceB = null;
        choiceConfirmedA = false;
        choiceConfirmedB = false;
    }

    public boolean isPlayerInSession(UUID uuid) {
        return playerA.equals(uuid) || playerB.equals(uuid);
    }
}
