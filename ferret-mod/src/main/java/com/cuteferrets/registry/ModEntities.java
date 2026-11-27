package com.cuteferrets.registry;

import com.cuteferrets.CuteFerrets;
import com.cuteferrets.entity.FerretEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CuteFerrets.MOD_ID);

    public static final RegistryObject<EntityType<FerretEntity>> FERRET = ENTITIES.register("ferret",
            () -> EntityType.Builder.of(FerretEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.45F)
                    .eyeHeight(0.35F)
                    .clientTrackingRange(10)
                    .build("ferret"));

    private ModEntities() {
    }
}
