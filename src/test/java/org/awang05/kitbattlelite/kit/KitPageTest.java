package org.awang05.kitbattlelite.kit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitPageTest {

    @Test
    void pageHoldsTwentyEightSlots() {
        assertEquals(28, KitPage.SIZE);
        KitPage page = new KitPage(1);
        assertEquals(1, page.index());
        for (int position = 0; position < KitPage.SIZE; position++) {
            assertNull(page.kitId(position));
        }
        assertTrue(page.isEmpty());
    }

    @Test
    void pageIndexStartsAtOne() {
        assertThrows(IllegalArgumentException.class, () -> new KitPage(0));
        assertThrows(IllegalArgumentException.class, () -> new KitPage(-1));
    }

    @Test
    void kitIdsAreTrimmedAndBlankClears() {
        KitPage page = new KitPage(2);
        page.setKitId(0, "  warrior  ");
        assertEquals("warrior", page.kitId(0));
        assertFalse(page.isEmpty());

        page.setKitId(0, "   ");
        assertNull(page.kitId(0));
        assertTrue(page.isEmpty());
    }

    @Test
    void positionOutOfRangeIsRejected() {
        KitPage page = new KitPage(1);
        assertThrows(IllegalArgumentException.class, () -> page.kitId(-1));
        assertThrows(IllegalArgumentException.class, () -> page.kitId(28));
        assertThrows(IllegalArgumentException.class, () -> page.setKitId(28, "warrior"));
    }
}
