package com.lothrazar.heartbalance;

import com.lothrazar.heartbalance.item.ItemHeart;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
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
    if (healthAttribute == null) {
      return;
    }
    AttributeModifier oldHealthModifier = healthAttribute.getModifier(ID);
    if (oldHealthModifier != null) {
      //delete and replace
      healthAttribute.removeModifier(oldHealthModifier);
    }
    //always apply to player if they do not have
    int h = 2 * ConfigManager.INIT_HEARTS.get();
    AttributeModifier healthModifier = new AttributeModifier(ID, ModMain.MODID, h, AttributeModifier.Operation.ADDITION);
    healthAttribute.addPermanentModifier(healthModifier);
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
      if (!resultStack.isEmpty() && resultStack.getItem() instanceof ItemHeart) {
        ItemHeart heart = (ItemHeart) resultStack.getItem();
        //try to heal one by one
        boolean healed = false;
        while (!resultStack.isEmpty() && player.isHurt()) {
          player.heal(heart.getHealing());
          resultStack.shrink(1);
          itemEntity.setItem(resultStack);
          healed = true;
        }
        if (healed && ConfigManager.DO_SOUND_PICKUP.get()) {
          ModRegistry.playSoundFromServer((ServerPlayerEntity) player, ModRegistry.HEART_GET, 0.3F, 0.95F);
        }
        //all done. so EITHER player is fully healed
        // OR we ran out of items... so do we cancel?
        //dont cancel if healed = true, there might be more remaining
        if (!ConfigManager.DO_PICKUP.get() ||
            itemEntity.getItem().isEmpty()) {
          //if config says no item pickup. always cancel and remove all items (even if not empty)
          //if config allows us through, then remove if empty i guess
          itemEntity.remove(false);
          //cancel to block the pickup 
          event.setCanceled(true);
        }
      }
    }
  }

  @SubscribeEvent
  public void onLivingDeathEvent(LivingDeathEvent event) {
    World world = event.getEntity().level;
    if (world.isClientSide || event.getSource() == null
        || world.random.nextDouble() >= ConfigManager.CHANCE.get()) {
      return;
    }
    //if config is at 10, and you roll in 10-100 you were cancelled,
    //else here we continue so our roll was < 10 so the percentage worked
    Entity trueSource = event.getSource().getEntity();
    if (trueSource instanceof PlayerEntity && !(trueSource instanceof FakePlayer)) {
      //killed by me  
      if (event.getEntityLiving().getType().getCategory() == EntityClassification.MONSTER) {
        //drop
        BlockPos pos = event.getEntity().blockPosition();
        world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
            new ItemStack(ModRegistry.HALF_HEART)));
      }
    }
  }
}
