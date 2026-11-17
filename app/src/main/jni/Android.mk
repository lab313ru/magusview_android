LOCAL_PATH := $(call my-dir)
WIFI_SDK_ROOT := $(LOCAL_PATH)/../../../../vendor/toupnamsdk
include $(CLEAR_VARS)
LOCAL_MODULE := toupnam
LOCAL_SRC_FILES := $(WIFI_SDK_ROOT)/android/arm64/libtoupnam.so
include $(PREBUILT_SHARED_LIBRARY)
include $(CLEAR_VARS)
LOCAL_MODULE := magusview
LOCAL_SRC_FILES := camera.cpp
LOCAL_C_INCLUDES := $(WIFI_SDK_ROOT)/inc
LOCAL_CPPFLAGS := -std=c++17 -Wall -Wextra -Werror -O2
LOCAL_LDFLAGS := -Wl,-z,max-page-size=16384
LOCAL_SHARED_LIBRARIES := toupnam
include $(BUILD_SHARED_LIBRARY)
