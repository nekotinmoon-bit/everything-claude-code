package com.cuteferrets.registry;

import com.cuteferrets.CuteFerrets;
import com.cuteferrets.item.FerretTreatItem;
import com.cuteferrets.item.FerretWhistleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CuteFerrets.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CuteFerrets.MOD_ID);

    public static final RegistryObject<Item> FERRET_SPAWN_EGG = ITEMS.register("ferret_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.FERRET, 0x8A5A3B, 0xF2E3C6, new Item.Properties()));

    public static final RegistryObject<Item> FERRET_WHISTLE = ITEMS.register("ferret_whistle",
            () -> new FerretWhistleItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> FERRET_TREAT = ITEMS.register("ferret_treat",
            () -> new FerretTreatItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cuteferrets"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> FERRET_TREAT.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(FERRET_SPAWN_EGG.get());
                output.accept(FERRET_WHISTLE.get());
                output.accept(FERRET_TREAT.get());
            })
            .build());

    private ModItems() {
    }
}
