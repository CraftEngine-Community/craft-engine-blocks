package cn.gtemc.craftengine.block.behavior;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.ticks.TickPriorityProxy;

import java.util.Set;
import java.util.stream.Collectors;

public final class PickaxeBlockBehavior extends FacingTriggerableBlockBehavior {
    public static final BlockBehaviorFactory<PickaxeBlockBehavior> FACTORY = new Factory();

    public PickaxeBlockBehavior(BlockDefinition blockDefinition, Property<Direction> facing, Property<Boolean> triggered, Set<Key> blocks, boolean whitelistMode) {
        super(blockDefinition, facing, triggered, blocks, whitelistMode);
    }

    @Override
    protected Object getTickPriority() {
        return TickPriorityProxy.EXTREMELY_HIGH;
    }

    @Override
    public void tick(Object thisBlock, Object[] args) {
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (blockState == null || blockState.isEmpty()) return;
        Object breakPos = BlockPosProxy.INSTANCE.relative(pos, DirectionUtils.toNMSDirection(blockState.get(this.facingProperty)));
        Object breakState = BlockGetterProxy.INSTANCE.getBlockState(level, breakPos);
        if (blockCheckByBlockState(breakState)) {
            LevelWriterProxy.INSTANCE.destroyBlock(level, breakPos, true);
        }
    }

    private static class Factory implements BlockBehaviorFactory<PickaxeBlockBehavior> {

        @Override
        public PickaxeBlockBehavior create(BlockDefinition block, ConfigSection section) {
            boolean whitelistMode = section.getBoolean("whitelist");
            Set<Key> blocks = section.getList("blocks", ConfigValue::getAsIdentifier).stream().collect(Collectors.toCollection(ObjectOpenHashSet::new));
            if (blocks.isEmpty() && !whitelistMode) {
                blocks = FacingTriggerableBlockBehavior.DEFAULT_BLACKLIST_BLOCKS;
            }
            return new PickaxeBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "facing", Direction.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "triggered", Boolean.class),
                    blocks,
                    whitelistMode
            );
        }
    }
}
