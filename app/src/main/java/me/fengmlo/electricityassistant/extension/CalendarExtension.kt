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

operator fun Calendar.minus(other: Calendar): Int {
    var d1 = this.clone() as Calendar
    var d2 = other.clone() as Calendar
    if (d1.after(d2)) { // swap dates so that d1 is start and d2 is end
        val swap = d1
        d1 = d2
        d2 = swap
    }
    var days = d2.get(Calendar.DAY_OF_YEAR) - d1.get(Calendar.DAY_OF_YEAR)
    val y2 = d2.get(Calendar.YEAR)
    if (d1.year != y2) {
        d1 = d1.clone() as Calendar
        do {
            days += d1.getActualMaximum(Calendar.DAY_OF_YEAR)//得到当年的实际天数
            d1.add(Calendar.YEAR, 1)
        } while (d1.get(Calendar.YEAR) != y2)
    }
    return if (this.after(d2)) days else -days
}