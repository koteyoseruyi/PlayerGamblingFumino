package com.example.playergamblingfumino.command;

import com.example.playergamblingfumino.PlayerGamblingFumino;
import com.example.playergamblingfumino.manager.GameManager;
import com.example.playergamblingfumino.manager.InvitationManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GamblingCommand implements CommandExecutor, TabCompleter {

    private final PlayerGamblingFumino plugin;
    private final GameManager gameManager;
    private final InvitationManager invitationManager;

    public GamblingCommand(PlayerGamblingFumino plugin, GameManager gameManager,
                           InvitationManager invitationManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.invitationManager = invitationManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player);
            case "disaccept" -> handleDisaccept(player);
            case "reload" -> handleReload(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l══════ 对赌插件 ══════");
        player.sendMessage("§e/gamble invite <玩家> §7- 发起对赌邀请");
        player.sendMessage("§e/gamble accept §7- 接受邀请");
        player.sendMessage("§e/gamble disaccept §7- 拒绝邀请");
        player.sendMessage("§e/gamble reload §7- 重载配置 (管理员)");
        player.sendMessage("§6§l══════════════════════");
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /gamble invite <玩家名>");
            return;
        }

        if (gameManager.isPlayerInGame(player)) {
            player.sendMessage("§c你已在游戏中");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c玩家 " + args[1] + " 不在线");
            return;
        }

        if (target.equals(player)) {
            player.sendMessage("§c不能邀请自己");
            return;
        }

        if (gameManager.isPlayerInGame(target)) {
            player.sendMessage("§c该玩家已在游戏中");
            return;
        }

        double balance = plugin.getEconomyManager().getBalance(player);
        if (balance < 20000) {
            player.sendMessage("§c你的Vault余额不足, 至少需要 20000");
            return;
        }

        if (invitationManager.createInvite(player, target)) {
            player.sendMessage("§a已向 §e" + target.getName() + " §a发起对赌邀请");
            target.sendMessage("\n§6§l══════ 对赌邀请 ══════");
            target.sendMessage("§e" + player.getName() + " §a向你发起了对赌邀请!");
            target.sendMessage("§a输入 §e/gamble accept §a接受");
            target.sendMessage("§c输入 §e/gamble disaccept §c拒绝");
            target.sendMessage("§7(60秒后自动过期)");
            target.sendMessage("§6§l══════════════════════");
        } else {
            player.sendMessage("§c邀请失败");
        }
    }

    private void handleAccept(Player player) {
        if (!invitationManager.hasInvite(player)) {
            player.sendMessage("§c没有待处理的邀请");
            return;
        }

        if (gameManager.isPlayerInGame(player)) {
            player.sendMessage("§c你已在游戏中");
            return;
        }

        InvitationManager.Invitation inv = invitationManager.acceptInvite(player);
        if (inv == null) {
            player.sendMessage("§c邀请已失效");
            return;
        }

        Player sender = Bukkit.getPlayer(inv.getSender());
        if (sender == null || !sender.isOnline()) {
            player.sendMessage("§c邀请者已离线");
            return;
        }

        if (gameManager.isPlayerInGame(sender)) {
            player.sendMessage("§c邀请者已在游戏中");
            return;
        }

        if (!gameManager.createGame(sender, player)) {
            player.sendMessage("§c创建游戏失败");
        }
    }

    private void handleDisaccept(Player player) {
        if (!invitationManager.hasInvite(player)) {
            player.sendMessage("§c没有待处理的邀请");
            return;
        }

        invitationManager.rejectInvite(player);
        player.sendMessage("§c已拒绝对赌邀请");
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("gambling.admin") && !player.isOp()) {
            player.sendMessage("§c没有权限");
            return;
        }

        plugin.reloadConfig();
        gameManager.reloadConfig();
        player.sendMessage("§a配置已重载");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("invite");
            completions.add("accept");
            completions.add("disaccept");
            if (sender.hasPermission("gambling.admin") || sender.isOp()) {
                completions.add("reload");
            }
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return completions;
    }
}
