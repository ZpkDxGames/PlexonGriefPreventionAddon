package net.plexon.claimflags;

import me.ryanhamshire.GriefPrevention.Claim;
import net.plexon.claimflags.api.ClaimFlagView;
import net.plexon.claimflags.api.FlagChangeResult;
import net.plexon.claimflags.api.FlagOverride;
import net.plexon.claimflags.api.PlexonClaimFlagsAPI;
import net.plexon.claimflags.event.FlagChangeSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class PlexonClaimFlagsApiImpl implements PlexonClaimFlagsAPI {
    private final PlexonClaimFlags plugin;
    private final ClaimService claims;
    private final ClaimFlagStore store;
    private final ClaimFlagService mutations;
    PlexonClaimFlagsApiImpl(PlexonClaimFlags plugin, ClaimService claims, ClaimFlagStore store, ClaimFlagService mutations) {
        this.plugin = plugin; this.claims = claims; this.store = store; this.mutations = mutations;
    }
    @Override public Optional<ClaimFlagView> flag(String flagId) { requirePrimaryThread(); return ClaimFlag.parse(flagId).map(this::view); }
    @Override public Collection<ClaimFlagView> flags() { requirePrimaryThread(); return java.util.Arrays.stream(ClaimFlag.values()).map(this::view).toList(); }
    @Override public boolean effectiveValue(long claimId, String flagId) { requirePrimaryThread(); return store.effective(requireClaim(claimId), requireFlag(flagId)); }
    @Override public Optional<Boolean> explicitValue(long claimId, String flagId) { requirePrimaryThread(); return Optional.ofNullable(store.explicitValue(requireClaim(claimId), requireFlag(flagId))); }
    @Override public Map<String, Boolean> effectiveValues(long claimId) {
        requirePrimaryThread(); Claim claim = requireClaim(claimId); Map<String, Boolean> values = new LinkedHashMap<>();
        for (ClaimFlag flag : ClaimFlag.values()) values.put(flag.key(), store.effective(claim, flag));
        return Map.copyOf(values);
    }
    @Override public FlagChangeResult setExplicit(Player actor, long claimId, String flagId, FlagOverride value) {
        if (!Bukkit.isPrimaryThread()) return new FlagChangeResult(FlagChangeResult.Status.NOT_PRIMARY_THREAD, claimId,
                flagId == null ? "" : flagId, null, null, null, "Claim flag mutations must run on the primary server thread");
        Claim claim = claims.getById(claimId);
        if (claim == null || !claim.inDataStore) return new FlagChangeResult(FlagChangeResult.Status.INVALID_CLAIM, claimId,
                flagId == null ? "" : flagId, null, null, null, "Claim is unavailable or no longer stored by GriefPrevention");
        ClaimFlag flag = ClaimFlag.parse(flagId).orElse(null);
        if (flag == null) return new FlagChangeResult(FlagChangeResult.Status.INVALID_FLAG, claimId,
                flagId == null ? "" : flagId, null, null, null, "Unknown claim flag");
        return mutations.set(actor, claim, flag, value, FlagChangeSource.API);
    }
    private ClaimFlagView view(ClaimFlag flag) {
        return new ClaimFlagView(flag.key(), plugin.getConfig().getString("flag-names." + flag.key(), flag.key()), flag.iconMaterial(), plugin.getConfig().getBoolean("defaults." + flag.key(), false));
    }
    private Claim requireClaim(long claimId) {
        Claim claim = claims.getById(claimId);
        if (claim == null || !claim.inDataStore) throw new IllegalArgumentException("Unknown GriefPrevention claim id: " + claimId);
        return claim;
    }
    private static ClaimFlag requireFlag(String flagId) { return ClaimFlag.parse(flagId).orElseThrow(() -> new IllegalArgumentException("Unknown claim flag: " + flagId)); }
    private static void requirePrimaryThread() { if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("PlexonClaimFlags API reads that resolve GriefPrevention claims must run on the primary server thread"); }
}
