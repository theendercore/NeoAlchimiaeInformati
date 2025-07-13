package com.theendercore.alchimiae_informati.client.cmd

import com.mojang.brigadier.context.CommandContext
import com.ssblur.alchimiae.alchemy.ClientAlchemyHelper
import com.theendercore.alchimiae_informati.client.AlchimiaeInformatiClient.CONFIG
import com.theendercore.alchimiae_informati.client.cmd.AICommands.getIngredients
import com.theendercore.alchimiae_informati.client.cmd.AICommands.text
import com.theendercore.alchimiae_informati.client.isUnknown
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item

fun optimalItems(it: CommandContext<CommandSourceStack>): Int {
    val src = it.source

    val tag = getIngredients()
    if (tag == null) {
        src.sendSystemMessage(text("No ingredients found!"))
        return 0
    }
//    val ids = BuiltInRegistries.MOB_EFFECT.keySet().toList()
    val itemsToEffectMap = tag.associate { it.value() to ClientAlchemyHelper.get(it.value())!! }

    val potionEffects = itemsToEffectMap.flatMap { it.value }.filter { it != ClientAlchemyHelper.UNKNOWN }.toSet()
    val optimalItemList =
        fetchOptimalItems(potionEffects, itemsToEffectMap.filterNot { CONFIG.optimalBlackList.contains(it.key) })
    if (optimalItemList.isEmpty()) {
        src.sendSystemMessage(text("No optimal items found!"))
    } else {
        src.sendSystemMessage(text("Optimal items: "))
        optimalItemList.forEach { (item, ids) ->
            if (ids.any { it != ClientAlchemyHelper.UNKNOWN })
                src.sendSystemMessage(text(" - $item: [${ids.joinToString(", ") { it.path }}]"))
        }
    }

    return 1
}

fun filterItemsById(
    potionEffects: List<ResourceLocation>, itemsToIds: Map<Item, List<ResourceLocation>>,
): Map<Item, List<ResourceLocation>> {
    val visitedInputs = mutableListOf<ResourceLocation>()
    val itemCopy = itemsToIds.toList().sortedByDescending { it.second.filterNot(::isUnknown).size }
    val output = mutableMapOf<Item, List<ResourceLocation>>()

    for (effect in potionEffects) {
        if (visitedInputs.contains(effect)) continue
        val item = itemCopy.find { it.second.contains(effect) } ?: error("Could not find item for id $effect")
        visitedInputs.addAll(item.second.filterNot(::isUnknown))
        output[item.first] = item.second.filterNot(::isUnknown)
    }
    return output
}


fun fetchOptimalItems(
    potionEffects: Set<ResourceLocation>, itemsToIds: Map<Item, List<ResourceLocation>>,
): Map<Item, List<ResourceLocation>> {
    val foundEffects = mutableListOf<ResourceLocation>()
    val itemCopy = itemsToIds.toList()
        .mapNotNull {
            val effects = it.second.filterNot(::isUnknown)
            if (effects.isNotEmpty()) it.first to effects
            else null
        }
        .sortedByDescending { it.second.size }
    if (itemCopy.isEmpty()) {
        return mapOf()
    }

    val output = mutableMapOf<Item, List<ResourceLocation>>()

    while (potionEffects.size >= foundEffects.size) {
        val item = (if (foundEffects.isEmpty()) itemCopy.first() else getOptimal(itemCopy, foundEffects)) ?: break

        foundEffects.addAll(item.second.filterNot(::isUnknown))
        output[item.first] = item.second
    }
    return output
}

fun getOptimal(
    itemStack: List<Pair<Item, List<ResourceLocation>>>,
    foundEffects: MutableList<ResourceLocation>,
): Pair<Item, List<ResourceLocation>>? {
    val list = itemStack.mapNotNull {
        val effects = it.second.filterNot(foundEffects::contains)
        if (effects.isNotEmpty()) it.first to effects
        else null
    }
    return list.maxByOrNull { it.second.size }
}
