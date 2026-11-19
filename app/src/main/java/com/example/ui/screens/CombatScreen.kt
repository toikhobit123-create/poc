package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.CombatPhase
import com.example.engine.CombatState
import com.example.engine.PriorityHolder
import com.example.model.HandCardInstance
import com.example.model.UnitOnBoard
import com.example.ui.components.CardTile
import com.example.ui.components.UnitToken
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.CrimsonNexus
import com.example.ui.theme.EmeraldHeal
import com.example.ui.theme.EpicViolet
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.LegendaryAmber
import com.example.ui.theme.ManaBlue
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ParchmentWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SpellManaCyan

@Composable
fun CombatScreen(
    combat: CombatState,
    revivesRemaining: Int,
    onPlayCard: (String) -> Unit,
    onEnterAttack: () -> Unit,
    onCancelAttack: () -> Unit,
    onToggleAttacker: (String) -> Unit,
    onCommitAttack: () -> Unit,
    onSelectEnemyAttackerForBlock: (String?) -> Unit,
    onAssignBlocker: (String, String) -> Unit,
    onRemoveBlocker: (String) -> Unit,
    onCommitBlock: () -> Unit,
    onResolveStack: () -> Unit,
    onPassTurn: () -> Unit,
    onVictoryContinue: () -> Unit,
    onUseRevive: () -> Unit,
    onAcceptDefeat: () -> Unit,
    onAbandonRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inspectedHandCard by remember { mutableStateOf<HandCardInstance?>(null) }
    var inspectedUnit by remember { mutableStateOf<UnitOnBoard?>(null) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showForfeitConfirm by remember { mutableStateOf(false) }

    BackHandler { showForfeitConfirm = true }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. ENEMY HUD + BOARD
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EnemyCombatHud(
                    combat = combat,
                    onShowLog = { showLogDialog = true }
                )

                BoardLane(
                    label = "FOE BATTLEFIELD (${combat.enemyBoard.size}/6)",
                    emptyText = "Foe has no units on board",
                    units = combat.enemyBoard,
                    isEnemy = true,
                    combat = combat,
                    onUnitClick = { unit ->
                        if (combat.priorityHolder == PriorityHolder.PLAYER &&
                            combat.phase == CombatPhase.COMBAT_DECLARE_BLOCK_PHASE &&
                            combat.attackingUnitIds.contains(unit.instanceId)
                        ) {
                            if (combat.blockPairings.containsKey(unit.instanceId)) {
                                onRemoveBlocker(unit.instanceId)
                            } else {
                                onSelectEnemyAttackerForBlock(unit.instanceId)
                            }
                        } else {
                            inspectedUnit = unit
                        }
                    }
                )
            }

            // 2. CENTER COMBAT LANE / SPELL STACK / ACTION BAR
            CombatActionStrip(
                combat = combat,
                onEnterAttack = onEnterAttack,
                onCancelAttack = onCancelAttack,
                onCommitAttack = onCommitAttack,
                onCommitBlock = onCommitBlock,
                onResolveStack = onResolveStack,
                onPassTurn = onPassTurn
            )

            // 3. PLAYER BOARD + MANA + HAND
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BoardLane(
                    label = "YOUR BATTLEFIELD (${combat.playerBoard.size}/6)",
                    emptyText = "Play followers or your Champion from hand",
                    units = combat.playerBoard,
                    isEnemy = false,
                    combat = combat,
                    onUnitClick = { unit ->
                        if (combat.priorityHolder == PriorityHolder.PLAYER) {
                            when (combat.phase) {
                                CombatPhase.COMBAT_DECLARE_ATTACK_PHASE -> onToggleAttacker(unit.instanceId)
                                CombatPhase.COMBAT_DECLARE_BLOCK_PHASE -> {
                                    val targetEnemy = combat.selectedEnemyAttackerForBlock
                                        ?: combat.attackingUnitIds.firstOrNull { !combat.blockPairings.containsKey(it) }
                                    if (targetEnemy != null) {
                                        onAssignBlocker(targetEnemy, unit.instanceId)
                                    } else {
                                        inspectedUnit = unit
                                    }
                                }
                                else -> inspectedUnit = unit
                            }
                        } else {
                            inspectedUnit = unit
                        }
                    }
                )

                ManaAndTokenBar(combat = combat)

                // Player Hand
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(combat.playerHand, key = { it.instanceId }) { handCard ->
                        val canPlay = combat.canPlayCard(handCard)
                        val cost = combat.effectivePlayCost(handCard)
                        CardTile(
                            template = handCard.template,
                            effectiveCost = cost,
                            bonusPower = handCard.bonusPower,
                            bonusHealth = handCard.bonusHealth,
                            attachedItems = handCard.attachedItems,
                            extraKeywords = handCard.extraKeywords,
                            isFleeting = handCard.isFleeting,
                            playable = canPlay,
                            compact = true,
                            testTag = "hand_card_${handCard.template.id}",
                            onClick = { inspectedHandCard = handCard },
                            modifier = Modifier.width(156.dp)
                        )
                    }
                }
            }
        }
    }

    // Hand Card Quick Play / Inspect Modal
    if (inspectedHandCard != null) {
        val card = inspectedHandCard!!
        val canPlay = combat.canPlayCard(card)
        val cost = combat.effectivePlayCost(card)
        AlertDialog(
            onDismissRequest = { inspectedHandCard = null },
            containerColor = ObsidianSurface,
            title = {
                Text(card.template.name, color = HextechGoldBright)
            },
            text = {
                CardTile(
                    template = card.template,
                    effectiveCost = cost,
                    bonusPower = card.bonusPower,
                    bonusHealth = card.bonusHealth,
                    attachedItems = card.attachedItems,
                    extraKeywords = card.extraKeywords,
                    isFleeting = card.isFleeting,
                    playable = canPlay
                )
            },
            confirmButton = {
                Button(
                    enabled = canPlay,
                    onClick = {
                        onPlayCard(card.instanceId)
                        inspectedHandCard = null
                    },
                    modifier = Modifier.testTag("confirm_play_card_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canPlay) ArcaneCyan else SlateBorder,
                        contentColor = ObsidianBg
                    )
                ) {
                    Text(if (canPlay) "Play ($cost Mana)" else "Cannot Play Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectedHandCard = null }) {
                    Text("Close", color = MutedSilver)
                }
            }
        )
    }

    // Board Unit Inspector
    if (inspectedUnit != null) {
        val u = inspectedUnit!!
        AlertDialog(
            onDismissRequest = { inspectedUnit = null },
            containerColor = ObsidianSurface,
            title = { Text("${u.template.name} (${u.strikePower}|${u.health})", color = HextechGoldBright) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(u.template.description, color = ParchmentWhite, style = MaterialTheme.typography.bodyMedium)
                    if (u.keywords.isNotEmpty()) {
                        Text(
                            text = "Keywords: ${u.keywords.joinToString { it.displayName }}",
                            color = ArcaneCyan,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (u.attachedItems.isNotEmpty()) {
                        Text(
                            text = "Items: ${u.attachedItems.joinToString { it.name }}",
                            color = EpicViolet,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedUnit = null }) {
                    Text("OK", color = HextechGoldBright)
                }
            }
        )
    }

    // Combat Log Dialog
    if (showLogDialog) {
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            containerColor = ObsidianSurface,
            title = { Text("Combat Log — Round ${combat.round}", color = HextechGoldBright) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    combat.combatLog.takeLast(12).forEach { entry ->
                        Text("• $entry", style = MaterialTheme.typography.bodySmall, color = ParchmentWhite)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogDialog = false }) {
                    Text("Close", color = HextechGoldBright)
                }
            }
        )
    }

    // Forfeit Confirmation
    if (showForfeitConfirm) {
        AlertDialog(
            onDismissRequest = { showForfeitConfirm = false },
            containerColor = ObsidianSurface,
            title = { Text("Abandon Adventure?", color = CrimsonNexus) },
            text = { Text("Leaving mid-combat forfeits your current run.", color = ParchmentWhite) },
            confirmButton = {
                Button(
                    onClick = {
                        showForfeitConfirm = false
                        onAbandonRun()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonNexus)
                ) {
                    Text("Forfeit Run")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForfeitConfirm = false }) {
                    Text("Stay", color = ArcaneCyan)
                }
            }
        )
    }

    // Victory / Defeat / Revive Overlay Dialog
    if (combat.phase == CombatPhase.GAME_OVER) {
        val won = combat.playerWon == true
        AlertDialog(
            onDismissRequest = {},
            containerColor = ObsidianSurface,
            title = {
                Text(
                    text = if (won) "VICTORY!" else "NEXUS DESTROYED",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (won) HextechGoldBright else CrimsonNexus
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (won) {
                            "You defeated ${combat.encounterNode.enemyName} and claimed +${combat.encounterNode.goldReward} Gold!"
                        } else if (revivesRemaining > 0) {
                            "Your Nexus fell, but you have $revivesRemaining Revive Token! Restore 50% Nexus HP and retry this battle."
                        } else {
                            "Your Nexus fell with no Revive Tokens remaining."
                        },
                        color = ParchmentWhite,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                if (won) {
                    Button(
                        onClick = onVictoryContinue,
                        modifier = Modifier.testTag("combat_victory_continue_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = HextechGold, contentColor = ObsidianBg)
                    ) {
                        Text("Claim Rewards")
                    }
                } else if (revivesRemaining > 0) {
                    Button(
                        onClick = onUseRevive,
                        modifier = Modifier.testTag("use_revive_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldHeal, contentColor = ObsidianBg)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Revive (50% HP)")
                    }
                } else {
                    Button(
                        onClick = onAcceptDefeat,
                        modifier = Modifier.testTag("accept_defeat_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonNexus)
                    ) {
                        Text("End Run")
                    }
                }
            },
            dismissButton = {
                if (!won && revivesRemaining > 0) {
                    TextButton(onClick = onAcceptDefeat) {
                        Text("Accept Defeat", color = MutedSilver)
                    }
                }
            }
        )
    }
}

@Composable
private fun EnemyCombatHud(
    combat: CombatState,
    onShowLog: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, CrimsonNexus.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = combat.encounterNode.enemyName,
                        style = MaterialTheme.typography.titleMedium,
                        color = ParchmentWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Rnd ${combat.round} • Hand ${combat.enemyHandCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArcaneCyan,
                        modifier = Modifier.testTag("enemy_hand_count")
                    )
                }
                if (combat.encounterNode.enemyPowerTitle.isNotBlank()) {
                    Text(
                        text = "${combat.encounterNode.enemyPowerTitle}: ${combat.encounterNode.enemyPowerDescription}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CrimsonNexus.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, CrimsonNexus)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Foe HP", tint = CrimsonNexus, modifier = Modifier.size(14.dp))
                        Text(
                            text = "${combat.enemyNexusHp}/${combat.enemyMaxHp}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ParchmentWhite,
                            modifier = Modifier.testTag("enemy_nexus_hp")
                        )
                    }
                }
                IconButton(onClick = onShowLog, modifier = Modifier.testTag("combat_log_button")) {
                    Icon(Icons.Default.History, contentDescription = "Combat Log", tint = HextechGoldBright)
                }
            }
        }
    }
}

@Composable
private fun BoardLane(
    label: String,
    emptyText: String,
    units: List<UnitOnBoard>,
    isEnemy: Boolean,
    combat: CombatState,
    onUnitClick: (UnitOnBoard) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurface.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isEnemy) CrimsonNexus else ArcaneCyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (units.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emptyText, style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    units.forEach { unit ->
                        val isAttacking = combat.attackingUnitIds.contains(unit.instanceId)
                        val isSelected = if (isEnemy) {
                            combat.selectedEnemyAttackerForBlock == unit.instanceId
                        } else {
                            isAttacking
                        }
                        val blockLabel = if (!isEnemy && combat.phase == CombatPhase.COMBAT_DECLARE_BLOCK_PHASE) {
                            val pairedAtkId = combat.blockPairings.entries.find { it.value == unit.instanceId }?.key
                            pairedAtkId?.let { id ->
                                val enemyName = combat.enemyBoard.find { it.instanceId == id }?.template?.name ?: "Foe"
                                "BLK: $enemyName"
                            }
                        } else if (isEnemy && combat.phase == CombatPhase.COMBAT_DECLARE_BLOCK_PHASE && isAttacking) {
                            val blkId = combat.blockPairings[unit.instanceId]
                            if (blkId != null) "BLOCKED" else "UNBLOCKED"
                        } else null

                        UnitToken(
                            unit = unit,
                            isAttacking = isAttacking,
                            isBlockingLabel = blockLabel,
                            isSelected = isSelected,
                            testTag = "board_unit_${unit.instanceId}",
                            onClick = { onUnitClick(unit) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CombatActionStrip(
    combat: CombatState,
    onEnterAttack: () -> Unit,
    onCancelAttack: () -> Unit,
    onCommitAttack: () -> Unit,
    onCommitBlock: () -> Unit,
    onResolveStack: () -> Unit,
    onPassTurn: () -> Unit
) {
    val isPlayerPriority = combat.priorityHolder == PriorityHolder.PLAYER
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurfaceElevated,
        border = BorderStroke(
            1.dp,
            if (isPlayerPriority) HextechGold.copy(alpha = 0.5f) else CrimsonNexus.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            if (combat.spellStack.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Stack: ${combat.spellStack.joinToString(" -> ") { it.card.name }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = EpicViolet,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        enabled = isPlayerPriority,
                        onClick = onResolveStack,
                        modifier = Modifier.testTag("resolve_stack_button")
                    ) {
                        Text(
                            text = if (isPlayerPriority) "Resolve Stack" else "Foe Priority",
                            color = if (isPlayerPriority) ArcaneCyan else MutedSilver
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        !isPlayerPriority -> "Foe is thinking..."
                        combat.phase == CombatPhase.COMBAT_DECLARE_ATTACK_PHASE ->
                            "Tap allies to Attack (${combat.attackingUnitIds.size} selected)"
                        combat.phase == CombatPhase.COMBAT_DECLARE_BLOCK_PHASE ->
                            "Tap Foe then Ally to Block (${combat.blockPairings.size} paired)"
                        combat.phase == CombatPhase.SPELL_STACK_PHASE ->
                            "Spell Stack open — respond or Resolve"
                        else -> if (combat.playerHasAttackToken) "Your Initiative (Attack Token Ready)" else "Your Initiative"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPlayerPriority) ParchmentWhite else MutedSilver,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("combat_priority_status")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (combat.phase) {
                        CombatPhase.COMBAT_DECLARE_ATTACK_PHASE -> {
                            OutlinedButton(
                                enabled = isPlayerPriority,
                                onClick = onCancelAttack
                            ) {
                                Text("Cancel", color = MutedSilver)
                            }
                            Button(
                                enabled = isPlayerPriority && combat.attackingUnitIds.isNotEmpty(),
                                onClick = onCommitAttack,
                                modifier = Modifier.testTag("commit_attack_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = LegendaryAmber, contentColor = ObsidianBg)
                            ) {
                                Text("Strike!")
                            }
                        }
                        CombatPhase.COMBAT_DECLARE_BLOCK_PHASE -> {
                            Button(
                                enabled = isPlayerPriority,
                                onClick = onCommitBlock,
                                modifier = Modifier.testTag("commit_block_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ArcaneCyan, contentColor = ObsidianBg)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm Block (${combat.blockPairings.size})")
                            }
                        }
                        CombatPhase.SPELL_STACK_PHASE -> {
                            Button(
                                enabled = isPlayerPriority,
                                onClick = onPassTurn,
                                modifier = Modifier.testTag("end_round_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = HextechGold, contentColor = ObsidianBg)
                            ) {
                                Text(if (isPlayerPriority) "Resolve Stack" else "Wait...")
                            }
                        }
                        else -> {
                            if (combat.playerHasAttackToken && combat.playerBoard.any { it.canAttack }) {
                                Button(
                                    enabled = isPlayerPriority,
                                    onClick = onEnterAttack,
                                    modifier = Modifier.testTag("enter_attack_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = LegendaryAmber, contentColor = ObsidianBg)
                                ) {
                                    Icon(Icons.Default.SportsKabaddi, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Attack")
                                }
                            }
                            Button(
                                enabled = isPlayerPriority,
                                onClick = onPassTurn,
                                modifier = Modifier.testTag("end_round_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = HextechGold, contentColor = ObsidianBg)
                            ) {
                                Text(
                                    when {
                                        !isPlayerPriority -> "Foe Turn"
                                        combat.spellStack.isNotEmpty() -> "Resolve Stack"
                                        combat.consecutivePasses >= 1 -> "End Round"
                                        else -> "Pass"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManaAndTokenBar(combat: CombatState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = EmeraldHeal.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, EmeraldHeal.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = "Your Nexus HP", tint = EmeraldHeal, modifier = Modifier.size(15.dp))
                Text(
                    text = "${combat.playerNexusHp}/${combat.playerMaxHp}",
                    style = MaterialTheme.typography.labelLarge,
                    color = ParchmentWhite,
                    modifier = Modifier.testTag("player_nexus_hp")
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ManaBlue.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, ManaBlue)
            ) {
                Text(
                    text = "Mana ${combat.playerMana}/${combat.playerMaxMana}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ParchmentWhite,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SpellManaCyan.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, SpellManaCyan)
            ) {
                Text(
                    text = "Spell ${combat.playerSpellMana}/3",
                    style = MaterialTheme.typography.labelMedium,
                    color = ArcaneCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            if (combat.playerHasAttackToken) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(LegendaryAmber.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = "Attack Token", tint = LegendaryAmber, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
