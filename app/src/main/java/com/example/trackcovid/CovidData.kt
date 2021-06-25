package com.example.trackcovid

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import java.util.*


// JSON response from server is the following format:
// { active : [{ CovidDataDetails item #1}, { CovidDataDetails item #2}, ... ]}

data class CovidData (
    @SerializedName("active") val active :List<CovidDataDetails>
)


data class CovidDataDetails(
    @SerializedName("date_active") val dateActive : Date,
    @SerializedName("active_cases") val activeCases : Int,
    @SerializedName("active_cases_change") val activeCasesChange : Int,
    @SerializedName("cumulative_deaths") val cumulativeDeaths : Int,
    @SerializedName("province") val province : String
)

