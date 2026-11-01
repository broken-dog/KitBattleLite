package org.awang05.kitbattlelite.player;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerSessionTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void returnLocationIsCopiedInAndOut() {
        PlayerSession session = new PlayerSession(ID);
        Location original = new Location(null, 1.0, 64.0, 3.0, 90.0f, 0.0f);

        session.setReturnLocation(original);
        original.setX(100.0);

        Location stored = session.returnLocation();
        assertNotNull(stored);
        assertEquals(1.0, stored.getX());
        stored.setX(200.0);
        assertEquals(1.0, session.returnLocation().getX());

        session.setReturnLocation(null);
        assertNull(session.returnLocation());
    }

    @Test
    void clearArenaStateKeepsReturnPointAndSnapshotUntouched() {
        PlayerSession session = new PlayerSession(ID);
        session.setReturnLocation(new Location(null, 0, 0, 0));
        session.setArenaId("arena");
        session.setPreview(true);
        session.setKillStreak(4);
        session.setProtectionUntil(System.currentTimeMillis() + 1000);
        session.setEdit(new PlayerSession.EditSession(1, 0, null, null));

        session.clearArenaState();

        assertNull(session.arenaId());
        assertEquals(0, session.killStreak());
        assertEquals(0, session.protectionUntil());
        assertNull(session.edit());
        assertNotNull(session.returnLocation());
    }

    @Test
    void killStreakNeverGoesNegative() {
        PlayerSession session = new PlayerSession(ID);
        session.setKillStreak(3);
        session.setKillStreak(-10);

        assertEquals(0, session.killStreak());
    }

    @Test
    void editSessionValidatesPage() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerSession.EditSession(0, 0, null, null));
        assertThrows(IllegalArgumentException.class, () -> new PlayerSession.EditSession(1, -1, null, null));
    }
}
