package com.example.trackcovid

import android.graphics.RectF
import com.robinhood.spark.SparkAdapter



class CovidSparkAdapter(private val dailyData: List<CovidDataDetails>) : SparkAdapter() {

    var metric = Metric.ACTIVE
    var daysAgo = TimeScale.ALL

    override fun getCount(): Int = dailyData.size
    override fun getItem(index: Int): Any = dailyData[index]

    // Display the corresponding data depending on user metric options
    override fun getY(index: Int): Float {
        val chosenData = dailyData[index]

        return when (metric){
            Metric.ACTIVE -> chosenData.activeCases.toFloat()
            Metric.CHANGE -> chosenData.activeCasesChange.toFloat()
            Metric.DEATH -> chosenData.cumulativeDeaths.toFloat()
        }
    }

    // Set data bounds for the graph depending on the metric options.
    // To reduce scope of graph to recent cases, just need to set bound on the left side (older dates)
    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if (daysAgo != TimeScale.ALL) {
            bounds.left = count - daysAgo.numDays.toFloat()
        }
        return bounds
    }

    // Display base line for Change case to easily identify +/-
    override fun hasBaseLine(): Boolean {
        return when (metric){
            Metric.ACTIVE -> false
            Metric.CHANGE -> true
            Metric.DEATH -> false
        }
    }

}
