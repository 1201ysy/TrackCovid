package com.example.trackcovid

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// GET from the servers
interface CovidService {

    @GET("timeseries")
    fun getNationalData(@Query("loc") loc:String): Call<CovidData>

    @GET("timeseries")
    fun getProvinceData(): Call<CovidData>
}