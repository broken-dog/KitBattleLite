package org.awang05.kitbattlelite.kit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.Objects;

/**
 * 一个职业：id + 图标 + 41 格内容 + 可选的药水效果。
 *
 * <p>内容下标见 {@link KitSlots}；空槽统一用 {@code null} 表示，不保存 AIR 物品。
 * 所有进出模型的 {@link ItemStack} 都 clone，模型不会把内部实例交给 GUI 或会话。
 *
 * <p>药水效果独立于 41 格内容，用一份可空的 {@link PotionMeta} 承载，同样进出 clone。
 *
 * <p>模型本身不持久化，读写由 {@link KitRepository}/{@link KitCodec} 负责。
 */
public final class Kit {
    private final String id;
    private ItemStack icon;
    private final ItemStack[] contents = new ItemStack[KitSlots.SIZE];
    private PotionMeta potion;

    /**
     * @param id       职业 id（非空、不含空白与点号：它会成为 YAML 键与命令参数）
     * @param icon     图标；null 表示用“纸 + id”作为默认图标
     * @param contents 41 格内容；null 等价于全空，元素中的 AIR/数量 0 会被当作空槽
     */
    public Kit(String id, ItemStack icon, ItemStack[] contents) {
        this.id = normalizeId(id);
        this.icon = icon == null ? defaultIcon(this.id) : icon.clone();
        if (contents != null) {
            if (contents.length != KitSlots.SIZE) {
                throw new IllegalArgumentException(
                        "职业内容必须是 " + KitSlots.SIZE + " 格，实际 " + contents.length);
            }
            for (int i = 0; i < KitSlots.SIZE; i++) {
                this.contents[i] = normalize(contents[i]);
            }
        }
    }

    public String id() {
        return id;
    }

    /** 图标的副本；永不返回 null。 */
    public ItemStack icon() {
        return icon.clone();
    }

    /** 深拷贝一份同 id、同图标、同内容、同药水效果的职业，用于编辑工作副本。 */
    public Kit copy() {
        Kit copy = new Kit(id, icon, contents);
        copy.potion = potion == null ? null : potion.clone();
        return copy;
    }

    /** 替换图标；传入 null 恢复默认图标。 */
    public void setIcon(ItemStack icon) {
        this.icon = icon == null ? defaultIcon(id) : icon.clone();
    }

    /** 指定槽位内容的副本；空槽返回 null。 */
    public ItemStack content(int slot) {
        checkSlot(slot);
        ItemStack item = contents[slot];
        return item == null ? null : item.clone();
    }

    /** 写入指定槽位；null 或 AIR 表示清空。 */
    public void setContent(int slot, ItemStack item) {
        checkSlot(slot);
        contents[slot] = normalize(item);
    }

    /** 药水效果的副本；未设置返回 null。 */
    public PotionMeta potion() {
        return potion == null ? null : potion.clone();
    }

    /** 写入药水效果；传入 null 表示无效果。 */
    public void setPotion(PotionMeta potion) {
        this.potion = potion == null ? null : potion.clone();
    }

    /**
     * 把整套内容应用到玩家：装备位 + 副手 + 背包 0..35 + 药水效果。
     *
     * <p>空槽会被清空（{@code setItem(slot, null)}），与旧版行为一致。
     * 药水效果按自定义效果列表逐条以无限时长应用（保留各自等级）；无效果时不动玩家已有状态。
     */
    public void applyTo(Player player) {
        Objects.requireNonNull(player, "player");
        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(content(KitSlots.HELMET));
        inventory.setChestplate(content(KitSlots.CHESTPLATE));
        inventory.setLeggings(content(KitSlots.LEGGINGS));
        inventory.setBoots(content(KitSlots.BOOTS));
        inventory.setItemInOffHand(content(KitSlots.OFFHAND));
        for (int i = 0; i < KitSlots.INVENTORY_SIZE; i++) {
            inventory.setItem(i, content(KitSlots.inventorySlot(i)));
        }
        applyPotionEffects(player);
    }

    /** 按自定义效果列表，用各自的效果类型与等级、以无限时长应用到玩家。 */
    private void applyPotionEffects(Player player) {
        if (potion == null) {
            return;
        }
        for (PotionEffect effect : potion.getCustomEffects()) {
            player.addPotionEffect(new PotionEffect(
                    effect.getType(),
                    PotionEffect.INFINITE_DURATION,
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.hasParticles(),
                    effect.hasIcon()));
        }
    }

    private static ItemStack normalize(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return null;
        }
        return item.clone();
    }

    private static ItemStack defaultIcon(String id) {
        ItemStack icon = new ItemStack(Material.PAPER);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(id, NamedTextColor.YELLOW));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private static String normalizeId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("职业 id 不能为 null");
        }
        String trimmed = id.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("职业 id 不能为空");
        }
        if (trimmed.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("职业 id 不能包含空白字符: " + id);
        }
        if (trimmed.indexOf('.') >= 0) {
            throw new IllegalArgumentException("职业 id 不能包含点号: " + id);
        }
        return trimmed;
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= KitSlots.SIZE) {
            throw new IllegalArgumentException("职业内容下标必须是 0.." + (KitSlots.SIZE - 1) + "，实际 " + slot);
        }
    }
}
