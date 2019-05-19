package me.fengmlo.electricityassistant.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import me.fengmlo.electricityassistant.database.entity.ElectricityFee;

import java.util.List;

@Dao
public interface ElectricityFeeDao {

    @Query("SELECT * FROM ELECTRICITY_FEE")
    LiveData<List<ElectricityFee>> loadAll();

    @Query("SELECT * FROM ELECTRICITY_FEE")
    List<ElectricityFee> loadAllSync();

    @Query("SELECT * FROM ELECTRICITY_FEE WHERE year = :year")
    LiveData<List<ElectricityFee>> loadElectricityFeeByYear(int year);

    @Query("SELECT * FROM ELECTRICITY_FEE WHERE year=:year AND month = :month")
    LiveData<List<ElectricityFee>> loadElectricityFeeByMonth(int year, int month);

    @Query("SELECT * FROM ELECTRICITY_FEE WHERE year=:year AND month = :month AND day=:day")
    ElectricityFee getElectricityFeeByDaySync(int year, int month, int day);

    @Query("SELECT * FROM ELECTRICITY_FEE ORDER BY id DESC LIMIT 1")
    ElectricityFee getLastElectricityFeeSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ElectricityFee fee);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ElectricityFee> fees);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(ElectricityFee fee);

    @Delete
    void delete(ElectricityFee fee);
}
