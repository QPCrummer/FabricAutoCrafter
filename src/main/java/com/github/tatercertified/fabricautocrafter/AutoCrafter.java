package com.github.tatercertified.fabricautocrafter;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.block.Blocks.CRAFTING_TABLE;

public class AutoCrafter extends Block implements PolymerBlock, BlockEntityProvider {

    protected AutoCrafter(AbstractBlock.Settings blockSettings) {
        super(blockSettings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        } else if (world.getBlockEntity(pos) instanceof AutoCraftingTableBlockEntity entity) {
            player.openHandledScreen(entity);
            player.incrementStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return CRAFTING_TABLE.getDefaultState();
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return state.hasBlockEntity();
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (!state.hasBlockEntity()) return 0;
        if (world.getBlockEntity(pos) instanceof AutoCraftingTableBlockEntity craftingTableBlockEntity) {
            int filled = 0;
            for (ItemStack stack : craftingTableBlockEntity.getHeldStacks()) {
                if (!stack.isEmpty()) filled++;
            }
            return (filled * 15) / 9;
        }
        return 0;
    }


    @Override
    public void onStateReplaced(BlockState oldState, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (oldState.getBlock() != newState.getBlock()) {
            if (world.getBlockEntity(pos) instanceof AutoCraftingTableBlockEntity entity) {
                ItemScatterer.spawn(world, pos, entity.getHeldStacks());
                if (!entity.getOutput().isEmpty()) {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), entity.getOutput());
                }
                world.updateNeighborsAlways(pos, this);
            }
            world.removeBlockEntity(pos);

            super.onStateReplaced(oldState, world, pos, newState, moved);
        }
    }

    @Override
    public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        if (world.getBlockEntity(pos) instanceof AutoCraftingTableBlockEntity entity) {
            ItemScatterer.spawn(world, pos, entity.getHeldStacks());
            if (!entity.getOutput().isEmpty()) {
                ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), entity.getOutput());
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.isOf(AutoCrafterMod.BLOCK) ? new AutoCraftingTableBlockEntity(pos, state) : null;
    }
}
