package com.moko.bxp.button.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.moko.ble.lib.task.OrderTask;
import com.moko.bxp.button.R;
import com.moko.support.MokoSupport;
import com.moko.support.OrderTaskAssembler;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DeviceFragment extends Fragment {

    @BindView(R.id.et_device_name)
    EditText etDeviceName;

    public DeviceFragment() {
    }

    public static DeviceFragment newInstance() {
        DeviceFragment fragment = new DeviceFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    public void setDeviceName(String deviceName) {
        etDeviceName.setText(deviceName);
    }

    public boolean isValid() {
        String deviceNameStr = etDeviceName.getText().toString();
        if (TextUtils.isEmpty(deviceNameStr))
            return false;
        return true;
    }

    public void saveParams() {
        String deviceName = etDeviceName.getText().toString();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setDeviceName(deviceName));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }
}
