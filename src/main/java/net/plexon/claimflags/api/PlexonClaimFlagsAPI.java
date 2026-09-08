package net.plexon.claimflags.api;

import org.bukkit.entity.Player;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface PlexonClaimFlagsAPI {
    Optional<ClaimFlagView> flag(String flagId);
    Collection<ClaimFlagView> flags();
    boolean effectiveValue(long claimId, String flagId);
    Optional<Boolean> explicitValue(long claimId, String flagId);
    Map<String, Boolean> effectiveValues(long claimId);
    FlagChangeResult setExplicit(Player actor, long claimId, String flagId, FlagOverride value);
}
