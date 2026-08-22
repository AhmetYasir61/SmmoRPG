package com.smmorpg.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.smmorpg.SmmoRPG;
import com.smmorpg.combat.HitLocation;
import com.smmorpg.wound.Wound;
import com.smmorpg.wound.WoundData;
import com.smmorpg.client.ClientState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws the cuts.
 *
 * <p>Each wound becomes one camera-independent quad, positioned by the wound's own UV
 * inside the struck body part and rotated by the swing angle. The decal atlas is high
 * resolution while the model underneath keeps its vanilla texture — that is what makes a
 * gash read as crisp and detailed on an otherwise ordinary-looking zombie.
 */
public class WoundRenderLayer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {

    private static final ResourceLocation SLASH = SmmoRPG.id("textures/decal/slash.png");
    private static final ResourceLocation PIERCE = SmmoRPG.id("textures/decal/pierce.png");
    private static final ResourceLocation BRUISE = SmmoRPG.id("textures/decal/bruise.png");
    private static final ResourceLocation STUMP = SmmoRPG.id("textures/decal/stump.png");
    private static final ResourceLocation BURN = SmmoRPG.id("textures/decal/burn.png");

    public WoundRenderLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poses, MultiBufferSource buffers, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        WoundData data = ClientState.wounds(entity.getId());
        if (data.wounds().isEmpty()) return;
        if (!(getParentModel() instanceof HumanoidModel<?> humanoid)) return;

        for (Wound wound : data.wounds()) {
            ModelPart part = partFor(humanoid, wound.hitLocation());
            if (part == null || !part.visible) continue;
            drawDecal(poses, buffers, packedLight, part, wound);
        }
    }

    private void drawDecal(PoseStack poses, MultiBufferSource buffers, int light,
                           ModelPart part, Wound wound) {
        poses.pushPose();
        // Enter the struck part's own space, so the decal follows the limb as it animates.
        part.translateAndRotate(poses);

        // Map the wound's normalised UV onto the part's cube, then lift it off the
        // surface by a hair so it never z-fights with the skin.
        float w = 8.0F, h = 12.0F;
        poses.translate((wound.u() - 0.5F) * w / 16.0F,
                        (0.5F - wound.v()) * h / 16.0F,
                        -0.001F - 0.126F);
        poses.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(wound.angle()));

        float size = (0.18F + wound.length() * 0.35F) * (1.0F + wound.depth() * 0.4F);
        float alpha = wound.visibility();
        // A fresh cut is bright; as it closes it dries toward a dark scar.
        float r = 0.62F - wound.closure() * 0.28F;
        float g = 0.06F + wound.closure() * 0.10F;
        float b = 0.06F + wound.closure() * 0.08F;

        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(textureFor(wound)));
        Matrix4f pose = poses.last().pose();
        Matrix3f normal = new Matrix3f(poses.last().normal());

        quad(vc, pose, normal, size, r, g, b, alpha, light);

        poses.popPose();
    }

    private void quad(VertexConsumer vc, Matrix4f pose, Matrix3f normal, float s,
                      float r, float g, float b, float a, int light) {
        vertex(vc, pose, -s, -s, 0.0F, 0.0F, r, g, b, a, light);
        vertex(vc, pose,  s, -s, 1.0F, 0.0F, r, g, b, a, light);
        vertex(vc, pose,  s,  s, 1.0F, 1.0F, r, g, b, a, light);
        vertex(vc, pose, -s,  s, 0.0F, 1.0F, r, g, b, a, light);
    }

    private void vertex(VertexConsumer vc, Matrix4f pose, float x, float y, float u, float v,
                        float r, float g, float b, float a, int light) {
        vc.addVertex(pose, x, y, 0.0F)
          .setColor(r, g, b, a)
          .setUv(u, v)
          .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
          .setLight(light)
          .setNormal(0.0F, 0.0F, -1.0F);
    }

    private ResourceLocation textureFor(Wound wound) {
        return switch (wound.type()) {
            case Wound.TYPE_PIERCE -> PIERCE;
            case Wound.TYPE_BLUNT -> BRUISE;
            case Wound.TYPE_SEVER -> STUMP;
            case Wound.TYPE_BURN -> BURN;
            default -> SLASH;
        };
    }

    private ModelPart partFor(HumanoidModel<?> model, HitLocation loc) {
        return switch (loc) {
            case HEAD, NECK -> model.head;
            case TORSO -> model.body;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_LEG -> model.leftLeg;
            case RIGHT_LEG -> model.rightLeg;
            default -> null;
        };
    }
}
