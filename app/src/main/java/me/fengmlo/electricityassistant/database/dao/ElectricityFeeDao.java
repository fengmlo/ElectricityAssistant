package me.fengmlo.electricityassistant.database.dao;

import android.arch.persistence.room.*;
import me.fengmlo.electricityassistant.database.entity.ElectricityFee;

import java.util.List;

@Dao
public interface ElectricityFeeDao {

    @Query("SELECT * FROM ELECTRICITY_FEE")
    List<ElectricityFee> getAll();

    @Query("SELECT * FROM ELECTRICITY_FEE WHERE year = :year")
    List<ElectricityFee> getElectricityFeeByYear(int year);

    @Query("SELECT * FROM ELECTRICITY_FEE WHERE year=:year AND month = :month")
    List<ElectricityFee> getElectricityFeeByMonth(int year, int month);

    @Query("SELECT * FROM ELECTRICITY_FEE WHERE year=:year AND month = :month AND day=:day")
    ElectricityFee getElectricityFeeByDay(int year, int month, int day);

    @Query("SELECT * FROM ELECTRICITY_FEE ORDER BY id DESC LIMIT 1")
    ElectricityFee getLastElectricityFee();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ElectricityFee fee);

    @Delete
    void delete(ElectricityFee fee);
}
