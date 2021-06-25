package com.example.trackcovid


// Radiobutton Metric options
enum class Metric {
    ACTIVE, CHANGE, DEATH
}

// Radiobutton Timescale options
enum class TimeScale(val numDays: Int){
    WEEK(7), MONTH (30), ALL (-1)
}