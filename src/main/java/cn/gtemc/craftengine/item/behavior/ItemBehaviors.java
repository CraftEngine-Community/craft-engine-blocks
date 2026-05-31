package cn.gtemc.craftengine.item.behavior;

import cn.gtemc.craftengine.util.RegistryUtils;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorType;
import net.momirealms.craftengine.core.util.Key;

public final class ItemBehaviors {
    private ItemBehaviors() {}

    public static final ItemBehaviorType<PlaceBlockBehavior> PLACE_BLOCK = RegistryUtils.registerItemBehavior(Key.of("gtemc:place_block"), PlaceBlockBehavior.FACTORY);

    public static void register() {
    }
}
