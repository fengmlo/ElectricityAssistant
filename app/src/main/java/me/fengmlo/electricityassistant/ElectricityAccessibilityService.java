package me.fengmlo.electricityassistant;

import android.accessibilityservice.AccessibilityService;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.orhanobut.logger.Logger;

import java.util.Calendar;
import java.util.List;

import me.fengmlo.electricityassistant.database.dao.ElectricityFeeDao;
import me.fengmlo.electricityassistant.database.dao.RechargeDao;
import me.fengmlo.electricityassistant.database.entity.ElectricityFee;
import me.fengmlo.electricityassistant.database.entity.Recharge;
import me.fengmlo.electricityassistant.event.BalanceEvent;
import me.fengmlo.electricityassistant.event.RxBus;

public class ElectricityAccessibilityService extends AccessibilityService {

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.i("onCreate");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Logger.i("onServiceConnected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Logger.i("onAccessibilityEvent: " + event.toString());
        AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
        if (rootInActiveWindow == null) return;
        List<AccessibilityNodeInfo> loginInfos = rootInActiveWindow.findAccessibilityNodeInfosByText("登录");
        List<AccessibilityNodeInfo> forgotInfos = rootInActiveWindow.findAccessibilityNodeInfosByText("忘记密码");

        if (loginInfos.size() > 0 && forgotInfos.size() > 0) { // 登录页
//            new NodeInfoDisplay(getRootInActiveWindow()).displayNodeInfo(2);
            AccessibilityNodeInfo passwordInfos = findPassword(rootInActiveWindow);
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
        } else if (loginInfos.size() > 0) { // 主页
            for (AccessibilityNodeInfo nodeInfo : loginInfos) {
                Logger.i(nodeInfo.toString());
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

//        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && "android.widget.TextView".contentEquals(event.getClassName())) {
        List<AccessibilityNodeInfo> nodeInfos = rootInActiveWindow.findAccessibilityNodeInfosByText("表内余额");
        for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
            Logger.i(nodeInfo.toString());
            AccessibilityNodeInfo parent = nodeInfo.getParent();
            if (parent != null) {
                String money = parent.getChild(0).getText().toString().replace("元", "");
                onCatchBalance(money);
            }
        }
//        }
    }

    @Override
    public void onInterrupt() {

    }

    private void onCatchBalance(String balance) {
        if (TextUtils.isEmpty(balance)) return;
        App.run(() -> {
            Calendar calendar = Calendar.getInstance();
            final ElectricityFeeDao dao = App.getDB().getElectricityFeeDao();
            final int year = calendar.get(Calendar.YEAR);
            final int month = calendar.get(Calendar.MONTH);
            final int day = calendar.get(Calendar.DAY_OF_MONTH);

            ElectricityFee feeOfToday = dao.getElectricityFeeByDaySync(year, month, day);
            if (feeOfToday == null) { // 今天还没有统计过余额
                ElectricityFee electricityFee = new ElectricityFee();
                electricityFee.setYear(year);
                electricityFee.setMonth(month);
                electricityFee.setDay(day);
                electricityFee.setBalance(Double.parseDouble(balance));

                ElectricityFee last = dao.getLastElectricityFeeSync();
                if (last != null) {
                    electricityFee.setId(last.getId() + 1);
                    electricityFee.setCost(Util.roundDouble(last.getBalance() - electricityFee.getBalance()));
                } else {
                    electricityFee.setId(1);
                    electricityFee.setCost(0);
                }
                Logger.i(electricityFee.toString());
                dao.insert(electricityFee);
                RxBus.getInstance().post(new BalanceEvent());
            } else if (!balance.equals(Double.toString(feeOfToday.getBalance()))) { // 今天统计过余额，但是余额变动，可能是充值，或者中途某天忘记统计
                double difference = Double.parseDouble(balance) - feeOfToday.getBalance();
                if (difference > 0) { // 充值
                    RechargeDao rechargeDao = App.getDB().getRechargeDao();
                    Recharge lastRecharge = rechargeDao.getLastRechargeSync();
                    if (lastRecharge == null || !(lastRecharge.getYear() == year && lastRecharge.getMonth() == month && lastRecharge.getDay() == day)) {
                        Recharge newRecharge = new Recharge();
                        newRecharge.setYear(year);
                        newRecharge.setMonth(month);
                        newRecharge.setDay(day);
                        newRecharge.setCharge(Util.roundDouble(difference));
                        if (lastRecharge == null) {
                            newRecharge.setId(1);
                        } else {
                            newRecharge.setId(lastRecharge.getId() + 1);
                        }
                        rechargeDao.insert(newRecharge);
                    }
                } else { // 某一天忘记统计

                }
            }
            feeOfToday.setBalance(Util.roundDouble(Double.parseDouble(balance)));
            dao.update(feeOfToday);

        });
    }

    private AccessibilityNodeInfo findPassword(AccessibilityNodeInfo nodeInfo) {
        AccessibilityNodeInfo result;
        if (nodeInfo.getClassName().equals("android.widget.EditText")) {
            if (nodeInfo.getHintText().equals("请输入密码")) return nodeInfo;
        }
        int childCount = nodeInfo.getChildCount();
        for (int i = 0; i < childCount; i++) {
            result = findPassword(nodeInfo.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    private static class NodeInfoDisplay {

        private StringBuilder stringBuilder = new StringBuilder();
        private AccessibilityNodeInfo nodeInfo;

        public NodeInfoDisplay(AccessibilityNodeInfo nodeInfo) {
            this.nodeInfo = nodeInfo;
        }

        public void displayNodeInfo(int paddingSize) {
            displayNodeInfoInternal(nodeInfo, paddingSize, 0);
            Logger.i(stringBuilder.toString());
        }

        private void displayNodeInfoInternal(AccessibilityNodeInfo nodeInfo, int paddingSize, int multiple) {
            for (int j = 0; j < paddingSize * multiple; j++) {
                stringBuilder.append(' ');
            }
            stringBuilder.append(nodeInfo.toString());
            stringBuilder.append("\n");
            int childCount = nodeInfo.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = nodeInfo.getChild(i);
                displayNodeInfoInternal(child, paddingSize, ++multiple);
            }
        }
    }

    private Calendar yesterday(@Nullable Calendar calendar) {
        Calendar result = Calendar.getInstance();
        if (calendar != null) result.setTime(calendar.getTime());
        result.add(Calendar.DAY_OF_MONTH, -1);
        return result;
    }
}
