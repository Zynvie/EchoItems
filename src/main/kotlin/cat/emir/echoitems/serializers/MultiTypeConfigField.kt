package cat.emir.echoitems.serializers

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

sealed class MultiTypeConfigField {
    data class StringValue(val value: String) : MultiTypeConfigField()
    data class LongValue(val value: Long) : MultiTypeConfigField()
    data class DoubleValue(val value: Double) : MultiTypeConfigField()
    data class BooleanValue(val value: Boolean) : MultiTypeConfigField()

    fun raw(): Any = when (this) {
        is StringValue -> value
        is LongValue -> value
        is DoubleValue -> value
        is BooleanValue -> value
    }
}

object MultiTypeConfigFieldSerializer : TypeSerializer<MultiTypeConfigField> {

    override fun deserialize(type: Type, node: ConfigurationNode): MultiTypeConfigField {
        return when (val raw = node.rawScalar()) {
            is Boolean -> MultiTypeConfigField.BooleanValue(raw)
            is Long -> MultiTypeConfigField.LongValue(raw)
            is Int -> MultiTypeConfigField.LongValue(raw.toLong())
            is Double -> MultiTypeConfigField.DoubleValue(raw)
            is Float -> MultiTypeConfigField.DoubleValue(raw.toDouble())
            is String -> MultiTypeConfigField.StringValue(raw)
            null -> throw SerializationException(
                "Expected a scalar (string/long/double/boolean) for ModifierValue, got a map or list"
            )
            else -> throw SerializationException(
                "Unsupported scalar type ${raw.javaClass} for ModifierValue"
            )
        }
    }

    override fun serialize(type: Type, obj: MultiTypeConfigField?, node: ConfigurationNode) {
        node.raw(obj?.raw())
    }
}