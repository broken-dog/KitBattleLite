package org.awang05.kitbattlelite.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiLayoutTest {

    @Test
    void pageGridIsSevenByFourInsideBorders() {
        assertEquals(10, GuiLayout.pageSlot(0));
        assertEquals(16, GuiLayout.pageSlot(6));
        assertEquals(19, GuiLayout.pageSlot(7));
        assertEquals(25, GuiLayout.pageSlot(13));
        assertEquals(37, GuiLayout.pageSlot(21));
        assertEquals(43, GuiLayout.pageSlot(27));
    }

    @Test
    void pagePositionRevertsPageSlot() {
        for (int position = 0; position < GuiLayout.PAGE_SLOT_COUNT; position++) {
            int guiSlot = GuiLayout.pageSlot(position);
            assertEquals(position, GuiLayout.pagePosition(guiSlot));
            assertTrue(GuiLayout.isPageSlot(guiSlot));
        }
    }

    @Test
    void bordersAndControlsAreNotPageSlots() {
        int[] notPageSlots = {
                0, 1, 8, 9, 17, 18, 26, 27, 35, 36, 44,
                GuiLayout.PAGE_PREV, GuiLayout.PAGE_INDICATOR, GuiLayout.PAGE_NEXT,
                51, 52, GuiLayout.KIT_SAVE
        };
        for (int guiSlot : notPageSlots) {
            assertEquals(-1, GuiLayout.pagePosition(guiSlot));
            assertFalse(GuiLayout.isPageSlot(guiSlot));
        }
        assertEquals(-1, GuiLayout.pagePosition(-1));
        assertEquals(-1, GuiLayout.pagePosition(GuiLayout.SIZE));
    }

    @Test
    void kitContentsFillFirstRowThenFourInventoryRows() {
        for (int kitSlot = 0; kitSlot < 5; kitSlot++) {
            assertEquals(kitSlot, GuiLayout.kitSlot(kitSlot));
        }
        assertEquals(9, GuiLayout.kitSlot(5));
        assertEquals(26, GuiLayout.kitSlot(22));
        assertEquals(44, GuiLayout.kitSlot(40));
    }

    @Test
    void kitSlotIndexRevertsKitSlot() {
        for (int kitSlot = 0; kitSlot < 41; kitSlot++) {
            int guiSlot = GuiLayout.kitSlot(kitSlot);
            assertEquals(kitSlot, GuiLayout.kitSlotIndex(guiSlot));
            assertTrue(GuiLayout.isKitSlot(guiSlot));
        }
        assertEquals(-1, GuiLayout.kitSlotIndex(5));
        assertEquals(-1, GuiLayout.kitSlotIndex(8));
        assertEquals(-1, GuiLayout.kitSlotIndex(45));
        assertEquals(-1, GuiLayout.kitSlotIndex(GuiLayout.KIT_SAVE));
        assertEquals(-1, GuiLayout.kitSlotIndex(-1));
    }

    @Test
    void outOfRangeIndexesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> GuiLayout.pageSlot(-1));
        assertThrows(IllegalArgumentException.class, () -> GuiLayout.pageSlot(GuiLayout.PAGE_SLOT_COUNT));
        assertThrows(IllegalArgumentException.class, () -> GuiLayout.kitSlot(-1));
        assertThrows(IllegalArgumentException.class, () -> GuiLayout.kitSlot(41));
    }

    @Test
    void controlSlotsStayInBottomRow() {
        assertEquals(GuiLayout.COLUMNS * 5 + 3, GuiLayout.PAGE_PREV);
        assertEquals(GuiLayout.COLUMNS * 5 + 4, GuiLayout.PAGE_INDICATOR);
        assertEquals(GuiLayout.COLUMNS * 5 + 5, GuiLayout.PAGE_NEXT);
    }

    @Test
    void worldSizeRoundsUpToRowsAndCapsAtSix() {
        assertEquals(9, GuiLayout.worldSize(0));
        assertEquals(9, GuiLayout.worldSize(-3));
        assertEquals(9, GuiLayout.worldSize(1));
        assertEquals(9, GuiLayout.worldSize(9));
        assertEquals(18, GuiLayout.worldSize(10));
        assertEquals(45, GuiLayout.worldSize(45));
        assertEquals(54, GuiLayout.worldSize(46));
        assertEquals(54, GuiLayout.worldSize(54));
        assertEquals(54, GuiLayout.worldSize(100));
    }
}
