package me.fengmlo.electricityassistant.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import me.fengmlo.electricityassistant.database.entity.Recharge;

@Dao
public interface RechargeDao {

    @Query("SELECT * FROM RECHARGE")
    LiveData<List<Recharge>> loadAll();

    @Query("SELECT * FROM RECHARGE")
    List<Recharge> loadAllSync();

    @Query("SELECT * FROM RECHARGE ORDER BY id DESC LIMIT 1")
    Recharge getLastRechargeSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Recharge> recharges);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Recharge recharge);
}
