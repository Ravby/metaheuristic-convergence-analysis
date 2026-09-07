package si.um.feri.lpm

import org.jetbrains.letsPlot.geom.geomLabel
import org.jetbrains.letsPlot.geom.geomLine
import org.jetbrains.letsPlot.geom.geomPoint
import org.jetbrains.letsPlot.geom.geomRect
import org.jetbrains.letsPlot.geom.geomRibbon
import org.jetbrains.letsPlot.geom.geomVLine
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.label.labs
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.scaleXContinuous
import org.jetbrains.letsPlot.scale.scaleYContinuous
import org.jetbrains.letsPlot.scale.scaleYReverse
import org.jetbrains.letsPlot.themes.elementBlank
import org.jetbrains.letsPlot.themes.elementText
import org.jetbrains.letsPlot.themes.theme
import java.io.File
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

data class EvalMetrics(val rating: Double, val deviation: Double)

data class AlgorithmPerformance(
    var name: String = "",
    val evalMetrics: MutableList<EvalMetrics> = mutableListOf()
)

/**
 * What the x-axis of a rating interval band plot measures.
 *
 * [CUTPOINT] labels the raw cutpoint index, which only reads correctly when every cutpoint is
 * drawn. [BUDGET_PERCENT] labels the share of the evaluation budget consumed, which is invariant
 * to both the benchmark's cutpoint count and to `cutpointStep`.
 */
enum class XAxisMode { CUTPOINT, BUDGET_PERCENT }

/**
 * Share of the evaluation budget consumed at [cutpoint], in percent. Cutpoint i covers (i + 1) of
 * [cutpointCount] equal slices of the budget, so the last cutpoint lands exactly on 100 % and the
 * first sits just past 0 (0.1 % for the 1000 cutpoints of CEC2024, 6.25 % for the 16 of CEC2022).
 */
private fun budgetPercent(cutpoint: Int, cutpointCount: Int): Double =
    (cutpoint + 1) * 100.0 / cutpointCount

/** One decimal place, but only when it carries information: 0.2, 7.5, 17.8, 100. */
private fun formatBudgetPercent(percent: Double): String {
    val rounded = (percent * 10.0).roundToInt() / 10.0
    return if (rounded == rounded.toInt().toDouble()) rounded.toInt().toString()
    else String.format(Locale.ROOT, "%.1f", rounded)
}

private fun xAxisTitle(xAxisMode: XAxisMode): String = when (xAxisMode) {
    XAxisMode.CUTPOINT -> "Cutpoint k"
    XAxisMode.BUDGET_PERCENT -> "Evaluation budget (%)"
}

fun loadAlgorithmResults(benchmarkId: BenchmarkId, dirPath: String): List<AlgorithmPerformance> {
    val algorithms: MutableList<AlgorithmPerformance> = mutableListOf()

    val directory = File(dirPath)

    if (directory.exists() && directory.isDirectory) {
        val files = directory.listFiles()
        println("Reading files from folder \"${directory}\"...")
        if (files != null) {
            for (file in files) {
                if (file.isFile && file.name.endsWith("rating_interval_band.txt")) {
                    println("\t${file.name}")
                    val algorithm = AlgorithmPerformance()
                    file.useLines { lines ->
                        val iterator = lines.iterator()
                        if (iterator.hasNext()) {
                            algorithm.name = iterator.next()
                            if (algorithm.name !in BenchmarkManager.get(benchmarkId).algorithms)
                            //if (algorithm.name == "GAOA" || algorithm.name == "GWO" || algorithm.name == "LSHADE" || algorithm.name == "SSA")
                                algorithm.name += "*"
                        }
                        iterator.forEachRemaining { line ->
                            val parts = line.split(" ")
                            if (parts.size == 2) {
                                val rating = parts[0].toDoubleOrNull()
                                val deviation = parts[1].toDoubleOrNull()
                                if (rating != null && deviation != null) {
                                    algorithm.evalMetrics.add(EvalMetrics(rating, deviation))
                                } else {
                                    throw IllegalArgumentException("Wrong data format!")
                                }
                            } else {
                                throw IllegalArgumentException("Wrong data format!")
                            }
                        }
                        algorithms.add(algorithm)
                    }
                }
            }
        } else {
            println("No files found in the directory.")
        }
    } else {
        println("Directory not found!")
    }

    // check if all algorithms have the same number of evaluations
    val evalCounter = algorithms.first().evalMetrics.size
    algorithms.drop(1).forEach { algorithm ->
        require(evalCounter == algorithm.evalMetrics.size) {
            "All algorithms must have the same number of evaluations!"
        }
    }

    return algorithms.toList()
}

fun generatePlot(
    algorithms: List<AlgorithmPerformance>,
    width: Int = 1500, height: Int = 750,
    minX: Double? = null, maxX: Double? = null,
    minY: Double? = null, maxY: Double? = null,
    showVerticalLines: Boolean = true,
    xAxisMode: XAxisMode = XAxisMode.CUTPOINT,
    xAxisBreaks: Int? = null,   // tick spacing in the units of the active axis
    cutpointStep: Int = 1
): Plot {
    var plot = letsPlot()

    val cutpointCount = algorithms.first().evalMetrics.size
    val lastCutpoint = cutpointCount - 1

    // cutpoints to draw: every cutpointStep-th one, anchored on the last cutpoint so it is always
    // included, with the first cutpoint always added as well
    // (1000 cutpoints, cutpointStep = 2 -> 0, 1, 3, 5, ... 999)
    val xValues = (listOf(0) + (lastCutpoint downTo 0 step cutpointStep)).distinct().sorted()

    fun xCoord(cutpoint: Int): Double = when (xAxisMode) {
        XAxisMode.CUTPOINT -> cutpoint.toDouble()
        XAxisMode.BUDGET_PERCENT -> budgetPercent(cutpoint, cutpointCount)
    }

    // names the data column, which is what Lets-Plot shows in tooltips
    val xColumn = when (xAxisMode) {
        XAxisMode.CUTPOINT -> "cutpoint"
        XAxisMode.BUDGET_PERCENT -> "budget"
    }

    for (i in 0 until algorithms.size) {    // iterate through each algorithm
        val evalMetrics = xValues.map { algorithms[i].evalMetrics[it] }
        val yValues = evalMetrics.map { it.rating } // extract rating values

        // calculate confidence interval bounds
        val lowerBounds = evalMetrics.map { it.rating - 2 * it.deviation }
        val upperBounds = evalMetrics.map { it.rating + 2 * it.deviation }

        // prepare data mapping for plotting
        val data = mapOf(
            xColumn to xValues.map(::xCoord),
            "rating" to yValues,
            "lowerBounds" to lowerBounds,
            "upperBounds" to upperBounds,
            "Algorithm" to List(yValues.size) { algorithms[i].name }  // algorithm name labels
        )

        // add rating line for the current algorithm
        plot += geomLine(data = data) {
            x = xColumn
            y = "rating"
            color = "Algorithm"
            linetype = "Algorithm"
        }

        // add a shaded confidence interval around the rating line
        plot += geomRibbon(data = data, alpha = 0.1, color = "rgba(0, 0, 0, 0)", showLegend = false) {
            x = xColumn
            ymin = "lowerBounds"
            ymax = "upperBounds"
            fill = "Algorithm"
        }

        // add points at the rating positions
        plot += geomPoint(data = data) {
            x = xColumn
            y = "rating"
            color = "Algorithm"
            shape = "Algorithm"
        }
    }

    // determine y-axis limits dynamically if not provided
    val allRatings = algorithms.flatMap { it.evalMetrics.map { it.rating } }
    val minY = minY ?: (allRatings.min() - 2 * 50)  //
    val maxY = maxY ?: (allRatings.max() + 2 * 50)

    plot += scaleYContinuous(limits = minY to maxY) // set y-axis limits

    // determine x-axis limits dynamically if not provided
    val axisMax = when (xAxisMode) {
        XAxisMode.CUTPOINT -> lastCutpoint.toDouble()
        XAxisMode.BUDGET_PERCENT -> 100.0
    }
    val minX = minX ?: 0.0
    val maxX = maxX ?: axisMax

    val breakStep = xAxisBreaks ?: when (xAxisMode) {
        XAxisMode.CUTPOINT -> 1
        XAxisMode.BUDGET_PERCENT -> 10
    }

    // tick marks and grid lines span the full axis range, independent of which cutpoints are drawn
    val breakValues = when (xAxisMode) {
        XAxisMode.CUTPOINT -> (0..lastCutpoint).filter { it % breakStep == 0 }.map { it.toDouble() }
        XAxisMode.BUDGET_PERCENT -> (0..100 step breakStep).map { it.toDouble() }
    }

    if (showVerticalLines) {
        breakValues.forEach { evalPoint ->
            plot += geomVLine(xintercept = evalPoint, color = "lightgrey", linetype = "dashed")
        }
    }

    plot += scaleXContinuous(
        breaks = breakValues,
        labels = breakValues.map { it.toInt().toString() }, // specify the tick marks
        limits = minX to maxX,
        expand = listOf(0, 0)   // remove left padding on the x-axis
    )

    plot += ggsize(width = width, height = height)  // set plot size

    // set axis labels and legend
    plot += labs(
        x = xAxisTitle(xAxisMode),
        y = "Rating",
        color = "Algorithm"
    )

    // apply theme settings for text formatting
    plot += theme(
        title = elementText(face = "bold"), // 'title' applies to plot's title, subtitle, caption
        axisTitleX = elementText(size = 28),
        axisTextX = elementText(size = 24, angle = 0),
        axisTitleY = elementText(size = 28),
        axisTextY = elementText(size = 24),
        legendTitle = elementText(size = 28),
        legendText = elementText(size = 24)
    )

    return plot
}

/**
 * Where algorithm names are written on a ranking plot. The legend identifies the lines in every
 * case, so [NONE] loses no information; [ENDS] names them only in the first and last column, and
 * [ALL] in every column, as figure 6 of MO-IOHinspector does.
 */
enum class AlgorithmLabels { NONE, ENDS, ALL }

/** One algorithm's position in a ranking at a single cutpoint. */
data class RankedAlgorithm(
    val name: String,
    val rank: Int,
    val tieGroup: Int,
    val rating: Double,
    val deviation: Double
)

/**
 * Ranks algorithms by rating at [cutpoint], best first, and groups the ones that cannot be told
 * apart.
 *
 * Two algorithms count as tied when their rating intervals overlap, using the same
 * rating +- [deviationMultiplier] * deviation interval that generatePlot shades as a ribbon.
 * Groups are built by chaining: walking down the ranking, an algorithm joins the current group when
 * its interval still overlaps ANY member of that group, and opens a new group otherwise.
 *
 * That leaves one guarantee worth relying on - algorithms in different groups have intervals that do
 * not overlap at all. The converse does NOT hold: because groups chain, the best and worst member of
 * one group can still be far enough apart to be separable, so sharing a group is not evidence of
 * equivalence.
 *
 * This is an interval-overlap heuristic, NOT the bootstrap significance test of the robustranking
 * package used by MO-IOHinspector - bootstrapping resamples the problem set, and the per-problem
 * results it would need are already aggregated away in the rating interval band files. It is also
 * conservative: non-overlapping intervals do imply a difference, but overlapping ones do not imply
 * the absence of one.
 */
fun robustRanking(
    algorithms: List<AlgorithmPerformance>,
    cutpoint: Int,
    deviationMultiplier: Double = 2.0
): List<RankedAlgorithm> {
    val sorted = algorithms.sortedByDescending { it.evalMetrics[cutpoint].rating }

    var tieGroup = 0
    var groupLowerBound = Double.MAX_VALUE
    return sorted.mapIndexed { index, algorithm ->
        val metrics = algorithm.evalMetrics[cutpoint]
        val lowerBound = metrics.rating - deviationMultiplier * metrics.deviation
        val upperBound = metrics.rating + deviationMultiplier * metrics.deviation
        if (index == 0 || upperBound < groupLowerBound) {
            tieGroup++              // reaches no member of the current group, so it opens a new one
            groupLowerBound = lowerBound
        } else {
            // the group reaches as far down as its widest member, not just its leading one
            groupLowerBound = minOf(groupLowerBound, lowerBound)
        }
        RankedAlgorithm(algorithm.name, index + 1, tieGroup, metrics.rating, metrics.deviation)
    }
}

/**
 * [columns] cutpoint indices spaced logarithmically over [cutpointCount] cutpoints, so the early
 * search gets as much horizontal room as the long tail. Duplicates are dropped, so a benchmark with
 * few cutpoints yields fewer than [columns] indices.
 */
fun logarithmicCutpoints(cutpointCount: Int, columns: Int = 8): List<Int> =
    (1..columns)
        .map { cutpointCount.toDouble().pow(it.toDouble() / columns).roundToInt() }
        .map { it.coerceIn(1, cutpointCount) - 1 }
        .distinct()

/**
 * Ranking over time: one column per cutpoint, algorithms ordered best-to-worst top-down, lines
 * tracking each algorithm across columns, and a grey box around each group of statistically tied
 * algorithms. Modelled on figure 6 of MO-IOHinspector (Rook et al., robust ranking).
 *
 * [cutpoints] selects the columns; when null, [columns] of them are picked with
 * [logarithmicCutpoints]. [xAxisMode] labels the columns the same way generatePlot labels its
 * x-axis. [algorithmLabels] controls whether algorithm names are written on the plot itself, and
 * [tieBoxWidth] how wide the grey boxes are, in column widths. See [robustRanking] for how ties are
 * determined.
 */
fun generateRankingPlot(
    algorithms: List<AlgorithmPerformance>,
    width: Int = 1500, height: Int = 750,
    cutpoints: List<Int>? = null,
    columns: Int = 8,
    xAxisMode: XAxisMode = XAxisMode.CUTPOINT,
    deviationMultiplier: Double = 2.0,
    algorithmLabels: AlgorithmLabels = AlgorithmLabels.NONE,
    labelSize: Double = 7.0,
    tieBoxWidth: Double = 0.3
): Plot {
    val cutpointCount = algorithms.first().evalMetrics.size
    val columnCutpoints = cutpoints ?: logarithmicCutpoints(cutpointCount, columns)

    require(columnCutpoints.isNotEmpty()) { "At least one cutpoint is needed for a ranking plot!" }
    columnCutpoints.forEach { cutpoint ->
        require(cutpoint in 0 until cutpointCount) {
            "Cutpoint $cutpoint is outside the available range 0..${cutpointCount - 1}!"
        }
    }

    val rankings = columnCutpoints.map { robustRanking(algorithms, it, deviationMultiplier) }

    var plot = letsPlot()

    // grey box behind each group of tied algorithms, added first so the lines, points and labels
    // are drawn on top of it
    val tiedGroups = rankings.flatMapIndexed { column, ranking ->
        ranking.groupBy { it.tieGroup }.values.filter { it.size > 1 }.map { column to it }
    }
    if (tiedGroups.isNotEmpty()) {
        val halfBox = tieBoxWidth / 2
        plot += geomRect(
            data = mapOf(
                "xmin" to tiedGroups.map { (column, _) -> column - halfBox },
                "xmax" to tiedGroups.map { (column, _) -> column + halfBox },
                "ymin" to tiedGroups.map { (_, group) -> group.minOf { it.rank } - 0.42 },
                "ymax" to tiedGroups.map { (_, group) -> group.maxOf { it.rank } + 0.42 }
            ),
            fill = "#d9d9d9", color = "#9e9e9e", alpha = 0.5, showLegend = false
        ) {
            xmin = "xmin"; xmax = "xmax"; ymin = "ymin"; ymax = "ymax"
        }
    }

    data class Node(val column: Int, val rank: Int, val name: String)

    val nodes = rankings.flatMapIndexed { column, ranking ->
        ranking.map { Node(column, it.rank, it.name) }
    }

    fun nodeData(of: List<Node>) = mapOf(
        "column" to of.map { it.column.toDouble() },
        "rank" to of.map { it.rank.toDouble() },
        "Algorithm" to of.map { it.name }
    )

    val data = nodeData(nodes)

    // one line per algorithm, tracking how its rank moves across the cutpoints
    plot += geomLine(data = data, size = 1.2) {
        x = "column"
        y = "rank"
        color = "Algorithm"
        linetype = "Algorithm"
    }

    plot += geomPoint(data = data, size = 4.5) {
        x = "column"
        y = "rank"
        color = "Algorithm"
        shape = "Algorithm"
    }

    // the legend already identifies every line, so names on the plot are opt-in;
    // the white box keeps them readable where they do appear
    val labelledColumns = when (algorithmLabels) {
        AlgorithmLabels.NONE -> emptySet()
        AlgorithmLabels.ENDS -> setOf(0, columnCutpoints.lastIndex)
        AlgorithmLabels.ALL -> columnCutpoints.indices.toSet()
    }
    if (labelledColumns.isNotEmpty()) {
        plot += geomLabel(
            data = nodeData(nodes.filter { it.column in labelledColumns }),
            size = labelSize, fill = "white", alpha = 0.75,
            labelPadding = 0.15, labelR = 0.3, showLegend = false
        ) {
            x = "column"
            y = "rank"
            label = "Algorithm"
            color = "Algorithm"
        }
    }

    plot += scaleXContinuous(
        breaks = columnCutpoints.indices.map { it.toDouble() },
        labels = columnCutpoints.map { cutpoint ->
            when (xAxisMode) {
                XAxisMode.CUTPOINT -> cutpoint.toString()
                XAxisMode.BUDGET_PERCENT -> formatBudgetPercent(budgetPercent(cutpoint, cutpointCount))
            }
        },
        limits = -0.5 to columnCutpoints.size - 0.5
    )

    // rank 1 belongs at the top
    plot += scaleYReverse(
        breaks = (1..algorithms.size).map { it.toDouble() },
        labels = (1..algorithms.size).map { it.toString() }
    )

    plot += ggsize(width = width, height = height)

    plot += labs(
        x = xAxisTitle(xAxisMode),
        y = "Rank",
        color = "Algorithm"
    )

    plot += theme(
        title = elementText(face = "bold"),
        panelGrid = elementBlank(),     // grid lines carry no meaning between ranks
        axisTitleX = elementText(size = 28),
        axisTextX = elementText(size = 24, angle = 0),
        axisTitleY = elementText(size = 28),
        axisTextY = elementText(size = 24),
        legendTitle = elementText(size = 28),
        legendText = elementText(size = 24)
    )

    return plot
}
