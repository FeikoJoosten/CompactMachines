package org.dave.cm2.world.tools;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.dave.cm2.block.BlockMiniaturizationFluid;
import org.dave.cm2.init.Blockss;
import org.dave.cm2.miniaturization.MiniaturizationRecipe;
import org.dave.cm2.miniaturization.MiniaturizationRecipes;

import java.util.ArrayList;
import java.util.List;

public class CraftingTools {
    private static final List<BlockPos> craftingLocks = new ArrayList<>();

    public static boolean tryCrafting(World world, BlockPos contactPos, Item catalyst) {
        // Step 0: Find the most top left corner of the miniaturization fluid
        int maxSearchStepsFluid = 32;

        BlockPos iterationPos = contactPos;
        for(int searchStep = 0; searchStep < maxSearchStepsFluid; searchStep++) {
            boolean moving = false;
            if(world.getBlockState(iterationPos.north()).getBlock() == Blockss.miniaturizationFluidBlock) {
                iterationPos = iterationPos.north();
                moving = true;
            }

            if(world.getBlockState(iterationPos.west()).getBlock() == Blockss.miniaturizationFluidBlock) {
                iterationPos = iterationPos.west();
                moving = true;
            }

            if(world.getBlockState(iterationPos.up()).getBlock() == Blockss.miniaturizationFluidBlock) {
                iterationPos = iterationPos.up();
                moving = true;
            }

            if(!moving) {
                break;
            }
        }

        BlockPos topNorthWestCornerPos = iterationPos.down().south().east();

        // Step 1: Find top left corner of crafting area
        // 1a: Get the block one level down
        Block initialBlock = world.getBlockState(topNorthWestCornerPos).getBlock();

        // 1c: Go to the highest x and z values that still have the same block,
        //     this is the most south-west corner possible
        BlockPos craftingCornerPos = topNorthWestCornerPos;

        int maxSearchSteps = 32;
        for(int searchStep = 0; searchStep < maxSearchSteps; searchStep++) {
            boolean moving = false;
            if(world.getBlockState(craftingCornerPos.south()).getBlock() == initialBlock) {
                craftingCornerPos = craftingCornerPos.south();
                moving = true;
            }

            if(world.getBlockState(craftingCornerPos.east()).getBlock() == initialBlock) {
                craftingCornerPos = craftingCornerPos.east();
                moving = true;
            }

            if(!moving) {
                break;
            }
        }

        if(craftingLocks.contains(craftingCornerPos)) {
            return false;
        }

        craftingLocks.add(craftingCornerPos);

        // Step 2: Depending on the recipe we need to check a few blocks
        boolean success = false;
        for(MiniaturizationRecipe recipe : MiniaturizationRecipes.getRecipes()) {
            if(recipe.getCatalyst() != catalyst) {
                continue;
            }

            if(testAndConsumeRecipe(world, craftingCornerPos, recipe.getSourceBlock(), recipe.getWidth(), recipe.getHeight(), recipe.getDepth(), recipe.isRequiresFloor())) {
                recipe.spawnResultInWorld(world, craftingCornerPos);
                success = true;
                break;
            }
        }

        craftingLocks.remove(craftingCornerPos);

        return success;
    }

    private static void drainRecipeFluid(World world, BlockPos cornerPos, int width, int height, int depth) {
        cornerPos = cornerPos.south().east().up();

        for(BlockPos pos : StructureTools.getCubePositions(cornerPos, width+2, height+1, depth+2, false)) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() == Blockss.miniaturizationFluidBlock && state.getValue(BlockMiniaturizationFluid.LEVEL) == 0) {
                world.setBlockState(pos, state.withProperty(BlockMiniaturizationFluid.LEVEL, 1));
            }
        }
    }

    private static boolean isCubeMadeOfBlocks(World world, BlockPos cornerPos, Block block, int width, int height, int depth, boolean requireFloor) {
        for(BlockPos pos : StructureTools.getCubePositions(cornerPos, width, height, depth, requireFloor)) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() != block) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasSurroundingFluid(World world, BlockPos cornerPos, int width, int height, int depth) {
        cornerPos = cornerPos.south().east().up();
        return isCubeMadeOfBlocks(world, cornerPos, Blockss.miniaturizationFluidBlock, width+2, height+1, depth+2, false);
    }

    private static boolean testAndConsumeRecipe(World world, BlockPos cornerPos, Block block, int width, int height, int depth, boolean requireFloor) {
        if(!isCubeMadeOfBlocks(world, cornerPos, block, width, height, depth, false)) {
            return false;
        }

        if(!hasSurroundingFluid(world, cornerPos, width, height, depth)) {
            return false;
        }

        StructureTools.getCubePositions(cornerPos, width, height, depth, true).forEach(world::setBlockToAir);
        drainRecipeFluid(world, cornerPos, width, height, depth);
        return true;
    }
}
