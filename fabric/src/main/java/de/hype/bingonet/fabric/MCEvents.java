package de.hype.bingonet.fabric;

import de.hype.bingonet.client.common.chat.Chat;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.client.common.client.updatelisteners.UpdateListenerManager;
import de.hype.bingonet.client.common.config.constants.ClickableArmorStand;
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore;
import de.hype.bingonet.fabric.mixins.mixinaccessinterfaces.IChestBlockEntityMixinAccess;
import de.hype.bingonet.shared.constants.Islands;
import de.hype.bingonet.shared.objects.Position;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public class MCEvents implements de.hype.bingonet.client.common.mclibraries.MCEvents {
    public Utils utils = (Utils) EnvironmentCore.utils;

    public void registerOverlays() {
        utils = (Utils) EnvironmentCore.utils;
        HudRenderCallback.EVENT.register((matrixStack, delta) -> utils.renderOverlays(matrixStack, delta));
    }

    @Override
    public void registerUseClick() {
        UseBlockCallback.EVENT.register(this::onBlockClickInteraction);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            BingoNet.executionService.execute(() -> {
                        onArmorstandInteraction(player, world, hand, entity, hitResult, true);
                    });
                    return ActionResult.PASS;
                }
        );
        AttackEntityCallback.EVENT.register(((player, world, hand, entity, hitResult) -> {
            BingoNet.executionService.execute(() -> {
                onArmorstandInteraction(player, world, hand, entity, hitResult, false);
            });
            return ActionResult.PASS;
        }));
    }

    private ActionResult onBlockClickInteraction(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (hitResult.getBlockPos() == null) return ActionResult.PASS;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = hitResult.getBlockPos();
            if (world.getBlockState(blockPos).getBlock() instanceof ChestBlock) {
                try {
                    if (utils.getCurrentIsland().equals(Islands.CRYSTAL_HOLLOWS)) {
                        UpdateListenerManager.chChestUpdateListener.addOpenedChest(new Position(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                        IChestBlockEntityMixinAccess access = (IChestBlockEntityMixinAccess) world.getBlockEntity(blockPos);
                        //is it open already? if so ignore cause not new
                        boolean alreadyOpened = access.BingoNet$isOpen();
                        //schedule to check after processing to allow detecting whether chest is now opened → just opened
//                        BingoNet.executionService.schedule(() -> {
//                            if (!(world.getBlockEntity(blockPos) instanceof ChestBlockEntity)) return;
//                            if (!access.BingoNet$isOpen() || alreadyOpened) return;
//                            BingoNet.executionService.schedule(() -> {
//                                //Check whether it still exists → filter for the powder chest with unfortunate timing
//                                if (!(world.getBlockEntity(blockPos) instanceof ChestBlockEntity)) return;
//                                Set<ChChestItem> items = BingoNet.temporaryConfig.chestParts;
//                                items.clear();
//                                BingoNet.executionService.schedule(() -> {
////                                    Chat.sendPrivateMessageToSelfDebug("Global Chest Detected");
//                                    BingoNet.executionService.schedule(() -> {
//                                        if (BingoNet.temporaryConfig.chestParts.isEmpty()) {
////                                            Chat.sendPrivateMessageToSelfDebug("Global Chest " + blockPos.toShortString() + ":" + " Nothing of value");
//                                            return;
//                                        }
//                                        Position pos = new Position(blockPos.getX(), blockPos.getY(), blockPos.getZ());
//                                        Chat.sendPrivateMessageToSelfText(Message.tellraw("{\"text\":\"Global Chest Found ($coords): $items (Click here to announce)\",\"color\":\"gold\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/chchest \\\"$items\\\" $coords \\\"/msg $username bb:party me\\\" Ⓐ\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[{\"text\":\"You remain responsible. The detection is not flawless and can be wrong\",\"color\":\"dark_red\"}]}}".replace("$coords", pos.toString()).replace("$username", BingoNet.generalConfig.getUsername()).replace("$items", String.join("2", items.stream().map(ChChestItem::getDisplayName).collect(Collectors.joining(";"))))));
//                                    }, 2, TimeUnit.SECONDS);
//                                }, 2, TimeUnit.SECONDS);
//                            }, 1, TimeUnit.SECONDS);
//                        }, 1, TimeUnit.SECONDS);
                        BingoNet.executionService.schedule(() -> {
                            if (!(world.getBlockEntity(blockPos) instanceof ChestBlockEntity)) return;
                            if (!access.BingoNet$isOpen() || alreadyOpened) return;
                            BingoNet.executionService.schedule(() -> {
                                //Check whether it still exists → filter for the powder chest with unfortunate timing
                                if (!(world.getBlockEntity(blockPos) instanceof ChestBlockEntity)) return;
                                BingoNet.executionService.schedule(() -> {
                                    if (BingoNet.developerConfig.devMode) {
                                        Chat.sendPrivateMessageToSelfDebug("Set the Global Chest Blockpos too %s".formatted(blockPos.toString()));
                                    }
                                    BingoNet.temporaryConfig.lastGlobalChchestCoords = new Position(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                                }, 2, TimeUnit.SECONDS);
                            }, 1, TimeUnit.SECONDS);
                        }, 1, TimeUnit.SECONDS);

                    }
                } catch (Exception e) {
                    return ActionResult.PASS;
                }
            }
        }
        return ActionResult.PASS;
    }

    /**
     * @param player
     * @param world
     * @param hand
     * @param entity
     * @param hitResult
     * @param use       whether it is a punch or an use on the entity. if true it is an right click (use)
     */
    public void onArmorstandInteraction(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult, boolean use) {
        ModInitialiser.tutorialManager.clickedEntity(entity, use);
        if (entity instanceof ArmorStandEntity) {
            var item = ((ArmorStandEntity) entity).getEquippedStack(EquipmentSlot.HEAD);
            if (item.getItem() == Items.PLAYER_HEAD) {
                ProfileComponent profileComponent = item.get(DataComponentTypes.PROFILE);
                if (profileComponent == null) return;
                String texture = profileComponent.properties().get("textures").stream().toList().getFirst().value();
                ClickableArmorStand armorStand = ClickableArmorStand.getFromTexture(texture);
//                    if (armorStand != null) Chat.sendPrivateMessageToSelfSuccess(armorStand.toString()+" was clicked");
                //TODO Maybe used for fairysouls here soon
            }
        }
        ;
    }
}
