package cc.mewcraft.mewutils.module.ore_announcer;

import cc.mewcraft.mewutils.MewPlugin;
import cc.mewcraft.mewutils.module.ModuleBase;
import com.google.inject.Inject;
import me.lucko.helper.metadata.Metadata;
import me.lucko.helper.metadata.MetadataKey;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public class OreAnnouncerModule extends ModuleBase implements Listener {

    public static final MetadataKey<Boolean> BROADCAST_TOGGLE_KEY = MetadataKey.createBooleanKey("subscribe.announcement.ore");

    private @MonotonicNonNull BlockCounter blockCounter;
    private @MonotonicNonNull Set<Material> enabledMaterials;
    private @MonotonicNonNull Set<String> enabledWorlds;

    @Inject
    public OreAnnouncerModule(MewPlugin plugin) {
        super(plugin);
    }

    @Override
    protected void load() throws Exception {
        this.blockCounter = new BlockCounter(configNode().node("max_iterations").getInt());
        this.enabledWorlds = new HashSet<>(configNode().node("worlds").getList(String.class, List.of()));
        this.enabledMaterials = configNode().node("blocks")
                .getList(String.class, List.of())
                .stream()
                .flatMap(name -> Stream.ofNullable(Material.matchMaterial(name)))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Material.class)));
    }

    @Override
    protected void enable() {
        // register listener
        registerListener(new BlockListener(this));

        // register command
        registerCommand(registry -> registry
                .commandBuilder("mewutils")
                .permission("mew.admin")
                .literal("toggle")
                .senderType(Player.class)
                .handler(commandContext -> {
                    Player player = (Player) commandContext.getSender();
                    if (isSubscriber(player.getUniqueId())) {
                        toggleSubscription(player.getUniqueId());
                        translations().of("toggle_broadcast_off").send(player);
                    } else {
                        toggleSubscription(player.getUniqueId());
                        translations().of("toggle_broadcast_on").send(player);
                    }
                })
        );
    }

    public BlockCounter getBlockCounter() {
        return this.blockCounter;
    }

    public boolean isSubscriber(UUID player) {
        return Metadata.provideForPlayer(player).getOrDefault(BROADCAST_TOGGLE_KEY, true);
    }

    public void toggleSubscription(UUID player) {
        Metadata.provideForPlayer(player).put(BROADCAST_TOGGLE_KEY, !isSubscriber(player));
    }

    public boolean shouldAnnounce(Block block) {
        return this.enabledMaterials.contains(block.getType()) && this.enabledWorlds.contains(block.getWorld().getName()) && !this.blockCounter.isDiscovered(block.getLocation());
    }

}
