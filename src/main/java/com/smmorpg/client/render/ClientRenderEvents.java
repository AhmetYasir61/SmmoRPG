package com.smmorpg.client.render;

import com.smmorpg.SmmoRPG;
import com.smmorpg.client.ClientState;
import com.smmorpg.combat.HitLocation;
import com.smmorpg.wound.WoundData;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.Set;

/** Attaches the wound layer to every humanoid renderer and hides severed limbs. */
@EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT)
public final class ClientRenderEvents {

    /** Remembers what we hid so we can restore it after the entity is drawn. */
    private static final java.util.Map<ModelPart, Boolean> HIDDEN = new java.util.IdentityHashMap<>();

    /**
     * Drives the third-person rig from the same {@link PoseState} the owning client wrote
     * for its own first-person view — the "what I do is what they see" half of the mod.
     *
     * <p>This runs off NeoForge's own event rather than a mixin into PlayerRenderer#render:
     * the event fires at exactly the point a mixin would have injected, and the loader
     * guarantees it across versions where a method signature might not survive.
     */
    @SubscribeEvent
    public static void onRenderPlayer(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre event) {
        var player = event.getEntity();
        PoseState pose = ClientState.pose(player.getId());
        var model = event.getRenderer().getModel();
        float partialTick = event.getPartialTick();

        float t = pose.progress(partialTick);
        // A cut is a fast strike and a slow recovery, so the curve is deliberately skewed.
        float strike = t < 0.35F ? t / 0.35F : 1.0F - (t - 0.35F) / 0.65F;

        switch (pose.animation) {
            case PoseState.ANIM_SLASH_DOWN -> {
                model.rightArm.xRot = -2.6F + strike * 3.4F;
                model.rightArm.zRot = -0.35F + strike * 0.5F;
            }
            case PoseState.ANIM_SLASH_RISING -> {
                model.rightArm.xRot = 0.9F - strike * 3.1F;
                model.rightArm.zRot = 0.4F - strike * 0.7F;
            }
            case PoseState.ANIM_SLASH_HORIZONTAL -> {
                model.rightArm.yRot = -1.5F + strike * 3.0F;
                model.rightArm.xRot = -1.4F;
                model.body.yRot = -0.35F + strike * 0.7F;
            }
            case PoseState.ANIM_THRUST -> {
                model.rightArm.xRot = -1.55F;
                model.rightArm.zRot = -0.1F;
                model.body.yRot = -0.2F * strike;
            }
            case PoseState.ANIM_PARRY -> {
                model.rightArm.xRot = -2.2F;
                model.leftArm.xRot = -1.9F;
                model.rightArm.zRot = 0.6F;
            }
            case PoseState.ANIM_GUARD -> {
                model.rightArm.xRot = -1.7F;
                model.leftArm.xRot = -1.5F;
            }
            case PoseState.ANIM_STAGGER -> {
                model.body.xRot = 0.35F * (1.0F - t);
                model.rightArm.xRot = 0.6F;
                model.leftArm.xRot = 0.6F;
            }
            case PoseState.ANIM_DRAW_BOW -> {
                model.rightArm.xRot = -1.4F;
                model.leftArm.xRot = -1.5F;
                model.rightArm.yRot = -0.5F;
            }
            default -> { }
        }

        // The head follows the real aim pitch rather than the smoothed network value.
        model.head.xRot = net.minecraft.util.Mth.clamp(
                pose.aimPitch(partialTick) * net.minecraft.util.Mth.DEG_TO_RAD, -1.5F, 1.5F);
    }

    @SubscribeEvent
    public static void onRenderPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        WoundData data = ClientState.wounds(entity.getId());
        if (data.severed().isEmpty()) return;
        if (!(event.getRenderer().getModel() instanceof HumanoidModel<?> model)) return;

        HIDDEN.clear();
        Set<HitLocation> severed = data.severedLocations();
        for (HitLocation loc : severed) {
            ModelPart part = partFor(model, loc);
            if (part == null) continue;
            HIDDEN.put(part, part.visible);
            part.visible = false;
        }
    }

    @SubscribeEvent
    public static void onRenderPost(RenderLivingEvent.Post<?, ?> event) {
        if (HIDDEN.isEmpty()) return;
        HIDDEN.forEach((part, wasVisible) -> part.visible = wasVisible);
        HIDDEN.clear();
    }

    private static ModelPart partFor(HumanoidModel<?> model, HitLocation loc) {
        return switch (loc) {
            case HEAD, NECK -> model.head;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_LEG -> model.leftLeg;
            case RIGHT_LEG -> model.rightLeg;
            default -> null;
        };
    }

    /** Mod-bus half: register the layer onto every living renderer that can carry it. */
    @EventBusSubscriber(modid = SmmoRPG.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static void addLayers(EntityRenderersEvent.AddLayers event) {
            for (net.minecraft.world.entity.EntityType<?> type
                    : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
                EntityRenderer<?> renderer = event.getRenderer((net.minecraft.world.entity.EntityType) type);
                if (renderer instanceof LivingEntityRenderer living) {
                    living.addLayer(new WoundRenderLayer(( RenderLayerParent) living));
                }
            }
            // Player renderers live in a separate map keyed by skin model.
            for (net.minecraft.client.resources.PlayerSkin.Model skin : event.getSkins()) {
                var renderer = event.getSkin(skin);
                if (renderer instanceof LivingEntityRenderer living) {
                    living.addLayer(new WoundRenderLayer((RenderLayerParent) living));
                }
            }
        }
    }
}
