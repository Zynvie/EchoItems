package cat.emir.echoitems.listeners

import cat.emir.echoitems.EchoItems
import cat.emir.echoitems.items.PacketListener
import cat.emir.echolib.event.EchoListener
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener(val plugin: EchoItems) : EchoListener(plugin) {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val craftPlayer = event.player as CraftPlayer

        craftPlayer.handle.connection.connection.channel.pipeline().addBefore(
            "packet_handler",
            "${craftPlayer.uniqueId}_lore_handler",
            PacketListener(plugin, craftPlayer)
        )
    }
}