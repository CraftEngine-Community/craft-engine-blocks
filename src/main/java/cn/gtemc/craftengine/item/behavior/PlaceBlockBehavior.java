package cn.gtemc.craftengine.item.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitCustomBlockStateWrapper;
import net.momirealms.craftengine.bukkit.util.AdventureModeUtils;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Cancellable;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.libraries.adventure.text.Component;
import net.momirealms.craftengine.libraries.adventure.text.format.NamedTextColor;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public final class PlaceBlockBehavior extends ItemBehavior {
    public static final ItemBehaviorFactory<PlaceBlockBehavior> FACTORY = new Factory();
    private final LazyReference<@Nullable BlockStateWrapper> blockStateWrapper;
    private final String blockState;
    private final SoundData placeSound;

    private PlaceBlockBehavior(String blockState, SoundData placeSound) {
        this.blockStateWrapper = LazyReference.lazyReference(() -> CraftEngine.instance().blockManager().createBlockState(blockState));
        this.blockState = blockState;
        this.placeSound = placeSound;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        return this.place(new BlockPlaceContext(context));
    }

    public InteractionResult place(BlockPlaceContext context) {
        BlockStateWrapper state = this.blockStateWrapper.get();
        if (state == null) {
            CraftEngine.instance().logger().warn("Failed to place unknown block state" + this.blockState);
            return InteractionResult.FAIL;
        }
        if (!context.canPlace()) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        World level = context.getLevel();
        int maxY = level.worldHeight().getMaxBuildHeight() - 1;
        if (context.getClickedFace() == Direction.UP && pos.y() > maxY) {
            if (player != null) {
                player.sendActionBar(Component.translatable("build.tooHigh").arguments(Component.text(maxY)).color(NamedTextColor.RED));
            }
            return InteractionResult.FAIL;
        }

        BlockPos againstPos = context.getAgainstPos();

        if (player != null && player.isAdventureMode() && !AdventureModeUtils.canPlace(context.getItem(), context.getLevel(), againstPos, state.minecraftState())) {
            return InteractionResult.FAIL;
        }

        org.bukkit.World world = (org.bukkit.World) context.getLevel().platformWorld();
        Location placeLocation = new Location(world, pos.x(), pos.y(), pos.z());
        Block bukkitBlock = world.getBlockAt(placeLocation);
        Block againstBlock = world.getBlockAt(againstPos.x(), againstPos.y(), againstPos.z());
        org.bukkit.entity.Player bukkitPlayer = player != null ? (org.bukkit.entity.Player) player.platformPlayer() : null;
        BlockState previousState = bukkitBlock.getState();

        level.setBlockState(pos, state, UpdateFlags.UPDATE_ALL);

        if (player != null) {
            @SuppressWarnings("UnstableApiUsage")
            BlockPlaceEvent bukkitPlaceEvent = new BlockPlaceEvent(
                    bukkitBlock, previousState, againstBlock,
                    ItemStackUtils.getBukkitStack(context.getItem()),
                    bukkitPlayer, true,
                    context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND
            );
            if (EventUtils.fireAndCheckCancel(bukkitPlaceEvent)) {
                previousState.update(true, false);
                return InteractionResult.FAIL;
            }
            if (!player.isCreativeMode()) {
                Item item = context.getItem();
                item.shrink(1);
            }
            player.swingHand(context.getHand());
        }

        WorldPosition position = new WorldPosition(context.getLevel(), pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        if (state instanceof BukkitCustomBlockStateWrapper custom) {
            Cancellable dummy = Cancellable.dummy();
            PlayerOptionalContext functionContext = PlayerOptionalContext.of(player,
                    ContextHolder.builder()
                            .withParameter(DirectContextParameters.BLOCK, new BukkitExistingBlock(bukkitBlock))
                            .withParameter(DirectContextParameters.POSITION, position)
                            .withParameter(DirectContextParameters.EVENT, dummy)
                            .withParameter(DirectContextParameters.HAND, context.getHand())
                            .withParameter(DirectContextParameters.ITEM_IN_HAND, context.getItem())
            );
            ImmutableBlockState immutableBlockState = custom.getImmutableBlockState().orElseThrow();
            immutableBlockState.owner().value().execute(functionContext, EventTrigger.PLACE);
            if (dummy.isCancelled()) {
                previousState.update(true, false);
                return InteractionResult.FAIL;
            }
            level.playBlockSound(position, immutableBlockState.settings().sounds().placeSound());
        } else if (this.placeSound != null) {
            level.playBlockSound(position, this.placeSound);
        }
        world.sendGameEvent(bukkitPlayer, GameEvent.BLOCK_PLACE, new Vector(pos.x(), pos.y(), pos.z()));
        return InteractionResult.SUCCESS;
    }

    private static class Factory implements ItemBehaviorFactory<PlaceBlockBehavior> {
        private static final String[] BLOCK_STATES = new String[] {"block_states", "block_state", "block-states", "block-state", "blockstate", "blockstates", "state", "states"};

        @Override
        public PlaceBlockBehavior create(Pack pack, Path path, Key id, ConfigSection section) {
            ConfigSection soundSection = section.getSection("sounds");
            return new PlaceBlockBehavior(
                    section.getNonEmptyString(BLOCK_STATES),
                    soundSection == null ? null : section.getValue("place", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1))
            );
        }
    }
}
