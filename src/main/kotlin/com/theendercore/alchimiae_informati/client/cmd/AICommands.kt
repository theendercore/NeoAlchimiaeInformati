package com.theendercore.alchimiae_informati.client.cmd

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.ssblur.alchimiae.alchemy.ClientAlchemyHelper
import com.theendercore.alchimiae_informati.client.AlchimiaeInformatiClient.TAG
import com.theendercore.alchimiae_informati.client.AlchimiaeInformatiClient.getAllKnowEffects
import com.theendercore.alchimiae_informati.client.isUnknown
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.neoforged.fml.loading.FMLLoader
import kotlin.jvm.optionals.getOrNull

object AICommands {
    fun commands(dispatcher: CommandDispatcher<CommandSourceStack>, ctx: CommandBuildContext) {
        val root = literal("alif").build()
        dispatcher.root.addChild(root)

        val missing = literal("missing").executes(::missing).build()
        root.addChild(missing)

        val contains = literal("contains").build()
        root.addChild(contains)

        val potionId = argument("id", StringArgumentType.greedyString())
            .suggests { context, builder ->
                BuiltInRegistries.MOB_EFFECT.keySet()
                    .filter { it.toString().lowercase().contains(builder.remainingLowerCase) }
                    .forEach { builder.suggest(it.toString().lowercase()) }

                builder.buildFuture()
            }
            .executes { contains(it, ResourceLocation.parse(StringArgumentType.getString(it, "id"))) }
            .build()
        contains.addChild(potionId)

        val optimalItems = literal("optimal").executes(::optimalItems).build()
        root.addChild(optimalItems)

        val knowEffects = literal("know_effects").executes(::knowEffects).build()
        root.addChild(knowEffects)

        val spreadsheet = literal("spreadsheet").executes(::spreadsheet).build()
        root.addChild(spreadsheet)
    }

    private fun spreadsheet(it: CommandContext<CommandSourceStack>): Int {
        val src = it.source

        val tag = getIngredients()?.map { it.value() }
        if (tag == null) {
            src.sendSystemMessage(text("No ingredients found!"))
            return 0
        }
        val effects = BuiltInRegistries.MOB_EFFECT.holders().toList()

        val file = FMLLoader.getGamePath().resolve("ai_export.csv").toFile()
        if (file.exists()) file.delete()


        var text = "[Item],[1],[2],[3],[4]"
        var hasLooped = false

        val map = tag.sortedBy { it.defaultInstance.hoverName.string }.associateWith { ClientAlchemyHelper.get(it) }

        for ((item, known) in map) {
            text += "\n${item.defaultInstance.hoverName.string}"
            if (known != null) {
                for (effect in known.sortedBy { it.toString() }) {
                    val name = effects.find { it.key().location() == effect }?.value()?.displayName?.string ?: " "
                    text += ",$name"
                }
            } else text += ", , , , "
        }

        file.writeText(text)
        src.sendSystemMessage(text("Exported to file!"))
        return 1
    }


    private fun knowEffects(it: CommandContext<CommandSourceStack>): Int {
        val src = it.source
        val effects = getAllKnowEffects()
        if (effects.isEmpty()) {
            src.sendSystemMessage(text("No effects known"))
            return 0
        }

        src.sendSystemMessage(text("All know effects:"))
        for (effect in effects) {
            src.sendSystemMessage(text(" - ").append(effect.displayName))
        }

        return 1
    }

    private fun contains(it: CommandContext<CommandSourceStack>, id: ResourceLocation): Int {
        val src = it.source

        val tag = getIngredients()
        if (tag == null) {
            src.sendSystemMessage(text("No ingredients found!"))
            return 0
        }

        src.sendSystemMessage(text("Items that have the [$id] effect: "))
        for (item in tag.map { it.value() }) {
            val effects = ClientAlchemyHelper.get(item) ?: continue
            if (effects.contains(id)) src.sendSystemMessage(text(" - $item "))
        }

        return 1
    }

    fun missing(it: CommandContext<CommandSourceStack>): Int {
        val src = it.source
        val tag = getIngredients()
        if (tag == null) {
            src.sendSystemMessage(text("No ingredients found!"))
            return 0
        }

        src.sendSystemMessage(text("Items missing effects: "))
        for (item in tag.map { it.value() }) {
            val effects = ClientAlchemyHelper.get(item)
            val count = effects?.count(::isUnknown)
            if (count != null && count > 0) src.sendSystemMessage(text(" - $item $count"))
        }

        return 1
    }

    fun getIngredients() = BuiltInRegistries.ITEM.getTag(TAG).getOrNull()
    fun text(str: String) = Component.literal(str)
}