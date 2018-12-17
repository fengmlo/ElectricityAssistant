package me.fengmlo.electricityassistant;

import java.math.BigDecimal;

public class Util {

    public static double roundDouble(double value) {
        return new BigDecimal(value).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}
