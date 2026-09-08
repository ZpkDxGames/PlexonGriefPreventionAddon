package net.plexon.claimflags;

import net.plexon.claimflags.api.PlexonClaimFlagsAPI;
import net.plexon.claimflags.integration.core.CoreBridge;
import net.plexon.claimflags.integration.core.CoreBridgeFactory;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public final class PlexonClaimFlags extends JavaPlugin {
    private ClaimService claimService;
    private ClaimFlagStore flagStore;
    private ClaimFlagService flagService;
    private ClaimsFlagsGui gui;
    private ProtectionListener protectionListener;
    private PlexonClaimFlagsAPI publicApi;
    private CoreBridge coreBridge;
    private boolean publicApiRegistered;

    @Override public void onEnable() {
        coreBridge = CoreBridgeFactory.resolve(this);
        coreBridge.registerStarting();
        try {
            saveDefaultConfig();
            claimService = new ClaimService();
            flagStore = new ClaimFlagStore(this, claimService);
            flagService = new ClaimFlagService(this, claimService, flagStore);
            publicApi = new PlexonClaimFlagsApiImpl(this, claimService, flagStore, flagService);
            getServer().getServicesManager().register(PlexonClaimFlagsAPI.class, publicApi, this, ServicePriority.Normal);
            publicApiRegistered = true;
            gui = new ClaimsFlagsGui(this, claimService, flagStore, flagService);
            protectionListener = new ProtectionListener(this, claimService, flagStore);
            ClaimsFlagsCommand commandHandler = new ClaimsFlagsCommand(this, claimService, flagStore, flagService, gui);
            PluginCommand command = getCommand("claimsflags");
            if (command == null) throw new IllegalStateException("Command /claimsflags is missing from plugin.yml");
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
            getServer().getPluginManager().registerEvents(gui, this);
            getServer().getPluginManager().registerEvents(protectionListener, this);
            coreBridge.markReady("GriefPrevention flag engine, in-memory resolver, public API/event and diagnostics ready");
            getLogger().info("PlexonClaimFlags " + getPluginMeta().getVersion() + " enabled with GriefPrevention in " + coreBridge.mode() + " mode.");
        } catch (RuntimeException | LinkageError exception) {
            if (coreBridge != null) coreBridge.markFailed("Critical startup failure: " + exception.getClass().getSimpleName());
            getLogger().log(Level.SEVERE, "PlexonClaimFlags failed to initialize safely.", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override public void onDisable() {
        if (gui != null) gui.closeAll();
        if (flagStore != null && !flagStore.save() && coreBridge != null) coreBridge.markDegraded("Final flags.yml save failed during shutdown");
        getServer().getServicesManager().unregisterAll(this);
        publicApiRegistered = false;
        if (coreBridge != null) coreBridge.unregister();
    }

    public boolean reloadPluginConfig() {
        reloadConfig();
        if (flagStore != null && !flagStore.load()) {
            if (coreBridge != null) coreBridge.markDegraded("flags.yml reload failed; previous in-memory flag state retained");
            return false;
        }
        if (protectionListener != null) protectionListener.reloadMaterialCaches();
        return true;
    }

    public CoreBridge coreBridge() { return coreBridge; }
    public boolean publicApiRegistered() { return publicApiRegistered; }
}
