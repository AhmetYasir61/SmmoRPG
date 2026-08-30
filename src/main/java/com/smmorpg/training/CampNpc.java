package com.smmorpg.training;

import com.smmorpg.SmmoRPG;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * The people standing in the camp between waves.
 *
 * <p>The camp exists so that going deeper is a decision with a moment attached to it, and
 * that moment is worth more if there is something to do in it. Four figures wait on the
 * floor: the master who starts the next wave, a smith who undoes the damage the last one
 * did, a craftsman who makes a weapon better than it was, and a merchant whose shelf is
 * eight things deep and never the same twice.
 *
 * <p>Nothing here is a villager trade. They are marked entities whose right-click is
 * intercepted, so none of them can be lured, bred, cured, or otherwise turned into
 * something the arena did not intend.
 */
public final class CampNpc {

    /** Marks an entity as camp staff and records whose camp it belongs to. */
    private static final String OWNER_TAG = "smmorpg:camp_owner";
    private static final String ROLE_TAG = "smmorpg:camp_role";

    public enum Role {
        /** Starts the next wave. */
        MASTER("training.smmorpg.master", ChatFormatting.GOLD, 0.0D, 0.0D),
        /** Repairs what you are holding. */
        SMITH("training.smmorpg.smith", ChatFormatting.AQUA, -3.0D, -3.0D),
        /** Improves what you are holding. */
        CRAFTSMAN("training.smmorpg.craftsman", ChatFormatting.LIGHT_PURPLE, 3.0D, -3.0D),
        /** Sells eight things, and rerolls them for a price. */
        MERCHANT("training.smmorpg.merchant", ChatFormatting.GREEN, 0.0D, -5.0D);

        private final String key;
        private final ChatFormatting colour;
        private final double dx;
        private final double dz;

        Role(String key, ChatFormatting colour, double dx, double dz) {
            this.key = key;
            this.colour = colour;
            this.dx = dx;
            this.dz = dz;
        }

        public String key() { return key; }
    }

    private CampNpc() {}

    /** Puts the whole camp on the floor and hands back who is now standing where. */
    public static Map<Role, Mob> spawnCamp(ServerLevel level, Vec3 centre, UUID owner) {
        Map<Role, Mob> staff = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            Mob mob = spawn(level, centre, owner, role);
            if (mob != null) staff.put(role, mob);
        }
        return staff;
    }

    public static Mob spawn(ServerLevel level, Vec3 centre, UUID owner, Role role) {
        Mob npc = EntityType.VILLAGER.create(level);
        if (npc == null) return null;

        npc.moveTo(centre.x + role.dx, centre.y + 1.0D, centre.z + role.dz, 0.0F, 0.0F);
        npc.setNoAi(true);
        npc.setInvulnerable(true);
        npc.setPersistenceRequired();
        npc.setSilent(true);
        npc.setCustomName(Component.translatable(role.key()).withStyle(role.colour));
        npc.setCustomNameVisible(true);
        npc.getPersistentData().putString(OWNER_TAG, owner.toString());
        npc.getPersistentData().putString(ROLE_TAG, role.name());

        if (!level.addFreshEntity(npc)) {
            npc.discard();
            return null;
        }
        return npc;
    }

    public static Role roleOf(Entity entity) {
        String raw = entity.getPersistentData().getString(ROLE_TAG);
        if (raw.isEmpty()) return null;
        try {
            return Role.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean belongsTo(Entity entity, UUID owner) {
        return entity.getPersistentData().getString(OWNER_TAG).equals(owner.toString());
    }

    /**
     * Right-clicking camp staff.
     *
     * <p>The interaction is cancelled whatever happens, even when nothing does: a villager
     * trade screen opening here would be a different, confusing thing standing in exactly
     * the same place.
     */
    @EventBusSubscriber(modid = SmmoRPG.MOD_ID)
    public static final class Interaction {

        @SubscribeEvent
        public static void onInteract(PlayerInteractEvent.EntityInteract event) {
            Role role = roleOf(event.getTarget());
            if (role == null) return;

            event.setCanceled(true);
            if (!(event.getEntity() instanceof ServerPlayer player)) return;

            TrainingSession session = TrainingManager.of(player);
            if (session == null || !belongsTo(event.getTarget(), session.owner())) return;

            switch (role) {
                case MASTER -> session.advance(player);
                case SMITH -> CampServices.repair(player);
                case CRAFTSMAN -> CampServices.improve(player);
                case MERCHANT -> CampShop.open(player, session);
            }
        }
    }
}
