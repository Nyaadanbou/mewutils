package cc.mewcraft.mewutils.module.elytra_limiter;

import cc.mewcraft.mewutils.MewPlugin;
import cc.mewcraft.mewutils.module.ModuleBase;
import com.google.inject.Inject;
import me.lucko.helper.cooldown.Cooldown;
import me.lucko.helper.cooldown.StackableCooldownMap;
import me.lucko.helper.progressbar.ProgressbarDisplay;
import me.lucko.helper.progressbar.ProgressbarGenerator;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public class ElytraLimiterModule extends ModuleBase {

    private @MonotonicNonNull Set<String> restrictedWorlds;
    private @MonotonicNonNull Set<BoostMethod> restrictedBoost;
    private @MonotonicNonNull ProgressbarDisplay progressbarDisplay;
    private @MonotonicNonNull StackableCooldownMap<UUID> cooldownMap;
    private double velocityMultiply;
    private double tpsThreshold;

    @Inject
    public ElytraLimiterModule(MewPlugin plugin) {
        super(plugin);
    }

    @Override protected void load() throws Exception {
        this.restrictedWorlds = new HashSet<>(configNode().node("worlds").getList(String.class, List.of()));

        this.restrictedBoost = configNode().node("methods")
                .getList(String.class, List.of())
                .stream().map(BoostMethod::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(BoostMethod.class)));

        this.cooldownMap = StackableCooldownMap.create(
                Cooldown.of(configNode().node("cooldown").getInt(), TimeUnit.MILLISECONDS),
                uuid -> configNode().node("stacks").getLong()
        );

        this.progressbarDisplay = new ProgressbarDisplay(
                configNode().node("bar_stay_time").getInt(),
                ProgressbarGenerator.builder()
                        .left(translations().of("slow_elytra.cooldown_progressbar.left").plain())
                        .full(translations().of("slow_elytra.cooldown_progressbar.full").plain())
                        .empty(translations().of("slow_elytra.cooldown_progressbar.empty").plain())
                        .right(translations().of("slow_elytra.cooldown_progressbar.right").plain())
                        .width(configNode().node("bar_width").getInt())
                        .build()
        );

        this.velocityMultiply = configNode().node("velocity_multiply").getDouble();

        this.tpsThreshold = configNode().node("tps_threshold").getDouble();
    }

    @Override protected void enable() {
        registerListener(new ElytraBoostListener(this));
    }

    public ProgressbarDisplay getProgressbarMessenger() {
        return this.progressbarDisplay;
    }

    public StackableCooldownMap<UUID> getCooldownMap() {
        return this.cooldownMap;
    }

    public boolean isBoostAllowed(BoostMethod method) {
        return !this.restrictedBoost.contains(method);
    }

    public boolean inRestrictedWorld(Player player) {
        return this.restrictedWorlds.contains(player.getWorld().getName());
    }

    public boolean underTPSThreshold() {
        return getParentPlugin().getServer().getTPS()[0] <= this.tpsThreshold;
    }

    public double getVelocityMultiply() {
        return this.velocityMultiply;
    }

}
