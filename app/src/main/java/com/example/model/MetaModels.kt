package com.example.model

data class StarPowerSpec(
    val starLevel: Int,
    val title: String,
    val description: String,
    val fragmentCost: Int,
    val starCrystalCost: Int = 0,
    val novaCrystalCost: Int = 0
)

data class ConstellationMinorNode(
    val id: String,
    val title: String,
    val description: String,
    val fragmentCost: Int,
    val gemstoneCost: Int = 0,
    val bonusMaxHp: Int = 0,
    val bonusRerolls: Int = 0,
    val bonusStartingGold: Int = 0
)

data class ChampionTemplate(
    val id: String,
    val name: String,
    val title: String,
    val region: Region,
    val cardId: String,
    val archetypeSummary: String,
    val unlockFragmentCost: Int = 0,
    val baseMaxHp: Int = 30,
    val baseDeck: List<DeckCardEntry>,
    val starPowers: List<StarPowerSpec>,
    val constellationNodes: List<ConstellationMinorNode>
)

data class ChampionLoadout(
    val championId: String,
    val isUnlocked: Boolean = true,
    val starLevel: Int = 1,
    val championLevel: Int = 1,
    val championXp: Int = 0,
    val fragments: Int = 10,
    val unlockedConstellationNodes: List<String> = emptyList(),
    val equippedRelics: List<String?> = listOf(null, null, null),
    val unlockedRelicSlots: Int = 1,
    val cosmicForgedSlots: Int = 0
) {
    val maxNexusHp: Int
        get() = 30 + (championLevel / 2) * 2 + (if (starLevel >= 4) 5 else 0) + (unlockedConstellationNodes.size * 3)

    val startingGold: Int
        get() = 100 + (championLevel * 10) + (unlockedConstellationNodes.count { it.contains("gold") } * 50)

    val startingRerolls: Int
        get() = 2 + (if (championLevel >= 6) 1 else 0) + (unlockedConstellationNodes.count { it.contains("reroll") })

    val startingManaBonus: Int
        get() = if (starLevel >= 2) 1 else 0
}

enum class NodeType(val displayName: String, val shortCode: String) {
    START("Trailhead", "START"),
    COMBAT("Skirmish", "FIGHT"),
    ELITE_COMBAT("Elite Foe", "ELITE"),
    MID_BOSS("Mid-Boss", "M-BOSS"),
    FINAL_BOSS("Final Boss", "BOSS"),
    POWER_NODE("Power Shrine", "POWER"),
    HEALER_NODE("Healer Sanctuary", "HEAL"),
    SHOP_NODE("Smooth Soloist", "SHOP"),
    REINFORCEMENT_NODE("Reinforcements", "ALLY"),
    ITEM_CHEST("Item Vault", "ITEM"),
    NARRATIVE_EVENT("Encounter", "EVENT")
}

data class MapNode(
    val id: String,
    val act: Int, // 1 = Start to Mid-Boss, 2 = Mid-Boss to Final Boss
    val row: Int, // 0..6 vertical progression
    val column: Int, // 0, 1, 2 horizontal branching lane
    val type: NodeType,
    val title: String,
    val subtitle: String,
    val enemyName: String = "",
    val enemyPowerTitle: String = "",
    val enemyPowerDescription: String = "",
    val enemyMaxHp: Int = 20,
    val enemyStartingMana: Int = 1,
    val enemyDeckCardIds: List<String> = emptyList(),
    val goldReward: Int = 60,
    val nextNodeIds: List<String> = emptyList()
)

data class GlobalMutator(
    val id: String,
    val name: String,
    val description: String
)

data class AdventureConfig(
    val id: String,
    val name: String,
    val region: Region,
    val starRating: Float,
    val difficultyLabel: String,
    val description: String,
    val bossName: String,
    val rewardXp: Int,
    val rewardFragments: Int,
    val mutators: List<GlobalMutator>,
    val mapNodes: List<MapNode>
)

data class RunState(
    val runId: String,
    val championId: String,
    val adventureId: String,
    val currentNodeId: String,
    val completedNodeIds: List<String> = emptyList(),
    val currentNexusHp: Int,
    val maxNexusHp: Int,
    val gold: Int,
    val rerollsRemaining: Int,
    val revivesRemaining: Int = 1,
    val activePowerIds: List<String> = emptyList(),
    val runDeck: List<DeckCardEntry> = emptyList(),
    val battlesWon: Int = 0,
    val isVictory: Boolean = false,
    val isDefeat: Boolean = false
)

data class LegendProfile(
    val legendLevel: Int = 1,
    val legendPoints: Int = 0,
    val wildFragments: Int = 30,
    val starCrystals: Int = 10,
    val novaCrystals: Int = 1,
    val gemstones: Int = 25,
    val ownedRelicIds: List<String> = listOf("relic_stalkers_blade", "relic_archangels_staff", "relic_ludens_tempest", "relic_guinsoos_rageblade"),
    val completedAdventureIds: List<String> = emptyList(),
    val totalRunsWon: Int = 0
) {
    val pointsToNextLevel: Int
        get() = legendLevel * 100
}
