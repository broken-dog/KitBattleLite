package org.awang05.kitbattlelite.arena;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaRegistryTest {

    @Test
    void keepsRegistrationOrderInsteadOfSortingById() {
        ArenaRegistry registry = new ArenaRegistry();
        registry.register(arena("zulu", "world_zulu"));
        registry.register(arena("alpha", "world_alpha"));
        registry.register(arena("mike", "world_mike"));

        assertEquals(List.of("zulu", "alpha", "mike"), List.copyOf(registry.ids()));
        assertEquals(List.of("zulu", "alpha", "mike"),
                registry.all().stream().map(Arena::id).toList());
    }

    @Test
    void unregisterThenRegisterAppendsToTheEnd() {
        ArenaRegistry registry = new ArenaRegistry();
        registry.register(arena("first", "world_first"));
        registry.register(arena("second", "world_second"));
        assertTrue(registry.unregister("first"));
        registry.register(arena("first", "world_first"));

        assertEquals(List.of("second", "first"), List.copyOf(registry.ids()));
    }

    @Test
    void duplicateIdIsRejectedAndOriginalIsKept() {
        ArenaRegistry registry = new ArenaRegistry();
        Arena original = arena("arena", "world_a");
        registry.register(original);

        assertThrows(IllegalStateException.class, () -> registry.register(arena("arena", "world_b")));
        assertEquals(1, registry.size());
        assertSame(original, registry.find("arena").orElseThrow());
        assertTrue(registry.byWorldName("world_b").isEmpty());
    }

    @Test
    void duplicateWorldIsRejectedAndLeavesNoPartialEntry() {
        ArenaRegistry registry = new ArenaRegistry();
        registry.register(arena("arena_a", "world_shared"));

        assertThrows(IllegalStateException.class, () -> registry.register(arena("arena_b", "world_shared")));
        assertEquals(1, registry.size());
        assertTrue(registry.find("arena_b").isEmpty());
        assertEquals("arena_a", registry.byWorldName("world_shared").orElseThrow().id());
    }

    @Test
    void unregisterDropsBothIndexes() {
        ArenaRegistry registry = new ArenaRegistry();
        registry.register(arena("arena", "world_solo"));

        assertTrue(registry.unregister("arena"));
        assertFalse(registry.unregister("arena"));
        assertFalse(registry.unregister(null));
        assertTrue(registry.find("arena").isEmpty());
        assertTrue(registry.byWorldName("world_solo").isEmpty());
        assertEquals(0, registry.size());
    }

    private static Arena arena(String id, String worldName) {
        Location location = new Location(null, 0, 64, 0);
        return new Arena(id, worldName, id, true, location, location);
    }
}
