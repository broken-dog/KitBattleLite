package org.awang05.kitbattlelite.item;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginItemTest {

    @Test
    void idsAreUniqueAndUsableAsPdcValues() {
        Set<String> ids = new HashSet<>();
        for (PluginItem item : PluginItem.values()) {
            assertNotNull(item.id(), item.name());
            assertFalse(item.id().isBlank(), item.name());
            assertEquals(item.id().trim(), item.id(), item.name());
            assertTrue(item.id().chars().noneMatch(Character::isWhitespace), item.name());
            assertTrue(ids.add(item.id()), "重复 id: " + item.id());
        }
    }

    @Test
    void idsAreFrozenProtocolValues() {
        // 这些 id 会写进 PDC，改名等于让旧物品失联；改之前先确认没有玩家物品还在用。
        assertEquals("select", PluginItem.SELECT.id());
        assertEquals("leave", PluginItem.LEAVE.id());
        assertEquals("back", PluginItem.BACK.id());
        assertEquals("save", PluginItem.SAVE.id());
        assertEquals("cancel", PluginItem.CANCEL.id());
        assertEquals("prev", PluginItem.PREV.id());
        assertEquals("next", PluginItem.NEXT.id());
        assertEquals("kit", PluginItem.KIT.id());
    }

    @Test
    void everyItemHasUsableMaterial() {
        for (PluginItem item : PluginItem.values()) {
            assertNotNull(item.material(), item.name());
            assertFalse(item.material().isAir(), item.name() + " 不能用空气");
        }
    }
}
