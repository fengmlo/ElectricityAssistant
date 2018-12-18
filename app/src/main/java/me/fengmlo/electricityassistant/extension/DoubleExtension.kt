package me.fengmlo.electricityassistant.extension

import java.math.BigDecimal

fun Double.roundHalfUp() = BigDecimal(this).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()