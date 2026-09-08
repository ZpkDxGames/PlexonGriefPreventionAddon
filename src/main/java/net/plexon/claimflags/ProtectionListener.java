package net.plexon.claimflags;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ProtectionListener implements Listener {
    private final PlexonClaimFlags plugin;
    private final ClaimService claims;
    private final ClaimFlagStore store;
    private final Map<UUID, Long> warningCooldown = new HashMap<>();
    private Set<String> naturalSpawnReasons = Set.of();
    private Set<String> spawnerSpawnReasons = Set.of();
    private Set<String> interactionMaterials = Set.of();
    private Set<String> interactionSuffixes = Set.of();
    private Set<String> containerMaterials = Set.of();

    public ProtectionListener(PlexonClaimFlags plugin, ClaimService claims, ClaimFlagStore store) {
        this.plugin = plugin; this.claims = claims; this.store = store; reloadMaterialCaches();
    }

    public void reloadMaterialCaches() {
        naturalSpawnReasons = uppercaseSet(plugin.getConfig().getStringList("settings.natural-spawn-reasons"));
        spawnerSpawnReasons = uppercaseSet(plugin.getConfig().getStringList("settings.spawner-spawn-reasons"));
        interactionMaterials = uppercaseSet(plugin.getConfig().getStringList("settings.interaction-materials"));
        interactionSuffixes = uppercaseSet(plugin.getConfig().getStringList("settings.interaction-material-suffixes"));
        containerMaterials = uppercaseSet(plugin.getConfig().getStringList("settings.container-materials"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Claim claim = claims.getAt(event.getLocation()); if (claim == null) return;
        String reason = event.getSpawnReason().name().toUpperCase(Locale.ROOT);
        if (naturalSpawnReasons.contains(reason) && store.effective(claim, ClaimFlag.NATURAL_MOBS)) { event.setCancelled(true); return; }
        if (spawnerSpawnReasons.contains(reason) && store.effective(claim, ClaimFlag.SPAWNER_MOBS)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) return;
        Claim victimClaim = claims.getAt(victim.getLocation()); Claim attackerClaim = claims.getAt(attacker.getLocation());
        boolean protectedPvp = (victimClaim != null && store.effective(victimClaim, ClaimFlag.PVP))
                || (attackerClaim != null && store.effective(attackerClaim, ClaimFlag.PVP));
        if (!protectedPvp || attacker.hasPermission("plexonclaimflags.bypass")) return;
        event.setCancelled(true); warn(attacker);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Claim claim = claims.getAt(event.getBlock().getLocation());
        if (claim == null || !store.effective(claim, ClaimFlag.BUILDING)) return;
        if (claims.bypassesPlayerRestriction(event.getPlayer(), claim)) return;
        event.setCancelled(true); warn(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Claim claim = claims.getAt(event.getBlock().getLocation());
        if (claim == null || !store.effective(claim, ClaimFlag.BUILDING)) return;
        if (claims.bypassesPlayerRestriction(event.getPlayer(), claim)) return;
        event.setCancelled(true); warn(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock(); if (block == null) return;
        Claim claim = claims.getAt(block.getLocation()); if (claim == null) return;
        String material = block.getType().name();
        if (material.equals("FARMLAND") && event.getAction().name().equals("PHYSICAL") && store.effective(claim, ClaimFlag.CROP_TRAMPLING)) {
            event.setCancelled(true); warn(event.getPlayer()); return;
        }
        if (claims.bypassesPlayerRestriction(event.getPlayer(), claim)) return;
        if (isContainerMaterial(material) && store.effective(claim, ClaimFlag.CONTAINERS)) {
            event.setCancelled(true); warn(event.getPlayer()); return;
        }
        if (isInteractionMaterial(material) && store.effective(claim, ClaimFlag.INTERACTIONS)) { event.setCancelled(true); warn(event.getPlayer()); }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Location location = event.getInventory().getLocation(); if (location == null || location.getWorld() == null) return;
        Block block = location.getBlock(); if (!isContainerMaterial(block.getType().name())) return;
        Claim claim = claims.getAt(location); if (claim == null || !store.effective(claim, ClaimFlag.CONTAINERS)) return;
        if (claims.bypassesPlayerRestriction(player, claim)) return;
        event.setCancelled(true); warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        boolean mobCaused = isMobCausedExplosion(event.getEntity());
        event.blockList().removeIf(block -> enabledAt(block.getLocation(), ClaimFlag.EXPLOSIONS)
                || (mobCaused && enabledAt(block.getLocation(), ClaimFlag.MOB_GRIEFING)));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) { event.blockList().removeIf(block -> enabledAt(block.getLocation(), ClaimFlag.EXPLOSIONS)); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) { if (enabledAt(event.getBlock().getLocation(), ClaimFlag.FIRE)) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) { if (enabledAt(event.getBlock().getLocation(), ClaimFlag.FIRE)) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) { if (enabledAt(event.getBlock().getLocation(), ClaimFlag.FIRE)) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Claim claim = claims.getAt(event.getBlock().getLocation()); if (claim == null) return;
        if (event.getBlock().getType().name().equals("FARMLAND") && event.getTo().name().equals("DIRT") && store.effective(claim, ClaimFlag.CROP_TRAMPLING)) {
            event.setCancelled(true); return;
        }
        if (!(event.getEntity() instanceof Player) && store.effective(claim, ClaimFlag.MOB_GRIEFING)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        Claim claim = claims.getAt(event.getBlock().getLocation()); if (claim == null) return;
        if (event.getBlock().getType().name().equals("FARMLAND") && store.effective(claim, ClaimFlag.CROP_TRAMPLING)) { event.setCancelled(true); return; }
        if (!(event.getEntity() instanceof Player) && store.effective(claim, ClaimFlag.MOB_GRIEFING)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClaimDeleted(ClaimDeletedEvent event) {
        if (!store.removeClaimTree(event.getClaim())) plugin.coreBridge().markDegraded("Claim deletion cleanup could not be persisted; in-memory state was rolled back");
    }

    private boolean enabledAt(Location location, ClaimFlag flag) { Claim claim = claims.getAt(location); return claim != null && store.effective(claim, flag); }
    private boolean isContainerMaterial(String material) { return containerMaterials.contains(material.toUpperCase(Locale.ROOT)); }
    private boolean isInteractionMaterial(String material) {
        String upper = material.toUpperCase(Locale.ROOT); if (interactionMaterials.contains(upper)) return true;
        for (String suffix : interactionSuffixes) if (upper.endsWith(suffix)) return true; return false;
    }
    private boolean isMobCausedExplosion(Entity source) {
        if (source instanceof Player) return false;
        if (source instanceof Projectile projectile) { ProjectileSource shooter = projectile.getShooter(); return shooter instanceof Entity && !(shooter instanceof Player); }
        String type = source.getType().name(); return type.equals("CREEPER") || type.equals("WITHER") || type.equals("WITHER_SKULL");
    }
    private Player attackingPlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) { ProjectileSource shooter = projectile.getShooter(); if (shooter instanceof Player player) return player; }
        return null;
    }
    private void warn(Player player) {
        long now = System.currentTimeMillis(); long last = warningCooldown.getOrDefault(player.getUniqueId(), 0L); if (now - last < 1500L) return;
        warningCooldown.put(player.getUniqueId(), now); Text.send(plugin.getConfig(), player, "messages.protected-action", Map.of());
    }
    private static Set<String> uppercaseSet(List<String> values) {
        Set<String> set = new HashSet<>(); for (String value : values) if (value != null) set.add(value.toUpperCase(Locale.ROOT)); return Set.copyOf(set);
    }
}
