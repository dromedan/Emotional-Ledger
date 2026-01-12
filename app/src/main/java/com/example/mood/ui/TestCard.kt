package com.example.mood.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mood.R
import androidx.compose.ui.graphics.luminance
import com.example.mood.model.InfluenceCardModel
import com.example.mood.model.CardPolarity
import com.example.mood.model.CardRank
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip




private val CardGreenBackground = Color(0xFF21392D)
private val CardGreenSurface    = Color(0xFF16261D)   // inner surface
private val CardGreenAccent     = Color(0xFF66BB6A)   // green
private val CardGreenAccentDark = Color(0xFF388E3C)   // deep green

private val CardRedBackground = Color(0xFF3A1F1F)

private val CardRedSurface    = Color(0xFF261616)
private val CardRedAccent     = Color(0xFFEF5350)
private val CardRedAccentDark = Color(0xFFC62828)




@Composable
fun TestCardScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = "Test Card",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        }

        // Placeholder content (we’ll animate here next)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val sampleCard = remember {
                InfluenceCardModel(
                    name = "Coding",
                    polarity = CardPolarity.GREEN,
                    deepGreenCount = 3,
                    greenCount = 2,
                    redCount = 0,
                    deepRedCount = 0,
                    imageResId = R.drawable.coding_image,
                    imagePath = null,
                    type = "Activity - Hobby",

                    subType = null,
                    rank = CardRank.COMMON,
                    averageImpact = 0.65f,
                    description = "Translating ideas into structured, functional systems through focused problem-solving."
                )
            }


            InfluenceGeneratedCard(card = sampleCard)
        }


    }
}
@Composable
private fun LiftingTestCard() {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 20.dp else 6.dp,
        label = "cardElevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.03f else 1f,
        label = "cardScale"
    )

    Card(
        onClick = { },
        modifier = Modifier
            .size(width = 260.dp, height = 380.dp)
            .scale(scale),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        ),
        colors = CardDefaults.cardColors(
            containerColor = CardGreenBackground
        ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp)
    ) {


    Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {

            // ─── Header ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        CardGreenSurface,
                        RoundedCornerShape(12.dp)
                    )

                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Coding",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )

                InfluenceSummary(
                    deepGreenCount = 3,
                    greenCount = 2,
                    redCount = 0,
                    deepRedCount = 0
                )




            }

            Spacer(Modifier.height(10.dp))

            // ─── Art Window ───────────────────────────
        val artShape = RoundedCornerShape(10.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(artShape)
                .background(
                    CardGreenAccentDark.copy(alpha = 0.25f),
                    artShape
                )
        ) {


        Image(
                painter = painterResource(id = R.drawable.coding_image),
                contentDescription = "Coding activity",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }


        Spacer(Modifier.height(8.dp))

            // ─── Type Line ────────────────────────────
        // ─── Type + Rank Line ─────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Activity - Hobby",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Common",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f)
            )
        }


        Spacer(Modifier.height(6.dp))

            Divider(color = Color.White.copy(alpha = 0.15f))

            Spacer(Modifier.height(6.dp))

            // ─── Rules + Data Box ─────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(
                        CardGreenSurface,
                        RoundedCornerShape(10.dp)
                    )

                    .padding(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Average Impact
                    Text(
                        text = "Average Impact +0.65",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Divider(color = Color.White.copy(alpha = 0.12f))

                    Text(
                        text = "Translating ideas into structured, functional systems through focused problem-solving.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )

                }
            }

            Spacer(Modifier.weight(1f))


// ─── Rules Text ───────────────────────────
            Text(
                text = "Trigger a strong emotional reaction that affects multiple internal states.",
                style = MaterialTheme.typography.bodySmall,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
            )

            Spacer(Modifier.weight(1f))


            // ─── Flavor Text ──────────────────────────
            Text(
                text = "In the moment, everything ignites.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = FontStyle.Italic
                ),
                color = Color.White.copy(alpha = 0.55f)
            )
        }

    }
}
@Composable
private fun InfluenceDots(
    deepGreenCount: Int,
    greenCount: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(deepGreenCount) {
            InfluenceDot(color = Color(0xFF388E3C)) // deep green
        }
        repeat(greenCount) {
            InfluenceDot(color = Color(0xFF66BB6A)) // green
        }
    }
}

@Composable
private fun InfluenceDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, shape = RoundedCornerShape(50))
    )
}
@Composable
private fun InfluenceCountRow(
    deepGreenCount: Int,
    greenCount: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        // ⭐ Deep Green group (e.g. 3⭐)
        InfluenceGroup(
            count = deepGreenCount,
            color = Color(0xFF388E3C) // deep green
        )

        // 🔥 Green group (e.g. 2🔥)
        InfluenceGroup(
            count = greenCount,
            color = Color(0xFF66BB6A) // green
        )
    }
}

@Composable
private fun InfluenceGroup(
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, shape = RoundedCornerShape(50))
            )
        }
    }
}
@Composable
private fun InfluenceSummary(
    deepGreenCount: Int,
    greenCount: Int,
    redCount: Int,
    deepRedCount: Int
)
 {
     Row(
         horizontalArrangement = Arrangement.spacedBy(6.dp),
         verticalAlignment = Alignment.CenterVertically
     ) {

         if (deepGreenCount > 0) {
             InfluenceCount(
                 count = deepGreenCount,
                 color = CardGreenAccentDark
             )
         }

         if (greenCount > 0) {
             InfluenceCount(
                 count = greenCount,
                 color = CardGreenAccent
             )
         }

         if (redCount > 0) {
             InfluenceCount(
                 count = redCount,
                 color = CardRedAccent
             )
         }

         if (deepRedCount > 0) {
             InfluenceCount(
                 count = deepRedCount,
                 color = CardRedAccentDark
             )
         }
     }

 }

@Composable
private fun InfluenceCount(
    count: Int,
    color: Color
) {
    val textColor =
        if (color.luminance() < 0.4f) Color.White
        else Color(0xFF102018) // near-black green

    Box(
        modifier = Modifier
            .size(18.dp)
            .background(color, shape = RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
@Composable
fun InfluenceGeneratedCard(
    card: InfluenceCardModel,
    onEditClick: (InfluenceCardModel) -> Unit = {}
) {

    val backgroundColor =
        if (card.polarity == CardPolarity.GREEN)
            CardGreenBackground
        else
            CardRedBackground

    val surfaceColor =
        if (card.polarity == CardPolarity.GREEN)
            CardGreenSurface
        else
            CardRedSurface

    val accentDark =
        if (card.polarity == CardPolarity.GREEN)
            CardGreenAccentDark
        else
            CardRedAccentDark


    val rankColor =
        when (card.rank) {
            CardRank.COMMON -> Color.White
            CardRank.UNCOMMON -> Color(0xFF90CAF9) // light blue
            CardRank.RARE -> Color(0xFFFFD54F)     // gold
            CardRank.LEGENDARY -> Color(0xFFEF5350) // red
        }


    val artShape = RoundedCornerShape(10.dp)



    val localInteractionSource = remember { MutableInteractionSource() }


    val isPressed by localInteractionSource.collectIsPressedAsState()

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 20.dp else 6.dp,
        label = "cardElevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.03f else 1f,
        label = "cardScale"
    )

    Card(
        onClick = { },
        modifier = Modifier
            .size(width = 260.dp, height = 380.dp)
            .scale(scale),

        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        interactionSource = localInteractionSource,
        shape = RoundedCornerShape(20.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {

            // ─── Header ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        surfaceColor,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatInfluenceName(card.name),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )



                InfluenceSummary(
                    deepGreenCount = card.deepGreenCount,
                    greenCount = card.greenCount,
                    redCount = card.redCount,
                    deepRedCount = card.deepRedCount
                )

            }

            Spacer(Modifier.height(10.dp))

            // ─── Art Window ───────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(artShape)
                    .background(
                        accentDark.copy(alpha = 0.25f),
                        artShape
                    )
            ) {


            val imageBitmap = remember(card.imagePath) {
                    card.imagePath
                        ?.let { path ->
                            runCatching {
                                android.graphics.BitmapFactory.decodeFile(path)
                            }.getOrNull()
                        }
                }

                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap.asImageBitmap(),
                        contentDescription = card.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(artShape),
                        contentScale = ContentScale.Crop
                    )

                } else {
                    Image(
                        painter = painterResource(id = card.imageResId),
                        contentDescription = card.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(artShape),
                        contentScale = ContentScale.Crop
                    )


                }

            }

            Spacer(Modifier.height(8.dp))

            // ─── Type + Rank Line ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val typeLabel =
                    if (!card.subType.isNullOrBlank())
                        "${card.type} — ${card.subType}"
                    else
                        card.type

                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )


                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = card.rank.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = rankColor
                )

            }

            Spacer(Modifier.height(6.dp))
            Divider(color = Color.White.copy(alpha = 0.15f))
            Spacer(Modifier.height(6.dp))

            // ─── Rules + Data Box ─────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(
                        surfaceColor,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Average Impact ${"%+.2f".format(card.averageImpact)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Divider(color = Color.White.copy(alpha = 0.12f))

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = card.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(end = 24.dp, bottom = 20.dp)
                        )

                        IconButton(
                            onClick = { onEditClick(card) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .alpha(0.55f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit influence",
                                tint = Color.White
                            )
                        }
                    }


                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}




