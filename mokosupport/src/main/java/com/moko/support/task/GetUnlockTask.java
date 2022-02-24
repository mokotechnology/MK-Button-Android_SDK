package com.moko.support.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.entity.OrderCHAR;

public class GetUnlockTask extends OrderTask {

    public byte[] data;

    public GetUnlockTask() {
        super(OrderCHAR.CHAR_UNLOCK, OrderTask.RESPONSE_TYPE_READ);
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
