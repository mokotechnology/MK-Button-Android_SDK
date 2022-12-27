package com.moko.bxp.button.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.SeekBar;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.bxp.button.AppConstants;
import com.moko.bxp.button.R;
import com.moko.bxp.button.databinding.ActivityAlarmModeConfigBinding;
import com.moko.bxp.button.dialog.LoadingMessageDialog;
import com.moko.bxp.button.utils.ToastUtils;
import com.moko.support.MokoSupport;
import com.moko.support.OrderTaskAssembler;
import com.moko.support.entity.OrderCHAR;
import com.moko.support.entity.ParamsKeyEnum;
import com.moko.support.entity.TxPowerEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;

public class AlarmModeConfigActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {

    private ActivityAlarmModeConfigBinding mBind;
    public boolean isConfigError;
    public int slotType;
    private boolean isAdvOpen;
    private boolean isTriggerOpen;
    private boolean isAdvBeforeTriggerOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = ActivityAlarmModeConfigBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        if (getIntent() != null && getIntent().getExtras() != null) {
            slotType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_SLOT_TYPE, 0);
        }
        switch (slotType) {
            case 0:
                mBind.tvAlarmTitle.setText("Single press alarm mode");
                break;
            case 1:
                mBind.tvAlarmTitle.setText("Double press alarm mode");
                break;
            case 2:
                mBind.tvAlarmTitle.setText("Long press alarm mode");
                break;
            case 3:
                mBind.tvAlarmTitle.setText("Abnormal inactivity alarm mode");
                break;
        }
        if (slotType == 3) {
            mBind.rlAbnormalInactivityTime.setVisibility(View.VISIBLE);
        }
        mBind.sbAdvRangeData.setOnSeekBarChangeListener(this);
        mBind.sbTxPower.setOnSeekBarChangeListener(this);
        mBind.sbTriggerTxPower.setOnSeekBarChangeListener(this);
        mBind.etAbnormalInactivityTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String advTime = mBind.etTriggerAdvTime.getText().toString();
                String inactivityTime = editable.toString();
                mBind.tvAbnormalInactivityTimeTips.setText(getString(R.string.abnormal_inactivity_time_tips, inactivityTime, advTime));
            }
        });
        mBind.etTriggerAdvTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String advTime = editable.toString();
                String inactivityTime = mBind.etAbnormalInactivityTime.getText().toString();
                mBind.tvAbnormalInactivityTimeTips.setText(getString(R.string.abnormal_inactivity_time_tips, inactivityTime, advTime));
            }
        });
        EventBus.getDefault().register(this);
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            MokoSupport.getInstance().enableBluetooth();
        } else {
            showSyncingProgressDialog();
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getSlotParams(slotType));
            orderTasks.add(OrderTaskAssembler.getSlotTriggerParams(slotType));
            orderTasks.add(OrderTaskAssembler.getSlotAdvBeforeTriggerEnable(slotType));
            if (slotType == 3) {
                orderTasks.add(OrderTaskAssembler.getAbnormalInactivityAlarmStaticInterval());
            }
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
                    AlarmModeConfigActivity.this.finish();
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
                                    case KEY_SLOT_PARAMS:
                                    case KEY_ABNORMAL_INACTIVITY_ALARM_STATIC_INTERVAL:
                                    case KEY_SLOT_ADV_BEFORE_TRIGGER_ENABLE:
                                        if (result == 0) {
                                            isConfigError = true;
                                        }
                                        break;
                                    case KEY_SLOT_TRIGGER_PARAMS:
                                        if (result == 0) {
                                            isConfigError = true;
                                        }
                                        if (isConfigError) {
                                            ToastUtils.showToast(AlarmModeConfigActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            ToastUtils.showToast(this, "Success");
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                // read
                                switch (configKeyEnum) {
                                    case KEY_SLOT_PARAMS:
                                        if (length == 6 && value[4] == slotType) {
                                            int slotEnable = value[5] & 0xFF;
                                            int rangingData = value[6];
                                            int advInterval = MokoUtils.toInt(Arrays.copyOfRange(value, 7, 9));
                                            int txPower = value[9];
                                            isAdvOpen = slotEnable == 1;
                                            mBind.ivSlotAdvSwitch.setImageResource(isAdvOpen ? R.drawable.ic_checked : R.drawable.ic_unchecked);
                                            int progress = rangingData + 100;
                                            mBind.sbAdvRangeData.setProgress(progress);
                                            mBind.etAdvInterval.setText(String.valueOf(advInterval / 20));
                                            TxPowerEnum txPowerEnum = TxPowerEnum.fromTxPower(txPower);
                                            mBind.sbTxPower.setProgress(txPowerEnum.ordinal());
                                            mBind.llSlotAlarmParams.setVisibility(isAdvOpen ? View.VISIBLE : View.GONE);
                                        }
                                        break;
                                    case KEY_SLOT_TRIGGER_PARAMS:
                                        if (length == 8 && value[4] == slotType) {
                                            int triggerEnable = value[5] & 0xFF;
                                            int triggerAdvInterval = MokoUtils.toInt(Arrays.copyOfRange(value, 7, 9));
                                            int txPower = value[9];
                                            int triggerAdvTime = MokoUtils.toInt(Arrays.copyOfRange(value, 10, 12));
                                            isTriggerOpen = triggerEnable == 1;
                                            mBind.ivAlarmMode.setImageResource(isTriggerOpen ? R.drawable.ic_checked : R.drawable.ic_unchecked);
                                            mBind.etTriggerAdvInterval.setText(String.valueOf(triggerAdvInterval / 20));
                                            TxPowerEnum txPowerEnum = TxPowerEnum.fromTxPower(txPower);
                                            mBind.sbTriggerTxPower.setProgress(txPowerEnum.ordinal());
                                            mBind.etTriggerAdvTime.setText(String.valueOf(triggerAdvTime));
                                            mBind.llSlotTriggerParams.setVisibility(isTriggerOpen ? View.VISIBLE : View.GONE);
                                        }
                                        break;
                                    case KEY_SLOT_ADV_BEFORE_TRIGGER_ENABLE:
                                        if (length == 2 && value[4] == slotType) {
                                            int advBeforeTriggerEnable = value[5] & 0xFF;
                                            isAdvBeforeTriggerOpen = advBeforeTriggerEnable == 1;
                                            mBind.ivAdvBeforeTriggered.setImageResource(isAdvBeforeTriggerOpen ? R.drawable.ic_checked : R.drawable.ic_unchecked);
                                        }
                                        break;
                                    case KEY_ABNORMAL_INACTIVITY_ALARM_STATIC_INTERVAL:
                                        if (length == 0x02) {
                                            int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            mBind.etAbnormalInactivityTime.setText(String.valueOf(interval));
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

    @Override
    public void onBackPressed() {
        back();
    }

    public void onBack(View view) {
        back();
    }

    private void back() {
        Intent intent = new Intent();
        intent.putExtra(AppConstants.EXTRA_KEY_SLOT_TYPE, slotType);
        setResult(RESULT_OK, intent);
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
        String advIntervalStr = mBind.etAdvInterval.getText().toString();
        String triggerAdvTimeStr = mBind.etTriggerAdvTime.getText().toString();
        String triggerAdvIntervalStr = mBind.etTriggerAdvInterval.getText().toString();
        String abnormalInactivityTimeStr = mBind.etAbnormalInactivityTime.getText().toString();
        int advInterval = Integer.parseInt(advIntervalStr);
        int triggerAdvTime = Integer.parseInt(triggerAdvTimeStr);
        int triggerAdvInterval = Integer.parseInt(triggerAdvIntervalStr);

        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        if (slotType == 3) {
            int abnormalInactivityTime = Integer.parseInt(abnormalInactivityTimeStr);
            orderTasks.add(OrderTaskAssembler.setAbnormalInactivityAlarmStaticInterval(abnormalInactivityTime));
        }
        orderTasks.add(OrderTaskAssembler.setSlotParams(
                slotType,
                isAdvOpen ? 1 : 0,
                mBind.sbAdvRangeData.getProgress() - 100,
                advInterval * 20,
                TxPowerEnum.fromOrdinal(mBind.sbTxPower.getProgress()).getTxPower()));
        orderTasks.add(OrderTaskAssembler.setSlotAdvBeforeTriggerEnable(slotType, isAdvBeforeTriggerOpen ? 1 : 0));
        orderTasks.add(OrderTaskAssembler.setSlotTriggerParams(
                slotType,
                isTriggerOpen ? 1 : 0,
                mBind.sbAdvRangeData.getProgress() - 100,
                triggerAdvInterval * 20,
                TxPowerEnum.fromOrdinal(mBind.sbTriggerTxPower.getProgress()).getTxPower(),
                triggerAdvTime));

        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    private boolean isValid() {
        String advIntervalStr = mBind.etAdvInterval.getText().toString();
        String triggerAdvTimeStr = mBind.etTriggerAdvTime.getText().toString();
        String triggerAdvIntervalStr = mBind.etTriggerAdvInterval.getText().toString();
        String abnormalInactivityTimeStr = mBind.etAbnormalInactivityTime.getText().toString();
        if (TextUtils.isEmpty(advIntervalStr)
                || TextUtils.isEmpty(triggerAdvTimeStr)
                || TextUtils.isEmpty(triggerAdvIntervalStr)
                || (slotType == 3 && TextUtils.isEmpty(abnormalInactivityTimeStr))) {
            return false;
        }
        int advInterval = Integer.parseInt(advIntervalStr);
        if (advInterval < 1 || advInterval > 500)
            return false;
        int triggerAdvTime = Integer.parseInt(triggerAdvTimeStr);
        if (triggerAdvTime < 1 || triggerAdvTime > 65535)
            return false;
        int triggerAdvInterval = Integer.parseInt(triggerAdvIntervalStr);
        if (triggerAdvInterval < 1 || triggerAdvInterval > 500)
            return false;
        if (slotType == 3) {
            int abnormalInactivityTime = Integer.parseInt(abnormalInactivityTimeStr);
            if (abnormalInactivityTime < 1 || abnormalInactivityTime > 65535)
                return false;
        }
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.sb_adv_range_data:
                mBind.tvAdvRangeData.setText(String.format("%ddBm", progress - 100));
                break;
            case R.id.sb_tx_power:
                TxPowerEnum txPowerEnum = TxPowerEnum.fromOrdinal(progress);
                mBind.tvTxPower.setText(String.format("%ddBm", txPowerEnum.getTxPower()));
                break;
            case R.id.sb_trigger_tx_power:
                TxPowerEnum triggerTxPowerEnum = TxPowerEnum.fromOrdinal(progress);
                mBind.tvTriggerTxPower.setText(String.format("%ddBm", triggerTxPowerEnum.getTxPower()));
                break;
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void onSlotAdvSwitch(View view) {
        if (isWindowLocked())
            return;
        isAdvOpen = !isAdvOpen;
        mBind.ivSlotAdvSwitch.setImageResource(isAdvOpen ? R.drawable.ic_checked : R.drawable.ic_unchecked);
        mBind.llSlotAlarmParams.setVisibility(isAdvOpen ? View.VISIBLE : View.GONE);
    }

    public void onSlotAlarmModeSwitch(View view) {
        if (isWindowLocked())
            return;
        isTriggerOpen = !isTriggerOpen;
        mBind.ivAlarmMode.setImageResource(isTriggerOpen ? R.drawable.ic_checked : R.drawable.ic_unchecked);
        mBind.llSlotTriggerParams.setVisibility(isTriggerOpen ? View.VISIBLE : View.GONE);
    }

    public void onSlotAdvBeforeTriggeredSwitch(View view) {
        if (isWindowLocked())
            return;
        isAdvBeforeTriggerOpen = !isAdvBeforeTriggerOpen;
        mBind.ivAdvBeforeTriggered.setImageResource(isAdvBeforeTriggerOpen ? R.drawable.ic_checked : R.drawable.ic_unchecked);
    }

    public void onTriggerNotifyType(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, AlarmNotifyTypeActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_SLOT_TYPE, slotType);
        startActivity(intent);
    }
}
