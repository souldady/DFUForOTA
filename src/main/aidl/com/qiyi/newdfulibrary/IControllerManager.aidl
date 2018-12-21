// IControllerManager.aidl
package com.qiyi.newdfulibrary;

// Declare any non-default types here with import statements
import com.qiyi.newdfulibrary.IControllerListenner;

interface IControllerManager {
    /**
         * Demonstrates some basic types that you can use as parameters
         * and return values in AIDL.
         */
        void registerListenner(IControllerListenner listenner);
        void unregisterListenner(IControllerListenner listenner);
}
