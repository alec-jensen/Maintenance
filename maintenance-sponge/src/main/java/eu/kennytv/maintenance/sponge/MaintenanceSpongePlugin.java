package eu.kennytv.maintenance.sponge;

import com.google.inject.Inject;
import eu.kennytv.maintenance.api.IMaintenance;
import eu.kennytv.maintenance.api.ISettings;
import eu.kennytv.maintenance.api.sponge.MaintenanceSpongeAPI;
import eu.kennytv.maintenance.core.MaintenanceModePlugin;
import eu.kennytv.maintenance.core.Settings;
import eu.kennytv.maintenance.core.hook.ServerListPlusHook;
import eu.kennytv.maintenance.core.runnable.MaintenanceRunnable;
import eu.kennytv.maintenance.core.util.ServerType;
import eu.kennytv.maintenance.sponge.command.MaintenanceSpongeCommand;
import eu.kennytv.maintenance.sponge.listener.GameReloadListener;
import eu.kennytv.maintenance.sponge.listener.ClientPingServerListener;
import eu.kennytv.maintenance.sponge.listener.ClientConnectionListener;
import eu.kennytv.maintenance.sponge.util.LoggerWrapper;
import net.minecrell.serverlistplus.core.plugin.ServerListPlusPlugin;
import org.bstats.sponge.Metrics2;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author KennyTV
 * @since 3.0
 */
@Plugin(id = "maintenancesponge", name = "MaintenanceSponge", version = "3.0-SNAPSHOT", authors = "KennyTV",
        description = "Enable maintenance mode with a custom maintenance motd and icon.", url = "https://www.spigotmc.org/resources/maintenancemode.40699/",
        dependencies = @Dependency(id = "serverlistplus", optional = true))
public final class MaintenanceSpongePlugin extends MaintenanceModePlugin {
    private SettingsSponge settings;
    private Logger logger;
    @Inject
    private Game game;
    @Inject
    private PluginContainer container;
    @Inject
    private Metrics2 metrics;

    // Sponge related
    //TODO entire settingssponge

    //TODO gamereload
    //TODO getPluginFile method
    //TODO MaintenanceVersion.VERSION
    //TODO look at the rest of command stuff

    // General
    //TODO abstract loginlistener
    //TODO split api module
    //TODO tabcompletion

    @Inject
    public MaintenanceSpongePlugin() {
        super("§8[§eMaintenanceSponge§8] ", "3.0-SNAPSHOT", ServerType.SPONGE);
    }

    @Listener
    public void onEnable(final GameInitializationEvent event) {
        logger = new LoggerWrapper(getSpongeLogger());
        settings = new SettingsSponge(this);

        getSpongeLogger().info("Plugin by KennyTV");
        getSpongeLogger().info(getUpdateMessage());

        game.getCommandManager().register(this, new MaintenanceSpongeCommand(this, settings), "maintenance", "maintenancesponge");
        final EventManager em = game.getEventManager();
        em.registerListeners(this, new ClientConnectionListener(this, settings));
        em.registerListeners(this, new ClientPingServerListener(this, settings));
        em.registerListeners(this, new GameReloadListener(this));

        // ServerListPlus integration
        game.getPluginManager().getPlugin("serverlistplus").ifPresent(slpContainer -> slpContainer.getInstance().ifPresent(serverListPlus -> {
            serverListPlusHook = new ServerListPlusHook(((ServerListPlusPlugin) serverListPlus).getCore());
            serverListPlusHook.setEnabled(!settings.isMaintenance());
            getSpongeLogger().info("Enabled ServerListPlus integration!");
        }));
    }

    @Deprecated
    public static IMaintenance getAPI() {
        return MaintenanceSpongeAPI.getAPI();
    }

    @Override
    public void setMaintenance(final boolean maintenance) {
        settings.setMaintenance(maintenance);
        settings.setToConfig("enable-maintenance-mode", maintenance);
        settings.saveConfig();
        if (serverListPlusHook != null)
            serverListPlusHook.setEnabled(!maintenance);
        if (isTaskRunning())
            cancelTask();
        if (maintenance) {
            getServer().getOnlinePlayers().stream()
                    .filter(p -> !p.hasPermission("maintenance.bypass") && !settings.getWhitelistedPlayers().containsKey(p.getUniqueId()))
                    .forEach(p -> p.kick(Text.of(settings.getKickMessage().replace("%NEWLINE%", "\n"))));
            broadcast(settings.getMessage("maintenanceActivated"));
        } else
            broadcast(settings.getMessage("maintenanceDeactivated"));
    }

    @Override
    public void startMaintenanceRunnable(final int minutes, final boolean enable) {
        runnable = new MaintenanceRunnable(this, (Settings) getSettings(), minutes, enable);
        game.getScheduler().createTaskBuilder().interval(1, TimeUnit.SECONDS).execute(runnable).submit(this);
    }

    @Override
    public boolean isTaskRunning() {
        return runnable != null;
    }

    @Override
    public int startMaintenanceRunnable(final Runnable runnable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void async(final Runnable runnable) {
        game.getScheduler().createTaskBuilder().async().execute(runnable).submit(this);
    }

    @Override
    public void cancelTask() {
        game.getScheduler().getScheduledTasks(this).forEach(Task::cancel);
        runnable = null;
    }

    @Override
    public void broadcast(final String message) {
        getServer().getBroadcastChannel().send(Text.of(message));
    }

    @Override
    public ISettings getSettings() {
        return settings;
    }

    @Override
    public File getPluginFile() {
        //return container.getSource().get();
        return null;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public org.slf4j.Logger getSpongeLogger() {
        return container.getLogger();
    }

    public Server getServer() {
        return game.getServer();
    }
}
