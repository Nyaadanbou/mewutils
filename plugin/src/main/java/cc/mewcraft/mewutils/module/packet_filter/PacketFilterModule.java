package cc.mewcraft.mewutils.module.packet_filter;

import cc.mewcraft.mewutils.MewPlugin;
import cc.mewcraft.mewutils.module.ModuleBase;
import com.google.inject.Inject;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PacketFilterModule extends ModuleBase implements Listener {

    /**
     * The UUIDs of the players who are afk-ing.
     */
    Set<UUID> afkPlayers;
    /**
     * Allowed entity packet types.
     */
    EnumSet<EntityType> filteredEntityTypes;
    /**
     * Names of the packet types to be blocked.
     */
    Set<String> blockedPacketTypes; //

    @Inject
    public PacketFilterModule(final MewPlugin parent) {
        super(parent);
    }

    @Override protected void load() throws Exception {
        // Initialize class fields
        afkPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

        // Read the config values:

        filteredEntityTypes = EnumSet.noneOf(EntityType.class);
        filteredEntityTypes.addAll(configNode().node("whitelistEntities").getList(String.class, List.of()).stream().map(EntityType::valueOf).toList());
        filteredEntityTypes.stream().map(Enum::name).reduce((e1, e2) -> e1 + ", " + e2).ifPresentOrElse(
                types -> info("Added whitelisted entity type: " + types),
                () -> info("Added whitelisted entity type: <Empty>")
        );

        blockedPacketTypes = new HashSet<>();
        blockedPacketTypes.addAll(configNode().node("blockedPackets").getList(String.class, List.of()));
        blockedPacketTypes.stream().reduce((e1, e2) -> e1 + ", " + e2).ifPresentOrElse(
                types -> info("Added blocked packet types: " + types),
                () -> info("Added blocked packet types: <Empty>")
        );
    }

    @Override protected void postEnable() {
        registerListener(new EssentialsListener(this));
        new ProtocolLibHook(this).bindWith(this);
    }

    @Override public boolean checkRequirement() {
        return isPluginPresent("Essentials") && isPluginPresent("ProtocolLib");
    }

}
