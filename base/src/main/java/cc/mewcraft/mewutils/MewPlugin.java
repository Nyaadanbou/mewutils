package cc.mewcraft.mewutils;

import cc.mewcraft.mewutils.command.CommandRegistry;
import cc.mewcraft.mewutils.module.ModuleBase;
import cc.mewcraft.spatula.message.Translations;
import org.bukkit.plugin.Plugin;

public interface MewPlugin extends Plugin {

    Translations translations();

    CommandRegistry commandRegistry();

    ClassLoader parentClassLoader();

    boolean isDevMode();

    boolean isModuleOn(ModuleBase module);

}
