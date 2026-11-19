package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.GameRepository
import com.example.engine.CombatEngine
import com.example.engine.CombatPhase
import com.example.engine.CombatState
import com.example.engine.EnemyAIController
import com.example.engine.PriorityHolder
import com.example.model.AdventureCatalog
import com.example.model.CardCatalog
import com.example.model.CardTemplate
import com.example.model.CardType
import com.example.model.ChampionLoadout
import com.example.model.DeckCardEntry
import com.example.model.ItemTemplate
import com.example.model.LegendProfile
import com.example.model.MapNode
import com.example.model.NodeType
import com.example.model.PowerTemplate
import com.example.model.Rarity
import com.example.model.RunState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AppScreen {
    data object HomeHub : AppScreen
    data class ChampionDetail(val championId: String) : AppScreen
    data object AdventureSelect : AppScreen
    data object AdventureMap : AppScreen
    data object Combat : AppScreen
    data object CardDraft : AppScreen
    data object PowerDraft : AppScreen
    data object Shop : AppScreen
    data object Healer : AppScreen
    data object Reinforcement : AppScreen
    data object ItemChest : AppScreen
    data object NarrativeEvent : AppScreen
    data object RunSummary : AppScreen
}

data class CardRewardOffer(
    val card: CardTemplate,
    val attachedItem: ItemTemplate
)

data class ShopCardOffer(
    val card: CardTemplate,
    val attachedItem: ItemTemplate,
    val goldCost: Int,
    val purchased: Boolean = false
)

data class ShopItemOffer(
    val item: ItemTemplate,
    val goldCost: Int,
    val purchased: Boolean = false
)

data class ShopPowerOffer(
    val power: PowerTemplate,
    val goldCost: Int,
    val purchased: Boolean = false
)

data class NarrativeOption(
    val id: String,
    val title: String,
    val description: String
)

data class NarrativeEventData(
    val title: String,
    val flavor: String,
    val options: List<NarrativeOption>
)

data class RunRewardsSummary(
    val isVictory: Boolean,
    val championName: String,
    val adventureName: String,
    val xpGained: Int,
    val fragmentsGained: Int,
    val legendPointsGained: Int,
    val wildFragmentsGained: Int,
    val battlesWon: Int,
    val leveledUpTo: Int?
)

data class GameUiState(
    val screen: AppScreen = AppScreen.HomeHub,
    val selectedChampionId: String = "champ_jinx",
    val legendProfile: LegendProfile = LegendProfile(),
    val championLoadouts: Map<String, ChampionLoadout> = emptyMap(),
    val savedRun: RunState? = null,
    val activeRun: RunState? = null,
    val combatState: CombatState? = null,
    val cardDraftOptions: List<CardRewardOffer> = emptyList(),
    val powerDraftOptions: List<PowerTemplate> = emptyList(),
    val shopCards: List<ShopCardOffer> = emptyList(),
    val shopItems: List<ShopItemOffer> = emptyList(),
    val shopPower: ShopPowerOffer? = null,
    val pendingShopItemIndex: Int? = null,
    val reinforcementOptions: List<CardCatalog.ReinforcementPackage> = emptyList(),
    val chestOfferedItem: ItemTemplate? = null,
    val narrativeEvent: NarrativeEventData? = null,
    val runRewardsSummary: RunRewardsSummary? = null,
    val pendingPostCombatNode: MapNode? = null,
    val activeNodeIdInProgress: String? = null,
    val bannerMessage: String? = null
)

class GameViewModel(
    private val repository: GameRepository,
    private val aiStepDelayMs: Long = 700L
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var aiTurnJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            combine(
                repository.legendProfile,
                repository.championLoadouts,
                repository.activeSavedRun
            ) { profile, loadouts, savedRun ->
                Triple(profile, loadouts, savedRun)
            }.collect { (profile, loadouts, savedRun) ->
                _uiState.update { current ->
                    current.copy(
                        legendProfile = profile,
                        championLoadouts = loadouts,
                        savedRun = savedRun
                    )
                }
            }
        }
    }

    fun clearBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }

    fun navigateHome() {
        _uiState.update { it.copy(screen = AppScreen.HomeHub) }
    }

    fun selectChampion(championId: String) {
        _uiState.update { it.copy(selectedChampionId = championId) }
    }

    fun openChampionDetail(championId: String) {
        _uiState.update {
            it.copy(
                selectedChampionId = championId,
                screen = AppScreen.ChampionDetail(championId)
            )
        }
    }

    fun openAdventureSelect() {
        val loadout = _uiState.value.championLoadouts[_uiState.value.selectedChampionId]
        if (loadout == null || !loadout.isUnlocked) {
            _uiState.update { it.copy(bannerMessage = "Unlock this Champion before embarking.") }
            return
        }
        _uiState.update { it.copy(screen = AppScreen.AdventureSelect) }
    }

    // --- META PROGRESSION ---

    fun unlockChampion(championId: String) {
        val state = _uiState.value
        val template = CardCatalog.getChampion(championId)
        val loadout = state.championLoadouts[championId] ?: return
        if (loadout.isUnlocked) return

        val cost = template.unlockFragmentCost
        val totalAvailable = loadout.fragments + state.legendProfile.wildFragments
        if (totalAvailable < cost) {
            _uiState.update { it.copy(bannerMessage = "Need $cost Fragments (${cost - totalAvailable} more).") }
            return
        }

        val champSpent = minOf(loadout.fragments, cost)
        val wildSpent = cost - champSpent

        val updatedLoadout = loadout.copy(
            isUnlocked = true,
            starLevel = 1,
            fragments = loadout.fragments - champSpent
        )
        val updatedMeta = state.legendProfile.copy(
            wildFragments = state.legendProfile.wildFragments - wildSpent
        )

        viewModelScope.launch {
            repository.saveChampionLoadout(updatedLoadout)
            repository.saveLegendProfile(updatedMeta)
        }
        _uiState.update {
            it.copy(
                selectedChampionId = championId,
                bannerMessage = "${template.name} unlocked at 1★!"
            )
        }
    }

    fun upgradeStarPower(championId: String, targetStar: Int) {
        val state = _uiState.value
        val template = CardCatalog.getChampion(championId)
        val loadout = state.championLoadouts[championId] ?: return
        if (!loadout.isUnlocked || targetStar != loadout.starLevel + 1) return

        val spec = template.starPowers.find { it.starLevel == targetStar } ?: return
        val meta = state.legendProfile
        val totalFragments = loadout.fragments + meta.wildFragments

        if (totalFragments < spec.fragmentCost) {
            _uiState.update { it.copy(bannerMessage = "Need ${spec.fragmentCost} Fragments for ${targetStar}★.") }
            return
        }
        if (meta.starCrystals < spec.starCrystalCost) {
            _uiState.update { it.copy(bannerMessage = "Need ${spec.starCrystalCost} Star Crystals.") }
            return
        }
        if (meta.novaCrystals < spec.novaCrystalCost) {
            _uiState.update { it.copy(bannerMessage = "Need ${spec.novaCrystalCost} Nova Crystal.") }
            return
        }

        val champSpent = minOf(loadout.fragments, spec.fragmentCost)
        val wildSpent = spec.fragmentCost - champSpent

        val updatedLoadout = loadout.copy(
            starLevel = targetStar,
            fragments = loadout.fragments - champSpent
        )
        val updatedMeta = meta.copy(
            wildFragments = meta.wildFragments - wildSpent,
            starCrystals = meta.starCrystals - spec.starCrystalCost,
            novaCrystals = meta.novaCrystals - spec.novaCrystalCost
        )

        viewModelScope.launch {
            repository.saveChampionLoadout(updatedLoadout)
            repository.saveLegendProfile(updatedMeta)
        }
        _uiState.update { it.copy(bannerMessage = "${template.name} ascended to ${targetStar}★ (${spec.title})!") }
    }

    fun unlockConstellationNode(championId: String, nodeId: String) {
        val state = _uiState.value
        val template = CardCatalog.getChampion(championId)
        val loadout = state.championLoadouts[championId] ?: return
        if (loadout.unlockedConstellationNodes.contains(nodeId)) return

        val node = template.constellationNodes.find { it.id == nodeId } ?: return
        val meta = state.legendProfile
        val totalFrags = loadout.fragments + meta.wildFragments

        if (totalFrags < node.fragmentCost || meta.gemstones < node.gemstoneCost) {
            _uiState.update {
                it.copy(bannerMessage = "Need ${node.fragmentCost} Fragments & ${node.gemstoneCost} Gemstones.")
            }
            return
        }

        val champSpent = minOf(loadout.fragments, node.fragmentCost)
        val wildSpent = node.fragmentCost - champSpent

        val updatedLoadout = loadout.copy(
            fragments = loadout.fragments - champSpent,
            unlockedConstellationNodes = loadout.unlockedConstellationNodes + nodeId
        )
        val updatedMeta = meta.copy(
            wildFragments = meta.wildFragments - wildSpent,
            gemstones = meta.gemstones - node.gemstoneCost
        )

        viewModelScope.launch {
            repository.saveChampionLoadout(updatedLoadout)
            repository.saveLegendProfile(updatedMeta)
        }
        _uiState.update { it.copy(bannerMessage = "Unlocked Constellation: ${node.title}!") }
    }

    fun equipRelic(championId: String, slotIndex: Int, relicId: String?) {
        val loadout = _uiState.value.championLoadouts[championId] ?: return
        if (slotIndex !in 0..2 || slotIndex >= loadout.unlockedRelicSlots) return

        val updated = loadout.equippedRelics.toMutableList()
        // Prevent duplicate equipping of same relic on different slots
        if (relicId != null) {
            for (i in updated.indices) {
                if (updated[i] == relicId) updated[i] = null
            }
        }
        updated[slotIndex] = relicId

        viewModelScope.launch {
            repository.saveChampionLoadout(loadout.copy(equippedRelics = updated))
        }
    }

    fun cosmicForgeSlot(championId: String) {
        val state = _uiState.value
        val loadout = state.championLoadouts[championId] ?: return
        val meta = state.legendProfile
        if (loadout.unlockedRelicSlots >= 3 && loadout.cosmicForgedSlots >= 3) {
            _uiState.update { it.copy(bannerMessage = "All Relic slots already Epic-forged!") }
            return
        }
        val gemstoneCost = 15
        if (meta.gemstones < gemstoneCost) {
            _uiState.update { it.copy(bannerMessage = "Cosmic Forge requires $gemstoneCost Gemstones.") }
            return
        }

        val newUnlocked = minOf(3, maxOf(loadout.unlockedRelicSlots + 1, 3))
        val newForged = minOf(3, loadout.cosmicForgedSlots + 1)
        viewModelScope.launch {
            repository.saveChampionLoadout(
                loadout.copy(unlockedRelicSlots = newUnlocked, cosmicForgedSlots = newForged)
            )
            repository.saveLegendProfile(meta.copy(gemstones = meta.gemstones - gemstoneCost))
        }
        _uiState.update { it.copy(bannerMessage = "Cosmic Forge upgraded Relic slots to Epic tier!") }
    }

    // --- ADVENTURE & DAG MAP ---

    fun startAdventure(adventureId: String) {
        val state = _uiState.value
        val champId = state.selectedChampionId
        val loadout = state.championLoadouts[champId] ?: return
        if (!loadout.isUnlocked) return

        val champion = CardCatalog.getChampion(champId)
        val adventure = AdventureCatalog.getAdventure(adventureId)
        val startNode = adventure.mapNodes.first()

        // Apply permanent Champion Level deck items at level thresholds
        val initialDeck = champion.baseDeck.mapIndexed { idx, entry ->
            val bonusItems = mutableListOf<String>()
            if (loadout.championLevel >= 2 && idx == 1) bonusItems.add("item_studded_leather")
            if (loadout.championLevel >= 4 && idx == 0) bonusItems.add("item_farsight")
            entry.copy(attachedItemIds = entry.attachedItemIds + bonusItems)
        }

        val run = RunState(
            runId = UUID.randomUUID().toString().take(8),
            championId = champId,
            adventureId = adventure.id,
            currentNodeId = startNode.id,
            completedNodeIds = listOf(startNode.id),
            currentNexusHp = loadout.maxNexusHp,
            maxNexusHp = loadout.maxNexusHp,
            gold = loadout.startingGold,
            rerollsRemaining = loadout.startingRerolls,
            revivesRemaining = 1,
            activePowerIds = emptyList(),
            runDeck = initialDeck,
            battlesWon = 0
        )

        persistAndShowMap(run, "Embarked on ${adventure.name} with ${champion.name}!")
    }

    fun resumeSavedRun() {
        val saved = _uiState.value.savedRun ?: return
        _uiState.update {
            it.copy(
                selectedChampionId = saved.championId,
                activeRun = saved,
                screen = AppScreen.AdventureMap,
                bannerMessage = "Resumed adventure."
            )
        }
    }

    fun abandonSavedRun() {
        aiTurnJob?.cancel()
        viewModelScope.launch {
            repository.clearActiveRun()
        }
        _uiState.update {
            it.copy(
                activeRun = null,
                combatState = null,
                screen = AppScreen.HomeHub,
                bannerMessage = "Abandoned active adventure."
            )
        }
    }

    fun isNodeAvailable(run: RunState, node: MapNode): Boolean {
        if (run.completedNodeIds.contains(node.id)) return false
        val adventure = AdventureCatalog.getAdventure(run.adventureId)
        val current = adventure.mapNodes.find { it.id == run.currentNodeId } ?: return false
        return run.completedNodeIds.contains(current.id) && current.nextNodeIds.contains(node.id)
    }

    fun selectMapNode(nodeId: String) {
        val state = _uiState.value
        val run = state.activeRun ?: return
        val adventure = AdventureCatalog.getAdventure(run.adventureId)
        val node = adventure.mapNodes.find { it.id == nodeId } ?: return
        if (!isNodeAvailable(run, node)) return

        when (node.type) {
            NodeType.COMBAT, NodeType.ELITE_COMBAT, NodeType.MID_BOSS, NodeType.FINAL_BOSS -> {
                val loadout = state.championLoadouts[run.championId] ?: return
                val combat = CombatEngine.createEncounter(run, loadout, adventure, node)
                _uiState.update {
                    it.copy(
                        combatState = combat,
                        activeNodeIdInProgress = node.id,
                        pendingPostCombatNode = node,
                        screen = AppScreen.Combat
                    )
                }
                scheduleAiTurnIfNeeded(combat)
            }
            NodeType.POWER_NODE -> {
                val pool = rollPowerOptions(run.activePowerIds)
                _uiState.update {
                    it.copy(
                        powerDraftOptions = pool,
                        activeNodeIdInProgress = node.id,
                        screen = AppScreen.PowerDraft
                    )
                }
            }
            NodeType.SHOP_NODE -> {
                val shopCards = rollCardOffers(3).map { offer ->
                    val cost = 75 + offer.card.cost * 15
                    ShopCardOffer(offer.card, offer.attachedItem, cost)
                }
                val shopItems = CardCatalog.items.shuffled().take(3).map { item ->
                    ShopItemOffer(item, item.rarity.shopCostGold)
                }
                val availablePowers = CardCatalog.powers.filterNot { run.activePowerIds.contains(it.id) }
                val shopPower = availablePowers.randomOrNull()?.let { pow ->
                    ShopPowerOffer(pow, pow.rarity.shopCostGold + 75)
                }
                _uiState.update {
                    it.copy(
                        shopCards = shopCards,
                        shopItems = shopItems,
                        shopPower = shopPower,
                        pendingShopItemIndex = null,
                        activeNodeIdInProgress = node.id,
                        screen = AppScreen.Shop
                    )
                }
            }
            NodeType.HEALER_NODE -> {
                _uiState.update {
                    it.copy(
                        activeNodeIdInProgress = node.id,
                        screen = AppScreen.Healer
                    )
                }
            }
            NodeType.REINFORCEMENT_NODE -> {
                val packages = CardCatalog.reinforcementPackages.shuffled().take(3)
                _uiState.update {
                    it.copy(
                        reinforcementOptions = packages,
                        activeNodeIdInProgress = node.id,
                        screen = AppScreen.Reinforcement
                    )
                }
            }
            NodeType.ITEM_CHEST -> {
                val item = CardCatalog.items.filter { it.rarity != Rarity.COMMON }.randomOrNull()
                    ?: CardCatalog.items.random()
                _uiState.update {
                    it.copy(
                        chestOfferedItem = item,
                        activeNodeIdInProgress = node.id,
                        screen = AppScreen.ItemChest
                    )
                }
            }
            NodeType.NARRATIVE_EVENT -> {
                val event = buildNarrativeEvent(node)
                _uiState.update {
                    it.copy(
                        narrativeEvent = event,
                        activeNodeIdInProgress = node.id,
                        screen = AppScreen.NarrativeEvent
                    )
                }
            }
            NodeType.START -> {
                completeNodeAndReturnToMap(run, node.id)
            }
        }
    }

    // --- DRAFT & ENCOUNTER ACTIONS ---

    private fun rollCardOffers(count: Int = 3): List<CardRewardOffer> {
        val pool = CardCatalog.collectibleRewardPool.shuffled().take(count)
        return pool.map { card ->
            val validItems = if (card.type == CardType.SPELL || card.type == CardType.LANDMARK) {
                CardCatalog.items.filter { it.category == com.example.model.ItemCategory.ECONOMY || it.farsightGameStartDraw }
            } else {
                CardCatalog.items
            }
            CardRewardOffer(card, validItems.random())
        }
    }

    private fun rollPowerOptions(ownedPowerIds: List<String>): List<PowerTemplate> {
        val remaining = CardCatalog.powers.filterNot { ownedPowerIds.contains(it.id) }
        return (if (remaining.size >= 3) remaining else CardCatalog.powers).shuffled().take(3)
    }

    fun pickCardReward(offer: CardRewardOffer) {
        val state = _uiState.value
        val run = state.activeRun ?: return
        val updatedDeck = addCardWithItemToDeck(run.runDeck, offer.card.id, offer.attachedItem.id)
        val updatedRun = run.copy(runDeck = updatedDeck)

        val wasMidBoss = state.pendingPostCombatNode?.type == NodeType.MID_BOSS
        if (wasMidBoss) {
            val powerOffers = rollPowerOptions(updatedRun.activePowerIds)
            viewModelScope.launch { repository.saveActiveRun(updatedRun) }
            _uiState.update {
                it.copy(
                    activeRun = updatedRun,
                    powerDraftOptions = powerOffers,
                    pendingPostCombatNode = null,
                    activeNodeIdInProgress = null,
                    screen = AppScreen.PowerDraft,
                    bannerMessage = "Mid-Boss cleared! Choose a bonus Power."
                )
            }
        } else {
            _uiState.update { it.copy(pendingPostCombatNode = null) }
            persistAndShowMap(updatedRun, "Added ${offer.card.name} [${offer.attachedItem.name}] to deck.")
        }
    }

    fun skipCardDraft() {
        val state = _uiState.value
        val run = state.activeRun ?: return
        val wasMidBoss = state.pendingPostCombatNode?.type == NodeType.MID_BOSS
        if (wasMidBoss) {
            _uiState.update {
                it.copy(
                    powerDraftOptions = rollPowerOptions(run.activePowerIds),
                    pendingPostCombatNode = null,
                    activeNodeIdInProgress = null,
                    screen = AppScreen.PowerDraft
                )
            }
        } else {
            _uiState.update { it.copy(pendingPostCombatNode = null) }
            persistAndShowMap(run)
        }
    }

    fun rerollCardDraft() {
        val run = _uiState.value.activeRun ?: return
        if (run.rerollsRemaining <= 0) return
        val updated = run.copy(rerollsRemaining = run.rerollsRemaining - 1)
        viewModelScope.launch { repository.saveActiveRun(updated) }
        _uiState.update {
            it.copy(
                activeRun = updated,
                cardDraftOptions = rollCardOffers(3),
                bannerMessage = "Rerolled Card choices."
            )
        }
    }

    fun pickPowerReward(power: PowerTemplate) {
        val state = _uiState.value
        val run = state.activeRun ?: return
        val nodeId = state.activeNodeIdInProgress
        val updatedRun = run.copy(
            activePowerIds = run.activePowerIds + power.id,
            currentNodeId = nodeId ?: run.currentNodeId,
            completedNodeIds = if (nodeId != null) (run.completedNodeIds + nodeId).distinct() else run.completedNodeIds
        )
        _uiState.update { it.copy(activeNodeIdInProgress = null) }
        persistAndShowMap(updatedRun, "Acquired Power: ${power.name}!")
    }

    fun rerollPowerDraft() {
        val run = _uiState.value.activeRun ?: return
        if (run.rerollsRemaining <= 0) return
        val updated = run.copy(rerollsRemaining = run.rerollsRemaining - 1)
        viewModelScope.launch { repository.saveActiveRun(updated) }
        _uiState.update {
            it.copy(
                activeRun = updated,
                powerDraftOptions = rollPowerOptions(updated.activePowerIds),
                bannerMessage = "Rerolled Power choices."
            )
        }
    }

    // --- SHOP ---

    fun buyShopCard(index: Int) {
        val state = _uiState.value
        val run = state.activeRun ?: return
        val offer = state.shopCards.getOrNull(index) ?: return
        if (offer.purchased || run.gold < offer.goldCost) return

        val updatedDeck = addCardWithItemToDeck(run.runDeck, offer.card.id, offer.attachedItem.id)
        val updatedRun = run.copy(gold = run.gold - offer.goldCost, runDeck = updatedDeck)
        val updatedOffers = state.shopCards.mapIndexed { idx, item ->
            if (idx == index) item.copy(purchased = true) else item
        }
        viewModelScope.launch { repository.saveActiveRun(updatedRun) }
        _uiState.update {
            it.copy(
                activeRun = updatedRun,
                shopCards = updatedOffers,
                bannerMessage = "Bought ${offer.card.name} [${offer.attachedItem.name}]!"
            )
        }
    }

    fun selectShopItemToAttach(index: Int?) {
        val state = _uiState.value
        if (index != null) {
            val offer = state.shopItems.getOrNull(index) ?: return
            val run = state.activeRun ?: return
            if (offer.purchased || run.gold < offer.goldCost) return
        }
        _uiState.update { it.copy(pendingShopItemIndex = index) }
    }

    fun attachPendingShopItemToDeckCard(cardId: String) {
        val state = _uiState.value
        val idx = state.pendingShopItemIndex ?: return
        val offer = state.shopItems.getOrNull(idx) ?: return
        val run = state.activeRun ?: return
        if (offer.purchased || run.gold < offer.goldCost) return

        val updatedDeck = attachItemToCardInDeck(run.runDeck, cardId, offer.item.id)
        val updatedRun = run.copy(gold = run.gold - offer.goldCost, runDeck = updatedDeck)
        val updatedItems = state.shopItems.mapIndexed { i, it -> if (i == idx) it.copy(purchased = true) else it }
        viewModelScope.launch { repository.saveActiveRun(updatedRun) }
        _uiState.update {
            it.copy(
                activeRun = updatedRun,
                shopItems = updatedItems,
                pendingShopItemIndex = null,
                bannerMessage = "Equipped ${offer.item.name} onto ${CardCatalog.getCard(cardId).name}!"
            )
        }
    }

    fun buyShopPower() {
        val state = _uiState.value
        val run = state.activeRun ?: return
        val offer = state.shopPower ?: return
        if (offer.purchased || run.gold < offer.goldCost) return

        val updatedRun = run.copy(
            gold = run.gold - offer.goldCost,
            activePowerIds = run.activePowerIds + offer.power.id
        )
        viewModelScope.launch { repository.saveActiveRun(updatedRun) }
        _uiState.update {
            it.copy(
                activeRun = updatedRun,
                shopPower = offer.copy(purchased = true),
                bannerMessage = "Bought Power: ${offer.power.name}!"
            )
        }
    }

    fun leaveShop() {
        val state = _uiState.value
        val run = state.activeRun ?: return
        val nodeId = state.activeNodeIdInProgress ?: return
        completeNodeAndReturnToMap(run, nodeId)
    }

    // --- HEALER, REINFORCEMENT, CHEST, EVENT ---

    fun healAtHealer() {
        val state = _uiState.value
        val run = state.activeRun ?: return
        val nodeId = state.activeNodeIdInProgress ?: return
        val healAmt = maxOf(6, (run.maxNexusHp * 0.4f).toInt())
        val healedHp = minOf(run.maxNexusHp, run.currentNexusHp + healAmt)
        val updated = run.copy(currentNexusHp = healedHp)
        completeNodeAndReturnToMap(updated, nodeId, "Restored Nexus to $healedHp/${run.maxNexusHp} HP.")
    }

    fun cutCardAtHealer(cardId: String) {
        val state = _uiState.value
        val run = state.activeRun ?: return
        val nodeId = state.activeNodeIdInProgress ?: return
        if (run.runDeck.sumOf { it.copies } <= 6) {
            _uiState.update { it.copy(bannerMessage = "Deck cannot go below 6 cards.") }
            return
        }
        val updatedDeck = run.runDeck.mapNotNull { entry ->
            if (entry.cardId == cardId) {
                if (entry.copies > 1) entry.copy(copies = entry.copies - 1) else null
            } else entry
        }
        val updated = run.copy(runDeck = updatedDeck)
        completeNodeAndReturnToMap(updated, nodeId, "Cut 1 copy of ${CardCatalog.getCard(cardId).name} from deck.")
    }

    fun pickReinforcementPackage(pkg: CardCatalog.ReinforcementPackage) {
        val state = _uiState.value
        val run = state.activeRun ?: return
        val nodeId = state.activeNodeIdInProgress ?: return
        var deck = run.runDeck
        pkg.cardIds.forEachIndexed { idx, cardId ->
            val bonusItem = if (idx == 0) "item_studded_leather" else null
            deck = addCardWithItemToDeck(deck, cardId, bonusItem)
        }
        val updated = run.copy(runDeck = deck)
        completeNodeAndReturnToMap(updated, nodeId, "Recruited ${pkg.title}!")
    }

    fun applyChestItemToCard(cardId: String) {
        val state = _uiState.value
        val run = state.activeRun ?: return
        val nodeId = state.activeNodeIdInProgress ?: return
        val item = state.chestOfferedItem ?: return
        val updatedDeck = attachItemToCardInDeck(run.runDeck, cardId, item.id)
        val updated = run.copy(runDeck = updatedDeck)
        completeNodeAndReturnToMap(updated, nodeId, "Attached ${item.name} to ${CardCatalog.getCard(cardId).name}!")
    }

    fun chooseNarrativeOption(optionId: String) {
        val state = _uiState.value
        val run = state.activeRun ?: return
        val nodeId = state.activeNodeIdInProgress ?: return

        var updated = run
        val msg: String
        when (optionId) {
            "opt_blood_power" -> {
                val hpCost = minOf(run.currentNexusHp - 1, 6)
                val epicPower = CardCatalog.powers.filter {
                    it.rarity == Rarity.EPIC && !run.activePowerIds.contains(it.id)
                }.randomOrNull() ?: CardCatalog.powers.random()
                updated = run.copy(
                    currentNexusHp = maxOf(1, run.currentNexusHp - hpCost),
                    activePowerIds = run.activePowerIds + epicPower.id
                )
                msg = "Sacrificed $hpCost HP for ${epicPower.name}!"
            }
            "opt_gold" -> {
                updated = run.copy(gold = run.gold + 130)
                msg = "Found a hidden stash: +130 Gold!"
            }
            else -> {
                val healed = minOf(run.maxNexusHp, run.currentNexusHp + 6)
                updated = run.copy(currentNexusHp = healed, rerollsRemaining = run.rerollsRemaining + 1)
                msg = "Restored +6 HP and gained +1 Reroll!"
            }
        }
        completeNodeAndReturnToMap(updated, nodeId, msg)
    }

    private fun buildNarrativeEvent(node: MapNode): NarrativeEventData {
        return NarrativeEventData(
            title = node.title,
            flavor = "You discover an ancient Runeterra shrine humming with arcane runes. Choose how to claim its blessing.",
            options = listOf(
                NarrativeOption("opt_blood_power", "Blood Pact for Power", "Lose 6 Nexus HP to gain a random Epic Power."),
                NarrativeOption("opt_gold", "Claim Smuggler's Cache", "Gain +130 Gold for upcoming Merchants."),
                NarrativeOption("opt_meditate", "Meditate at the Altar", "Heal +6 Nexus HP and gain +1 Reroll Token.")
            )
        )
    }

    // --- COMBAT CONTROLS ---

    private fun scheduleAiTurnIfNeeded(combat: CombatState?) {
        aiTurnJob?.cancel()
        if (combat == null ||
            combat.phase == CombatPhase.GAME_OVER ||
            combat.priorityHolder != PriorityHolder.ENEMY
        ) {
            return
        }
        aiTurnJob = viewModelScope.launch {
            while (true) {
                delay(aiStepDelayMs)
                val latest = _uiState.value.combatState ?: break
                if (latest.phase == CombatPhase.GAME_OVER || latest.priorityHolder != PriorityHolder.ENEMY) {
                    break
                }
                val next = EnemyAIController.executeTurn(latest)
                _uiState.update { it.copy(combatState = next) }
                if (next.phase == CombatPhase.GAME_OVER || next.priorityHolder != PriorityHolder.ENEMY) {
                    break
                }
            }
        }
    }

    fun playCard(handCardId: String) {
        val current = _uiState.value.combatState ?: return
        if (current.priorityHolder != PriorityHolder.PLAYER) return
        val next = CombatEngine.playCardFromHand(current, handCardId)
        _uiState.update { it.copy(combatState = next) }
        scheduleAiTurnIfNeeded(next)
    }

    fun enterAttack() {
        val current = _uiState.value.combatState ?: return
        if (current.priorityHolder != PriorityHolder.PLAYER) return
        _uiState.update { it.copy(combatState = CombatEngine.enterAttackDeclaration(current)) }
    }

    fun cancelAttack() {
        val current = _uiState.value.combatState ?: return
        if (current.priorityHolder != PriorityHolder.PLAYER) return
        _uiState.update { it.copy(combatState = CombatEngine.cancelAttackDeclaration(current)) }
    }

    fun toggleAttacker(unitId: String) {
        val current = _uiState.value.combatState ?: return
        if (current.priorityHolder != PriorityHolder.PLAYER) return
        _uiState.update { it.copy(combatState = CombatEngine.toggleUnitAttack(current, unitId)) }
    }

    fun commitAttack() {
        val current = _uiState.value.combatState ?: return
        if (current.priorityHolder != PriorityHolder.PLAYER) return
        val next = CombatEngine.commitPlayerAttack(current)
        _uiState.update { it.copy(combatState = next) }
        scheduleAiTurnIfNeeded(next)
    }

    fun selectEnemyAttackerForBlock(enemyUnitId: String?) {
        val current = _uiState.value.combatState ?: return
        if (current.priorityHolder != PriorityHolder.PLAYER) return
        _uiState.update {
            it.copy(combatState = current.copy(selectedEnemyAttackerForBlock = enemyUnitId))
        }
    }

    fun assignBlocker(attackerId: String, blockerId: String) {
        val current = _uiState.value.combatState ?: return
        if (current.priorityHolder != PriorityHolder.PLAYER) return
        _uiState.update { it.copy(combatState = CombatEngine.assignPlayerBlocker(current, attackerId, blockerId)) }
    }

    fun removeBlocker(attackerId: String) {
        val current = _uiState.value.combatState ?: return
        if (current.priorityHolder != PriorityHolder.PLAYER) return
        _uiState.update { it.copy(combatState = CombatEngine.removePlayerBlocker(current, attackerId)) }
    }

    fun commitBlock() {
        val current = _uiState.value.combatState ?: return
        if (current.priorityHolder != PriorityHolder.PLAYER) return
        val next = CombatEngine.commitPlayerBlock(current)
        _uiState.update { it.copy(combatState = next) }
        scheduleAiTurnIfNeeded(next)
    }

    fun resolveSpellStack() {
        val current = _uiState.value.combatState ?: return
        if (current.priorityHolder != PriorityHolder.PLAYER || current.spellStack.isEmpty()) return
        val next = CombatEngine.passPriority(current)
        _uiState.update { it.copy(combatState = next) }
        scheduleAiTurnIfNeeded(next)
    }

    fun passTurn() {
        val current = _uiState.value.combatState ?: return
        if (current.priorityHolder != PriorityHolder.PLAYER) return
        val next = CombatEngine.passPriority(current)
        _uiState.update { it.copy(combatState = next) }
        scheduleAiTurnIfNeeded(next)
    }

    fun finishCombatVictory() {
        aiTurnJob?.cancel()
        val state = _uiState.value
        val combat = state.combatState ?: return
        val run = state.activeRun ?: return
        if (combat.phase != CombatPhase.GAME_OVER || combat.playerWon != true) return

        val node = combat.encounterNode
        val updatedRun = run.copy(
            currentNexusHp = maxOf(1, minOf(run.maxNexusHp, combat.playerNexusHp)),
            gold = run.gold + node.goldReward,
            battlesWon = run.battlesWon + 1,
            currentNodeId = node.id,
            completedNodeIds = (run.completedNodeIds + node.id).distinct()
        )

        if (node.type == NodeType.FINAL_BOSS) {
            finishRunWithRewards(updatedRun, isVictory = true)
        } else {
            viewModelScope.launch { repository.saveActiveRun(updatedRun) }
            _uiState.update {
                it.copy(
                    activeRun = updatedRun,
                    combatState = null,
                    activeNodeIdInProgress = null,
                    pendingPostCombatNode = node,
                    cardDraftOptions = rollCardOffers(3),
                    screen = AppScreen.CardDraft,
                    bannerMessage = "Victory! +${node.goldReward} Gold. Draft a Card."
                )
            }
        }
    }

    fun useReviveAndRetry() {
        val state = _uiState.value
        val run = state.activeRun ?: return
        val combat = state.combatState ?: return
        if (run.revivesRemaining <= 0) return

        val loadout = state.championLoadouts[run.championId] ?: return
        val restoredHp = maxOf(1, run.maxNexusHp / 2)
        val revivedRun = run.copy(
            revivesRemaining = run.revivesRemaining - 1,
            currentNexusHp = restoredHp
        )
        val freshCombat = CombatEngine.createEncounter(revivedRun, loadout, combat.adventure, combat.encounterNode)
        viewModelScope.launch { repository.saveActiveRun(revivedRun) }
        _uiState.update {
            it.copy(
                activeRun = revivedRun,
                combatState = freshCombat,
                bannerMessage = "Revive Token consumed! Nexus restored to $restoredHp HP."
            )
        }
        scheduleAiTurnIfNeeded(freshCombat)
    }

    fun acceptDefeat() {
        aiTurnJob?.cancel()
        val run = _uiState.value.activeRun ?: return
        finishRunWithRewards(run, isVictory = false)
    }

    private fun finishRunWithRewards(run: RunState, isVictory: Boolean) {
        val state = _uiState.value
        val adventure = AdventureCatalog.getAdventure(run.adventureId)
        val champion = CardCatalog.getChampion(run.championId)
        val loadout = state.championLoadouts[run.championId] ?: return
        val meta = state.legendProfile

        val progressFactor = if (isVictory) 1f else maxOf(0.25f, run.battlesWon / 4f)
        val xpGained = (adventure.rewardXp * progressFactor).toInt()
        val fragGained = if (isVictory) adventure.rewardFragments else maxOf(5, adventure.rewardFragments / 3)
        val wildGained = if (isVictory) 5 else 2
        val legendGained = if (isVictory) (100 * adventure.starRating).toInt().coerceAtLeast(50) else 25

        var newXp = loadout.championXp + xpGained
        var newLevel = loadout.championLevel
        while (newXp >= newLevel * 200 && newLevel < 30) {
            newXp -= newLevel * 200
            newLevel++
        }
        val leveledUpTo = if (newLevel > loadout.championLevel) newLevel else null
        val newSlots = maxOf(loadout.unlockedRelicSlots, if (newLevel >= 8) 3 else if (newLevel >= 3) 2 else 1)

        val updatedLoadout = loadout.copy(
            championLevel = newLevel,
            championXp = newXp,
            fragments = loadout.fragments + fragGained,
            unlockedRelicSlots = newSlots
        )

        var newLegendPts = meta.legendPoints + legendGained
        var newLegendLvl = meta.legendLevel
        while (newLegendPts >= newLegendLvl * 100) {
            newLegendPts -= newLegendLvl * 100
            newLegendLvl++
        }
        val updatedMeta = meta.copy(
            legendLevel = newLegendLvl,
            legendPoints = newLegendPts,
            wildFragments = meta.wildFragments + wildGained,
            starCrystals = meta.starCrystals + (if (isVictory) 2 else 0),
            gemstones = meta.gemstones + (if (isVictory) 10 else 3),
            completedAdventureIds = if (isVictory) (meta.completedAdventureIds + adventure.id).distinct() else meta.completedAdventureIds,
            totalRunsWon = meta.totalRunsWon + (if (isVictory) 1 else 0)
        )

        viewModelScope.launch {
            repository.saveChampionLoadout(updatedLoadout)
            repository.saveLegendProfile(updatedMeta)
            repository.clearActiveRun()
        }

        _uiState.update {
            it.copy(
                activeRun = null,
                combatState = null,
                activeNodeIdInProgress = null,
                pendingPostCombatNode = null,
                runRewardsSummary = RunRewardsSummary(
                    isVictory = isVictory,
                    championName = champion.name,
                    adventureName = adventure.name,
                    xpGained = xpGained,
                    fragmentsGained = fragGained,
                    legendPointsGained = legendGained,
                    wildFragmentsGained = wildGained,
                    battlesWon = run.battlesWon,
                    leveledUpTo = leveledUpTo
                ),
                screen = AppScreen.RunSummary
            )
        }
    }

    // --- HELPERS ---

    private fun completeNodeAndReturnToMap(run: RunState, nodeId: String, message: String? = null) {
        val updated = run.copy(
            currentNodeId = nodeId,
            completedNodeIds = (run.completedNodeIds + nodeId).distinct()
        )
        _uiState.update { it.copy(activeNodeIdInProgress = null, pendingShopItemIndex = null) }
        persistAndShowMap(updated, message)
    }

    private fun persistAndShowMap(run: RunState, message: String? = null) {
        viewModelScope.launch { repository.saveActiveRun(run) }
        _uiState.update {
            it.copy(
                activeRun = run,
                screen = AppScreen.AdventureMap,
                bannerMessage = message
            )
        }
    }

    private fun addCardWithItemToDeck(
        deck: List<DeckCardEntry>,
        cardId: String,
        itemId: String?
    ): List<DeckCardEntry> {
        val existing = deck.find { it.cardId == cardId }
        return if (existing != null) {
            deck.map { entry ->
                if (entry.cardId == cardId) {
                    entry.copy(
                        copies = entry.copies + 1,
                        attachedItemIds = if (itemId != null) entry.attachedItemIds + itemId else entry.attachedItemIds
                    )
                } else entry
            }
        } else {
            deck + DeckCardEntry(
                cardId = cardId,
                copies = 2,
                attachedItemIds = if (itemId != null) listOf(itemId) else emptyList()
            )
        }
    }

    private fun attachItemToCardInDeck(
        deck: List<DeckCardEntry>,
        cardId: String,
        itemId: String
    ): List<DeckCardEntry> {
        return deck.map { entry ->
            if (entry.cardId == cardId) {
                entry.copy(attachedItemIds = entry.attachedItemIds + itemId)
            } else entry
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameViewModel(GameRepository.getInstance(application)) as T
                }
            }
        }
    }
}
