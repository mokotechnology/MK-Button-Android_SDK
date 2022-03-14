package com.moko.bxp.button.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.bxp.button.AppConstants;
import com.moko.bxp.button.R;
import com.moko.bxp.button.dialog.AlertMessageDialog;
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

import androidx.constraintlayout.widget.ConstraintLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import cn.carbswang.android.numberpickerview.library.NumberPickerView;

public class DismissAlarmNotifyTypeActivity extends BaseActivity {

    @BindView(R.id.npv_notify_type)
    NumberPickerView npvNotifyType;
    @BindView(R.id.et_blinking_time)
    EditText etBlinkingTime;
    @BindView(R.id.et_blinking_interval)
    EditText etBlinkingInterval;
    @BindView(R.id.cl_led_notify)
    ConstraintLayout clLedNotify;
    @BindView(R.id.et_vibrating_time)
    EditText etVibratingTime;
    @BindView(R.id.et_vibrating_interval)
    EditText etVibratingInterval;
    @BindView(R.id.cl_vibration_notify)
    ConstraintLayout clVibrationNotify;
    @BindView(R.id.et_ringing_time)
    EditText etRingingTime;
    @BindView(R.id.et_ringing_interval)
    EditText etRingingInterval;
    @BindView(R.id.cl_buzzer_notify)
    ConstraintLayout clBuzzerNotify;
    private String[] dismissAlarmNotifyTypeArray;
    public boolean isConfigError;
    public int notifyType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dismiss_alarm_notify_type);
        ButterKnife.bind(this);

        dismissAlarmNotifyTypeArray = getResources().getStringArray(R.array.alarm_notify_type);
        npvNotifyType.setDisplayedValues(dismissAlarmNotifyTypeArray);
        npvNotifyType.setMinValue(0);
        npvNotifyType.setMaxValue(dismissAlarmNotifyTypeArray.length - 1);
        npvNotifyType.setValue(notifyType);
        npvNotifyType.setOnValueChangedListener((picker, oldVal, newVal) -> {
            notifyType = newVal;
            if (notifyType == 1 || notifyType == 4 || notifyType == 5) {
                // LED/LED+Vibration/LED+Buzzer
                clLedNotify.setVisibility(View.VISIBLE);
            } else {
                clLedNotify.setVisibility(View.GONE);
            }
            if (notifyType == 2 || notifyType == 4) {
                // Vibration/LED+Vibration
                clVibrationNotify.setVisibility(View.VISIBLE);
            } else {
                clVibrationNotify.setVisibility(View.GONE);
            }
            if (notifyType == 3 || notifyType == 5) {
                // Buzzer/LED+Buzzer
                clBuzzerNotify.setVisibility(View.VISIBLE);
            } else {
                clBuzzerNotify.setVisibility(View.GONE);
            }
        });

        EventBus.getDefault().register(this);
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            MokoSupport.getInstance().enableBluetooth();
        } else {
            showSyncingProgressDialog();
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getDismissAlarmType());
            orderTasks.add(OrderTaskAssembler.getDismissLEDNotifyAlarmParams());
            orderTasks.add(OrderTaskAssembler.getDismissBuzzerNotifyAlarmParams());
            orderTasks.add(OrderTaskAssembler.getDismissVibrationNotifyAlarmParams());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }
    }


    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                    // 设备断开，通知页面更新
                    DismissAlarmNotifyTypeActivity.this.finish();
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
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
                        if (value.length == 4) {
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
                                    case KEY_DISMISS_LED_NOTIFY_ALARM_PARAMS:
                                    case KEY_DISMISS_BUZZER_NOTIFY_ALARM_PARAMS:
                                    case KEY_DISMISS_VIBRATION_NOTIFY_ALARM_PARAMS:
                                        if (result != 0) {
                                            isConfigError = true;
                                        }
                                        break;
                                    case KEY_DISMISS_ALARM_TYPE:
                                    case KEY_DISMISS_ALARM:
                                        if (result != 0) {
                                            isConfigError = true;
                                        }
                                        if (isConfigError) {
                                            ToastUtils.showToast(DismissAlarmNotifyTypeActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            AlertMessageDialog dialog = new AlertMessageDialog();
                                            dialog.setMessage("Saved Successfully！");
                                            dialog.setConfirm("OK");
                                            dialog.setCancelGone();
                                            dialog.show(getSupportFragmentManager());
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                // read
                                switch (configKeyEnum) {
                                    case KEY_DISMISS_ALARM_TYPE:
                                        if (length == 1) {
                                            notifyType = value[4] & 0xFF;
                                            if (notifyType == 1 || notifyType == 4 || notifyType == 5) {
                                                // LED/LED+Vibration/LED+Buzzer
                                                clLedNotify.setVisibility(View.VISIBLE);
                                            } else {
                                                clLedNotify.setVisibility(View.GONE);
                                            }
                                            if (notifyType == 2 || notifyType == 4) {
                                                // Vibration/LED+Vibration
                                                clVibrationNotify.setVisibility(View.VISIBLE);
                                            } else {
                                                clVibrationNotify.setVisibility(View.GONE);
                                            }
                                            if (notifyType == 3 || notifyType == 5) {
                                                // Buzzer/LED+Buzzer
                                                clBuzzerNotify.setVisibility(View.VISIBLE);
                                            } else {
                                                clBuzzerNotify.setVisibility(View.GONE);
                                            }
                                        }
                                        break;
                                    case KEY_DISMISS_LED_NOTIFY_ALARM_PARAMS:
                                        if (length == 4) {
                                            int time = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 6, 8));
                                            etBlinkingTime.setText(String.valueOf(time));
                                            etBlinkingInterval.setText(String.valueOf(interval));
                                        }
                                        break;
                                    case KEY_DISMISS_VIBRATION_NOTIFY_ALARM_PARAMS:
                                        if (length == 4) {
                                            int time = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 6, 8));
                                            etVibratingTime.setText(String.valueOf(time));
                                            etVibratingInterval.setText(String.valueOf(interval));
                                        }
                                        break;
                                    case KEY_DISMISS_BUZZER_NOTIFY_ALARM_PARAMS:
                                        if (length == 4) {
                                            int time = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 7));
                                            int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 6, 8));
                                            etRingingTime.setText(String.valueOf(time));
                                            etRingingInterval.setText(String.valueOf(interval));
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


    public void onDismissAlarm(View view) {
        if (isWindowLocked())
            return;
        showSyncingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setDismissAlarm());
    }

    public void onBack(View view) {
        finish();
    }

    public void onSave(View view) {
        if (isWindowLocked())
            return;

        if (isValid()) {
            showSyncingProgressDialog();
            saveParams();
        } else {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
        }
    }

    private void saveParams() {
        String ledTimeStr = etBlinkingTime.getText().toString();
        String ledIntervalStr = etBlinkingInterval.getText().toString();
        String vibrationTimeStr = etVibratingTime.getText().toString();
        String vibrationIntervalStr = etVibratingInterval.getText().toString();
        String buzzerTimeStr = etRingingTime.getText().toString();
        String buzzerIntervalStr = etRingingInterval.getText().toString();

        int ledTime = Integer.parseInt(ledTimeStr);
        int ledInterval = Integer.parseInt(ledIntervalStr);
        int vibrationTime = Integer.parseInt(vibrationTimeStr);
        int vibrationInterval = Integer.parseInt(vibrationIntervalStr);
        int buzzerTime = Integer.parseInt(buzzerTimeStr);
        int buzzerInterval = Integer.parseInt(buzzerIntervalStr);

        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        if (notifyType == 1 || notifyType == 4 || notifyType == 5) {
            // LED/LED+Vibration/LED+Buzzer
            orderTasks.add(OrderTaskAssembler.setDismissLEDNotifyAlarmParams(ledTime, ledInterval));
        }
        if (notifyType == 2 || notifyType == 4) {
            // Vibration/LED+Vibration
            orderTasks.add(OrderTaskAssembler.setDismissVibrationNotifyAlarmParams(vibrationTime, vibrationInterval));
        }
        if (notifyType == 3 || notifyType == 5) {
            // Buzzer/LED+Buzzer
            orderTasks.add(OrderTaskAssembler.setDismissBuzzerNotifyAlarmParams(buzzerTime, buzzerInterval));
        }
        orderTasks.add(OrderTaskAssembler.setDismissAlarmType(notifyType));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    private boolean isValid() {
        if (notifyType == 0)
            return true;
        String ledTimeStr = etBlinkingTime.getText().toString();
        String ledIntervalStr = etBlinkingInterval.getText().toString();
        String vibrationTimeStr = etVibratingTime.getText().toString();
        String vibrationIntervalStr = etVibratingInterval.getText().toString();
        String buzzerTimeStr = etRingingTime.getText().toString();
        String buzzerIntervalStr = etRingingInterval.getText().toString();
        if (notifyType == 1 || notifyType == 4 || notifyType == 5) {
            if (TextUtils.isEmpty(ledTimeStr) || TextUtils.isEmpty(ledIntervalStr)) {
                return false;
            }
            int ledTime = Integer.parseInt(ledTimeStr);
            if (ledTime < 1 || ledTime > 6000)
                return false;
            int ledInterval = Integer.parseInt(ledIntervalStr);
            if (ledInterval < 1 || ledInterval > 100)
                return false;
        }
        if (notifyType == 2 || notifyType == 4) {
            if (TextUtils.isEmpty(vibrationTimeStr) || TextUtils.isEmpty(vibrationIntervalStr)) {
                return false;
            }
            int vibrationTime = Integer.parseInt(vibrationTimeStr);
            if (vibrationTime < 1 || vibrationTime > 6000)
                return false;
            int vibrationInterval = Integer.parseInt(vibrationIntervalStr);
            if (vibrationInterval < 1 || vibrationInterval > 100)
                return false;
        }
        if (notifyType == 3 || notifyType == 5) {
            if (TextUtils.isEmpty(buzzerTimeStr) || TextUtils.isEmpty(buzzerIntervalStr)) {
                return false;
            }
            int buzzerTime = Integer.parseInt(buzzerTimeStr);
            if (buzzerTime < 1 || buzzerTime > 6000)
                return false;
            int buzzerInterval = Integer.parseInt(buzzerIntervalStr);
            if (buzzerInterval < 1 || buzzerInterval > 100)
                return false;
        }
        return true;
    }
}
