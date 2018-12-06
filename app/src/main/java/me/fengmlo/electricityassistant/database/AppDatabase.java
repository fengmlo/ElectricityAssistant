package me.fengmlo.electricityassistant.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import me.fengmlo.electricityassistant.database.dao.ElectricityFeeDao;
import me.fengmlo.electricityassistant.database.entity.ElectricityFee;

@Database(entities = {ElectricityFee.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ElectricityFeeDao getElectricityFeeDao();
}
