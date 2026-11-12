package cat.emir.echoitems

import cat.emir.echoitems.serializers.MultiTypeConfigField
import org.bukkit.Material
import org.bukkit.inventory.EquipmentSlotGroup
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.NodeKey
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class EchoItemsConfig(
    val tags: MutableMap<String, List<String>> = mutableMapOf(),
    val enchantments: Map<String, Enchantment> = emptyMap(),
    val items: Map<String, Item> = emptyMap()
) {
    @ConfigSerializable
    data class Enchantment(
        val name: String,
        @Setting("max_level") val maxLevel: Int,
        val weight: Int,
        @Setting("anvil_cost") val anvilCost: Int,
        @Setting("min_cost") val minCost: Cost,
        @Setting("max_cost") val maxCost: Cost,
        val incompatible: List<String> = emptyList(),
        @Setting("supported_items") val supportedItems: List<String>,
        @Setting("primary_items") val primaryItems: List<String> = emptyList(),
        val properties: Properties = Properties(),
        @Setting("active_slots") val activeSlots: List<String> = emptyList(),
        val effects: Effects = Effects()
    ) {
        @ConfigSerializable
        data class Properties(
            val curse: Boolean = false,
            val enchantable: Boolean = false,
            val tradeable: Boolean = false,
            val treasure: Boolean = false,
            @Setting("on_random_loot") val onRandomLoot: Boolean = false,
            @Setting("on_traded_equipment") val onTradedEquipment: Boolean = false,
            @Setting("on_mob_equipment") val onMobEquipment: Boolean = false
        )

        @ConfigSerializable
        data class Cost(
            val base: Int,
            @Setting("per_level") val perLevel: Int
        )

        @ConfigSerializable
        data class Effects(
            val attributes: List<Attribute> = emptyList()
        ) {
            @ConfigSerializable
            data class Attribute(
                val attribute: String,
                val key: String,
                val amount: Amount,
                val operation: String
            ) {
                @ConfigSerializable
                data class Amount(
                    val base: Float,
                    @Setting("add_per_level") val addPerLevel: Float
                )
            }
        }
    }

    @ConfigSerializable
    data class Item(
        @NodeKey val id: String,
        @Setting("itemname") val itemName: String? = null,
        @Setting("customname") val customName: String? = null,
        @Setting("displayname") val displayName: String? = null,
        val name: String? = null,
        @Setting("material") val _material: String? = null,
        val lore: List<String>? = null,
        val enchantments: Map<String, Int> = emptyMap(),
        val modifiers: Map<String, MultiTypeConfigField> = emptyMap(),
        val flags: List<String> = emptyList(),
        val attributes: List<Attribute> = emptyList(),
        @Setting("has_creation_time") val hasCreationTime: Boolean = false,
        val model: String? =  null,
        @Setting("glint_override") val glintOverride: Boolean? = null,
        @Setting("max_stack_size") val maxStackSize: Int? = null,
        val rarity: String? = null,
        val tooltip: String? = null,
        val glider: Boolean? = null,
        val equippable: Equippable? = null,
        val food: Food? = null,
        val consumable: Consumable? = null,
        val tool: Tool? = null,
        val weapon: Weapon? = null,
        val use: Use? = null,
        val damage: Damage? = null,
        val trim: Trim? = null,
        val nbt: List<Nbt> = emptyList(),
        val recipe: Recipe? = null
    ) {
        val material: Material
            get() = Material.getMaterial(_material!!.uppercase())!!

        @ConfigSerializable
        data class Attribute(
            val attribute: String,
            val key: String,
            val amount: Double,
            val operation: String,
            val slot: String
        )

        @ConfigSerializable
        data class Equippable(
            val slot: String,
            val model: String,
            val sound: String? = null,
            val shearable: Boolean = false,
            @Setting("shear_sound") val shearSound: String? = null,
            @Setting("damage_on_hurt") val damageOnHurt: Boolean = false,
            val dispensable: Boolean = false,
            val swappable: Boolean = false
        )

        @ConfigSerializable
        data class Food(
            @Setting("can_always_eat") val canAlwaysEat: Boolean,
            val nutrition: Int,
            val saturation: Float
        )

        @ConfigSerializable
        data class Consumable(
            val seconds: Float,
            @Setting("has_particles") val hasParticles: Boolean,
            val sound: String,
            val animation: String,
        )

        @ConfigSerializable
        data class Tool(
            @Setting("damage_per_block") val damagePerBlock: Int? = null,
            @Setting("default_speed") val defaultSpeed: Float? = null,
            @Setting("can_destroy_in_creative") val canDestroyInCreative: Boolean? = null,
            val rules: List<Rule> = emptyList()
        ) {
            @ConfigSerializable
            data class Rule(
                val speed: Float? = null,
                @Setting("correct_for_drops") val correctForDrops: Boolean? = null,
                val blocks: List<String>
            )
        }

        @ConfigSerializable
        data class Weapon(
            @Setting("item_damage") val itemDamage: Int? = null,
            @Setting("disable_blocking_seconds") val disableBlockingSeconds: Float? = null
        )

        @ConfigSerializable
        data class Use(
            val cooldown: Cooldown,
            val remainder: String? = null,
        ) {
            @ConfigSerializable
            data class Cooldown(
                val group: String? = null,
                val seconds: Float
            )
        }

        @ConfigSerializable
        data class Damage(
            val unbreakable: Boolean? = null,
            val max: Int? = null,
            val current: Int? = null
        )

        @ConfigSerializable
        data class Trim(
            val pattern: String,
            val material: String
        )

        @ConfigSerializable
        data class Nbt(
            val key: String,
            val value: MultiTypeConfigField
        )

        @ConfigSerializable
        data class Recipe(
            val crafting: List<Crafting>? = null,
            val smithing: List<Smithing>? = null,
            val furnace: List<Furnace>? = null
        ) {
            @ConfigSerializable
            data class Crafting(
                val shape: List<String>? = null,
                val ingredients: Map<String, String>,
                val amount: Int = 1
            )

            @ConfigSerializable
            data class Smithing(
                val template: String? = null,
                val base: String,
                val addition: String? = null,
                val result: String? = null,
                @Setting("copy_data") val copyData: Boolean? = true
            )

            @ConfigSerializable
            data class Furnace(
                val input: String,
                val result: String? = null,
                val experience: Float,
                val time: Int,
            )
        }
    }
}