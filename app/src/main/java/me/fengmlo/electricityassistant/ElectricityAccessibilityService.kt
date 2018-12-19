package me.fengmlo.electricityassistant

import android.accessibilityservice.AccessibilityService
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.orhanobut.logger.Logger
import me.fengmlo.electricityassistant.database.entity.ElectricityFee
import me.fengmlo.electricityassistant.database.entity.Recharge
import me.fengmlo.electricityassistant.event.BalanceEvent
import me.fengmlo.electricityassistant.event.RxBus
import me.fengmlo.electricityassistant.event.UnrecordRechargeEvent
import me.fengmlo.electricityassistant.extension.*
import java.util.*

class ElectricityAccessibilityService : AccessibilityService() {

    override fun onCreate() {
        super.onCreate()
        Logger.i("onCreate")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Logger.i("onServiceConnected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Logger.i("onAccessibilityEvent: " + event.toString())
        val rootInActiveWindow = rootInActiveWindow ?: return
        val loginInfos = rootInActiveWindow.findAccessibilityNodeInfosByText("登录")
        val forgotInfos = rootInActiveWindow.findAccessibilityNodeInfosByText("忘记密码")

        if (loginInfos.size > 0 && forgotInfos.size > 0) { // 登录页
            //            new NodeInfoDisplay(getRootInActiveWindow()).displayNodeInfo(2);
            val passwordInfos = findPassword(rootInActiveWindow)
            if (passwordInfos != null) {
                //                passwordInfos.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                //                try {
                //                    Field mSealed = passwordInfos.getClass().getDeclaredField("mSealed");
                //                    mSealed.setAccessible(true);
                //                    mSealed.setBoolean(passwordInfos, false);
                //                    passwordInfos.setText("wuai1013");
                //
                //                } catch (Exception e) {
                //                    e.printStackTrace();
                //                }
            }
        } else if (loginInfos.size > 0) { // 主页
            for (nodeInfo in loginInfos) {
                Logger.i(nodeInfo.toString())
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        //        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && "android.widget.TextView".contentEquals(event.getClassName())) {
        val nodeInfos = rootInActiveWindow.findAccessibilityNodeInfosByText("表内余额")
        for (nodeInfo in nodeInfos) {
            Logger.i(nodeInfo.toString())
            val parent = nodeInfo.parent
            if (parent != null) {
                val money = parent.getChild(0).text.toString().replace("元", "")
                onCatchBalance(money)
            }
        }
        //        }
    }

    override fun onInterrupt() {

    }

    private fun onCatchBalance(balance: String) {
        if (TextUtils.isEmpty(balance)) return
        App.run {
            val parseBalance: Double = balance.toDoubleOrNull() ?: return@run

            val today = Calendar.getInstance()
            if (today.get(Calendar.HOUR_OF_DAY) < 7) return@run  // 7点之前数据可能没有刷新，不统计
            val year = today.get(Calendar.YEAR)
            val month = today.get(Calendar.MONTH)
            val day = today.get(Calendar.DAY_OF_MONTH)

            val dao = App.getDB().electricityFeeDao
            val feeOfToday = dao.getElectricityFeeByDaySync(year, month, day)
            if (feeOfToday == null) { // 今天还没有统计过余额
                val electricityFee = ElectricityFee()
                electricityFee.year = year
                electricityFee.month = month
                electricityFee.day = day
                electricityFee.balance = parseBalance
                electricityFee.cost = 0.0

                val lastRecord = dao.lastElectricityFeeSync
                if (lastRecord != null) { // 之前有统计过数据
                    var chargeMoney = 0.0
                    if (lastRecord.balance < parseBalance) { // 这段时间有充值
                        val rechargeDao = App.getDB().rechargeDao
                        val lastRecharge = rechargeDao.lastRechargeSync
                        if (lastRecharge == null || !lastRecharge.inDate(lastRecord.getStartDate(), electricityFee.getEndDate())) {
                            RxBus.getInstance().post(UnrecordRechargeEvent())
                            return@run
                        } else {
                            chargeMoney = lastRecharge.charge
                        }
                    }
                    val yesterday = today.yesterday()
                    val yesterdayFee = dao.getElectricityFeeByDaySync(yesterday.year, yesterday.month, yesterday.day)
                    if (yesterdayFee == null) { // 昨天没有统计过数据，需要补数据
                        val lastRecordDay = Calendar.getInstance().also {
                            it.year = lastRecord.year
                            it.month = lastRecord.month
                            it.day = lastRecord.day
                        }
                        val days = today - lastRecordDay
                        val averageCost = ((lastRecord.balance + chargeMoney - electricityFee.balance) / days).roundHalfUp()
                        val temp = today.clone() as Calendar
                        for (i in 1 until days) { // 补数据
                            temp.add(Calendar.DAY_OF_YEAR, -1)
                            dao.insert(ElectricityFee().also {
                                it.year = temp.year
                                it.month = temp.month
                                it.day = temp.day
                                it.balance = (parseBalance + averageCost * i).roundHalfUp()
                                it.cost = averageCost
                                it.id = lastRecord.id + days - i
                            })
                        }
                        electricityFee.id = lastRecord.id + days
                        lastRecord.cost = averageCost
                    } else { // 昨天统计过数据
                        electricityFee.id = lastRecord.id + 1
                        lastRecord.cost = (lastRecord.balance + chargeMoney - electricityFee.balance).roundHalfUp()
                    }
                    dao.update(lastRecord)
                } else { // 第一次统计到数据
                    electricityFee.id = 1
                }
                Logger.i(electricityFee.toString())
                dao.insert(electricityFee)
                RxBus.getInstance().post(BalanceEvent())
            } else if (balance != java.lang.Double.toString(feeOfToday.balance)) { // 今天统计过余额，但是余额变动，可能是电力刷新时间有误

            }
            feeOfToday!!.balance = Util.roundDouble(parseBalance)
            dao.update(feeOfToday)
        }
    }

    private fun findPassword(nodeInfo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var result: AccessibilityNodeInfo?
        if (nodeInfo.className == "android.widget.EditText") {
            if (nodeInfo.hintText == "请输入密码") return nodeInfo
        }
        val childCount = nodeInfo.childCount
        for (i in 0 until childCount) {
            result = findPassword(nodeInfo.getChild(i))
            if (result != null) return result
        }
        return null
    }

    private class NodeInfoDisplay(private val nodeInfo: AccessibilityNodeInfo) {

        private val stringBuilder = StringBuilder()

        fun displayNodeInfo(paddingSize: Int) {
            displayNodeInfoInternal(nodeInfo, paddingSize, 0)
            Logger.i(stringBuilder.toString())
        }

        private fun displayNodeInfoInternal(nodeInfo: AccessibilityNodeInfo, paddingSize: Int, multiple: Int) {
            var multiple = multiple
            for (j in 0 until paddingSize * multiple) {
                stringBuilder.append(' ')
            }
            stringBuilder.append(nodeInfo.toString())
            stringBuilder.append("\n")
            val childCount = nodeInfo.childCount
            for (i in 0 until childCount) {
                val child = nodeInfo.getChild(i)
                displayNodeInfoInternal(child, paddingSize, ++multiple)
            }
        }
    }

    private fun yesterday(calendar: Calendar?): Calendar {
        val result = Calendar.getInstance()
        if (calendar != null) result.time = calendar.time
        result.add(Calendar.DAY_OF_MONTH, -1)
        return result
    }

    private fun Recharge?.inDate(startDate: Calendar, endDate: Calendar): Boolean {
        if (this == null) return false
        val rechargeDate = Calendar.getInstance().also {
            it.year = year
            it.month = month
            it.day = day
        }
        return !rechargeDate.before(startDate) && !rechargeDate.after(endDate)
    }

    private fun ElectricityFee.getStartDate(): Calendar {
        return Calendar.getInstance().also {
            it.year = year
            it.month = month
            it.day = day
            it.hour = 0
            it.minute = 0
            it.second = 0
        }
    }

    private fun ElectricityFee.getEndDate(): Calendar {
        return Calendar.getInstance().also {
            it.year = year
            it.month = month
            it.day = day
            it.hour = 23
            it.minute = 59
            it.second = 59
        }
    }
}
