package com.example.playergamblingfumino.model;

public enum GamePhase {
    /** 押注设定阶段（选择档位+确认） */
    BET_SETUP,
    /** 猜拳阶段 */
    RPS_CHOICE,
    /** 结果显示 */
    RESULT,
    /** 游戏结束 */
    FINISHED
}
