package cat.emir.echoitems.commands

import cat.emir.echoitems.EchoItems
import cat.emir.echoitems.EchoItemsConfig
import cat.emir.echoitems.arguments.ItemArgument
import cat.emir.echoitems.items.buildItem
import cat.emir.echolib.command.EchoCommand
import cat.emir.echolib.command.getPlayer
import cat.emir.echolib.extensions.toComponent
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys
import net.kyori.adventure.key.Key
import org.bukkit.craftbukkit.enchantments.CraftEnchantment
import org.bukkit.enchantments.Enchantment

class MainCommand(plugin: EchoItems) : EchoCommand<EchoItems>(plugin) {

    companion object {
        var wasEnchantmentReloaded = false
        var wasTagsReloaded = false
    }

    override val aliases = setOf("items")

    override fun getCommand(): LiteralArgumentBuilder<CommandSourceStack> {
        return command("echoitems") {
            requires { it.sender.hasPermission("echoitems.admin") }
            subcommand("get") {
                argument("item", ItemArgument()) {
                    executes(::get)
                }
            }
            subcommand("reload") {
                executes(::reload)
            }
            subcommand("debug") {
                executes(::debug)
            }
        }
    }

    fun get(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.getPlayer() ?: return 1
        val item = ctx.getArgument("item", EchoItemsConfig.Item::class.java)

        player.inventory.addItem(item.buildItem())

        return 1
    }

    fun reload(ctx: CommandContext<CommandSourceStack>): Int {
        val enchantments = plugin.config.typedConfig.enchantments
        val tags = plugin.config.typedConfig.tags

        val time = plugin.reload()

        fun error(type: String, wasReloaded: Boolean) = "<red>There were $type modified in config${if (wasReloaded) " previously" else ""}, they require a full restart to be updated.".toComponent()

        if (enchantments != plugin.config.typedConfig.enchantments || wasEnchantmentReloaded) {
            ctx.source.sender.sendMessage(error("enchantments", wasEnchantmentReloaded))
            wasEnchantmentReloaded = true
        } else if (tags != plugin.config.typedConfig.tags || wasTagsReloaded) {
            ctx.source.sender.sendMessage(error("tags", wasTagsReloaded))
            wasTagsReloaded = true
        }

        ctx.source.sender.sendMessage("<aqua>Reloaded in ${time}ms.".toComponent())
        return 1
    }

    fun debug(ctx: CommandContext<CommandSourceStack>): Int {
        val myKey = TypedKey.create(RegistryKey.ENCHANTMENT, Key.key("echoitems:test"))
        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)

        val enchantData = registry.get(myKey)
        println("[Debug] Is Registered: ${enchantData != null}")

        if (enchantData == null) return 1

        println("[Debug] Is in CURSE tag: ${registry.getTag(EnchantmentTagKeys.CURSE).contains(myKey)}")
        println("[Debug] Is in IN_ENCHANTING_TABLE tag: ${registry.getTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE).contains(myKey)}")
        println("[Debug] Is in NON_TREASURE tag: ${registry.getTag(EnchantmentTagKeys.NON_TREASURE).contains(myKey)}")
        println("[Debug] Is in TRADEABLE tag: ${registry.getTag(EnchantmentTagKeys.TRADEABLE).contains(myKey)}")
        println("[Debug] Is in TREASURE tag: ${registry.getTag(EnchantmentTagKeys.TREASURE).contains(myKey)}")
        println("[Debug] Is in ON_RANDOM_LOOT tag: ${registry.getTag(EnchantmentTagKeys.ON_RANDOM_LOOT).contains(myKey)}")
        println("[Debug] Is in ON_TRADED_EQUIPMENT tag: ${registry.getTag(EnchantmentTagKeys.ON_TRADED_EQUIPMENT).contains(myKey)}")
        println("[Debug] Is in ON_MOB_SPAWN_EQUIPMENT tag: ${registry.getTag(EnchantmentTagKeys.ON_MOB_SPAWN_EQUIPMENT).contains(myKey)}")

        println("[Debug] Supported Items: ${enchantData.supportedItems}")
        println("[Debug] Primary Items: ${enchantData.primaryItems}")
        println("[Debug] Weight: ${enchantData.weight}")
        println("[Debug] Effects: ${(enchantData as CraftEnchantment).handle.effects()}")

        return 1
    }
}