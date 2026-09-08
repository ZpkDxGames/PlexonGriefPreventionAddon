package net.plexon.claimflags.integration.core;

import com.zpkdxgames.plexoncore.api.PlexonCoreAPI;
import com.zpkdxgames.plexoncore.api.PlexonCoreAPI.CoreVersion;
import com.zpkdxgames.plexoncore.integration.IntegrationRegistry.IntegrationState;
import com.zpkdxgames.plexoncore.module.ModuleRegistry;
import com.zpkdxgames.plexoncore.module.ModuleRegistry.ModuleDescriptor;
import com.zpkdxgames.plexoncore.module.ModuleRegistry.ModuleState;
import com.zpkdxgames.plexoncore.module.ModuleRegistry.ModuleVersionRange;
import java.time.Instant;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlexonCoreBridge implements CoreBridge {
    private static final String INTEGRATION_ID = "PLEXON_CLAIM_FLAGS";
    private static final Set<String> CAPABILITIES = Set.of("claim-flags", "griefprevention-addon", "subclaim-inheritance",
            "claimflags-api", "claim-flag-changed-event", "in-memory-protection-cache", "yaml-persistence");
    private final JavaPlugin plugin;
    private final PlexonCoreAPI core;
    private final CoreVersion version;
    private final boolean compatible;
    private boolean ownsRegistration;
    private String registrationState = "NOT_REGISTERED";
    private String detail = "PlexonCore API resolved";

    public PlexonCoreBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        RegisteredServiceProvider<PlexonCoreAPI> registration = Bukkit.getServicesManager().getRegistration(PlexonCoreAPI.class);
        if (registration == null) throw new IllegalStateException("PlexonCore API service is not registered");
        this.core = registration.getProvider();
        this.version = core.version();
        this.compatible = ModuleVersionRange.parse(SUPPORTED_API_RANGE).contains(version);
        if (!compatible) {
            registrationState = "INCOMPATIBLE";
            detail = "Core API " + version.apiVersion() + " is outside supported range " + SUPPORTED_API_RANGE;
            publish(IntegrationState.INCOMPATIBLE, detail);
        }
    }
    @Override public boolean installed() { return true; }
    @Override public boolean available() { return compatible; }
    @Override public boolean compatible() { return compatible; }
    @Override public String pluginVersion() { return version.pluginVersion(); }
    @Override public String apiVersion() { return version.apiVersion(); }
    @Override public String mode() { return compatible && ownsRegistration ? "CORE" : "STANDALONE"; }
    @Override public String registrationState() {
        if (ownsRegistration) return core.modules().find(MODULE_ID).map(descriptor -> descriptor.state().name()).orElse("NOT_REGISTERED");
        return registrationState;
    }
    @Override public String detail() {
        if (ownsRegistration) return core.modules().find(MODULE_ID).map(ModuleDescriptor::detail).orElse(detail);
        return detail;
    }
    @Override public void registerStarting() {
        if (!compatible) {
            plugin.getLogger().warning("PlexonCore API " + version.apiVersion() + " is incompatible with supported range "
                    + SUPPORTED_API_RANGE + "; PlexonClaimFlags will continue in standalone compatibility mode.");
            return;
        }
        ModuleDescriptor descriptor = new ModuleDescriptor(MODULE_ID, "PlexonClaimFlags", plugin.getName(),
                plugin.getPluginMeta().getVersion(), plugin, ModuleVersionRange.parse(SUPPORTED_API_RANGE), CAPABILITIES,
                ModuleState.STARTING, "Initializing PlexonClaimFlags", Instant.now());
        ModuleRegistry.RegistrationResult result = core.modules().register(descriptor);
        ModuleDescriptor registered = result.descriptor();
        ownsRegistration = registered != null && registered.plugin() == plugin;
        registrationState = registered == null ? "NOT_REGISTERED" : registered.state().name();
        detail = result.message();
        if (!result.success() && !ownsRegistration) {
            plugin.getLogger().warning("PlexonCore module registration rejected: " + result.message());
            publish(IntegrationState.DEGRADED, result.message());
        }
    }
    @Override public void markReady(String detail) { update(ModuleState.READY, IntegrationState.READY, detail); }
    @Override public void markDegraded(String detail) { update(ModuleState.DEGRADED, IntegrationState.DEGRADED, detail); }
    @Override public void markFailed(String detail) { update(ModuleState.FAILED, IntegrationState.FAILED, detail); }
    private void update(ModuleState moduleState, IntegrationState integrationState, String newDetail) {
        if (!compatible || !ownsRegistration) return;
        core.modules().updateState(MODULE_ID, moduleState, newDetail);
        registrationState = moduleState.name();
        detail = newDetail == null ? "" : newDetail;
        publish(integrationState, detail);
    }
    private void publish(IntegrationState state, String newDetail) {
        core.integrations().publish(INTEGRATION_ID, "PlexonClaimFlags", plugin.getPluginMeta().getVersion(), state, CAPABILITIES, newDetail);
    }
    @Override public void unregister() {
        if (compatible) publish(IntegrationState.MISSING, "PlexonClaimFlags disabled/unregistered");
        if (!ownsRegistration) return;
        core.modules().find(MODULE_ID).filter(descriptor -> descriptor.plugin() == plugin)
                .ifPresent(descriptor -> core.modules().unregister(MODULE_ID));
        ownsRegistration = false;
        registrationState = "UNREGISTERED";
    }
}
