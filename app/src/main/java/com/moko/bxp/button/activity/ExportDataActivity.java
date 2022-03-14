package com.moko.bxp.button.activity;


import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.bxp.button.AppConstants;
import com.moko.bxp.button.BaseApplication;
import com.moko.bxp.button.R;
import com.moko.bxp.button.adapter.ExportDataListAdapter;
import com.moko.bxp.button.dialog.LoadingMessageDialog;
import com.moko.bxp.button.entity.ExportData;
import com.moko.bxp.button.utils.Utils;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderCHAR;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ExportDataActivity extends BaseActivity {

    private static final String TRACKED_FILE = "trigger_event.txt";

    private static String PATH_LOGCAT;
    @BindView(R.id.iv_sync)
    ImageView ivSync;
    @BindView(R.id.tv_sync)
    TextView tvSync;
    @BindView(R.id.tv_export)
    TextView tvExport;
    @BindView(R.id.rv_export_data)
    RecyclerView rvExportData;
    @BindView(R.id.tv_title)
    TextView tvTitle;

    private StringBuilder storeString;
    private ArrayList<ExportData> exportDatas;
    private boolean mIsSync;
    private ExportDataListAdapter adapter;
    private SimpleDateFormat sdf;
    private TimeZone timeZone;


    public int slotType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_data);
        ButterKnife.bind(this);
        if (getIntent() != null && getIntent().getExtras() != null) {
            slotType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_SLOT_TYPE, 0);
        }
        switch (slotType) {
            case 0:
                tvTitle.setText("Single press event");
                break;
            case 1:
                tvTitle.setText("Double press event");
                break;
            case 2:
                tvTitle.setText("Long press event");
                break;
        }
        exportDatas = new ArrayList<>();
        storeString = new StringBuilder();
        adapter = new ExportDataListAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(exportDatas);
        rvExportData.setLayoutManager(new LinearLayoutManager(this));
        rvExportData.setAdapter(adapter);
        timeZone = TimeZone.getTimeZone("GMT");
        sdf = new SimpleDateFormat(AppConstants.PATTERN_YYYY_MM_DD_T_HH_MM_SS_Z, Locale.US);
        sdf.setTimeZone(timeZone);
        PATH_LOGCAT = BaseApplication.PATH_LOGCAT + File.separator + TRACKED_FILE;
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                finish();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (!MokoConstants.ACTION_CURRENT_DATA.equals(action))
            EventBus.getDefault().cancelEventDelivery(event);
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {
                    case CHAR_SINGLE_TRIGGER:
                    case CHAR_DOUBLE_TRIGGER:
                    case CHAR_LONG_TRIGGER:
                        int header = value[0] & 0xFF;// 0xEB
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header != 0xEB)
                            return;
                        int length = value[3] & 0xFF;
                        if (flag == 0x02 && cmd == (slotType + 1) && length == 0x09) {
                            ExportData exportData = new ExportData();
                            byte[] timeBytes = Arrays.copyOfRange(value, 4, 4 + 8);
                            ByteBuffer byteBuffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).put(timeBytes, 0, timeBytes.length);
                            byteBuffer.flip();
                            long time = byteBuffer.getLong();
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeZone(timeZone);
                            calendar.setTimeInMillis(time);

                            String timestampStr = sdf.format(calendar.getTime());
                            exportData.timestamp = timestampStr;
                            switch (slotType) {
                                case 0:
                                    exportData.triggerMode = "Single press mode";
                                    break;
                                case 1:
                                    exportData.triggerMode = "Double press mode";
                                    break;
                                case 2:
                                    exportData.triggerMode = "Long press mode";
                                    break;
                            }
                            exportDatas.add(exportData);
                            storeString.append(timestampStr);
                            storeString.append("  ");
                            storeString.append(exportData.triggerMode);
                            storeString.append("\n");
                        }
                        adapter.replaceData(exportDatas);
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

    private void back() {
        if (mIsSync) {
            if (slotType == 0) {
                MokoSupport.getInstance().disableSingleTriggerNotify();
            }
            if (slotType == 1) {
                MokoSupport.getInstance().disableDoubleTriggerNotify();
            }
            if (slotType == 2) {
                MokoSupport.getInstance().disableLongTriggerNotify();
            }
        }
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            back();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }


    public void onSync(View view) {
        if (isWindowLocked())
            return;
        if (!mIsSync) {
            mIsSync = true;
            tvExport.setEnabled(false);
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
            ivSync.startAnimation(animation);
            tvSync.setText("Stop");
            if (slotType == 0) {
                MokoSupport.getInstance().enableSingleTriggerNotify();
            }
            if (slotType == 1) {
                MokoSupport.getInstance().enableDoubleTriggerNotify();
            }
            if (slotType == 2) {
                MokoSupport.getInstance().enableLongTriggerNotify();
            }
        } else {
            mIsSync = false;
            if (exportDatas != null && exportDatas.size() > 0 && storeString != null) {
                tvExport.setEnabled(true);
            }
            ivSync.clearAnimation();
            tvSync.setText("Sync");
            if (slotType == 0) {
                MokoSupport.getInstance().disableSingleTriggerNotify();
            }
            if (slotType == 1) {
                MokoSupport.getInstance().disableDoubleTriggerNotify();
            }
            if (slotType == 2) {
                MokoSupport.getInstance().disableLongTriggerNotify();
            }
        }
    }

    public void onEmpty(View view) {
        if (isWindowLocked())
            return;
        exportDatas.clear();
        adapter.replaceData(exportDatas);
    }

    public void onExport(View view) {
        if (isWindowLocked())
            return;
        showSyncingProgressDialog();
        writeTrackedFile("");
        tvExport.postDelayed(() -> {
            dismissSyncProgressDialog();
            final String log = storeString.toString();
            if (!TextUtils.isEmpty(log)) {
                writeTrackedFile(log);
                File file = getTrackedFile();
                // 发送邮件
                String address = "Development@mokotechnology.com";
                String title = "Trigger Event";
                String content = title;
                Utils.sendEmail(ExportDataActivity.this, address, content, title, "Choose Email Client", file);
            }
        }, 500);
    }

    public void onBack(View view) {
        back();
    }


    public static void writeTrackedFile(String thLog) {
        File file = new File(PATH_LOGCAT);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(thLog);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File getTrackedFile() {
        File file = new File(PATH_LOGCAT);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
