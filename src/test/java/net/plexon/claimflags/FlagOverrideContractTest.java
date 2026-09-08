package net.plexon.claimflags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.plexon.claimflags.api.FlagOverride;
import org.junit.jupiter.api.Test;

final class FlagOverrideContractTest {
    @Test
    void explicitRepresentationsRemainStable() {
        assertEquals(Boolean.TRUE, FlagOverride.ON.explicitValue());
        assertEquals(Boolean.FALSE, FlagOverride.OFF.explicitValue());
        assertNull(FlagOverride.INHERIT.explicitValue());
        assertEquals(FlagOverride.INHERIT, FlagOverride.fromExplicit(null));
    }
}
