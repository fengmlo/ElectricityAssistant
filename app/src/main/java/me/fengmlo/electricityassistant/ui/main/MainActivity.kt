package me.fengmlo.electricityassistant.ui.main

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.orhanobut.logger.Logger
import com.tbruyelle.rxpermissions.RxPermissions
import me.fengmlo.electricityassistant.R
import me.fengmlo.electricityassistant.base.BaseActivity
import me.fengmlo.electricityassistant.bindView
import me.fengmlo.electricityassistant.extension.showToast
import me.fengmlo.electricityassistant.util.ToastUtil
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

const val APP_PACKAGE_NAME = "com.sgcc.cs" // 包名
private const val FILE_SELECT_CODE = 1

class MainActivity : BaseActivity() {

    private val lcMonth: LineChart by bindView(R.id.lc_month)
    private val btOpenApp: Button by bindView(R.id.bt_open_app)
    private val btExport: Button by bindView(R.id.bt_export)
    private val btImport: Button by bindView(R.id.bt_import)

    private lateinit var model: MainViewModel

    override fun getLayoutId(): Int = R.layout.activity_main

    override fun initView() {
        model = ViewModelProviders.of(this).get(MainViewModel::class.java)
        model.getElectricityFees().observe(this, Observer { list ->
            if (list.isNullOrEmpty()) return@Observer
            val dataSet = LineDataSet(list.map { Entry(it.day.toFloat(), it.cost.toFloat()) }, "月度用电情况")
            lcMonth.data = LineData(dataSet)
            lcMonth.invalidate()
        })

        lcMonth.apply {
            setBackgroundColor(Color.WHITE)
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            xAxis.apply {
                enableGridDashedLine(10f, 10f, 0f)
                position = XAxis.XAxisPosition.BOTTOM
                axisMinimum = 0f
                axisMaximum = 31f
            }
            axisLeft.apply {
                enableGridDashedLine(10f, 10f, 0f)
                axisMaximum = 200f
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            legend.form = Legend.LegendForm.LINE
        }

        btOpenApp.setOnClickListener {
            if (isAppInstalled(this, APP_PACKAGE_NAME)) {
                startActivity(this.packageManager.getLaunchIntentForPackage(APP_PACKAGE_NAME))
            } else {
                ToastUtil.show("未找到掌上电力App")
            }
        }

        btExport.setOnClickListener { v ->
            RxPermissions(context)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .observeOn(Schedulers.io())
                .flatMap { granted ->
                    if (granted) {
                        Observable.create<Unit> {
                            it.onNext(model.exportDatabase())
                            it.onCompleted()
                        }
                    } else {
                        Observable.error<Unit>(Throwable(""))
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { showToast("正在保存到外部存储根目录") }
                .subscribe({ showToast("数据已保存到外部存储根目录，文件名：electricity_fee.json") }, { e ->
                    e.printStackTrace()
                    showToast("保存失败")
                })
        }

        btImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE
                ) { resultCode, data ->
                    if (resultCode == Activity.RESULT_OK) {
                        val uri = data?.data
                        if (uri == null) {
                            showToast("无法打开文件")
                            return@startActivityForResult
                        }
                        Observable.create<Boolean> {
                            var inputStream: InputStream? = null
                            try {
                                inputStream = contentResolver.openInputStream(uri)
                                val line = BufferedReader(InputStreamReader(inputStream)).readLine()
                                model.importDatabase(line)
                                it.onNext(true)
                                it.onCompleted()
                            } finally {
                                inputStream?.close()
                            }
                        }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe { showToast("正在导入电费数据") }
                            .subscribe({ showToast("数据导入成功") }, { e ->
                                e.printStackTrace()
                                showToast("文件导入失败")
                            })
                    }
                }
            }
        }

        val serviceName = "me.fengmlo.electricityassistant.ElectricityAccessibilityService"
        if (!isAccessibilitySettingsOn(serviceName, this)) {
            openAccessibility(serviceName, this)
        }
    }

    /**
     * 该辅助功能开关是否打开了
     * @param accessibilityServiceName：指定辅助服务名字
     * @param context：上下文
     * @return
     */
    private fun isAccessibilitySettingsOn(accessibilityServiceName: String, context: Context): Boolean {
        var accessibilityEnable = 0
        val serviceName = context.packageName + "/" + accessibilityServiceName
        try {
            accessibilityEnable =
                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        } catch (e: Exception) {
            Logger.e("get accessibility enable failed, the err:" + e.message)
        }

        if (accessibilityEnable == 1) {
            val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')
            val settingValue =
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    if (accessibilityService.equals(serviceName, ignoreCase = true)) {
                        Logger.v("We've found the correct setting - accessibility is switched on!")
                        return true
                    }
                }
            }
        } else {
            Logger.d("Accessibility service disable")
        }
        return false
    }

    /**
     * 跳转到系统设置页面开启辅助功能
     * @param accessibilityServiceName：指定辅助服务名字
     * @param context：上下文
     */
    private fun openAccessibility(accessibilityServiceName: String, context: Context) {
        if (!isAccessibilitySettingsOn(accessibilityServiceName, context)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }


    /**
     * 检测某个应用是否安装
     *
     * @param context
     * @param packageName
     * @return
     */
    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

}
