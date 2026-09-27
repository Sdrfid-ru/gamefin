package ru.findrug.app.ui.pet

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.findrug.app.R
import ru.findrug.app.audio.GameSound
import ru.findrug.app.audio.LocalGameAudio
import ru.findrug.app.audio.LocalGameForeground
import ru.findrug.app.audio.LocalLastInteraction
import ru.findrug.app.audio.LocalSceneAvailable
import ru.findrug.app.ui.LocalGameState
import ru.findrug.domain.*

@Composable
internal fun Fox(
    profile: ScenarioProfile?,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    happy: Boolean = false,
    stage: Int = LocalGameState.current?.growthStage ?: 0,
    interactive: Boolean = false,
) {
    val sound = LocalGameAudio.current
    val petInteraction = remember { MutableInteractionSource() }
    val resumed = LocalGameForeground.current
    val available = LocalSceneAvailable.current
    val lastInteraction = LocalLastInteraction.current
    val motion =
        animate && LocalGameState.current?.animations != false && resumed && (happy || available)
    var elapsed by remember { mutableFloatStateOf(0f) }
    var touchAt by remember { mutableStateOf<Long?>(null) }
    var touched by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    LaunchedEffect(touchAt) {
        if (touchAt != null) {
            touched = true
            delay(2200)
            touched = false
        }
    }
    // Таймер живёт только пока разрешена анимация. Изменение lifecycle/настроек отменяет
    // эффект; частоту обновления ограничиваем примерно 30 кадрами для растрового персонажа.
    LaunchedEffect(motion, happy, interactive) {
        elapsed = 0f
        if (motion) {
            val started = SystemClock.uptimeMillis()
            var previous = 0L
            while (interactive || !happy || elapsed < 2.4f) {
                withFrameNanos { frameTime ->
                    if (frameTime - previous >= 32_000_000L) {
                        now = SystemClock.uptimeMillis()
                        elapsed = (now - started) / 1000f
                        previous = frameTime
                    }
                }
            }
        }
    }
    val idleSeconds = (now - lastInteraction).coerceAtLeast(0) / 1000f
    val frame =
        PetMotion.sample(
            elapsed,
            if (interactive) idleSeconds else 0f,
            sinceTouch =
                touchAt?.let { (now - it).coerceAtLeast(0) / 1000f } ?: Float.POSITIVE_INFINITY,
            happySeconds = if (happy) elapsed else Float.POSITIVE_INFINITY,
            enabled = motion,
        )
    LaunchedEffect(frame.resting, motion) {
        if (interactive && frame.resting && motion) sound?.play(GameSound.REST)
    }
    val ageScale = listOf(.78f, .92f, 1.06f)[stage.coerceIn(0, 2)]
    val ink = Color(0xFF0B2496)
    BoxWithConstraints(
        modifier.semantics {
            contentDescription =
                "${profile?.pet ?: "Рыжик"}, ${listOf("Малыш", "Исследователь", "Знаток")[stage.coerceIn(0, 2)]}${if(happy) ", радуется успеху" else ""}"
        },
        contentAlignment = Alignment.Center,
    ) {
        val w = minOf(maxWidth, maxHeight * .81f)
        Box(Modifier.width(w).height(w / .81f)) {
            Image(
                painterResource(R.drawable.figma_6d3c3),
                null,
                Modifier.offset(y = w * .22f).size(w),
            )
            Canvas(Modifier.offset(x = w * .29f, y = w * .80f).width(w * .46f).height(w * .075f)) {
                drawOval(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF532919).copy(alpha = .22f - frame.jump * .08f),
                            Color.Transparent,
                        ),
                        radius = size.width / 2f,
                    )
                )
            }
            val petModifier =
                Modifier.offset(x = w * .21f)
                    .width(w * .62f)
                    .height(w * .855f)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(.5f, 1f)
                        scaleX = ageScale
                        scaleY = ageScale
                        translationY = -frame.jump * w.toPx() * .045f
                        rotationZ = frame.jump * 2f
                        rotationY = frame.look * 4f
                        cameraDistance = 16f * density
                    }
                    .then(
                        if (interactive)
                            Modifier.clickable(
                                    interactionSource = petInteraction,
                                    indication = null,
                                    enabled = resumed && available,
                                    onClickLabel = "Погладить лисёнка",
                                    role = Role.Button,
                                ) {
                                    touchAt = SystemClock.uptimeMillis()
                                    now = touchAt!!
                                    sound?.play(GameSound.PET)
                                }
                                .semantics { contentDescription = "Погладить лисёнка" }
                        else Modifier
                    )
            PetMesh(profile ?: ScenarioProfile(""), frame, petModifier)
            if (stage > 0)
                Box(
                    Modifier.align(Alignment.BottomCenter)
                        .offset(y = -w * .12f)
                        .size(27.dp)
                        .background(
                            if (stage == 1) Color(0xFFCAD7FF) else Color(0xFFFFDA72),
                            CircleShape,
                        )
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (stage == 1) "★" else "★★",
                        color = ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            if (happy || touched)
                Row(
                    Modifier.fillMaxWidth().padding(top = w * .18f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(if (touched) "♥" else "✦", color = Color(0xFFED728F), fontSize = 28.sp)
                    Text(if (touched) "♥" else "✦", color = Color(0xFFFF982E), fontSize = 28.sp)
                }
            if (touched || frame.resting)
                Text(
                    if (touched) "Радуется" else "Отдыхает",
                    Modifier.align(Alignment.BottomCenter)
                        .offset(y = -w * .03f)
                        .background(Color(0xEEFAF6F4), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    color = ink,
                    fontSize = 12.sp,
                )
        }
    }
}

/** A shared mesh keeps the original Figma body, clothes and accessories in register. */
@Composable
private fun PetMesh(profile: ScenarioProfile, frame: PetMotionFrame, modifier: Modifier) {
    val resources = LocalContext.current.resources
    val layerIds =
        listOf(
                listOf(R.drawable.figma_83cd7, R.drawable.figma_7bf48, R.drawable.figma_26b5f)[
                    profile.color],
                listOf(null, R.drawable.figma_12a3e, R.drawable.figma_f5258)[profile.clothes],
                listOf(R.drawable.figma_1c2e8, R.drawable.figma_8466b, null)[profile.accessory],
                listOf(null, R.drawable.figma_d87ac, R.drawable.figma_8af33)[profile.hat],
            )
            .filterNotNull()
    val bitmaps =
        remember(layerIds) {
            layerIds.map {
                BitmapFactory.decodeResource(
                    resources,
                    it,
                    BitmapFactory.Options().apply {
                        inScaled = false
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    },
                )
            }
        }
    val columns = 18
    val rows = 24
    val vertices = remember { FloatArray((columns + 1) * (rows + 1) * 2) }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    Canvas(modifier) {
        var index = 0
        for (y in 0..rows) for (x in 0..columns) {
            val point = PetMotion.vertex(x.toFloat() / columns, y.toFloat() / rows, frame)
            vertices[index++] = point.first * size.width
            vertices[index++] = point.second * size.height
        }
        drawContext.canvas.nativeCanvas.let { canvas ->
            bitmaps.forEach { bitmap ->
                canvas.drawBitmapMesh(bitmap, columns, rows, vertices, 0, null, 0, paint)
            }
        }
    }
}
