package cn.gtemc.craftengine.entity.seat;

import cn.gtemc.craftengine.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import static cn.gtemc.craftengine.block.entity.SeatBlockEntityController.SEAT_KEY;

public final class SeatManager implements Manageable, Listener {
    private final CraftEngineBlocks plugin;
    private final Listener dismountListener;

    public SeatManager(CraftEngineBlocks plugin) {
        this.plugin = plugin;
        this.dismountListener = VersionHelper.isOrAbove1_20_3
                ? new DismountListener1_20_3(this::handleDismount)
                : new DismountListener1_20(this::handleDismount);
    }

    private void handleDismount(Player player, @NotNull Entity dismounted) {
        if (!isSeatEntityType(dismounted)) return;
        tryLeavingSeat(player, dismounted);
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this.dismountListener, this.plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this.dismountListener);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                tryLeavingSeat(player, vehicle);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Entity entity = player.getVehicle();
        if (entity == null) return;
        if (this.isSeatEntityType(entity)) {
            this.tryLeavingSeat(player, entity);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Entity entity = player.getVehicle();
        if (entity == null) return;
        if (this.isSeatEntityType(entity)) {
            this.tryLeavingSeat(player, entity);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInteractArmorStand(PlayerInteractAtEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (clicked instanceof ArmorStand armorStand) {
            if (!armorStand.getPersistentDataContainer().has(SEAT_KEY)) return;
            event.setCancelled(true);
        }
    }

    private boolean isSeatEntityType(Entity entity) {
        return (entity instanceof ArmorStand || entity instanceof ItemDisplay);
    }

    private void tryLeavingSeat(@NotNull Player player, @NotNull Entity seat) {
        boolean isSeat = seat.getPersistentDataContainer().has(SEAT_KEY);
        if (!isSeat) return;
        Location location = seat.getLocation().add(0, seat instanceof ArmorStand ? 0.3875 : -0.35, 0);
        seat.remove();
        EntityUtils.safeDismount(player, location);
    }
}
