package cc.mewcraft.mewutils.module.color_palette;

import cc.mewcraft.mewutils.MewPlugin;
import cc.mewcraft.mewutils.module.ModuleBase;
import com.google.inject.Inject;

import java.util.List;

/**
 * Adds the ability to dye items with a fancy UI.
 */
public class ColorPaletteModule extends ModuleBase {

    final List<PaletteHandler<?>> furnitureHandlers;

    @Inject
    public ColorPaletteModule(final MewPlugin plugin) {
        super(plugin);
        this.furnitureHandlers = List.of(
                new ArmorStandPaletteHandler(this),
                new ItemFramePaletteHandler(this)
        );
    }

    @Override protected void enable() {
        registerListener(new FurnitureListener(this));
    }

    @Override public boolean checkRequirement() {
        return isPluginPresent("ItemsAdder");
    }

}
