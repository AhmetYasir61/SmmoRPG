package com.smmorpg.client.render;

import com.smmorpg.SmmoRPG;
import com.smmorpg.client.ClientState;
import com.smmorpg.client.render.AnimationApplier;
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
     * Drives the third-person rig from the keyframed animation the entity is actually
     * playing — the same clip, at the same time index, that the server used to decide when
     * the blade was live. What you see and what hits cannot drift apart, because they are
     * reading one timeline.
     *
     * <p>This runs off NeoForge's own event rather than a mixin into PlayerRenderer#render:
     * the event fires at exactly the point a mixin would have injected, and the loader
     * guarantees it across versions where a method signature might not survive.
     */
    @SubscribeEvent
    public static void onRenderPlayer(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre event) {
        var player = event.getEntity();
        AnimationApplier.apply(player, event.getRenderer().getModel(), event.getPartialTick(),
                AnimationApplier.weightFor(player, event.getPartialTick()));
    }

    /** Mobs animate through the very same clips; nothing here is player-only. */
    @SubscribeEvent
    public static void onRenderLivingPose(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player) return;
        if (!(event.getRenderer().getModel() instanceof HumanoidModel<?> model)) return;
        AnimationApplier.apply(event.getEntity(), model, event.getPartialTick(),
                AnimationApplier.weightFor(event.getEntity(), event.getPartialTick()));
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
