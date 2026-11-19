package com.example.data.repository

import android.content.Context
import com.example.data.local.GameDatabase
import com.example.data.local.dao.GameDao
import com.example.data.local.entity.ChampionLoadoutEntity
import com.example.data.local.entity.PlayerMetaEntity
import com.example.data.local.entity.SavedRunEntity
import com.example.model.CardCatalog
import com.example.model.ChampionLoadout
import com.example.model.DeckCardEntry
import com.example.model.LegendProfile
import com.example.model.RunState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GameRepository(
    private val dao: GameDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val runStateAdapter = moshi.adapter(RunState::class.java)

    val championLoadouts: Flow<Map<String, ChampionLoadout>> =
        dao.observeChampionLoadouts().map { list ->
            list.associate { it.championId to it.toDomain() }
        }

    val legendProfile: Flow<LegendProfile> =
        dao.observePlayerMeta().map { it?.toDomain() ?: LegendProfile() }

    val activeSavedRun: Flow<RunState?> =
        dao.observeSavedRun().map { entity ->
            entity?.let { deserializeRunState(it.runJson) }
        }

    suspend fun ensureSeeded() = withContext(ioDispatcher) {
        val existingMeta = dao.getPlayerMeta()
        if (existingMeta == null) {
            dao.upsertPlayerMeta(PlayerMetaEntity.fromDomain(LegendProfile()))
        }

        for (champ in CardCatalog.champions) {
            val existing = dao.getChampionLoadout(champ.id)
            if (existing == null) {
                val initialUnlocked = champ.unlockFragmentCost == 0
                val initialRelics = when (champ.id) {
                    "champ_jinx" -> listOf("relic_ludens_tempest", null, null)
                    "champ_lux" -> listOf("relic_archangels_staff", null, null)
                    "champ_yasuo" -> listOf("relic_guinsoos_rageblade", null, null)
                    else -> listOf(null, null, null)
                }
                val initialFragments = if (initialUnlocked) 15 else 5
                val loadout = ChampionLoadout(
                    championId = champ.id,
                    isUnlocked = initialUnlocked,
                    starLevel = if (initialUnlocked) 1 else 0,
                    championLevel = if (initialUnlocked) 4 else 1,
                    championXp = 0,
                    fragments = initialFragments,
                    unlockedConstellationNodes = emptyList(),
                    equippedRelics = initialRelics,
                    unlockedRelicSlots = 2,
                    cosmicForgedSlots = 0
                )
                dao.upsertChampionLoadout(ChampionLoadoutEntity.fromDomain(loadout))
            }
        }
    }

    suspend fun saveChampionLoadout(loadout: ChampionLoadout) = withContext(ioDispatcher) {
        dao.upsertChampionLoadout(ChampionLoadoutEntity.fromDomain(loadout))
    }

    suspend fun saveLegendProfile(profile: LegendProfile) = withContext(ioDispatcher) {
        dao.upsertPlayerMeta(PlayerMetaEntity.fromDomain(profile))
    }

    suspend fun saveActiveRun(runState: RunState) = withContext(ioDispatcher) {
        dao.upsertSavedRun(SavedRunEntity(id = 1, runJson = serializeRunState(runState)))
    }

    suspend fun clearActiveRun() = withContext(ioDispatcher) {
        dao.clearSavedRun()
    }

    private fun serializeRunState(run: RunState): String {
        return runStateAdapter.toJson(run)
    }

    private fun deserializeRunState(raw: String): RunState? {
        return try {
            runStateAdapter.fromJson(raw)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: GameRepository? = null

        fun getInstance(context: Context): GameRepository {
            return INSTANCE ?: synchronized(this) {
                val db = GameDatabase.getDatabase(context)
                val instance = GameRepository(db.gameDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
