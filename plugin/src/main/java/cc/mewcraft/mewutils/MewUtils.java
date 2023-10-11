package cc.mewcraft.mewutils;

import cc.mewcraft.mewutils.command.CommandRegistry;
import cc.mewcraft.mewutils.module.ModuleBase;
import cc.mewcraft.mewutils.module.better_beehive.BetterBeehiveModule;
import cc.mewcraft.mewutils.module.case_insensitive_commands.CaseInsensitiveCommandsModule;
import cc.mewcraft.mewutils.module.color_palette.ColorPaletteModule;
import cc.mewcraft.mewutils.module.death_logger.DeathLoggerModule;
import cc.mewcraft.mewutils.module.drop_overflow.DropOverflowModule;
import cc.mewcraft.mewutils.module.elytra_limiter.ElytraLimiterModule;
import cc.mewcraft.mewutils.module.eternal_lootchest.EternalLootChestModule;
import cc.mewcraft.mewutils.module.fireball_utils.FireballUtilsModule;
import cc.mewcraft.mewutils.module.ore_announcer.OreAnnouncerModule;
import cc.mewcraft.mewutils.module.packet_filter.PacketFilterModule;
import cc.mewcraft.mewutils.module.slime_utils.SlimeUtilsModule;
import cc.mewcraft.mewutils.module.string_replacer.StringReplacerModule;
import cc.mewcraft.mewutils.module.villager_utils.VillagerUtilsModule;
import cc.mewcraft.spatula.message.Translations;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import me.lucko.helper.plugin.ExtendedJavaPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;

public final class MewUtils extends ExtendedJavaPlugin implements MewPlugin {

    public static MewUtils INSTANCE;

    // --- config ---
    private YamlConfigurationLoader mainConfigLoader;
    private ConfigurationNode mainConfigNode; // main config
    private YamlConfigurationLoader moduleConfigLoader;
    private ConfigurationNode moduleConfigNode; // module config to control whether a module should be enabled
    private Translations translations; // main translations

    // --- modules ---
    private List<ModuleBase> modules;

    // --- commands ---
    private CommandRegistry commandRegistry;

    // --- variables ---
    private boolean verbose;

    @Override
    protected void enable() {
        INSTANCE = this;

        this.getLogger().info("Enabling modules ...");

        // --- Load main translations ---

        this.translations = new Translations(this);

        // --- Load main config ---

        try {
            // Load: "config.yml"
            this.saveDefaultConfig();
            this.mainConfigLoader = YamlConfigurationLoader.builder()
                    .path(getDataFolder().toPath().resolve("config.yml"))
                    .nodeStyle(NodeStyle.BLOCK)
                    .indent(2)
                    .build();
            this.mainConfigNode = this.mainConfigLoader.load();
            this.verbose = this.mainConfigNode.node("verbose").getBoolean();

            // Load: "modules.yml"
            this.moduleConfigLoader = YamlConfigurationLoader.builder()
                    .path(getDataFolder().toPath().resolve("modules.yml"))
                    .nodeStyle(NodeStyle.BLOCK)
                    .indent(2)
                    .build();
            this.moduleConfigNode = this.moduleConfigLoader.load();
        } catch (ConfigurateException e) {
            this.getLogger().severe("Failed to load main config! See the stack trace below");
            e.printStackTrace();
        }

        // --- Initialise commands ---

        try {
            this.commandRegistry = new CommandRegistry(this);
            this.prepareInternalCommands();
        } catch (Exception e) {
            this.getLogger().severe("Failed to initialize commands! See the stack trace below");
            e.printStackTrace();
            return;
        }

        // --- Configure guice ---

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override protected void configure() {
                bind(Plugin.class).toInstance(MewUtils.this);
                bind(MewPlugin.class).toInstance(MewUtils.this);
                bind(JavaPlugin.class).toInstance(MewUtils.this);
            }
        });

        // --- Load modules ---

        this.modules = new ArrayList<>();
        this.modules.add(injector.getInstance(BetterBeehiveModule.class));
        this.modules.add(injector.getInstance(DeathLoggerModule.class));
        this.modules.add(injector.getInstance(ElytraLimiterModule.class));
        this.modules.add(injector.getInstance(FireballUtilsModule.class));
        this.modules.add(injector.getInstance(ColorPaletteModule.class));
        this.modules.add(injector.getInstance(DropOverflowModule.class));
        this.modules.add(injector.getInstance(OreAnnouncerModule.class));
        this.modules.add(injector.getInstance(SlimeUtilsModule.class));
        this.modules.add(injector.getInstance(VillagerUtilsModule.class));
        this.modules.add(injector.getInstance(PacketFilterModule.class));
        this.modules.add(injector.getInstance(StringReplacerModule.class));
        this.modules.add(injector.getInstance(CaseInsensitiveCommandsModule.class));
        this.modules.add(injector.getInstance(EternalLootChestModule.class));

        for (ModuleBase module : this.modules) {
            if (!isModuleOn(module)) {
                this.getLogger().info("Module " + module.getLongId() + " is disabled in the config");
                continue;
            }
            try {
                module.onLoad();
                module.onEnable();
            } catch (Exception e) {
                this.getLogger().severe("Module " + module.getLongId() + " failed to load/enable! See the stack trace below");
                e.printStackTrace();
            }
        }

        try {
            this.moduleConfigLoader.save(this.moduleConfigNode);
        } catch (ConfigurateException e) {
            this.getLogger().severe("Failed to save modules.yml");
        }

        // --- Make all commands effective ---

        this.commandRegistry.registerCommands();
    }

    @Override
    protected void disable() {
        this.getLogger().info("Disabling modules ...");

        for (ModuleBase module : this.modules) {
            try {
                module.onDisable();
            } catch (Exception e) {
                this.getLogger().severe("Module " + module.getLongId() + " failed to disable! Check the stacktrace below for more details");
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isDevMode() {
        return this.verbose;
    }

    @Override
    public boolean isModuleOn(ModuleBase module) {
        if (this.moduleConfigNode == null) {
            this.getLogger().severe("The modules.yml is not initialized");
            return false;
        }

        String path = LOWER_UNDERSCORE.to(LOWER_HYPHEN, module.getId());
        ConfigurationNode node = this.moduleConfigNode.node(path);

        // Disable the module by default if the node not already existing
        try {
            if (node.virtual())
                node.set(false);
        } catch (SerializationException e) {
            return false;
        }

        return node.getBoolean();
    }

    public void reload() {
        onDisable();
        onEnable();
    }

    @Override
    public CommandRegistry commandRegistry() {
        return this.commandRegistry;
    }

    @Override public ClassLoader parentClassLoader() {
        return this.getClassLoader();
    }

    @Override public Translations translations() {
        return this.translations;
    }

    private void prepareInternalCommands() {
        // At the moment, we only need a reload command for the parent plugin
        this.commandRegistry.prepareCommand(this.commandRegistry
                .commandBuilder("mewutils")
                .permission("mew.admin")
                .literal("reload")
                .handler(context -> {
                    this.reload();
                    this.translations().of("reloaded").send(context.getSender());
                }).build()
        );
    }

}
