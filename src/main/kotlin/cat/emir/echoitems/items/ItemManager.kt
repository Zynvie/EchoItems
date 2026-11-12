package cat.emir.echoitems.items

import cat.emir.echoitems.EchoItems
import cat.emir.echoitems.EchoItemsConfig
import cat.emir.echoitems.serializers.MultiTypeConfigField
import cat.emir.echolib.extensions.toComponent
import de.tr7zw.changeme.nbtapi.NBT
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.Equippable
import io.papermc.paper.datacomponent.item.FoodProperties
import io.papermc.paper.datacomponent.item.ItemArmorTrim
import io.papermc.paper.datacomponent.item.Tool
import io.papermc.paper.datacomponent.item.UseCooldown
import io.papermc.paper.datacomponent.item.UseRemainder
import io.papermc.paper.datacomponent.item.Weapon
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.set.RegistrySet
import io.papermc.paper.registry.tag.TagKey
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.util.TriState
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.legacy.MaterialRerouting.setIngredient
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.SmithingRecipe
import org.bukkit.inventory.SmithingTransformRecipe
import org.bukkit.inventory.meta.trim.ArmorTrim
import kotlin.enums.enumEntries

fun EchoItemsConfig.Item.buildItem() =
    EchoItems.instance.itemManager.buildItem(this)

fun EchoItemsConfig.Item.buildLore(player: OfflinePlayer? = null) =
    EchoItems.instance.itemManager.buildLore(this, player)

fun EchoItemsConfig.Item.buildName(player: OfflinePlayer? = null) =
    EchoItems.instance.itemManager.buildName(this, player)

@Suppress("UnstableApiUsage")
class ItemManager(val plugin: EchoItems) {
    val recipes = mutableMapOf<String, MutableList<NamespacedKey>>()

    fun getItem(id: String): EchoItemsConfig.Item? {
        return plugin.config.typedConfig.items[id]
    }

    private fun parseToComponent(item: EchoItemsConfig.Item, text: String, player: OfflinePlayer? = null): Component {
        return cleanItalic(parseText(item, text, player).toComponent())
    }

    private fun parseToComponent(item: ItemStack, text: String, player: OfflinePlayer? = null): Component {
        return cleanItalic(parseText(item, text, player).toComponent())
    }

    private fun cleanItalic(component: Component) : Component {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
    }

    private fun parseText(item: EchoItemsConfig.Item, text: String, player: OfflinePlayer? = null): String {
        return parsePapi(plugin.textParser.parseText(item, text, player), player)
    }

    private fun parseText(item: ItemStack, text: String, player: OfflinePlayer? = null): String {
        return parsePapi(plugin.textParser.parseText(item, text, player), player)
    }

    private fun parsePapi(text: String, player: OfflinePlayer? = null): String {
        return if (plugin.isPapiEnabled) PlaceholderAPI.setPlaceholders(player, text) else text
    }

    fun buildItem(id: String): ItemStack? {
        return getItem(id)?.buildItem()
    }

    fun buildLore(item: EchoItemsConfig.Item, player: OfflinePlayer? = null): List<Component>? {
        return item.lore?.let { it.map { line -> parseToComponent(item, line, player) } }
    }

    fun buildName(item: EchoItemsConfig.Item, player: OfflinePlayer? = null): Component? {
        return (item.customName ?: item.name)?.let { parseToComponent(item, it, player) }
    }

    private fun parseBlockEntries(entries: List<String>): RegistryKeySet<BlockType> {
        val tagEntries = entries.filter { it.startsWith("#") }

        if (tagEntries.isNotEmpty()) {
            val tagId = tagEntries.first().removePrefix("#")
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK).getTag(
                TagKey.create(RegistryKey.BLOCK, Key.key(tagId)
            ))
        }

        val keys = entries.map { TypedKey.create(RegistryKey.BLOCK, Key.key(it)) }
        return RegistrySet.keySet(RegistryKey.BLOCK, keys)
    }

    fun registerAllRecipes() {
        plugin.config.typedConfig.items.forEach { (id, item) ->
            registerRecipe(item)
        }
    }

    fun registerRecipe(item: EchoItemsConfig.Item) {
        fun error(id: String, type: String, error: String) = IllegalArgumentException("Failed to register $type recipe for item $id: $error")
        fun item(material: String, index: Int, key: String? = null, default: ItemStack? = null): ItemStack {
            if (material == "self") return item.buildItem()
            return buildItem(material)
                ?: enumEntries<Material>().find { it.name == material.uppercase() }?.let { ItemStack(it) }
                ?: default
                ?: throw error(item.id, "crafting", "invalid material for ingredient${if (key != null) " $key" else ""} at index $index: $material")
        }
        fun <T> addRecipe(recipe: T) where T: Recipe, T: Keyed {
            plugin.server.addRecipe(recipe)
            recipes.getOrPut(item.id) { mutableListOf() }
                .add(recipe.key)
        }
        fun createKey(size: Int, type: String, index: Int): NamespacedKey {
            return if (size == 1) NamespacedKey.fromString("echoitems:${item.id}_$type")!!
                else NamespacedKey.fromString("echoitems:${item.id}_${type}_$index")!!
        }

        recipes[item.id]?.forEach { plugin.server.removeRecipe(it) }

        val recipe = item.recipe ?: return

        recipe.crafting?.withIndex()?.forEach { (index, crafting) ->
            val key = createKey(recipe.crafting.size, "crafting", index)

            addRecipe(
                if (crafting.shape != null)
                    ShapedRecipe(key, item.buildItem().asQuantity(crafting.amount))
                        .shape(*crafting.shape.toTypedArray())
                        .apply {
                            crafting.ingredients.forEach { (key, value) ->
                                setIngredient(key.first(), item(value, index, key))
                            }
                        }
                else ShapelessRecipe(key, item.buildItem().asQuantity(crafting.amount)).apply {
                    crafting.ingredients.forEach { (_, value) ->
                        addIngredient(item(value, index))
                    }
                }
            )

            plugin.logger.info("Registered crafting recipe $key")
        }

        fun buildRecipeChoice(input: String?, rtype: String, type: String, index: Int): RecipeChoice {
            if (input == null) return RecipeChoice.empty()
            if (input == "self") return RecipeChoice.ExactChoice(item.buildItem())
            return buildItem(input)?.let { RecipeChoice.ExactChoice(it) }
                ?: enumEntries<Material>().find { it.name == input.uppercase() }?.let { RecipeChoice.MaterialChoice(it) }
                ?: throw error(item.id, rtype, "invalid material for $rtype $type at index $index: $input")
        }

        recipe.smithing?.withIndex()?.forEach { (index, smithing) ->
            val key = createKey(recipe.smithing.size, "smithing", index)

            addRecipe(
                SmithingTransformRecipe(
                    key,
                    smithing.result?.let {
                        item(it, -1, "smithing result", item.buildItem())
                    } ?: item.buildItem(),
                    buildRecipeChoice(smithing.template, "smithing", "template", index),
                    buildRecipeChoice(smithing.base, "smithing", "base", index),
                    buildRecipeChoice(smithing.addition, "smithing", "addition", index),
                    smithing.copyData ?: true
                )
            )

            plugin.logger.info("Registered smithing recipe $key")
        }

        recipe.furnace?.withIndex()?.forEach { (index, furnace) ->
            val key = createKey(recipe.furnace.size, "furnace", index)

            addRecipe(
                FurnaceRecipe(
                    key,
                    furnace.result?.let {
                        item(it, -1, "furnace result", item.buildItem())
                    } ?: item.buildItem(),
                    buildRecipeChoice(furnace.input, "furnace", "input", index),
                    furnace.experience,
                    furnace.time
                )
            )
        }

        plugin.server.updateRecipes()
    }

    @Suppress("UnstableApiUsage")
    fun buildItem(item: EchoItemsConfig.Item): ItemStack {
        val registry = RegistryAccess.registryAccess()
        val enchantmentRegistry = registry.getRegistry(RegistryKey.ENCHANTMENT)
        val attributeRegistry = registry.getRegistry(RegistryKey.ATTRIBUTE)
        val trimMaterialRegistry = registry.getRegistry(RegistryKey.TRIM_MATERIAL)
        val trimPatternRegistry = registry.getRegistry(RegistryKey.TRIM_PATTERN)

        return ItemStack(item.material).apply {
            editMeta { meta ->
//                (item.customName ?: item.name)?.let { meta.customName(parseToComponent(item, it)) }
                item.displayName?.let { meta.displayName(parseToComponent(item, it)) }
                item.itemName?.let { meta.itemName(parseToComponent(item, it)) }
//                item.buildLore()?.let { meta.lore(it) }
                item.enchantments.forEach { (key, level) ->
                    enchantmentRegistry.get(Key.key(key))?.let {
                        meta.addEnchant(it, level, true)
                    }
                }
                item.flags.forEach { flag ->
                    enumEntries<ItemFlag>().find { it.name == flag.uppercase() }?.let {
                        meta.addItemFlags(it)
                    }
                }
                item.attributes.forEach { attribute ->
                    attributeRegistry.get(Key.key(attribute.attribute))?.let {
                        meta.addAttributeModifier(
                            it,
                            AttributeModifier(
                                NamespacedKey.fromString(attribute.key)!!,
                                attribute.amount,
                                AttributeModifier.Operation.valueOf(attribute.operation.uppercase()),
                                EquipmentSlotGroup.getByName(attribute.slot.uppercase()) ?: EquipmentSlotGroup.ANY
                            )
                        )
                    }
                }
                item.model?.let { meta.itemModel = NamespacedKey.fromString(it) }
                meta.setEnchantmentGlintOverride(item.glintOverride)
                meta.setMaxStackSize(item.maxStackSize)
                meta.setRarity(enumEntries<ItemRarity>().find { it.name == item.rarity?.uppercase() })
                item.tooltip?.let { meta.tooltipStyle = NamespacedKey.fromString(it) }
                item.glider?.let { meta.isGlider = it }
                item.damage?.unbreakable?.let { meta.isUnbreakable = it }
            }

            item.equippable?.let { setData(
                DataComponentTypes.EQUIPPABLE,
                Equippable.equippable(EquipmentSlot.valueOf(it.slot.uppercase()))
                    .assetId(Key.key(it.model))
                    .apply { it.sound?.let { sound -> equipSound(Key.key(sound)) } }
                    .canBeSheared(it.shearable)
                    .apply { it.shearSound?.let { sound -> shearSound(Key.key(sound)) } }
                    .damageOnHurt(it.damageOnHurt)
                    .dispensable(it.dispensable)
                    .swappable(it.swappable)
            ) }
            item.food?.let { setData(
                DataComponentTypes.FOOD,
                FoodProperties.food()
                    .canAlwaysEat(it.canAlwaysEat)
                    .nutrition(it.nutrition)
                    .saturation(it.saturation)
            ) }
            item.consumable?.let { setData(
                DataComponentTypes.CONSUMABLE,
                Consumable.consumable()
                    .consumeSeconds(it.seconds)
                    .hasConsumeParticles(it.hasParticles)
                    .sound(Key.key(it.sound))
                    .animation(ItemUseAnimation.valueOf(it.animation.uppercase()))
            ) }
            item.tool?.let { setData(
                DataComponentTypes.TOOL,
                Tool.tool().apply {
                    it.damagePerBlock?.let { damage -> damagePerBlock(damage) }
                    it.defaultSpeed?.let { speed -> defaultMiningSpeed(speed) }
                    it.canDestroyInCreative?.let { can -> canDestroyBlocksInCreative(can) }
                    it.rules.forEach { rule -> addRule(
                        Tool.rule(
                            parseBlockEntries(rule.blocks),
                            rule.speed,
                            TriState.byBoolean(rule.correctForDrops)
                        )
                    ) }
                }
            ) }
            item.weapon?.let { setData(
                DataComponentTypes.WEAPON,
                Weapon.weapon().apply {
                    it.itemDamage?.let { itemDamage -> itemDamagePerAttack(itemDamage) }
                    it.disableBlockingSeconds?.let { seconds -> disableBlockingForSeconds(seconds) }
                }
            ) }
            item.use?.let {
                setData(
                    DataComponentTypes.USE_COOLDOWN,
                    UseCooldown.useCooldown(it.cooldown.seconds)
                        .cooldownGroup(it.cooldown.group?.let { group -> Key.key(group) })
                )
                it.remainder?.let { remainder ->
                    buildItem(remainder)?.let { remainderItem -> setData(
                        DataComponentTypes.USE_REMAINDER,
                        UseRemainder.useRemainder(remainderItem)
                    ) } ?: ItemStack(Material.valueOf(remainder.uppercase()))
                }
            }
            item.damage?.let {
                it.current?.let { current -> setData(DataComponentTypes.DAMAGE, current) }
                it.max?.let { max -> setData(DataComponentTypes.MAX_DAMAGE, max) }
            }
            item.trim?.let {
                val material = trimMaterialRegistry.get(Key.key(it.material)) ?: return@let
                val pattern = trimPatternRegistry.get(Key.key(it.pattern)) ?: return@let
                setData(
                    DataComponentTypes.TRIM,
                    ItemArmorTrim.itemArmorTrim(ArmorTrim(material, pattern))
                )
            }
            if (item.modifiers.isNotEmpty()) NBT.modify(this) {
                val modifiersNbt = it.getOrCreateCompound("modifiers")
                item.modifiers.forEach { (key, value) ->
                    when (value) {
                        is MultiTypeConfigField.StringValue -> modifiersNbt.setString(key, value.value)
                        is MultiTypeConfigField.LongValue -> modifiersNbt.setLong(key, value.value)
                        is MultiTypeConfigField.DoubleValue -> modifiersNbt.setDouble(key, value.value)
                        is MultiTypeConfigField.BooleanValue -> modifiersNbt.setBoolean(key, value.value)
                    }
                }
            }
            if (item.nbt.isNotEmpty()) NBT.modify(this) {
                item.nbt.forEach { (key, value) ->
                    when (value) {
                        is MultiTypeConfigField.StringValue -> it.setString(key, value.value)
                        is MultiTypeConfigField.LongValue -> it.setLong(key, value.value)
                        is MultiTypeConfigField.DoubleValue -> it.setDouble(key, value.value)
                        is MultiTypeConfigField.BooleanValue -> it.setBoolean(key, value.value)
                    }
                }
            }

            NBT.modify(this) {
                it.setString("id", item.id)
                if (item.hasCreationTime) it.setLong("created_at", System.currentTimeMillis())
            }
        }
    }

    fun updateItem(item: ItemStack) {
        val id = NBT.get<String>(item) { it.getString("id") } ?: return
        val configItem = getItem(id) ?: return
        item.editMeta { meta ->
//            (configItem.customName ?: configItem.name)?.let { meta.customName(parseToComponent(item, it)) }
            configItem.displayName?.let { meta.displayName(parseToComponent(item, it)) }
            configItem.itemName?.let { meta.itemName(parseToComponent(item, it)) }
//            configItem.buildLore()?.let { meta.lore(it) }
        }
    }
}