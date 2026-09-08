package net.plexon.claimflags.integration.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;

final class CoreAbsentLinkageTest {
    @Test
    void standaloneBoundaryLoadsWithoutResolvingCoreRuntimeTypes() {
        assertDoesNotThrow(() -> Class.forName("net.plexon.claimflags.integration.core.CoreBridge"));
        assertDoesNotThrow(() -> Class.forName("net.plexon.claimflags.integration.core.StandaloneCoreBridge"));
    }
}
