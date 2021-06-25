package com.example.trackcovid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.ticker.TickerUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.android.synthetic.main.activity_main.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://api.opencovid.ca/"
private const val TAG = "MainActivity"
private const val ALL_PROVINCE = "All (Nationwide)"

class MainActivity : AppCompatActivity() {

    private lateinit var currentlyShownData:  List<CovidDataDetails>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var perProvinceDailyData: Map<String, List<CovidDataDetails>>
    private lateinit var nationalDailyData: CovidData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = getString(R.string.app_description)

        // Initialize Retrofit and Gson to convert JSON response from Retrofit GET requests to Objects
        // Date Format example 26-01-2020

        val gson = GsonBuilder().setDateFormat("dd-MM-yyyy").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val covidService = retrofit.create(CovidService::class.java)

        //Fetch the national data
        covidService.getNationalData("canada").enqueue(object: Callback<CovidData>{
            override fun onResponse(
                call: Call<CovidData>,
                response: Response<CovidData>
            ) {
                Log.i(TAG, "onResponse $response")
                val nationalData = response.body()
                if (nationalData == null){
                    Log.w(TAG, "Did nto receive a valid response body")
                    return
                }
                setupEventListeners()
                nationalDailyData = nationalData
                Log.i(TAG, "Update graph with national data")
                
                updateDisplayWithData(nationalDailyData)

            }

            override fun onFailure(call: Call<CovidData>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

        })


        //Fetch the provincial data
        covidService.getProvinceData().enqueue(object: Callback<CovidData> {
            override fun onResponse(
                call: Call<CovidData>,
                response: Response<CovidData>
            ) {
                Log.i(TAG, "onResponse $response")
                val provinceData = response.body()
                if (provinceData == null) {
                    Log.w(TAG, "Did nto receive a valid response body")
                    return
                }
                // Sort response grouped by province
                perProvinceDailyData = provinceData.active.groupBy{ it.province}
                Log.i(TAG, "Update spinner with province names")

                updateSpinnerWithProvinceData(perProvinceDailyData.keys)
            }

            override fun onFailure(call: Call<CovidData>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }
        })



    }

    private fun updateSpinnerWithProvinceData(provinceName: Set<String>) {
        // Set spinner names from a set of Province names
        val provinceAbbreviationList = provinceName.toMutableList()
        provinceAbbreviationList.sort()
        provinceAbbreviationList.add(0, ALL_PROVINCE)

        // Add province list to data source for the spinner
        spinnerSelect.attachDataSource(provinceAbbreviationList)
        spinnerSelect.setOnSpinnerItemSelectedListener{ parent, _, position, _ ->
            var selectedProvince = parent.getItemAtPosition(position) as String
            var provinceData = perProvinceDailyData[selectedProvince]?.let { CovidData(active = it) }
            val selectedData = provinceData ?: nationalDailyData

            updateDisplayWithData(selectedData)

        }

    }

    private fun setupEventListeners() {
        tickerView.setCharacterLists(TickerUtils.provideNumberList())

        // Add a listener for the user scrubbing on the chart
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener {itemData ->
            if (itemData is CovidDataDetails){
                updateInfoForDate(itemData)
            }
        }

        // Respond to radio button selected events
        // for the Timescale radio button
        radioGroupTimeSelection.setOnCheckedChangeListener{ _, checkedId ->
            adapter.daysAgo = when (checkedId){
                R.id.radioButtonMonth -> TimeScale.MONTH
                R.id.radioButtonWeek -> TimeScale.WEEK
                else -> TimeScale.ALL
            }
            adapter.notifyDataSetChanged()
        }
        // for the Metric radio button
        radioGroupMetricSelection.setOnCheckedChangeListener{ _, checkedId ->
            when (checkedId){
                R.id.radioButtonActiveCase -> updateDisplayMetric(Metric.ACTIVE)
                R.id.radioButtonActiveCaseChange ->  updateDisplayMetric(Metric.CHANGE)
                R.id.radioButtonCumulativeDeath ->  updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        // Update color of the chart
        val colorRes = when (metric){
            Metric.DEATH -> R.color.Death
            Metric.ACTIVE -> R.color.Active
            Metric.CHANGE -> R.color.Change
        }

        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)
        sparkView.lineColor = colorInt
        tickerView.setTextColor(colorInt)

        // Update the metric on the adapter
        adapter.metric = metric
        adapter.notifyDataSetChanged()

        // Reset Number and date shown in the bottom text views
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(covidData: CovidData) {
        // Create a new SparkAdapter with the data
        // Update radio buttons to select the active cases and max time by default
        // Display metric for the most recent date
        val dailyData : List<CovidDataDetails> = covidData.active
        currentlyShownData = dailyData
        adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        radioButtonAll.isChecked = true
        radioButtonActiveCaseChange.isChecked = true
        updateDisplayMetric(Metric.CHANGE)

    }

    private fun updateInfoForDate(covidDataItem: CovidDataDetails) {
        // update the date and case numbers for the UI to match user selected settings
        val numCases = when (adapter.metric) {
            Metric.ACTIVE -> covidDataItem.activeCases
            Metric.CHANGE -> covidDataItem.activeCasesChange
            Metric.DEATH -> covidDataItem.cumulativeDeaths
        }

        tickerView.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.CANADA)
        tvDateLabel.text = outputDateFormat.format(covidDataItem.dateActive)
    }
}