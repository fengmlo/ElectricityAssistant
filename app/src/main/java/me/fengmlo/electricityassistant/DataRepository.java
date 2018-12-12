package me.fengmlo.electricityassistant;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import me.fengmlo.electricityassistant.database.AppDatabase;
import me.fengmlo.electricityassistant.database.entity.ElectricityFee;

import java.util.List;

public class DataRepository {

    private static DataRepository sInstance;

    private final AppDatabase mDatabase;
    private MediatorLiveData<List<ElectricityFee>> mObservableFees;

    private DataRepository(final AppDatabase database) {
        mDatabase = database;
        mObservableFees = new MediatorLiveData<>();

        mObservableFees.addSource(mDatabase.getElectricityFeeDao().loadAll(),
                electricityFees -> {
                    if (mDatabase.getDatabaseCreated().getValue() != null) {
                        mObservableFees.postValue(electricityFees);
                    }
                });
    }

    public static DataRepository getInstance(final AppDatabase database) {
        if (sInstance == null) {
            synchronized (DataRepository.class) {
                if (sInstance == null) {
                    sInstance = new DataRepository(database);
                }
            }
        }
        return sInstance;
    }

    /**
     * Get the list of products from the database and get notified when the data changes.
     */
    public LiveData<List<ElectricityFee>> getElectricityFees() {
        return mObservableFees;
    }

}
