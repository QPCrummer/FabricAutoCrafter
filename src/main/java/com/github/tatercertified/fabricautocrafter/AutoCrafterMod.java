package com.github.tatercertified.fabricautocrafter;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent;

public class AutoCrafterMod implements ModInitializer {

    public static final Identifier IDENTIFIER = new Identifier("autocrafter", "autocrafter");
    public static final Block BLOCK = new AutoCrafter(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE).strength(2.5f, 2.5f));
    public static final BlockItem ITEM = new PolymerBlockItem(BLOCK, new Item.Settings(), Items.CRAFTING_TABLE);
    public static final BlockEntityType<AutoCraftingTableBlockEntity> TYPE = BlockEntityType.Builder.create(AutoCraftingTableBlockEntity::new, BLOCK).build(null);

    @Override
    public void onInitialize() {
        modifyEntriesEvent(ItemGroups.REDSTONE).register((content) -> content.add(ITEM));

        Registry.register(Registries.BLOCK, IDENTIFIER, BLOCK);
        Registry.register(Registries.ITEM, IDENTIFIER, ITEM);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, IDENTIFIER, TYPE);
        PolymerBlockUtils.registerBlockEntity(TYPE);
    }
}
