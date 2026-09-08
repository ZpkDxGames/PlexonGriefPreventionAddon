package net.plexon.claimflags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClaimFlagContractTest {
    @Test
    void preservesAllProductionFlagIdsInOrder() {
        assertEquals(List.of(
                "natural-mobs",
                "spawner-mobs",
                "pvp",
                "building",
                "interactions",
                "containers",
                "explosions",
                "fire",
                "crop-trampling",
                "mob-griefing"
        ), java.util.Arrays.stream(ClaimFlag.values()).map(ClaimFlag::key).toList());
    }

    @Test
    void legacyParsingFormsRemainAccepted() {
        assertTrue(ClaimFlag.parse("natural-mobs").isPresent());
        assertTrue(ClaimFlag.parse("natural_mobs").isPresent());
        assertTrue(ClaimFlag.parse("NATURAL_MOBS").isPresent());
    }
}
