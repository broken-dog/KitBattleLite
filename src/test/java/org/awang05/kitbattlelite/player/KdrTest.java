package org.awang05.kitbattlelite.player;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KdrTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void freshProfileHasZeroKdr() {
        PlayerProfile profile = new PlayerProfile(ID, "Steve", 0, 0);

        assertEquals(0, profile.kills());
        assertEquals(0, profile.deaths());
        assertEquals(0.0f, profile.kdr());
    }

    @Test
    void killsWithoutDeathsEqualToKills() {
        PlayerProfile profile = new PlayerProfile(ID, "Steve", 3, 0);

        assertEquals(3.0f, profile.kdr());
    }

    @Test
    void ratioUsesDeaths() {
        PlayerProfile profile = new PlayerProfile(ID, "Steve", 1, 2);

        assertEquals(0.5f, profile.kdr(), 0.0001f);
    }

    @Test
    void countersAccumulate() {
        PlayerProfile profile = new PlayerProfile(ID, "Steve", 0, 0);
        profile.addKill();
        profile.addKill();
        profile.addDeath();

        assertEquals(2, profile.kills());
        assertEquals(1, profile.deaths());
        assertEquals(2.0f, profile.kdr());
    }

    @Test
    void negativeCountersAreClampedOnLoad() {
        PlayerProfile profile = new PlayerProfile(ID, "Steve", -5, -2);

        assertEquals(0, profile.kills());
        assertEquals(0, profile.deaths());
        assertEquals(0.0f, profile.kdr());
    }

    @Test
    void blankNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerProfile(ID, "  ", 0, 0));
    }
}
