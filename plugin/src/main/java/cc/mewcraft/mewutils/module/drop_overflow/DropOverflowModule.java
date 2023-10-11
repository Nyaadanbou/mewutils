package cc.mewcraft.mewutils.module.drop_overflow;

import cc.mewcraft.mewutils.MewPlugin;
import cc.mewcraft.mewutils.module.ModuleBase;
import com.google.inject.Inject;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class DropOverflowModule extends ModuleBase implements Listener {

    private EnumSet<Material> types;
    private int mergeLimitThreshold;

    @Inject
    public DropOverflowModule(MewPlugin plugin) {
        super(plugin);
    }

    @Override protected void load() throws Exception {
        this.types = configNode().node("types")
                .getList(String.class, List.of())
                .stream()
                .map(Material::matchMaterial)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Material.class)));
        this.mergeLimitThreshold = configNode().node("threshold").getInt();
    }

    @Override protected void enable() {
        registerListener(this);
    }

    @EventHandler
    public void onMerge(ItemMergeEvent event) {
        if (this.types.contains(event.getEntity().getItemStack().getType())) {
            if (event.getEntity().getItemStack().getAmount() > this.mergeLimitThreshold ||
                event.getTarget().getItemStack().getAmount() > this.mergeLimitThreshold) {
                event.getEntity().remove();
                event.getTarget().remove();
            }
        }
    }

}
