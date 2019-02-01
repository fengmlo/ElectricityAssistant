package me.fengmlo.electricityassistant.ui.applist

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.pm.PackageInfo
import me.fengmlo.electricityassistant.App

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val appList: MutableLiveData<List<PackageInfo>?> = MutableLiveData()

    fun getAppList(): LiveData<List<PackageInfo>?> {
        appList.value = getApplication<App>().packageManager.getInstalledPackages(0)
        return appList
    }
}