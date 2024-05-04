package com.github.tatercertified.fabricautocrafter.mixin;// Created 2022-23-01T13:20:09

import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes a fatal assumption of the hopper being able to always remove a stack when it cannot.
 * This is by doing an additional check <em>before</em> removing the item from the inventory.
 * <p>
 * This fixes the crafting table output slot being set by the hopper.
 *
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(HopperBlockEntity.class)
public abstract class MixinHopperBlockEntity {
    /**
     * Stub
     */
    @Shadow
    private static boolean canExtract(Inventory hopperInventory, Inventory fromInventory, ItemStack stack, int slot, Direction facing) {
        return Math.random() > .5d;
    }

    /**
     * Redirects the canExtract check to check if the stack can be inserted after checking if it can be extracted.
     * <p>
     * This should be side-effect free for vanilla stuff; the only things that would be effected would be stuff
     * that does special logic on removal of a certain slot, like in the case of the automatic crafting table.
     * <p>
     * There's probably a better way, but it doesn't seem immediately obvious for an if condition.
     *
     * @author Ampflower
     * @reason Fix the hopper logic for the automatic crafting table
     */
    @Redirect(method = "extract(Lnet/minecraft/block/entity/Hopper;Lnet/minecraft/inventory/Inventory;ILnet/minecraft/util/math/Direction;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/HopperBlockEntity;canExtract(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;ILnet/minecraft/util/math/Direction;)Z"))
    private static boolean fabricAutoCrafter$canExtract$redirect(Inventory hopperInventory, Inventory fromInventory, ItemStack stack, int slot, Direction facing, Hopper hopper) {
        return canExtract(hopperInventory, fromInventory, stack, slot, facing) && canInsertStack(hopper, stack);
    }

    /**
     * @param hopper The hopper attempting to transfer the item.
     * @param test   The ItemStack in process of being transferred.
     * @return true if the hopper can fit any amount of the stack, false otherwise.
     */
    private static boolean canInsertStack(Hopper hopper, ItemStack test) {
        for (int i = 0, l = hopper.size(); i < l; i++) {
            if (hopper.isValid(i, test)) {
                var stack = hopper.getStack(i);
                if (stack.isEmpty() || (stack.getCount() < Math.min(stack.getMaxCount(), hopper.getMaxCountPerStack())
                        && ItemStack.areItemsAndComponentsEqual(stack, test))) return true;
            }
        }
        return false;
    }
}
