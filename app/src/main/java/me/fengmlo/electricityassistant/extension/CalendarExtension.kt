package me.fengmlo.electricityassistant.extension

import java.util.*

fun Calendar?.yesterday(): Calendar {
    val result = Calendar.getInstance()
    if (this != null) result.time = time
    result.add(Calendar.DAY_OF_MONTH, -1)
    return result
}

var Calendar.year
    get() = get(Calendar.YEAR)
    set(value) {
        set(Calendar.YEAR, value)
    }

var Calendar.month
    get() = get(Calendar.MONTH)
    set(value) {
        set(Calendar.MONTH, value)
    }

var Calendar.day
    get() = get(Calendar.DAY_OF_MONTH)
    set(value) {
        set(Calendar.DAY_OF_MONTH, value)
    }

var Calendar.hour
    get() = get(Calendar.HOUR_OF_DAY)
    set(value) {
        set(Calendar.HOUR_OF_DAY, value)
    }

var Calendar.minute
    get() = get(Calendar.MINUTE)
    set(value) {
        set(Calendar.MINUTE, value)
    }

var Calendar.second
    get() = get(Calendar.SECOND)
    set(value) {
        set(Calendar.SECOND, value)
    }

operator fun Calendar.minus(other: Calendar): Int {
    var start = this.clone() as Calendar
    var end = other.clone() as Calendar
    if (start.after(end)) { // swap dates so that d1 is start and d2 is end
        val swap = start
        start = end
        end = swap
    }
    var days = end.get(Calendar.DAY_OF_YEAR) - start.get(Calendar.DAY_OF_YEAR)
    val endYear = end.get(Calendar.YEAR)
    if (start.year != endYear) {
        start = start.clone() as Calendar
        do {
            days += start.getActualMaximum(Calendar.DAY_OF_YEAR)//得到当年的实际天数
            start.add(Calendar.YEAR, 1)
        } while (start.get(Calendar.YEAR) != endYear)
    }
    return if (this.after(other)) days else -days
}