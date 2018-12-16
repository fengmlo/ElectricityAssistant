package me.fengmlo.electricityassistant.ui.main

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.fengmlo.electricityassistant.App
import me.fengmlo.electricityassistant.database.entity.ElectricityFee
import java.io.File
import java.io.OutputStream
import java.io.PrintStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    fun getElectricityFees(): LiveData<List<ElectricityFee>> = App.getDB().electricityFeeDao.loadAll()

    fun exportDatabase() {
        val output = File(Environment.getExternalStorageDirectory().path, "electricity_fee.json")
        val electricityFee = App.getDB().electricityFeeDao.loadAllSync()
        val jsonString = Gson().toJson(electricityFee)
        var outputStream: OutputStream? = null
        try {
            outputStream = output.outputStream()
            PrintStream(outputStream).apply {
                println(jsonString)
                flush()
                close()
            }
        } finally {
            outputStream?.close()
        }
    }

    fun importDatabase(jsonString: String) {
        val electricityFee =
            Gson().fromJson<List<ElectricityFee>>(jsonString, object : TypeToken<List<ElectricityFee>>() {}.type)
        App.getDB().electricityFeeDao.insertAll(electricityFee)
    }
}
