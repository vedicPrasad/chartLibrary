package com.vedicrishiastro.vedicchart.compose

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vedicrishiastro.vedicchart.ChartStyle
import com.vedicrishiastro.vedicchart.ChartTheme
import com.vedicrishiastro.vedicchart.ZodiacHouse
import java.util.ArrayDeque
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val SmoothLineEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val SmoothRevealEasing = CubicBezierEasing(0.2f, 0.92f, 0.18f, 1f)
private const val MinYawDegrees = -85f
private const val MaxYawDegrees = 85f
private const val MinPitchDegrees = -85f
private const val MaxPitchDegrees = 85f
private const val MaxPlanetsPerHouse = 10
private const val HouseNumberTextSizeBoost = 2f
private val IsoSurfaceColor: Int = Color.rgb(250, 246, 236)
private val IsoEdgeColor: Int = Color.rgb(43, 35, 28)
private val IsoSignExtrudeColor: Int = Color.rgb(116, 82, 42)
private const val EastBlockGapRatio = 0f

data class VedicPlanetSelection(
    val houseIndex: Int,
    val planetIndex: Int,
    val planetName: String,
    val planetLabel: String,
    val house: ZodiacHouse,
)

@Composable
fun VedicChart(
    houses: List<ZodiacHouse>,
    modifier: Modifier = Modifier,
    chartStyle: ChartStyle = ChartStyle.NORTH,
    chartTheme: ChartTheme = ChartTheme.light(),
    animate: Boolean = true,
    animationDurationMillis: Int = 1200,
    usePlanetIcons: Boolean = false,
    chartSizeInset: Dp = 5.dp,
    selectedHouseIndex: Int? = null,
    onHouseSelected: ((houseIndex: Int, house: ZodiacHouse) -> Unit)? = null,
    onPlanetSelected: ((VedicPlanetSelection) -> Unit)? = null,
) {
    val chartSide = (LocalConfiguration.current.screenWidthDp.dp - chartSizeInset).coerceAtLeast(0.dp)
    val density = LocalDensity.current.density
    var internalSelectedHouseIndex by remember { mutableStateOf<Int?>(null) }
    var rotationYawDegrees by remember { mutableStateOf(0f) }
    var rotationPitchDegrees by remember { mutableStateOf(0f) }
    val activeSelectedHouseIndex = selectedHouseIndex ?: internalSelectedHouseIndex
    var incomingSelectedHouseIndex by remember { mutableStateOf<Int?>(activeSelectedHouseIndex) }
    var outgoingSelectedHouseIndex by remember { mutableStateOf<Int?>(null) }
    val incomingLiftProgress = remember { Animatable(if (activeSelectedHouseIndex != null) 1f else 0f) }
    val outgoingLiftProgress = remember { Animatable(0f) }
    val lineReveal = remember { Animatable(if (animate) 0f else 1f) }
    val textReveal = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(activeSelectedHouseIndex, chartStyle) {
        if (incomingSelectedHouseIndex == activeSelectedHouseIndex) {
            incomingLiftProgress.animateTo(
                targetValue = if (activeSelectedHouseIndex != null) 1f else 0f,
                animationSpec = tween(durationMillis = 420, easing = SmoothRevealEasing),
            )
            return@LaunchedEffect
        }

        val previousHouseIndex = incomingSelectedHouseIndex
        val previousProgress = incomingLiftProgress.value
        outgoingSelectedHouseIndex = previousHouseIndex
        incomingSelectedHouseIndex = activeSelectedHouseIndex
        coroutineScope {
            if (previousHouseIndex != null && previousProgress > 0f) {
                launch {
                    outgoingLiftProgress.snapTo(previousProgress)
                    outgoingLiftProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 440, easing = SmoothRevealEasing),
                    )
                    outgoingSelectedHouseIndex = null
                }
            } else {
                outgoingLiftProgress.snapTo(0f)
                outgoingSelectedHouseIndex = null
            }
            if (activeSelectedHouseIndex != null) {
                launch {
                    incomingLiftProgress.snapTo(0f)
                    incomingLiftProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 460, easing = SmoothRevealEasing),
                    )
                }
            } else {
                incomingLiftProgress.snapTo(0f)
            }
        }
    }
    LaunchedEffect(houses, chartStyle, animate, animationDurationMillis) {
        if (animate) {
            coroutineScope {
                lineReveal.snapTo(0f)
                textReveal.snapTo(0f)
                launch {
                    lineReveal.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = (animationDurationMillis * 0.62f).toInt().coerceAtLeast(420),
                            easing = SmoothLineEasing,
                        ),
                    )
                }
                launch {
                    textReveal.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = (animationDurationMillis * 0.46f).toInt().coerceAtLeast(420),
                            delayMillis = (animationDurationMillis * 0.62f).toInt(),
                            easing = SmoothRevealEasing,
                        ),
                    )
                }
            }
        } else {
            lineReveal.snapTo(1f)
            textReveal.snapTo(1f)
        }
    }

    val progress = lineReveal.value
    val textProgress = textReveal.value
    val selectedHouseLifts = listOfNotNull(
        outgoingSelectedHouseIndex?.let {
            SelectedHouseLift(visualHouseIndexForOrFallback(chartStyle, houses.getOrNull(it), it), outgoingLiftProgress.value)
        },
        incomingSelectedHouseIndex?.let {
            SelectedHouseLift(visualHouseIndexForOrFallback(chartStyle, houses.getOrNull(it), it), incomingLiftProgress.value)
        },
    ).filter { it.progress > 0f }
    Canvas(
        modifier = modifier
            .requiredSize(chartSide)
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    rotationYawDegrees = clampYaw(rotationYawDegrees + dragAmount.x * 0.42f)
                    rotationPitchDegrees = clampPitch(rotationPitchDegrees - dragAmount.y * 0.42f)
                }
            }
            .pointerInput(houses, chartStyle, chartTheme, density, usePlanetIcons, selectedHouseLifts, rotationYawDegrees, rotationPitchDegrees) {
            detectTapGestures { offset ->
                val bounds = chartBounds(size.width.toFloat(), size.height.toFloat(), density)
                val projection = IsoProjection(bounds, rotationYawDegrees, rotationPitchDegrees)
                val planetSelection = hitTestIsoPlanet(
                    offset = offset,
                    bounds = bounds,
                    houses = houses,
                    chartStyle = chartStyle,
                    chartTheme = chartTheme,
                    density = density,
                    usePlanetIcons = usePlanetIcons,
                    selectedHouseLifts = selectedHouseLifts,
                    projection = projection,
                )
                if (planetSelection != null) {
                    if (onPlanetSelected != null) {
                        onPlanetSelected.invoke(planetSelection)
                        return@detectTapGestures
                    }
                }
                val visualHouseIndex = hitTestIsoHouse(
                    offset = offset,
                    bounds = bounds,
                    chartStyle = chartStyle,
                    selectedHouseLifts = selectedHouseLifts,
                    projection = projection,
                )
                val houseIndex = visualHouseIndex?.let { dataHouseIndexForVisualHouseIndex(chartStyle, houses, it) }
                if (houseIndex != null && houseIndex in houses.indices) {
                    if (selectedHouseIndex == null) {
                        internalSelectedHouseIndex = if (internalSelectedHouseIndex == houseIndex) {
                            null
                        } else {
                            houseIndex
                        }
                    }
                    onHouseSelected?.invoke(houseIndex, houses[houseIndex])
                }
            }
        },
    ) {
        drawVedicChart(
            houses = houses,
            chartStyle = chartStyle,
            chartTheme = chartTheme,
            lineReveal = progress,
            textProgress = textProgress,
            selectedHouseLifts = selectedHouseLifts,
            rotationYawDegrees = rotationYawDegrees,
            rotationPitchDegrees = rotationPitchDegrees,
            usePlanetIcons = usePlanetIcons,
        )
    }
}

private fun DrawScope.drawVedicChart(
    houses: List<ZodiacHouse>,
    chartStyle: ChartStyle,
    chartTheme: ChartTheme,
    lineReveal: Float,
    textProgress: Float,
    selectedHouseLifts: List<SelectedHouseLift>,
    rotationYawDegrees: Float,
    rotationPitchDegrees: Float,
    usePlanetIcons: Boolean,
) {
    val bounds = chartBounds(size.width, size.height, density)
    val paints = ChartPaints(chartTheme)

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        drawIsometricVedicChart(
            canvas = nativeCanvas,
            bounds = bounds,
            houses = houses,
            paints = paints,
            theme = chartTheme,
            chartStyle = chartStyle,
            lineReveal = lineReveal,
            textProgress = textProgress,
            selectedHouseLifts = selectedHouseLifts,
            rotationYawDegrees = rotationYawDegrees,
            rotationPitchDegrees = rotationPitchDegrees,
            usePlanetIcons = usePlanetIcons,
        )
    }
}

private fun DrawScope.ChartPaints(theme: ChartTheme): ChartPaints {
    val signTextSize = theme.signTextSizeSp.sp.toPx()
    val planetTextSize = theme.planetTextSizeSp.sp.toPx()
    return ChartPaints(
        fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = theme.backgroundColor
        },
        line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = theme.borderWidth * density
            color = theme.borderColor
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        },
        grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = theme.borderWidth * density
            color = gridLineColor(theme.backgroundColor)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        },
        accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = theme.borderWidth * 1.4f * density
            color = theme.accentColor
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        },
        signText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = theme.signTextColor
            textSize = signTextSize
        },
        planetText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            color = theme.planetTextColor
            textSize = planetTextSize
        },
        signBaseTextSize = signTextSize,
        planetBaseTextSize = planetTextSize,
    )
}

private fun drawIsometricVedicChart(
    canvas: android.graphics.Canvas,
    bounds: RectF,
    houses: List<ZodiacHouse>,
    paints: ChartPaints,
    theme: ChartTheme,
    chartStyle: ChartStyle,
    lineReveal: Float,
    textProgress: Float,
    selectedHouseLifts: List<SelectedHouseLift>,
    rotationYawDegrees: Float,
    rotationPitchDegrees: Float,
    usePlanetIcons: Boolean,
) {
    val projection = IsoProjection(bounds, rotationYawDegrees, rotationPitchDegrees)
    val housesToDraw = buildIsoHouses(chartStyle, bounds, selectedHouseLifts, projection)
    val restingHouses = housesToDraw
        .filter { it.selectedProgress <= 0f }
        .sortedBy { it.depth }
    val liftedHouses = housesToDraw
        .filter { it.selectedProgress > 0f }
        .sortedBy { it.depth }
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.TRANSPARENT
    }
    canvas.drawRoundRect(bounds, outerCornerRadius(1f), outerCornerRadius(1f), backgroundPaint)
    if (chartStyle == ChartStyle.EAST) {
        drawEastCenterTile(canvas, bounds, projection, lineReveal, paints)
    }
    restingHouses.forEach { house ->
        drawIsoHousePrism(canvas, house, paints, theme, chartStyle, lineReveal)
    }
    if (textProgress > 0f) {
        drawIsoHouseTexts(
            canvas = canvas,
            bounds = bounds,
            houses = houses,
            paints = paints,
            theme = theme,
            chartStyle = chartStyle,
            selectedHouseLifts = selectedHouseLifts,
            projection = projection,
            textProgress = textProgress,
            usePlanetIcons = usePlanetIcons,
            houseFilter = { houseIndex ->
                liftProgressFor(visualHouseIndexForOrFallback(chartStyle, houses.getOrNull(houseIndex), houseIndex), selectedHouseLifts) <= 0f
            },
        )
    }
    liftedHouses.forEach { house ->
        drawIsoHousePrism(canvas, house, paints, theme, chartStyle, lineReveal)
    }
    if (textProgress > 0f) {
        drawIsoHouseTexts(
            canvas = canvas,
            bounds = bounds,
            houses = houses,
            paints = paints,
            theme = theme,
            chartStyle = chartStyle,
            selectedHouseLifts = selectedHouseLifts,
            projection = projection,
            textProgress = textProgress,
            usePlanetIcons = usePlanetIcons,
            houseFilter = { houseIndex ->
                liftProgressFor(visualHouseIndexForOrFallback(chartStyle, houses.getOrNull(houseIndex), houseIndex), selectedHouseLifts) > 0f
            },
        )
    }
}

private fun drawEastCenterTile(
    canvas: android.graphics.Canvas,
    bounds: RectF,
    projection: IsoProjection,
    reveal: Float,
    paints: ChartPaints,
) {
    val alpha = smoothStep(reveal)
    if (alpha <= 0f) return
    val third = bounds.width() / 3f
    val centerRect = RectF(
        bounds.left + third,
        bounds.top + third,
        bounds.left + third * 2f,
        bounds.top + third * 2f,
    )
    val flatPoints = insetPolygon(centerRect.corners(), bounds.width() * EastBlockGapRatio)
    val topPoints = flatPoints.map { projection.project(it, z = projection.blockHeight) }
    val topPath = pointsToPath(topPoints)
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = withAlpha(IsoSurfaceColor, (255 * alpha).toInt())
    }
    canvas.drawPath(topPath, fillPaint)
}

private fun buildIsoHouses(
    chartStyle: ChartStyle,
    bounds: RectF,
    selectedHouseLifts: List<SelectedHouseLift>,
    projection: IsoProjection,
): List<IsoHouse> {
    return (0 until 12).mapNotNull { houseIndex ->
        val points = when (chartStyle) {
            ChartStyle.EAST -> insetPolygon(
                eastHouseCornerPoints(houseIndex, bounds),
                bounds.width() * EastBlockGapRatio,
            )

            else -> {
                val path = selectedHousePath(chartStyle, houseIndex, bounds) ?: return@mapNotNull null
                samplePath(path, 64)
            }
        }.ifEmpty { return@mapNotNull null }
        val selectedLift = liftProgressFor(houseIndex, selectedHouseLifts)
        val height = projection.blockHeight
        val extraZ = projection.selectedLiftHeight * selectedLift
        val basePoints = points.map { projection.project(it, z = extraZ) }
        val topPoints = points.map { projection.project(it, z = height + extraZ) }
        val cornerPoints = houseCornerPoints(chartStyle, houseIndex, bounds)
        val connectedEastBlock = chartStyle == ChartStyle.EAST && selectedLift <= 0f
        IsoHouse(
            houseIndex = houseIndex,
            basePoints = basePoints,
            topPoints = topPoints,
            cornerBasePoints = cornerPoints.map { projection.project(it, z = extraZ) },
            cornerTopPoints = cornerPoints.map { projection.project(it, z = height + extraZ) },
            wallEdgeVisible = if (connectedEastBlock) eastOuterEdgeVisibility(points, bounds) else List(points.size) { true },
            depth = basePoints.map { it.y }.average().toFloat(),
            selectedProgress = selectedLift,
            extraZ = extraZ,
        )
    }
}

private fun drawIsoHousePrism(
    canvas: android.graphics.Canvas,
    house: IsoHouse,
    paints: ChartPaints,
    theme: ChartTheme,
    chartStyle: ChartStyle,
    reveal: Float,
) {
    val alpha = smoothStep(reveal)
    if (alpha <= 0f) return
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb((18 * alpha).toInt(), 0, 0, 0)
    }
    val topColor = isoHouseTopColor(theme, house.houseIndex, house.selectedProgress)
    val topPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = withAlpha(topColor, (255 * alpha).toInt())
    }
    val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = paints.grid.strokeWidth
        color = withAlpha(IsoEdgeColor, (230 * alpha).toInt())
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val basePath = pointsToPath(house.basePoints)
    val topPath = pointsToPath(house.topPoints)
    val shadowPath = Path(basePath).apply {
        transform(Matrix().apply { setTranslate(0f, 18f) })
    }
    canvas.drawPath(shadowPath, shadowPaint)

    for (index in house.basePoints.indices) {
        if (house.wallEdgeVisible.getOrNull(index) == false) continue
        val next = (index + 1) % house.basePoints.size
        val topA = house.topPoints[index]
        val topB = house.topPoints[next]
        val baseB = house.basePoints[next]
        val baseA = house.basePoints[index]
        val wallPath = Path().apply {
            moveTo(topA.x, topA.y)
            lineTo(topB.x, topB.y)
            lineTo(baseB.x, baseB.y)
            lineTo(baseA.x, baseA.y)
            close()
        }
        val centerX = (topA.x + topB.x + baseA.x + baseB.x) / 4f
        val warmSide = centerX >= topPathBoundsCenterX(house.topPoints)
        val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            val sideColor = if (warmSide) {
                mixColors(topColor, Color.BLACK, 0.24f)
            } else {
                mixColors(topColor, Color.BLACK, 0.38f)
            }
            color = withAlpha(sideColor, (255 * alpha).toInt())
        }
        canvas.drawPath(wallPath, wallPaint)
    }
    canvas.drawPath(topPath, topPaint)
    canvas.drawPath(topPath, edgePaint)
    drawIsoHouseCornerSideEdges(canvas, house, chartStyle, edgePaint)
}

private fun drawIsoHouseCornerSideEdges(
    canvas: android.graphics.Canvas,
    house: IsoHouse,
    chartStyle: ChartStyle,
    edgePaint: Paint,
) {
    val visibleEdgeFloor = topPathBoundsCenterY(house.basePoints)
    house.cornerBasePoints.indices.forEach { index ->
        if (isNorthCenterCornerEdge(chartStyle, house.houseIndex, index)) return@forEach
        val base = house.cornerBasePoints[index]
        val top = house.cornerTopPoints.getOrNull(index) ?: return@forEach
        if (base.y < visibleEdgeFloor) return@forEach
        canvas.drawLine(base.x, base.y, top.x, top.y, edgePaint)
    }
}

private fun isNorthCenterCornerEdge(chartStyle: ChartStyle, houseIndex: Int, cornerIndex: Int): Boolean {
    if (chartStyle != ChartStyle.NORTH) return false
    return when (houseIndex) {
        0, 3, 6 -> cornerIndex == 2
        9 -> cornerIndex == 3
        else -> false
    }
}

private fun drawIsoHouseTexts(
    canvas: android.graphics.Canvas,
    bounds: RectF,
    houses: List<ZodiacHouse>,
    paints: ChartPaints,
    theme: ChartTheme,
    chartStyle: ChartStyle,
    selectedHouseLifts: List<SelectedHouseLift>,
    projection: IsoProjection,
    textProgress: Float,
    usePlanetIcons: Boolean,
    houseFilter: (Int) -> Boolean = { true },
) {
    val count = min(houses.size, 12)
    for (houseIndex in 0 until count) {
        if (!houseFilter(houseIndex)) continue
        val visualHouseIndex = visualHouseIndexFor(chartStyle, houses[houseIndex], houseIndex)
        val liftProgress = liftProgressFor(visualHouseIndex, selectedHouseLifts)
        val height = projection.blockHeight + projection.selectedLiftHeight * liftProgress
        val houseLabel = houseLabelText(chartStyle, houses[houseIndex], houseIndex, theme)
        val labelBoxes = labelBoxesFor(chartStyle, visualHouseIndex, bounds, paints.planetText.textSize, houseLabel.isNotBlank()) ?: continue
        drawIsoSign(canvas, houseLabel, labelBoxes.signBox, paints, projection, height, textProgress)
        val planetPlacements = drawIsoHousePlanets(
            canvas = canvas,
            house = houses[houseIndex],
            houseIndex = visualHouseIndex,
            bounds = bounds,
            signBox = labelBoxes.signBox,
            paints = paints,
            projection = projection,
            height = height,
            textProgress = textProgress,
            chartStyle = chartStyle,
            usePlanetIcons = usePlanetIcons,
        )
        if (chartStyle == ChartStyle.NORTH && houseIndex == 0) {
            drawIsoAscendantLabel(
                canvas = canvas,
                houseIndex = visualHouseIndex,
                bounds = bounds,
                signBox = labelBoxes.signBox,
                planetPlacements = planetPlacements,
                paints = paints,
                projection = projection,
                height = height,
                textProgress = textProgress,
            )
        }
    }
}

private fun drawIsoSign(
    canvas: android.graphics.Canvas,
    signText: String,
    box: RectF,
    paints: ChartPaints,
    projection: IsoProjection,
    height: Float,
    textProgress: Float,
) {
    val text = fitSingleLine(
        signText,
        paints.signText,
        paints.signBaseTextSize * 0.86f + HouseNumberTextSizeBoost,
        max(1f, box.width() - signLinePadding(box) * 2f),
        minTextSize = 7f,
    )
    if (text.isBlank()) return
    val point = projection.project(Offset(box.centerX(), box.centerY()), z = height + 8f)
    val baselineY = point.y + centeredTextOffset(paints.signText)
    val extrudeOffset = max(1.5f, paints.signText.textSize * 0.08f)
    val extrudePaint = Paint(paints.signText).apply {
        color = IsoSignExtrudeColor
        alpha = (255 * textProgress).toInt().coerceIn(0, 255)
    }
    val highlightPaint = Paint(paints.signText).apply {
        color = mixColors(Color.WHITE, paints.signText.color, 0.2f)
        alpha = (230 * textProgress).toInt().coerceIn(0, 255)
    }
    setPaintAlpha(paints.signText, textProgress)
    canvas.drawText(text, point.x + extrudeOffset, baselineY + extrudeOffset, extrudePaint)
    canvas.drawText(text, point.x - extrudeOffset * 0.35f, baselineY - extrudeOffset * 0.4f, highlightPaint)
    canvas.drawText(text, point.x, baselineY, paints.signText)
    setPaintAlpha(paints.signText, 1f)
}

private fun drawIsoPlanets(
    canvas: android.graphics.Canvas,
    house: ZodiacHouse,
    box: RectF,
    paints: ChartPaints,
    maxRows: Int,
    projection: IsoProjection,
    height: Float,
    textProgress: Float,
    usePlanetIcons: Boolean,
) {
    val tokens = planetLabels(house, usePlanetIcons)
    if (tokens.isEmpty()) return
    val layout = buildPlanetGrid(tokens, box, paints.planetText, crowdedPlanetTextSize(paints.planetBaseTextSize * 0.82f, tokens.size), maxRows)
    if (layout.tokens.isEmpty()) return
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = withAlpha(mixColors(paints.planetText.color, paints.fill.color, 0.84f), (92 * textProgress).toInt())
    }
    setPaintAlpha(paints.planetText, textProgress)
    layout.tokens.forEachIndexed { index, token ->
        val row = index / layout.columns
        val column = index % layout.columns
        val flatX = box.left + layout.cellWidth * column + layout.cellWidth / 2f
        val flatY = box.top + layout.cellHeight * row + layout.cellHeight / 2f
        val point = projection.project(Offset(flatX, flatY), z = height + 10f)
        val textWidth = paints.planetText.measureText(token)
        val pill = RectF(
            point.x - textWidth / 2f - 7f,
            point.y - paints.planetText.textSize * 0.62f,
            point.x + textWidth / 2f + 7f,
            point.y + paints.planetText.textSize * 0.52f,
        )
        canvas.drawRoundRect(pill, pill.height() * 0.48f, pill.height() * 0.48f, pillPaint)
        canvas.drawText(token, point.x, point.y + centeredTextOffset(paints.planetText), paints.planetText)
    }
    setPaintAlpha(paints.planetText, 1f)
}

private fun drawIsoHousePlanets(
    canvas: android.graphics.Canvas,
    house: ZodiacHouse,
    houseIndex: Int,
    bounds: RectF,
    signBox: RectF,
    paints: ChartPaints,
    projection: IsoProjection,
    height: Float,
    textProgress: Float,
    chartStyle: ChartStyle,
    usePlanetIcons: Boolean,
): List<PlanetPlacement> {
    val tokens = planetLabels(house, usePlanetIcons).take(MaxPlanetsPerHouse)
    if (tokens.isEmpty()) return emptyList()
    val placements = buildHousePlanetPlacements(
        tokens = tokens,
        chartStyle = chartStyle,
        houseIndex = houseIndex,
        bounds = bounds,
        signBox = signBox,
        paint = paints.planetText,
        baseTextSize = crowdedPlanetTextSize(paints.planetBaseTextSize * 0.82f, tokens.size),
        maxRows = maxPlanetRowsFor(chartStyle, houseIndex),
    )
    if (placements.isEmpty()) return emptyList()
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = withAlpha(mixColors(paints.planetText.color, paints.fill.color, 0.84f), (92 * textProgress).toInt())
    }
    setPaintAlpha(paints.planetText, textProgress)
    placements.forEach { placement ->
        val point = projection.project(placement.center, z = height + 10f)
        val textWidth = paints.planetText.measureText(placement.token)
        val pill = RectF(
            point.x - textWidth / 2f - 7f,
            point.y - paints.planetText.textSize * 0.62f,
            point.x + textWidth / 2f + 7f,
            point.y + paints.planetText.textSize * 0.52f,
        )
        canvas.drawRoundRect(pill, pill.height() * 0.48f, pill.height() * 0.48f, pillPaint)
        canvas.drawText(placement.token, point.x, point.y + centeredTextOffset(paints.planetText), paints.planetText)
    }
    setPaintAlpha(paints.planetText, 1f)
    return placements
}

private fun drawIsoAscendantLabel(
    canvas: android.graphics.Canvas,
    houseIndex: Int,
    bounds: RectF,
    signBox: RectF,
    planetPlacements: List<PlanetPlacement>,
    paints: ChartPaints,
    projection: IsoProjection,
    height: Float,
    textProgress: Float,
) {
    val ascText = "ASC"
    val textSize = max(7f, paints.signBaseTextSize * 0.82f)
    paints.signText.textSize = textSize
    val point = ascendantPlacement(
        label = ascText,
        houseIndex = houseIndex,
        bounds = bounds,
        signBox = signBox,
        planetPlacements = planetPlacements,
        paint = paints.signText,
    ) ?: return
    val projected = projection.project(point, z = height + 12f)
    val extrudeOffset = max(1.8f, paints.signText.textSize * 0.12f)
    val extrudePaint = Paint(paints.signText).apply {
        color = IsoSignExtrudeColor
        alpha = (255 * textProgress).toInt().coerceIn(0, 255)
    }
    val highlightPaint = Paint(paints.signText).apply {
        color = mixColors(Color.WHITE, paints.signText.color, 0.18f)
        alpha = (230 * textProgress).toInt().coerceIn(0, 255)
    }
    setPaintAlpha(paints.signText, textProgress)
    canvas.drawText(ascText, projected.x + extrudeOffset, projected.y + centeredTextOffset(paints.signText) + extrudeOffset, extrudePaint)
    canvas.drawText(ascText, projected.x - extrudeOffset * 0.35f, projected.y + centeredTextOffset(paints.signText) - extrudeOffset * 0.4f, highlightPaint)
    canvas.drawText(ascText, projected.x, projected.y + centeredTextOffset(paints.signText), paints.signText)
    setPaintAlpha(paints.signText, 1f)
}

private fun labelBoxesFor(
    chartStyle: ChartStyle,
    houseIndex: Int,
    bounds: RectF,
    planetTextSize: Float,
    hasSignLabel: Boolean = true,
): LabelBoxes? {
    return when (chartStyle) {
        ChartStyle.NORTH -> northSlots().getOrNull(houseIndex)?.let { slot ->
            val signBox = slot.signBox.toRect(bounds)
            LabelBoxes(
                signBox = signBox,
                planetBox = slot.planetBox.toRect(bounds).withInset(bounds.width() * 0.012f)
                    .let { planetBox ->
                        if (hasSignLabel) {
                            planetBoxAvoidingSign(planetBox, signBox, planetSignGap(bounds))
                        } else {
                            planetBox
                        }
                    },
            )
        }
        ChartStyle.SOUTH,
        ChartStyle.EAST -> houseHitBoxes(chartStyle).getOrNull(houseIndex)?.toRect(bounds)?.let { box ->
            val inset = 3f
            val signHeight = if (hasSignLabel) min(box.height() * 0.34f, planetTextSize * 1.7f) else 0f
            val signBox = RectF(box.left + inset, box.top + inset, box.right - inset, box.top + inset + signHeight)
            LabelBoxes(
                signBox = signBox,
                planetBox = RectF(
                    box.left + inset,
                    if (hasSignLabel) signBox.bottom + planetSignGap(box) else box.top + inset,
                    box.right - inset,
                    box.bottom - inset,
                ).let { planetBox ->
                    if (hasSignLabel) {
                        planetBoxAvoidingSign(planetBox, signBox, planetSignGap(box))
                    } else {
                        planetBox
                    }
                },
            )
        }
    }
}

private fun pointsToPath(points: List<Offset>): Path {
    return Path().apply {
        if (points.isEmpty()) return@apply
        moveTo(points.first().x, points.first().y)
        for (index in 1 until points.size) {
            lineTo(points[index].x, points[index].y)
        }
        close()
    }
}

private fun insetPolygon(points: List<Offset>, amount: Float): List<Offset> {
    if (points.size < 3 || amount <= 0f) return points
    val centerX = points.map { it.x }.average().toFloat()
    val centerY = points.map { it.y }.average().toFloat()
    return points.map { point ->
        val dx = centerX - point.x
        val dy = centerY - point.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        if (distance <= amount) {
            point
        } else {
            val ratio = amount / distance
            Offset(
                x = point.x + dx * ratio,
                y = point.y + dy * ratio,
            )
        }
    }
}

private fun eastOuterEdgeVisibility(points: List<Offset>, bounds: RectF): List<Boolean> {
    return points.indices.map { index ->
        val next = (index + 1) % points.size
        isOuterEastEdge(points[index], points[next], bounds)
    }
}

private fun isOuterEastEdge(start: Offset, end: Offset, bounds: RectF): Boolean {
    val tolerance = max(bounds.width(), bounds.height()) * 0.001f
    val verticalOuter = kotlin.math.abs(start.x - end.x) <= tolerance &&
        (kotlin.math.abs(start.x - bounds.left) <= tolerance || kotlin.math.abs(start.x - bounds.right) <= tolerance)
    val horizontalOuter = kotlin.math.abs(start.y - end.y) <= tolerance &&
        (kotlin.math.abs(start.y - bounds.top) <= tolerance || kotlin.math.abs(start.y - bounds.bottom) <= tolerance)
    return verticalOuter || horizontalOuter
}

private fun topPathBoundsCenterX(points: List<Offset>): Float {
    if (points.isEmpty()) return 0f
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    return (minX + maxX) / 2f
}

private fun topPathBoundsCenterY(points: List<Offset>): Float {
    if (points.isEmpty()) return 0f
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    return (minY + maxY) / 2f
}

private fun isoHouseTopColor(theme: ChartTheme, houseIndex: Int, selectedProgress: Float): Int {
    val baseColor = theme.getHouseColor(houseIndex)
    if (selectedProgress <= 0f) return baseColor
    return mixColors(baseColor, theme.selectedHouseColor, selectedProgress.coerceIn(0f, 1f))
}

private class IsoProjection(
    private val bounds: RectF,
    yawDegrees: Float,
    pitchDegrees: Float,
) {
    val blockHeight: Float = bounds.width() * 0.11f
    val selectedLiftHeight: Float = bounds.width() * 0.17f
    private val yaw = yawDegrees * PI.toFloat() / 180f
    private val pitch = pitchDegrees * PI.toFloat() / 180f
    private val cosYaw = cos(yaw)
    private val sinYaw = sin(yaw)
    private val cosPitch = cos(pitch)
    private val sinPitch = sin(pitch)
    private val cameraDistance = bounds.width() * 3.2f
    private val perspectiveStrength = 0.16f

    fun project(point: Offset, z: Float): Offset {
        val x0 = point.x - bounds.centerX()
        val y0 = point.y - bounds.centerY()
        val z0 = z

        val y1 = y0 * cosPitch - z0 * sinPitch
        val z1 = y0 * sinPitch + z0 * cosPitch
        val x2 = x0 * cosYaw + z1 * sinYaw
        val z2 = -x0 * sinYaw + z1 * cosYaw
        val perspective = cameraDistance / (cameraDistance - z2 * perspectiveStrength)

        return Offset(
            x = bounds.centerX() + x2 * perspective,
            y = bounds.centerY() + y1 * perspective,
        )
    }
}


private fun drawHouseTexts(
    canvas: android.graphics.Canvas,
    bounds: RectF,
    boxes: Array<FloatArray>,
    houses: List<ZodiacHouse>,
    paints: ChartPaints,
    theme: ChartTheme,
    textProgress: Float,
    selectedHouseLifts: List<SelectedHouseLift>,
    chartStyle: ChartStyle,
    usePlanetIcons: Boolean,
    houseFilter: ((Int) -> Boolean)? = null,
) {
    val count = min(min(boxes.size, houses.size), 12)
    for (index in 0 until count) {
        if (houseFilter != null && !houseFilter(index)) continue
        val box = boxes[index]
        val rect = RectF(
            bounds.left + bounds.width() * box[0],
            bounds.top + bounds.height() * box[1],
            bounds.left + bounds.width() * box[2],
            bounds.top + bounds.height() * box[3],
        )
        val liftProgress = liftProgressFor(index, selectedHouseLifts)
        drawHouse(
            canvas,
            houses[index],
            index,
            rect,
            paints,
            theme,
            textProgress,
            liftProgress,
            textLiftTransform(chartStyle, index, bounds, liftProgress),
            usePlanetIcons,
        )
    }
}

private fun drawHouse(
    canvas: android.graphics.Canvas,
    house: ZodiacHouse,
    houseIndex: Int,
    box: RectF,
    paints: ChartPaints,
    theme: ChartTheme,
    textProgress: Float,
    selectedLiftProgress: Float,
    liftTransform: TextLiftTransform?,
    usePlanetIcons: Boolean,
) {
    val inset = 3f
    val signHeight = min(box.height() * 0.34f, paints.signText.textSize * 1.7f)
    val signBox = RectF(box.left + inset, box.top + inset, box.right - inset, box.top + signHeight)
    val planetBox = planetBoxAvoidingSign(
        planetBox = RectF(box.left + inset, signBox.bottom + planetSignGap(box), box.right - inset, box.bottom - inset),
        signBox = signBox,
        gap = planetSignGap(box),
    )

    drawSign(canvas, houseLabelText(chartStyle = ChartStyle.NORTH, house = house, houseIndex = houseIndex, theme = theme), signBox, paints, textProgress, selectedLiftProgress, liftTransform)
    drawPlanets(canvas, house, planetBox, paints, if (usesComplexPlanetLayout(houseIndex)) 4 else 2, textProgress, selectedLiftProgress, liftTransform, usePlanetIcons)
}

private fun drawSign(
    canvas: android.graphics.Canvas,
    signText: String,
    box: RectF,
    paints: ChartPaints,
    textProgress: Float,
    selectedLiftProgress: Float,
    liftTransform: TextLiftTransform?,
) {
    val text = fitSingleLine(
        signText,
        paints.signText,
        paints.signBaseTextSize + HouseNumberTextSizeBoost,
        max(1f, box.width() - signLinePadding(box) * 2f),
        minTextSize = 7f,
    )
    if (text.isBlank()) return

    val saveCount = canvas.save()
    canvas.clipRect(liftedTextClip(box, selectedLiftProgress, liftTransform))
    applyLift(canvas, box, textProgress, liftTransform)
    setPaintAlpha(paints.signText, textProgress)
    canvas.drawText(text, box.centerX(), box.centerY() + centeredTextOffset(paints.signText), paints.signText)
    setPaintAlpha(paints.signText, 1f)
    canvas.restoreToCount(saveCount)
}

private fun drawPlanets(
    canvas: android.graphics.Canvas,
    house: ZodiacHouse,
    box: RectF,
    paints: ChartPaints,
    maxRows: Int,
    textProgress: Float,
    selectedLiftProgress: Float,
    liftTransform: TextLiftTransform?,
    usePlanetIcons: Boolean,
) {
    if (box.width() <= 1f || box.height() <= 1f) return

    val tokens = planetLabels(house, usePlanetIcons)
    val layout = buildPlanetGrid(tokens, box, paints.planetText, crowdedPlanetTextSize(paints.planetBaseTextSize, tokens.size), maxRows)
    if (layout.tokens.isEmpty()) return

    val saveCount = canvas.save()
    canvas.clipRect(liftedTextClip(box, selectedLiftProgress, liftTransform))
    applyLift(canvas, box, textProgress, liftTransform)
    val planetTargetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = withAlpha(mixColors(paints.planetText.color, paints.fill.color, 0.86f), (92 * textProgress).toInt())
    }
    setPaintAlpha(paints.planetText, textProgress)
    for (index in layout.tokens.indices) {
        val row = index / layout.columns
        val column = index % layout.columns
        val x = box.left + layout.cellWidth * column + layout.cellWidth / 2f
        val y = box.top + layout.cellHeight * row + layout.cellHeight / 2f
        val targetRect = planetTokenRect(box, layout, row, column, layout.tokens[index], paints.planetText)
        val radius = planetTokenRadius(targetRect)
        canvas.drawRoundRect(targetRect, radius, radius, planetTargetPaint)
        canvas.drawText(
            layout.tokens[index],
            x,
            y + centeredTextOffset(paints.planetText),
            paints.planetText,
        )
    }
    setPaintAlpha(paints.planetText, 1f)
    canvas.restoreToCount(saveCount)
}

private fun buildPlanetGrid(
    tokens: List<String>,
    box: RectF,
    paint: Paint,
    baseTextSize: Float,
    maxRows: Int,
): PlanetGrid {
    if (tokens.isEmpty()) return PlanetGrid(emptyList(), columns = 1, cellWidth = box.width(), cellHeight = box.height())
    val maxWidth = max(1f, box.width())
    val maxHeight = max(1f, box.height())
    val minTextSize = 6f
    var best: PlanetGrid? = null
    var bestTextSize = 0f

    for (rows in 1..min(maxRows, tokens.size)) {
        val columns = kotlin.math.ceil(tokens.size / rows.toFloat()).toInt()
        val cellWidth = maxWidth / columns
        val cellHeight = maxHeight / rows
        var textSize = baseTextSize
        paint.textSize = textSize
        while (textSize >= minTextSize && !tokensFit(tokens, paint, cellWidth, cellHeight)) {
            textSize -= 1f
            paint.textSize = textSize
        }
        if (textSize >= minTextSize && tokensFit(tokens, paint, cellWidth, cellHeight) && textSize > bestTextSize) {
            bestTextSize = textSize
            best = PlanetGrid(tokens, columns, cellWidth, cellHeight)
        }
    }

    if (best != null) {
        paint.textSize = bestTextSize
        return best
    }

    val fallbackRows = min(maxRows, tokens.size)
    val fallbackColumns = kotlin.math.ceil(tokens.size / fallbackRows.toFloat()).toInt()
    val fallbackCellWidth = maxWidth / fallbackColumns
    val fallbackCellHeight = maxHeight / fallbackRows
    paint.textSize = min(minTextSize, fallbackCellHeight / 1.15f)
    return PlanetGrid(
        tokens.map { ellipsize(it, paint, fallbackCellWidth * 0.84f) },
        columns = fallbackColumns,
        cellWidth = fallbackCellWidth,
        cellHeight = fallbackCellHeight,
    )
}

private fun buildHousePlanetPlacements(
    tokens: List<String>,
    chartStyle: ChartStyle,
    houseIndex: Int,
    bounds: RectF,
    signBox: RectF,
    paint: Paint,
    baseTextSize: Float,
    maxRows: Int,
): List<PlanetPlacement> {
    val points = houseCornerPoints(chartStyle, houseIndex, bounds)
    if (tokens.isEmpty() || points.size < 3) return emptyList()

    val pathBounds = points.bounds()
    val minTextSize = 6f
    val horizontalInset = max(4f, bounds.width() * 0.012f)
    val verticalInset = max(4f, bounds.height() * 0.012f)
    val availableHeight = max(1f, pathBounds.height() - verticalInset * 2f)
    var textSize = baseTextSize

    while (textSize >= minTextSize) {
        paint.textSize = textSize
        if (tokens.size <= 2) {
            buildSparseHousePlanetPlacements(
                tokens = tokens,
                points = points,
                pathBounds = pathBounds,
                signBox = signBox,
                bounds = bounds,
                paint = paint,
                horizontalInset = horizontalInset,
                verticalInset = verticalInset,
            )?.let { return it }
        }

        val maxTokenWidth = tokens.maxOf { paint.measureText(it) }
        val minCellWidth = maxTokenWidth + max(10f, textSize * 0.9f)
        val rowHeight = textSize * 1.45f
        val rowCount = min(maxRows, max(1, (availableHeight / rowHeight).toInt()))
        val placements = mutableListOf<PlanetPlacement>()
        var nextToken = 0

        for (row in 0 until rowCount) {
            if (nextToken >= tokens.size) break
            val y = pathBounds.top + verticalInset + availableHeight * (row + 0.5f) / rowCount
            val span = houseSpanForRow(
                points = points,
                y = y,
                signBox = signBox,
                gap = planetSignGap(bounds),
                horizontalInset = horizontalInset,
            ) ?: continue
            val capacity = min(tokens.size - nextToken, max(1, (span.width() / minCellWidth).toInt()))
            if (span.width() < minCellWidth * 0.72f) continue
            val cellWidth = span.width() / capacity
            for (column in 0 until capacity) {
                val token = tokens[nextToken]
                if (paint.measureText(token) > cellWidth * 0.86f) break
                placements += PlanetPlacement(
                    token = token,
                    center = Offset(span.left + cellWidth * column + cellWidth / 2f, y),
                )
                nextToken += 1
                if (nextToken >= tokens.size) break
            }
        }

        if (nextToken >= tokens.size) return placements
        textSize -= 1f
    }

    paint.textSize = minTextSize
    return fallbackHousePlanetPlacements(tokens, points, signBox, bounds, paint, maxRows)
}

private fun buildSparseHousePlanetPlacements(
    tokens: List<String>,
    points: List<Offset>,
    pathBounds: RectF,
    signBox: RectF,
    bounds: RectF,
    paint: Paint,
    horizontalInset: Float,
    verticalInset: Float,
): List<PlanetPlacement>? {
    val maxTokenWidth = tokens.maxOf { paint.measureText(it) }
    val tokenGap = max(8f, paint.textSize * 0.65f)
    val requiredWidth = maxTokenWidth * tokens.size + tokenGap * (tokens.size - 1) + max(10f, paint.textSize * 0.9f)
    val centerY = points.centroid().y.coerceIn(pathBounds.top + verticalInset, pathBounds.bottom - verticalInset)
    val step = max(2f, paint.textSize * 0.35f)
    val maxSteps = max(1, ((pathBounds.height() / 2f) / step).toInt())

    for (stepIndex in 0..maxSteps) {
        val offsets = if (stepIndex == 0) listOf(0f) else listOf(step * stepIndex, -step * stepIndex)
        for (offset in offsets) {
            val y = (centerY + offset).coerceIn(pathBounds.top + verticalInset, pathBounds.bottom - verticalInset)
            val span = houseSpanForRow(
                points = points,
                y = y,
                signBox = signBox,
                gap = planetSignGap(bounds),
                horizontalInset = horizontalInset,
            ) ?: continue
            if (span.width() < requiredWidth * 0.72f) continue

            val groupWidth = min(requiredWidth, span.width())
            val startX = span.centerX() - groupWidth / 2f
            val cellWidth = groupWidth / tokens.size
            val placements = tokens.mapIndexed { index, token ->
                PlanetPlacement(
                    token = token,
                    center = Offset(startX + cellWidth * index + cellWidth / 2f, y),
                )
            }
            if (placements.all { pointInPolygon(it.center, points) && !planetPlacementOverlapsSign(it, signBox, paint, planetSignGap(bounds)) }) {
                return placements
            }
        }
    }

    return null
}

private fun fallbackHousePlanetPlacements(
    tokens: List<String>,
    points: List<Offset>,
    signBox: RectF,
    bounds: RectF,
    paint: Paint,
    maxRows: Int,
): List<PlanetPlacement> {
    val pathBounds = points.bounds()
    val horizontalInset = max(4f, bounds.width() * 0.012f)
    val verticalInset = max(4f, bounds.height() * 0.012f)
    val availableHeight = max(1f, pathBounds.height() - verticalInset * 2f)
    val rowCount = min(maxRows, tokens.size)
    val placements = mutableListOf<PlanetPlacement>()
    var nextToken = 0

    for (row in 0 until rowCount) {
        if (nextToken >= tokens.size) break
        val y = pathBounds.top + verticalInset + availableHeight * (row + 0.5f) / rowCount
        val span = houseSpanForRow(
            points = points,
            y = y,
            signBox = signBox,
            gap = planetSignGap(bounds),
            horizontalInset = horizontalInset,
        ) ?: continue
        placements += PlanetPlacement(
            token = ellipsize(tokens[nextToken], paint, span.width() * 0.86f),
            center = Offset(span.centerX(), y),
        )
        nextToken += 1
    }
    return placements
}

private fun houseSpanForRow(
    points: List<Offset>,
    y: Float,
    signBox: RectF,
    gap: Float,
    horizontalInset: Float,
): RectF? {
    val intersections = mutableListOf<Float>()
    for (index in points.indices) {
        val start = points[index]
        val end = points[(index + 1) % points.size]
        if ((start.y <= y && end.y > y) || (end.y <= y && start.y > y)) {
            val ratio = (y - start.y) / (end.y - start.y)
            intersections += start.x + (end.x - start.x) * ratio
        }
    }
    if (intersections.size < 2) return null
    intersections.sort()

    var left = intersections.first() + horizontalInset
    var right = intersections.last() - horizontalInset
    if (right <= left) return null

    if (y >= signBox.top - gap && y <= signBox.bottom + gap) {
        val leftCandidate = RectF(left, y, min(right, signBox.left - gap), y)
        val rightCandidate = RectF(max(left, signBox.right + gap), y, right, y)
        if (rightCandidate.width() >= leftCandidate.width()) {
            left = rightCandidate.left
            right = rightCandidate.right
        } else {
            left = leftCandidate.left
            right = leftCandidate.right
        }
    }

    return if (right > left) RectF(left, y, right, y) else null
}

private fun List<Offset>.bounds(): RectF {
    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY
    forEach { point ->
        left = min(left, point.x)
        top = min(top, point.y)
        right = max(right, point.x)
        bottom = max(bottom, point.y)
    }
    return RectF(left, top, right, bottom)
}

private fun List<Offset>.centroid(): Offset {
    if (isEmpty()) return Offset.Zero
    var x = 0f
    var y = 0f
    forEach { point ->
        x += point.x
        y += point.y
    }
    return Offset(x / size, y / size)
}

private fun planetPlacementOverlapsSign(
    placement: PlanetPlacement,
    signBox: RectF,
    paint: Paint,
    gap: Float,
): Boolean {
    val textWidth = paint.measureText(placement.token)
    val tokenBox = RectF(
        placement.center.x - textWidth / 2f - 7f,
        placement.center.y - paint.textSize * 0.62f,
        placement.center.x + textWidth / 2f + 7f,
        placement.center.y + paint.textSize * 0.52f,
    )
    val protectedSignBox = RectF(signBox).apply { inset(-gap, -gap) }
    return RectF.intersects(tokenBox, protectedSignBox)
}

private fun ascendantPlacement(
    label: String,
    houseIndex: Int,
    bounds: RectF,
    signBox: RectF,
    planetPlacements: List<PlanetPlacement>,
    paint: Paint,
): Offset? {
    val points = northHouseCornerPoints(houseIndex, bounds)
    if (points.size < 3) return null
    val pathBounds = points.bounds()
    val protectedSignBox = RectF(signBox).apply { inset(-planetSignGap(bounds), -planetSignGap(bounds)) }
    val textWidth = paint.measureText(label)
    val labelWidth = textWidth + 14f
    val labelHeight = paint.textSize * 1.18f
    val planetBoxes = planetPlacements.map { placement ->
        RectF(
            placement.center.x - paint.measureText(placement.token) / 2f - 9f,
            placement.center.y - paint.textSize * 0.7f,
            placement.center.x + paint.measureText(placement.token) / 2f + 9f,
            placement.center.y + paint.textSize * 0.58f,
        )
    }
    val center = points.centroid()
    val step = max(4f, paint.textSize * 0.45f)
    val maxSteps = max(2, (pathBounds.height() / step).toInt())

    for (ring in 0..maxSteps) {
        val candidates = if (ring == 0) {
            listOf(center)
        } else {
            listOf(
                Offset(center.x, center.y - step * ring),
                Offset(center.x, center.y + step * ring),
                Offset(center.x - step * ring, center.y),
                Offset(center.x + step * ring, center.y),
                Offset(center.x - step * ring, center.y - step * ring),
                Offset(center.x + step * ring, center.y - step * ring),
                Offset(center.x - step * ring, center.y + step * ring),
                Offset(center.x + step * ring, center.y + step * ring),
            )
        }
        candidates.forEach { candidate ->
            val labelBox = RectF(
                candidate.x - labelWidth / 2f,
                candidate.y - labelHeight / 2f,
                candidate.x + labelWidth / 2f,
                candidate.y + labelHeight / 2f,
            )
            if (
                pointInPolygon(candidate, points) &&
                pathBounds.contains(labelBox.left, labelBox.top) &&
                pathBounds.contains(labelBox.right, labelBox.bottom) &&
                !RectF.intersects(labelBox, protectedSignBox) &&
                planetBoxes.none { RectF.intersects(labelBox, it) }
            ) {
                return candidate
            }
        }
    }

    return null
}

private fun crowdedPlanetTextSize(baseTextSize: Float, planetCount: Int): Float {
    val reduction = when {
        planetCount > 8 -> 2f
        planetCount > 5 -> 1f
        else -> 0f
    }
    return max(6f, baseTextSize - reduction)
}

private fun tokensFit(tokens: List<String>, paint: Paint, cellWidth: Float, cellHeight: Float): Boolean {
    return tokens.all { paint.measureText(it) <= cellWidth * 0.86f } && paint.textSize * 1.15f <= cellHeight
}

private fun planetTokenRect(
    box: RectF,
    layout: PlanetGrid,
    row: Int,
    column: Int,
    token: String,
    paint: Paint,
): RectF {
    val cellLeft = box.left + layout.cellWidth * column
    val cellTop = box.top + layout.cellHeight * row
    val centerX = cellLeft + layout.cellWidth / 2f
    val centerY = cellTop + layout.cellHeight / 2f
    val horizontalPadding = min(max(4f, paint.textSize * 0.32f), layout.cellWidth * 0.18f)
    val verticalPadding = min(max(2f, paint.textSize * 0.18f), layout.cellHeight * 0.18f)
    val targetWidth = min(layout.cellWidth * 0.9f, paint.measureText(token) + horizontalPadding * 2f)
    val targetHeight = min(layout.cellHeight * 0.78f, paint.textSize * 1.08f + verticalPadding * 2f)
    return RectF(
        centerX - targetWidth / 2f,
        centerY - targetHeight / 2f,
        centerX + targetWidth / 2f,
        centerY + targetHeight / 2f,
    )
}

private fun planetTokenRadius(rect: RectF): Float {
    return min(rect.width(), rect.height()) * 0.48f
}

private fun planetBoxAvoidingSign(
    planetBox: RectF,
    signBox: RectF,
    gap: Float,
): RectF {
    if (!RectF.intersects(planetBox, signBox)) return planetBox
    val protectedSignBox = RectF(signBox).apply { inset(-gap, -gap) }
    if (!RectF.intersects(planetBox, protectedSignBox)) return planetBox

    val candidates = listOf(
        RectF(planetBox.left, planetBox.top, planetBox.right, min(planetBox.bottom, protectedSignBox.top)),
        RectF(planetBox.left, max(planetBox.top, protectedSignBox.bottom), planetBox.right, planetBox.bottom),
        RectF(planetBox.left, planetBox.top, min(planetBox.right, protectedSignBox.left), planetBox.bottom),
        RectF(max(planetBox.left, protectedSignBox.right), planetBox.top, planetBox.right, planetBox.bottom),
    )
    val minWidth = min(planetBox.width(), max(10f, signBox.height()))
    val minHeight = min(planetBox.height(), max(10f, signBox.height() * 0.72f))
    return candidates
        .filter { it.width() >= minWidth && it.height() >= minHeight }
        .maxByOrNull { it.width() * it.height() }
        ?: RectF(
            planetBox.left,
            min(max(planetBox.top, protectedSignBox.bottom), planetBox.bottom),
            planetBox.right,
            planetBox.bottom,
        ).takeIf { it.height() >= minHeight }
        ?: RectF(
            planetBox.left,
            planetBox.top,
            planetBox.right,
            max(planetBox.top, min(planetBox.bottom, protectedSignBox.top)),
        )
}

private fun planetSignGap(bounds: RectF): Float {
    return max(6f, bounds.width() * 0.018f)
}

private fun signLinePadding(box: RectF): Float {
    return max(2f, min(box.width(), box.height()) * 0.08f)
}

private fun chartBounds(width: Float, height: Float, density: Float): RectF {
    val padding = 12f * density
    val side = min(width, height) - padding * 2f
    val left = (width - side) / 2f
    val top = (height - side) / 2f
    return RectF(left, top, left + side, top + side)
}

private fun outerCornerRadius(density: Float): Float {
    return 6f * density
}

private fun hitTestHouse(offset: Offset, bounds: RectF, chartStyle: ChartStyle): Int? {
    if (!bounds.contains(offset.x, offset.y)) return null
    val boxes = houseHitBoxes(chartStyle)
    boxes.forEachIndexed { index, box ->
        if (box.toRect(bounds).contains(offset.x, offset.y)) {
            return index
        }
    }
    return boxes.indices.minByOrNull { index ->
        val rect = boxes[index].toRect(bounds)
        val dx = offset.x - rect.centerX()
        val dy = offset.y - rect.centerY()
        dx * dx + dy * dy
    }
}


private fun hitTestIsoHouse(
    offset: Offset,
    bounds: RectF,
    chartStyle: ChartStyle,
    selectedHouseLifts: List<SelectedHouseLift>,
    projection: IsoProjection,
): Int? {
    val houses = buildIsoHouses(chartStyle, bounds, selectedHouseLifts, projection)
        .sortedWith(
            compareByDescending<IsoHouse> { it.selectedProgress }
                .thenByDescending { it.depth },
        )
    return houses.firstOrNull { house ->
        pointInIsoHouse(offset, house)
    }?.houseIndex
}

private fun pointInIsoHouse(point: Offset, house: IsoHouse): Boolean {
    if (pointInPolygon(point, house.topPoints)) return true
    for (index in house.basePoints.indices) {
        if (house.wallEdgeVisible.getOrNull(index) == false) continue
        val next = (index + 1) % house.basePoints.size
        val wallPoints = listOf(
            house.topPoints[index],
            house.topPoints[next],
            house.basePoints[next],
            house.basePoints[index],
        )
        if (pointInPolygon(point, wallPoints)) return true
    }
    return false
}

private fun hitTestIsoPlanet(
    offset: Offset,
    bounds: RectF,
    houses: List<ZodiacHouse>,
    chartStyle: ChartStyle,
    chartTheme: ChartTheme,
    density: Float,
    usePlanetIcons: Boolean,
    selectedHouseLifts: List<SelectedHouseLift>,
    projection: IsoProjection,
): VedicPlanetSelection? {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        textSize = chartTheme.planetTextSizeSp * density
    }
    val count = min(houses.size, 12)
    for (houseIndex in 0 until count) {
        val visualHouseIndex = visualHouseIndexFor(chartStyle, houses[houseIndex], houseIndex)
        val houseLabel = houseLabelText(chartStyle, houses[houseIndex], houseIndex, chartTheme)
        val labelBoxes = labelBoxesFor(chartStyle, visualHouseIndex, bounds, paint.textSize, houseLabel.isNotBlank()) ?: continue
        val labels = planetLabels(houses[houseIndex], usePlanetIcons)
        if (labels.isEmpty()) continue
        val names = planetNames(houses[houseIndex])
        val visibleLabels = labels.take(MaxPlanetsPerHouse)
        val placements = buildHousePlanetPlacements(
            tokens = visibleLabels,
            chartStyle = chartStyle,
            houseIndex = visualHouseIndex,
            bounds = bounds,
            signBox = labelBoxes.signBox,
            paint = paint,
            baseTextSize = crowdedPlanetTextSize(chartTheme.planetTextSizeSp * density * 0.82f, visibleLabels.size),
            maxRows = maxPlanetRowsFor(chartStyle, houseIndex),
        )
        val z = projection.blockHeight + projection.selectedLiftHeight * liftProgressFor(visualHouseIndex, selectedHouseLifts) + 10f
        placements.forEachIndexed { planetIndex, placement ->
            val point = projection.project(placement.center, z = z)
            val textWidth = paint.measureText(placement.token)
            val hitRect = RectF(
                point.x - textWidth / 2f - 9f * density,
                point.y - paint.textSize * 0.68f,
                point.x + textWidth / 2f + 9f * density,
                point.y + paint.textSize * 0.58f,
            )
            if (hitRect.contains(offset.x, offset.y)) {
                val planetName = names.getOrNull(planetIndex).orEmpty()
                return VedicPlanetSelection(
                    houseIndex = houseIndex,
                    planetIndex = planetIndex,
                    planetName = planetName.ifBlank { visibleLabels[planetIndex] },
                    planetLabel = visibleLabels[planetIndex],
                    house = houses[houseIndex],
                )
            }
        }
    }
    return null
}

private fun pointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    if (polygon.size < 3) return false
    var inside = false
    var previousIndex = polygon.lastIndex
    for (index in polygon.indices) {
        val current = polygon[index]
        val previous = polygon[previousIndex]
        val crossesY = (current.y > point.y) != (previous.y > point.y)
        val intersectX = if (previous.y != current.y) {
            (previous.x - current.x) * (point.y - current.y) / (previous.y - current.y) + current.x
        } else {
            current.x
        }
        if (crossesY && point.x < intersectX) {
            inside = !inside
        }
        previousIndex = index
    }
    return inside
}


private fun maxPlanetRowsFor(chartStyle: ChartStyle, houseIndex: Int): Int {
    return MaxPlanetsPerHouse
}

private fun visualHouseIndexFor(chartStyle: ChartStyle, house: ZodiacHouse, fallbackIndex: Int): Int {
    if (chartStyle != ChartStyle.SOUTH && chartStyle != ChartStyle.EAST) return fallbackIndex
    val sign = house.sign
    return if (sign in 1..12) sign - 1 else fallbackIndex
}

private fun visualHouseIndexForOrFallback(chartStyle: ChartStyle, house: ZodiacHouse?, fallbackIndex: Int): Int {
    return if (house == null) fallbackIndex else visualHouseIndexFor(chartStyle, house, fallbackIndex)
}

private fun dataHouseIndexForVisualHouseIndex(
    chartStyle: ChartStyle,
    houses: List<ZodiacHouse>,
    visualHouseIndex: Int,
): Int {
    if (chartStyle != ChartStyle.SOUTH && chartStyle != ChartStyle.EAST) return visualHouseIndex
    return houses.indexOfFirst { house ->
        house.sign in 1..12 && house.sign - 1 == visualHouseIndex
    }.takeIf { it >= 0 } ?: visualHouseIndex
}

private fun houseLabelText(
    chartStyle: ChartStyle,
    house: ZodiacHouse,
    houseIndex: Int,
    theme: ChartTheme,
): String {
    if (chartStyle == ChartStyle.SOUTH || chartStyle == ChartStyle.EAST) {
        return if (houseIndex == 0) "ASC" else ""
    }

    return if (theme.shouldShowSignNames()) {
        "${house.signName} (${house.sign})"
    } else {
        house.sign.toString()
    }
}

private fun clampYaw(value: Float): Float {
    return value.coerceIn(MinYawDegrees, MaxYawDegrees)
}

private fun clampPitch(value: Float): Float {
    return value.coerceIn(MinPitchDegrees, MaxPitchDegrees)
}

private fun drawSelectedHouseHighlight(
    canvas: android.graphics.Canvas,
    bounds: RectF,
    selectedHouseIndex: Int?,
    chartStyle: ChartStyle,
    progress: Float,
    paints: ChartPaints,
) {
    if (selectedHouseIndex == null || progress <= 0f) return
    val housePath = selectedHousePath(chartStyle, selectedHouseIndex, bounds) ?: return
    val pathBounds = RectF()
    housePath.computeBounds(pathBounds, true)
    val liftX = -bounds.width() * 0.026f * progress
    val liftY = -bounds.width() * 0.042f * progress
    val scale = 1f + 0.024f * progress
    val pivotX = pathBounds.centerX()
    val pivotY = pathBounds.centerY()
    val liftedPath = Path(housePath)
    val liftMatrix = Matrix().apply {
        setScale(scale, scale, pivotX, pivotY)
        postTranslate(liftX, liftY)
    }
    liftedPath.transform(liftMatrix)
    val topColor = mixColors(paints.accent.color, Color.WHITE, 0.08f)
    val rightSideColor = mixColors(paints.accent.color, Color.WHITE, 0.2f)
    val bottomSideColor = mixColors(paints.accent.color, Color.BLACK, 0.28f)
    val sideStrokeColor = mixColors(paints.accent.color, Color.BLACK, 0.48f)

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb((44 * progress).toInt(), 17, 24, 39)
    }
    val rightSidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = withAlpha(rightSideColor, (255 * progress).toInt())
    }
    val bottomSidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = withAlpha(bottomSideColor, (255 * progress).toInt())
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = topColor
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = max(3f, bounds.width() * 0.0075f) * progress
        color = withAlpha(sideStrokeColor, (235 * progress).toInt())
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val sideStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = max(1.4f, bounds.width() * 0.0035f) * progress
        color = withAlpha(sideStrokeColor, (180 * progress).toInt())
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    val shadowPath = Path(housePath)
    shadowPath.transform(Matrix().apply {
        setTranslate(-liftX * 0.45f, -liftY * 0.55f)
    })
    canvas.drawPath(shadowPath, shadowPaint)

    val lowerWall = buildExtrudedSidePath(housePath, liftedPath) { midpoint, center ->
        midpoint.y >= center.y
    }
    val rightWall = buildExtrudedSidePath(housePath, liftedPath) { midpoint, center ->
        midpoint.x >= center.x && midpoint.y < center.y
    }
    canvas.drawPath(lowerWall, bottomSidePaint)
    canvas.drawPath(rightWall, rightSidePaint)
    canvas.drawPath(lowerWall, sideStrokePaint)
    canvas.drawPath(rightWall, sideStrokePaint)
    canvas.drawPath(liftedPath, fillPaint)
    canvas.drawPath(liftedPath, strokePaint)
}


private fun liftProgressFor(houseIndex: Int, selectedHouseLifts: List<SelectedHouseLift>): Float {
    return selectedHouseLifts
        .firstOrNull { it.houseIndex == houseIndex }
        ?.progress
        ?.coerceIn(0f, 1f)
        ?: 0f
}

private fun buildExtrudedSidePath(
    basePath: Path,
    liftedPath: Path,
    shouldIncludeSegment: (midpoint: Offset, center: Offset) -> Boolean,
): Path {
    val basePoints = samplePath(basePath, 112)
    val liftedPoints = samplePath(liftedPath, basePoints.size)
    val sidePath = Path()
    val count = min(basePoints.size, liftedPoints.size)
    if (count < 2) return sidePath
    val baseBounds = RectF()
    basePath.computeBounds(baseBounds, true)
    val center = Offset(baseBounds.centerX(), baseBounds.centerY())
    for (index in 0 until count) {
        val next = (index + 1) % count
        val base = basePoints[index]
        val baseNext = basePoints[next]
        val midpoint = Offset((base.x + baseNext.x) / 2f, (base.y + baseNext.y) / 2f)
        if (!shouldIncludeSegment(midpoint, center)) continue
        val liftedNext = liftedPoints[next]
        val lifted = liftedPoints[index]
        sidePath.moveTo(base.x, base.y)
        sidePath.lineTo(baseNext.x, baseNext.y)
        sidePath.lineTo(liftedNext.x, liftedNext.y)
        sidePath.lineTo(lifted.x, lifted.y)
        sidePath.close()
    }
    return sidePath
}

private fun samplePath(path: Path, preferredCount: Int): List<Offset> {
    val measure = PathMeasure(path, true)
    val length = measure.length
    if (length <= 0f) return emptyList()
    val count = max(8, preferredCount)
    val point = FloatArray(2)
    return List(count) { index ->
        measure.getPosTan(length * index / count, point, null)
        Offset(point[0], point[1])
    }
}

private fun setPaintAlpha(paint: Paint, progress: Float) {
    paint.alpha = (255 * progress.coerceIn(0f, 1f)).toInt()
}

private fun revealScale(progress: Float): Float {
    return 0.94f + 0.06f * smoothStep(progress.coerceIn(0f, 1f))
}

private fun applyLift(
    canvas: android.graphics.Canvas,
    box: RectF,
    textProgress: Float,
    liftTransform: TextLiftTransform?,
) {
    val baseScale = revealScale(textProgress)
    if (liftTransform != null) {
        canvas.translate(liftTransform.x, liftTransform.y)
        canvas.scale(liftTransform.scale, liftTransform.scale, liftTransform.pivotX, liftTransform.pivotY)
    }
    canvas.scale(baseScale, baseScale, box.centerX(), box.centerY())
}

private fun liftedTextClip(
    box: RectF,
    selectedLiftProgress: Float,
    liftTransform: TextLiftTransform?,
): RectF {
    if (selectedLiftProgress <= 0f || liftTransform == null) return box
    val horizontalExtra = max(box.width() * 0.16f, kotlin.math.abs(liftTransform.x) * 1.8f)
    val verticalExtra = max(box.height() * 0.28f, kotlin.math.abs(liftTransform.y) * 1.8f)
    return RectF(
        box.left - horizontalExtra,
        box.top - verticalExtra,
        box.right + horizontalExtra,
        box.bottom + verticalExtra,
    )
}

private fun textLiftTransform(
    chartStyle: ChartStyle,
    houseIndex: Int,
    bounds: RectF,
    progress: Float,
): TextLiftTransform? {
    if (progress <= 0f) return null
    val housePath = selectedHousePath(chartStyle, houseIndex, bounds) ?: return null
    val pathBounds = RectF()
    housePath.computeBounds(pathBounds, true)
    return TextLiftTransform(
        x = -bounds.width() * 0.026f * progress,
        y = -bounds.width() * 0.042f * progress,
        scale = 1f + 0.024f * progress,
        pivotX = pathBounds.centerX(),
        pivotY = pathBounds.centerY(),
    )
}

private fun smoothStep(value: Float): Float {
    val x = value.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}


private fun fitSingleLine(
    text: String,
    paint: Paint,
    baseTextSize: Float,
    maxWidth: Float,
    minTextSize: Float,
): String {
    var scale = 1f
    while (scale >= 0.55f) {
        paint.textSize = max(minTextSize, baseTextSize * scale)
        if (paint.measureText(text) <= maxWidth) {
            return text
        }
        scale -= 0.05f
    }
    paint.textSize = minTextSize
    return ellipsize(text, paint, maxWidth)
}

private fun usesComplexPlanetLayout(houseIndex: Int): Boolean {
    return houseIndex == 2 || houseIndex == 4 || houseIndex == 8 || houseIndex == 10
}

private fun northSlot(
    signLeft: Float,
    signTop: Float,
    signRight: Float,
    signBottom: Float,
    planetLeft: Float,
    planetTop: Float,
    planetRight: Float,
    planetBottom: Float,
    maxPlanetRows: Int,
): NorthHouseSlot {
    return NorthHouseSlot(
        signBox = RelativeBox(signLeft, signTop, signRight, signBottom),
        planetBox = RelativeBox(planetLeft, planetTop, planetRight, planetBottom),
        maxPlanetRows = maxPlanetRows,
    )
}

private fun northSlots(): Array<NorthHouseSlot> {
    return arrayOf(
        northSlot(0.47f, 0.41f, 0.53f, 0.47f, 0.36f, 0.07f, 0.64f, 0.32f, 4),
        northSlot(0.21f, 0.17f, 0.27f, 0.23f, 0.09f, 0.06f, 0.43f, 0.18f, 4),
        northSlot(0.18f, 0.22f, 0.24f, 0.29f, 0.07f, 0.19f, 0.20f, 0.36f, 4),
        northSlot(0.42f, 0.47f, 0.48f, 0.53f, 0.14f, 0.43f, 0.41f, 0.57f, 4),
        northSlot(0.17f, 0.74f, 0.23f, 0.80f, 0.07f, 0.61f, 0.27f, 0.75f, 4),
        northSlot(0.23f, 0.79f, 0.29f, 0.86f, 0.12f, 0.86f, 0.45f, 0.96f, 4),
        northSlot(0.47f, 0.54f, 0.53f, 0.60f, 0.37f, 0.66f, 0.63f, 0.92f, 4),
        northSlot(0.73f, 0.79f, 0.79f, 0.86f, 0.58f, 0.86f, 0.88f, 0.96f, 4),
        northSlot(0.78f, 0.73f, 0.84f, 0.80f, 0.84f, 0.63f, 0.95f, 0.79f, 4),
        northSlot(0.52f, 0.47f, 0.58f, 0.53f, 0.59f, 0.43f, 0.86f, 0.57f, 4),
        northSlot(0.78f, 0.22f, 0.84f, 0.29f, 0.80f, 0.19f, 0.93f, 0.36f, 4),
        northSlot(0.73f, 0.17f, 0.79f, 0.23f, 0.57f, 0.06f, 0.91f, 0.18f, 4),
    )
}

private fun planetLabels(house: ZodiacHouse, usePlanetIcons: Boolean): List<String> {
    val names = planetNames(house)
    if (names.isEmpty()) return emptyList()
    if (usePlanetIcons) {
        return names.map { planetIconFor(it) }
    }
    val shortNames = house.planetShortNames
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return names.mapIndexed { index, planetName ->
        shortNames.getOrNull(index)?.ifBlank { planetName } ?: planetName
    }
}

private fun planetNames(house: ZodiacHouse): List<String> {
    return house.planets
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private fun planetIconFor(planetName: String): String {
    return when (planetName.trim().uppercase()) {
        "SUN", "SURYA" -> "\u2609"
        "MOON", "CHANDRA" -> "\u263E"
        "MARS", "MANGAL" -> "\u2642"
        "MERCURY", "BUDH" -> "\u263F"
        "JUPITER", "GURU", "BRIHASPATI" -> "\u2643"
        "VENUS", "SHUKRA" -> "\u2640"
        "SATURN", "SHANI" -> "\u2644"
        "RAHU" -> "\u260A"
        "KETU" -> "\u260B"
        else -> planetName.take(2).replaceFirstChar { it.uppercase() }
    }
}

private fun houseHitBoxes(chartStyle: ChartStyle): Array<RelativeBox> {
    return when (chartStyle) {
        ChartStyle.SOUTH -> southHitBoxes()
        ChartStyle.EAST -> eastHitBoxes()
        else -> northHitBoxes()
    }
}

private fun northHitBoxes(): Array<RelativeBox> {
    return arrayOf(
        RelativeBox(0.34f, 0.02f, 0.66f, 0.36f),
        RelativeBox(0.02f, 0.02f, 0.45f, 0.22f),
        RelativeBox(0.02f, 0.18f, 0.25f, 0.48f),
        RelativeBox(0.12f, 0.38f, 0.46f, 0.62f),
        RelativeBox(0.02f, 0.52f, 0.29f, 0.82f),
        RelativeBox(0.02f, 0.78f, 0.48f, 0.98f),
        RelativeBox(0.34f, 0.62f, 0.66f, 0.98f),
        RelativeBox(0.52f, 0.78f, 0.98f, 0.98f),
        RelativeBox(0.71f, 0.52f, 0.98f, 0.82f),
        RelativeBox(0.54f, 0.38f, 0.88f, 0.62f),
        RelativeBox(0.75f, 0.18f, 0.98f, 0.48f),
        RelativeBox(0.55f, 0.02f, 0.98f, 0.22f),
    )
}

private fun selectedHousePath(chartStyle: ChartStyle, houseIndex: Int, bounds: RectF): Path? {
    return when (chartStyle) {
        ChartStyle.NORTH -> northHousePath(houseIndex, bounds)
        ChartStyle.EAST -> eastHousePath(houseIndex, bounds)
        ChartStyle.SOUTH -> houseHitBoxes(chartStyle).getOrNull(houseIndex)?.toRect(bounds)?.toPath()
    }
}

private fun houseCornerPoints(chartStyle: ChartStyle, houseIndex: Int, bounds: RectF): List<Offset> {
    return when (chartStyle) {
        ChartStyle.NORTH -> northHouseCornerPoints(houseIndex, bounds)
        ChartStyle.EAST -> eastHouseCornerPoints(houseIndex, bounds)
        ChartStyle.SOUTH -> houseHitBoxes(chartStyle).getOrNull(houseIndex)?.toRect(bounds)?.corners().orEmpty()
    }
}

private fun eastHousePath(houseIndex: Int, bounds: RectF): Path? {
    val points = eastHouseCornerPoints(houseIndex, bounds)
    if (points.isEmpty()) return null
    return points.toPath()
}

private fun eastHouseCornerPoints(houseIndex: Int, bounds: RectF): List<Offset> {
    val x0 = bounds.left
    val x1 = bounds.left + bounds.width() / 3f
    val x2 = bounds.left + bounds.width() * 2f / 3f
    val x3 = bounds.right
    val y0 = bounds.top
    val y1 = bounds.top + bounds.height() / 3f
    val y2 = bounds.top + bounds.height() * 2f / 3f
    val y3 = bounds.bottom
    return when (houseIndex) {
        0 -> listOf(Offset(x1, y0), Offset(x2, y0), Offset(x2, y1), Offset(x1, y1))
        1 -> listOf(Offset(x0, y0), Offset(x1, y0), Offset(x1, y1))
        2 -> listOf(Offset(x0, y0), Offset(x1, y1), Offset(x0, y1))
        3 -> listOf(Offset(x0, y1), Offset(x1, y1), Offset(x1, y2), Offset(x0, y2))
        4 -> listOf(Offset(x0, y2), Offset(x1, y2), Offset(x0, y3))
        5 -> listOf(Offset(x0, y3), Offset(x1, y2), Offset(x1, y3))
        6 -> listOf(Offset(x1, y2), Offset(x2, y2), Offset(x2, y3), Offset(x1, y3))
        7 -> listOf(Offset(x2, y2), Offset(x3, y3), Offset(x2, y3))
        8 -> listOf(Offset(x2, y2), Offset(x3, y2), Offset(x3, y3))
        9 -> listOf(Offset(x2, y1), Offset(x3, y1), Offset(x3, y2), Offset(x2, y2))
        10 -> listOf(Offset(x3, y0), Offset(x3, y1), Offset(x2, y1))
        11 -> listOf(Offset(x2, y0), Offset(x3, y0), Offset(x2, y1))
        else -> emptyList()
    }
}

private fun northHouseCornerPoints(houseIndex: Int, bounds: RectF): List<Offset> {
    val topLeft = relPoint(bounds, 0f, 0f)
    val top = relPoint(bounds, 0.5f, 0f)
    val topRight = relPoint(bounds, 1f, 0f)
    val right = relPoint(bounds, 1f, 0.5f)
    val bottomRight = relPoint(bounds, 1f, 1f)
    val bottom = relPoint(bounds, 0.5f, 1f)
    val bottomLeft = relPoint(bounds, 0f, 1f)
    val left = relPoint(bounds, 0f, 0.5f)
    val center = relPoint(bounds, 0.5f, 0.5f)
    val northWest = relPoint(bounds, 0.25f, 0.25f)
    val northEast = relPoint(bounds, 0.75f, 0.25f)
    val southEast = relPoint(bounds, 0.75f, 0.75f)
    val southWest = relPoint(bounds, 0.25f, 0.75f)
    return when (houseIndex) {
        0 -> listOf(top, northEast, center, northWest)
        1 -> listOf(topLeft, top, northWest)
        2 -> listOf(topLeft, northWest, left)
        3 -> listOf(left, northWest, center, southWest)
        4 -> listOf(left, southWest, bottomLeft)
        5 -> listOf(bottomLeft, southWest, bottom)
        6 -> listOf(bottom, southWest, center, southEast)
        7 -> listOf(bottom, southEast, bottomRight)
        8 -> listOf(right, bottomRight, southEast)
        9 -> listOf(northEast, right, southEast, center)
        10 -> listOf(topRight, right, northEast)
        11 -> listOf(top, topRight, northEast)
        else -> emptyList()
    }
}

private fun northHousePath(houseIndex: Int, bounds: RectF): Path? {
    val topLeft = relPoint(bounds, 0f, 0f)
    val top = relPoint(bounds, 0.5f, 0f)
    val topRight = relPoint(bounds, 1f, 0f)
    val right = relPoint(bounds, 1f, 0.5f)
    val bottomRight = relPoint(bounds, 1f, 1f)
    val bottom = relPoint(bounds, 0.5f, 1f)
    val bottomLeft = relPoint(bounds, 0f, 1f)
    val left = relPoint(bounds, 0f, 0.5f)
    val center = relPoint(bounds, 0.5f, 0.5f)
    val northWest = relPoint(bounds, 0.25f, 0.25f)
    val northEast = relPoint(bounds, 0.75f, 0.25f)
    val southEast = relPoint(bounds, 0.75f, 0.75f)
    val southWest = relPoint(bounds, 0.25f, 0.75f)
    val w = bounds.width()
    val h = bounds.height()

    return when (houseIndex) {
        0 -> Path().apply {
            moveTo(top.x, top.y)
            curveTopToNorthEast(top, northEast, w, h)
            lineTo(center.x, center.y)
            lineTo(northWest.x, northWest.y)
            curveNorthWestToTop(northWest, top, w, h)
            close()
        }
        1 -> Path().apply {
            moveTo(topLeft.x, topLeft.y)
            lineTo(top.x, top.y)
            curveTopToNorthWest(top, northWest, w, h)
            close()
        }
        2 -> Path().apply {
            moveTo(topLeft.x, topLeft.y)
            lineTo(northWest.x, northWest.y)
            curveNorthWestToLeft(northWest, left, w, h)
            close()
        }
        3 -> Path().apply {
            moveTo(left.x, left.y)
            curveLeftToNorthWest(left, northWest, w, h)
            lineTo(center.x, center.y)
            lineTo(southWest.x, southWest.y)
            curveSouthWestToLeft(southWest, left, w, h)
            close()
        }
        4 -> Path().apply {
            moveTo(left.x, left.y)
            curveLeftToSouthWest(left, southWest, w, h)
            lineTo(bottomLeft.x, bottomLeft.y)
            close()
        }
        5 -> Path().apply {
            moveTo(bottomLeft.x, bottomLeft.y)
            lineTo(southWest.x, southWest.y)
            curveSouthWestToBottom(southWest, bottom, w, h)
            close()
        }
        6 -> Path().apply {
            moveTo(bottom.x, bottom.y)
            curveBottomToSouthWest(bottom, southWest, w, h)
            lineTo(center.x, center.y)
            lineTo(southEast.x, southEast.y)
            curveSouthEastToBottom(southEast, bottom, w, h)
            close()
        }
        7 -> Path().apply {
            moveTo(bottom.x, bottom.y)
            curveBottomToSouthEast(bottom, southEast, w, h)
            lineTo(bottomRight.x, bottomRight.y)
            close()
        }
        8 -> Path().apply {
            moveTo(right.x, right.y)
            lineTo(bottomRight.x, bottomRight.y)
            lineTo(southEast.x, southEast.y)
            curveSouthEastToRight(southEast, right, w, h)
            close()
        }
        9 -> Path().apply {
            moveTo(northEast.x, northEast.y)
            curveNorthEastToRight(northEast, right, w, h)
            curveRightToSouthEast(right, southEast, w, h)
            lineTo(center.x, center.y)
            close()
        }
        10 -> Path().apply {
            moveTo(topRight.x, topRight.y)
            lineTo(right.x, right.y)
            curveRightToNorthEast(right, northEast, w, h)
            close()
        }
        11 -> Path().apply {
            moveTo(top.x, top.y)
            lineTo(topRight.x, topRight.y)
            lineTo(northEast.x, northEast.y)
            curveNorthEastToTop(northEast, top, w, h)
            close()
        }
        else -> null
    }
}

private fun relPoint(bounds: RectF, x: Float, y: Float): Offset {
    return Offset(
        x = bounds.left + bounds.width() * x,
        y = bounds.top + bounds.height() * y,
    )
}

private fun Path.curveTopToNorthEast(top: Offset, northEast: Offset, w: Float, h: Float) {
    quadTo(top.x + w * 0.075f, top.y + h * 0.16f, northEast.x, northEast.y)
}

private fun Path.curveNorthEastToTop(northEast: Offset, top: Offset, w: Float, h: Float) {
    quadTo(top.x + w * 0.075f, top.y + h * 0.16f, top.x, top.y)
}

private fun Path.curveTopToNorthWest(top: Offset, northWest: Offset, w: Float, h: Float) {
    quadTo(top.x - w * 0.075f, top.y + h * 0.16f, northWest.x, northWest.y)
}

private fun Path.curveNorthWestToTop(northWest: Offset, top: Offset, w: Float, h: Float) {
    quadTo(top.x - w * 0.075f, top.y + h * 0.16f, top.x, top.y)
}

private fun Path.curveNorthEastToRight(northEast: Offset, right: Offset, w: Float, h: Float) {
    quadTo(right.x - w * 0.16f, right.y - h * 0.075f, right.x, right.y)
}

private fun Path.curveRightToNorthEast(right: Offset, northEast: Offset, w: Float, h: Float) {
    quadTo(right.x - w * 0.16f, right.y - h * 0.075f, northEast.x, northEast.y)
}

private fun Path.curveRightToSouthEast(right: Offset, southEast: Offset, w: Float, h: Float) {
    quadTo(right.x - w * 0.16f, right.y + h * 0.075f, southEast.x, southEast.y)
}

private fun Path.curveSouthEastToRight(southEast: Offset, right: Offset, w: Float, h: Float) {
    quadTo(right.x - w * 0.16f, right.y + h * 0.075f, right.x, right.y)
}

private fun Path.curveSouthEastToBottom(southEast: Offset, bottom: Offset, w: Float, h: Float) {
    quadTo(bottom.x + w * 0.075f, bottom.y - h * 0.16f, bottom.x, bottom.y)
}

private fun Path.curveBottomToSouthEast(bottom: Offset, southEast: Offset, w: Float, h: Float) {
    quadTo(bottom.x + w * 0.075f, bottom.y - h * 0.16f, southEast.x, southEast.y)
}

private fun Path.curveBottomToSouthWest(bottom: Offset, southWest: Offset, w: Float, h: Float) {
    quadTo(bottom.x - w * 0.075f, bottom.y - h * 0.16f, southWest.x, southWest.y)
}

private fun Path.curveSouthWestToBottom(southWest: Offset, bottom: Offset, w: Float, h: Float) {
    quadTo(bottom.x - w * 0.075f, bottom.y - h * 0.16f, bottom.x, bottom.y)
}

private fun Path.curveSouthWestToLeft(southWest: Offset, left: Offset, w: Float, h: Float) {
    quadTo(left.x + w * 0.16f, left.y + h * 0.075f, left.x, left.y)
}

private fun Path.curveLeftToSouthWest(left: Offset, southWest: Offset, w: Float, h: Float) {
    quadTo(left.x + w * 0.16f, left.y + h * 0.075f, southWest.x, southWest.y)
}

private fun Path.curveLeftToNorthWest(left: Offset, northWest: Offset, w: Float, h: Float) {
    quadTo(left.x + w * 0.16f, left.y - h * 0.075f, northWest.x, northWest.y)
}

private fun Path.curveNorthWestToLeft(northWest: Offset, left: Offset, w: Float, h: Float) {
    quadTo(left.x + w * 0.16f, left.y - h * 0.075f, left.x, left.y)
}

private fun RectF.toPath(): Path {
    return Path().apply {
        addRect(this@toPath, Path.Direction.CW)
    }
}

private fun List<Offset>.toPath(): Path {
    return Path().apply {
        if (isEmpty()) return@apply
        moveTo(first().x, first().y)
        drop(1).forEach { point ->
            lineTo(point.x, point.y)
        }
        close()
    }
}

private fun RectF.corners(): List<Offset> {
    return listOf(
        Offset(left, top),
        Offset(right, top),
        Offset(right, bottom),
        Offset(left, bottom),
    )
}

private fun southHitBoxes(): Array<RelativeBox> {
    return arrayOf(
        RelativeBox(0.25f, 0.00f, 0.50f, 0.25f), RelativeBox(0.50f, 0.00f, 0.75f, 0.25f),
        RelativeBox(0.75f, 0.00f, 1.00f, 0.25f), RelativeBox(0.75f, 0.25f, 1.00f, 0.50f),
        RelativeBox(0.75f, 0.50f, 1.00f, 0.75f), RelativeBox(0.75f, 0.75f, 1.00f, 1.00f),
        RelativeBox(0.50f, 0.75f, 0.75f, 1.00f), RelativeBox(0.25f, 0.75f, 0.50f, 1.00f),
        RelativeBox(0.00f, 0.75f, 0.25f, 1.00f), RelativeBox(0.00f, 0.50f, 0.25f, 0.75f),
        RelativeBox(0.00f, 0.25f, 0.25f, 0.50f), RelativeBox(0.00f, 0.00f, 0.25f, 0.25f),
    )
}



private fun eastHitBoxes(): Array<RelativeBox> {
    return arrayOf(
        RelativeBox(0.36f, 0.02f, 0.64f, 0.31f),
        RelativeBox(0.17f, 0.02f, 0.33f, 0.18f),
        RelativeBox(0.02f, 0.16f, 0.18f, 0.33f),
        RelativeBox(0.02f, 0.36f, 0.31f, 0.64f),
        RelativeBox(0.02f, 0.69f, 0.18f, 0.84f),
        RelativeBox(0.17f, 0.82f, 0.33f, 0.98f),
        RelativeBox(0.36f, 0.69f, 0.64f, 0.98f),
        RelativeBox(0.67f, 0.82f, 0.83f, 0.98f),
        RelativeBox(0.82f, 0.69f, 0.98f, 0.84f),
        RelativeBox(0.69f, 0.36f, 0.98f, 0.64f),
        RelativeBox(0.82f, 0.16f, 0.98f, 0.33f),
        RelativeBox(0.67f, 0.02f, 0.83f, 0.18f),
    )
}



private fun RelativeBox.toRect(bounds: RectF): RectF {
    return RectF(
        bounds.left + bounds.width() * left,
        bounds.top + bounds.height() * top,
        bounds.left + bounds.width() * right,
        bounds.top + bounds.height() * bottom,
    )
}

private fun RectF.withInset(value: Float): RectF {
    return RectF(
        left + value,
        top + value,
        right - value,
        bottom - value,
    )
}

private fun withAlpha(color: Int, alpha: Int): Int {
    return Color.argb(
        alpha.coerceIn(0, 255),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )
}

private fun mixColors(startColor: Int, endColor: Int, amount: Float): Int {
    val fraction = amount.coerceIn(0f, 1f)
    return Color.rgb(
        (Color.red(startColor) + (Color.red(endColor) - Color.red(startColor)) * fraction).toInt().coerceIn(0, 255),
        (Color.green(startColor) + (Color.green(endColor) - Color.green(startColor)) * fraction).toInt().coerceIn(0, 255),
        (Color.blue(startColor) + (Color.blue(endColor) - Color.blue(startColor)) * fraction).toInt().coerceIn(0, 255),
    )
}

private fun gridLineColor(backgroundColor: Int): Int {
    val luminance = (
        Color.red(backgroundColor) * 0.299f +
            Color.green(backgroundColor) * 0.587f +
            Color.blue(backgroundColor) * 0.114f
        ) / 255f
    return if (luminance < 0.45f) {
        Color.rgb(226, 232, 240)
    } else {
        Color.rgb(43, 35, 28)
    }
}

private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    val suffix = "..."
    var end = text.length
    while (end > 0 && paint.measureText(text.substring(0, end) + suffix) > maxWidth) {
        end--
    }
    return if (end == 0) "" else text.substring(0, end) + suffix
}

private fun centeredTextOffset(paint: Paint): Float {
    val metrics = paint.fontMetrics
    return -(metrics.ascent + metrics.descent) / 2f
}

private data class ChartPaints(
    val fill: Paint,
    val line: Paint,
    val grid: Paint,
    val accent: Paint,
    val signText: Paint,
    val planetText: Paint,
    val signBaseTextSize: Float,
    val planetBaseTextSize: Float,
)

private data class RevealedPath(
    val path: Path,
    val paint: Paint,
)

private data class SelectedHouseLift(
    val houseIndex: Int,
    val progress: Float,
)

private data class IsoHouse(
    val houseIndex: Int,
    val basePoints: List<Offset>,
    val topPoints: List<Offset>,
    val cornerBasePoints: List<Offset>,
    val cornerTopPoints: List<Offset>,
    val wallEdgeVisible: List<Boolean>,
    val depth: Float,
    val selectedProgress: Float,
    val extraZ: Float,
)

private data class LabelBoxes(
    val signBox: RectF,
    val planetBox: RectF,
)

private data class TextLiftTransform(
    val x: Float,
    val y: Float,
    val scale: Float,
    val pivotX: Float,
    val pivotY: Float,
)

private data class NorthHouseSlot(
    val signBox: RelativeBox,
    val planetBox: RelativeBox,
    val maxPlanetRows: Int,
)

private data class RelativeBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

private data class PlanetGrid(
    val tokens: List<String>,
    val columns: Int,
    val cellWidth: Float,
    val cellHeight: Float,
)

private data class PlanetPlacement(
    val token: String,
    val center: Offset,
)
