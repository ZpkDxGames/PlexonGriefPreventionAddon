package net.plexon.claimflags;

import me.ryanhamshire.GriefPrevention.Claim;
import net.plexon.claimflags.api.FlagChangeResult;
import net.plexon.claimflags.api.FlagOverride;
import net.plexon.claimflags.event.FlagChangeSource;
import net.plexon.claimflags.event.PlexonClaimFlagChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.Objects;
import java.util.UUID;

public final class ClaimFlagService {
    private final PlexonClaimFlags plugin;
    private final ClaimService claims;
    private final ClaimFlagStore store;
    public ClaimFlagService(PlexonClaimFlags plugin, ClaimService claims, ClaimFlagStore store) {
        this.plugin = plugin; this.claims = claims; this.store = store;
    }
    public FlagChangeResult set(Player actor, Claim claim, ClaimFlag flag, FlagOverride override, FlagChangeSource source) {
        if (!Bukkit.isPrimaryThread()) return result(FlagChangeResult.Status.NOT_PRIMARY_THREAD, claims.safeId(claim), flag, null, null, null, "Claim flag mutations must run on the primary server thread");
        if (actor == null || claim == null || !claim.inDataStore || claim.getID() == null) return result(FlagChangeResult.Status.INVALID_CLAIM, claims.safeId(claim), flag, null, null, null, "Claim is unavailable or no longer stored by GriefPrevention");
        if (flag == null || override == null) return result(FlagChangeResult.Status.INVALID_FLAG, claims.safeId(claim), flag, null, null, null, "Flag or override is invalid");
        if (!claims.canManage(actor, claim)) return result(FlagChangeResult.Status.NOT_AUTHORIZED, claims.safeId(claim), flag, store.effective(claim, flag), null, null, "Actor is not authorized to configure this claim");
        Boolean oldExplicit = store.explicitValue(claim, flag);
        Boolean newExplicit = override.explicitValue();
        boolean oldEffective = store.effective(claim, flag);
        if (Objects.equals(oldExplicit, newExplicit)) return result(FlagChangeResult.Status.NO_CHANGE, claims.safeId(claim), flag, oldEffective, null, null, "Explicit flag value is already unchanged");
        if (!store.set(claim, flag, newExplicit)) {
            plugin.coreBridge().markDegraded("flags.yml persistence failed; in-memory mutation rolled back");
            return result(FlagChangeResult.Status.PERSISTENCE_FAILED, claims.safeId(claim), flag, oldEffective, null, null, "flags.yml could not be durably updated");
        }
        boolean newEffective = store.effective(claim, flag);
        String transactionId = UUID.randomUUID().toString();
        String eventId = transactionId + ":flag:" + flag.key();
        Long parentId = claim.parent == null || claim.parent.getID() == null ? null : claim.parent.getID();
        FlagChangeSource actualSource = source == null ? FlagChangeSource.API : source;
        Bukkit.getPluginManager().callEvent(new PlexonClaimFlagChangedEvent(actor, claim.getID(), parentId, claim.parent != null,
                flag.key(), FlagOverride.fromExplicit(oldExplicit), FlagOverride.fromExplicit(newExplicit), oldEffective,
                newEffective, eventId, transactionId, actualSource));
        return result(FlagChangeResult.Status.SUCCESS, claim.getID(), flag, newEffective, transactionId, eventId, "Claim flag updated successfully");
    }
    private static FlagChangeResult result(FlagChangeResult.Status status, long claimId, ClaimFlag flag, Boolean effective,
            String transactionId, String eventId, String message) {
        return new FlagChangeResult(status, claimId, flag == null ? "" : flag.key(), effective, transactionId, eventId, message);
    }
}
