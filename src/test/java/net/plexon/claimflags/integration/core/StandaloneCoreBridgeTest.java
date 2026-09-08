package net.plexon.claimflags.integration.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

final class StandaloneCoreBridgeTest {
    @Test
    void absentCoreUsesStandaloneMode() {
        CoreBridge bridge = new StandaloneCoreBridge(false, "-", "-", "not installed");
        assertFalse(bridge.installed());
        assertFalse(bridge.available());
        assertFalse(bridge.compatible());
        assertEquals("STANDALONE", bridge.mode());
        assertEquals("NOT_INSTALLED", bridge.registrationState());
    }

    @Test
    void supportedRangeAndModuleIdAreStable() {
        assertEquals(">=1.0 <2.0", CoreBridge.SUPPORTED_API_RANGE);
        assertEquals("claimflags", CoreBridge.MODULE_ID);
    }
}
