package com.github.tatercertified.fabricautocrafter.mixin;

import com.github.tatercertified.fabricautocrafter.AutoCraftingTableBlockEntity;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import me.jellysquid.mods.lithium.common.block.entity.SleepingBlockEntity;
import me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.WrappedBlockEntityTickInvokerAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.recipe.RecipeUnlocker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import org.spongepowered.asm.mixin.Mixin;

@IfModLoaded(value = "lithium")
@Mixin(AutoCraftingTableBlockEntity.class)
public abstract class LithiumBlockEntityOptimizations extends LockableContainerBlockEntity implements SidedInventory, RecipeUnlocker, RecipeInputProvider, SleepingBlockEntity {

    // Block Entity Sleeping
    private WrappedBlockEntityTickInvokerAccessor tickWrapper = null;
    private BlockEntityTickInvoker sleepingTicker = null;

    protected LithiumBlockEntityOptimizations(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    public WrappedBlockEntityTickInvokerAccessor lithium$getTickWrapper() {
        return this.tickWrapper;
    }

    @Override
    public void lithium$setTickWrapper(WrappedBlockEntityTickInvokerAccessor tickWrapper) {
        this.tickWrapper = tickWrapper;
        this.lithium$setSleepingTicker(null);
    }

    @Override
    public BlockEntityTickInvoker lithium$getSleepingTicker() {
        return this.sleepingTicker;
    }

    @Override
    public void lithium$setSleepingTicker(BlockEntityTickInvoker sleepingTicker) {
        this.sleepingTicker = sleepingTicker;
    }

    @Override
    public boolean lithium$startSleeping() {
        if (this.isSleeping()) {
            return false;
        }

        WrappedBlockEntityTickInvokerAccessor tickWrapper = this.lithium$getTickWrapper();
        if (tickWrapper != null) {
            this.lithium$setSleepingTicker(tickWrapper.getWrapped());
            tickWrapper.callSetWrapped(SleepingBlockEntity.SLEEPING_BLOCK_ENTITY_TICKER);
            return true;
        }
        return false;
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        lithium$startSleeping();
    }
}
