package com.cuteferrets.client;

import com.cuteferrets.CuteFerrets;
import com.cuteferrets.entity.FerretEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** A long, low noodle of a ferret: body, head with ears and snout, four stubby legs and a fluffy tail. */
public class FerretModel extends HierarchicalModel<FerretEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(CuteFerrets.id("ferret"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart legFrontLeft;
    private final ModelPart legFrontRight;
    private final ModelPart legBackLeft;
    private final ModelPart legBackRight;

    public FerretModel(ModelPart root) {
        this.root = root;
        ModelPart ferret = root.getChild("ferret");
        this.body = ferret.getChild("body");
        this.head = ferret.getChild("head");
        this.tail = ferret.getChild("tail");
        this.legFrontLeft = ferret.getChild("leg_front_left");
        this.legFrontRight = ferret.getChild("leg_front_right");
        this.legBackLeft = ferret.getChild("leg_back_left");
        this.legBackRight = ferret.getChild("leg_back_right");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition ferret = mesh.getRoot().addOrReplaceChild("ferret", CubeListBuilder.create(), PartPose.ZERO);

        ferret.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5F, -2.0F, -6.0F, 5.0F, 4.0F, 12.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));

        PartDefinition head = ferret.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-2.5F, -2.5F, -5.0F, 5.0F, 4.0F, 5.0F)
                        .texOffs(20, 16).addBox(-1.5F, -0.5F, -7.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 18.5F, -5.5F));
        head.addOrReplaceChild("ear_left", CubeListBuilder.create()
                        .texOffs(20, 20).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(2.0F, -2.0F, -2.0F, 0.0F, 0.0F, 0.3F));
        head.addOrReplaceChild("ear_right", CubeListBuilder.create()
                        .texOffs(26, 20).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-2.0F, -2.0F, -2.0F, 0.0F, 0.0F, -0.3F));

        ferret.addOrReplaceChild("tail", CubeListBuilder.create()
                        .texOffs(34, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, 5.5F, -0.35F, 0.0F, 0.0F));

        CubeListBuilder leg = CubeListBuilder.create().texOffs(34, 10).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F);
        ferret.addOrReplaceChild("leg_front_left", leg, PartPose.offset(1.5F, 21.0F, -4.0F));
        ferret.addOrReplaceChild("leg_front_right", leg, PartPose.offset(-1.5F, 21.0F, -4.0F));
        ferret.addOrReplaceChild("leg_back_left", leg, PartPose.offset(1.5F, 21.0F, 4.0F));
        ferret.addOrReplaceChild("leg_back_right", leg, PartPose.offset(-1.5F, 21.0F, 4.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(FerretEntity ferret, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        ModelPart whole = root.getChild("ferret");

        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;

        // bouncy little gallop: front and back pairs move together, like a real ferret's "inchworm" run
        float swing = Mth.cos(limbSwing * 0.9F) * 1.3F * limbSwingAmount;
        legFrontLeft.xRot = swing;
        legFrontRight.xRot = swing;
        legBackLeft.xRot = -swing;
        legBackRight.xRot = -swing;
        body.xRot = Mth.sin(limbSwing * 0.9F) * 0.08F * limbSwingAmount;

        tail.yRot = Mth.sin(ageInTicks * 0.12F) * 0.25F + Mth.cos(limbSwing * 0.6F) * 0.3F * limbSwingAmount;

        if (ferret.isInSittingPose()) {
            // flop down flat, paws stretched out: the classic "ferret pancake"
            whole.y += 2.0F;
            legFrontLeft.xRot = -1.45F;
            legFrontRight.xRot = -1.45F;
            legBackLeft.xRot = 1.45F;
            legBackRight.xRot = 1.45F;
            legFrontLeft.y -= 0.5F;
            legFrontRight.y -= 0.5F;
            legBackLeft.y -= 0.5F;
            legBackRight.y -= 0.5F;
            head.y += 0.5F;
            tail.xRot = -0.05F;
            tail.yRot = Mth.sin(ageInTicks * 0.05F) * 0.15F + 0.4F;
        } else if (ferret.isDigging()) {
            head.xRot = 0.7F;
            head.y += 0.8F;
            float dig = Mth.sin(ageInTicks * 1.7F);
            legFrontLeft.xRot = -0.6F + dig * 0.9F;
            legFrontRight.xRot = -0.6F - dig * 0.9F;
            body.xRot = 0.12F;
            tail.xRot = 0.15F;
            tail.yRot = Mth.sin(ageInTicks * 0.6F) * 0.5F;
        } else if (ferret.isDancing()) {
            // weasel war dance: arched back, wiggling, tail up
            body.xRot = -0.15F;
            whole.zRot = Mth.sin(ageInTicks * 0.9F) * 0.25F;
            head.zRot = Mth.sin(ageInTicks * 0.9F + 1.0F) * 0.3F;
            head.xRot = -0.25F;
            tail.xRot = 0.35F;
            tail.yRot = Mth.sin(ageInTicks * 1.4F) * 0.6F;
        }

        float headScale = this.young ? 1.35F : 1.0F;
        head.xScale = headScale;
        head.yScale = headScale;
        head.zScale = headScale;
    }
}
