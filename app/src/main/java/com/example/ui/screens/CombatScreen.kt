package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.engine.CombatPhase
import com.example.engine.CombatState
import com.example.engine.PriorityHolder
import com.example.model.CardType
import com.example.model.HandCardInstance
import com.example.model.UnitOnBoard
import com.example.ui.components.HandCardView
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
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ParchmentWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SpellManaCyan
import kotlin.math.roundToInt

private sealed interface ActiveDragState {
    val pointerInRoot: Offset

    data class HandCardDrag(
        val card: HandCardInstance,
        override val pointerInRoot: Offset
    ) : ActiveDragState

    data class BenchUnitDrag(
        val unit: UnitOnBoard,
        override val pointerInRoot: Offset
    ) : ActiveDragState

    data class CombatUnitDrag(
        val unit: UnitOnBoard,
        override val pointerInRoot: Offset
    ) : ActiveDragState
}

@Composable
fun CombatScreen(
    combat: CombatState,
    revivesRemaining: Int,
    onPlayCard: (String) -> Unit,
    onEnterAttack: () -> Unit,
    onCancelAttack: () -> Unit,
    onToggleAttacker: (String) -> Unit,
    onDragBenchUnitToAttack: (String) -> Unit = {},
    onRemoveAttacker: (String) -> Unit = {},
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
    var inspectHandDetailOnly by remember { mutableStateOf(false) }
    var inspectedUnit by remember { mutableStateOf<UnitOnBoard?>(null) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showPowerInfo by remember { mutableStateOf(false) }
    var showGraveyardDialog by remember { mutableStateOf(false) }
    var showMatchInfoDialog by remember { mutableStateOf(false) }
    var showEmotePicker by remember { mutableStateOf(false) }
    var activeEmote by remember { mutableStateOf<CombatEmoteOption?>(null) }
    var showForfeitConfirm by remember { mutableStateOf(false) }

    var activeDrag by remember { mutableStateOf<ActiveDragState?>(null) }
    var dragWarningText by remember { mutableStateOf<String?>(null) }

    // Auto-clear emote bubble and drag warning
    LaunchedEffect(activeEmote) {
        if (activeEmote != null) {
            kotlinx.coroutines.delay(1800)
            activeEmote = null
        }
    }
    LaunchedEffect(dragWarningText) {
        if (dragWarningText != null) {
            kotlinx.coroutines.delay(1000)
            dragWarningText = null
        }
    }

    // Track Nexus HP deltas for hit reaction
    var prevPlayerHp by remember { mutableIntStateOf(combat.playerNexusHp) }
    var playerNexusDelta by remember { mutableIntStateOf(0) }
    LaunchedEffect(combat.playerNexusHp) {
        if (combat.playerNexusHp < prevPlayerHp) {
            playerNexusDelta = prevPlayerHp - combat.playerNexusHp
            prevPlayerHp = combat.playerNexusHp
            kotlinx.coroutines.delay(650)
            playerNexusDelta = 0
        } else {
            prevPlayerHp = combat.playerNexusHp
        }
    }

    var prevEnemyHp by remember { mutableIntStateOf(combat.enemyNexusHp) }
    var enemyNexusDelta by remember { mutableIntStateOf(0) }
    LaunchedEffect(combat.enemyNexusHp) {
        if (combat.enemyNexusHp < prevEnemyHp) {
            enemyNexusDelta = prevEnemyHp - combat.enemyNexusHp
            prevEnemyHp = combat.enemyNexusHp
            kotlinx.coroutines.delay(650)
            enemyNexusDelta = 0
        } else {
            prevEnemyHp = combat.enemyNexusHp
        }
    }

    // Drop target bounds in root coordinates
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var playerBenchBounds by remember { mutableStateOf(Rect.Zero) }
    var sharedBattlefieldBounds by remember { mutableStateOf(Rect.Zero) }
    val enemyAttackerBounds = remember { mutableStateMapOf<String, Rect>() }

    BackHandler {
        when {
            activeDrag != null -> activeDrag = null
            showEmotePicker -> showEmotePicker = false
            showGraveyardDialog -> showGraveyardDialog = false
            showMatchInfoDialog -> showMatchInfoDialog = false
            inspectedHandCard != null -> inspectedHandCard = null
            inspectedUnit != null -> inspectedUnit = null
            showPowerInfo -> showPowerInfo = false
            else -> showForfeitConfirm = true
        }
    }

    val latestCombat by rememberUpdatedState(combat)

    val handleHandCardDrop: (HandCardInstance, Offset) -> Unit = { card, pointer ->
        val c = latestCombat
        val isUnitLike = card.template.type == CardType.UNIT ||
            card.template.type == CardType.CHAMPION ||
            card.template.type == CardType.LANDMARK
        val validZone = if (isUnitLike) {
            playerBenchBounds.inflate(24f).contains(pointer)
        } else {
            sharedBattlefieldBounds.inflate(24f).contains(pointer)
        }
        if (!c.canPlayCard(card)) {
            dragWarningText = if (c.priorityHolder != PriorityHolder.PLAYER) "Wait for Foe's turn"
            else if (isUnitLike && c.playerBoard.size >= 6) "Player Bench full (6/6)"
            else "Cannot play ${card.template.name}"
        } else if (validZone) {
            onPlayCard(card.instanceId)
            inspectedHandCard = null
        } else {
            dragWarningText = if (isUnitLike) "Drop on Player Bench to summon"
            else "Drop on Battlefield to cast"
        }
        activeDrag = null
    }

    val handleBenchUnitDrop: (UnitOnBoard, Offset) -> Unit = { unit, pointer ->
        val c = latestCombat
        val inField = sharedBattlefieldBounds.inflate(20f).contains(pointer)
        if (c.priorityHolder != PriorityHolder.PLAYER) {
            dragWarningText = "Wait for your turn"
        } else if (c.phase == CombatPhase.COMBAT_DECLARE_BLOCK_PHASE && !c.playerInitiatedCombat) {
            if (!unit.canBlock) {
                dragWarningText = "${unit.template.name} cannot block"
            } else {
                val hitAttackerId = enemyAttackerBounds.entries
                    .firstOrNull { it.value.inflate(28f).contains(pointer) }?.key
                    ?: if (inField) {
                        c.selectedEnemyAttackerForBlock
                            ?: c.attackingUnitIds.firstOrNull { atkId ->
                                val atk = c.enemyBoard.find { it.instanceId == atkId }
                                !c.blockPairings.containsKey(atkId) && atk != null && unit.canBlockAttacker(atk)
                            }
                    } else null
                val targetEnemy = hitAttackerId?.let { id -> c.enemyBoard.find { it.instanceId == id } }
                if (targetEnemy != null && unit.canBlockAttacker(targetEnemy)) {
                    onAssignBlocker(targetEnemy.instanceId, unit.instanceId)
                } else if (inField) {
                    dragWarningText = "Cannot block selected attacker"
                }
            }
        } else if ((c.phase == CombatPhase.MAIN_ACTION_PHASE && c.playerHasAttackToken) ||
            c.phase == CombatPhase.COMBAT_DECLARE_ATTACK_PHASE
        ) {
            if (!unit.canAttack) {
                dragWarningText = "${unit.template.name} cannot attack"
            } else if (inField) {
                onDragBenchUnitToAttack(unit.instanceId)
            } else {
                dragWarningText = "Drag onto Shared Battlefield to attack"
            }
        }
        activeDrag = null
    }

    val handleCombatUnitDrop: (UnitOnBoard, Offset) -> Unit = { unit, pointer ->
        val c = latestCombat
        val backToBench = playerBenchBounds.inflate(24f).contains(pointer)
        if (backToBench && c.priorityHolder == PriorityHolder.PLAYER) {
            when (c.phase) {
                CombatPhase.COMBAT_DECLARE_ATTACK_PHASE -> onRemoveAttacker(unit.instanceId)
                CombatPhase.COMBAT_DECLARE_BLOCK_PHASE -> {
                    val attackerId = c.blockPairings.entries.firstOrNull { it.value == unit.instanceId }?.key
                    if (attackerId != null) onRemoveBlocker(attackerId)
                }
                else -> {}
            }
        }
        activeDrag = null
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOrigin = it.positionInRoot() }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1222),
                        ObsidianBg,
                        Color(0xFF0E1728)
                    )
                )
            )
            .clipToBounds()
    ) {
        val vw = { percent: Float -> maxWidth * (percent / 100f) }
        val vh = { percent: Float -> maxHeight * (percent / 100f) }

        val descFontSize = (maxHeight.value * 0.018f).coerceIn(10f, 14f).sp
        val statFontSize = (maxHeight.value * 0.024f).coerceIn(12f, 18f).sp

        // Board token sizing: max 10.2vw wide, ~4:5 near-square ratio (~17vh tall)
        val unitTileWidth = minOf(vw(10.2f), vh(16.5f) * 0.8f)
        val unitTileHeight = minOf(vh(16.5f), unitTileWidth * 1.25f)
        val unitGap = vw(1.0f)

        // Hand card sizing: 2:3 vertical ratio (11vh x 16.5vh)
        val handCardHeight = vh(16.5f)
        val handCardWidth = handCardHeight * (2f / 3f)

        val onEnemyUnitClick: (UnitOnBoard) -> Unit = { unit ->
            inspectedHandCard = null
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

        val onPlayerUnitClick: (UnitOnBoard) -> Unit = { unit ->
            inspectedHandCard = null
            if (combat.priorityHolder == PriorityHolder.PLAYER) {
                when (combat.phase) {
                    CombatPhase.COMBAT_DECLARE_ATTACK_PHASE -> onToggleAttacker(unit.instanceId)
                    CombatPhase.COMBAT_DECLARE_BLOCK_PHASE -> {
                        val targetEnemy = combat.selectedEnemyAttackerForBlock
                            ?: combat.attackingUnitIds.firstOrNull { atkId ->
                                val atk = combat.enemyBoard.find { it.instanceId == atkId }
                                !combat.blockPairings.containsKey(atkId) && atk != null && unit.canBlockAttacker(atk)
                            }
                        if (targetEnemy != null) {
                            onAssignBlocker(targetEnemy, unit.instanceId)
                        } else {
                            inspectedUnit = unit
                        }
                    }
                    // Spec II.3 / VIII: Tap bench unit in main phase to view unit info
                    else -> inspectedUnit = unit
                }
            } else {
                inspectedUnit = unit
            }
        }

        // Compute zone drop feedback states
        val currentDrag = activeDrag
        val playerBenchHighlight = when (currentDrag) {
            is ActiveDragState.HandCardDrag -> {
                val c = currentDrag.card
                val isUnit = c.template.type == CardType.UNIT || c.template.type == CardType.CHAMPION || c.template.type == CardType.LANDMARK
                if (isUnit) {
                    if (combat.canPlayCard(c)) DropHighlight.VALID else DropHighlight.INVALID
                } else DropHighlight.NONE
            }
            is ActiveDragState.CombatUnitDrag -> DropHighlight.VALID
            else -> DropHighlight.NONE
        }
        val battlefieldHighlight = when (currentDrag) {
            is ActiveDragState.HandCardDrag -> {
                val c = currentDrag.card
                val isSpellOrGear = c.template.type == CardType.SPELL || c.template.type == CardType.EQUIPMENT || c.template.type == CardType.CURSE
                if (isSpellOrGear) {
                    if (combat.canPlayCard(c)) DropHighlight.VALID else DropHighlight.INVALID
                } else DropHighlight.NONE
            }
            is ActiveDragState.BenchUnitDrag -> {
                val canMoveToField = combat.priorityHolder == PriorityHolder.PLAYER && when (combat.phase) {
                    CombatPhase.MAIN_ACTION_PHASE -> combat.playerHasAttackToken && currentDrag.unit.canAttack
                    CombatPhase.COMBAT_DECLARE_ATTACK_PHASE -> currentDrag.unit.canAttack
                    CombatPhase.COMBAT_DECLARE_BLOCK_PHASE -> !combat.playerInitiatedCombat && currentDrag.unit.canBlock
                    else -> false
                }
                if (canMoveToField) DropHighlight.VALID else DropHighlight.INVALID
            }
            else -> DropHighlight.NONE
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT RAIL (14.5vw, includes 3vw safe left margin): AI Nexus + Log/Info + Spell Stack + Graveyard/Emote + Player Nexus
            LeftHudRail(
                combat = combat,
                railWidth = vw(14.5f),
                safeStartPadding = vw(3f),
                laneGap = vh(0.8f),
                descFontSize = descFontSize,
                statFontSize = statFontSize,
                enemyNexusDelta = enemyNexusDelta,
                playerNexusDelta = playerNexusDelta,
                activeEmote = activeEmote,
                onShowLog = { showLogDialog = true },
                onShowPower = { showPowerInfo = true },
                onShowGraveyard = { showGraveyardDialog = true },
                onShowMatchInfo = { showMatchInfoDialog = true },
                onToggleEmote = { showEmotePicker = !showEmotePicker }
            )

            // CENTER BATTLEFIELD (71vw, 5 proportional vertical bands summing to 100vh)
            Column(
                modifier = Modifier
                    .width(vw(71f))
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP: Enemy Hand & Header (11vh)
                EnemyTopHandZone(
                    combat = combat,
                    zoneHeight = vh(11f),
                    cardBackWidth = vh(9f),
                    cardBackHeight = vh(13.5f),
                    descFontSize = descFontSize,
                    onShowPower = { showPowerInfo = true }
                )

                // ZONE 1: AI BENCH (19vh, max 6 slots)
                val enemyCombatIds = if (combat.phase == CombatPhase.COMBAT_DECLARE_BLOCK_PHASE && !combat.playerInitiatedCombat) {
                    combat.attackingUnitIds.toSet()
                } else emptySet()
                val enemyBenchUnits = combat.enemyBoard.filterNot { enemyCombatIds.contains(it.instanceId) }

                BenchLaneZone(
                    units = enemyBenchUnits,
                    isEnemy = true,
                    combat = combat,
                    zoneHeight = vh(19f),
                    tileWidth = unitTileWidth,
                    tileHeight = unitTileHeight,
                    unitGap = unitGap,
                    statFontSize = statFontSize,
                    descFontSize = descFontSize,
                    zoneLabel = "AI BENCH (${enemyBenchUnits.size}/6)",
                    emptyHint = if (enemyCombatIds.isNotEmpty()) "Foe attackers in Shared Battlefield" else "AI Bench Empty (0/6)",
                    dropHighlight = DropHighlight.NONE,
                    draggingUnitId = null,
                    zoneTestTag = "enemy_bench_zone",
                    onUnitClick = onEnemyUnitClick,
                    onUnitLongClick = { inspectedUnit = it },
                    onUnitDragStart = null,
                    onUnitDrag = null,
                    onUnitDragEnd = null
                )

                // ZONE 2: SHARED BATTLEFIELD (26vh) — Top: AI Combat Row, Center: Clash Axis, Bottom: Player Combat Row
                CenterCombatZone(
                    combat = combat,
                    zoneHeight = vh(26f),
                    tileWidth = unitTileWidth,
                    tileHeight = unitTileHeight,
                    unitGap = unitGap,
                    statFontSize = statFontSize,
                    descFontSize = descFontSize,
                    dropHighlight = battlefieldHighlight,
                    draggingUnitId = (currentDrag as? ActiveDragState.CombatUnitDrag)?.unit?.instanceId,
                    isDraggingBenchBlocker = currentDrag is ActiveDragState.BenchUnitDrag &&
                        combat.phase == CombatPhase.COMBAT_DECLARE_BLOCK_PHASE &&
                        !combat.playerInitiatedCombat,
                    draggedBenchUnit = (currentDrag as? ActiveDragState.BenchUnitDrag)?.unit,
                    onBoundsChanged = { sharedBattlefieldBounds = it },
                    onEnemyAttackerBoundsChanged = { id, rect -> enemyAttackerBounds[id] = rect },
                    onPlayerUnitClick = onPlayerUnitClick,
                    onEnemyUnitClick = onEnemyUnitClick,
                    onUnitLongClick = { inspectedUnit = it },
                    onCombatUnitDragStart = { unit, ptr -> activeDrag = ActiveDragState.CombatUnitDrag(unit, ptr) },
                    onCombatUnitDrag = { unit, ptr -> activeDrag = ActiveDragState.CombatUnitDrag(unit, ptr) },
                    onCombatUnitDragEnd = { unit, ptr -> handleCombatUnitDrop(unit, ptr) },
                    onResolveStack = onResolveStack
                )

                // ZONE 3: PLAYER BENCH (19vh, max 6 slots)
                val playerAttackingIds = if (combat.phase == CombatPhase.COMBAT_DECLARE_ATTACK_PHASE ||
                    (combat.phase == CombatPhase.COMBAT_DECLARE_BLOCK_PHASE && combat.playerInitiatedCombat)
                ) {
                    combat.attackingUnitIds.toSet()
                } else emptySet()
                val playerBlockingIds = if (combat.phase == CombatPhase.COMBAT_DECLARE_BLOCK_PHASE && !combat.playerInitiatedCombat) {
                    combat.blockPairings.values.toSet()
                } else emptySet()
                val playerBenchUnits = combat.playerBoard.filterNot {
                    playerAttackingIds.contains(it.instanceId) || playerBlockingIds.contains(it.instanceId)
                }

                BenchLaneZone(
                    units = playerBenchUnits,
                    isEnemy = false,
                    combat = combat,
                    zoneHeight = vh(19f),
                    tileWidth = unitTileWidth,
                    tileHeight = unitTileHeight,
                    unitGap = unitGap,
                    statFontSize = statFontSize,
                    descFontSize = descFontSize,
                    zoneLabel = "PLAYER BENCH (${playerBenchUnits.size}/6)",
                    emptyHint = if (combat.playerBoard.isEmpty()) "Play or drag units here to summon" else "All allies in Shared Battlefield",
                    dropHighlight = playerBenchHighlight,
                    draggingUnitId = (currentDrag as? ActiveDragState.BenchUnitDrag)?.unit?.instanceId,
                    zoneTestTag = "player_bench_zone",
                    modifier = Modifier.onGloballyPositioned { playerBenchBounds = it.boundsInRoot() },
                    onUnitClick = onPlayerUnitClick,
                    onUnitLongClick = { inspectedUnit = it },
                    onUnitDragStart = { unit, ptr ->
                        inspectedHandCard = null
                        activeDrag = ActiveDragState.BenchUnitDrag(unit, ptr)
                    },
                    onUnitDrag = { unit, ptr -> activeDrag = ActiveDragState.BenchUnitDrag(unit, ptr) },
                    onUnitDragEnd = { unit, ptr -> handleBenchUnitDrop(unit, ptr) }
                )

                // BOTTOM: PLAYER HAND (21vh, fanned/overlapping, 1.5x lift on tap, drag-and-drop)
                PlayerHandZone(
                    combat = combat,
                    focusedCardId = inspectedHandCard?.instanceId,
                    draggingCardId = (currentDrag as? ActiveDragState.HandCardDrag)?.card?.instanceId,
                    zoneHeight = vh(21f),
                    zoneWidth = vw(71f),
                    cardWidth = handCardWidth,
                    cardHeight = handCardHeight,
                    descFontSize = descFontSize,
                    statFontSize = statFontSize,
                    onCardTap = { tapped ->
                        inspectedUnit = null
                        inspectHandDetailOnly = false
                        inspectedHandCard = if (inspectedHandCard?.instanceId == tapped.instanceId) null else tapped
                    },
                    onCardLongPress = { held ->
                        inspectedUnit = null
                        inspectHandDetailOnly = true
                        inspectedHandCard = held
                    },
                    onCardDragStart = { card, ptr ->
                        inspectedHandCard = null
                        activeDrag = ActiveDragState.HandCardDrag(card, ptr)
                    },
                    onCardDrag = { card, ptr -> activeDrag = ActiveDragState.HandCardDrag(card, ptr) },
                    onCardDragEnd = { card, ptr -> handleHandCardDrop(card, ptr) }
                )
            }

            // RIGHT RAIL (14.5vw, includes 3vw safe right margin): Round/Token + Pulsing Turn Action Button (45vh-55vh) + Segmented Mana Bar & Gems
            RightActionRail(
                combat = combat,
                railWidth = vw(14.5f),
                safeEndPadding = vw(3f),
                actionDiameter = maxOf(48.dp, vh(12f)),
                manaDiameter = maxOf(44.dp, vh(10f)),
                descFontSize = descFontSize,
                statFontSize = statFontSize,
                onEnterAttack = onEnterAttack,
                onCancelAttack = onCancelAttack,
                onCommitAttack = onCommitAttack,
                onCommitBlock = onCommitBlock,
                onPassTurn = onPassTurn
            )
        }

        // Drag Warning Toast (e.g. "Drop on Player Bench to summon" / "Not enough mana")
        AnimatedVisibility(
            visible = dragWarningText != null,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(220)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = vh(22f))
                .zIndex(80f)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CrimsonNexus.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, ParchmentWhite)
            ) {
                Text(
                    text = dragWarningText ?: "",
                    fontSize = descFontSize,
                    fontWeight = FontWeight.Bold,
                    color = ParchmentWhite,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Floating Drag Preview Overlay (tracks finger smoothly above all 3 zones)
        if (currentDrag != null) {
            val localOffset = currentDrag.pointerInRoot - rootOrigin
            when (currentDrag) {
                is ActiveDragState.HandCardDrag -> {
                    val card = currentDrag.card
                    Box(
                        modifier = Modifier
                            .zIndex(95f)
                            .offset {
                                IntOffset(
                                    (localOffset.x - handCardWidth.toPx() / 2f).roundToInt(),
                                    (localOffset.y - handCardHeight.toPx() * 0.7f).roundToInt()
                                )
                            }
                    ) {
                        HandCardView(
                            handCard = card,
                            effectiveCost = combat.effectivePlayCost(card),
                            playable = combat.canPlayCard(card),
                            isFocused = true,
                            cardWidth = handCardWidth,
                            cardHeight = handCardHeight,
                            descFontSize = descFontSize,
                            statFontSize = statFontSize,
                            onClick = {}
                        )
                    }
                }
                is ActiveDragState.BenchUnitDrag -> {
                    val unit = currentDrag.unit
                    Box(
                        modifier = Modifier
                            .zIndex(95f)
                            .offset {
                                IntOffset(
                                    (localOffset.x - unitTileWidth.toPx() / 2f).roundToInt(),
                                    (localOffset.y - unitTileHeight.toPx() * 0.65f).roundToInt()
                                )
                            }
                    ) {
                        UnitToken(
                            unit = unit,
                            isSelected = true,
                            tileWidth = unitTileWidth * 1.08f,
                            tileHeight = unitTileHeight * 1.08f,
                            statFontSize = statFontSize,
                            onClick = {}
                        )
                    }
                }
                is ActiveDragState.CombatUnitDrag -> {
                    val unit = currentDrag.unit
                    Box(
                        modifier = Modifier
                            .zIndex(95f)
                            .offset {
                                IntOffset(
                                    (localOffset.x - unitTileWidth.toPx() / 2f).roundToInt(),
                                    (localOffset.y - unitTileHeight.toPx() * 0.65f).roundToInt()
                                )
                            }
                    ) {
                        UnitToken(
                            unit = unit,
                            isSelected = true,
                            tileWidth = unitTileWidth * 1.05f,
                            tileHeight = unitTileHeight * 1.05f,
                            statFontSize = statFontSize,
                            onClick = {}
                        )
                    }
                }
            }
        }

        // Hand Card Inspect / Quick-Play Overlay
        if (inspectedHandCard != null) {
            val card = inspectedHandCard!!
            HandCardInspectOverlay(
                card = card,
                canPlay = combat.canPlayCard(card),
                effectiveCost = combat.effectivePlayCost(card),
                detailOnly = inspectHandDetailOnly,
                minWidth = vw(28f),
                maxWidth = vw(40f),
                buttonHeight = maxOf(48.dp, vh(7f)),
                offsetY = -vh(8f),
                padH = vw(1.2f),
                padV = vh(1f),
                descFontSize = descFontSize,
                onPlay = {
                    onPlayCard(card.instanceId)
                    inspectedHandCard = null
                },
                onDismiss = { inspectedHandCard = null }
            )
        }

        // Board Unit Inspect Side Popup
        if (inspectedUnit != null) {
            UnitInspectOverlay(
                unit = inspectedUnit!!,
                sidePadding = vw(16f),
                minWidth = vw(26f),
                maxWidth = vw(36f),
                padH = vw(1.2f),
                padV = vh(0.8f),
                descFontSize = descFontSize,
                onDismiss = { inspectedUnit = null }
            )
        }

        // Emote Picker Overlay (anchored bottom-left, never obscures battlefield)
        if (showEmotePicker) {
            EmotePickerOverlay(
                safeStartPadding = vw(3.5f),
                bottomPadding = vh(12f),
                descFontSize = descFontSize,
                onSelectEmote = { emote ->
                    activeEmote = emote
                    showEmotePicker = false
                },
                onDismiss = { showEmotePicker = false }
            )
        }
    }

    if (showPowerInfo) {
        EnemyPowerDialog(combat = combat, onDismiss = { showPowerInfo = false })
    }
    if (showLogDialog) {
        CombatLogDialog(combat = combat, onDismiss = { showLogDialog = false })
    }
    if (showGraveyardDialog) {
        GraveyardDialog(combat = combat, onDismiss = { showGraveyardDialog = false })
    }
    if (showMatchInfoDialog) {
        MatchInfoDialog(combat = combat, onDismiss = { showMatchInfoDialog = false })
    }
    if (showForfeitConfirm) {
        ForfeitConfirmDialog(
            onConfirmForfeit = {
                showForfeitConfirm = false
                onAbandonRun()
            },
            onDismiss = { showForfeitConfirm = false }
        )
    }
    CombatGameOverDialog(
        combat = combat,
        revivesRemaining = revivesRemaining,
        onVictoryContinue = onVictoryContinue,
        onUseRevive = onUseRevive,
        onAcceptDefeat = onAcceptDefeat
    )
}

private enum class DropHighlight {
    NONE,
    VALID,
    INVALID
}

@Composable
private fun LeftHudRail(
    combat: CombatState,
    railWidth: Dp,
    safeStartPadding: Dp,
    laneGap: Dp,
    descFontSize: TextUnit,
    statFontSize: TextUnit,
    enemyNexusDelta: Int,
    playerNexusDelta: Int,
    activeEmote: CombatEmoteOption?,
    onShowLog: () -> Unit,
    onShowPower: () -> Unit,
    onShowGraveyard: () -> Unit,
    onShowMatchInfo: () -> Unit,
    onToggleEmote: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(railWidth)
            .fillMaxHeight()
            .padding(start = safeStartPadding, end = railWidth * 0.05f, top = laneGap, bottom = laneGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP: AI Nexus + Hand Count + Log & Match Info Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(laneGap * 0.4f)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onShowPower),
                    shape = RoundedCornerShape(10.dp),
                    color = if (enemyNexusDelta > 0) CrimsonNexus.copy(alpha = 0.42f) else CrimsonNexus.copy(alpha = 0.18f),
                    border = BorderStroke(if (enemyNexusDelta > 0) 2.2.dp else 1.5.dp, CrimsonNexus)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = laneGap * 0.45f, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Foe Nexus HP", tint = CrimsonNexus, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${combat.enemyNexusHp}/${combat.enemyMaxHp}",
                                fontSize = statFontSize,
                                fontWeight = FontWeight.Bold,
                                color = ParchmentWhite,
                                maxLines = 1,
                                modifier = Modifier.testTag("enemy_nexus_hp")
                            )
                        }
                        Text(
                            text = combat.encounterNode.enemyName,
                            fontSize = (descFontSize.value * 0.85f).coerceAtLeast(9f).sp,
                            color = MutedSilver,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (enemyNexusDelta > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        shape = RoundedCornerShape(6.dp),
                        color = CrimsonNexus,
                        border = BorderStroke(1.dp, ParchmentWhite)
                    ) {
                        Text(
                            text = "-$enemyNexusDelta",
                            fontSize = descFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = ParchmentWhite,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "H:${combat.enemyHandCount}",
                    fontSize = descFontSize,
                    color = ArcaneCyan,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("enemy_hand_count")
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShowMatchInfo,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("match_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Match Info",
                            tint = ArcaneCyan,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onShowLog,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("combat_log_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Combat Log",
                            tint = HextechGoldBright,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        // MIDDLE: Spell Stack Column Display
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = laneGap * 0.4f),
            shape = RoundedCornerShape(10.dp),
            color = if (combat.spellStack.isNotEmpty()) EpicViolet.copy(alpha = 0.16f) else ObsidianSurface.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, if (combat.spellStack.isNotEmpty()) EpicViolet else SlateBorder.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (combat.spellStack.isEmpty()) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Spell Stack Empty",
                        tint = SlateBorder,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "STACK",
                        fontSize = (descFontSize.value * 0.78f).coerceAtLeast(9f).sp,
                        color = MutedSilver.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        text = "STACK (${combat.spellStack.size})",
                        fontSize = (descFontSize.value * 0.78f).coerceAtLeast(9f).sp,
                        fontWeight = FontWeight.Bold,
                        color = EpicViolet
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    combat.spellStack.reversed().take(3).forEach { entry ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = if (entry.sourceIsPlayer) ArcaneCyan.copy(alpha = 0.25f) else CrimsonNexus.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, if (entry.sourceIsPlayer) ArcaneCyan else CrimsonNexus)
                        ) {
                            Text(
                                text = entry.card.name,
                                fontSize = (descFontSize.value * 0.76f).coerceAtLeast(8.5f).sp,
                                color = ParchmentWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // BOTTOM: Deck + Graveyard + Emote + Player Nexus
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(laneGap * 0.4f)
        ) {
            AnimatedVisibility(
                visible = activeEmote != null,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(250))
            ) {
                activeEmote?.let { emote ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HextechGold.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, ParchmentWhite)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(emote.icon, contentDescription = emote.label, tint = ObsidianBg, modifier = Modifier.size(12.dp))
                            Text(
                                text = emote.label,
                                fontSize = (descFontSize.value * 0.8f).coerceAtLeast(8.5f).sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(Icons.Default.Style, contentDescription = "Deck", tint = HextechGoldBright, modifier = Modifier.size(13.dp))
                    Text(
                        text = "${combat.playerDrawPile.size}",
                        fontSize = descFontSize,
                        color = HextechGoldBright,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("graveyard_button")
                        .clickable(onClick = onShowGraveyard),
                    shape = RoundedCornerShape(6.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = "Graveyard", tint = MutedSilver, modifier = Modifier.size(12.dp))
                        Text(
                            text = "${combat.playerGraveyard.size + combat.enemyGraveyard.size}",
                            fontSize = (descFontSize.value * 0.85f).coerceAtLeast(9f).sp,
                            color = ParchmentWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onToggleEmote,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("emote_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "Emotes",
                        tint = HextechGoldBright,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (playerNexusDelta > 0) CrimsonNexus.copy(alpha = 0.36f) else EmeraldHeal.copy(alpha = 0.16f),
                    border = BorderStroke(if (playerNexusDelta > 0) 2.2.dp else 1.5.dp, if (playerNexusDelta > 0) CrimsonNexus else EmeraldHeal)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = laneGap * 0.45f, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Your Nexus HP", tint = EmeraldHeal, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${combat.playerNexusHp}/${combat.playerMaxHp}",
                                fontSize = statFontSize,
                                fontWeight = FontWeight.Bold,
                                color = ParchmentWhite,
                                maxLines = 1,
                                modifier = Modifier.testTag("player_nexus_hp")
                            )
                        }
                    }
                }
                if (playerNexusDelta > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        shape = RoundedCornerShape(6.dp),
                        color = CrimsonNexus,
                        border = BorderStroke(1.dp, ParchmentWhite)
                    ) {
                        Text(
                            text = "-$playerNexusDelta",
                            fontSize = descFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = ParchmentWhite,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnemyTopHandZone(
    combat: CombatState,
    zoneHeight: Dp,
    cardBackWidth: Dp,
    cardBackHeight: Dp,
    descFontSize: TextUnit,
    onShowPower: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(zoneHeight)
            .clipToBounds()
    ) {
        val shownBacks = minOf(combat.enemyHandCount, 7)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -cardBackHeight * 0.65f),
            horizontalArrangement = Arrangement.spacedBy(-cardBackWidth * 0.25f)
        ) {
            repeat(shownBacks) {
                Surface(
                    modifier = Modifier.size(width = cardBackWidth, height = cardBackHeight),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1A1429),
                    border = BorderStroke(1.2.dp, CrimsonNexus.copy(alpha = 0.7f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(ObsidianSurface, Color(0xFF2B1326))
                                )
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = cardBackHeight * 0.08f)
                                .size(cardBackWidth * 0.35f)
                                .clip(CircleShape)
                                .background(CrimsonNexus.copy(alpha = 0.35f))
                                .border(1.dp, HextechGold.copy(alpha = 0.7f), CircleShape)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = zoneHeight * 0.08f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (combat.encounterNode.enemyPowerTitle.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onShowPower),
                    shape = RoundedCornerShape(8.dp),
                    color = ObsidianSurface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, CrimsonNexus.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = "Foe Power", tint = CrimsonNexus, modifier = Modifier.size(12.dp))
                        Text(
                            text = combat.encounterNode.enemyPowerTitle,
                            fontSize = descFontSize,
                            color = ParchmentWhite,
                            maxLines = 1
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ManaBlue.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, ManaBlue.copy(alpha = 0.7f))
            ) {
                Text(
                    text = "Foe ${combat.enemyMana}/${combat.enemyMaxMana}",
                    fontSize = descFontSize,
                    color = ParchmentWhite,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun BenchLaneZone(
    units: List<UnitOnBoard>,
    isEnemy: Boolean,
    combat: CombatState,
    zoneHeight: Dp,
    tileWidth: Dp,
    tileHeight: Dp,
    unitGap: Dp,
    statFontSize: TextUnit,
    descFontSize: TextUnit,
    zoneLabel: String,
    emptyHint: String,
    dropHighlight: DropHighlight,
    draggingUnitId: String?,
    zoneTestTag: String,
    modifier: Modifier = Modifier,
    onUnitClick: (UnitOnBoard) -> Unit,
    onUnitLongClick: (UnitOnBoard) -> Unit,
    onUnitDragStart: ((UnitOnBoard, Offset) -> Unit)?,
    onUnitDrag: ((UnitOnBoard, Offset) -> Unit)?,
    onUnitDragEnd: ((UnitOnBoard, Offset) -> Unit)?
) {
    val borderColor = when (dropHighlight) {
        DropHighlight.VALID -> ArcaneCyan
        DropHighlight.INVALID -> CrimsonNexus
        DropHighlight.NONE -> if (isEnemy) CrimsonNexus.copy(alpha = 0.28f) else ArcaneCyan.copy(alpha = 0.28f)
    }
    val bgColor = when (dropHighlight) {
        DropHighlight.VALID -> ArcaneCyan.copy(alpha = 0.14f)
        DropHighlight.INVALID -> CrimsonNexus.copy(alpha = 0.14f)
        DropHighlight.NONE -> ObsidianSurface.copy(alpha = 0.34f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(zoneHeight)
            .testTag(zoneTestTag)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (dropHighlight != DropHighlight.NONE) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle Zone Label in Top-Start Corner
        Text(
            text = zoneLabel,
            fontSize = (descFontSize.value * 0.72f).coerceAtLeast(8f).sp,
            fontWeight = FontWeight.SemiBold,
            color = if (dropHighlight == DropHighlight.VALID) ArcaneCyan else MutedSilver.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )

        if (units.isEmpty()) {
            Text(
                text = if (dropHighlight == DropHighlight.VALID) "Release to place on Bench" else emptyHint,
                fontSize = descFontSize,
                color = if (dropHighlight == DropHighlight.VALID) ArcaneCyan else MutedSilver.copy(alpha = 0.65f),
                maxLines = 1
            )
        } else {
            val isBlockCandidatePhase = !isEnemy &&
                combat.priorityHolder == PriorityHolder.PLAYER &&
                combat.phase == CombatPhase.COMBAT_DECLARE_BLOCK_PHASE &&
                !combat.playerInitiatedCombat &&
                combat.selectedEnemyAttackerForBlock != null
            val targetEnemy = if (isBlockCandidatePhase) {
                combat.enemyBoard.find { it.instanceId == combat.selectedEnemyAttackerForBlock }
            } else null

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(unitGap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                units.take(6).forEach { unit ->
                    val canDragUnit = !isEnemy &&
                        onUnitDragStart != null &&
                        combat.priorityHolder == PriorityHolder.PLAYER &&
                        ((combat.phase == CombatPhase.MAIN_ACTION_PHASE && combat.playerHasAttackToken && unit.canAttack) ||
                            (combat.phase == CombatPhase.COMBAT_DECLARE_ATTACK_PHASE && unit.canAttack) ||
                            (combat.phase == CombatPhase.COMBAT_DECLARE_BLOCK_PHASE && !combat.playerInitiatedCombat && unit.canBlock))

                    var itemOrigin by remember(unit.instanceId) { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { itemOrigin = it.positionInRoot() }
                            .then(
                                if (canDragUnit) {
                                    Modifier.pointerInput(unit.instanceId, combat.phase, combat.priorityHolder) {
                                        var latestRootPos = Offset.Zero
                                        detectDragGestures(
                                            onDragStart = { localOffset ->
                                                latestRootPos = itemOrigin + localOffset
                                                onUnitDragStart.invoke(unit, latestRootPos)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                latestRootPos = itemOrigin + change.position
                                                onUnitDrag?.invoke(unit, latestRootPos)
                                            },
                                            onDragEnd = { onUnitDragEnd?.invoke(unit, latestRootPos) },
                                            onDragCancel = { onUnitDragEnd?.invoke(unit, Offset(-9999f, -9999f)) }
                                        )
                                    }
                                } else Modifier
                            )
                    ) {
                        UnitToken(
                            unit = unit,
                            isAttacking = combat.attackingUnitIds.contains(unit.instanceId),
                            isSelected = isEnemy && combat.selectedEnemyAttackerForBlock == unit.instanceId,
                            isValidBlockCandidate = targetEnemy != null && unit.canBlockAttacker(targetEnemy),
                            isDragging = draggingUnitId == unit.instanceId,
                            tileWidth = tileWidth,
                            tileHeight = tileHeight,
                            statFontSize = statFontSize,
                            testTag = "board_unit_${unit.instanceId}",
                            onClick = { onUnitClick(unit) },
                            onLongClick = { onUnitLongClick(unit) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterCombatZone(
    combat: CombatState,
    zoneHeight: Dp,
    tileWidth: Dp,
    tileHeight: Dp,
    unitGap: Dp,
    statFontSize: TextUnit,
    descFontSize: TextUnit,
    dropHighlight: DropHighlight,
    draggingUnitId: String?,
    isDraggingBenchBlocker: Boolean,
    draggedBenchUnit: UnitOnBoard?,
    onBoundsChanged: (Rect) -> Unit,
    onEnemyAttackerBoundsChanged: (String, Rect) -> Unit,
    onPlayerUnitClick: (UnitOnBoard) -> Unit,
    onEnemyUnitClick: (UnitOnBoard) -> Unit,
    onUnitLongClick: (UnitOnBoard) -> Unit,
    onCombatUnitDragStart: (UnitOnBoard, Offset) -> Unit,
    onCombatUnitDrag: (UnitOnBoard, Offset) -> Unit,
    onCombatUnitDragEnd: (UnitOnBoard, Offset) -> Unit,
    onResolveStack: () -> Unit
) {
    val isPlayerPriority = combat.priorityHolder == PriorityHolder.PLAYER
    val hasActiveCombatUnits = combat.attackingUnitIds.isNotEmpty() &&
        (combat.phase == CombatPhase.COMBAT_DECLARE_ATTACK_PHASE || combat.phase == CombatPhase.COMBAT_DECLARE_BLOCK_PHASE)

    val statusText = when {
        dropHighlight == DropHighlight.VALID -> "Release here to engage in Shared Battlefield"
        dropHighlight == DropHighlight.INVALID -> "Action not valid here right now"
        !isPlayerPriority -> "Foe is thinking..."
        combat.phase == CombatPhase.COMBAT_DECLARE_ATTACK_PHASE ->
            "Shared Battlefield • Drag or tap allies to Attack (${combat.attackingUnitIds.size})"
        combat.phase == CombatPhase.COMBAT_DECLARE_BLOCK_PHASE ->
            "Shared Battlefield • Drag or pair defenders to Block (${combat.blockPairings.size})"
        combat.phase == CombatPhase.SPELL_STACK_PHASE ->
            "Spell Stack open — respond or Resolve"
        else -> if (combat.playerHasAttackToken) "Shared Battlefield • Your Attack Token" else "Shared Battlefield • Hold or Cast"
    }

    val borderColor = when (dropHighlight) {
        DropHighlight.VALID -> ArcaneCyan
        DropHighlight.INVALID -> CrimsonNexus
        DropHighlight.NONE -> if (hasActiveCombatUnits) LegendaryAmber.copy(alpha = 0.55f) else SlateBorder.copy(alpha = 0.45f)
    }
    val bgColor = when (dropHighlight) {
        DropHighlight.VALID -> ArcaneCyan.copy(alpha = 0.14f)
        DropHighlight.INVALID -> CrimsonNexus.copy(alpha = 0.14f)
        DropHighlight.NONE -> if (hasActiveCombatUnits) LegendaryAmber.copy(alpha = 0.08f) else ObsidianBg.copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(zoneHeight)
            .testTag("shared_battlefield_zone")
            .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) }
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (dropHighlight != DropHighlight.NONE) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Center Battlefield Axis Line separating AI Combat Row (Top) & Player Combat Row (Bottom)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val midY = size.height / 2f
            drawLine(
                color = if (hasActiveCombatUnits) Color(0x66F59E0B) else Color(0x3364748B),
                start = Offset(16f, midY),
                end = Offset(size.width - 16f, midY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
            )
        }

        if (hasActiveCombatUnits) {
            val pairTileHeight = zoneHeight * 0.41f
            val pairTileWidth = minOf(tileWidth, pairTileHeight * 0.88f)

            if (combat.playerInitiatedCombat) {
                // Player attackers in Lower Combat Row facing upward toward AI Combat Row
                val attackers = combat.attackingUnitIds.mapNotNull { id ->
                    combat.playerBoard.find { it.instanceId == id }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(unitGap, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    attackers.take(6).forEach { atk ->
                        var atkOrigin by remember(atk.instanceId) { mutableStateOf(Offset.Zero) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Top Opposing AI Clash Slot + Forward Strike Connector
                            Box(
                                modifier = Modifier
                                    .size(width = pairTileWidth, height = pairTileHeight * 0.72f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, LegendaryAmber.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsKabaddi,
                                    contentDescription = "Attacking Lane",
                                    tint = LegendaryAmber.copy(alpha = 0.75f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Canvas(modifier = Modifier.size(width = 6.dp, height = 8.dp)) {
                                drawLine(
                                    color = LegendaryAmber,
                                    start = Offset(size.width / 2f, size.height),
                                    end = Offset(size.width / 2f, 0f),
                                    strokeWidth = 3f,
                                    cap = StrokeCap.Round
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { atkOrigin = it.positionInRoot() }
                                    .then(
                                        if (combat.phase == CombatPhase.COMBAT_DECLARE_ATTACK_PHASE && isPlayerPriority) {
                                            Modifier.pointerInput(atk.instanceId, combat.phase) {
                                                var latestRoot = Offset.Zero
                                                detectDragGestures(
                                                    onDragStart = { local ->
                                                        latestRoot = atkOrigin + local
                                                        onCombatUnitDragStart(atk, latestRoot)
                                                    },
                                                    onDrag = { change, _ ->
                                                        change.consume()
                                                        latestRoot = atkOrigin + change.position
                                                        onCombatUnitDrag(atk, latestRoot)
                                                    },
                                                    onDragEnd = { onCombatUnitDragEnd(atk, latestRoot) },
                                                    onDragCancel = { onCombatUnitDragEnd(atk, Offset(-9999f, -9999f)) }
                                                )
                                            }
                                        } else Modifier
                                    )
                            ) {
                                UnitToken(
                                    unit = atk,
                                    isAttacking = true,
                                    isSelected = true,
                                    isDragging = draggingUnitId == atk.instanceId,
                                    tileWidth = pairTileWidth,
                                    tileHeight = pairTileHeight,
                                    statFontSize = (statFontSize.value * 0.88f).coerceAtLeast(10f).sp,
                                    testTag = "board_unit_${atk.instanceId}",
                                    onClick = { onPlayerUnitClick(atk) },
                                    onLongClick = { onUnitLongClick(atk) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Enemy attackers (Top) paired with Player blockers (Bottom) connected by clash line
                val enemyAttackers = combat.attackingUnitIds.mapNotNull { id ->
                    combat.enemyBoard.find { it.instanceId == id }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(unitGap, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    enemyAttackers.take(6).forEach { enemyAtk ->
                        val blockerId = combat.blockPairings[enemyAtk.instanceId]
                        val blocker = blockerId?.let { id -> combat.playerBoard.find { it.instanceId == id } }
                        val isTargeted = combat.selectedEnemyAttackerForBlock == enemyAtk.instanceId
                        val canDraggedBlockThis = isDraggingBenchBlocker &&
                            draggedBenchUnit?.canBlockAttacker(enemyAtk) == true

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier.onGloballyPositioned {
                                    onEnemyAttackerBoundsChanged(enemyAtk.instanceId, it.boundsInRoot())
                                }
                            ) {
                                UnitToken(
                                    unit = enemyAtk,
                                    isAttacking = true,
                                    isSelected = isTargeted || canDraggedBlockThis,
                                    tileWidth = pairTileWidth,
                                    tileHeight = pairTileHeight,
                                    statFontSize = (statFontSize.value * 0.85f).coerceAtLeast(10f).sp,
                                    testTag = "board_unit_${enemyAtk.instanceId}",
                                    onClick = { onEnemyUnitClick(enemyAtk) },
                                    onLongClick = { onUnitLongClick(enemyAtk) }
                                )
                            }

                            // Clash Connector Line between AI Attacker & Player Blocker
                            Canvas(modifier = Modifier.size(width = 8.dp, height = 8.dp)) {
                                drawLine(
                                    color = if (blocker != null) EmeraldHeal else CrimsonNexus.copy(alpha = 0.7f),
                                    start = Offset(size.width / 2f, 0f),
                                    end = Offset(size.width / 2f, size.height),
                                    strokeWidth = if (blocker != null) 4f else 2f,
                                    cap = StrokeCap.Round
                                )
                            }

                            if (blocker != null) {
                                var blkOrigin by remember(blocker.instanceId) { mutableStateOf(Offset.Zero) }
                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { blkOrigin = it.positionInRoot() }
                                        .then(
                                            if (isPlayerPriority) {
                                                Modifier.pointerInput(blocker.instanceId, combat.phase) {
                                                    var latestRoot = Offset.Zero
                                                    detectDragGestures(
                                                        onDragStart = { local ->
                                                            latestRoot = blkOrigin + local
                                                            onCombatUnitDragStart(blocker, latestRoot)
                                                        },
                                                        onDrag = { change, _ ->
                                                            change.consume()
                                                            latestRoot = blkOrigin + change.position
                                                            onCombatUnitDrag(blocker, latestRoot)
                                                        },
                                                        onDragEnd = { onCombatUnitDragEnd(blocker, latestRoot) },
                                                        onDragCancel = { onCombatUnitDragEnd(blocker, Offset(-9999f, -9999f)) }
                                                    )
                                                }
                                            } else Modifier
                                        )
                                ) {
                                    UnitToken(
                                        unit = blocker,
                                        isBlockingLabel = "BLK",
                                        isDragging = draggingUnitId == blocker.instanceId,
                                        tileWidth = pairTileWidth,
                                        tileHeight = pairTileHeight,
                                        statFontSize = (statFontSize.value * 0.85f).coerceAtLeast(10f).sp,
                                        testTag = "board_unit_${blocker.instanceId}",
                                        onClick = { onPlayerUnitClick(blocker) },
                                        onLongClick = { onUnitLongClick(blocker) }
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(width = pairTileWidth, height = pairTileHeight)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (canDraggedBlockThis) ArcaneCyan.copy(alpha = 0.16f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            if (isTargeted || canDraggedBlockThis) 1.8.dp else 1.dp,
                                            if (isTargeted || canDraggedBlockThis) ArcaneCyan else CrimsonNexus.copy(alpha = 0.55f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onEnemyUnitClick(enemyAtk) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Assign Blocker",
                                        tint = if (isTargeted || canDraggedBlockThis) ArcaneCyan else MutedSilver.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = statusText,
                fontSize = (descFontSize.value * 0.82f).coerceAtLeast(9f).sp,
                color = ParchmentWhite.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .testTag("combat_priority_status")
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = descFontSize,
                    fontWeight = FontWeight.Medium,
                    color = when (dropHighlight) {
                        DropHighlight.VALID -> ArcaneCyan
                        DropHighlight.INVALID -> CrimsonNexus
                        DropHighlight.NONE -> if (isPlayerPriority) HextechGoldBright else CrimsonNexus
                    },
                    modifier = Modifier.testTag("combat_priority_status")
                )

                if (combat.spellStack.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = combat.spellStack.joinToString(" → ") { it.card.name },
                            fontSize = descFontSize,
                            color = EpicViolet,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = tileWidth * 4f)
                        )
                        Button(
                            enabled = isPlayerPriority,
                            onClick = onResolveStack,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("resolve_stack_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ArcaneCyan, contentColor = ObsidianBg)
                        ) {
                            Text(if (isPlayerPriority) "Resolve" else "Wait", fontSize = descFontSize)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerHandZone(
    combat: CombatState,
    focusedCardId: String?,
    draggingCardId: String?,
    zoneHeight: Dp,
    zoneWidth: Dp,
    cardWidth: Dp,
    cardHeight: Dp,
    descFontSize: TextUnit,
    statFontSize: TextUnit,
    onCardTap: (HandCardInstance) -> Unit,
    onCardLongPress: (HandCardInstance) -> Unit,
    onCardDragStart: (HandCardInstance, Offset) -> Unit,
    onCardDrag: (HandCardInstance, Offset) -> Unit,
    onCardDragEnd: (HandCardInstance, Offset) -> Unit
) {
    val hand = combat.playerHand
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(zoneHeight),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (hand.isEmpty()) {
            Text(
                text = "Hand empty (0/10)",
                fontSize = descFontSize,
                color = MutedSilver.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val count = hand.size
            val maxSpread = zoneWidth * 0.94f
            val idealSpacing = cardWidth * 0.14f
            val totalIdeal = count * cardWidth.value + (count - 1) * idealSpacing.value
            val dynamicSpacing = if (totalIdeal > maxSpread.value && count > 1) {
                ((maxSpread.value - count * cardWidth.value) / (count - 1)).dp
            } else {
                idealSpacing
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = zoneHeight * 0.10f),
                horizontalArrangement = Arrangement.spacedBy(dynamicSpacing, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom
            ) {
                val midIdx = (count - 1) / 2f
                hand.forEachIndexed { index, handCard ->
                    val canPlay = combat.canPlayCard(handCard)
                    val cost = combat.effectivePlayCost(handCard)
                    val distFromCenter = index - midIdx
                    val fanAngle = (distFromCenter * 1.6f).coerceIn(-6f, 6f)
                    val fanLiftY = (kotlin.math.abs(distFromCenter) * 1.4f).coerceAtMost(8f).dp

                    var cardOrigin by remember(handCard.instanceId) { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { cardOrigin = it.positionInRoot() }
                            .pointerInput(handCard.instanceId, combat.priorityHolder, combat.phase) {
                                var latestRootPos = Offset.Zero
                                detectDragGestures(
                                    onDragStart = { local ->
                                        latestRootPos = cardOrigin + local
                                        onCardDragStart(handCard, latestRootPos)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        latestRootPos = cardOrigin + change.position
                                        onCardDrag(handCard, latestRootPos)
                                    },
                                    onDragEnd = { onCardDragEnd(handCard, latestRootPos) },
                                    onDragCancel = { onCardDragEnd(handCard, Offset(-9999f, -9999f)) }
                                )
                            }
                    ) {
                        HandCardView(
                            handCard = handCard,
                            effectiveCost = cost,
                            playable = canPlay,
                            isFocused = focusedCardId == handCard.instanceId,
                            cardWidth = cardWidth,
                            cardHeight = cardHeight,
                            descFontSize = descFontSize,
                            statFontSize = statFontSize,
                            fanAngle = fanAngle,
                            fanLiftY = fanLiftY,
                            isDragging = draggingCardId == handCard.instanceId,
                            testTag = "hand_card_${handCard.template.id}",
                            onClick = { onCardTap(handCard) },
                            onLongClick = { onCardLongPress(handCard) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RightActionRail(
    combat: CombatState,
    railWidth: Dp,
    safeEndPadding: Dp,
    actionDiameter: Dp,
    manaDiameter: Dp,
    descFontSize: TextUnit,
    statFontSize: TextUnit,
    onEnterAttack: () -> Unit,
    onCancelAttack: () -> Unit,
    onCommitAttack: () -> Unit,
    onCommitBlock: () -> Unit,
    onPassTurn: () -> Unit
) {
    val isPlayerPriority = combat.priorityHolder == PriorityHolder.PLAYER
    val pulseTransition = rememberInfiniteTransition(label = "turn_btn_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "turn_btn_scale"
    )

    Box(
        modifier = Modifier
            .width(railWidth)
            .fillMaxHeight()
            .padding(start = railWidth * 0.05f, end = safeEndPadding, top = 6.dp, bottom = 6.dp)
    ) {
        // TOP: Round & Attack Token indicator + Attack / Cancel button
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, if (combat.playerHasAttackToken) LegendaryAmber else SlateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (combat.playerHasAttackToken) {
                        Icon(
                            imageVector = Icons.Default.SportsKabaddi,
                            contentDescription = "Attack Token",
                            tint = LegendaryAmber,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "R${combat.round}",
                        fontSize = descFontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (combat.playerHasAttackToken) HextechGoldBright else MutedSilver
                    )
                }
            }

            if (combat.phase == CombatPhase.MAIN_ACTION_PHASE &&
                combat.playerHasAttackToken &&
                combat.playerBoard.any { it.canAttack }
            ) {
                Button(
                    enabled = isPlayerPriority,
                    onClick = onEnterAttack,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("enter_attack_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = LegendaryAmber, contentColor = ObsidianBg)
                ) {
                    Text("ATK", fontSize = descFontSize, fontWeight = FontWeight.ExtraBold)
                }
            } else if (combat.phase == CombatPhase.COMBAT_DECLARE_ATTACK_PHASE) {
                OutlinedButton(
                    enabled = isPlayerPriority,
                    onClick = onCancelAttack,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Attack", tint = MutedSilver, modifier = Modifier.size(14.dp))
                }
            }
        }

        // CENTER (45vh–55vh thumb zone): Primary Circular Action Button with Active Pulse & Locked AI State
        val (btnLabel, btnColor, btnTag, btnAction) = when (combat.phase) {
            CombatPhase.COMBAT_DECLARE_ATTACK_PHASE -> Quad(
                "STRIKE\n${combat.attackingUnitIds.size}",
                LegendaryAmber,
                "commit_attack_button",
                onCommitAttack
            )
            CombatPhase.COMBAT_DECLARE_BLOCK_PHASE -> Quad(
                "BLOCK\n${combat.blockPairings.size}",
                ArcaneCyan,
                "commit_block_button",
                onCommitBlock
            )
            CombatPhase.SPELL_STACK_PHASE -> Quad(
                if (isPlayerPriority) "RESOLVE" else "WAIT",
                HextechGold,
                "end_round_button",
                onPassTurn
            )
            else -> Quad(
                when {
                    !isPlayerPriority -> "WAIT"
                    combat.spellStack.isNotEmpty() -> "RESOLVE"
                    combat.consecutivePasses >= 1 -> "END"
                    else -> "PASS"
                },
                HextechGold,
                "end_round_button",
                onPassTurn
            )
        }

        val btnEnabled = when (combat.phase) {
            CombatPhase.COMBAT_DECLARE_ATTACK_PHASE -> isPlayerPriority && combat.attackingUnitIds.isNotEmpty()
            else -> isPlayerPriority
        }

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    val s = if (btnEnabled) pulseScale else 1f
                    scaleX = s
                    scaleY = s
                }
                .size(actionDiameter)
                .minimumInteractiveComponentSize()
                .testTag(btnTag)
                .clickable(enabled = btnEnabled, onClick = btnAction),
            shape = CircleShape,
            color = if (btnEnabled) btnColor else ObsidianSurfaceElevated,
            border = BorderStroke(
                if (btnEnabled) 2.4.dp else 1.5.dp,
                if (btnEnabled) ParchmentWhite.copy(alpha = 0.9f) else SlateBorder
            ),
            shadowElevation = if (btnEnabled) 10.dp else 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = btnLabel,
                    fontSize = descFontSize,
                    lineHeight = descFontSize * 1.1f,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (btnEnabled) ObsidianBg else MutedSilver,
                    textAlign = TextAlign.Center
                )
            }
        }

        // BOTTOM: Segmented Mana Pips + Circular Mana Orb + 3 Spell Mana Crystals
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Unit Mana Pip Bar (up to maxMana, max 10)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.5.dp, Alignment.CenterHorizontally)
            ) {
                val maxPips = combat.playerMaxMana.coerceIn(1, 10)
                repeat(maxPips) { idx ->
                    val filled = idx < combat.playerMana
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(min = 3.dp, max = 7.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (filled) ManaBlue else ObsidianSurface)
                            .border(0.6.dp, if (filled) ArcaneCyan else SlateBorder, RoundedCornerShape(2.dp))
                    )
                }
            }

            // 3 Spell Mana Crystals
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) { idx ->
                    val filled = idx < combat.playerSpellMana
                    Box(
                        modifier = Modifier
                            .size(maxOf(8.dp, manaDiameter * 0.22f))
                            .clip(CircleShape)
                            .background(if (filled) SpellManaCyan else ObsidianSurface)
                            .border(1.dp, if (filled) ArcaneCyan else SlateBorder, CircleShape)
                    )
                }
            }

            // Unit Mana Circle
            Surface(
                modifier = Modifier.size(manaDiameter),
                shape = CircleShape,
                color = ManaBlue.copy(alpha = 0.24f),
                border = BorderStroke(2.dp, ManaBlue)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${combat.playerMana}/${combat.playerMaxMana}",
                        fontSize = statFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = ParchmentWhite
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
