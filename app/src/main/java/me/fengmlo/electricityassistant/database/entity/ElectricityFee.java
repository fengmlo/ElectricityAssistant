package me.fengmlo.electricityassistant.database.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import org.jetbrains.annotations.NotNull;

@Entity(tableName = "electricity_fee", indices = {@Index("year"), @Index(value = {"year", "month"}), @Index(value = {"year", "month", "day"})})
public class ElectricityFee {

    @PrimaryKey
    private int id;

    private double balance;

    private int year;

    private int month;

    private int day;

    private double cost;

    @NotNull
    @Override
    public String toString() {
        return "ElectricityFee{" +
                "id=" + id +
                ", balance=" + balance +
                ", year=" + year +
                ", month=" + month +
                ", day=" + day +
                ", cost=" + cost +
                '}';
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
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

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
