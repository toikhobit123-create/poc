package com.example

import com.example.data.local.dao.GameDao
import com.example.data.local.entity.ChampionLoadoutEntity
import com.example.data.local.entity.PlayerMetaEntity
import com.example.data.local.entity.SavedRunEntity
import com.example.data.repository.GameRepository
import com.example.engine.CombatEngine
import com.example.engine.CombatPhase
import com.example.engine.CombatState
import com.example.engine.EnemyAIController
import com.example.engine.PriorityHolder
import com.example.model.AdventureCatalog
import com.example.model.CardCatalog
import com.example.model.ChampionLoadout
import com.example.model.DeckCardEntry
import com.example.model.HandCardInstance
import com.example.model.Keyword
import com.example.model.RunState
import com.example.model.UnitOnBoard
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeGameDao : GameDao {
    private val loadouts = MutableStateFlow<List<ChampionLoadoutEntity>>(emptyList())
    private val meta = MutableStateFlow<PlayerMetaEntity?>(null)
    private val savedRun = MutableStateFlow<SavedRunEntity?>(null)

    override fun observeChampionLoadouts(): Flow<List<ChampionLoadoutEntity>> = loadouts
    override suspend fun getChampionLoadout(id: String): ChampionLoadoutEntity? =
        loadouts.value.find { it.championId == id }

    override suspend fun upsertChampionLoadout(entity: ChampionLoadoutEntity) {
        loadouts.value = loadouts.value.filterNot { it.championId == entity.championId } + entity
    }

    override suspend fun upsertAllChampionLoadouts(entities: List<ChampionLoadoutEntity>) {
        val ids = entities.map { it.championId }.toSet()
        loadouts.value = loadouts.value.filterNot { ids.contains(it.championId) } + entities
    }

    override fun observePlayerMeta(): Flow<PlayerMetaEntity?> = meta
    override suspend fun getPlayerMeta(): PlayerMetaEntity? = meta.value
    override suspend fun upsertPlayerMeta(entity: PlayerMetaEntity) {
        meta.value = entity
    }

    override fun observeSavedRun(): Flow<SavedRunEntity?> = savedRun
    override suspend fun upsertSavedRun(entity: SavedRunEntity) {
        savedRun.value = entity
    }

    override suspend fun clearSavedRun() {
        savedRun.value = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `combat engine enforces keyword blocking rules for Elusive Fearsome and Brash`() {
        val elusiveAttacker = UnitOnBoard(
            instanceId = "a1",
            template = CardCatalog.getCard("card_sumpworks_mapper"),
            ownerIsPlayer = true,
            power = 1,
            health = 1,
            maxHealth = 1,
            keywords = setOf(Keyword.ELUSIVE)
        )
        val normalBlocker = UnitOnBoard(
            instanceId = "b1",
            template = CardCatalog.getCard("card_cithria"),
            ownerIsPlayer = false,
            power = 2,
            health = 2,
            maxHealth = 2,
            keywords = setOf(Keyword.TOUGH)
        )
        val sharpsightBlocker = UnitOnBoard(
            instanceId = "b2",
            template = CardCatalog.getCard("card_brightsteel_protector"),
            ownerIsPlayer = false,
            power = 3,
            health = 2,
            maxHealth = 2,
            keywords = setOf(Keyword.SHARPSIGHT)
        )
        val brashAttacker = UnitOnBoard(
            instanceId = "a2",
            template = CardCatalog.getCard("card_ashe"),
            ownerIsPlayer = true,
            power = 5,
            health = 3,
            maxHealth = 3,
            keywords = setOf(Keyword.BRASH)
        )

        assertFalse(normalBlocker.canBlockAttacker(elusiveAttacker))
        assertTrue(sharpsightBlocker.canBlockAttacker(elusiveAttacker))
        // Brash requires 3+ Health to block
        assertFalse(normalBlocker.canBlockAttacker(brashAttacker))
    }

    @Test
    fun `combat engine resolves Overwhelm Quick Attack and Deathless accurately`() {
        val adv = AdventureCatalog.getAdventure("adv_demacia_outskirts")
        val node = adv.mapNodes.first { it.id == "d_fight_1a" }
        val attacker = UnitOnBoard(
            instanceId = "atk_1",
            template = CardCatalog.getCard("card_ruthless_raider"),
            ownerIsPlayer = true,
            power = 5,
            health = 3,
            maxHealth = 3,
            keywords = setOf(Keyword.OVERWHELM, Keyword.QUICK_ATTACK)
        )
        val deathlessDefender = UnitOnBoard(
            instanceId = "def_1",
            template = CardCatalog.getCard("card_rekindled_knight"),
            ownerIsPlayer = false,
            power = 3,
            health = 2,
            maxHealth = 2,
            keywords = setOf(Keyword.DEATHLESS),
            hasDeathless = true
        )

        val initialState = CombatState(
            encounterNode = node,
            adventure = adv,
            championId = "champ_jinx",
            starLevel = 1,
            equippedRelics = emptyList(),
            activePowers = emptyList(),
            mutators = emptyList(),
            playerNexusHp = 30,
            playerMaxHp = 30,
            enemyNexusHp = 15,
            enemyMaxHp = 15,
            playerBoard = listOf(attacker),
            enemyBoard = listOf(deathlessDefender),
            phase = CombatPhase.COMBAT_DECLARE_ATTACK_PHASE,
            attackingUnitIds = listOf("atk_1")
        )

        val afterDeclare = CombatEngine.commitPlayerAttack(initialState)
        assertEquals(CombatPhase.COMBAT_DECLARE_BLOCK_PHASE, afterDeclare.phase)
        assertEquals(PriorityHolder.ENEMY, afterDeclare.priorityHolder)

        val afterAttack = EnemyAIController.executeTurn(afterDeclare)
        // Quick Attack kills defender before return strike -> attacker unharmed
        assertEquals(3, afterAttack.playerBoard.first().health)
        // Overwhelm deals 5 - 2 = 3 excess damage to enemy Nexus (15 -> 12)
        assertEquals(12, afterAttack.enemyNexusHp)
        // Deathless revives defender with 1 HP and Stunned
        assertEquals(1, afterAttack.enemyBoard.size)
        assertEquals(1, afterAttack.enemyBoard.first().health)
        assertTrue(afterAttack.enemyBoard.first().isStunned)
        assertFalse(afterAttack.enemyBoard.first().hasDeathless)
    }

    @Test
    fun `playing Burst spell with Spellslinger and Suppression calculates effective cost and resolves immediately`() {
        val adv = AdventureCatalog.getAdventure("adv_demacia_outskirts")
        val node = adv.mapNodes.first { it.id == "d_fight_1a" }
        val burstCard = HandCardInstance(
            instanceId = "hc_1",
            template = CardCatalog.getCard("card_prismatic_barrier"), // base cost 2
            effectiveCost = 2
        )
        val ally = UnitOnBoard(
            instanceId = "u_1",
            template = CardCatalog.getCard("card_cithria"),
            ownerIsPlayer = true,
            power = 2,
            health = 2,
            maxHealth = 2,
            keywords = emptySet()
        )
        val state = CombatState(
            encounterNode = node,
            adventure = adv,
            championId = "champ_lux",
            starLevel = 1,
            equippedRelics = emptyList(),
            activePowers = listOf(CardCatalog.getPower("pow_spellslinger")),
            mutators = emptyList(),
            playerNexusHp = 30,
            playerMaxHp = 30,
            enemyNexusHp = 15,
            enemyMaxHp = 15,
            playerMana = 1,
            playerSpellMana = 0,
            playerHand = listOf(burstCard),
            playerBoard = listOf(ally)
        )

        assertEquals(1, state.effectivePlayCost(burstCard))
        assertTrue(state.canPlayCard(burstCard))

        val afterPlay = CombatEngine.playCardFromHand(state, "hc_1")
        assertEquals(0, afterPlay.playerMana)
        assertTrue(afterPlay.playerBoard.first().hasBarrier)
        // Lux 1★ grants +1|+1 when casting a spell
        assertEquals(3, afterPlay.playerBoard.first().power)
        assertEquals(3, afterPlay.playerBoard.first().health)
    }

    @Test
    fun `viewModel manages adventure launch DAG node progression and healer restoration`() = runTest {
        val repo = GameRepository(FakeGameDao(), testDispatcher)
        repo.ensureSeeded()
        val vm = GameViewModel(repo)
        advanceUntilIdle()

        vm.startAdventure("adv_demacia_outskirts")
        advanceUntilIdle()

        val run = vm.uiState.value.activeRun
        assertNotNull(run)
        assertEquals(AppScreen.AdventureMap, vm.uiState.value.screen)

        val adv = AdventureCatalog.getAdventure("adv_demacia_outskirts")
        val powerNode = adv.mapNodes.first { it.id == "d_power_1" }
        assertTrue(vm.isNodeAvailable(run!!, powerNode))

        // Select Power Shrine
        vm.selectMapNode("d_power_1")
        assertEquals(AppScreen.PowerDraft, vm.uiState.value.screen)
        assertEquals(3, vm.uiState.value.powerDraftOptions.size)

        val chosenPower = vm.uiState.value.powerDraftOptions.first()
        vm.pickPowerReward(chosenPower)
        advanceUntilIdle()

        val updatedRun = vm.uiState.value.activeRun!!
        assertTrue(updatedRun.activePowerIds.contains(chosenPower.id))
        assertTrue(updatedRun.completedNodeIds.contains("d_power_1"))
        assertEquals(AppScreen.AdventureMap, vm.uiState.value.screen)
        // Branching nodes d_fight_1a and d_fight_1b both become available
        assertTrue(vm.isNodeAvailable(updatedRun, adv.mapNodes.first { it.id == "d_fight_1a" }))
        assertTrue(vm.isNodeAvailable(updatedRun, adv.mapNodes.first { it.id == "d_fight_1b" }))
    }

    @Test
    fun `playing unit passes priority to enemy and Burst spell keeps priority on player`() {
        val adv = AdventureCatalog.getAdventure("adv_demacia_outskirts")
        val node = adv.mapNodes.first { it.id == "d_fight_1a" }
        val unitCard = HandCardInstance(
            instanceId = "hc_unit",
            template = CardCatalog.getCard("card_cithria"), // 1-cost unit
            effectiveCost = 1
        )
        val burstCard = HandCardInstance(
            instanceId = "hc_burst",
            template = CardCatalog.getCard("card_prismatic_barrier"), // 2-cost Burst
            effectiveCost = 2
        )
        val enemyUnit = CardCatalog.getCard("card_cithria")
        val state = CombatState(
            encounterNode = node,
            adventure = adv,
            championId = "champ_garen",
            starLevel = 1,
            equippedRelics = emptyList(),
            activePowers = emptyList(),
            mutators = emptyList(),
            playerNexusHp = 30,
            playerMaxHp = 30,
            enemyNexusHp = 15,
            enemyMaxHp = 15,
            playerMana = 3,
            enemyMana = 2,
            playerHand = listOf(burstCard, unitCard),
            enemyHand = listOf(enemyUnit),
            phase = CombatPhase.MAIN_ACTION_PHASE,
            priorityHolder = PriorityHolder.PLAYER
        )

        // Burst spell resolves immediately and keeps priority on PLAYER
        val afterBurst = CombatEngine.playCardFromHand(state, "hc_burst")
        assertEquals(PriorityHolder.PLAYER, afterBurst.priorityHolder)
        assertEquals(CombatPhase.MAIN_ACTION_PHASE, afterBurst.phase)

        // Playing a unit summons it and switches priority to ENEMY
        val afterUnit = CombatEngine.playCardFromHand(afterBurst, "hc_unit")
        assertEquals(1, afterUnit.playerBoard.size)
        assertEquals(PriorityHolder.ENEMY, afterUnit.priorityHolder)

        // EnemyAIController plays its unit and returns priority to PLAYER
        val afterAi = EnemyAIController.executeTurn(afterUnit)
        assertEquals(1, afterAi.enemyBoard.size)
        assertEquals(PriorityHolder.PLAYER, afterAi.priorityHolder)
    }

    @Test
    fun `fast spell enters SPELL_STACK_PHASE and resolves LIFO when opponent passes`() {
        val adv = AdventureCatalog.getAdventure("adv_demacia_outskirts")
        val node = adv.mapNodes.first { it.id == "d_fight_1a" }
        val fastShot = HandCardInstance(
            instanceId = "hc_fast",
            template = CardCatalog.getCard("card_mystic_shot"), // 2-cost Fast, deal 2
            effectiveCost = 2
        )
        val state = CombatState(
            encounterNode = node,
            adventure = adv,
            championId = "champ_jinx",
            starLevel = 1,
            equippedRelics = emptyList(),
            activePowers = emptyList(),
            mutators = emptyList(),
            playerNexusHp = 30,
            playerMaxHp = 30,
            enemyNexusHp = 15,
            enemyMaxHp = 15,
            playerMana = 2,
            enemyMana = 1,
            playerHand = listOf(fastShot),
            enemyHand = emptyList(),
            phase = CombatPhase.MAIN_ACTION_PHASE,
            priorityHolder = PriorityHolder.PLAYER
        )

        val onStack = CombatEngine.playCardFromHand(state, "hc_fast")
        assertEquals(CombatPhase.SPELL_STACK_PHASE, onStack.phase)
        assertEquals(PriorityHolder.ENEMY, onStack.priorityHolder)
        assertEquals(1, onStack.spellStack.size)
        assertEquals(PriorityHolder.PLAYER, onStack.lastStackCaster)

        // AI has no reactive Fast spell -> AI passes to resolve Spell Stack LIFO
        val resolved = EnemyAIController.executeTurn(onStack)
        assertTrue(resolved.spellStack.isEmpty())
        assertEquals(CombatPhase.MAIN_ACTION_PHASE, resolved.phase)
        // Priority returns to opponent of lastStackCaster (PLAYER -> ENEMY)
        assertEquals(PriorityHolder.ENEMY, resolved.priorityHolder)
        // Jinx 1★ dealt 1 on play + Mystic Shot dealt 2 = 3 damage total (15 -> 12)
        assertEquals(12, resolved.enemyNexusHp)
    }

    @Test
    fun `consecutive passes advance round bank spell mana draw cards and swap attack token`() {
        val adv = AdventureCatalog.getAdventure("adv_demacia_outskirts")
        val node = adv.mapNodes.first { it.id == "d_fight_1a" }
        val drawCard = HandCardInstance(
            instanceId = "hc_draw",
            template = CardCatalog.getCard("card_cithria"),
            effectiveCost = 1
        )
        val state = CombatState(
            encounterNode = node,
            adventure = adv,
            championId = "champ_garen",
            starLevel = 1,
            equippedRelics = emptyList(),
            activePowers = emptyList(),
            mutators = emptyList(),
            round = 1,
            playerNexusHp = 30,
            playerMaxHp = 30,
            enemyNexusHp = 15,
            enemyMaxHp = 15,
            playerMana = 4, // up to 3 unspent Unit Mana banks into Spell Mana
            playerMaxMana = 4,
            playerSpellMana = 0,
            enemyMana = 1,
            enemyMaxMana = 1,
            enemySpellMana = 0,
            playerHasAttackToken = true,
            enemyHasAttackToken = false,
            playerDrawPile = listOf(drawCard),
            enemyDrawPool = listOf(CardCatalog.getCard("card_cithria")),
            phase = CombatPhase.MAIN_ACTION_PHASE,
            priorityHolder = PriorityHolder.PLAYER,
            consecutivePasses = 0
        )

        val afterPlayerPass = CombatEngine.passPriority(state)
        assertEquals(1, afterPlayerPass.consecutivePasses)
        assertEquals(PriorityHolder.ENEMY, afterPlayerPass.priorityHolder)

        // Enemy has no cards in hand -> AI passes (consecutivePasses == 2 -> Round 2)
        val nextRound = EnemyAIController.executeTurn(afterPlayerPass)
        assertEquals(2, nextRound.round)
        assertEquals(5, nextRound.playerMana)
        assertEquals(5, nextRound.playerMaxMana)
        assertEquals(3, nextRound.playerSpellMana)
        assertEquals(1, nextRound.enemySpellMana)
        assertFalse(nextRound.playerHasAttackToken)
        assertTrue(nextRound.enemyHasAttackToken)
        assertEquals(PriorityHolder.ENEMY, nextRound.priorityHolder)
        assertEquals(1, nextRound.playerHand.size)
        assertEquals(1, nextRound.enemyHand.size)
    }

    @Test
    fun `viewModel automatically triggers EnemyAIController after 700ms delay when priority shifts to enemy`() = runTest(testDispatcher) {
        val repo = GameRepository(FakeGameDao(), testDispatcher)
        repo.ensureSeeded()
        val vm = GameViewModel(repo, aiStepDelayMs = 700L)
        advanceUntilIdle()

        vm.startAdventure("adv_demacia_outskirts")
        advanceUntilIdle()
        vm.selectMapNode("d_power_1")
        vm.pickPowerReward(vm.uiState.value.powerDraftOptions.first())
        advanceUntilIdle()

        vm.selectMapNode("d_fight_1a")
        runCurrent()

        val startCombat = vm.uiState.value.combatState
        assertNotNull(startCombat)
        assertEquals(PriorityHolder.PLAYER, startCombat!!.priorityHolder)

        // Pass priority -> ENEMY gets priority; AI should NOT execute before 700ms
        vm.passTurn()
        runCurrent()
        val afterPass = vm.uiState.value.combatState!!
        assertEquals(PriorityHolder.ENEMY, afterPass.priorityHolder)

        advanceTimeBy(699L)
        runCurrent()
        assertEquals(afterPass, vm.uiState.value.combatState)

        // At 700ms, EnemyAIController executes automatically (summons 1-cost unit -> PLAYER, or passes -> Round 2)
        advanceTimeBy(2L)
        runCurrent()
        val afterAi = vm.uiState.value.combatState!!
        assertTrue(afterAi != afterPass)
        assertTrue(afterAi.priorityHolder == PriorityHolder.PLAYER || afterAi.round == 2)

        // Once AI finishes any Round 2 opening action, priority returns to PLAYER
        advanceUntilIdle()
        assertEquals(PriorityHolder.PLAYER, vm.uiState.value.combatState!!.priorityHolder)
    }
}
