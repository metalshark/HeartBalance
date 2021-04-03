package com.lothrazar.heartbalance;

import java.util.UUID;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class HeartEvents {

  public static final UUID ID = UUID.fromString("55550aa2-eff2-4a81-b92b-a1cb95f15555");

  private static void forceHearts(PlayerEntity player) {
    ModifiableAttributeInstance healthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
    AttributeModifier oldHealthModifier = healthAttribute.getModifier(ID);
    if (oldHealthModifier == null) {
      //always apply to player if they do not have
      int h = 2 * ConfigManager.INIT_HEARTS.get();
      AttributeModifier healthModifier = new AttributeModifier(ID, ModMain.MODID, h, AttributeModifier.Operation.ADDITION);
      healthAttribute.applyPersistentModifier(healthModifier);
    }
  }

  @SubscribeEvent
  public void onEntityJoinWorld(EntityJoinWorldEvent event) {
    if (event.getEntity() instanceof PlayerEntity) {
      forceHearts((PlayerEntity) event.getEntity());
    }
  }

  @SubscribeEvent
  public void onPlayerCloneDeath(PlayerEvent.Clone event) {
    forceHearts(event.getPlayer());
  }

  @SubscribeEvent
  public void onPlayerPickup(EntityItemPickupEvent event) {
    if (event.getEntityLiving() instanceof PlayerEntity) {
      PlayerEntity player = (PlayerEntity) event.getEntityLiving();
      ItemEntity itemEntity = event.getItem();
      ItemStack resultStack = itemEntity.getItem();
      if (!resultStack.isEmpty() &&
          resultStack.getItem() == ModRegistry.HALF_HEART) {
        // multi stack
        while (!resultStack.isEmpty() && player.shouldHeal()) {
          player.heal(1F);
          resultStack.shrink(1);
          itemEntity.setItem(resultStack);
        }
        //all done. so EITHER player is fully healed
        // OR we ran out of items... so do we cancel?
        if (itemEntity.getItem().isEmpty()) {
          itemEntity.remove();
          event.setCanceled(true);//cancel to block the pickup
          //        event.setResult(Result.DENY);
        }
      }
    }
  }

  @SubscribeEvent
  public void onLivingDeathEvent(LivingDeathEvent event) {
    World world = event.getEntity().world;
    if (world.isRemote || event.getSource() == null
        || world.rand.nextDouble() >= ConfigManager.CHANCE.get()) {
      return;
    }
    //if config is at 10, and you roll in 10-100 you were cancelled,
    //else here we continue so our roll was < 10 so the percentage worked
    if (event.getSource().getTrueSource() instanceof PlayerEntity &&
        !(event.getSource().getTrueSource() instanceof FakePlayer)) {
      //killed by me
      //      PlayerEntity realPlayer = (PlayerEntity) event.getSource().getTrueSource();
      EntityClassification eclass = event.getEntityLiving().getType().getClassification();
      if (eclass == EntityClassification.MONSTER) {
        //drop
        BlockPos pos = event.getEntity().getPosition();
        world.addEntity(new ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
            new ItemStack(ModRegistry.HALF_HEART)));
      }
    }
  }
}