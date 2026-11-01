package org.awang05.kitbattlelite.kit;

/**
 * 职业 41 格内容的数据下标。
 *
 * <p>数据下标与 GUI 槽位是两套坐标：这里只描述 {@link Kit} 内部的 0..40，
 * GUI 槽位由 {@code gui/GuiLayout} 负责。前 5 格是装备，后 36 格是玩家背包。
 */
public final class KitSlots {
    public static final int HELMET = 0;
    public static final int CHESTPLATE = 1;
    public static final int LEGGINGS = 2;
    public static final int BOOTS = 3;
    public static final int OFFHAND = 4;
    public static final int INVENTORY_START = 5;
    public static final int INVENTORY_SIZE = 36;
    public static final int SIZE = INVENTORY_START + INVENTORY_SIZE;

    private KitSlots() {
    }

    /**
     * 把玩家背包下标（0..35）映射成职业内容下标（5..40）。
     *
     * @throws IllegalArgumentException 下标越界
     */
    public static int inventorySlot(int playerInventoryIndex) {
        if (playerInventoryIndex < 0 || playerInventoryIndex >= INVENTORY_SIZE) {
            throw new IllegalArgumentException(
                    "玩家背包下标必须是 0.." + (INVENTORY_SIZE - 1) + "，实际 " + playerInventoryIndex);
        }
        return INVENTORY_START + playerInventoryIndex;
    }

    /** 是否是前 5 格的装备位（头盔/胸甲/护腿/靴子/副手）。 */
    public static boolean isEquipment(int kitSlot) {
        return kitSlot >= HELMET && kitSlot <= OFFHAND;
    }
}
