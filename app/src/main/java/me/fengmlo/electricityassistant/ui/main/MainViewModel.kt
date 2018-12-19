package me.fengmlo.electricityassistant.ui.main

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.fengmlo.electricityassistant.App
import me.fengmlo.electricityassistant.Util
import me.fengmlo.electricityassistant.database.entity.ElectricityFee
import me.fengmlo.electricityassistant.database.entity.Recharge
import me.fengmlo.electricityassistant.extension.day
import me.fengmlo.electricityassistant.extension.month
import me.fengmlo.electricityassistant.extension.year
import java.io.*
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    fun getElectricityFees(): LiveData<List<ElectricityFee>> = App.getDB().electricityFeeDao.loadAll()

    fun exportDatabase() {
        val output = File(Environment.getExternalStorageDirectory().path, "electricity_fee.json")
        val electricityFee = App.getDB().electricityFeeDao.loadAllSync()
        val jsonString = Gson().toJson(electricityFee)

        val recharge = App.getDB().rechargeDao.loadAllSync()
        val rechargeString = Gson().toJson(recharge)

        var outputStream: OutputStream? = null
        try {
            outputStream = output.outputStream()
            PrintStream(outputStream).apply {
                println(jsonString)
                println(rechargeString)
                flush()
                close()
            }
        } finally {
            outputStream?.close()
        }
    }

    fun importDatabase(inputStream: InputStream) {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val electricityFee =
                Gson().fromJson<List<ElectricityFee>>(reader.readLine(), object : TypeToken<List<ElectricityFee>>() {}.type)
        val recharge =
                Gson().fromJson<List<Recharge>>(reader.readLine(), object : TypeToken<List<Recharge>>() {}.type)
        App.getDB().electricityFeeDao.insertAll(electricityFee)
        App.getDB().rechargeDao.insertAll(recharge)
    }

    fun recordCharge(money: Double) {
        val today = Calendar.getInstance()
        val rechargeDao = App.getDB().rechargeDao
        val lastRecharge = rechargeDao.lastRechargeSync
        val newRecharge = Recharge()
        newRecharge.year = today.year
        newRecharge.month = today.month
        newRecharge.day = today.day
        newRecharge.charge = Util.roundDouble(money)
        if (lastRecharge == null) {
            newRecharge.id = 1
        } else {
            newRecharge.id = lastRecharge.id + 1
        }
        rechargeDao.insert(newRecharge)
    }
}
