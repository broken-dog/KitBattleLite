package org.awang05.kitbattlelite.kit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitSlotsTest {

    @Test
    void layoutMatchesDesign() {
        assertEquals(41, KitSlots.SIZE);
        assertEquals(0, KitSlots.HELMET);
        assertEquals(1, KitSlots.CHESTPLATE);
        assertEquals(2, KitSlots.LEGGINGS);
        assertEquals(3, KitSlots.BOOTS);
        assertEquals(4, KitSlots.OFFHAND);
        assertEquals(5, KitSlots.INVENTORY_START);
        assertEquals(36, KitSlots.INVENTORY_SIZE);
    }

    @Test
    void inventoryIndexMapsToContentSlot() {
        assertEquals(5, KitSlots.inventorySlot(0));
        assertEquals(6, KitSlots.inventorySlot(1));
        assertEquals(40, KitSlots.inventorySlot(35));
    }

    @Test
    void inventoryIndexOutOfRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> KitSlots.inventorySlot(-1));
        assertThrows(IllegalArgumentException.class, () -> KitSlots.inventorySlot(36));
    }

    @Test
    void equipmentSlotsAreTheFirstFive() {
        for (int slot = 0; slot < KitSlots.SIZE; slot++) {
            assertEquals(slot < 5, KitSlots.isEquipment(slot), "槽位 " + slot);
        }
        assertTrue(KitSlots.isEquipment(KitSlots.OFFHAND));
        assertFalse(KitSlots.isEquipment(KitSlots.INVENTORY_START));
    }
}
