package com.cuteferrets.client;

import com.cuteferrets.entity.FerretEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class FerretRenderer extends MobRenderer<FerretEntity, FerretModel> {
    public FerretRenderer(EntityRendererProvider.Context context) {
        super(context, new FerretModel(context.bakeLayer(FerretModel.LAYER)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(FerretEntity ferret) {
        return ferret.getVariant().texture();
    }

    @Override
    protected void scale(FerretEntity ferret, PoseStack poseStack, float partialTick) {
        if (ferret.isBaby()) {
            poseStack.scale(0.55F, 0.55F, 0.55F);
        }
    }
}
