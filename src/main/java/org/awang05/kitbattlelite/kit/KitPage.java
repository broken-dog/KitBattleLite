package org.awang05.kitbattlelite.kit;

/**
 * 一页职业：固定 28 个位置，每个位置存 kit id 或 null。
 *
 * <p>页面只引用 id，不复制职业内容；同一个 kit 出现在多个位置时改一次全部同步。
 */
public final class KitPage {
    public static final int SIZE = 28;

    private final int index;
    private final String[] slots = new String[SIZE];

    /**
     * @param index 页面下标，从 1 开始
     */
    public KitPage(int index) {
        if (index < 1) {
            throw new IllegalArgumentException("页面下标从 1 开始，实际 " + index);
        }
        this.index = index;
    }

    public int index() {
        return index;
    }

    /** 指定位置引用的 kit id；空位返回 null。 */
    public String kitId(int position) {
        checkPosition(position);
        return slots[position];
    }

    /** 写入引用；null 或空白字符串表示清空该位置。 */
    public void setKitId(int position, String kitId) {
        checkPosition(position);
        if (kitId == null || kitId.isBlank()) {
            slots[position] = null;
            return;
        }
        slots[position] = kitId.trim();
    }

    /** 这一页是否没有任何职业。 */
    public boolean isEmpty() {
        for (String slot : slots) {
            if (slot != null) {
                return false;
            }
        }
        return true;
    }

    private static void checkPosition(int position) {
        if (position < 0 || position >= SIZE) {
            throw new IllegalArgumentException("页面位置必须是 0.." + (SIZE - 1) + "，实际 " + position);
        }
    }
}
