package com.snailmod;

import com.snailmod.entity.SnailEntity;
import com.snailmod.registry.ModEntities;
import com.snailmod.registry.ModItems;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SnailMod.MODID)
public class SnailMod {
    public static final String MODID = "snailmod";

    public SnailMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);

        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::registerSpawnPlacements);
        modBus.addListener(this::addCreative);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SNAIL.get(), SnailEntity.createAttributes().build());
    }

    private void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.SNAIL.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SnailEntity::checkSnailSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.SNAIL_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.SNAIL_SHELL);
        }
    }
}
