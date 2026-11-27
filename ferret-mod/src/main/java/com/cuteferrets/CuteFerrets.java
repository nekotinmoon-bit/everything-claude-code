package com.cuteferrets;

import com.cuteferrets.registry.ModEntities;
import com.cuteferrets.registry.ModItems;
import com.cuteferrets.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CuteFerrets.MOD_ID)
public class CuteFerrets {
    public static final String MOD_ID = "cuteferrets";

    public CuteFerrets(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);
        ModMenus.MENUS.register(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
