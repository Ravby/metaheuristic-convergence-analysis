package si.um.feri.lpm

import org.jetbrains.letsPlot.geom.geomLine
import org.jetbrains.letsPlot.geom.geomPoint
import org.jetbrains.letsPlot.geom.geomRibbon
import org.jetbrains.letsPlot.geom.geomVLine
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.label.labs
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.scaleXContinuous
import org.jetbrains.letsPlot.scale.scaleYContinuous
import org.jetbrains.letsPlot.themes.elementText
import org.jetbrains.letsPlot.themes.theme
import java.io.File

data class EvalMetrics(val rating: Double, val deviation: Double)

data class AlgorithmPerformance(
    var name: String = "",
    val evalMetrics: MutableList<EvalMetrics> = mutableListOf()
)

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
    xAxisBreaks: Int = 1
): Plot {
    var plot = letsPlot()

    val xValues = 0 until algorithms.first().evalMetrics.size   // x-axis values representing different evaluation points

    for (i in 0 until algorithms.size) {    // iterate through each algorithm
        val yValues = algorithms[i].evalMetrics.map { it.rating } // extract rating values

        // calculate confidence interval bounds
        val lowerBounds = algorithms[i].evalMetrics.map { it.rating - 2 * it.deviation }
        val upperBounds = algorithms[i].evalMetrics.map { it.rating + 2 * it.deviation }

        // prepare data mapping for plotting
        val data = mapOf(
            "cutpoint" to xValues,
            "rating" to yValues,
            "lowerBounds" to lowerBounds,
            "upperBounds" to upperBounds,
            "Algorithm" to List(yValues.size) { algorithms[i].name }  // algorithm name labels
        )

        // add rating line for the current algorithm
        plot += geomLine(data = data) {
            x = "cutpoint"
            y = "rating"
            color = "Algorithm"
            linetype = "Algorithm"
        }

        // add a shaded confidence interval around the rating line
        plot += geomRibbon(data = data, alpha = 0.1, color = "rgba(0, 0, 0, 0)", showLegend = false) {
            x = "cutpoint"
            ymin = "lowerBounds"
            ymax = "upperBounds"
            fill = "Algorithm"
        }

        // add points at the rating positions
        plot += geomPoint(data = data) {
            x = "cutpoint"
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
    val minX = minX ?: 0
    val maxX = maxX ?: (xValues.maxOrNull() ?: 0)

    if (showVerticalLines) {
        val verticalLines = (0..xValues.toList().size - 1 step xAxisBreaks).toList()
        verticalLines.forEach { evalPoint ->
            plot += geomVLine(xintercept = evalPoint, color = "lightgrey", linetype = "dashed")
        }
    }

    // define x-axis breaks for labeling, filtering for specific cases
    val filteredXValues = (xValues.filter { it % xAxisBreaks == 0 })

    // plot x breaks and labels using 'xValues' instead of 'filteredXValues' for CEC 2017 and CEC 2022
    plot += scaleXContinuous(
        breaks = filteredXValues.toList(),
        labels = filteredXValues.map { it.toInt().toString() }, // specify the tick marks
        limits = minX to maxX,
        expand = listOf(0, 0)   // remove left padding on the x-axis
    )

    plot += ggsize(width = width, height = height)  // set plot size

    // set axis labels and legend
    plot += labs(
        x = "Cutpoint k",
        y = "Rating",
        color = "Algorithm"
    )

    // apply theme settings for text formatting
    plot += theme(
        title = elementText(face = "bold"), // 'title' applies to plot's title, subtitle, caption
        axisTextX = elementText(size = 24, angle = 0),
        axisTitleY = elementText(size = 28),
        axisTextY = elementText(size = 24),
        legendTitle = elementText(size = 28),
        legendText = elementText(size = 24)
    )

    return plot
}