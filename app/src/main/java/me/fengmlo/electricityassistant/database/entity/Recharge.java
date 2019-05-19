package me.fengmlo.electricityassistant.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "recharge")
public class Recharge {

    @PrimaryKey
    private int id;

    private double charge;

    private int year;

    private int month;

    private int day;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getCharge() {
        return charge;
    }

    public void setCharge(double charge) {
        this.charge = charge;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    @NotNull
    @Override
    public String toString() {
        return "Recharge{" +
                "id=" + id +
                ", charge=" + charge +
                ", year=" + year +
                ", month=" + month +
                ", day=" + day +
                '}';
    }
}
