package net.plexon.claimflags;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClaimService {
    public Claim getAt(Location location) {
        if (location == null || GriefPrevention.instance == null || GriefPrevention.instance.dataStore == null) return null;
        return GriefPrevention.instance.dataStore.getClaimAt(location, true, null);
    }

    public Claim getById(long claimId) {
        if (claimId < 0 || GriefPrevention.instance == null || GriefPrevention.instance.dataStore == null) return null;
        return GriefPrevention.instance.dataStore.getClaim(claimId);
    }

    public Claim parentOf(Claim claim) { return claim != null && claim.parent != null ? claim.parent : claim; }

    public List<Claim> subclaimsOf(Claim claim) {
        Claim parent = parentOf(claim);
        return parent == null ? List.of() : new ArrayList<>(parent.children);
    }

    public boolean canManage(Player player, Claim claim) {
        if (player == null || claim == null) return false;
        if (claim.isAdminClaim()) return player.hasPermission("plexonclaimflags.adminclaims");
        UUID owner = claim.getOwnerID();
        return (owner != null && owner.equals(player.getUniqueId())) || player.hasPermission("plexonclaimflags.admin");
    }

    public boolean bypassesPlayerRestriction(Player player, Claim claim) {
        if (player == null) return false;
        if (player.hasPermission("plexonclaimflags.bypass")) return true;
        UUID owner = claim == null ? null : claim.getOwnerID();
        return owner != null && owner.equals(player.getUniqueId());
    }

    public String areaLabel(Claim claim) {
        if (claim == null) return "Unknown";
        if (claim.parent == null) return "Main Claim #" + safeId(claim);
        Claim parent = claim.parent;
        int index = parent.children.indexOf(claim);
        return "Subclaim #" + (index < 0 ? "?" : index + 1) + " (Claim #" + safeId(parent) + ")";
    }

    public String ownerLabel(Claim claim) {
        if (claim == null) return "Unknown";
        return claim.isAdminClaim() ? "Administrative Claim" : claim.getOwnerName();
    }

    public String boundsLabel(Claim claim) {
        if (claim == null) return "Unknown";
        Location a = claim.getLesserBoundaryCorner();
        Location b = claim.getGreaterBoundaryCorner();
        return a.getWorld().getName() + " " + a.getBlockX() + "," + a.getBlockZ() + " → " + b.getBlockX() + "," + b.getBlockZ();
    }

    public long safeId(Claim claim) {
        Long id = claim == null ? null : claim.getID();
        return id == null ? -1L : id;
    }
}
