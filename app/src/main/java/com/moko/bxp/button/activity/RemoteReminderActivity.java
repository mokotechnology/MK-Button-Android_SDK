package com.moko.bxp.button.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.bxp.button.R;
import com.moko.bxp.button.dialog.LoadingMessageDialog;
import com.moko.bxp.button.utils.ToastUtils;
import com.moko.support.MokoSupport;
import com.moko.support.OrderTaskAssembler;
import com.moko.support.entity.OrderCHAR;
import com.moko.support.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RemoteReminderActivity extends BaseActivity {


    @BindView(R.id.et_blinking_time)
    EditText etBlinkingTime;
    @BindView(R.id.et_blinking_interval)
    EditText etBlinkingInterval;
    @BindView(R.id.et_vibrating_time)
    EditText etVibratingTime;
    @BindView(R.id.et_vibrating_interval)
    EditText etVibratingInterval;
    @BindView(R.id.et_ringing_time)
    EditText etRingingTime;
    @BindView(R.id.et_ringing_interval)
    EditText etRingingInterval;
    public boolean isConfigError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_reminder_notify_type);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            MokoSupport.getInstance().enableBluetooth();
        } else {
            showSyncingProgressDialog();
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getRemoteLEDNotifyAlarmParams());
            orderTasks.add(OrderTaskAssembler.getRemoteBuzzerNotifyAlarmParams());
            orderTasks.add(OrderTaskAssembler.getRemoteVibrationNotifyAlarmParams());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }
    }


    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                    // 设备断开，通知页面更新
                    RemoteReminderActivity.this.finish();
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
            }
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                dismissSyncProgressDialog();
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {
                    case CHAR_PARAMS:
                        if (value.length > 4) {
                            int header = value[0] & 0xFF;// 0xEB
                            int flag = value[1] & 0xFF;// read or write
                            int cmd = value[2] & 0xFF;
                            if (header != 0xEB)
                                return;
                            ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            int length = value[3] & 0xFF;
                            if (flag == 0x01 && length == 0x01) {
                                // write
                                int result = value[4] & 0xFF;
                                switch (configKeyEnum) {
                                    case KEY_REMOTE_LED_NOTIFY_ALARM_PARAMS:
                                    case KEY_REMOTE_BUZZER_NOTIFY_ALARM_PARAMS:
                                    case KEY_REMOTE_VIBRATION_NOTIFY_ALARM_PARAMS:
                                        if (result == 0) {
                                            isConfigError = true;
                                        }
                                        if (isConfigError) {
                                            ToastUtils.showToast(RemoteReminderActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            ToastUtils.showToast(this, "Success");
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                // read
                                switch (configKeyEnum) {
                                    case KEY_REMOTE_LED_NOTIFY_ALARM_PARAMS:
                                        if (length == 4) {
                                            int time = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 6, 8));
                                            etBlinkingTime.setText(String.valueOf(time));
                                            etBlinkingInterval.setText(String.valueOf(interval / 100));
                                        }
                                        break;
                                    case KEY_REMOTE_VIBRATION_NOTIFY_ALARM_PARAMS:
                                        if (length == 4) {
                                            int time = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 6, 8));
                                            etVibratingTime.setText(String.valueOf(time));
                                            etVibratingInterval.setText(String.valueOf(interval / 100));
                                        }
                                        break;
                                    case KEY_REMOTE_BUZZER_NOTIFY_ALARM_PARAMS:
                                        if (length == 4) {
                                            int time = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 6, 8));
                                            etRingingTime.setText(String.valueOf(time));
                                            etRingingInterval.setText(String.valueOf(interval / 100));
                                        }
                                        break;
                                }
                            }
                        }
                        break;
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private LoadingMessageDialog mLoadingMessageDialog;

    public void showSyncingProgressDialog() {
        mLoadingMessageDialog = new LoadingMessageDialog();
        mLoadingMessageDialog.setMessage("Syncing..");
        mLoadingMessageDialog.show(getSupportFragmentManager());
    }

    public void dismissSyncProgressDialog() {
        if (mLoadingMessageDialog != null)
            mLoadingMessageDialog.dismissAllowingStateLoss();
    }

    public void onLedNotifyRemind(View view) {
        if (isWindowLocked())
            return;

        if (isLEDValid()) {
            showSyncingProgressDialog();
            String ledTimeStr = etBlinkingTime.getText().toString();
            String ledIntervalStr = etBlinkingInterval.getText().toString();
            int ledTime = Integer.parseInt(ledTimeStr);
            int ledInterval = Integer.parseInt(ledIntervalStr) * 100;
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setRemoteLEDNotifyAlarmParams(ledTime, ledInterval));
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } else {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
        }
    }

    public void onVibrationNotifyRemind(View view) {
        if (isWindowLocked())
            return;
        if (isVibrationValid()) {
            showSyncingProgressDialog();
            String vibrationTimeStr = etVibratingTime.getText().toString();
            String vibrationIntervalStr = etVibratingInterval.getText().toString();
            int vibrationTime = Integer.parseInt(vibrationTimeStr);
            int vibrationInterval = Integer.parseInt(vibrationIntervalStr) * 100;
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setRemoteVibrationNotifyAlarmParams(vibrationTime, vibrationInterval));
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } else {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
        }
    }

    public void onBuzzerNotifyRemind(View view) {
        if (isWindowLocked())
            return;
        if (isBuzzerValid()) {
            showSyncingProgressDialog();
            String buzzerTimeStr = etRingingTime.getText().toString();
            String buzzerIntervalStr = etRingingInterval.getText().toString();
            int buzzerTime = Integer.parseInt(buzzerTimeStr);
            int buzzerInterval = Integer.parseInt(buzzerIntervalStr) * 100;
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setRemoteBuzzerNotifyAlarmParams(buzzerTime, buzzerInterval));
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } else {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
        }
    }

    public void onBack(View view) {
        finish();
    }

    private boolean isBuzzerValid() {
        String buzzerTimeStr = etRingingTime.getText().toString();
        String buzzerIntervalStr = etRingingInterval.getText().toString();
        if (TextUtils.isEmpty(buzzerTimeStr) || TextUtils.isEmpty(buzzerIntervalStr)) {
            return false;
        }
        int buzzerTime = Integer.parseInt(buzzerTimeStr);
        if (buzzerTime < 1 || buzzerTime > 6000)
            return false;
        int buzzerInterval = Integer.parseInt(buzzerIntervalStr);
        if (buzzerInterval < 1 || buzzerInterval > 100)
            return false;
        return true;
    }

    private boolean isLEDValid() {
        String ledTimeStr = etBlinkingTime.getText().toString();
        String ledIntervalStr = etBlinkingInterval.getText().toString();
        if (TextUtils.isEmpty(ledTimeStr) || TextUtils.isEmpty(ledIntervalStr)) {
            return false;
        }
        int ledTime = Integer.parseInt(ledTimeStr);
        if (ledTime < 1 || ledTime > 6000)
            return false;
        int ledInterval = Integer.parseInt(ledIntervalStr);
        if (ledInterval < 1 || ledInterval > 100)
            return false;
        return true;
    }

    private boolean isVibrationValid() {
        String vibrationTimeStr = etVibratingTime.getText().toString();
        String vibrationIntervalStr = etVibratingInterval.getText().toString();
        if (TextUtils.isEmpty(vibrationTimeStr) || TextUtils.isEmpty(vibrationIntervalStr)) {
            return false;
        }
        int vibrationTime = Integer.parseInt(vibrationTimeStr);
        if (vibrationTime < 1 || vibrationTime > 6000)
            return false;
        int vibrationInterval = Integer.parseInt(vibrationIntervalStr);
        if (vibrationInterval < 1 || vibrationInterval > 100)
            return false;
        return true;
    }
}
