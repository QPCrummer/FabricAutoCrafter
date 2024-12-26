package com.github.tatercertified.fabricautocrafter;

import com.github.tatercertified.fabricautocrafter.mixin.CraftingInventoryMixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AutoCraftingTableBlockEntity extends LockableContainerBlockEntity implements SidedInventory, RecipeUnlocker, RecipeInputInventory {

    private static final int[] OUTPUT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int[] INPUT_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int GRID_WIDTH = 3;
    private static final int GRID_HEIGHT = 3;

    private final List<AutoCraftingTableContainer> openContainers = new ArrayList<>();
    private final CraftingInventory craftingInventory = new CraftingInventory(null, 3, 3);
    public DefaultedList<ItemStack> inventory;
    private ItemStack output = ItemStack.EMPTY;
    private RecipeEntry<?> lastRecipe;
    private static final RecipeCache recipeCache = new RecipeCache(10);

    public AutoCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(AutoCrafterMod.TYPE, pos, state);
        this.inventory = DefaultedList.ofSize(9, ItemStack.EMPTY);
        ((CraftingInventoryMixin) craftingInventory).setInventory(this.inventory);
    }

    public CraftingInventory bindInventory(ScreenHandler handler) {
        ((CraftingInventoryMixin) craftingInventory).setHandler(handler);
        return craftingInventory;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
        if (!output.isEmpty()) {
            nbt.put("Output", output.toNbt(registryLookup));
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
        this.output = ItemStack.fromNbtOrEmpty(registryLookup, nbt.getCompound("Output"));
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("block.autocrafter.autocrafter");
    }

    @Override
    public int getWidth() {
        return GRID_WIDTH;
    }

    @Override
    public int getHeight() {
        return GRID_HEIGHT;
    }

    @Override
    public DefaultedList<ItemStack> getHeldStacks() {
        return this.inventory;
    }

    public ItemStack getOutput() {
        return this.output;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
        this.inventory = inventory;
    }

    @Override
    protected ScreenHandler createScreenHandler(int id, PlayerInventory playerInventory) {
        final AutoCraftingTableContainer container = new AutoCraftingTableContainer(id, playerInventory, this);
        this.openContainers.add(container);
        return container;
    }

    @Override
    public int[] getAvailableSlots(Direction dir) {
        return (dir == Direction.DOWN && (!output.isEmpty() || (!quickEscape() && !getCurrentRecipe().isEmpty()))) ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return slot > 0 && getStack(slot).isEmpty();
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot != 0 || !output.isEmpty() || (!quickEscape() && !getCurrentRecipe().isEmpty());
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot != 0 && slot <= size();
    }

    @Override
    public int size() {
        return 10;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.inventory) {
            if (!stack.isEmpty()) return false;
        }
        return output.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot > 0) return this.inventory.get(slot - 1);
        if (!output.isEmpty()) return output;
        return quickEscape()? ItemStack.EMPTY : getCurrentRecipe();
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot == 0) {
            if (output.isEmpty()) output = craft();
            return output.split(amount);
        }
        return Inventories.splitStack(this.inventory, slot - 1, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot == 0) {
            ItemStack output = this.output;
            this.output = ItemStack.EMPTY;
            return output;
        }
        return Inventories.removeStack(this.inventory, slot - 1);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot == 0) {
            output = stack;
            return;
        }
        inventory.set(slot - 1, stack);
        markDirty();
    }

    @Override
    public void markDirty() {
        super.markDirty();
        for (AutoCraftingTableContainer c : openContainers) c.onContentChanged(this);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return player.getBlockPos().getSquaredDistance(this.pos) <= 64.0D;
    }

    @Override
    public void provideRecipeInputs(RecipeFinder finder) {
        for (ItemStack stack : this.inventory) finder.addInput(stack);
    }

    @Override
    public void setLastRecipe(@Nullable RecipeEntry<?> recipe) {
        lastRecipe = recipe;
    }

    @Override
    public RecipeEntry<?> getLastRecipe() {
        return lastRecipe;
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    private ItemStack getCurrentRecipe() {
        BlockEntity craftingRecipeInput = this.world.getBlockEntity(pos);
        if (craftingRecipeInput instanceof AutoCraftingTableBlockEntity autoCraftingTableBlockEntity) {
            CraftingRecipeInput var11 = autoCraftingTableBlockEntity.createRecipeInput();
            Optional<RecipeEntry<CraftingRecipe>> optional = getCraftingRecipe((ServerWorld) this.world, var11);
            if (optional.isPresent()) {
                RecipeEntry<CraftingRecipe> recipeEntry = optional.get();
                return recipeEntry.value().craft(var11, this.world.getRegistryManager());
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean quickEscape() {
        return this.world == null || this.isEmpty();
    }

    private ItemStack craft() {
        if (quickEscape()) return ItemStack.EMPTY;
        BlockEntity craftingRecipeInput = this.world.getBlockEntity(pos);
        if (craftingRecipeInput instanceof AutoCraftingTableBlockEntity autoCraftingTableBlockEntity) {
            ItemStack itemStack = getCurrentRecipe();
            if (!itemStack.isEmpty()) {
                itemStack.onCraftByCrafter(this.world);
                autoCraftingTableBlockEntity.getHeldStacks().forEach((stack) -> {
                    if (!stack.isEmpty()) {
                        stack.decrement(1);
                    }
                });
                autoCraftingTableBlockEntity.markDirty();
                return itemStack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static Optional<RecipeEntry<CraftingRecipe>> getCraftingRecipe(ServerWorld world, CraftingRecipeInput input) {
        return recipeCache.getRecipe(world, input);
    }

    public CraftingInventory unsetHandler() {
        ((CraftingInventoryMixin) craftingInventory).setHandler(null);
        return craftingInventory;
    }

    public void onContainerClose(AutoCraftingTableContainer container) {
        this.openContainers.remove(container);
    }
}
