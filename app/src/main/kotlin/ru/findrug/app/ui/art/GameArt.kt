package ru.findrug.app.ui.art

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
        "🚲" to R.drawable.figma_8be7a,
    )
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
        Image(painterResource(it), null, modifier.size(dimension))
        return
    }
    vectors[symbol]?.let {
        FigmaVector(
            it,
            modifier.size(dimension).scale(scaleX = if (symbol == "‹") -1f else 1f, scaleY = 1f),
        )
        return
    }
    val cell = mapOf("🏡" to 10, "🔭" to 11)[symbol]
    if (cell == null) {
        Text(symbol, modifier, fontSize = dimension.value.sp)
        return
    }
    val resources = LocalContext.current.resources
    val atlas =
        remember(resources) { ImageBitmap.imageResource(resources, R.drawable.reference_icons) }
    Canvas(modifier.size(dimension)) {
        val w = atlas.width / 4
        val h = atlas.height / 3
        drawImage(
            atlas,
            srcOffset = IntOffset((cell % 4) * w, (cell / 4) * h),
            srcSize = IntSize(w, h),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.High,
        )
    }
}
