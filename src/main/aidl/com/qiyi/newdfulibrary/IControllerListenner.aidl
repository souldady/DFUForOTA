// IControllerListenner.aidl
package com.qiyi.newdfulibrary;

// Declare any non-default types here with import statements

interface IControllerListenner {
    /**
         * Demonstrates some basic types that you can use as parameters
         * and return values in AIDL.
         */
        void onOtaProcessChange(int percent, float speed, float avgSpeed, int currentPart, int partsTotal);
        void onProcessStart();
        void onProcessCompleted();
        void onProcessError(String message);
        void onProcessAborted();
        //status : 0 not ota; 1 completed; 2 error; 3 aborted; 4 not enough battary; 5 file missing; 6 otaing;
        void onOtaStatusChange(int status);
}
