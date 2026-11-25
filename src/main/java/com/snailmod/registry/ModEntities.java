package com.snailmod.registry;

import com.snailmod.SnailMod;
import com.snailmod.entity.SnailEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SnailMod.MODID);

    public static final RegistryObject<EntityType<SnailEntity>> SNAIL = ENTITIES.register("snail",
            () -> EntityType.Builder.of(SnailEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.5F)
                    .eyeHeight(0.35F)
                    .clientTrackingRange(8)
                    .build("snail"));

    private ModEntities() {}
}
