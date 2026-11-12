package cat.emir.echoitems.text

import cat.emir.echoitems.EchoItems
import cat.emir.echoitems.EchoItemsConfig
import de.tr7zw.changeme.nbtapi.NBT
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack

/**
 *   ${modifier, string,  key='value with # in it', key2='...', else='...'}
 *   ${modifier, long,    1='modifier is #', 2='...', else='# is modifier'}
 *   ${modifier, double,  1.5='...', else='...'}
 *   ${modifier, boolean, true='...', false='...', else='...'}
 */
class TextParser(val plugin: EchoItems) {
    fun parseText(item: EchoItemsConfig.Item, text: String, player: OfflinePlayer? = null): String {
        return MessageFormat.format(text) {
            if (it.startsWith("%") && it.endsWith("%")) {
                return@format if (plugin.isPapiEnabled) PlaceholderAPI.setPlaceholders(player, it) else it
            }
            return@format item.modifiers[it]?.raw() ?: item.nbt.find { nbt -> nbt.key == it }?.value?.raw()
        }
    }

    fun parseText(item: ItemStack, text: String, player: OfflinePlayer? = null): String {
        return MessageFormat.format(text) {
            if (it.startsWith("%") && it.endsWith("%")) {
                return@format if (plugin.isPapiEnabled) PlaceholderAPI.setPlaceholders(player, it) else it
            }
            return@format NBT.get<Any>(item) { nbt ->
                nbt.getCompound("modifiers")?.getOrNull<Any>(it, Any::class.java)
                    ?: nbt.getOrNull(it, Any::class.java)
            }
        }
    }
}