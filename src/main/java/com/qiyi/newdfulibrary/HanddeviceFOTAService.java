package com.qiyi.newdfulibrary;

import android.app.ActivityManager;
import android.app.QiyiAlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ProgressBar;

import java.io.File;
import java.lang.reflect.Method;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class HanddeviceFOTAService extends Service {

    private static final String TAG = "HanddeviceFOTAService";
    private static final Boolean DEBUG = true;
    private static final String ZIP_FILE_PATH = "/sdcard/Controller/";
    private static final String SETTINGSNAME = "com.qiyi.settings";
    private static String mAbsolutePath = null;

    private static String mDevicesName = null;
    private static String mAddress = null;
    private static int mBondStatus = -1;
    public  static BluetoothDevice mDeviceService = null;
    public static boolean mHaveDisable = false;
    private Method getStringMethod = null;
    private ProgressBar mProgressBar;

    private static RemoteCallbackList<IControllerListenner> mListListener = new RemoteCallbackList<>();

    public HanddeviceFOTAService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        if(DEBUG) {
            Log.d(TAG, "bind service now...");
        }
        return mHandOTAServiceBinder;
    }

    private final IControllerManager.Stub mHandOTAServiceBinder = new IControllerManager.Stub() {
        @Override
        public void registerListenner(IControllerListenner listenner) throws RemoteException {
            if(listenner != null){
                mListListener.register(listenner);
                final int N = mListListener.beginBroadcast();
                if(DEBUG) {
                    Log.d(TAG, "registerListenner in the hand ota services");
                }
                mListListener.finishBroadcast();
            }
        }

        @Override
        public void unregisterListenner(IControllerListenner listenner) throws RemoteException {
            if(listenner != null){
                mListListener.unregister(listenner);
                final int N = mListListener.beginBroadcast();
                if(DEBUG) {
                    Log.d(TAG, "unregisterListenner in the hand ota services");
                }
                mListListener.finishBroadcast();
            }
        }
    };

    public static RemoteCallbackList<IControllerListenner> getmListListener(){
        return mListListener;
    }

    @Override
    public void onCreate(){
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
    }

    private void stopService()
    {
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);//取消监听升级回调
        stopSelf();
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
    }

    public int onStartCommand(Intent intent, int int1, int int2)
    {
        mDevicesName = intent.getStringExtra("deviceName");
        mAddress = intent.getStringExtra("deviceAddress");
        mBondStatus = intent.getIntExtra("deviceBondStatus",-1);
        if(DEBUG){
            Log.d(TAG, "Edward onStartCommand bind() while mDevices = " + mDevicesName + ", mAdddress = " + mAddress + ", mBondStatus" + mBondStatus);
        }

        mDeviceService = (BluetoothDevice)intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        if(DEBUG){
            //Log.d(TAG, "Edward onStartCommand get mDevice.getName() = " + mDevice.getName() + ", mDevice.getAddress() = " + mDevice.getAddress() + ", mDevice.getBondState()" + mDevice.getBondState());
        }
        onStatusCheck();

        //begin to start ota
        beginStartFOTA(mDevicesName,mAddress);
        return Service.START_NOT_STICKY;
    }

    private void beginStartFOTA(String devname, String addr){
        if(DEBUG){
            Log.d(TAG, "Edward begin to build new initiator start nowwwww1");
        }
        /*
        final DfuServiceInitiator dfuService = new DfuServiceInitiator(addr)
                .setDeviceName(devname)
                .setForceDfu(false)
                .setKeepBond(false)
                .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
                .setZip(mAbsolutePath);
                */
        final DfuServiceInitiator dfuService = new DfuServiceInitiator(addr)
                .setDeviceName(devname)
                //.setForceDfu(false)
                .setKeepBond(false)
                //.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
                .setZip(mAbsolutePath);
        if(DEBUG){
            Log.d(TAG, "Edward begin to start fota");
        }
        dfuService.start(this, DfuService.class);
    }

    public  int onStatusCheck(){
        if(DEBUG){
            Log.d(TAG, "Edward begin to check status to see ota or not");
        }
        checkFile();

        if(DEBUG){
            Log.d(TAG, "Edward begin to change status");
        }
        stopBTMachine();

        if(DEBUG){
            Log.d(TAG, "Edward begin to remove Bond status");
        }
        removeBondStatus(HandDeviceRec.mDevice);

        stopControllerService();

        if(DEBUG){
            Log.d(TAG, "Edward begin to check bondstatus");
        }
        return -1;
    }

    private void stopBTMachine(){
        //begin to told the bt machine to stop for ota reason
        if(!mHaveDisable) {
            if(DEBUG) {
                Log.d(TAG, "tell the bluetooth machine to stop for ota");
            }
            Intent intent = new Intent("bluetooth_ota_update");
            intent.putExtra("start_bluetooth_state", false);
            HanddeviceFOTAService.this.sendBroadcast(intent);

            //delay 500ms for bt to stop
            try {
                Thread.sleep(500);
            } catch (Exception e){
                Log.d(TAG, "Sleep error");
            }
        }
    }

    private void stopControllerService(){
        if(DEBUG) {
            Log.d(TAG, "will tell the controller service to stop for ota");
        }
        Intent intent1 = new Intent("com.qiyi.iqiyicontrollerota.startsignal");
        HanddeviceFOTAService.this.sendBroadcast(intent1);
    }

    public  int checkFile(){
        if(DEBUG){
            Log.d(TAG, "Edward begin to checkFile");
        }
        mAbsolutePath = getmPathFile();
        File otaFile = new File(mAbsolutePath);

        if(!otaFile.exists()){
            Log.d(TAG, "Edward ota file do not exist");

            final int N = HanddeviceFOTAService.getmListListener().beginBroadcast();
            for(int i = 0; i< N; i++){
                IControllerListenner listenner = HanddeviceFOTAService.getmListListener().getBroadcastItem(i);
                if(listenner != null){
                    try{
                        listenner.onOtaStatusChange(5);
                    } catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
            }

            HanddeviceFOTAService.getmListListener().finishBroadcast();

            return -1;
        } else {
            Log.d(TAG, "Edward ota file exists");
            return 0;
        }
    }
    public  static String getmPathFile(){
        if(DEBUG){
            Log.d(TAG, "Edward begin to get file path");
        }
        String otapackagepath = null;

        File file = new File(ZIP_FILE_PATH);

        File[] array = file.listFiles();
        for(File f:array){
            String path = f.getAbsolutePath();
            if(TextUtils.isEmpty(path)){
                continue;
            }
            if(path.endsWith(".zip")){
                Log.e(TAG,"getmPathFile : f = "+f.getAbsolutePath());
                otapackagepath = f.getAbsolutePath();
            }
        }

        return otapackagepath;
    }

    public void removeBondStatus(BluetoothDevice btDevices){
        boolean result = false;
        try {
            final Method removeBondStatus = btDevices.getClass().getMethod("removeBond");
            if (removeBondStatus != null) {
                result = (Boolean) removeBondStatus.invoke(btDevices);
                if(DEBUG){
                    Log.w(TAG, "removeBondStatus result is " + result);
                }
                while (btDevices.getBondState() != BluetoothDevice.BOND_NONE){
                    Thread.sleep(20);
                }
            }

        } catch (final Exception e) {
            if(DEBUG) {
                Log.w(TAG, "An exception occurred while removing bond information", e);
            }
        }
    }

    private final DfuProgressListener mDfuProgressListener = new DfuProgressListener() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            if(DEBUG){
                Log.d(TAG, "Edward onDeviceConnecting");
            }
            //mProgressBar.setIndeterminate(true);
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {
            if(DEBUG){
                Log.d(TAG, "Edward onDeviceConnected");
            }
            //mProgressBar.setIndeterminate(true);
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            if(DEBUG){
                Log.d(TAG, "Edward onDfuProcessStarting");
            }
            //mProgressBar.setIndeterminate(true);
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            if(DEBUG){
                Log.d(TAG, "Edward onDfuProcessStarted");
            }
            final int N = HanddeviceFOTAService.getmListListener().beginBroadcast();
            for(int i = 0; i< N; i++){
                IControllerListenner listenner = HanddeviceFOTAService.getmListListener().getBroadcastItem(i);
                if(listenner != null){
                    try{
                        listenner.onProcessStart();
                    } catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
            }
            HanddeviceFOTAService.getmListListener().finishBroadcast();
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            if(DEBUG){
                Log.d(TAG, "Edward onEnablingDfuMode");
            }
            //mProgressBar.setIndeterminate(true);
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            if(DEBUG){
                Log.d(TAG, "Edward onProgressChanged");
            }
            final int N = HanddeviceFOTAService.getmListListener().beginBroadcast();
            for(int i = 0; i< N; i++){
                IControllerListenner listenner = HanddeviceFOTAService.getmListListener().getBroadcastItem(i);
                if(listenner != null){
                    try{
                        listenner.onProcessStart();
                        listenner.onOtaStatusChange(6);
                    } catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
            }
            HanddeviceFOTAService.getmListListener().finishBroadcast();
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            if(DEBUG){
                Log.d(TAG, "Edward onFirmwareValidating");
            }
            //mProgressBar.setIndeterminate(true);
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            if(DEBUG){
                Log.d(TAG, "Edward onDeviceDisconnecting");
            }
            //mProgressBar.setIndeterminate(true);
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            if(DEBUG){
                Log.d(TAG, "Edward onDeviceDisconnected");
            }
            //mProgressBar.setIndeterminate(true);
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            if(DEBUG){
                Log.d(TAG, "Edward onDfuCompleted begin to start controller service");
            }
            //tell the controller service to start
            Intent intent3 = new Intent("com.qiyi.iqiyicontrollerota.stopsignal");
            HanddeviceFOTAService.this.sendBroadcast(intent3);

            if(DEBUG){
                Log.d(TAG, "Edward begin to revive BT machine");
            }
            //tell the bluetooth machine to start
            Intent intent1 = new Intent("bluetooth_ota_update");
            intent1.putExtra("start_bluetooth_state", true);
            HanddeviceFOTAService.this.sendBroadcast(intent1);

            //tell app dfu completed
            final int N = HanddeviceFOTAService.getmListListener().beginBroadcast();
            for(int i = 0; i< N; i++){
                IControllerListenner listenner = HanddeviceFOTAService.getmListListener().getBroadcastItem(i);
                if(listenner != null){
                    try{
                        listenner.onProcessStart();
                        listenner.onOtaStatusChange(1);
                    } catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
            }
            HanddeviceFOTAService.getmListListener().finishBroadcast();
            //tell the service process done
            mHaveDisable = false;

            //DFU completed will delete the zip file
            if(DEBUG) {
                Log.d(TAG, "begin to delete the update zipfile");
            }
            clearFile(ZIP_FILE_PATH);

            if(DEBUG) {
                Log.d(TAG, "onDfuCompleted");
            }
            HanddeviceFOTAService.this.stopService();
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            if(DEBUG){
                Log.d(TAG, "Edward onDfuAborted");
            }
            //tell the controller service to start
            Intent intent3 = new Intent("com.qiyi.iqiyicontrollerota.stopsignal");
            HanddeviceFOTAService.this.sendBroadcast(intent3);

            //tell the bluetooth machine to start
            Intent intent1 = new Intent("bluetooth_ota_update");
            intent1.putExtra("start_bluetooth_state", true);
            HanddeviceFOTAService.this.sendBroadcast(intent1);

            //tell app dfu completed
            final int N = HanddeviceFOTAService.getmListListener().beginBroadcast();
            for(int i = 0; i< N; i++){
                IControllerListenner listenner = HanddeviceFOTAService.getmListListener().getBroadcastItem(i);
                if(listenner != null){
                    try{
                        listenner.onProcessStart();
                        listenner.onOtaStatusChange(3);
                    } catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
            }
            HanddeviceFOTAService.getmListListener().finishBroadcast();
            //tell the service process done
            mHaveDisable = false;

            //DFU completed will delete the zip file
            if(DEBUG) {
                Log.d(TAG, "begin to delete the update zipfile");
            }
            clearFile(ZIP_FILE_PATH);

            if(DEBUG) {
                Log.d(TAG, "onDfuAborted");
            }
            HanddeviceFOTAService.this.stopService();
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            if(DEBUG){
                Log.d(TAG, "Edward onError where error message is " + message);
            }
            //tell the controller service to start
            Intent intent3 = new Intent("com.qiyi.iqiyicontrollerota.stopsignal");
            HanddeviceFOTAService.this.sendBroadcast(intent3);

            //tell the bluetooth machine to start
            Intent intent1 = new Intent("bluetooth_ota_update");
            intent1.putExtra("start_bluetooth_state", true);
            HanddeviceFOTAService.this.sendBroadcast(intent1);

            //tell app dfu completed
            final int N = HanddeviceFOTAService.getmListListener().beginBroadcast();
            for(int i = 0; i< N; i++){
                IControllerListenner listenner = HanddeviceFOTAService.getmListListener().getBroadcastItem(i);
                if(listenner != null){
                    try{
                        listenner.onProcessStart();
                        listenner.onOtaStatusChange(2);
                    } catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
            }
            HanddeviceFOTAService.getmListListener().finishBroadcast();
            //tell the service process done
            mHaveDisable = false;

            //DFU completed will delete the zip file
            if(DEBUG) {
                Log.d(TAG, "begin to delete the update zipfile");
            }
            clearFile(ZIP_FILE_PATH);

            if(DEBUG) {
                Log.d(TAG, "onError");
            }
            HanddeviceFOTAService.this.stopService();
        }
    };

    public static void clearFile(String filePath){
        File targetFile = new File(filePath);
        File[] fileList = targetFile.listFiles();
        if(fileList[0].exists()){
            fileList[0].delete();
            Log.d(TAG, "clearFile success");
        }else {
            Log.d(TAG, "no file to delete");
        }
    }

    private boolean isSettingTop(){
        ActivityManager activityManager = (ActivityManager)HanddeviceFOTAService.this.getSystemService(Context.ACTIVITY_SERVICE);
        assert activityManager != null;
        String topPackage = activityManager.getRecentTasks(1,activityManager.RECENT_IGNORE_UNAVAILABLE).get(0).topActivity.getPackageName();
        Log.e(TAG,"focusedPackage isSettingTop" + topPackage);
        return SETTINGSNAME.equalsIgnoreCase(topPackage);
    }

    public void qiyiAlertDialogComp(){
        Log.d(TAG, "showCompletedDialog");
        if(isSettingTop()){
            Log.d(TAG, "Setting will show start Dialog");
        } else {
            QiyiAlertDialog.Builder Builder = new QiyiAlertDialog.Builder(this);
            QiyiAlertDialog dialog;
            Builder.setTitle("升级成功");
            Builder.setMessage("恭喜你遥控器更新成功，现在可以正常使用");
            dialog = Builder.create();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.setCanceledOnTouchOutside(false);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            dialog.show();
            dialog.delayDismiss();
        }
    }

    public void qiyiAlertDialogError(){
        Log.d(TAG, "show error Dialog");
        if(isSettingTop()){
            Log.d(TAG, "Setting will show start Dialog");
        } else {
            QiyiAlertDialog.Builder Builder = new QiyiAlertDialog.Builder(this);
            QiyiAlertDialog dialog;
            Builder.setTitle("升级失败");
            Builder.setMessage("遥控器电量过低或其他原因导致升级失败，请前往设置页面手动升级");
            dialog = Builder.create();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.setCanceledOnTouchOutside(false);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            dialog.show();
            dialog.delayDismiss();
        }
    }
}
