package net.plexon.claimflags;

import me.ryanhamshire.GriefPrevention.Claim;
import net.plexon.claimflags.api.FlagChangeResult;
import net.plexon.claimflags.api.FlagOverride;
import net.plexon.claimflags.event.FlagChangeSource;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClaimsFlagsGui implements Listener {
    private static final int[] FLAG_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
    private static final int[] AREA_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private final PlexonClaimFlags plugin;
    private final ClaimService claims;
    private final ClaimFlagStore store;
    private final ClaimFlagService mutations;
    private final Map<Integer, ClaimFlag> flagBySlot = new HashMap<>();

    public ClaimsFlagsGui(PlexonClaimFlags plugin, ClaimService claims, ClaimFlagStore store, ClaimFlagService mutations) {
        this.plugin = plugin; this.claims = claims; this.store = store; this.mutations = mutations;
        ClaimFlag[] flags = ClaimFlag.values();
        for (int i = 0; i < flags.length && i < FLAG_SLOTS.length; i++) flagBySlot.put(FLAG_SLOTS[i], flags[i]);
    }

    public void openFlags(Player player, Claim claim) {
        if (!validateManage(player, claim)) return;
        String title = plugin.getConfig().getString("gui.title", "&8Claim Flags &7• &f%area%").replace("%area%", claims.areaLabel(claim));
        FlagsHolder holder = new FlagsHolder(claim);
        Inventory inventory = Bukkit.createInventory(holder, 54, Text.color(title)); holder.inventory = inventory; fill(inventory);
        inventory.setItem(4, item(material("gui.info-material", "BOOK"), "&#41C902&l" + claims.areaLabel(claim),
                List.of("&7Owner: &f" + claims.ownerLabel(claim), "&7Bounds: &f" + claims.boundsLabel(claim),
                        claim.parent == null ? "&7Type: &fMain Claim" : "&7Type: &fSubdivision", "", "&8Claim ID: " + claims.safeId(claim))));
        for (Map.Entry<Integer, ClaimFlag> entry : flagBySlot.entrySet()) inventory.setItem(entry.getKey(), flagItem(claim, entry.getValue()));
        if (claim.parent != null) inventory.setItem(45, item(material("gui.parent-material", "GRASS_BLOCK"), "&a&lParent Claim",
                List.of("&7Configure the main claim", "&7that contains this subdivision.", "", "&eClick to open")));
        inventory.setItem(49, item(material("gui.areas-material", "MAP"), "&#41C902&lClaim Areas",
                List.of("&7Choose the main claim or any", "&7subdivision inside it.", "", "&eClick to browse")));
        inventory.setItem(53, item(material("gui.close-material", "BARRIER"), "&c&lClose", List.of("&7Close this menu.")));
        player.openInventory(inventory);
    }

    public void openAreas(Player player, Claim parent, int page) {
        parent = claims.parentOf(parent);
        if (!validateManage(player, parent)) return;
        List<Claim> children = claims.subclaimsOf(parent);
        int pages = Math.max(1, (int) Math.ceil(children.size() / (double) AREA_SLOTS.length));
        int safePage = Math.max(0, Math.min(page, pages - 1));
        String title = plugin.getConfig().getString("gui.areas-title", "&8Claim Areas &7• &f%owner%").replace("%owner%", claims.ownerLabel(parent));
        AreasHolder holder = new AreasHolder(parent, safePage);
        Inventory inventory = Bukkit.createInventory(holder, 54, Text.color(title)); holder.inventory = inventory; fill(inventory);
        inventory.setItem(4, item(material("gui.parent-material", "GRASS_BLOCK"), "&#41C902&lMain Claim #" + claims.safeId(parent),
                List.of("&7" + claims.boundsLabel(parent), "", "&eClick to configure")));
        int offset = safePage * AREA_SLOTS.length;
        for (int i = 0; i < AREA_SLOTS.length; i++) {
            int childIndex = offset + i; if (childIndex >= children.size()) break; Claim child = children.get(childIndex);
            inventory.setItem(AREA_SLOTS[i], item(material("gui.subclaim-material", "OAK_FENCE"), "&a&lSubclaim #" + (childIndex + 1),
                    List.of("&7" + claims.boundsLabel(child), "&7Claim ID: &f" + claims.safeId(child), "", "&eClick to configure")));
        }
        if (safePage > 0) inventory.setItem(45, item(Material.ARROW, "&ePrevious Page", List.of("&7Page " + safePage + " of " + pages)));
        inventory.setItem(49, item(material("gui.info-material", "BOOK"), "&f&lArea Browser",
                List.of("&7Subclaims: &f" + children.size(), "&7Page: &f" + (safePage + 1) + "&7/&f" + pages)));
        if (safePage + 1 < pages) inventory.setItem(53, item(Material.ARROW, "&eNext Page", List.of("&7Page " + (safePage + 2) + " of " + pages)));
        else inventory.setItem(53, item(material("gui.close-material", "BARRIER"), "&c&lClose", List.of("&7Close this menu.")));
        player.openInventory(inventory);
    }

    @EventHandler public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory(); InventoryHolder rawHolder = top.getHolder();
        if (!(rawHolder instanceof FlagsHolder) && !(rawHolder instanceof AreasHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) return;
        if (rawHolder instanceof FlagsHolder holder) handleFlagsClick(player, holder, event);
        else if (rawHolder instanceof AreasHolder holder) handleAreasClick(player, holder, event.getRawSlot());
    }

    @EventHandler public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof FlagsHolder || holder instanceof AreasHolder) event.setCancelled(true);
    }

    private void handleFlagsClick(Player player, FlagsHolder holder, InventoryClickEvent event) {
        Claim claim = holder.claim;
        if (!validateManage(player, claim)) { player.closeInventory(); return; }
        int slot = event.getRawSlot(); ClaimFlag flag = flagBySlot.get(slot);
        if (flag != null) {
            FlagOverride override = event.isRightClick() ? FlagOverride.INHERIT : (store.effective(claim, flag) ? FlagOverride.OFF : FlagOverride.ON);
            FlagChangeResult result = mutations.set(player, claim, flag, override, FlagChangeSource.GUI);
            if (result.status() == FlagChangeResult.Status.PERSISTENCE_FAILED) {
                String configured = plugin.getConfig().getString("messages.persistence-error");
                if (configured == null || configured.isBlank()) player.sendMessage(Text.color("&#41C902&lClaim Flags &8» &cThe flag change could not be saved safely. No durable change was applied."));
                else Text.send(plugin.getConfig(), player, "messages.persistence-error", Map.of());
            }
            openFlags(player, claim); return;
        }
        if (slot == 45 && claim.parent != null) openFlags(player, claim.parent);
        else if (slot == 49) openAreas(player, claims.parentOf(claim), 0);
        else if (slot == 53) player.closeInventory();
    }

    private void handleAreasClick(Player player, AreasHolder holder, int slot) {
        Claim parent = holder.parent;
        if (!validateManage(player, parent)) { player.closeInventory(); return; }
        if (slot == 4) { openFlags(player, parent); return; }
        if (slot == 45 && holder.page > 0) { openAreas(player, parent, holder.page - 1); return; }
        if (slot == 53) {
            List<Claim> children = claims.subclaimsOf(parent); int pages = Math.max(1, (int) Math.ceil(children.size() / (double) AREA_SLOTS.length));
            if (holder.page + 1 < pages) openAreas(player, parent, holder.page + 1); else player.closeInventory(); return;
        }
        int visibleIndex = indexOf(AREA_SLOTS, slot); if (visibleIndex < 0) return;
        int childIndex = holder.page * AREA_SLOTS.length + visibleIndex; List<Claim> children = claims.subclaimsOf(parent);
        if (childIndex >= 0 && childIndex < children.size()) openFlags(player, children.get(childIndex));
    }

    private ItemStack flagItem(Claim claim, ClaimFlag flag) {
        boolean enabled = store.effective(claim, flag); boolean inherited = store.isInherited(claim, flag);
        String display = plugin.getConfig().getString("flag-names." + flag.key(), flag.key()); List<String> lore = new ArrayList<>();
        lore.add("&7Protection: " + (enabled ? "&a&lON" : "&c&lOFF"));
        if (inherited) lore.add("&7Source: &bInherited from parent");
        else if (store.explicitValue(claim, flag) != null) lore.add("&7Source: &fExplicit override"); else lore.add("&7Source: &fServer default");
        lore.add(""); lore.add(enabled ? "&aThis behavior is prevented." : "&7This addon does not block this behavior."); lore.add("");
        lore.add("&eLeft-click &7to toggle");
        lore.add(claim.parent != null ? "&bRight-click &7to inherit from parent" : "&bRight-click &7to reset to server default");
        return item(material(flag.iconMaterial()), (enabled ? "&a" : "&c") + "&l" + display, lore);
    }

    private boolean validateManage(Player player, Claim claim) {
        if (claim == null || !claim.inDataStore) { Text.send(plugin.getConfig(), player, "messages.no-claim", Map.of()); return false; }
        if (claims.canManage(player, claim)) return true;
        Text.send(plugin.getConfig(), player, claim.isAdminClaim() ? "messages.admin-claim-denied" : "messages.not-owner", Map.of()); return false;
    }

    public void closeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof FlagsHolder || holder instanceof AreasHolder) player.closeInventory();
        }
    }

    private void fill(Inventory inventory) { ItemStack filler = item(material("gui.filler", "BLACK_STAINED_GLASS_PANE"), " ", List.of()); for (int i=0;i<inventory.getSize();i++) inventory.setItem(i, filler); }
    private Material material(String configPath, String fallback) { return material(plugin.getConfig().getString(configPath, fallback)); }
    private Material material(String name) { Material material = name == null ? null : Material.matchMaterial(name); return material == null ? Material.STONE : material; }
    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material); ItemMeta meta = stack.getItemMeta();
        if (meta != null) { meta.setDisplayName(Text.color(name)); if (lore != null && !lore.isEmpty()) meta.setLore(lore.stream().map(Text::color).toList()); stack.setItemMeta(meta); }
        return stack;
    }
    private static int indexOf(int[] values, int needle) { for (int i=0;i<values.length;i++) if (values[i] == needle) return i; return -1; }

    private static final class FlagsHolder implements InventoryHolder {
        private final Claim claim; private Inventory inventory; private FlagsHolder(Claim claim) { this.claim = claim; }
        @Override public Inventory getInventory() { return inventory; }
    }
    private static final class AreasHolder implements InventoryHolder {
        private final Claim parent; private final int page; private Inventory inventory;
        private AreasHolder(Claim parent, int page) { this.parent = parent; this.page = page; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
