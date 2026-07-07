package cn.gtemc.craftengine.block.behavior;

import cn.gtemc.craftengine.injector.PlaceBlockBlockPlaceContextGenerator;
import cn.gtemc.craftengine.item.context.PlaceBlockBlockPlaceContext;
import cn.gtemc.craftengine.util.PositionUtils;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.behavior.BlockItemBehavior;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.*;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.item.ItemEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.BlockItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.EntityGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.ChestBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.entity.ChestBlockEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.BlockHitResultProxy;
import net.momirealms.craftengine.proxy.minecraft.world.ticks.TickPriorityProxy;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PlaceBlockBehavior extends FacingTriggerableBlockBehavior {
    public static final BlockBehaviorFactory<PlaceBlockBehavior> FACTORY = new Factory();

    public PlaceBlockBehavior(BlockDefinition blockDefinition, Property<Direction> facing, Property<Boolean> triggered, Set<Key> blocks, boolean whitelistMode) {
        super(blockDefinition, facing, triggered, blocks, whitelistMode);
    }

    private static IntStream getSlots(Object container, Object direction) {
        if (WorldlyContainerProxy.CLASS.isInstance(container)) {
            return IntStream.of(WorldlyContainerProxy.INSTANCE.getSlotsForFace(container, direction));
        } else {
            return IntStream.range(0, ContainerProxy.INSTANCE.getContainerSize(container));
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean getItemAndDoThings(Object level, Object blockPos, Object direction, Function<Object, Boolean> function) {
        for (Object container : getContainersAt(level, blockPos)) {
            boolean success = getSlots(container, direction).anyMatch(index -> {
                Object stack = ContainerProxy.INSTANCE.removeItem(container, index, 1);
                if (ItemStackProxy.INSTANCE.isEmpty(stack)) return false;
                boolean result = function.apply(ItemStackProxy.INSTANCE.copy(stack));
                if (result) {
                    ContainerProxy.INSTANCE.setChanged(container);
                } else {
                    ContainerProxy.INSTANCE.setItem(container, index, stack);
                }
                return true;
            });
            if (success) return true;
        }
        Object itemEntity = getItemAt(level, blockPos);
        if (itemEntity == null) return false;
        Object stack = ItemEntityProxy.INSTANCE.getItem(itemEntity);
        if (ItemStackProxy.INSTANCE.isEmpty(stack)) return false;
        boolean result = function.apply(ItemStackProxy.INSTANCE.copyWithCount(stack, 1));
        if (!result) return true;
        ItemStackProxy.INSTANCE.shrink(stack, 1);
        if (ItemStackProxy.INSTANCE.getCount(stack) <= 0) {
            EntityProxy.INSTANCE.discard(itemEntity);
        }
        return true;
    }

    private static List<Object> getContainersAt(Object level, Object blockPos) {
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(level, blockPos);
        Object block = BlockStateProxy.INSTANCE.getBlock(blockState);
        if (WorldlyContainerHolderProxy.CLASS.isInstance(block)) {
            Object container = WorldlyContainerHolderProxy.INSTANCE.getContainer(block, blockState, level, blockPos);
            if (container != null) {
                return List.of(container);
            }
        } else if (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.hasBlockEntity(blockState)) {
            Object blockEntity = BlockGetterProxy.INSTANCE.getBlockEntity(level, blockPos);
            if (ContainerProxy.CLASS.isInstance(blockEntity)) {
                if (!(ChestBlockEntityProxy.CLASS.isInstance(blockEntity)) || !(ChestBlockProxy.CLASS.isInstance(block))) {
                    return List.of(blockEntity);
                }
                Object container = ChestBlockProxy.INSTANCE.getContainer(block, blockState, level, blockPos, true);
                if (container != null) {
                    return List.of(container);
                }
            }
        }
        List<Object> list = new ArrayList<>();
        for (Object entity : EntityGetterProxy.INSTANCE.getEntities(
                level, null, blockAABB(blockPos),
                entity -> ContainerProxy.CLASS.isInstance(entity) && EntityProxy.INSTANCE.isAlive(entity)
        )) {
            if (ContainerProxy.CLASS.isInstance(entity)) {
                list.add(entity);
            }
        }
        return list;
    }

    @Nullable
    public static Object getItemAt(Object level, Object blockPos) {
        List<Object> entitiesOfClass = EntityGetterProxy.INSTANCE.getEntitiesOfClass(
                level, ItemEntityProxy.CLASS, blockAABB(blockPos), EntityProxy.INSTANCE::isAlive
        );
        return entitiesOfClass.isEmpty() ? null : entitiesOfClass.getFirst();
    }

    private static Object blockAABB(Object blockPos) {
        return AABBProxy.INSTANCE.ofSize(PositionUtils.getCenterVec3(blockPos), 0.9999999, 0.9999999, 0.9999999);
    }

    @Override
    protected Object getTickPriority() {
        return TickPriorityProxy.EXTREMELY_LOW;
    }

    @Override
    public void tick(Object thisBlock, Object[] args) {
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (blockState == null || blockState.isEmpty()) return;
        Direction facingDirection = blockState.get(this.facingProperty);
        Direction oppositeDirection = facingDirection.opposite();
        Object oppositeNMSDirection = DirectionUtils.toNMSDirection(oppositeDirection);
        Object sourcePos = BlockPosProxy.INSTANCE.relative(pos, oppositeNMSDirection);
        Object targetPos = BlockPosProxy.INSTANCE.relative(pos, DirectionUtils.toNMSDirection(facingDirection));
        getItemAndDoThings(level, sourcePos, oppositeNMSDirection, stack -> {
            if (ItemStackProxy.INSTANCE.isEmpty(stack)) return false;
            Object item = ItemStackProxy.INSTANCE.getItem(stack);
            if (tryPlaceVanillaBlock(level, targetPos, oppositeNMSDirection, stack, item)) return true;
            if (tryPlaceCustomBlock(level, targetPos, oppositeDirection, stack)) return true;
            spawnItemEntity(level, targetPos, stack);
            return true;
        });
    }

    private boolean tryPlaceVanillaBlock(Object level, Object targetPos, Object oppositeDirection, Object stack, Object item) {
        if (!BlockItemProxy.CLASS.isInstance(item)) return false;
        Object block = BlockItemProxy.INSTANCE.getBlock(item);
        if (!blockCheckByBlockState(BlockProxy.INSTANCE.getDefaultBlockState(block))) return false;
        Object blockHitResult = BlockHitResultProxy.INSTANCE.newInstance(
                PositionUtils.getCenterVec3(targetPos),
                oppositeDirection,
                targetPos,
                false
        );
        Object placeContext = PlaceBlockBlockPlaceContextGenerator.create(
                level, InteractionHandProxy.MAIN_HAND, stack, blockHitResult
        );
        Object interactionResult = BlockItemProxy.INSTANCE.place(item, placeContext);
        return InteractionResultProxy.INSTANCE.consumesAction(interactionResult);
    }

    private boolean tryPlaceCustomBlock(Object level, Object targetPos, Direction oppositeDirection, Object stack) {
        ItemBehavior itemBehavior = BukkitAdaptor.adapt(ItemStackUtils.getBukkitStack(stack)).getBehavior().orElse(null);
        if (itemBehavior == null) return false;
        List<BlockItemBehavior> behaviors = new ArrayList<>();
        itemBehavior.let(BlockItemBehavior.class, behaviors::add);
        for (BlockItemBehavior behavior : behaviors) {
            if (!blockCheckByKey(behavior.block())) continue;
            BlockHitResult hitResult = new BlockHitResult(
                    PositionUtils.toVec3d(targetPos),
                    oppositeDirection,
                    LocationUtils.fromBlockPos(targetPos),
                    false
            );
            PlaceBlockBlockPlaceContext context = new PlaceBlockBlockPlaceContext(
                    BukkitAdaptor.adapt(LevelProxy.INSTANCE.getWorld(level)),
                    InteractionHand.MAIN_HAND,
                    BukkitAdaptor.adapt(ItemStackUtils.getBukkitStack(stack)),
                    hitResult
            );
            if (behavior.place(context).success()) return true;
        }
        return false;
    }

    private static void spawnItemEntity(Object level, Object targetPos, Object stack) {
        double itemHeightOffset = EntityTypeProxy.INSTANCE.getHeight(EntityTypesProxy.ITEM) / 2.0;
        double spawnX = BlockPosProxy.INSTANCE.getX(targetPos) + 0.5;
        double spawnY = BlockPosProxy.INSTANCE.getY(targetPos) + 0.5 - itemHeightOffset;
        double spawnZ = BlockPosProxy.INSTANCE.getZ(targetPos) + 0.5;
        Object spawnedItemEntity = ItemEntityProxy.INSTANCE.newInstance(level, spawnX, spawnY, spawnZ, stack);
        ItemEntityProxy.INSTANCE.setDefaultPickUpDelay(spawnedItemEntity);
        LevelWriterProxy.INSTANCE.addFreshEntity(level, spawnedItemEntity, null);
    }

    private static class Factory implements BlockBehaviorFactory<PlaceBlockBehavior> {

        @Override
        public PlaceBlockBehavior create(BlockDefinition block, ConfigSection section) {
            boolean whitelistMode = section.getBoolean("whitelist");
            Set<Key> blocks = section.getList("blocks", ConfigValue::getAsIdentifier).stream().collect(Collectors.toCollection(ObjectOpenHashSet::new));
            if (blocks.isEmpty() && !whitelistMode) {
                blocks = FacingTriggerableBlockBehavior.DEFAULT_BLACKLIST_BLOCKS;
            }
            return new PlaceBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "facing", Direction.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "triggered", Boolean.class),
                    blocks,
                    whitelistMode
            );
        }
    }
}
