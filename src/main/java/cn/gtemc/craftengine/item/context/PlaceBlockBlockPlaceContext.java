package cn.gtemc.craftengine.item.context;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;

public final class PlaceBlockBlockPlaceContext extends BlockPlaceContext {
    public PlaceBlockBlockPlaceContext(World world, InteractionHand hand, Item stack, BlockHitResult hit) {
        super(world, null, hand, stack, hit);
    }

    @Override
    public Direction getNearestLookingDirection() {
        return this.getHitResult().direction();
    }
}
