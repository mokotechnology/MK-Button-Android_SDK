package com.moko.bxp.button.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.moko.ble.lib.task.OrderTask;
import com.moko.bxp.button.databinding.FragmentDeviceBinding;
import com.moko.support.MokoSupport;
import com.moko.support.OrderTaskAssembler;

import java.util.ArrayList;
import java.util.List;

public class DeviceFragment extends Fragment {
    private final String FILTER_ASCII = "[ -~]*";

    private FragmentDeviceBinding mBind;

    public DeviceFragment() {
    }

    public static DeviceFragment newInstance() {
        DeviceFragment fragment = new DeviceFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBind = FragmentDeviceBinding.inflate(inflater, container, false);
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (!(source + "").matches(FILTER_ASCII)) {
                    return "";
                }

                return null;
            }
        };
        mBind.etDeviceName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10), filter});
        return mBind.getRoot();
    }

    public void setDeviceName(String deviceName) {
        mBind.etDeviceName.setText(deviceName);
    }

    public boolean isValid() {
        String deviceNameStr = mBind.etDeviceName.getText().toString();
        if (TextUtils.isEmpty(deviceNameStr))
            return false;
        int length = deviceNameStr.length();
        if (length < 1 || length > 10)
            return false;
        String deviceIdStr = mBind.etDeviceId.getText().toString();
        if (TextUtils.isEmpty(deviceIdStr))
            return false;
        if (deviceIdStr.length() % 2 != 0)
            return false;
        return true;
    }

    public void saveParams() {
        String deviceName = mBind.etDeviceName.getText().toString();
        String deviceStr = mBind.etDeviceId.getText().toString();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setDeviceName(deviceName));
        orderTasks.add(OrderTaskAssembler.setDeviceId(deviceStr));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void setViewShown(boolean enablePasswordVerify) {
        mBind.rlResetFactory.setVisibility(enablePasswordVerify ? View.VISIBLE : View.GONE);
        mBind.rlPassword.setVisibility(enablePasswordVerify ? View.VISIBLE : View.GONE);
    }

    public void setResetShown() {
        mBind.rlResetFactory.setVisibility(View.VISIBLE);
    }

    public void setDeviceId(String deviceIdHex) {
        mBind.etDeviceId.setText(deviceIdHex);
    }
}
