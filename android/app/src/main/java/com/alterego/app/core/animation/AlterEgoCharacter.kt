package com.alterego.app.core.animation

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alterego.app.domain.models.CharacterState
import kotlin.math.sin

/**
 * The companion, drawn rather than imported.
 *
 * Every persona shares one figure and differs by colour, posture and motion, which keeps the app
 * small and lets a Moment trigger a state directly. The state contract matches a Rive state
 * machine, so swapping in .riv files later is a change behind this composable only.
 */
@Composable
fun AlterEgoCharacter(
    state: CharacterState,
    primary: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
) {
    val transition = rememberInfiniteTransition(label = "character")

    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = state.breathDurationMillis(), easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )

    val sway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = state.swayDurationMillis(), easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sway",
    )

    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(4200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "blink",
    )

    Canvas(modifier = modifier.size(size)) {
        drawCharacter(state = state, primary = primary, accent = accent, breath = breath, sway = sway, blink = blink)
    }
}

private fun CharacterState.breathDurationMillis(): Int = when (this) {
    CharacterState.BREATHE -> 4000
    CharacterState.CELEBRATE, CharacterState.LAUGH -> 700
    CharacterState.SERIOUS, CharacterState.THINK -> 3200
    CharacterState.PRAY -> 3800
    else -> 2600
}

private fun CharacterState.swayDurationMillis(): Int = when (this) {
    CharacterState.CELEBRATE, CharacterState.WAVE -> 600
    CharacterState.LAUGH -> 500
    CharacterState.NOD -> 900
    CharacterState.SERIOUS, CharacterState.PRAY -> 5200
    else -> 3400
}

private fun DrawScope.drawCharacter(
    state: CharacterState,
    primary: Color,
    accent: Color,
    breath: Float,
    sway: Float,
    blink: Float,
) {
    val w = size.width
    val h = size.height
    val unit = w / 100f

    val headRadius = 21f * unit
    val bodyTop = 46f * unit
    val centerX = w / 2f
    val breathOffset = (breath - 0.5f) * 2.2f * unit
    val nodOffset = if (state == CharacterState.NOD) sin(sway * 3f) * 3f * unit else 0f
    val headY = 28f * unit + breathOffset * 0.6f + nodOffset
    val tilt = when (state) {
        CharacterState.THINK -> -9f
        CharacterState.LOOK -> 5f
        CharacterState.PRAY -> 12f
        CharacterState.SERIOUS -> 0f
        CharacterState.LAUGH -> -6f
        else -> sway * 2.5f
    }

    // The second circle from the logo: the other self, standing just behind.
    drawCircle(
        color = accent.copy(alpha = 0.16f),
        radius = headRadius * 1.75f,
        center = Offset(centerX + 6f * unit, headY + 2f * unit),
    )

    val torso = Path().apply {
        moveTo(centerX - 25f * unit, h)
        lineTo(centerX - 20f * unit, bodyTop + breathOffset)
        quadraticBezierTo(centerX, bodyTop - 6f * unit + breathOffset, centerX + 20f * unit, bodyTop + breathOffset)
        lineTo(centerX + 25f * unit, h)
        close()
    }
    drawPath(torso, color = primary)

    rotate(degrees = tilt, pivot = Offset(centerX, headY + headRadius)) {
        drawCircle(color = primary, radius = headRadius, center = Offset(centerX, headY))

        val eyeY = headY - 2f * unit
        val eyeDx = 7.5f * unit
        val isBlinking = blink > 0.94f

        when {
            state == CharacterState.SERIOUS -> {
                drawEyeLine(centerX - eyeDx, eyeY, unit, accent)
                drawEyeLine(centerX + eyeDx, eyeY, unit, accent)
                drawBrow(centerX - eyeDx, eyeY - 5f * unit, unit, accent, -12f)
                drawBrow(centerX + eyeDx, eyeY - 5f * unit, unit, accent, 12f)
            }
            state == CharacterState.LAUGH || state == CharacterState.CELEBRATE -> {
                drawHappyEye(centerX - eyeDx, eyeY, unit, accent)
                drawHappyEye(centerX + eyeDx, eyeY, unit, accent)
            }
            state == CharacterState.PRAY || (state == CharacterState.BREATHE && breath < 0.5f) || isBlinking -> {
                drawEyeLine(centerX - eyeDx, eyeY, unit, accent)
                drawEyeLine(centerX + eyeDx, eyeY, unit, accent)
            }
            else -> {
                val gaze = if (state == CharacterState.THINK) 1.6f * unit else 0f
                drawCircle(color = accent, radius = 2.6f * unit, center = Offset(centerX - eyeDx + gaze, eyeY))
                drawCircle(color = accent, radius = 2.6f * unit, center = Offset(centerX + eyeDx + gaze, eyeY))
            }
        }

        drawMouth(state = state, centerX = centerX, y = headY + 9f * unit, unit = unit, color = accent)
    }

    when (state) {
        CharacterState.WAVE -> drawHand(centerX + 26f * unit, bodyTop + 4f * unit + sway * 3f * unit, unit, primary, accent)
        CharacterState.POINT, CharacterState.ENCOURAGE -> drawHand(centerX + 28f * unit, bodyTop - 2f * unit, unit, primary, accent)
        CharacterState.CELEBRATE -> {
            drawHand(centerX + 28f * unit, bodyTop - 6f * unit - sway * 2f * unit, unit, primary, accent)
            drawHand(centerX - 28f * unit, bodyTop - 6f * unit + sway * 2f * unit, unit, primary, accent)
        }
        CharacterState.THINK -> drawHand(centerX + 14f * unit, headY + 16f * unit, unit, primary, accent)
        else -> Unit
    }
}

private fun DrawScope.drawEyeLine(x: Float, y: Float, unit: Float, color: Color) {
    drawLine(
        color = color,
        start = Offset(x - 3f * unit, y),
        end = Offset(x + 3f * unit, y),
        strokeWidth = 1.6f * unit,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawHappyEye(x: Float, y: Float, unit: Float, color: Color) {
    val path = Path().apply {
        moveTo(x - 3.4f * unit, y + 1.2f * unit)
        quadraticBezierTo(x, y - 3.4f * unit, x + 3.4f * unit, y + 1.2f * unit)
    }
    drawPath(path, color = color, style = Stroke(width = 1.6f * unit, cap = StrokeCap.Round))
}

private fun DrawScope.drawBrow(x: Float, y: Float, unit: Float, color: Color, angle: Float) {
    rotate(degrees = angle, pivot = Offset(x, y)) {
        drawLine(
            color = color.copy(alpha = 0.85f),
            start = Offset(x - 3.6f * unit, y),
            end = Offset(x + 3.6f * unit, y),
            strokeWidth = 1.4f * unit,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawMouth(state: CharacterState, centerX: Float, y: Float, unit: Float, color: Color) {
    val path = Path()
    when (state) {
        CharacterState.SMILE, CharacterState.ENCOURAGE, CharacterState.CELEBRATE, CharacterState.WAVE -> {
            path.moveTo(centerX - 5f * unit, y)
            path.quadraticBezierTo(centerX, y + 4.5f * unit, centerX + 5f * unit, y)
        }
        CharacterState.LAUGH -> {
            path.moveTo(centerX - 6f * unit, y - 1f * unit)
            path.quadraticBezierTo(centerX, y + 7f * unit, centerX + 6f * unit, y - 1f * unit)
        }
        CharacterState.SERIOUS, CharacterState.POINT -> {
            path.moveTo(centerX - 4.5f * unit, y + 1f * unit)
            path.lineTo(centerX + 4.5f * unit, y + 1f * unit)
        }
        CharacterState.THINK -> {
            path.moveTo(centerX - 4f * unit, y + 1.5f * unit)
            path.quadraticBezierTo(centerX, y - 0.5f * unit, centerX + 4f * unit, y + 1.5f * unit)
        }
        else -> {
            path.moveTo(centerX - 4f * unit, y)
            path.quadraticBezierTo(centerX, y + 2.6f * unit, centerX + 4f * unit, y)
        }
    }
    drawPath(path, color = color, style = Stroke(width = 1.7f * unit, cap = StrokeCap.Round))
}

private fun DrawScope.drawHand(x: Float, y: Float, unit: Float, primary: Color, accent: Color) {
    drawCircle(color = primary, radius = 5.5f * unit, center = Offset(x, y))
    drawCircle(color = accent.copy(alpha = 0.35f), radius = 5.5f * unit, center = Offset(x, y), style = Stroke(width = 1f * unit))
}
