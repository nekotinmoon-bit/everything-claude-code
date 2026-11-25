package com.snailmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.snailmod.SnailMod;
import com.snailmod.entity.SnailEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class SnailRenderer extends MobRenderer<SnailEntity, SnailModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SnailMod.MODID, "textures/entity/snail.png");

    public SnailRenderer(EntityRendererProvider.Context context) {
        super(context, new SnailModel(context.bakeLayer(SnailModel.LAYER)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(SnailEntity snail) {
        return TEXTURE;
    }

    @Override
    protected void scale(SnailEntity snail, PoseStack poseStack, float partialTick) {
        if (snail.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }
}
