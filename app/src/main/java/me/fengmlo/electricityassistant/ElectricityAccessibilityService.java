package me.fengmlo.electricityassistant;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.orhanobut.logger.Logger;
import me.fengmlo.electricityassistant.database.dao.ElectricityFeeDao;
import me.fengmlo.electricityassistant.database.entity.ElectricityFee;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

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

        List<AccessibilityNodeInfo> nodeInfos = rootInActiveWindow.findAccessibilityNodeInfosByText("表内余额");
        for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
            Logger.i(nodeInfo.toString());
            AccessibilityNodeInfo parent = nodeInfo.getParent();
            if (parent != null) {
                String money = parent.getChild(0).getText().toString().replace("元", "");
                App.run(() -> {
                    Calendar calendar = Calendar.getInstance();
                    final ElectricityFeeDao dao = App.getDB().getElectricityFeeDao();
                    final int year = calendar.get(Calendar.YEAR);
                    final int month = calendar.get(Calendar.MONTH);
                    final int day = calendar.get(Calendar.DAY_OF_MONTH);

                    ElectricityFee feeOfToday = dao.getElectricityFeeByDaySync(year, month, day);
                    if (feeOfToday == null) {
                        ElectricityFee electricityFee = new ElectricityFee();
                        electricityFee.setYear(year);
                        electricityFee.setMonth(month);
                        electricityFee.setDay(day);
                        electricityFee.setBalance(Double.parseDouble(money));

                        ElectricityFee last = dao.getLastElectricityFeeSync();
                        if (last != null) {
                            electricityFee.setId(last.getId() + 1);
                            electricityFee.setCost(new BigDecimal(last.getBalance() - electricityFee.getBalance()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                        } else {
                            electricityFee.setId(1);
                            electricityFee.setCost(0);
                        }
                        Logger.i(electricityFee.toString());
                        dao.insert(electricityFee);
                    }
                });
            }
        }
    }

    @Override
    public void onInterrupt() {

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
}
