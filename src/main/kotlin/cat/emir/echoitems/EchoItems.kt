package cat.emir.echoitems

import cat.emir.echoitems.items.ItemManager
import cat.emir.echoitems.serializers.MultiTypeConfigField
import cat.emir.echoitems.serializers.MultiTypeConfigFieldSerializer
import cat.emir.echoitems.text.TextParser
import cat.emir.echolib.command.CommandLib
import cat.emir.echolib.config.TypedEchoConfig
import cat.emir.echolib.event.EventLoader
import cat.emir.echolib.plugin.EchoJavaPlugin
import de.tr7zw.changeme.nbtapi.NBT

class EchoItems : EchoJavaPlugin() {
    companion object {
        lateinit var instance: EchoItems
    }

    val config = TypedEchoConfig(EchoItemsConfig::class, this, "config.yml") {
        it.defaultOptions { options ->
            options.serializers { builder ->
                builder.register(MultiTypeConfigField::class.java, MultiTypeConfigFieldSerializer)
            }
        }
    }
    val textParser = TextParser(this)
    val itemManager = ItemManager(this)
    var isPapiEnabled = false

    override fun onEnable() {
        instance = this
        config.load()
        isPapiEnabled = server.pluginManager.isPluginEnabled("PlaceholderAPI")
        itemManager.registerAllRecipes()

        if (!NBT.preloadApi()) {
            logger.severe("NBT-API wasn't initialized properly, disabling the plugin")
            server.pluginManager.disablePlugin(this)
            return
        }

        CommandLib.registerCommands(this, "cat.emir.echoitems.commands")
        EventLoader.registerListeners(this, "cat.emir.echoitems.listeners")
    }

    fun reload(): Long {
        val time = System.currentTimeMillis()
        logger.info("Reloading...")

        config.load()
        itemManager.registerAllRecipes()

        val elapsed = System.currentTimeMillis() - time
        logger.info("Reloaded in ${elapsed}ms")
        return elapsed
    }
}
