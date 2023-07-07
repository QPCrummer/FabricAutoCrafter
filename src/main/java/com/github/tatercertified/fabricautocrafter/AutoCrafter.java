package com.github.tatercertified.fabricautocrafter;

import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

public class AutoCrafter extends Block implements PolymerTexturedBlock, BlockEntityProvider {
    private final BlockState polymerBlockState;

    protected AutoCrafter(FabricBlockSettings blockSettings, BlockModelType type, String modelId) {
        super(blockSettings);

        this.polymerBlockState = PolymerBlockResourceUtils.requestBlock(
                type,
                PolymerBlockModel.of(new Identifier("autocrafter", modelId)));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        } else if (world.getBlockEntity(pos) instanceof CraftingTableBlockEntity entity) {
            player.openHandledScreen(entity);
            player.incrementStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return this.polymerBlockState.getBlock();
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return this.polymerBlockState;
    }

        @Override
    public boolean hasComparatorOutput(BlockState state) {
        return state.hasBlockEntity();
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (!state.hasBlockEntity()) return 0;
        if (world.getBlockEntity(pos) instanceof CraftingTableBlockEntity craftingTableBlockEntity) {
            int filled = 0;
            for (ItemStack stack : craftingTableBlockEntity.inventory) {
                if (!stack.isEmpty()) filled++;
            }
            return (filled * 15) / 9;
        }
        return 0;
    }


    @Override
    public void onStateReplaced(BlockState oldState, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (oldState.getBlock() != newState.getBlock()) {
            if (world.getBlockEntity(pos) instanceof CraftingTableBlockEntity entity) {
                ItemScatterer.spawn(world, pos, entity.inventory);
                if (!entity.output.isEmpty()) {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), entity.output);
                }
                world.updateNeighborsAlways(pos, this);
            }
            world.removeBlockEntity(pos);

            super.onStateReplaced(oldState, world, pos, newState, moved);
        }
    }

    @Override
    public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        if (world.getBlockEntity(pos) instanceof CraftingTableBlockEntity entity) {
            ItemScatterer.spawn(world, pos, entity.inventory);
            if (!entity.output.isEmpty()) {
                ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), entity.output);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.isOf(AutoCrafterMod.BLOCK) ? new CraftingTableBlockEntity(pos, state) : null;
    }
}
