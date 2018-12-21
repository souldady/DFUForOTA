package com.qiyi.newdfulibrary;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HandDeviceRec extends BroadcastReceiver {

    public static final String TAG = "HandDeviceRec";
    public static final Boolean DEBUG = true;
    private static final String ACTION_READY_FOR_OTA = "com.longcheer.net.action.readyForOTA";
    private static final String DEVICE_NAME = "iQIYI VR iDream";
    private static final String BT_CONNECTED = "android.bluetooth.device.action.ACL_CONNECTED";
    public  static BluetoothDevice mDevice = null;
    private static String mDevicesNameRec = null;
    private static String mAddressRec = null;
    private static int mBondStatusRec = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        String actionStr =  intent.getAction();
        if(DEBUG){
            Log.d(TAG, "Edward Hand devices FOTA HandDeviceRec is alive");
        }

        if(BT_CONNECTED.equals(actionStr)){
            mDevice = (BluetoothDevice)intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            mDevicesNameRec = mDevice.getName();
            mAddressRec = mDevice.getAddress();
            mBondStatusRec = mDevice.getBondState();
            if(DEBUG){
                Log.d(TAG, "Edward Hand devices FOTA has detected BT connected. while device name is " + mDevice.getName());
            }
        } else if(ACTION_READY_FOR_OTA.equals(actionStr)){
            if(DEBUG){
                Log.d(TAG, "Edward Hand devices FOTA has received FOTA ready~~~~~~~~~~~~~~.");
            }

            //Start service
            Intent intent0 = new Intent(context, HanddeviceFOTAService.class);
            intent0.putExtra("deviceName", mDevicesNameRec);
            intent0.putExtra("deviceAddress", mAddressRec);
            intent0.putExtra("deviceBondStatus", mBondStatusRec);
            context.startService(intent0);
        }
    }
}
