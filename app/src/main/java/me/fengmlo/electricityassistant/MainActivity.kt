package me.fengmlo.electricityassistant

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.widget.Button
import com.orhanobut.logger.Logger
import me.fengmlo.electricityassistant.util.ToastUtil

const val APP_PACKAGE_NAME = "com.sgcc.cs"//包名

class MainActivity : AppCompatActivity() {

    private val btOpenApp: Button by bindView(R.id.bt_open_app)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Logger.i("onCreate")

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
