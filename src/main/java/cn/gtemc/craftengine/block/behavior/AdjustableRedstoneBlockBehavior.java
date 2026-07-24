package cn.gtemc.craftengine.block.behavior;

import net.momirealms.craftengine.bukkit.block.behavior.BukkitBlockBehavior;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.property.IntegerProperty;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.world.context.UseOnContext;

public final class AdjustableRedstoneBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<AdjustableRedstoneBlockBehavior> FACTORY = new Factory();
    private final IntegerProperty powerProperty;

    public AdjustableRedstoneBlockBehavior(BlockDefinition blockDefinition, IntegerProperty power) {
        super(blockDefinition);
        this.powerProperty = power;
    }

    @Override
    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        int power = state.get(this.powerProperty);
        if (context.isSecondaryUseActive()) {
            if (power - 1 < this.powerProperty.min) {
                power = this.powerProperty.max;
            } else {
                power--;
            }
        } else {
            if (power + 1 > this.powerProperty.max) {
                power = this.powerProperty.min;
            } else {
                power++;
            }
        }
        context.getLevel().setBlockState(context.getClickedPos(), state.with(this.powerProperty, power), UpdateFlags.UPDATE_ALL);
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    @Override
    public boolean isSignalSource(Object thisBlock, Object[] args) {
        return true;
    }

    public int getSignal(Object thisBlock, Object[] args) {
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (state == null || state.isEmpty()) return 0;
        return state.get(this.powerProperty);
    }

    public int getDirectSignal(Object thisBlock, Object[] args) {
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (state == null || state.isEmpty()) return 0;
        return state.get(this.powerProperty);
    }

    private static class Factory implements BlockBehaviorFactory<AdjustableRedstoneBlockBehavior> {

        @Override
        public AdjustableRedstoneBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new AdjustableRedstoneBlockBehavior(
                    block,
                    (IntegerProperty) BlockBehaviorFactory.getProperty(section.path(), block, "power", Integer.class)
            );
        }
    }
}
