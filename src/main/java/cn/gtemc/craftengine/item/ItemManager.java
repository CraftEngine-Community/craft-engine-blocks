package cn.gtemc.craftengine.item;

import cn.gtemc.craftengine.CraftEngineBlocks;
import cn.gtemc.craftengine.item.settings.AttributesSetting;
import cn.gtemc.craftengine.plugin.context.RandomNumberContext;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import net.kyori.adventure.key.Key;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.plugin.Manageable;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class ItemManager implements Manageable, Listener {
    private final CraftEngineBlocks plugin;

    public ItemManager(CraftEngineBlocks plugin) {
        this.plugin = plugin;
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @SuppressWarnings({"PatternValidation", "deprecation", "UnstableApiUsage"})
    private void removeAttributes(BukkitServerPlayer player, ItemStack itemStack, EquipmentSlot equipmentSlot) {
        BukkitItem item = BukkitAdaptor.adapt(itemStack);
        ItemDefinition itemDefinition = item.getDefinition().orElse(null);
        if (itemDefinition == null) {
            return;
        }
        List<AttributesSetting.AttributeData> data = itemDefinition.settings().getCustomData(AttributesSetting.ATTRIBUTES);
        if (data == null || data.isEmpty()) {
            return;
        }
        for (AttributesSetting.AttributeData attributeData : data) {
            EquipmentSlotGroup slot = Objects.requireNonNull(EquipmentSlotGroup.getByName(attributeData.modifier().slot().name().toLowerCase(Locale.ROOT)));
            if (!equipmentSlot.equals(slot.getExample())) {
                continue; // 不是正确槽位不管
            }
            AttributeInstance attribute = player.platformPlayer().getAttribute(Registry.ATTRIBUTE.getOrThrow(Key.key(attributeData.modifier().type())));
            if (attribute == null) {
                continue;
            }
            attribute.removeModifier(KeyUtils.toNamespacedKey(attributeData.modifier().id().orElseThrow()));
        }
    }

    @SuppressWarnings({"PatternValidation", "deprecation", "UnstableApiUsage"})
    private void addAttributes(BukkitServerPlayer player, ItemStack itemStack, EquipmentSlot equipmentSlot) {
        BukkitItem item = BukkitAdaptor.adapt(itemStack);
        ItemDefinition itemDefinition = item.getDefinition().orElse(null);
        if (itemDefinition == null) {
            return;
        }
        RandomNumberContext context = RandomNumberContext.of(player, item);
        List<AttributesSetting.AttributeData> data = itemDefinition.settings().getCustomData(AttributesSetting.ATTRIBUTES);
        if (data == null || data.isEmpty()) {
            return;
        }
        for (AttributesSetting.AttributeData attributeData : data) {
            EquipmentSlotGroup slot = Objects.requireNonNull(EquipmentSlotGroup.getByName(attributeData.modifier().slot().name().toLowerCase(Locale.ROOT)));
            if (!equipmentSlot.equals(slot.getExample())) {
                continue; // 不是正确槽位不管
            }
            if (attributeData.expires() != null && attributeData.expires().before(new Date())) {
                continue; // 过期不管
            }
            if (attributeData.conditions() != null && !attributeData.conditions().test(context)) {
                continue; // 不符合条件不管
            }
            AttributeInstance attribute = player.platformPlayer().getAttribute(Registry.ATTRIBUTE.getOrThrow(Key.key(attributeData.modifier().type())));
            if (attribute == null) {
                continue;
            }
            AttributeModifier.Operation operation = AttributeModifier.Operation.values()[attributeData.modifier().operation().ordinal()];
            AttributeModifier attributeModifier = new AttributeModifier(
                    KeyUtils.toNamespacedKey(attributeData.modifier().id().orElseThrow()),
                    attributeData.modifier().amount().getDouble(context),
                    operation,
                    slot
            );
            attribute.addTransientModifier(attributeModifier);
        }
    }

    @EventHandler
    public void onEntityEquipmentChanged(EntityEquipmentChangedEvent event) {
        if (!(event.getEntity() instanceof Player bukkitPlayer)) {
            return;
        }
        BukkitServerPlayer player = BukkitAdaptor.adapt(bukkitPlayer);
        if (player == null) {
            return;
        }
        for (Map.Entry<EquipmentSlot, EntityEquipmentChangedEvent.EquipmentChange> entry : event.getEquipmentChanges().entrySet()) {
            removeAttributes(player, entry.getValue().oldItem(), entry.getKey());
            addAttributes(player, entry.getValue().newItem(), entry.getKey());
        }
    }
}
