package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CardCatalog
import com.example.model.CardTemplate
import com.example.model.CardType
import com.example.model.DeckCardEntry
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

@Composable
fun UnitToken(
    unit: UnitOnBoard,
    isAttacking: Boolean = false,
    isBlockingLabel: String? = null,
    isSelected: Boolean = false,
    testTag: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isSelected -> ArcaneCyan
        isAttacking -> LegendaryAmber
        isBlockingLabel != null -> EmeraldHeal
        unit.isStunned -> CrimsonNexus
        unit.isFrostbitten -> ManaBlue
        unit.hasBarrier -> HextechGoldBright
        unit.template.isChampion -> HextechGold
        else -> SlateBorderHighlight
    }

    Surface(
        modifier = modifier
            .width(108.dp)
            .minimumInteractiveComponentSize()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (unit.isFrostbitten) ObsidianSurfaceElevated else ObsidianCard,
        border = BorderStroke(if (isSelected || isAttacking) 2.5.dp else 1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = unit.template.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unit.template.isChampion) HextechGoldBright else ParchmentWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (unit.isLeveledUp) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Leveled Up",
                        tint = HextechGoldBright,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Status / Keyword Icon Strip
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (unit.hasBarrier) {
                    Icon(Icons.Default.Shield, "Barrier", tint = HextechGoldBright, modifier = Modifier.size(13.dp))
                }
                if (unit.hasSpellShield) {
                    Icon(Icons.Default.AutoFixHigh, "SpellShield", tint = EpicViolet, modifier = Modifier.size(13.dp))
                }
                if (unit.isFrostbitten) {
                    Icon(Icons.Default.AcUnit, "Frostbitten", tint = ManaBlue, modifier = Modifier.size(13.dp))
                }
                if (unit.isStunned) {
                    Icon(Icons.Default.FlashOn, "Stunned", tint = CrimsonNexus, modifier = Modifier.size(13.dp))
                }
                val topKw = unit.keywords.firstOrNull { it != Keyword.BARRIER && it != Keyword.SPELLSHIELD && it != Keyword.STUN }
                if (topKw != null) {
                    Text(
                        text = topKw.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = ArcaneCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isAttacking) {
                Text(
                    text = "ATTACKING",
                    style = MaterialTheme.typography.labelSmall,
                    color = LegendaryAmber
                )
            } else if (isBlockingLabel != null) {
                Text(
                    text = isBlockingLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldHeal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (unit.template.type == CardType.LANDMARK) {
                Text(
                    text = "Countdown ${unit.countdownRemaining}",
                    style = MaterialTheme.typography.labelSmall,
                    color = HextechGoldBright
                )
            }

            if (unit.template.type != CardType.LANDMARK) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LegendaryAmber.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "${unit.strikePower}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (unit.isFrostbitten) ManaBlue else ParchmentWhite,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CrimsonNexus.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "${unit.health}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (unit.health < unit.maxHealth) LegendaryAmber else ParchmentWhite,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
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
