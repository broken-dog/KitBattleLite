package org.awang05.kitbattlelite.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 插件物品的工厂与识别器：所有 PDC 读写都集中在这里。
 *
 * <p>两个 key 都挂在本插件的命名空间下（当前为 {@code kitbattlelite:item} / {@code kitbattlelite:kit}），
 * 而不是固定字符串命名空间：本插件不把 PDC 身份当作持久化协议，按钮与图标都是运行时现造、
 * 重启后重新打标，插件改名时旧物品退化为普通物品是可接受的。
 *
 * <p>生命周期：由组合根创建一次并注入；无内部可变状态（{@code byId} 构建后只读）。
 */
public final class ItemService {
    private final NamespacedKey itemKey;
    private final NamespacedKey kitKey;
    private final Map<String, PluginItem> byId;

    public ItemService(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.itemKey = new NamespacedKey(plugin, "item");
        this.kitKey = new NamespacedKey(plugin, "kit");
        this.byId = buildIdMap();
    }

    private static Map<String, PluginItem> buildIdMap() {
        Map<String, PluginItem> map = new HashMap<>();
        for (PluginItem type : PluginItem.values()) {
            PluginItem previous = map.put(type.id(), type);
            if (previous != null) {
                throw new IllegalStateException("PluginItem id 重复: " + type.id()
                        + "（" + previous + " 与 " + type + "）");
            }
        }
        return Map.copyOf(map);
    }

    /** 创建一个带插件身份标记的新物品实例。 */
    public ItemStack create(PluginItem type) {
        Objects.requireNonNull(type, "type");
        return tag(type.newDisplay(), type);
    }

    /**
     * 给已有物品打上插件身份标记。
     *
     * <p><b>会修改传入实例</b>并返回同一实例；调用方要传自己独占的 ItemStack（通常是 clone），
     * 不要把仓库里的模型实例直接交出来。
     *
     * @throws IllegalArgumentException 传入 null 或空气
     * @throws IllegalStateException    物品没有 ItemMeta
     */
    public ItemStack tag(ItemStack item, PluginItem type) {
        Objects.requireNonNull(type, "type");
        ItemMeta meta = requireMeta(item);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, type.id());
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 把物品标记为“职业图标”，并记录它指向的 kit id。
     *
     * @param icon  职业图标的原始物品（会被修改）
     * @param kitId 职业 id，不能为空
     * @throws IllegalArgumentException 图标为 null/空气，或 kitId 为空
     */
    public ItemStack tagKit(ItemStack icon, String kitId) {
        if (kitId == null || kitId.isBlank()) {
            throw new IllegalArgumentException("kitId 不能为空");
        }
        ItemMeta meta = requireMeta(icon);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(itemKey, PersistentDataType.STRING, PluginItem.KIT.id());
        container.set(kitKey, PersistentDataType.STRING, kitId.trim());
        icon.setItemMeta(meta);
        return icon;
    }

    /** 识别物品类型；不是本插件物品、或 id 未注册时返回空。 */
    public Optional<PluginItem> find(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        String id = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    /** 是否是特定类型的插件物品；热路径（点击、交互）用这个。 */
    public boolean is(ItemStack item, PluginItem expected) {
        if (expected == null) {
            return false;
        }
        return find(item).map(actual -> actual == expected).orElse(false);
    }

    /**
     * 是否带本插件的 PDC 标记。
     *
     * <p>用于“禁止把插件物品丢出去/塞进箱子”这类防守判断：即使 id 过期，
     * 只要还带着本插件的 key 就当插件物品处理。
     */
    public boolean isPluginItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(itemKey, PersistentDataType.STRING);
    }

    /** 读取职业图标指向的 kit id；普通物品返回空。 */
    public Optional<String> kitId(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        String kitId = meta.getPersistentDataContainer().get(kitKey, PersistentDataType.STRING);
        if (kitId == null || kitId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(kitId);
    }

    private static ItemMeta requireMeta(ItemStack item) {
        if (item == null) {
            throw new IllegalArgumentException("物品不能为 null");
        }
        if (item.getType().isAir() || item.getAmount() <= 0) {
            throw new IllegalArgumentException("不能给空气打标");
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("材质 " + item.getType() + " 没有 ItemMeta，无法打标");
        }
        return meta;
    }
}
