package cc.mewcraft.mewutils.module.slime_utils;

import cc.mewcraft.mewutils.MewPlugin;
import cc.mewcraft.mewutils.module.ModuleBase;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import com.google.inject.Inject;
import org.bukkit.entity.Player;

public class SlimeUtilsModule extends ModuleBase {

    @Inject
    public SlimeUtilsModule(final MewPlugin parent) {
        super(parent);
    }

    @Override protected void enable() {
        registerCommand(commandRegistry -> commandRegistry
                .commandBuilder("mewutils")
                .permission("mew.admin")
                .literal("slimechunk")
                .argument(PlayerArgument.of("player"))
                .handler(context -> {
                    Player player = context.get("player");
                    if (player.getChunk().isSlimeChunk()) {
                        translations().of("found").send(player);
                    } else {
                        translations().of("not_found").send(player);
                    }
                })
        );
    }

}
