package com.moko.bxp.button.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.bxp.button.AppConstants;
import com.moko.bxp.button.R;
import com.moko.bxp.button.databinding.ActivityAlarmNotifyTypeBinding;
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

public class AlarmNotifyTypeActivity extends BaseActivity {


    private ActivityAlarmNotifyTypeBinding mBind;
    public boolean isConfigError;
    public int slotType;
    private String[] alarmNotifyTypeArray;
    public int notifyType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = ActivityAlarmNotifyTypeBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        if (getIntent() != null && getIntent().getExtras() != null) {
            slotType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_SLOT_TYPE, 0);
        }

        alarmNotifyTypeArray = getResources().getStringArray(R.array.alarm_notify_type);
        mBind.npvNotifyType.setDisplayedValues(alarmNotifyTypeArray);
        mBind.npvNotifyType.setMinValue(0);
        mBind.npvNotifyType.setMaxValue(alarmNotifyTypeArray.length - 1);
        mBind.npvNotifyType.setValue(notifyType);
        mBind.npvNotifyType.setOnValueChangedListener((picker, oldVal, newVal) -> {
            notifyType = newVal;
            if (notifyType == 1 || notifyType == 4 || notifyType == 5) {
                // LED/LED+Vibration/LED+Buzzer
                mBind.clLedNotify.setVisibility(View.VISIBLE);
            } else {
                mBind.clLedNotify.setVisibility(View.GONE);
            }
            if (notifyType == 2 || notifyType == 4) {
                // Vibration/LED+Vibration
                mBind.clVibrationNotify.setVisibility(View.VISIBLE);
            } else {
                mBind.clVibrationNotify.setVisibility(View.GONE);
            }
            if (notifyType == 3 || notifyType == 5) {
                // Buzzer/LED+Buzzer
                mBind.clBuzzerNotify.setVisibility(View.VISIBLE);
            } else {
                mBind.clBuzzerNotify.setVisibility(View.GONE);
            }
        });

        EventBus.getDefault().register(this);
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            MokoSupport.getInstance().enableBluetooth();
        } else {
            showSyncingProgressDialog();
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getSlotTriggerAlarmNotifyType(slotType));
            orderTasks.add(OrderTaskAssembler.getSlotLEDNotifyAlarmParams(slotType));
            orderTasks.add(OrderTaskAssembler.getSlotBuzzerNotifyAlarmParams(slotType));
            orderTasks.add(OrderTaskAssembler.getSlotVibrationNotifyAlarmParams(slotType));
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
                    AlarmNotifyTypeActivity.this.finish();
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
                                    case KEY_SLOT_LED_NOTIFY_ALARM_PARAMS:
                                    case KEY_SLOT_BUZZER_NOTIFY_ALARM_PARAMS:
                                    case KEY_SLOT_VIBRATION_NOTIFY_ALARM_PARAMS:
                                        if (result == 0) {
                                            isConfigError = true;
                                        }
                                        break;
                                    case KEY_SLOT_TRIGGER_ALARM_NOTIFY_TYPE:
                                        if (result == 0) {
                                            isConfigError = true;
                                        }
                                        if (isConfigError) {
                                            ToastUtils.showToast(AlarmNotifyTypeActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            ToastUtils.showToast(this, "Success");
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                // read
                                switch (configKeyEnum) {
                                    case KEY_SLOT_TRIGGER_ALARM_NOTIFY_TYPE:
                                        if (length == 2 && value[4] == slotType) {
                                            notifyType = value[5] & 0xFF;
                                            mBind.npvNotifyType.setValue(notifyType);
                                            if (notifyType == 1 || notifyType == 4 || notifyType == 5) {
                                                // LED/LED+Vibration/LED+Buzzer
                                                mBind.clLedNotify.setVisibility(View.VISIBLE);
                                            } else {
                                                mBind.clLedNotify.setVisibility(View.GONE);
                                            }
                                            if (notifyType == 2 || notifyType == 4) {
                                                // Vibration/LED+Vibration
                                                mBind.clVibrationNotify.setVisibility(View.VISIBLE);
                                            } else {
                                                mBind.clVibrationNotify.setVisibility(View.GONE);
                                            }
                                            if (notifyType == 3 || notifyType == 5) {
                                                // Buzzer/LED+Buzzer
                                                mBind.clBuzzerNotify.setVisibility(View.VISIBLE);
                                            } else {
                                                mBind.clBuzzerNotify.setVisibility(View.GONE);
                                            }
                                        }
                                        break;
                                    case KEY_SLOT_LED_NOTIFY_ALARM_PARAMS:
                                        if (length == 5 && value[4] == slotType) {
                                            int time = MokoUtils.toInt(Arrays.copyOfRange(value, 5, 7));
                                            int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 7, 9));
                                            mBind.etBlinkingTime.setText(String.valueOf(time));
                                            mBind.etBlinkingInterval.setText(String.valueOf(interval / 100));
                                        }
                                        break;
                                    case KEY_SLOT_VIBRATION_NOTIFY_ALARM_PARAMS:
                                        if (length == 5 && value[4] == slotType) {
                                            int time = MokoUtils.toInt(Arrays.copyOfRange(value, 5, 7));
                                            int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 7, 9));
                                            mBind.etVibratingTime.setText(String.valueOf(time));
                                            mBind.etVibratingInterval.setText(String.valueOf(interval / 100));
                                        }
                                        break;
                                    case KEY_SLOT_BUZZER_NOTIFY_ALARM_PARAMS:
                                        if (length == 5 && value[4] == slotType) {
                                            int time = MokoUtils.toInt(Arrays.copyOfRange(value, 5, 7));
                                            int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 7, 9));
                                            mBind.etRingingTime.setText(String.valueOf(time));
                                            mBind.etRingingInterval.setText(String.valueOf(interval / 100));
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
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        if (notifyType == 1 || notifyType == 4 || notifyType == 5) {
            String ledTimeStr = mBind.etBlinkingTime.getText().toString();
            String ledIntervalStr = mBind.etBlinkingInterval.getText().toString();
            int ledTime = Integer.parseInt(ledTimeStr);
            int ledInterval = Integer.parseInt(ledIntervalStr) * 100;
            // LED/LED+Vibration/LED+Buzzer
            orderTasks.add(OrderTaskAssembler.setSlotLEDNotifyAlarmParams(slotType, ledTime, ledInterval));
        }
        if (notifyType == 2 || notifyType == 4) {
            String vibrationTimeStr = mBind.etVibratingTime.getText().toString();
            String vibrationIntervalStr = mBind.etVibratingInterval.getText().toString();
            int vibrationTime = Integer.parseInt(vibrationTimeStr);
            int vibrationInterval = Integer.parseInt(vibrationIntervalStr) * 100;
            // Vibration/LED+Vibration
            orderTasks.add(OrderTaskAssembler.setSlotVibrationNotifyAlarmParams(slotType, vibrationTime, vibrationInterval));
        }
        if (notifyType == 3 || notifyType == 5) {
            String buzzerTimeStr = mBind.etRingingTime.getText().toString();
            String buzzerIntervalStr = mBind.etRingingInterval.getText().toString();
            int buzzerTime = Integer.parseInt(buzzerTimeStr);
            int buzzerInterval = Integer.parseInt(buzzerIntervalStr) * 100;
            // Buzzer/LED+Buzzer
            orderTasks.add(OrderTaskAssembler.setSlotBuzzerNotifyAlarmParams(slotType, buzzerTime, buzzerInterval));
        }
        orderTasks.add(OrderTaskAssembler.setSlotTriggerAlarmNotifyType(slotType, notifyType));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    private boolean isValid() {
        if (notifyType == 0)
            return true;
        String ledTimeStr = mBind.etBlinkingTime.getText().toString();
        String ledIntervalStr = mBind.etBlinkingInterval.getText().toString();
        String vibrationTimeStr = mBind.etVibratingTime.getText().toString();
        String vibrationIntervalStr = mBind.etVibratingInterval.getText().toString();
        String buzzerTimeStr = mBind.etRingingTime.getText().toString();
        String buzzerIntervalStr = mBind.etRingingInterval.getText().toString();
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
