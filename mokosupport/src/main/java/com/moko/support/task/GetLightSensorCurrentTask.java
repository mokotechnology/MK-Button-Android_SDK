package com.moko.support.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.entity.OrderCHAR;


public class GetLightSensorCurrentTask extends OrderTask {

    public byte[] data;

    public GetLightSensorCurrentTask() {
        super(OrderCHAR.CHAR_LIGHT_SENSOR_CURRENT, OrderTask.RESPONSE_TYPE_READ);
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
