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
import android.support.design.widget.TextInputEditText
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.flyco.tablayout.SegmentTabLayout
import com.flyco.tablayout.listener.OnTabSelectListener
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
import me.fengmlo.electricityassistant.event.UnrecordRechargeEvent
import me.fengmlo.electricityassistant.extension.showToast
import me.fengmlo.electricityassistant.util.ToastUtil
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.InputStream

const val APP_PACKAGE_NAME = "com.sgcc.cs" // 包名
private const val FILE_SELECT_CODE = 1

class MainActivity : BaseActivity() {

    private val lcMonth: LineChart by bindView(R.id.lc_month)
    private val btOpenApp: Button by bindView(R.id.bt_open_app)
    private val tvCost: TextView by bindView(R.id.tv_cost)
    private val tvDate: TextView by bindView(R.id.tv_date)
    private val tlSegment: SegmentTabLayout by bindView(R.id.tl_segment)

    private lateinit var model: MainViewModel
    private val rxSubscriptionHelper = RxSubscriptionHelper()
    private var monthCost: Float = 0f
    private var monthCostDataSet: LineDataSet? = null
    private var monthBalanceDataSet: LineDataSet? = null
    private var monthRechageDataSet: LineDataSet? = null

    override fun getLayoutId(): Int = R.layout.activity_main

    override fun initView() {
        tlSegment.setTabData(arrayOf("本月电费", "电费余额"))
        tlSegment.setOnTabSelectListener(object : OnTabSelectListener {
            override fun onTabSelect(position: Int) = reloadData(position)
            override fun onTabReselect(position: Int) {}
        })

        lcMonth.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            isDragEnabled = true
            setScaleEnabled(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
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
                        when (tlSegment.currentTab) {
                            0 -> tvCost.text = data.cost.toString()
                            1 -> tvCost.text = data.balance.toString()
                        }
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

        val serviceName = "me.fengmlo.electricityassistant.ElectricityAccessibilityService"
        if (!isAccessibilitySettingsOn(serviceName, this)) {
            openAccessibility(serviceName, this)
        }

        model = ViewModelProviders.of(this).get(MainViewModel::class.java)
        model.getElectricityFees().observe(this, Observer { list ->
            if (list.isNullOrEmpty()) return@Observer
            monthCost = list.sumByDouble { it.cost }.toFloat()
            tvCost.text = monthCost.toString()
            monthCostDataSet =
                    LineDataSet(list.filter { it.cost > 0 }.map { Entry(it.day.toFloat(), it.cost.toFloat(), it) },
                            null
                    ).apply {
                        initDataSetStyle()
                    }
            monthBalanceDataSet =
                    LineDataSet(list.map { Entry(it.day.toFloat(), it.balance.toFloat(), it) }, null).apply {
                        initDataSetStyle()
                    }
            reloadData(tlSegment.currentTab)
        })

        val subscribe = RxBus.getInstance().toObservable(BalanceEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { showToast("电费记录成功，您现在可以关闭掌上电力了") }
        rxSubscriptionHelper.addSubscribe(subscribe)

        val subscribe1 = RxBus.getInstance().toObservable(UnrecordRechargeEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { showToast("您忘了记录充值了") }
        rxSubscriptionHelper.addSubscribe(subscribe1)
    }

    override fun onDestroy() {
        rxSubscriptionHelper.unSubscribe()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_import -> {
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
                true
            }
            R.id.item_export -> {
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
                true
            }
            R.id.item_record_charge -> {
                val view = layoutInflater.inflate(R.layout.dialog_record_charge, LinearLayout(context), true)
                val etCharge = view.findViewById<TextInputEditText>(R.id.et_charge)
                AlertDialog.Builder(context)
                        .setTitle("记录充值")
                        .setView(view)
                        .setPositiveButton("确定") { _, _ ->
                            val string = etCharge.text.toString()
                            if (string.isBlank()) {
                                showToast("请输入充值金额")
                                return@setPositiveButton
                            }
                            val money = string.toDouble()
                            val subscribe = Observable.create<Unit> {
                                it.onNext(model.recordCharge(money))
                                it.onCompleted()
                            }
                                    .compose(RxUtil.rxSchedulerHelper())
                                    .subscribe({ showToast("记录成功") }, { e -> e.printStackTrace() })
                            rxSubscriptionHelper.addSubscribe(subscribe)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun reloadData(tab: Int) {
        when (tab) {
            0 -> lcMonth.data = LineData(monthCostDataSet)
            1 -> lcMonth.data = LineData(monthBalanceDataSet)
        }
        lcMonth.invalidate()
        lcMonth.animateX(200)
    }

    private fun LineDataSet.initDataSetStyle() {
        mode = LineDataSet.Mode.CUBIC_BEZIER
        cubicIntensity = 0.05f
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
