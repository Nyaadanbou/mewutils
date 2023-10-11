package cc.mewcraft.mewutils.module;

import cc.mewcraft.mewutils.MewPlugin;
import cc.mewcraft.mewutils.command.CommandRegistry;
import cc.mewcraft.spatula.message.Translations;
import cloud.commandframework.Command;
import me.lucko.helper.Schedulers;
import me.lucko.helper.terminable.Terminable;
import me.lucko.helper.terminable.TerminableConsumer;
import me.lucko.helper.terminable.composite.CompositeTerminable;
import me.lucko.helper.terminable.module.TerminableModule;
import me.lucko.helper.utils.ResourceExtractor;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.text.Component.text;

@DefaultQualifier(NonNull.class)
public abstract class ModuleBase
        implements TerminableConsumer, ModuleLogger, ModuleRequirement {

    private final MewPlugin parentPlugin;
    private final CompositeTerminable terminableRegistry;
    private final Path directory;
    private final YamlConfigurationLoader configLoader;
    private @MonotonicNonNull CommentedConfigurationNode configNode;
    private @MonotonicNonNull Translations translations;
    private boolean moduleOn;

    public ModuleBase(MewPlugin parentPlugin) {
        this.parentPlugin = parentPlugin;

        // backed closeable of this module
        this.terminableRegistry = CompositeTerminable.create();

        // create dedicated data directory for this module
        this.directory = this.parentPlugin.getDataFolder().toPath().resolve("modules").resolve(getId());
        this.directory.toFile().mkdirs();

        // create dedicated config file for this module
        File configFile = this.directory.resolve("config.yml").toFile();
        if (!configFile.exists())
            ResourceExtractor.copyResourceRecursively(parentPlugin.parentClassLoader().getResource("modules/" + getId() + "/config.yml"), configFile);
        this.configLoader = YamlConfigurationLoader.builder()
                .file(configFile)
                .indent(2)
                .build();
    }

    /**
     * This runs before {@link #enable()}. Initialisation should be done here.
     */
    protected void load() throws Exception {}

    /**
     * This runs after {@link #load()}
     */
    protected void enable() throws Exception {}

    /**
     * This runs after the console prints "Done!", which means all the plugins are loaded at that time.
     * <p>
     * It's useful if you only, for example, need to register listeners of other plugins where the plugins are not
     * fully initialized yet.
     */
    protected void postEnable() throws Exception {}

    /**
     * This runs when the server shutdown.
     */
    protected void disable() throws Exception {}

    public final void onLoad() throws Exception {
        // load the config file into node
        this.configNode = this.configLoader.load();

        // dedicated language files for this module
        Path langDirectory = Path.of("modules").resolve(getId()).resolve("lang");
        if (this.parentPlugin.getDataFolder().toPath().resolve(langDirectory).toFile().mkdirs())
            info("translation directory does not exist - creating one");
        this.translations = new Translations(this.parentPlugin, langDirectory.toString(), "zh");

        // call subclass
        load();
    }

    public final void onEnable() throws Exception {
        if (!checkRequirement()) {
            warn(getLongId() + " is not enabled due to requirement not met");
            return;
        }

        // schedule cleanup of the registry
        Schedulers.builder()
                .async()
                .after(10, TimeUnit.SECONDS)
                .every(30, TimeUnit.SECONDS)
                .run(this.terminableRegistry::cleanup)
                .bindWith(this.terminableRegistry);

        // call subclass
        enable();

        this.parentPlugin.getComponentLogger().info(text()
                .append(text(getLongId()).color(NamedTextColor.GOLD))
                .appendSpace().append(text("is enabled!"))
                .build()
        );

        Schedulers.bukkit().runTask(this.parentPlugin, () -> {
            try {
                postEnable();
            } catch (Throwable e) {
                this.parentPlugin.getLogger().severe("Errors occurred in postLoad()");
                e.printStackTrace();
            }
        });
    }

    public final void onDisable() throws Exception {
        // call subclass
        disable();

        // terminate the registry
        this.terminableRegistry.closeAndReportException();

        this.parentPlugin.getComponentLogger().info(text()
                .append(text(getLongId()).color(NamedTextColor.GOLD))
                .appendSpace().append(text("is disabled!"))
                .build()
        );
    }

    public final Translations translations() {
        return this.translations;
    }

    public final YamlConfigurationLoader configLoader() {
        return this.configLoader;
    }

    public final CommentedConfigurationNode configNode() {
        return requireNonNull(this.configNode);
    }

    public final Path dataDirectory() {
        return this.directory.resolve("data");
    }

    public final Path moduleDirectory() {
        return this.directory;
    }

    private record AutoCloseableListenerWrapper(Listener listener) implements Terminable {
        @Override public void close() {
            HandlerList.unregisterAll(listener);
        }
    }

    public final <T extends Listener> void registerListener(@NonNull T listener) {
        requireNonNull(listener);
        getParentPlugin().getServer().getPluginManager().registerEvents(listener, getParentPlugin());
        bind(new AutoCloseableListenerWrapper(listener));
    }

    public final void registerCommand(@NonNull Function<CommandRegistry, Command.Builder<CommandSender>> command) {
        requireNonNull(command, "command");
        CommandRegistry registry = getParentPlugin().commandRegistry();
        Command<CommandSender> built = command.apply(registry).build();
        registry.prepareCommand(built);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public final <T> T getPlugin(@NonNull String name, @NonNull Class<T> pluginClass) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(pluginClass, "pluginClass");
        return (T) Bukkit.getServer().getPluginManager().getPlugin(name);
    }

    public final boolean isPluginPresent(String name) {
        return Bukkit.getServer().getPluginManager().getPlugin(name) != null;
    }

    @Override
    public final <T extends TerminableModule> @NonNull T bindModule(@NonNull final T module) {
        requireNonNull(module, "module");
        return this.terminableRegistry.bindModule(module);
    }

    @Override
    public final <T extends AutoCloseable> @NonNull T bind(@NonNull final T terminable) {
        requireNonNull(terminable, "terminable");
        return this.terminableRegistry.bind(terminable);
    }

    @Override
    public final MewPlugin getParentPlugin() {
        return this.parentPlugin;
    }

}
