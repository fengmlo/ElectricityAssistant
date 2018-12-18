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
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.orhanobut.logger.Logger
import com.tbruyelle.rxpermissions.RxPermissions
import me.fengmlo.electricityassistant.R
import me.fengmlo.electricityassistant.base.BaseActivity
import me.fengmlo.electricityassistant.base.RxSubscriptionHelper
import me.fengmlo.electricityassistant.base.RxUtil
import me.fengmlo.electricityassistant.bindView
import me.fengmlo.electricityassistant.database.entity.ElectricityFee
import me.fengmlo.electricityassistant.event.BalanceEvent
import me.fengmlo.electricityassistant.event.RxBus
import me.fengmlo.electricityassistant.extension.showToast
import me.fengmlo.electricityassistant.util.ToastUtil
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.InputStream
import java.util.*

const val APP_PACKAGE_NAME = "com.sgcc.cs" // 包名
private const val FILE_SELECT_CODE = 1

class MainActivity : BaseActivity() {

    private val lcMonth: LineChart by bindView(R.id.lc_month)
    private val btOpenApp: Button by bindView(R.id.bt_open_app)
    private val btExport: Button by bindView(R.id.bt_export)
    private val btImport: Button by bindView(R.id.bt_import)
    private val tvCost: TextView by bindView(R.id.tv_cost)
    private val tvDate: TextView by bindView(R.id.tv_date)

    private lateinit var model: MainViewModel
    private val rxSubscriptionHelper = RxSubscriptionHelper()
    private var monthCost: Float = 0f

    override fun getLayoutId(): Int = R.layout.activity_main

    override fun initView() {

        lcMonth.apply {
            setBackgroundColor(Color.WHITE)
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            isDragEnabled = true
            setScaleEnabled(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                axisMinimum = 1f
                axisMaximum = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH).toFloat()
                setDrawGridLines(false)
                textColor = 0xFFB2B2B2.toInt()
                textSize = 8f
            }
            axisLeft.isEnabled = false
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            legend.isEnabled = false
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onNothingSelected() {
                    tvCost.text = monthCost.toString()
                    tvDate.visibility = View.INVISIBLE
                }

                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.data?.let {
                        tvDate.visibility = View.VISIBLE
                        val data = it as ElectricityFee
                        tvDate.text = "${data.month}月${data.day}日"
                        tvCost.text = data.cost.toString()
                    } ?: kotlin.run {
                        tvDate.visibility = View.INVISIBLE
                    }
                }
            })
            invalidate()
        }

        btOpenApp.setOnClickListener {
            if (isAppInstalled(this, APP_PACKAGE_NAME)) {
                startActivity(this.packageManager.getLaunchIntentForPackage(APP_PACKAGE_NAME))
            } else {
                ToastUtil.show("未找到掌上电力App")
            }
        }

        btExport.setOnClickListener { v ->
            val subscribe = RxPermissions(context)
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
                    .compose(RxUtil.rxSchedulerHelper())
                    .doOnSubscribe { showToast("正在保存到外部存储根目录") }
                    .subscribe({ showToast("数据已保存到外部存储根目录，文件名：electricity_fee.json") }, { e ->
                        e.printStackTrace()
                        showToast("保存失败")
                    })
            rxSubscriptionHelper.addSubscribe(subscribe)
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
                        val subscribe = Observable.create<Unit> {
                            var inputStream: InputStream? = null
                            try {
                                inputStream = contentResolver.openInputStream(uri)
                                it.onNext(model.importDatabase(inputStream))
                                it.onCompleted()
                            } finally {
                                inputStream?.close()
                            }
                        }
                                .compose(RxUtil.rxSchedulerHelper())
                                .doOnSubscribe { showToast("正在导入电费数据") }
                                .subscribe({ showToast("数据导入成功") }, { e ->
                                    e.printStackTrace()
                                    showToast("文件导入失败")
                                })
                        rxSubscriptionHelper.addSubscribe(subscribe)
                    }
                }
            }
        }

        val serviceName = "me.fengmlo.electricityassistant.ElectricityAccessibilityService"
        if (!isAccessibilitySettingsOn(serviceName, this)) {
            openAccessibility(serviceName, this)
        }

        model = ViewModelProviders.of(this).get(MainViewModel::class.java)
        model.getElectricityFees().observe(this, Observer { list ->
            if (list.isNullOrEmpty()) return@Observer
            monthCost = list.sumByDouble { it.cost }.toFloat()
            tvCost.text = monthCost.toString()
            val dataSet = LineDataSet(list.map { Entry(it.day.toFloat(), it.cost.toFloat(), it) }, "月度用电情况").apply {
                mode = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity = 0.2f
//                setDrawCircles(false)
                circleRadius = 3f
                setCircleColor(Color.WHITE)
                setDrawCircleHole(true)
                circleHoleRadius = 2f
                color = 0xFF23A6DE.toInt()
                setCircleColorHole(color)
                setDrawFilled(true)
                fillDrawable = getDrawable(R.drawable.fade_blue)
                setDrawHorizontalHighlightIndicator(false)
                lineWidth = 2f
                highlightLineWidth = 1f
                highLightColor = color
                setValueTextColors(arrayListOf(0xFF878D92.toInt()))
            }
            lcMonth.axisLeft.mAxisMaximum = list.maxBy { it.cost }!!.cost.toFloat() + 2f
            lcMonth.axisLeft.mAxisMinimum =
                    (list.minBy { it.cost }!!.cost.toFloat() - 2f).let { if (it < 0f) 0f else it }
            lcMonth.data = LineData(dataSet)
        })

        val subscribe = RxBus.getInstance().toObservable(BalanceEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { showToast("电费记录成功，您现在可以关闭掌上电力了") }
        rxSubscriptionHelper.addSubscribe(subscribe)
    }

    override fun onDestroy() {
        rxSubscriptionHelper.unSubscribe()
        super.onDestroy()
    }

    /**
     * 该辅助功能开关是否打开了
     *
     * @param accessibilityServiceName：指定辅助服务名字
     * @param context：上下文
     * @return
     */
    private fun isAccessibilitySettingsOn(accessibilityServiceName: String, context: Context): Boolean {
        var accessibilityEnable = 0
        val serviceName = context.packageName + "/" + accessibilityServiceName
        try {
            accessibilityEnable = Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        } catch (e: Exception) {
            Logger.e("get accessibility enable failed, the err:" + e.message)
        }

        if (accessibilityEnable == 1) {
            val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')
            val settingValue = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
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
