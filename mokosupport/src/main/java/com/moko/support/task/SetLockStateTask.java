package com.moko.support.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.entity.OrderCHAR;

public class SetLockStateTask extends OrderTask {

    public byte[] data;

    public SetLockStateTask() {
        super(OrderCHAR.CHAR_LOCK_STATE, OrderTask.RESPONSE_TYPE_WRITE);
    }

    @Override
    public byte[] assemble() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
