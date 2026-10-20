package ru.findrug.app.ui.art

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.*
import com.caverock.androidsvg.SVG
import ru.findrug.app.R

@OptIn(ExperimentalTextApi::class)
internal val BodyFont =
    FontFamily(
        Font(R.font.sn_pro, variationSettings = FontVariation.Settings(FontVariation.weight(400)))
    )
internal val TitleFont = FontFamily(Font(R.font.days_one))
@OptIn(ExperimentalTextApi::class)
internal val FriendlyFont =
    FontFamily(
        Font(
            R.font.playpen_sans,
            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
        )
    )

/** Original Figma SVG bytes; aspect-fit without rewriting viewBox or path geometry. */
@Composable
internal fun FigmaVector(resource: Int, modifier: Modifier = Modifier, stretch: Boolean = false) {
    val context = LocalContext.current
    val picture = remember(resource) { SVG.getFromResource(context, resource).renderToPicture() }
    Canvas(modifier) {
        val factor = minOf(size.width / picture.width, size.height / picture.height)
        val w = if (stretch) size.width else picture.width * factor
        val h = if (stretch) size.height else picture.height * factor
        drawIntoCanvas {
            it.nativeCanvas.drawPicture(
                picture,
                RectF(
                    (size.width - w) / 2,
                    (size.height - h) / 2,
                    (size.width + w) / 2,
                    (size.height + h) / 2,
                ),
            )
        }
    }
}

private val pictures =
    mapOf(
        "📅" to R.drawable.figma_4bf93,
        "🐷" to R.drawable.figma_ba1a5,
        "🥣" to R.drawable.figma_a49fe,
        "❤️" to R.drawable.figma_1e1d8,
        "🪮" to R.drawable.figma_2ff9d,
        "⚡" to R.drawable.figma_3bcb0,
        "📋" to R.drawable.figma_9ade2,
        "🛒" to R.drawable.figma_43524,
        "🎁" to R.drawable.figma_0d0bf,
        "🪙" to R.drawable.figma_ec083,
        "🚲" to R.drawable.item_bicycle,
        "🏀" to R.drawable.item_basketball,
        "⚽" to R.drawable.item_ball,
        "🧴" to R.drawable.item_soap,
        "💧" to R.drawable.item_water,
        "🧸" to R.drawable.item_toy,
        "🧢" to R.drawable.item_cap,
        "goal_tent" to R.drawable.goal_tent,
        "goal_construction_set" to R.drawable.goal_construction_set,
        "goal_camera" to R.drawable.goal_camera,
        "goal_star_projector" to R.drawable.goal_star_projector,
        "category_need_umbrella" to R.drawable.category_need_umbrella,
        "category_need_toothpaste" to R.drawable.category_need_toothpaste,
        "category_need_toothbrush" to R.drawable.category_need_toothbrush,
        "category_need_backpack" to R.drawable.category_need_backpack,
        "category_need_apple" to R.drawable.category_need_apple,
        "category_need_boots" to R.drawable.category_need_boots,
        "category_need_jacket" to R.drawable.category_need_jacket,
        "category_need_soup" to R.drawable.category_need_soup,
        "category_need_bread" to R.drawable.category_need_bread,
        "category_need_notebook" to R.drawable.category_need_notebook,
        "category_want_game_console" to R.drawable.category_want_game_console,
        "category_want_doll" to R.drawable.category_want_doll,
        "category_want_teddy_bear" to R.drawable.category_want_teddy_bear,
        "category_want_comic_book" to R.drawable.category_want_comic_book,
        "category_want_robot" to R.drawable.category_want_robot,
        "category_want_kite" to R.drawable.category_want_kite,
        "category_want_ice_cream" to R.drawable.category_want_ice_cream,
        "category_want_sunglasses" to R.drawable.category_want_sunglasses,
        "category_want_skateboard" to R.drawable.category_want_skateboard,
        "category_want_headphones" to R.drawable.category_want_headphones,
    )

internal fun roomResource(id: String): Int =
    when (id) {
        "pets" -> R.drawable.room_pets
        "astronomer" -> R.drawable.room_astronomer
        "park" -> R.drawable.room_park
        "scout" -> R.drawable.room_scout
        "constructor" -> R.drawable.room_constructor
        "sport" -> R.drawable.room_sport
        "photographer" -> R.drawable.room_photographer
        "projector" -> R.drawable.room_projector
        else -> R.drawable.room_default
    }

private val vectors =
    mapOf(
        "⚙" to R.raw.figma_aaf70,
        "‹" to R.raw.figma_4a57a,
        "⌂" to R.raw.figma_9116b,
        "🐾" to R.raw.figma_3e7e7,
        "▥" to R.raw.figma_12df1,
        "📊" to R.raw.figma_12df1,
        "✪" to R.raw.figma_c0512,
        "●" to R.raw.figma_e2b81,
    )

@Composable
internal fun GameIcon(symbol: String, dimension: Dp, modifier: Modifier = Modifier) {
    pictures[symbol]?.let {
        Image(painterResource(it), null, modifier.size(dimension), contentScale = ContentScale.Fit)
        return
    }
    vectors[symbol]?.let {
        FigmaVector(
            it,
            modifier.size(dimension).scale(scaleX = if (symbol == "‹") -1f else 1f, scaleY = 1f),
        )
        return
    }
    // The illustrations cross the atlas's nominal row boundaries, especially the telescope lens.
    val source =
        when (symbol) {
            "🏡" -> IntRect(724, 710, 1086, 1040)
            "🔭" -> IntRect(1086, 700, 1448, 1040)
            else -> null
        }
    if (source == null) {
        Box(modifier.size(dimension), contentAlignment = Alignment.Center) {
            Text(symbol, fontSize = (dimension.value * .75f).sp)
        }
        return
    }
    val resources = LocalContext.current.resources
    val atlas =
        remember(resources) { ImageBitmap.imageResource(resources, R.drawable.reference_icons) }
    Canvas(modifier.size(dimension)) {
        val factor = minOf(size.width / source.width, size.height / source.height)
        val destination = IntSize((source.width * factor).toInt(), (source.height * factor).toInt())
        drawImage(
            atlas,
            srcOffset = source.topLeft,
            srcSize = source.size,
            dstOffset =
                IntOffset(
                    (size.width.toInt() - destination.width) / 2,
                    (size.height.toInt() - destination.height) / 2,
                ),
            dstSize = destination,
            filterQuality = FilterQuality.High,
        )
    }
}
