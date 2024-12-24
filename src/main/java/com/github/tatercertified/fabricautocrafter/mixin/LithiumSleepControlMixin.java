package com.github.tatercertified.fabricautocrafter.mixin;

import com.github.tatercertified.fabricautocrafter.AutoCraftingTableBlockEntity;
import com.github.tatercertified.fabricautocrafter.AutoCraftingTableContainer;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.caffeinemc.mods.lithium.common.block.entity.SleepingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@IfModLoaded(value = "lithium")
@Mixin(AutoCraftingTableContainer.class)
public class LithiumSleepControlMixin {

    @Shadow
    AutoCraftingTableBlockEntity blockEntity;

    @Inject(method = "onContentChanged", at = @At("TAIL"))
    public void injectSleep(CallbackInfo ci) {
        ((SleepingBlockEntity)blockEntity).lithium$startSleeping();
    }

    @Inject(method = "onContentChanged", at = @At("HEAD"))
    public void injectWakeUp(CallbackInfo ci) {
        ((SleepingBlockEntity)blockEntity).wakeUpNow();
    }
}
