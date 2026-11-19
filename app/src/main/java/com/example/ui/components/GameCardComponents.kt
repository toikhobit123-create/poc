package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.model.CardCatalog
import com.example.model.CardTemplate
import com.example.model.CardType
import com.example.model.DeckCardEntry
import com.example.model.HandCardInstance
import com.example.model.ItemTemplate
import com.example.model.Keyword
import com.example.model.KeywordCategory
import com.example.model.SpellSpeed
import com.example.model.StateCondition
import com.example.model.UnitOnBoard
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.CrimsonNexus
import com.example.ui.theme.CurseMagenta
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
import com.example.ui.theme.SlateBorderHighlight

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardTile(
    template: CardTemplate,
    effectiveCost: Int = template.cost,
    bonusPower: Int = 0,
    bonusHealth: Int = 0,
    attachedItems: List<ItemTemplate> = emptyList(),
    extraKeywords: Set<Keyword> = emptySet(),
    isFleeting: Boolean = false,
    playable: Boolean = true,
    highlighted: Boolean = false,
    compact: Boolean = false,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val regionColor = regionAccentColor(template.region)
    val borderColor = when {
        highlighted -> HextechGoldBright
        template.type == CardType.CURSE -> CurseMagenta
        template.isChampion -> HextechGold
        playable -> SlateBorderHighlight
        else -> SlateBorder
    }

    val allKeywords = template.keywords + extraKeywords + attachedItems.mapNotNull { it.grantedKeyword }

    Surface(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .then(
                if (onClick != null) Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onClick)
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (playable) ObsidianCard else ObsidianBg,
        border = BorderStroke(if (highlighted) 2.dp else 1.2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Mana gem
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(ManaBlue.copy(alpha = 0.25f))
                        .border(1.5.dp, ManaBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$effectiveCost",
                        style = MaterialTheme.typography.labelMedium,
                        color = ParchmentWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (template.isChampion) HextechGoldBright else ParchmentWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(template.region.displayName)
                            append(" • ")
                            append(
                                if (template.spellSpeed != SpellSpeed.NONE) template.spellSpeed.displayName
                                else template.type.displayName
                            )
                            if (isFleeting) append(" • Fleeting")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = regionColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (attachedItems.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EpicViolet.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, EpicViolet)
                    ) {
                        Text(
                            text = "${attachedItems.size}★",
                            style = MaterialTheme.typography.labelSmall,
                            color = ParchmentWhite,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (allKeywords.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allKeywords.take(if (compact) 3 else 6).forEach { kw ->
                        val kwColor = when (kw.category) {
                            KeywordCategory.COMBAT_POSITIVE -> ArcaneCyan
                            KeywordCategory.NEGATIVE_STATUS -> CrimsonNexus
                            KeywordCategory.CARD_PROPERTY -> HextechGoldBright
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = kwColor.copy(alpha = 0.16f),
                            border = BorderStroke(0.8.dp, kwColor.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = kw.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = kwColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            if (!compact) {
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSilver,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (attachedItems.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    attachedItems.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = rarityColor(item.rarity).copy(alpha = 0.18f),
                            border = BorderStroke(0.8.dp, rarityColor(item.rarity).copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "${item.name} (${item.description})",
                                style = MaterialTheme.typography.labelSmall,
                                color = ParchmentWhite,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (template.type == CardType.UNIT || template.type == CardType.CHAMPION || template.type == CardType.EQUIPMENT) {
                val totalPow = template.basePower + bonusPower
                val totalHp = template.baseHealth + bonusHealth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBadge(value = totalPow, label = "ATK", color = LegendaryAmber)
                    if (template.isChampion) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Champion",
                            tint = HextechGoldBright,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    StatBadge(value = totalHp, label = "HP", color = CrimsonNexus)
                }
            }
        }
    }
}

@Composable
fun StatBadge(
    value: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.labelMedium,
                color = ParchmentWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@DrawableRes
fun cardPortraitRes(cardId: String): Int? = when (cardId) {
    "card_jinx", "card_super_mega_rocket" -> R.drawable.img_champ_jinx
    "card_lux", "card_final_spark" -> R.drawable.img_champ_lux
    "card_yasuo", "card_steel_tempest" -> R.drawable.img_champ_yasuo
    else -> null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HandCardView(
    handCard: HandCardInstance,
    effectiveCost: Int,
    playable: Boolean,
    isFocused: Boolean,
    cardWidth: Dp,
    cardHeight: Dp,
    descFontSize: TextUnit,
    statFontSize: TextUnit,
    fanAngle: Float = 0f,
    fanLiftY: Dp = 0.dp,
    isDragging: Boolean = false,
    testTag: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val template = handCard.template
    val regionColor = regionAccentColor(template.region)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val elevated = (isPressed || isFocused) && !isDragging

    val scale by animateFloatAsState(
        targetValue = if (elevated) 1.5f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "hand_card_scale"
    )
    val liftY by animateDpAsState(
        targetValue = if (elevated) -cardHeight * 0.48f else fanLiftY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "hand_card_lift"
    )
    val rotation by animateFloatAsState(
        targetValue = if (elevated || isDragging) 0f else fanAngle,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "hand_card_rotation"
    )

    val borderColor = when {
        isFocused -> HextechGoldBright
        playable && template.isChampion -> HextechGold
        playable -> ArcaneCyan
        template.type == CardType.CURSE -> CurseMagenta
        else -> SlateBorder
    }

    val portraitRes = cardPortraitRes(template.id)
    val artHeight = cardHeight * 0.44f
    val smallTextSize = (descFontSize.value * 0.72f).coerceAtLeast(7f).sp
    val tinyStatSize = (statFontSize.value * 0.8f).coerceAtLeast(9f).sp

    Surface(
        modifier = modifier
            .zIndex(if (elevated) 50f else 1f)
            .alpha(if (isDragging) 0.25f else 1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
                translationY = liftY.toPx()
            }
            .size(width = cardWidth, height = cardHeight)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(cardWidth * 0.11f),
        color = if (playable) ObsidianCard else ObsidianBg,
        border = BorderStroke(if (elevated) 2.dp else 1.2.dp, borderColor),
        shadowElevation = if (elevated) 12.dp else 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Upper Art Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(artHeight)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(regionColor.copy(alpha = 0.42f), ObsidianSurface)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (portraitRes != null) {
                        Image(
                            painter = painterResource(id = portraitRes),
                            contentDescription = template.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = when (template.type) {
                                CardType.SPELL -> Icons.Default.AutoFixHigh
                                CardType.CHAMPION -> Icons.Default.Star
                                CardType.EQUIPMENT -> Icons.Default.Gavel
                                CardType.LANDMARK -> Icons.Default.HourglassBottom
                                CardType.CURSE -> Icons.Default.Bolt
                                CardType.UNIT -> Icons.Default.SportsKabaddi
                            },
                            contentDescription = template.name,
                            tint = regionColor,
                            modifier = Modifier.size(cardWidth * 0.38f)
                        )
                    }
                    // Subtle top scrim for Mana & Item readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)
                                )
                            )
                    )
                }

                // Name + Skill Description
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = cardWidth * 0.06f, vertical = cardHeight * 0.02f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = template.name,
                        fontSize = smallTextSize,
                        lineHeight = smallTextSize * 1.1f,
                        fontWeight = FontWeight.Bold,
                        color = if (template.isChampion) HextechGoldBright else ParchmentWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    val descSummary = if (template.spellSpeed != SpellSpeed.NONE) {
                        "${template.spellSpeed.displayName}: ${template.description}"
                    } else {
                        template.description
                    }
                    Text(
                        text = descSummary,
                        fontSize = (smallTextSize.value * 0.85f).coerceAtLeast(6.5f).sp,
                        lineHeight = (smallTextSize.value * 0.95f).coerceAtLeast(7.5f).sp,
                        color = MutedSilver,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // Bottom Stats Row (ATK Gold / HP Red or Spell badge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = cardWidth * 0.06f, vertical = cardHeight * 0.03f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (template.type == CardType.UNIT || template.type == CardType.CHAMPION || template.type == CardType.EQUIPMENT) {
                        val totalPow = template.basePower + handCard.bonusPower
                        val totalHp = template.baseHealth + handCard.bonusHealth
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LegendaryAmber.copy(alpha = 0.9f))
                                .padding(horizontal = cardWidth * 0.07f, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$totalPow",
                                fontSize = tinyStatSize,
                                fontWeight = FontWeight.ExtraBold,
                                color = ObsidianBg
                            )
                        }
                        if (handCard.attachedItems.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Items",
                                tint = EpicViolet,
                                modifier = Modifier.size(cardWidth * 0.16f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CrimsonNexus.copy(alpha = 0.92f))
                                .padding(horizontal = cardWidth * 0.07f, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$totalHp",
                                fontSize = tinyStatSize,
                                fontWeight = FontWeight.ExtraBold,
                                color = ParchmentWhite
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = template.spellSpeed.displayName.uppercase(),
                            fontSize = (smallTextSize.value * 0.85f).coerceAtLeast(6.5f).sp,
                            fontWeight = FontWeight.Bold,
                            color = ArcaneCyan,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Mana Gem Overlay (Top-Left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(cardWidth * 0.05f)
                    .size(cardWidth * 0.30f)
                    .clip(CircleShape)
                    .background(ManaBlue)
                    .border(1.dp, ParchmentWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$effectiveCost",
                    fontSize = tinyStatSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = ObsidianBg
                )
            }

            // Fleeting indicator (Top-Right)
            if (handCard.isFleeting) {
                Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = "Fleeting",
                    tint = EpicViolet,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(cardWidth * 0.06f)
                        .size(cardWidth * 0.2f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnitToken(
    unit: UnitOnBoard,
    isAttacking: Boolean = false,
    isBlockingLabel: String? = null,
    isSelected: Boolean = false,
    isValidBlockCandidate: Boolean = false,
    isDragging: Boolean = false,
    tileWidth: Dp = 88.dp,
    tileHeight: Dp = 110.dp,
    statFontSize: TextUnit = 14.sp,
    testTag: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Summon entrance pop-in animation
    val spawnScale = remember(unit.instanceId) { Animatable(0.84f) }
    LaunchedEffect(unit.instanceId) {
        spawnScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    // Damage hit feedback when unit health drops
    var prevHealth by remember(unit.instanceId) { mutableIntStateOf(unit.health) }
    var damageDelta by remember(unit.instanceId) { mutableIntStateOf(0) }
    LaunchedEffect(unit.health) {
        if (unit.health < prevHealth) {
            damageDelta = prevHealth - unit.health
            prevHealth = unit.health
            kotlinx.coroutines.delay(650)
            damageDelta = 0
        } else {
            prevHealth = unit.health
        }
    }

    val borderColor = when {
        damageDelta > 0 -> CrimsonNexus
        isSelected -> ArcaneCyan
        isAttacking -> LegendaryAmber
        isBlockingLabel != null -> EmeraldHeal
        isValidBlockCandidate -> ArcaneCyan.copy(alpha = 0.85f)
        unit.isStunned -> CrimsonNexus
        unit.isFrostbitten -> ManaBlue
        unit.hasBarrier -> HextechGoldBright
        unit.template.isChampion -> HextechGold
        else -> SlateBorderHighlight
    }

    val regionColor = regionAccentColor(unit.template.region)
    val portraitRes = cardPortraitRes(unit.template.id)

    Surface(
        modifier = modifier
            .alpha(if (isDragging) 0.25f else 1f)
            .graphicsLayer {
                scaleX = spawnScale.value
                scaleY = spawnScale.value
            }
            .size(width = tileWidth, height = tileHeight)
            .minimumInteractiveComponentSize()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(tileWidth * 0.14f),
        color = if (unit.isFrostbitten) ObsidianSurfaceElevated else ObsidianCard,
        border = BorderStroke(
            if (damageDelta > 0 || isSelected || isAttacking || isBlockingLabel != null) 2.2.dp else 1.4.dp,
            borderColor
        ),
        shadowElevation = if (isSelected || isAttacking) 6.dp else 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Character Portrait / Emblem occupying main tile area (description text hidden)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(regionColor.copy(alpha = 0.38f), ObsidianBg)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (portraitRes != null) {
                    Image(
                        painter = painterResource(id = portraitRes),
                        contentDescription = unit.template.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when {
                                unit.template.type == CardType.LANDMARK -> Icons.Default.HourglassBottom
                                unit.template.isChampion -> Icons.Default.Star
                                else -> Icons.Default.SportsKabaddi
                            },
                            contentDescription = unit.template.name,
                            tint = if (unit.template.isChampion) HextechGoldBright else regionColor,
                            modifier = Modifier.size(tileWidth * 0.42f)
                        )
                    }
                }

                // Dark bottom vignette so corner ATK/HP numbers & keyword strip stay sharp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )
            }

            // Top-right Champion Level Up Star or Countdown
            if (unit.isLeveledUp) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Leveled Up",
                    tint = HextechGoldBright,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(tileWidth * 0.06f)
                        .size(tileWidth * 0.22f)
                )
            } else if (unit.template.type == CardType.LANDMARK) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ObsidianSurface.copy(alpha = 0.85f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${unit.countdownRemaining}",
                        fontSize = statFontSize,
                        fontWeight = FontWeight.Bold,
                        color = HextechGoldBright
                    )
                }
            }

            // Center Keyword / Status Icon Strip (compact symbols only, no description text)
            val iconSize = (tileWidth * 0.21f).coerceIn(11.dp, 18.dp)
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = tileHeight * 0.16f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBg.copy(alpha = 0.65f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (unit.hasBarrier) {
                    Icon(Icons.Default.Shield, "Barrier", tint = HextechGoldBright, modifier = Modifier.size(iconSize))
                }
                if (unit.hasSpellShield) {
                    Icon(Icons.Default.AutoFixHigh, "SpellShield", tint = EpicViolet, modifier = Modifier.size(iconSize))
                }
                if (unit.isFrostbitten) {
                    Icon(Icons.Default.AcUnit, "Frostbitten", tint = ManaBlue, modifier = Modifier.size(iconSize))
                }
                if (unit.isStunned) {
                    Icon(Icons.Default.FlashOn, "Stunned", tint = CrimsonNexus, modifier = Modifier.size(iconSize))
                }
                if (unit.keywords.contains(Keyword.QUICK_ATTACK) || unit.keywords.contains(Keyword.DOUBLE_ATTACK)) {
                    Icon(Icons.Default.Bolt, "Quick Attack", tint = LegendaryAmber, modifier = Modifier.size(iconSize))
                }
                if (unit.keywords.contains(Keyword.OVERWHELM)) {
                    Icon(Icons.Default.Whatshot, "Overwhelm", tint = CrimsonNexus, modifier = Modifier.size(iconSize))
                }
                if (unit.keywords.contains(Keyword.ELUSIVE)) {
                    Icon(Icons.Default.VisibilityOff, "Elusive", tint = ArcaneCyan, modifier = Modifier.size(iconSize))
                }
                if (unit.keywords.contains(Keyword.TOUGH) || unit.keywords.contains(Keyword.REGENERATION)) {
                    Icon(Icons.Default.Shield, "Tough/Regen", tint = EmeraldHeal, modifier = Modifier.size(iconSize))
                }
            }

            // Bottom-Left Power (Gold) & Bottom-Right Health (Red)
            if (unit.template.type != CardType.LANDMARK) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(tileWidth * 0.05f)
                        .clip(RoundedCornerShape(5.dp))
                        .background(LegendaryAmber)
                        .border(0.8.dp, ObsidianBg, RoundedCornerShape(5.dp))
                        .padding(horizontal = tileWidth * 0.08f, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${unit.strikePower}",
                        fontSize = statFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (unit.isFrostbitten) ManaBlue else ObsidianBg
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(tileWidth * 0.05f)
                        .clip(RoundedCornerShape(5.dp))
                        .background(CrimsonNexus)
                        .border(0.8.dp, ObsidianBg, RoundedCornerShape(5.dp))
                        .padding(horizontal = tileWidth * 0.08f, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${unit.health}",
                        fontSize = statFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (unit.health < unit.maxHealth) HextechGoldBright else ParchmentWhite
                    )
                }
            }

            // Floating Damage Impact Pill (top-center, clears bottom ATK/HP stats)
            androidx.compose.animation.AnimatedVisibility(
                visible = damageDelta > 0,
                enter = fadeIn(tween(120)),
                exit = fadeOut(tween(240)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = tileHeight * 0.04f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CrimsonNexus,
                    border = BorderStroke(1.dp, ParchmentWhite)
                ) {
                    Text(
                        text = "-$damageDelta",
                        fontSize = (statFontSize.value * 0.85f).coerceAtLeast(10f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ParchmentWhite,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RunDeckDialog(
    runDeck: List<DeckCardEntry>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Run Deck (${runDeck.sumOf { it.copies }} Cards)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = HextechGoldBright
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close Deck", tint = MutedSilver)
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(runDeck, key = { it.cardId }) { entry ->
                    val card = CardCatalog.getCard(entry.cardId)
                    val items = entry.attachedItemIds.map { CardCatalog.getItem(it) }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${entry.copies}x ${card.name}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ParchmentWhite
                        )
                        CardTile(
                            template = card,
                            effectiveCost = maxOf(0, card.cost - items.sumOf { it.costReduction }),
                            bonusPower = items.sumOf { it.bonusPower },
                            bonusHealth = items.sumOf { it.bonusHealth },
                            attachedItems = items,
                            compact = false
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = HextechGoldBright)
            }
        }
    )
}

@Composable
fun KeywordGlossaryDialog(onDismiss: () -> Unit) {
    var selectedCategory by remember { mutableStateOf<KeywordCategory?>(null) }
    val filteredKeywords = remember(selectedCategory) {
        if (selectedCategory == null) Keyword.entries
        else Keyword.entries.filter { it.category == selectedCategory }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "Keywords & Rules Codex",
                style = MaterialTheme.typography.headlineSmall,
                color = HextechGoldBright
            )
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 450.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterTag("All", selectedCategory == null) { selectedCategory = null }
                    FilterTag("Positive", selectedCategory == KeywordCategory.COMBAT_POSITIVE) {
                        selectedCategory = KeywordCategory.COMBAT_POSITIVE
                    }
                    FilterTag("Status", selectedCategory == KeywordCategory.NEGATIVE_STATUS) {
                        selectedCategory = KeywordCategory.NEGATIVE_STATUS
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredKeywords, key = { it.name }) { kw ->
                        Column {
                            Text(
                                text = kw.displayName + if (kw.isStackable) " (Stackable)" else "",
                                style = MaterialTheme.typography.titleSmall,
                                color = when (kw.category) {
                                    KeywordCategory.COMBAT_POSITIVE -> ArcaneCyan
                                    KeywordCategory.NEGATIVE_STATUS -> CrimsonNexus
                                    KeywordCategory.CARD_PROPERTY -> HextechGoldBright
                                }
                            )
                            Text(
                                text = kw.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSilver
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = SlateBorder)
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "State Conditions",
                            style = MaterialTheme.typography.titleSmall,
                            color = HextechGoldBright
                        )
                    }
                    items(StateCondition.entries, key = { it.name }) { sc ->
                        Column {
                            Text(
                                text = sc.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldHeal
                            )
                            Text(
                                text = sc.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSilver
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = HextechGoldBright)
            }
        }
    )
}

@Composable
private fun FilterTag(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) HextechGold.copy(alpha = 0.25f) else ObsidianBg,
        border = BorderStroke(1.dp, if (selected) HextechGoldBright else SlateBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) HextechGoldBright else MutedSilver,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
