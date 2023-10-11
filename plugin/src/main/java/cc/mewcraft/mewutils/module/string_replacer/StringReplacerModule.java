package cc.mewcraft.mewutils.module.string_replacer;

import cc.mewcraft.mewutils.MewPlugin;
import cc.mewcraft.mewutils.module.ModuleBase;
import com.google.inject.Inject;
import io.leangen.geantyref.TypeToken;
import me.lucko.helper.function.chain.Chain;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringReplacerModule extends ModuleBase implements Listener {

    Map<String, String> replacement;

    @Inject
    public StringReplacerModule(final MewPlugin parent) {
        super(parent);
    }

    @Override protected void load() throws Exception {
        this.replacement = new HashMap<>();

        // Read the config values

        configNode().node("replacement").getList(new TypeToken<Map<String, String>>() {}, List.of())
                .forEach(map -> {
                    String oldValue = map.get("old");
                    String newValue = map.get("new");
                    Chain.start(newValue)
                            .map(MiniMessage.miniMessage()::deserialize)
                            .map(GsonComponentSerializer.gson()::serialize)
                            .end()
                            .ifPresent(gsonString ->
                                    this.replacement.put(oldValue, gsonString)
                            );
                });
    }

    @Override protected void postEnable() {
        new ProtocolLibHook(this).bindWith(this);
    }

    @Override public boolean checkRequirement() {
        return isPluginPresent("ProtocolLib");
    }

}
