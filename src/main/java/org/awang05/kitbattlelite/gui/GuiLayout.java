package org.awang05.kitbattlelite.gui;

import org.awang05.kitbattlelite.kit.KitPage;
import org.awang05.kitbattlelite.kit.KitSlots;

/**
 * GUI 的 54 格槽位常量，以及“数据下标 ↔ GUI 槽位”两套坐标的映射。
 *
 * <p>槽位数字只在这里出现，菜单里不允许再写魔数；两套坐标分别是：
 * <ul>
 *   <li>职业页位置 0..27：落在第 2..5 行（GUI 下标 1..4 行）的第 2..8 列，列数 7；</li>
 *   <li>职业内容下标 0..40（见 {@link KitSlots}）：前 5 格在首行，背包 36 格占第 2..5 行。</li>
 * </ul>
 *
 * <p>纯函数、无状态；不依赖 Bukkit 运行时。
 */
public final class GuiLayout {
    public static final int COLUMNS = 9;
    public static final int ROWS = 6;
    public static final int SIZE = COLUMNS * ROWS;

    /** 职业页：7 列 × 4 行，共 28 个位置。 */
    public static final int PAGE_COLUMNS = 7;
    public static final int PAGE_ROWS = 4;
    public static final int PAGE_SLOT_COUNT = KitPage.SIZE;
    /** 职业页位置 0..27 所在的第一行（0 起）。 */
    public static final int PAGE_FIRST_ROW = 1;

    /** 职业页底部：上一页 / 页码 / 下一页（居中三格）。 */
    public static final int PAGE_PREV = 48;
    public static final int PAGE_INDICATOR = 49;
    public static final int PAGE_NEXT = 50;

    /** 职业详情/编辑：返回（编辑中即取消）、图标、保存，与旧版位置一致。 */
    public static final int BACK = 8;
    public static final int KIT_ICON = 49;
    public static final int KIT_SAVE = 53;

    /**
     * 药水格：编辑/详情页底行展示药水效果，独立于 41 格职业内容（不属于 {@link KitSlots}）。
     * 编辑页可放取药水，保存时读取其 {@code PotionMeta}。
     */
    public static final int POTION_SLOT = 45;

    /** 地图选择页的格子数：5 行 × 9 列 */
    public static final int WORLD_SLOT_COUNT = COLUMNS * (ROWS - 1);

    private GuiLayout() {
    }

    /**
     * 职业页位置（0..27）→ GUI 槽位。
     *
     * @throws IllegalArgumentException 位置越界
     */
    public static int pageSlot(int position) {
        if (position < 0 || position >= PAGE_SLOT_COUNT) {
            throw new IllegalArgumentException(
                    "职业页位置必须是 0.." + (PAGE_SLOT_COUNT - 1) + "，实际 " + position);
        }
        int row = position / PAGE_COLUMNS + PAGE_FIRST_ROW;
        int column = position % PAGE_COLUMNS + 1;
        return row * COLUMNS + column;
    }

    /**
     * GUI 槽位 → 职业页位置；不是职业页格子时返回 -1。
     */
    public static int pagePosition(int guiSlot) {
        if (guiSlot < 0 || guiSlot >= SIZE) {
            return -1;
        }
        int row = guiSlot / COLUMNS - PAGE_FIRST_ROW;
        int column = guiSlot % COLUMNS - 1;
        if (row < 0 || row >= PAGE_ROWS || column < 0 || column >= PAGE_COLUMNS) {
            return -1;
        }
        return row * PAGE_COLUMNS + column;
    }

    /** 是否是职业页的可编辑格子。 */
    public static boolean isPageSlot(int guiSlot) {
        return pagePosition(guiSlot) >= 0;
    }

    /**
     * 职业内容下标（0..40）→ GUI 槽位：装备 0..4 落在首行 0..4，背包 0..35 落在第 2..5 行 9..44。
     *
     * @throws IllegalArgumentException 下标越界
     */
    public static int kitSlot(int kitSlot) {
        if (kitSlot < 0 || kitSlot >= KitSlots.SIZE) {
            throw new IllegalArgumentException(
                    "职业内容下标必须是 0.." + (KitSlots.SIZE - 1) + "，实际 " + kitSlot);
        }
        if (kitSlot < KitSlots.INVENTORY_START) {
            return kitSlot;
        }
        return COLUMNS + (kitSlot - KitSlots.INVENTORY_START);
    }

    /**
     * GUI 槽位 → 职业内容下标；不是套装格子时返回 -1。
     */
    public static int kitSlotIndex(int guiSlot) {
        if (guiSlot >= 0 && guiSlot < KitSlots.INVENTORY_START) {
            return guiSlot;
        }
        if (guiSlot >= COLUMNS && guiSlot < COLUMNS + KitSlots.INVENTORY_SIZE) {
            return KitSlots.INVENTORY_START + (guiSlot - COLUMNS);
        }
        return -1;
    }

    /** 是否是套装格（装备 5 格 + 背包 36 格），不含图标/保存/返回槽。 */
    public static boolean isKitSlot(int guiSlot) {
        return kitSlotIndex(guiSlot) >= 0;
    }
}
