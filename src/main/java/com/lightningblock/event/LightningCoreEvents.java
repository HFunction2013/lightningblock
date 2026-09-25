package com.lightningblock.event;

import com.lightningblock.LightningBlockMod;
import com.lightningblock.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = LightningBlockMod.MOD_ID)
public class LightningCoreEvents {

    private static final int STRIKE_INTERVAL_TICKS = 40; // strike every 2 seconds
    private static final double STRIKE_RADIUS = 10.0;

    /**
     * While holding a Lightning Core, the player is immune to fire, lava,
     * explosion, and lightning damage.
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isHoldingCore(player)) return;

        DamageSource source = event.getSource();
        String typeId = source.getMsgId();

        boolean isFire = "inFire".equals(typeId)
                || "lava".equals(typeId)
                || "hotFloor".equals(typeId)
                || "onFire".equals(typeId);
        boolean isExplosion = "explosion".equals(typeId)
                || "playerExplosion".equals(typeId)
                || "badRespawnPoint".equals(typeId);
        boolean isLightning = "lightningBolt".equals(typeId);

        if (isFire || isExplosion || isLightning) {
            event.setCanceled(true);
        }
    }

    /**
     * While holding a Lightning Core, hostile mobs within 10 blocks are
     * periodically struck by lightning.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        Level level = player.level();
        if (level.isClientSide()) return;
        if (!isHoldingCore(player)) return;
        if (player.tickCount % STRIKE_INTERVAL_TICKS != 0) return;

        AABB area = AABB.ofSize(player.position(),
                STRIKE_RADIUS * 2, STRIKE_RADIUS * 2, STRIKE_RADIUS * 2);

        List<Mob> hosts = level.getEntitiesOfClass(Mob.class, area,
                m -> m.isAlive() && m.distanceTo(player) <= STRIKE_RADIUS);

        for (Mob host : hosts) {
            LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
            bolt.moveTo(host.getX(), host.getY(), host.getZ());
            ((ServerLevel) level).addFreshEntity(bolt);
        }
    }

    private static boolean isHoldingCore(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(ModItems.LIGHTNING_CORE.get())
                || off.is(ModItems.LIGHTNING_CORE.get());
    }
}
