package com.snailmod.registry;

import com.snailmod.SnailMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SnailMod.MODID);

    /** Shell colour (brown) and body colour (pale green) for the spawn egg. */
    public static final RegistryObject<Item> SNAIL_SPAWN_EGG = ITEMS.register("snail_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SNAIL, 0x8B5A2B, 0xB8C98A, new Item.Properties()));

    public static final RegistryObject<Item> SNAIL_SHELL = ITEMS.register("snail_shell",
            () -> new Item(new Item.Properties()));

    private ModItems() {}
}
