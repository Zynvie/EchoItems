package cat.emir.echoitems

import cat.emir.echoitems.serializers.MultiTypeConfigField
import cat.emir.echoitems.serializers.MultiTypeConfigFieldSerializer
import cat.emir.echolib.config.TypedEchoConfig
import cat.emir.echolib.extensions.toComponent
import cat.emir.echolib.plugin.EchoPlugin
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.data.PaperEnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryEvents
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.set.RegistrySet
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents
import net.minecraft.world.item.enchantment.LevelBasedValue
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemType
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.logging.Logger
import kotlin.jvm.optionals.getOrNull

@Suppress("UnstableApiUsage")
class EchoItemsBootstrap : PluginBootstrap {

    class DummyPlugin(val bootstrap: BootstrapContext) : EchoPlugin {
        override fun getDataPath() = bootstrap.dataDirectory
        override fun getDataFolder(): File = bootstrap.dataDirectory.toFile()
        override fun getLogger(): Logger = Logger.getLogger(bootstrap.configuration.name)
        override fun getSLF4JLogger() = bootstrap.logger
        override fun getResource(filename: String): InputStream? {
            try {
                val url = javaClass.classLoader.getResource(filename) ?: return null

                val connection = url.openConnection()
                connection.useCaches = false
                return connection.getInputStream()
            } catch (ex: IOException) {
                return null
            }
        }
    }

    fun createKey(value: String) = Key.key("echoitems", value)
    fun createEnchantmentKey(id: String) = TypedKey.create(RegistryKey.ENCHANTMENT, createKey(id))

    override fun bootstrap(context: BootstrapContext) {
        val config = TypedEchoConfig(EchoItemsConfig::class, DummyPlugin(context), "config.yml") {
            it.defaultOptions { options ->
                options.serializers { builder ->
                    builder.register(MultiTypeConfigField::class.java, MultiTypeConfigFieldSerializer)
                }
            }
        }
        config.load()

        context.lifecycleManager.registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.ITEM)) { event ->
            val registrar = event.registrar()

            config.typedConfig.tags.forEach { (key, tags) ->
                registrar.setTag(
                    TagKey.create(RegistryKey.ITEM, Key.key("echoitems", key)),
                    tags.map { TypedKey.create(RegistryKey.ITEM, Key.key(it)) }
                )

                context.logger.info("Registered new key $key: $tags")
            }
        }

        fun error(id: String, error: String) = IllegalArgumentException("Failed to register enchantment $id: $error")

        context.lifecycleManager.registerEventHandler(RegistryEvents.ENCHANTMENT.compose()) { event ->
            val registrar = event.registry()

            fun parseItemEntries(entries: List<String>): RegistryKeySet<ItemType> {
                val tagEntries = entries.filter { it.startsWith("#") }

                if (tagEntries.isNotEmpty()) {
                    val tagId = tagEntries.first().removePrefix("#")
                    return event.getOrCreateTag(TagKey.create(RegistryKey.ITEM, Key.key(tagId)))
                }

                val keys = entries.map { TypedKey.create(RegistryKey.ITEM, Key.key(it)) }
                return RegistrySet.keySet(RegistryKey.ITEM, keys)
            }

            fun parseEnchantEntries(entries: List<String>): RegistryKeySet<Enchantment> {
                val tagEntries = entries.filter { it.startsWith("#") }

                if (tagEntries.isNotEmpty()) {
                    val tagId = tagEntries.first().removePrefix("#")
                    return event.getOrCreateTag(TagKey.create(RegistryKey.ENCHANTMENT, Key.key(tagId)))
                }

                val keys = entries.map { TypedKey.create(RegistryKey.ENCHANTMENT, Key.key(it)) }
                return RegistrySet.keySet(RegistryKey.ENCHANTMENT, keys)
            }

            config.typedConfig.enchantments.forEach { (id, enchantment) ->
                if (enchantment.maxLevel !in 1..255) throw error(id, "max_level must be between 1 and 255")
                if (enchantment.weight !in 1..1024) throw error(id, "weight must be between 1 and 1024")
                if (enchantment.anvilCost < 0) throw error(id, "anvil_cost cannot be negative")
                if (enchantment.supportedItems.isEmpty()) throw error(id, "supported_items cannot be empty")

                registrar.register(createEnchantmentKey(id)) { builder ->
                    builder
                        .description(enchantment.name.toComponent())
                        .maxLevel(enchantment.maxLevel)
                        .weight(enchantment.weight)
                        .anvilCost(enchantment.anvilCost)
                        .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(enchantment.minCost.base, enchantment.minCost.perLevel))
                        .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(enchantment.maxCost.base, enchantment.maxCost.perLevel))
                        .supportedItems(parseItemEntries(enchantment.supportedItems))
                        .activeSlots(enchantment.activeSlots.map { EquipmentSlotGroup.getByName(it) ?: throw error(id, "$it is not a valid equipment slot group") })

                    if (enchantment.primaryItems.isNotEmpty())
                        builder.primaryItems(parseItemEntries(enchantment.primaryItems))

                    if (enchantment.incompatible.isNotEmpty())
                        builder.exclusiveWith(parseEnchantEntries(enchantment.incompatible))

                    val effectsField = PaperEnchantmentRegistryEntry::class.java.getDeclaredField("effects")
                    effectsField.isAccessible = true
                    val existingEffects = effectsField.get(builder) as DataComponentMap

                    fun parseIdentifierWithDefaultNamespace(key: String): Identifier {
                        return if (key.contains(":")) Identifier.parse(key)
                        else Identifier.withDefaultNamespace(key)
                    }

                    val newEffects = DataComponentMap.builder()
                        .addAll(existingEffects)
                        .set(EnchantmentEffectComponents.ATTRIBUTES,
                            enchantment.effects.attributes.map { attribute ->
                                EnchantmentAttributeEffect(
                                    parseIdentifierWithDefaultNamespace(attribute.key),
                                    BuiltInRegistries.ATTRIBUTE.get(ResourceKey.create(
                                        Registries.ATTRIBUTE, parseIdentifierWithDefaultNamespace(attribute.attribute)
                                    )).getOrNull() ?: throw error(id, "Unknown attribute for effect: ${attribute.key}"),
                                    LevelBasedValue.perLevel(attribute.amount.base, attribute.amount.addPerLevel),
                                    AttributeModifier.Operation.valueOf(attribute.operation.uppercase())
                                )
                            }
                        )
                        .build()

                    effectsField.set(builder, newEffects)
                }

                context.logger.info("Registered new enchantment $id.")
            }
        }

        context.lifecycleManager.registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT)) { event ->
            val registrar = event.registrar()
            val curses = mutableListOf<TypedKey<Enchantment>>()
            val enchantables = mutableListOf<TypedKey<Enchantment>>()
            val tradeables = mutableListOf<TypedKey<Enchantment>>()
            val treasures = mutableListOf<TypedKey<Enchantment>>()
            val randomLootables = mutableListOf<TypedKey<Enchantment>>()
            val tradedEquipmentables = mutableListOf<TypedKey<Enchantment>>()
            val mobEquipmentables = mutableListOf<TypedKey<Enchantment>>()


            config.typedConfig.enchantments.forEach { (id, enchantment) ->
                val tag = createEnchantmentKey(id)
                if (enchantment.properties.curse) curses.add(tag)
                if (enchantment.properties.enchantable) enchantables.add(tag)
                if (enchantment.properties.tradeable) tradeables.add(tag)
                if (enchantment.properties.treasure) treasures.add(tag)
                if (enchantment.properties.onRandomLoot) randomLootables.add(tag)
                if (enchantment.properties.onTradedEquipment) tradedEquipmentables.add(tag)
                if (enchantment.properties.onMobEquipment) mobEquipmentables.add(tag)
            }
            if (curses.isNotEmpty())
                registrar.addToTag(EnchantmentTagKeys.CURSE, curses)
            if (enchantables.isNotEmpty())
                registrar.addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, enchantables)
            if (tradeables.isNotEmpty())
                registrar.addToTag(EnchantmentTagKeys.TRADEABLE, tradeables)
            if (treasures.isNotEmpty())
                registrar.addToTag(EnchantmentTagKeys.TREASURE, treasures)
            if (randomLootables.isNotEmpty())
                registrar.addToTag(EnchantmentTagKeys.ON_RANDOM_LOOT, randomLootables)
            if (tradedEquipmentables.isNotEmpty())
                registrar.addToTag(EnchantmentTagKeys.ON_TRADED_EQUIPMENT, tradedEquipmentables)
            if (mobEquipmentables.isNotEmpty())
                registrar.addToTag(EnchantmentTagKeys.ON_MOB_SPAWN_EQUIPMENT, mobEquipmentables)
        }
    }
}