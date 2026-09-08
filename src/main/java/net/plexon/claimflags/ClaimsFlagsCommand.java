package net.plexon.claimflags;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.plexon.claimflags.api.FlagChangeResult;
import net.plexon.claimflags.api.FlagOverride;
import net.plexon.claimflags.event.FlagChangeSource;
import net.plexon.claimflags.integration.core.CoreBridge;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClaimsFlagsCommand implements CommandExecutor, TabCompleter {
    private final PlexonClaimFlags plugin;
    private final ClaimService claims;
    private final ClaimFlagStore store;
    private final ClaimFlagService mutations;
    private final ClaimsFlagsGui gui;

    public ClaimsFlagsCommand(PlexonClaimFlags plugin, ClaimService claims, ClaimFlagStore store,
                              ClaimFlagService mutations, ClaimsFlagsGui gui) {
        this.plugin = plugin; this.claims = claims; this.store = store; this.mutations = mutations; this.gui = gui;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("plexonclaimflags.admin")) { Text.send(plugin.getConfig(), sender, "messages.no-permission", Map.of()); return true; }
            if (plugin.reloadPluginConfig()) Text.send(plugin.getConfig(), sender, "messages.reloaded", Map.of()); else sendPersistenceError(sender);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("diagnostics")) {
            if (!sender.hasPermission("plexonclaimflags.admin")) { Text.send(plugin.getConfig(), sender, "messages.no-permission", Map.of()); return true; }
            diagnostics(sender); return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("PlexonClaimFlags: only players can manage claim flags. Console may use /claimsflags reload or diagnostics."); return true;
        }
        if (!player.hasPermission("plexonclaimflags.use")) { Text.send(plugin.getConfig(), player, "messages.no-permission", Map.of()); return true; }
        Claim current = claims.getAt(player.getLocation());
        if (current == null) { Text.send(plugin.getConfig(), player, "messages.no-claim", Map.of()); return true; }
        if (!claims.canManage(player, current)) {
            Text.send(plugin.getConfig(), player, current.isAdminClaim() ? "messages.admin-claim-denied" : "messages.not-owner", Map.of()); return true;
        }
        if (args.length == 0) { gui.openFlags(player, current); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "areas", "area" -> gui.openAreas(player, claims.parentOf(current), 0);
            case "parent" -> gui.openFlags(player, claims.parentOf(current));
            case "sub", "subclaim" -> openSubclaim(player, current, args);
            case "list" -> { Claim target = resolveTarget(player, current, args.length >= 2 ? args[1] : "current"); if (target != null) listFlags(player, target); }
            case "set" -> setFlag(player, current, args);
            case "help" -> sendHelp(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void openSubclaim(Player player, Claim current, String[] args) {
        if (args.length < 2) { gui.openAreas(player, claims.parentOf(current), 0); return; }
        int index;
        try { index = Integer.parseInt(args[1]) - 1; }
        catch (NumberFormatException ex) { Text.send(plugin.getConfig(), player, "messages.invalid-subclaim", Map.of()); return; }
        List<Claim> children = claims.subclaimsOf(current);
        if (index < 0 || index >= children.size()) { Text.send(plugin.getConfig(), player, "messages.invalid-subclaim", Map.of()); return; }
        gui.openFlags(player, children.get(index));
    }

    private void setFlag(Player player, Claim current, String[] args) {
        if (args.length < 3) { player.sendMessage(Text.color("&7Usage: &f/claimsflags set <flag> <on|off|inherit> [current|parent|sub:<number>]")); return; }
        Claim claim = resolveTarget(player, current, args.length >= 4 ? args[3] : "current");
        if (claim == null) return;
        ClaimFlag flag = ClaimFlag.parse(args[1]).orElse(null);
        if (flag == null) { Text.send(plugin.getConfig(), player, "messages.invalid-flag", Map.of()); return; }
        String rawState = args[2].toLowerCase(Locale.ROOT);
        FlagOverride override;
        if (rawState.equals("inherit")) {
            if (claim.parent == null || !plugin.getConfig().getBoolean("settings.subclaims-inherit-parent", true)) {
                Text.send(plugin.getConfig(), player, "messages.invalid-state", Map.of()); return;
            }
            override = FlagOverride.INHERIT;
        } else {
            override = switch (rawState) {
                case "on", "true", "enabled", "enable" -> FlagOverride.ON;
                case "off", "false", "disabled", "disable" -> FlagOverride.OFF;
                default -> null;
            };
            if (override == null) { Text.send(plugin.getConfig(), player, "messages.invalid-state", Map.of()); return; }
        }
        FlagChangeResult result = mutations.set(player, claim, flag, override, FlagChangeSource.COMMAND);
        if (result.status() == FlagChangeResult.Status.PERSISTENCE_FAILED) { sendPersistenceError(player); return; }
        if (result.status() == FlagChangeResult.Status.NOT_AUTHORIZED) {
            Text.send(plugin.getConfig(), player, claim.isAdminClaim() ? "messages.admin-claim-denied" : "messages.not-owner", Map.of()); return;
        }
        if (result.status() == FlagChangeResult.Status.INVALID_CLAIM) { Text.send(plugin.getConfig(), player, "messages.no-claim", Map.of()); return; }
        if (override == FlagOverride.INHERIT) Text.send(plugin.getConfig(), player, "messages.inherited", Map.of("flag", displayName(flag)));
        else Text.send(plugin.getConfig(), player, "messages.changed", changePlaceholders(flag, override == FlagOverride.ON, claim));
    }

    private void listFlags(Player player, Claim claim) {
        player.sendMessage(Text.color("&8&m----------------------------------------"));
        player.sendMessage(Text.color("&#41C902&lClaim Flags &8• &f" + claims.areaLabel(claim)));
        player.sendMessage(Text.color("&7Left click in the GUI toggles. Right click returns a subclaim flag to inheritance."));
        for (ClaimFlag flag : ClaimFlag.values()) {
            boolean effective = store.effective(claim, flag);
            boolean inherited = store.isInherited(claim, flag);
            player.sendMessage(Text.color(" &8• &f" + flag.key() + " &8— " + (effective ? "&aENABLED" : "&cDISABLED") + (inherited ? " &b(inherited)" : "")));
        }
        player.sendMessage(Text.color("&8&m----------------------------------------"));
    }

    private void sendHelp(Player player) {
        player.sendMessage(Text.color("&#41C902&lClaim Flags &8» &7Commands"));
        player.sendMessage(Text.color("&f/claimsflags &8- &7Open flags for the area you are standing in."));
        player.sendMessage(Text.color("&f/claimsflags areas &8- &7Choose the parent claim or one of its subdivisions."));
        player.sendMessage(Text.color("&f/claimsflags list [current|parent|sub:<#>] &8- &7Show effective flag values."));
        player.sendMessage(Text.color("&f/claimsflags set <flag> <on|off|inherit> [current|parent|sub:<#>] &8- &7Set a flag."));
        player.sendMessage(Text.color("&f/claimsflags parent &8- &7Open the parent claim."));
        player.sendMessage(Text.color("&f/claimsflags sub <number> &8- &7Open a subdivision."));
    }

    private void diagnostics(CommandSender sender) {
        CoreBridge core = plugin.coreBridge();
        String gpVersion = GriefPrevention.instance == null ? "MISSING" : GriefPrevention.instance.getPluginMeta().getVersion();
        sender.sendMessage(Text.color("&#41C902&lPlexonClaimFlags Diagnostics"));
        diagnostic(sender, "Plugin", plugin.getPluginMeta().getVersion());
        diagnostic(sender, "Paper", Bukkit.getVersion());
        diagnostic(sender, "Java", Runtime.version().toString());
        diagnostic(sender, "Mode", core.mode());
        diagnostic(sender, "Core plugin/API", core.pluginVersion() + " / " + core.apiVersion());
        diagnostic(sender, "Supported Core", CoreBridge.SUPPORTED_API_RANGE);
        diagnostic(sender, "Module", core.registrationState());
        diagnostic(sender, "GriefPrevention", GriefPrevention.instance == null ? "MISSING" : "READY " + gpVersion);
        diagnostic(sender, "Flags loaded", Integer.toString(ClaimFlag.values().length));
        diagnostic(sender, "Explicit claim/subclaim records", Integer.toString(store.explicitRecordCount()));
        diagnostic(sender, "Inheritance", plugin.getConfig().getBoolean("settings.subclaims-inherit-parent", true) ? "ENABLED" : "DISABLED");
        diagnostic(sender, "Persistence", "flags.yml " + (store.persistenceHealthy() ? "READY" : "DEGRADED"));
        diagnostic(sender, "Dirty/pending", "0");
        diagnostic(sender, "Public API", plugin.publicApiRegistered() ? "REGISTERED" : "UNREGISTERED");
        diagnostic(sender, "Flag change event", "READY");
        diagnostic(sender, "Hot-path storage", "MEMORY");
        if (!store.persistenceHealthy() && !store.lastPersistenceError().isBlank()) diagnostic(sender, "Persistence detail", store.lastPersistenceError());
    }

    private static void diagnostic(CommandSender sender, String key, String value) { sender.sendMessage(Text.color("&8• &7" + key + ": &f" + value)); }

    private void sendPersistenceError(CommandSender sender) {
        String configured = plugin.getConfig().getString("messages.persistence-error");
        if (configured == null || configured.isBlank()) sender.sendMessage(Text.color("&#41C902&lClaim Flags &8» &cThe flag change could not be saved safely. No durable change was applied."));
        else Text.send(plugin.getConfig(), sender, "messages.persistence-error", Map.of());
    }

    private Claim resolveTarget(Player player, Claim current, String selector) {
        String target = selector == null ? "current" : selector.toLowerCase(Locale.ROOT);
        if (target.equals("current")) return current;
        if (target.equals("parent") || target.equals("main")) return claims.parentOf(current);
        if (target.startsWith("sub:")) {
            int index;
            try { index = Integer.parseInt(target.substring(4)) - 1; }
            catch (NumberFormatException ex) { Text.send(plugin.getConfig(), player, "messages.invalid-target", Map.of()); return null; }
            List<Claim> children = claims.subclaimsOf(current);
            if (index < 0 || index >= children.size()) { Text.send(plugin.getConfig(), player, "messages.invalid-subclaim", Map.of()); return null; }
            return children.get(index);
        }
        Text.send(plugin.getConfig(), player, "messages.invalid-target", Map.of()); return null;
    }

    private List<String> targetSuggestions(CommandSender sender, String input) {
        List<String> targets = new ArrayList<>(List.of("current", "parent"));
        if (sender instanceof Player player) {
            Claim current = claims.getAt(player.getLocation());
            if (current != null && claims.canManage(player, current)) {
                List<Claim> children = claims.subclaimsOf(current);
                for (int i = 1; i <= children.size(); i++) targets.add("sub:" + i);
            }
        }
        return startsWith(targets, input);
    }

    private Map<String, String> changePlaceholders(ClaimFlag flag, boolean enabled, Claim claim) {
        Map<String, String> values = new HashMap<>();
        values.put("flag", displayName(flag)); values.put("area", claims.areaLabel(claim)); values.put("state", enabled ? "&aENABLED" : "&cDISABLED"); return values;
    }
    private String displayName(ClaimFlag flag) { return plugin.getConfig().getString("flag-names." + flag.key(), flag.key()); }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>(Arrays.asList("areas", "list", "set", "parent", "sub", "help"));
            if (sender.hasPermission("plexonclaimflags.admin")) { base.add("reload"); base.add("diagnostics"); }
            return startsWith(base, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) return startsWith(Arrays.stream(ClaimFlag.values()).map(ClaimFlag::key).toList(), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) return startsWith(List.of("on", "off", "inherit"), args[2]);
        if (args.length == 4 && args[0].equalsIgnoreCase("set")) return targetSuggestions(sender, args[3]);
        if (args.length == 2 && args[0].equalsIgnoreCase("list")) return targetSuggestions(sender, args[1]);
        return Collections.emptyList();
    }

    private static List<String> startsWith(List<String> candidates, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).sorted().toList();
    }
}
