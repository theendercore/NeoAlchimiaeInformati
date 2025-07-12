package com.theendercore.alchimiae_informati.client

import com.mojang.brigadier.context.CommandContext
import com.ssblur.alchimiae.alchemy.ClientAlchemyHelper
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import com.theendercore.alchimiae_informati.client.AICommands.getIngredients
import com.theendercore.alchimiae_informati.client.AICommands.text
import net.minecraft.commands.CommandSourceStack

fun optimalItems(it: CommandContext<CommandSourceStack>): Int {
    val src = it.source

    val tag = getIngredients()
    if (tag == null) {
        src.sendSystemMessage(text("No ingredients found!"))
        return 0
    }
    val ids = BuiltInRegistries.MOB_EFFECT.keySet().toList()
    val items = tag.associate { it.value() to ClientAlchemyHelper.get(it.value())!! }

    val ids2 = items.flatMap { it.value.toSet() }.filter { it != ClientAlchemyHelper.UNKNOWN }
    val optimal = filterItemsById(ids2, items)
    if (optimal.isEmpty()) {
        src.sendSystemMessage(text("No optimal items found!"))
    } else {
        src.sendSystemMessage(text("Optimal items: "))
        optimal.forEach { (item, ids) ->
            if (ids.any { it != ClientAlchemyHelper.UNKNOWN })
                src.sendSystemMessage(text(" - $item: [${ids.joinToString(", ") { it.path }}]"))
        }
    }


    return 1
}

fun isUnknown(id: ResourceLocation) = id == ClientAlchemyHelper.UNKNOWN

fun filterItemsById(
    inputIds: List<ResourceLocation>, itemsToIds: Map<Item, List<ResourceLocation>>
): Map<Item, List<ResourceLocation>> {
    val visitedInputs = mutableListOf<ResourceLocation>()
    val itemCopy = itemsToIds.toList().sortedByDescending { it.second.filterNot(::isUnknown).size }
    val output = mutableMapOf<Item, List<ResourceLocation>>()

    for (id in inputIds) {
        if (visitedInputs.contains(id)) continue
        val item = itemCopy.find { it.second.contains(id) } ?: error("Could not find item for id $id")
        visitedInputs.addAll(item.second.filterNot(::isUnknown))
        output[item.first] = item.second.filterNot(::isUnknown)
    }
    return output
}
