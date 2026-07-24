package cn.gtemc.craftengine.block.behavior;

import cn.gtemc.craftengine.block.entity.SeatBlockEntityController;
import net.momirealms.craftengine.bukkit.block.behavior.BukkitBlockBehavior;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.EntityBlock;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.joml.Vector3f;

public final class SeatBlockBehavior extends BukkitBlockBehavior implements EntityBlock {
    public static final BlockBehaviorFactory<SeatBlockBehavior> FACTORY = new Factory();
    public final Vector3f offset;
    public final float yaw;
    public final boolean limitPlayerRotation;
    private int controllerId;

    public SeatBlockBehavior(BlockDefinition blockDefinition, Vector3f offset, float yaw, boolean limitPlayerRotation) {
        super(blockDefinition);
        this.offset = offset;
        this.yaw = yaw;
        this.limitPlayerRotation = limitPlayerRotation;
    }

    @Override
    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        BukkitServerPlayer player = (BukkitServerPlayer) context.getPlayer();
        if (player == null || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        player.swingHand(context.getHand());
        CEWorld world = context.getLevel().storageWorld();
        BlockEntity blockEntity = world.getBlockEntityAtIfLoaded(context.getClickedPos());
        if (blockEntity == null) return InteractionResult.PASS;
        SeatBlockEntityController controller = blockEntity.controller.get(SeatBlockEntityController.class, this.controllerId);
        controller.seat(player.platformPlayer());
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    @Override
    public BlockEntityController createBlockEntityController(BlockEntity blockEntity) {
        return new SeatBlockEntityController(blockEntity, this);
    }

    @Override
    public void initControllerId(int id) {
        this.controllerId = id;
    }

    private static class Factory implements BlockBehaviorFactory<SeatBlockBehavior> {
        private static final String[] LIMIT_PLAYER_ROTATION = new String[]{"limit_player_rotation", "limit-player-rotation"};

        @Override
        public SeatBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new SeatBlockBehavior(
                    block,
                    section.getVector3f("offset", ConfigConstants.ZERO_VECTOR3),
                    section.getFloat("yaw"),
                    section.getBoolean(LIMIT_PLAYER_ROTATION, true)
            );
        }
    }
}
