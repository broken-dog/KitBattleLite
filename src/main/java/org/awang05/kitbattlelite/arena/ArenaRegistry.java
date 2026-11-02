package org.awang05.kitbattlelite.arena;

import org.bukkit.World;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 运行时地图索引：按 id、按世界名查找。
 *
 * <p>顺序是**注册顺序**——即 {@code arenas.yml} 里段的书写顺序，新导入的地图追加在末尾。
 *
 * <p>由 {@code ArenaRepository} 填充。
 */
public final class ArenaRegistry {
    private final Map<String, Arena> byId = new LinkedHashMap<>();
    private final Map<String, Arena> byWorldName = new LinkedHashMap<>();

    /** 注册地图；id 或世界名重复时抛异常 */
    public void register(Arena arena) {
        Arena previousId = byId.put(arena.id(), arena);
        if (previousId != null) {
            byId.put(previousId.id(), previousId);
            throw new IllegalStateException("地图 id 重复: " + arena.id());
        }
        Arena previousWorld = byWorldName.put(arena.worldName(), arena);
        if (previousWorld != null) {
            byWorldName.put(previousWorld.worldName(), previousWorld);
            byId.remove(arena.id());
            throw new IllegalStateException("世界 " + arena.worldName() + " 已绑定地图 " + previousWorld.id());
        }
    }

    public boolean unregister(String id) {
        if (id == null) {
            return false;
        }
        Arena removed = byId.remove(id);
        if (removed == null) {
            return false;
        }
        byWorldName.remove(removed.worldName());
        return true;
    }

    public Optional<Arena> find(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    public Optional<Arena> byWorldName(String worldName) {
        return worldName == null ? Optional.empty() : Optional.ofNullable(byWorldName.get(worldName));
    }

    public Optional<Arena> byWorld(World world) {
        return world == null ? Optional.empty() : byWorldName(world.getName());
    }

    /** 全部地图（按注册顺序，只读视图）。 */
    public Collection<Arena> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    /** 已注册的地图 id（按注册顺序，只读视图）。 */
    public Set<String> ids() {
        return Collections.unmodifiableSet(byId.keySet());
    }

    public int size() {
        return byId.size();
    }

    public void clear() {
        byId.clear();
        byWorldName.clear();
    }
}
