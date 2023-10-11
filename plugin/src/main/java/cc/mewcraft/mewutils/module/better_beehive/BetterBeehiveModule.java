package cc.mewcraft.mewutils.module.better_beehive;

import cc.mewcraft.mewutils.MewPlugin;
import cc.mewcraft.mewutils.module.ModuleBase;
import com.google.inject.Inject;

public class BetterBeehiveModule extends ModuleBase {

    @Inject
    public BetterBeehiveModule(MewPlugin plugin) {
        super(plugin);
    }

    @Override
    protected void enable() {
        registerListener(new BeehiveListener(this));
    }

}
