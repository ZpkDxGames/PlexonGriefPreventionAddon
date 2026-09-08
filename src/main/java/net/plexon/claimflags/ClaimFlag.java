package net.plexon.claimflags;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ClaimFlag {
    NATURAL_MOBS("natural-mobs", "ZOMBIE_HEAD"),
    SPAWNER_MOBS("spawner-mobs", "SPAWNER"),
    PVP("pvp", "DIAMOND_SWORD"),
    BUILDING("building", "DIAMOND_PICKAXE"),
    INTERACTIONS("interactions", "OAK_DOOR"),
    CONTAINERS("containers", "CHEST"),
    EXPLOSIONS("explosions", "TNT"),
    FIRE("fire", "FLINT_AND_STEEL"),
    CROP_TRAMPLING("crop-trampling", "WHEAT"),
    MOB_GRIEFING("mob-griefing", "CREEPER_HEAD");

    private final String key;
    private final String iconMaterial;

    ClaimFlag(String key, String iconMaterial) {
        this.key = key;
        this.iconMaterial = iconMaterial;
    }

    public String key() { return key; }
    public String iconMaterial() { return iconMaterial; }

    public static Optional<ClaimFlag> parse(String input) {
        if (input == null) return Optional.empty();
        String normalized = input.toLowerCase(Locale.ROOT).replace('_', '-');
        return Arrays.stream(values())
                .filter(flag -> flag.key.equals(normalized) || flag.name().equalsIgnoreCase(input))
                .findFirst();
    }
}
