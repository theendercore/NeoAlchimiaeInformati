package com.theendercore.alchimiae_informati.client

import com.theendercore.alchimiae_informati.client.AlchimiaeInformatiClient.MODID
import com.theendercore.alchimiae_informati.client.AlchimiaeInformatiClient.id
import me.fzzyhmstrs.fzzy_config.config.Config
import net.minecraft.world.item.Item

class AIConfig : Config(id(MODID)) {
    var optimalBlackList = listOf<Item>()
}