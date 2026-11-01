package org.awang05.kitbattlelite.player;

import org.bukkit.Location;

import java.util.UUID;

/**
 * 玩家会话：一次进图期间才需要的数据。
 *
 * <p>持有 UUID 与会话数据；退服时由 {@code SessionRegistry} 移除，离场时清空竞技场字段。
 */
public final class PlayerSession {
    /**
     * 正在编辑的页面槽位。
     *
     * @param pageIndex     页面下标，从 1 开始
     * @param position      页面位置，调用方保证 0..27
     * @param kitId         当前槽位上的 kit id，可为 null（新建）
     * @param expectedKitId 打开编辑器时槽位上的 kit id，保存前用于并发校验
     */
    public record EditSession(int pageIndex, int position, String kitId, String expectedKitId) {
        public EditSession {
            if (pageIndex < 1) {
                throw new IllegalArgumentException("页面下标从 1 开始，实际 " + pageIndex);
            }
            if (position < 0) {
                throw new IllegalArgumentException("页面位置不能为负，实际 " + position);
            }
        }
    }

    private final UUID playerId;
    private Location returnLocation;
    private PlayerSnapshot snapshot;
    private String arenaId;
    private boolean preview;
    private int killStreak;
    private long protectionUntil;
    private EditSession edit;

    public PlayerSession(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID playerId() {
        return playerId;
    }

    /** 进图前的返回点副本；没有记录时返回 null。 */
    public Location returnLocation() {
        return returnLocation == null ? null : returnLocation.clone();
    }

    /** 保存返回点副本；传 null 清除。 */
    public void setReturnLocation(Location returnLocation) {
        this.returnLocation = returnLocation == null ? null : returnLocation.clone();
    }

    public PlayerSnapshot snapshot() {
        return snapshot;
    }

    public void setSnapshot(PlayerSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    /** 当前所在竞技场 id；null 表示不在竞技场。 */
    public String arenaId() {
        return arenaId;
    }

    public void setArenaId(String arenaId) {
        this.arenaId = arenaId;
    }

    /** 是否是管理员预览（不计入人数、不参与死亡/保护流程）。 */
    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public int killStreak() {
        return killStreak;
    }

    public void setKillStreak(int killStreak) {
        this.killStreak = Math.max(0, killStreak);
    }

    /** 免伤到期时间（epoch millis）；0 表示无保护。 */
    public long protectionUntil() {
        return protectionUntil;
    }

    public void setProtectionUntil(long protectionUntil) {
        this.protectionUntil = protectionUntil;
    }

    public EditSession edit() {
        return edit;
    }

    public void setEdit(EditSession edit) {
        this.edit = edit;
    }

    public boolean idPlayerProtected() {
        return arenaId != null && protectionUntil > System.currentTimeMillis();
    }

    /** 清空与竞技场有关的状态；返回点/快照由调用方先处理。 */
    public void clearArenaState() {
        arenaId = null;
        preview = false;
        killStreak = 0;
        protectionUntil = 0;
        edit = null;
    }
}
