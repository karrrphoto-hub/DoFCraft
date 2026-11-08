package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.model.CalculatedDistance
import com.example.dofcalculator.settings.ThemeMode
import com.example.dofcalculator.ui.theme.DOFCalculatorTheme
import com.example.dofcalculator.settings.DistanceDisplay
import kotlin.math.ln1p
import kotlin.math.max

/**
 * A schematic, adaptive visualization of depth-of-field boundaries.
 *
 * Finite positions use a logarithmic scale. An infinite far limit is represented
 * by a continuation arrow and never mapped to a finite position.
 */
@Composable
fun DofVisualization(
    nearLimitMeters: Double,
    focusDistanceMeters: Double,
    farLimit: CalculatedDistance,
    modifier: Modifier = Modifier,
    focusAtInfinity: Boolean = false,
) {
    val formatter = currentDistanceDisplay()
    val metersUnit = distanceUnitLabel()
    val cameraText = formatVisualizationMeters(0.0, formatter, metersUnit)
    val nearText =
        formatVisualizationMeters(nearLimitMeters, formatter, metersUnit)
    val focusText =
        if (focusAtInfinity) INFINITY_SYMBOL else formatVisualizationMeters(focusDistanceMeters, formatter, metersUnit)
    val farText = when (farLimit) {
        is CalculatedDistance.Finite ->
            formatVisualizationMeters(farLimit.meters, formatter, metersUnit)
        CalculatedDistance.Infinity -> INFINITY_SYMBOL
    }
    val description = androidx.compose.ui.res.stringResource(
        R.string.dof_visualization_description,
        nearText,
        focusText,
        farText,
    )

    val colorScheme = MaterialTheme.colorScheme
    val cameraColor = colorScheme.onSurface
    val nearColor = colorScheme.tertiary
    val focusColor = colorScheme.primary
    val farColor = colorScheme.secondary
    val rangeColor = colorScheme.primaryContainer
    val baselineColor = colorScheme.outline
    val surfaceColor = colorScheme.surfaceContainer

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(
                    R.string.dof_visualization_title,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .semantics {
                        contentDescription = description
                    },
            ) {
                drawDepthOfFieldScale(
                    nearLimitMeters = nearLimitMeters,
                    focusDistanceMeters = focusDistanceMeters,
                    farLimit = farLimit,
                    cameraColor = cameraColor,
                    nearColor = nearColor,
                    focusColor = focusColor,
                    farColor = farColor,
                    rangeColor = rangeColor,
                    baselineColor = baselineColor,
                    surfaceColor = surfaceColor,
                    focusAtInfinity = focusAtInfinity,
                )
            }

            Text(
                text = androidx.compose.ui.res.stringResource(
                    R.string.dof_visualization_scale_note,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )

            VisualizationValueRow(
                color = cameraColor,
                label = androidx.compose.ui.res.stringResource(
                    R.string.dof_visualization_camera,
                ),
                value = cameraText,
            )
            VisualizationValueRow(
                color = nearColor,
                label = androidx.compose.ui.res.stringResource(
                    R.string.near_limit_result,
                ),
                value = nearText,
            )
            VisualizationValueRow(
                color = focusColor,
                label = androidx.compose.ui.res.stringResource(
                    R.string.dof_visualization_focus,
                ),
                value = focusText,
            )
            VisualizationValueRow(
                color = farColor,
                label = androidx.compose.ui.res.stringResource(
                    R.string.far_limit_result,
                ),
                value = farText,
            )
        }
    }
}

@Composable
private fun VisualizationValueRow(
    color: Color,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun DrawScope.drawDepthOfFieldScale(
    nearLimitMeters: Double,
    focusDistanceMeters: Double,
    farLimit: CalculatedDistance,
    cameraColor: Color,
    nearColor: Color,
    focusColor: Color,
    farColor: Color,
    rangeColor: Color,
    baselineColor: Color,
    surfaceColor: Color,
    focusAtInfinity: Boolean,
) {
    val cameraWidth = 30.dp.toPx()
    val cameraHeight = 22.dp.toPx()
    val scaleStartX = 50.dp.toPx()
    val scaleEndX = size.width - 12.dp.toPx()
    val scaleWidth = (scaleEndX - scaleStartX).coerceAtLeast(1f)
    val baselineY = size.height * 0.52f
    val finiteFarMeters =
        (farLimit as? CalculatedDistance.Finite)?.meters

    val scaleMaxMeters = if (finiteFarMeters != null) {
        max(
            finiteFarMeters * 1.08,
            max(focusDistanceMeters, nearLimitMeters) * 1.08,
        )
    } else {
        max(
            focusDistanceMeters * 1.6,
            nearLimitMeters * 2.0,
        )
    }

    fun positionFor(distanceMeters: Double): Float =
        scaleStartX +
            (adaptiveScaleFraction(distanceMeters, scaleMaxMeters) * scaleWidth)

    val nearX = positionFor(nearLimitMeters)
    val focusX = positionFor(focusDistanceMeters)
    val farX = finiteFarMeters?.let(::positionFor)
    val rangeEndX = farX ?: (scaleEndX - 8.dp.toPx())
    val rangeHeight = 24.dp.toPx()

    drawLine(
        color = baselineColor,
        start = Offset(scaleStartX, baselineY),
        end = Offset(scaleEndX, baselineY),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )

    if (finiteFarMeters == null) {
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    rangeColor.copy(alpha = 0.85f),
                    rangeColor.copy(alpha = 0.55f),
                ),
                startX = nearX,
                endX = rangeEndX,
            ),
            topLeft = Offset(nearX, baselineY - (rangeHeight / 2f)),
            size = Size(
                width = (rangeEndX - nearX).coerceAtLeast(0f),
                height = rangeHeight,
            ),
            cornerRadius = CornerRadius(rangeHeight / 2f),
        )
        drawContinuationArrow(
            startX = rangeEndX,
            endX = scaleEndX,
            centerY = baselineY,
            color = farColor,
        )
    } else {
        drawRoundRect(
            color = rangeColor,
            topLeft = Offset(nearX, baselineY - (rangeHeight / 2f)),
            size = Size(
                width = (rangeEndX - nearX).coerceAtLeast(0f),
                height = rangeHeight,
            ),
            cornerRadius = CornerRadius(rangeHeight / 2f),
        )
    }

    drawCamera(
        rightX = scaleStartX - 4.dp.toPx(),
        centerY = baselineY,
        width = cameraWidth,
        height = cameraHeight,
        cameraColor = cameraColor,
        surfaceColor = surfaceColor,
    )
    drawBoundaryMarker(
        x = nearX,
        centerY = baselineY,
        color = nearColor,
        height = 52.dp.toPx(),
    )
    if (!focusAtInfinity) drawBoundaryMarker(
        x = focusX,
        centerY = baselineY,
        color = focusColor,
        height = 68.dp.toPx(),
    )
    farX?.let { finiteFarX ->
        drawBoundaryMarker(
            x = finiteFarX,
            centerY = baselineY,
            color = farColor,
            height = 52.dp.toPx(),
        )
    }
}

private fun DrawScope.drawCamera(
    rightX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    cameraColor: Color,
    surfaceColor: Color,
) {
    drawRoundRect(
        color = cameraColor,
        topLeft = Offset(rightX - width, centerY - (height / 2f)),
        size = Size(width, height),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )
    drawRoundRect(
        color = cameraColor,
        topLeft = Offset(
            rightX - (width * 0.78f),
            centerY - (height * 0.72f),
        ),
        size = Size(width * 0.42f, height * 0.26f),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
    drawCircle(
        color = surfaceColor,
        radius = height * 0.28f,
        center = Offset(rightX - (width * 0.42f), centerY),
    )
}

private fun DrawScope.drawBoundaryMarker(
    x: Float,
    centerY: Float,
    color: Color,
    height: Float,
) {
    drawLine(
        color = color,
        start = Offset(x, centerY - (height / 2f)),
        end = Offset(x, centerY + (height / 2f)),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(
        color = color,
        radius = 5.dp.toPx(),
        center = Offset(x, centerY),
    )
}

private fun DrawScope.drawContinuationArrow(
    startX: Float,
    endX: Float,
    centerY: Float,
    color: Color,
) {
    val arrowHeight = 9.dp.toPx()
    drawLine(
        color = color,
        start = Offset(startX, centerY),
        end = Offset(endX, centerY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(endX - arrowHeight, centerY - arrowHeight),
        end = Offset(endX, centerY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(endX - arrowHeight, centerY + arrowHeight),
        end = Offset(endX, centerY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

internal fun adaptiveScaleFraction(
    distanceMeters: Double,
    scaleMaxMeters: Double,
): Float {
    require(distanceMeters.isFinite() && (distanceMeters >= 0.0))
    require(scaleMaxMeters.isFinite() && (scaleMaxMeters > 0.0))

    return (
        ln1p(distanceMeters) / ln1p(scaleMaxMeters)
    ).toFloat().coerceIn(0f, 1f)
}

private fun formatVisualizationMeters(
    meters: Double,
    formatter: DistanceDisplay,
    metersUnit: String,
): String = "${formatter.number(meters)} $metersUnit"

@Preview(
    name = "Finite DOF narrow",
    widthDp = 320,
    showBackground = true,
)
@Composable
private fun DofVisualizationFinitePreview() {
    DOFCalculatorTheme {
        DofVisualization(
            nearLimitMeters = 3.389,
            focusDistanceMeters = 5.0,
            farLimit = CalculatedDistance.Finite(9.527),
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Preview(
    name = "Infinite DOF narrow",
    widthDp = 320,
    showBackground = true,
)
@Composable
private fun DofVisualizationInfinitePreview() {
    DOFCalculatorTheme(themeMode = ThemeMode.Dark) {
        DofVisualization(
            nearLimitMeters = 6.9,
            focusDistanceMeters = 12.0,
            farLimit = CalculatedDistance.Infinity,
            modifier = Modifier.padding(8.dp),
        )
    }
}

private const val INFINITY_SYMBOL = "∞"
