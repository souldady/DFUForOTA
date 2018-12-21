# DFUForOTA
Using dfu to OTA BT devices

#prebuild
include $(CLEAR_VARS)
LOCAL_MODULE        := controllerota
LOCAL_MODULE_OWNER  := qcom
LOCAL_MODULE_TAGS   := optional
LOCAL_MODULE_CLASS  := APPS
LOCAL_CERTIFICATE   := platform
LOCAL_DEX_PREOPT    := false
LOCAL_MODULE_SUFFIX := .apk
LOCAL_SRC_FILES     := system/priv-app/controllerota/controllerota.apk
LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PREBUILT)

#redesign the broadcast and file path
