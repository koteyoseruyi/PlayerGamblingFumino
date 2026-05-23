package com.example.playergamblingfumino.manager;

import com.example.playergamblingfumino.PlayerGamblingFumino;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InvitationManager {

    private final PlayerGamblingFumino plugin;
    private final Map<UUID, Invitation> invitations = new HashMap<>();

    public InvitationManager(PlayerGamblingFumino plugin) {
        this.plugin = plugin;
    }

    /**
     * 创建对赌邀请
     */
    public boolean createInvite(Player sender, Player target) {
        // 检查双方是否已被邀请或已在对战中
        if (invitations.containsKey(target.getUniqueId())) {
            sender.sendMessage("§c该玩家已有待处理的邀请");
            return false;
        }
        if (invitations.containsKey(sender.getUniqueId())) {
            sender.sendMessage("§c你已有一个待处理的邀请，请先取消");
            return false;
        }

        Invitation invitation = new Invitation(sender.getUniqueId(), target.getUniqueId());
        invitations.put(target.getUniqueId(), invitation);

        // 超时自动取消
        int timeout = plugin.getConfig().getInt("settings.invite-timeout", 60);
        new BukkitRunnable() {
            @Override
            public void run() {
                Invitation inv = invitations.get(target.getUniqueId());
                if (inv != null && inv.equals(invitation) && !inv.isAccepted() && !inv.isDeclined()) {
                    invitations.remove(target.getUniqueId());
                    Player s = plugin.getServer().getPlayer(sender.getUniqueId());
                    Player t = plugin.getServer().getPlayer(target.getUniqueId());
                    if (s != null) s.sendMessage("§c对 " + target.getName() + " 的邀请已超时");
                    if (t != null) t.sendMessage("§c来自 " + sender.getName() + " 的对赌邀请已超时");
                }
            }
        }.runTaskLater(plugin, timeout * 20L);

        return true;
    }

    /**
     * 获取目标玩家的邀请
     */
    public Invitation getInvite(Player target) {
        return invitations.get(target.getUniqueId());
    }

    /**
     * 接受邀请
     */
    public Invitation acceptInvite(Player target) {
        Invitation inv = invitations.remove(target.getUniqueId());
        if (inv != null) {
            inv.setAccepted(true);
        }
        return inv;
    }

    /**
     * 拒绝邀请
     */
    public void rejectInvite(Player target) {
        Invitation inv = invitations.remove(target.getUniqueId());
        if (inv != null) {
            inv.setDeclined(true);
            Player sender = plugin.getServer().getPlayer(inv.getSender());
            if (sender != null) {
                sender.sendMessage("§c" + target.getName() + " 拒绝了你的对赌邀请");
            }
        }
    }

    /**
     * 是否有待处理的邀请
     */
    public boolean hasInvite(Player target) {
        return invitations.containsKey(target.getUniqueId());
    }

    /**
     * 邀请数据类
     */
    public static class Invitation {
        private final UUID sender;
        private final UUID target;
        private boolean accepted;
        private boolean declined;
        private final long timestamp;

        public Invitation(UUID sender, UUID target) {
            this.sender = sender;
            this.target = target;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getSender() { return sender; }
        public UUID getTarget() { return target; }
        public boolean isAccepted() { return accepted; }
        public void setAccepted(boolean accepted) { this.accepted = accepted; }
        public boolean isDeclined() { return declined; }
        public void setDeclined(boolean declined) { this.declined = declined; }
        public long getTimestamp() { return timestamp; }
    }
}
