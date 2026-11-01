package org.awang05.kitbattlelite.player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 维护在线玩家的会话表。
 */
public final class SessionRegistry {
    private final Map<UUID, PlayerSession> sessions = new HashMap<>();

    /** 取会话，没有就创建一个空的（进图/预览时用）。 */
    public PlayerSession getOrCreate(UUID playerId) {
        return sessions.computeIfAbsent(playerId, PlayerSession::new);
    }

    public PlayerSession get(UUID playerId) {
        return sessions.get(playerId);
    }

    public PlayerSession remove(UUID playerId) {
        return sessions.remove(playerId);
    }

    public void clear() {
        sessions.clear();
    }

    public int size() {
        return sessions.size();
    }

    /** 只读视图；不要在遍历时调用 remove/clear，需要写时先复制 {@link #ids()}。 */
    public Collection<PlayerSession> all() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    /** 当前所有会话的 UUID 副本，可安全用于遍历中删除。 */
    public Set<UUID> ids() {
        return new HashSet<>(sessions.keySet());
    }
}
