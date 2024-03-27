package folk.sisby.surveyor.util;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;

public class RaycastUtil {
    public static HitResult playerViewRaycast(PlayerEntity player, int renderDistance) {
        // Calculate View Distance to Rendering Cylinder
        Vec3d cameraPos = player.getCameraPosVec(1.0F);
        double pitch = player.getPitch(1.0F);
        double phi = -pitch * (float) (Math.PI / 180.0);
        int blockRadius = renderDistance << 4;
        double y = blockRadius * Math.tan(phi);
        double distance;
        double bottom = player.getWorld().getBottomY() - cameraPos.y;
        double top = player.getWorld().getTopY() - cameraPos.y;
        if (y < bottom || y > top) { // Distance To Circular Planes
            distance = Math.abs(MathHelper.clamp(y, bottom, top) / Math.sin(phi));
        } else { // Distance To Curved Surface
            distance = Math.sqrt(y * y + blockRadius * blockRadius);
        }

        // Scale Cartesian Rotation Vector By Distance
        Vec3d cameraRotation = player.getRotationVec(1.0F);
        Vec3d endPos = cameraPos.add(cameraRotation.x * distance, cameraRotation.y * distance, cameraRotation.z * distance);
        return BlockView.raycast(
            cameraPos,
            endPos,
            new RaycastContext(
                cameraPos, endPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player
            ),
            (innerContext, pos) -> {
                ChunkPos chunkPos = new ChunkPos(pos);
                if (!player.getWorld().getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                    Vec3d vec3d = innerContext.getStart().subtract(innerContext.getEnd());
                    return BlockHitResult.createMissed(pos.toCenterPos(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), pos);
                }
                BlockState blockState = player.getWorld().getBlockState(pos);
                FluidState fluidState = player.getWorld().getFluidState(pos);
                Vec3d vec3d = innerContext.getStart();
                Vec3d vec3d2 = innerContext.getEnd();
                VoxelShape voxelShape = innerContext.getBlockShape(blockState, player.getWorld(), pos);
                BlockHitResult blockHitResult = player.getWorld().raycastBlock(vec3d, vec3d2, pos, voxelShape, blockState);
                VoxelShape voxelShape2 = innerContext.getFluidShape(fluidState, player.getWorld(), pos);
                BlockHitResult blockHitResult2 = voxelShape2.raycast(vec3d, vec3d2, pos);
                double d = blockHitResult == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(blockHitResult.getPos());
                double e = blockHitResult2 == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(blockHitResult2.getPos());
                return d <= e ? blockHitResult : blockHitResult2;
            }, innerContext -> {
                Vec3d vec3d = innerContext.getStart().subtract(innerContext.getEnd());
                return BlockHitResult.createMissed(innerContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(innerContext.getEnd()));
            }
        );
    }
}
