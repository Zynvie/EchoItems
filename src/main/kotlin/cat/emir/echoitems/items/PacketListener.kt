package cat.emir.echoitems.items

import cat.emir.echoitems.EchoItems
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.papermc.paper.adventure.PaperAdventure
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket
//import net.minecraft.network.protocol.game.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import kotlin.jvm.optionals.getOrNull

class PacketListener(val plugin: EchoItems, val player: CraftPlayer) : ChannelDuplexHandler() {

    override fun channelRead(ctx: ChannelHandlerContext?, packet: Any?) {
//        if (packet !is ServerboundClientTickEndPacket &&
//            packet !is ServerboundMovePlayerPacket.Pos &&
//            packet !is ServerboundMovePlayerPacket.Rot &&
//            packet !is ServerboundMovePlayerPacket.PosRot &&
//            packet !is ServerboundKeepAlivePacket &&
//            packet !is ServerboundPlayerInputPacket
//        )
//            player.sendRichMessage("serverbound packet: ${packet?.javaClass?.simpleName}")

        when (packet) {
            is ServerboundSetCreativeModeSlotPacket ->
                if (!packet.itemStack.isEmpty && getItemId(packet.itemStack) != null) {
                    packet.itemStack.set(DataComponents.LORE, ItemLore.EMPTY)
                }
            is ServerboundSetCarriedItemPacket -> {
                val item = player.inventory.getItem(packet.slot)
                val nmsItem = (item as CraftItemStack?)?.handle
                if (item != null && nmsItem != null && !item.isEmpty && getItemId(nmsItem) != null) {
                    modifyNmsItem(nmsItem)

                    player.handle.connection.send(
                        ClientboundContainerSetSlotPacket(
                            player.handle.inventoryMenu.containerId,
                            player.handle.inventoryMenu.stateId,
                            36 + packet.slot,
                            nmsItem
                        )
                    )
                }
            }
        }

        super.channelRead(ctx, packet)
    }

    override fun write(ctx: ChannelHandlerContext, packet: Any?, promise: ChannelPromise) {
//        if (packet !is ClientboundSystemChatPacket &&
//            packet !is ClientboundSetTimePacket &&
//            packet !is ClientboundKeepAlivePacket &&
//            packet !is ClientboundLevelChunkWithLightPacket &&
//            packet !is ClientboundForgetLevelChunkPacket &&
//            packet !is ClientboundRotateHeadPacket &&
//            packet !is ClientboundMoveEntityPacket.Rot &&
//            packet !is ClientboundMoveEntityPacket.Pos &&
//            packet !is ClientboundMoveEntityPacket.PosRot &&
//            packet !is ClientboundSetEntityMotionPacket &&
//            packet !is ClientboundSetEntityDataPacket &&
//            packet !is ClientboundEntityPositionSyncPacket &&
//            packet !is ClientboundEntityEventPacket &&
//            packet !is ClientboundBundlePacket &&
//            packet !is ClientboundUpdateAttributesPacket &&
//            packet !is ClientboundSoundPacket &&
//            packet !is ClientboundRemoveEntitiesPacket)
//            player.sendRichMessage("clientbound packet: ${packet?.javaClass?.simpleName}")
        when (packet) {
            is ClientboundContainerSetSlotPacket -> modifyNmsItem(packet.item)
            is ClientboundContainerSetContentPacket -> packet.items.forEach { modifyNmsItem(it) }
            is ClientboundSetCursorItemPacket ->
                if (!packet.contents.isEmpty && getItemId(packet.contents) != null) return
        }

        super.write(ctx, packet, promise)
    }

    private fun getItemId(nmsItem: ItemStack): String? {
        val customData = nmsItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
        val tag = customData.copyTag()
        return tag.getString("id").getOrNull()
    }

    private fun modifyNmsItem(nmsItem: ItemStack) {
        if (nmsItem.isEmpty) return
        val id = getItemId(nmsItem) ?: return

        val item = plugin.itemManager.getItem(id) ?: return

        val name = item.buildName(player)?.let { PaperAdventure.asVanilla(it) }
        val lore = item.buildLore(player)?.let { lore ->
            lore.map { PaperAdventure.asVanilla(it) }
        }

        if (name != null) nmsItem.set(DataComponents.CUSTOM_NAME, name)
        if (!lore.isNullOrEmpty()) {
            nmsItem.set(DataComponents.LORE, ItemLore(lore))
        }
    }
}