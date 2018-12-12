package me.fengmlo.electricityassistant.ui.main

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import me.fengmlo.electricityassistant.App
import me.fengmlo.electricityassistant.database.entity.ElectricityFee

class MainViewModel(application: Application) : AndroidViewModel(application) {

    fun getElectricityFees(): LiveData<List<ElectricityFee>> = App.getDB().electricityFeeDao.loadAll()
}
