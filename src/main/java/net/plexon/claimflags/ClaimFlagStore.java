package net.plexon.claimflags;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class ClaimFlagStore {
    private final PlexonClaimFlags plugin;
    private final ClaimService claims;
    private final Map<Long, EnumMap<ClaimFlag, Boolean>> explicit = new HashMap<>();
    private final File file;
    private YamlConfiguration yaml = new YamlConfiguration();
    private boolean persistenceHealthy = true;
    private String lastPersistenceError = "";

    public ClaimFlagStore(PlexonClaimFlags plugin, ClaimService claims) {
        this.plugin = plugin;
        this.claims = claims;
        this.file = new File(plugin.getDataFolder(), "flags.yml");
        if (!load()) throw new IllegalStateException("Could not safely load flags.yml: " + lastPersistenceError);
    }

    public synchronized boolean load() {
        YamlConfiguration loaded = new YamlConfiguration();
        Map<Long, EnumMap<ClaimFlag, Boolean>> next = new HashMap<>();
        try {
            if (file.isFile()) loaded.load(file);
            ConfigurationSection claimsSection = loaded.getConfigurationSection("claims");
            if (claimsSection != null) {
                for (String idKey : claimsSection.getKeys(false)) {
                    long id;
                    try { id = Long.parseLong(idKey); }
                    catch (NumberFormatException ignored) {
                        plugin.getLogger().warning("Ignoring invalid claim id in flags.yml: " + idKey);
                        continue;
                    }
                    ConfigurationSection flags = claimsSection.getConfigurationSection(idKey + ".flags");
                    if (flags == null) continue;
                    EnumMap<ClaimFlag, Boolean> map = new EnumMap<>(ClaimFlag.class);
                    for (ClaimFlag flag : ClaimFlag.values()) if (flags.contains(flag.key())) map.put(flag, flags.getBoolean(flag.key()));
                    if (!map.isEmpty()) next.put(id, map);
                }
            }
        } catch (IOException | InvalidConfigurationException exception) {
            markPersistenceFailure("Could not load flags.yml", exception);
            return false;
        }
        explicit.clear();
        explicit.putAll(next);
        yaml = loaded;
        persistenceHealthy = true;
        lastPersistenceError = "";
        return true;
    }

    public boolean effective(Claim claim, ClaimFlag flag) {
        if (claim == null) return false;
        Boolean direct = explicitValue(claim, flag);
        if (direct != null) return direct;
        if (claim.parent != null && plugin.getConfig().getBoolean("settings.subclaims-inherit-parent", true)) return effective(claim.parent, flag);
        return plugin.getConfig().getBoolean("defaults." + flag.key(), false);
    }

    public Boolean explicitValue(Claim claim, ClaimFlag flag) {
        if (claim == null || claim.getID() == null) return null;
        Map<ClaimFlag, Boolean> map = explicit.get(claim.getID());
        return map == null ? null : map.get(flag);
    }

    public boolean isInherited(Claim claim, ClaimFlag flag) {
        return claim != null && claim.parent != null
                && plugin.getConfig().getBoolean("settings.subclaims-inherit-parent", true)
                && explicitValue(claim, flag) == null;
    }

    public synchronized boolean set(Claim claim, ClaimFlag flag, Boolean value) {
        if (claim == null || claim.getID() == null) return false;
        Map<Long, EnumMap<ClaimFlag, Boolean>> before = copyExplicit();
        String yamlBefore = yaml.saveToString();
        long id = claim.getID();
        if (value == null) {
            EnumMap<ClaimFlag, Boolean> map = explicit.get(id);
            if (map != null) {
                map.remove(flag);
                if (map.isEmpty()) explicit.remove(id);
            }
        } else {
            explicit.computeIfAbsent(id, ignored -> new EnumMap<>(ClaimFlag.class)).put(flag, value);
        }
        persistClaim(claim);
        if (save()) return true;
        restore(before, yamlBefore);
        return false;
    }

    public synchronized boolean removeClaimTree(Claim claim) {
        if (claim == null) return true;
        Map<Long, EnumMap<ClaimFlag, Boolean>> before = copyExplicit();
        String yamlBefore = yaml.saveToString();
        removeClaimTreeFromMemory(claim);
        if (save()) return true;
        restore(before, yamlBefore);
        return false;
    }

    private void removeClaimTreeFromMemory(Claim claim) {
        if (claim == null) return;
        for (Claim child : new ArrayList<>(claim.children)) removeClaimTreeFromMemory(child);
        if (claim.getID() == null) return;
        long id = claim.getID();
        explicit.remove(id);
        yaml.set("claims." + id, null);
    }

    public synchronized boolean save() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) throw new IOException("Could not create plugin data directory");
            Path target = file.toPath();
            Path temporary = target.resolveSibling(file.getName() + ".tmp");
            Files.writeString(temporary, yaml.saveToString(), StandardCharsets.UTF_8);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
            persistenceHealthy = true;
            lastPersistenceError = "";
            return true;
        } catch (IOException exception) {
            markPersistenceFailure("Could not save flags.yml", exception);
            return false;
        }
    }

    public int explicitRecordCount() { return explicit.size(); }
    public boolean persistenceHealthy() { return persistenceHealthy; }
    public String lastPersistenceError() { return lastPersistenceError; }

    private void persistClaim(Claim claim) {
        long id = claims.safeId(claim);
        String root = "claims." + id;
        yaml.set(root + ".owner", claim.getOwnerID() == null ? "ADMIN" : claim.getOwnerID().toString());
        yaml.set(root + ".type", claim.parent == null ? "MAIN" : "SUBCLAIM");
        yaml.set(root + ".bounds", claims.boundsLabel(claim));
        Map<ClaimFlag, Boolean> map = explicit.get(id);
        yaml.set(root + ".flags", null);
        if (map != null) for (Map.Entry<ClaimFlag, Boolean> entry : map.entrySet()) yaml.set(root + ".flags." + entry.getKey().key(), entry.getValue());
        if (map == null || map.isEmpty()) yaml.set(root, null);
    }

    private Map<Long, EnumMap<ClaimFlag, Boolean>> copyExplicit() {
        Map<Long, EnumMap<ClaimFlag, Boolean>> copy = new HashMap<>();
        explicit.forEach((id, map) -> copy.put(id, new EnumMap<>(map)));
        return copy;
    }

    private void restore(Map<Long, EnumMap<ClaimFlag, Boolean>> before, String yamlBefore) {
        explicit.clear();
        explicit.putAll(before);
        try {
            YamlConfiguration restored = new YamlConfiguration();
            restored.loadFromString(yamlBefore);
            yaml = restored;
        } catch (InvalidConfigurationException impossible) {
            plugin.getLogger().log(Level.SEVERE, "Could not restore in-memory flags snapshot after a failed save", impossible);
        }
    }

    private void markPersistenceFailure(String message, Exception exception) {
        persistenceHealthy = false;
        lastPersistenceError = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        plugin.getLogger().log(Level.SEVERE, message, exception);
    }
}
