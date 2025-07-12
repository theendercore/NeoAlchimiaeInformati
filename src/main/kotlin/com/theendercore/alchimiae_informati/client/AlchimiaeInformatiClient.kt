package com.theendercore.alchimiae_informati.client

import com.ssblur.alchimiae.AlchimiaeMod
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

@Mod(value = AlchimiaeInformatiClient.MODID, dist = [Dist.CLIENT])
object AlchimiaeInformatiClient {
    const val MODID = "alchimiae_informati"

    @JvmField
    val log: Logger = LoggerFactory.getLogger(MODID)

    init {
        log.info("Give me all of the information!")
        FORGE_BUS.addListener { event: RegisterClientCommandsEvent ->
            AICommands.commands(event.dispatcher, event.buildContext)
        }
    }

    fun id(id: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MODID, id)
    val TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(AlchimiaeMod.MOD_ID, "ingredients"))
}
