package com.snailmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.snailmod.SnailMod;
import com.snailmod.entity.SnailEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class SnailModel extends EntityModel<SnailEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SnailMod.MODID, "snail"), "main");

    private static final float BODY_Y = 24.0F;
    private static final float SHELL_Y = 22.0F;
    private static final float STALK_Y = 22.0F;

    private final ModelPart body;
    private final ModelPart shell;
    private final ModelPart leftStalk;
    private final ModelPart rightStalk;

    public SnailModel(ModelPart root) {
        this.body = root.getChild("body");
        this.shell = root.getChild("shell");
        this.leftStalk = root.getChild("left_stalk");
        this.rightStalk = root.getChild("right_stalk");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // The foot: a long, flat slab touching the ground.
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, -2.0F, -6.0F, 4.0F, 2.0F, 11.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));

        // The shell sits on the back half of the foot.
        root.addOrReplaceChild("shell", CubeListBuilder.create()
                        .texOffs(0, 13).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, SHELL_Y, 1.5F));

        // Eye stalks: a thin stem with an eyeball on top.
        CubeListBuilder stalk = CubeListBuilder.create()
                .texOffs(30, 0).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F)
                .texOffs(34, 0).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 2.0F, 2.0F);
        root.addOrReplaceChild("left_stalk", stalk, PartPose.offset(1.2F, STALK_Y, -5.0F));
        root.addOrReplaceChild("right_stalk", stalk, PartPose.offset(-1.2F, STALK_Y, -5.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(SnailEntity snail, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float partialTick = ageInTicks - snail.tickCount;
        float hide = snail.getHideProgress(partialTick);
        float out = 1.0F - hide;

        // The foot stretches and contracts as the snail crawls.
        float crawl = Mth.sin(limbSwing * 0.8F) * limbSwingAmount;
        this.body.zScale = (1.0F + 0.12F * crawl) * Mth.lerp(hide, 1.0F, 0.4F);
        this.body.yScale = Mth.lerp(hide, 1.0F, 0.3F);
        this.body.visible = hide < 0.98F;

        // When hiding, the shell drops onto the ground.
        this.shell.y = SHELL_Y + 2.0F * hide;
        this.shell.z = 1.5F - 0.15F * crawl;
        this.shell.xRot = 0.05F * Mth.sin(ageInTicks * 0.05F) * out;

        // Eye stalks follow the head and wobble a little; they retract when hiding.
        float yaw = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        float pitch = headPitch * Mth.DEG_TO_RAD * 0.6F;
        float wobble = Mth.sin(ageInTicks * 0.12F) * 0.08F;
        setupStalk(this.leftStalk, yaw, pitch + wobble, 0.15F, out);
        setupStalk(this.rightStalk, yaw, pitch - wobble, -0.15F, out);
    }

    private static void setupStalk(ModelPart stalk, float yaw, float pitch, float splay, float out) {
        stalk.yRot = yaw;
        stalk.xRot = 0.25F + pitch;
        stalk.zRot = splay;
        stalk.yScale = out;
        stalk.y = STALK_Y + 1.0F * (1.0F - out);
        stalk.visible = out > 0.02F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        this.body.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.shell.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.leftStalk.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.rightStalk.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
