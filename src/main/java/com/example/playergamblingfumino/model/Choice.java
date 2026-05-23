package com.example.playergamblingfumino.model;

public enum Choice {
    ROCK,
    PAPER,
    SCISSORS;

    /**
     * 判断当前选择与对手选择的胜负关系
     * @param other 对手的选择
     * @return WIN - 当前玩家赢, LOSE - 当前玩家输, DRAW - 平局
     */
    public Result vs(Choice other) {
        if (this == other) return Result.DRAW;
        return switch (this) {
            case ROCK -> other == SCISSORS ? Result.WIN : Result.LOSE;
            case PAPER -> other == ROCK ? Result.WIN : Result.LOSE;
            case SCISSORS -> other == PAPER ? Result.WIN : Result.LOSE;
        };
    }

    public enum Result {
        WIN,
        LOSE,
        DRAW
    }
}
