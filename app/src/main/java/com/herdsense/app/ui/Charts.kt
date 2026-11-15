package com.herdsense.app.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.roundToInt

private val GridColor = Color(0xFFE6E6E6)
private val AxisColor = Color(0xFFBDBDBD)
private const val LabelColor = 0xFF8A8A8A.toInt()

data class LineSeries(val values: List<Float>, val color: Color, val width: Dp = 1.5.dp)

@Composable
private fun rememberPaints(): Pair<Paint, Paint> {
    val px = with(LocalDensity.current) { 10.sp.toPx() }
    val y = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LabelColor; textAlign = Paint.Align.RIGHT } }
    val x = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LabelColor; textAlign = Paint.Align.CENTER } }
    y.textSize = px; x.textSize = px
    return y to x
}

@Composable
fun LineChart(
    series: List<LineSeries>,
    yMin: Float,
    yMax: Float,
    yStep: Float,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    band: ClosedFloatingPointRange<Float>? = null,
    bandColor: Color = Color(0x6634A853),
    yFormat: (Float) -> String = { it.roundToInt().toString() },
) {
    val (yPaint, xPaint) = rememberPaints()
    Canvas(modifier.fillMaxWidth().height(220.dp)) {
        val left = 36.dp.toPx(); val right = 8.dp.toPx()
        val top = 8.dp.toPx(); val bottom = 22.dp.toPx()
        val w = size.width - left - right
        val h = size.height - top - bottom
        val range = (yMax - yMin).takeIf { it > 0f } ?: 1f
        fun yOf(v: Float) = top + h * (1f - (v.coerceIn(yMin, yMax) - yMin) / range)
        val native = drawContext.canvas.nativeCanvas

        band?.let {
            drawRect(bandColor, Offset(left + w * it.start, top), Size(w * (it.endInclusive - it.start), h))
        }
        var v = yMin
        while (v <= yMax + yStep * 0.01f) {
            val yy = yOf(v)
            drawLine(GridColor, Offset(left, yy), Offset(left + w, yy), 1f)
            native.drawText(yFormat(v), left - 6.dp.toPx(), yy + yPaint.textSize * 0.35f, yPaint)
            v += yStep
        }
        series.forEach { s ->
            val n = s.values.size
            if (n >= 2) {
                val path = Path()
                s.values.forEachIndexed { i, value ->
                    val x = left + w * i / (n - 1)
                    val yy = yOf(value)
                    if (i == 0) path.moveTo(x, yy) else path.lineTo(x, yy)
                }
                drawPath(path, s.color, style = Stroke(s.width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        drawLine(AxisColor, Offset(left, top + h), Offset(left + w, top + h), 1.5f)
        val nL = xLabels.size
        val pad = 16.dp.toPx()
        xLabels.forEachIndexed { i, l ->
            val x = if (nL == 1) left + w / 2 else left + w * i / (nL - 1)
            native.drawText(l, x.coerceIn(left + pad, left + w - pad), size.height - 4.dp.toPx(), xPaint)
        }
    }
}

/**
 * Stacked bars; each bar is an array of segment values (bottom → top).
 * Tapping a bar selects it.
 */
@Composable
fun StackedBarChart(
    bars: List<FloatArray>,
    colors: List<Color>,
    xLabels: List<String>,
    yMax: Float,
    yStep: Float,
    modifier: Modifier = Modifier,
    selected: Int? = null,
    onSelect: (Int) -> Unit = {},
    yFormat: (Float) -> String = { it.roundToInt().toString() },
) {
    val (yPaint, xPaint) = rememberPaints()
    val select by rememberUpdatedState(onSelect)
    val leftDp = 36.dp; val rightDp = 8.dp
    Canvas(
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(bars.size) {
                detectTapGestures { pos ->
                    val left = leftDp.toPx()
                    val w = size.width - left - rightDp.toPx()
                    val idx = ((pos.x - left) / w * bars.size).toInt()
                    if (idx in bars.indices) select(idx)
                }
            }
    ) {
        val left = leftDp.toPx(); val right = rightDp.toPx()
        val top = 8.dp.toPx(); val bottom = 22.dp.toPx()
        val w = size.width - left - right
        val h = size.height - top - bottom
        val base = top + h
        val native = drawContext.canvas.nativeCanvas

        var v = 0f
        while (v <= yMax + yStep * 0.01f) {
            val yy = base - h * v / yMax
            drawLine(GridColor, Offset(left, yy), Offset(left + w, yy), 1f)
            native.drawText(yFormat(v), left - 6.dp.toPx(), yy + yPaint.textSize * 0.35f, yPaint)
            v += yStep
        }
        val n = bars.size.coerceAtLeast(1)
        val slot = w / n
        val bw = (slot * 0.68f).coerceAtMost(48.dp.toPx())
        bars.forEachIndexed { i, segs ->
            val x0 = left + slot * i + (slot - bw) / 2f
            val alpha = if (selected == null || selected == i) 1f else 0.35f
            var acc = 0f
            segs.forEachIndexed { k, value ->
                val segH = h * (value / yMax).coerceAtLeast(0f)
                drawRect(colors[k % colors.size].copy(alpha = alpha), Offset(x0, base - acc - segH), Size(bw, segH))
                acc += segH
            }
        }
        drawLine(AxisColor, Offset(left, base), Offset(left + w, base), 1.5f)
        val step = ceil(xLabels.size / 6f).toInt().coerceAtLeast(1)
        xLabels.forEachIndexed { i, l ->
            if ((xLabels.size - 1 - i) % step == 0) {
                native.drawText(l, left + slot * i + slot / 2f, size.height - 4.dp.toPx(), xPaint)
            }
        }
    }
}
