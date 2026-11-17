#include <jni.h>
#include <atomic>
#include <cstdint>
#include <cstring>
#include <ctime>
#include <algorithm>
#include <string>
#include <vector>
#include "toupnam.h"

// Serialized by the Java camera executor; SDK callbacks only store errors.
static HToupnam camera = nullptr;
static std::atomic<unsigned> lastEvent{0};
static int width = 0, height = 0;
static ToupnamImagePtr heldFrame{};
static constexpr int invalidArgument = static_cast<int>(0x80070057u);
static constexpr int unexpected = static_cast<int>(0x8000ffffu);
static constexpr int failed = static_cast<int>(0x80004005u);

static void releaseFrame() {
    if (camera && heldFrame.id) Toupnam_PushImagePtr(camera, heldFrame.id);
    heldFrame = {};
}

static void callback(unsigned event, unsigned, void*, ToupnamEventExtra*) {
    if (event == TOUPNAM_EVENT_ERROR || event == TOUPNAM_EVENT_EOF) lastEvent.store(event);
}
extern "C" {
JNIEXPORT jstring JNICALL Java_com_magusview_android_CameraSdk_version(JNIEnv* env, jclass) {
    return env->NewStringUTF(Toupnam_Version());
}
JNIEXPORT void JNICALL Java_com_magusview_android_CameraSdk_enableWifi(JNIEnv*, jclass) {
    static bool initialized = false;
    if (!initialized) {
        // Broadcast must be enabled BEFORE Init. NAM discovery is distinct from GigE.
        // CHD50 preview succeeds with default transport; forcing RTP-over-RTSP stalls.
        Toupnam_PriFlag(TOUPNAM_PRIFLAG_BROADCAST,
                       TOUPNAM_PRIFLAG_BROADCAST | TOUPNAM_PRIFLAG_RTPOVERRTSP);
        Toupnam_Init(callback, nullptr);
        initialized = true;
    }
}
JNIEXPORT jobjectArray JNICALL Java_com_magusview_android_CameraSdk_discover(JNIEnv* env, jclass) {
    std::vector<ToupnamDevice> devices(TOUPNAM_MAX);
    unsigned count = Toupnam_Enum(devices.data(), static_cast<unsigned>(devices.size()));
    if (count > devices.size()) {
        devices.resize(std::min(count, 1024u));
        count = Toupnam_Enum(devices.data(), static_cast<unsigned>(devices.size()));
    }
    count = std::min(count, static_cast<unsigned>(devices.size()));
    std::vector<std::string> entries;
    for (unsigned i = 0; i < count; ++i) {
        const auto& device = devices[i];
        if (device.state != TOUPNAM_STATE_NORMAL) continue;
        std::string name(device.name, strnlen(device.name, sizeof(device.name)));
        std::string model(device.model, strnlen(device.model, sizeof(device.model)));
        std::string address(device.addr, strnlen(device.addr, sizeof(device.addr)));
        std::string id(device.id, strnlen(device.id, sizeof(device.id)));
        if (id.empty()) continue;
        std::string label = name.empty() ? model : name;
        if (label.empty()) label = "Wi-Fi camera";
        if (!address.empty()) label += " - " + address;
        entries.push_back(label);
        entries.push_back(id);
    }
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(static_cast<jsize>(entries.size()), stringClass, nullptr);
    for (size_t i = 0; i < entries.size(); ++i) {
        jstring value = env->NewStringUTF(entries[i].c_str());
        env->SetObjectArrayElement(result, static_cast<jsize>(i), value);
        env->DeleteLocalRef(value);
    }
    return result;
}
JNIEXPORT void JNICALL Java_com_magusview_android_CameraSdk_close(JNIEnv*, jclass) {
    releaseFrame();
    if (camera) Toupnam_Close(camera);
    camera = nullptr;
    width = height = 0;
    lastEvent.store(0);
}
JNIEXPORT jint JNICALL Java_com_magusview_android_CameraSdk_open(JNIEnv* env, jclass, jstring id) {
    if (camera) return unexpected;
    const char* value = env->GetStringUTFChars(id, nullptr);
    if (!value) return invalidArgument;
    camera = Toupnam_Open(value);
    env->ReleaseStringUTFChars(id, value);
    if (!camera) return failed;
    lastEvent.store(0);
    // Stream dimensions are not necessarily known until the decoder receives a frame.
    width = height = 0;
    // The Android SDK crashed in its four-channel conversion (PC 0x4e74ec).
    // Keep its default BGR24 path; expand to Android RGBA in our bounds-checked bridge.
    return Toupnam_put_Para(camera, TOUPNAM_PARA_FORMAT_LOCAL, 0); // BGR888
}
JNIEXPORT jintArray JNICALL Java_com_magusview_android_CameraSdk_size(JNIEnv* env, jclass) {
    jint values[] = {width, height};
    jintArray result = env->NewIntArray(2);
    env->SetIntArrayRegion(result, 0, 2, values);
    return result;
}
JNIEXPORT jint JNICALL Java_com_magusview_android_CameraSdk_start(JNIEnv*, jclass) {
    return camera ? Toupnam_StartPullModeWithCallback(camera, nullptr) : unexpected;
}
JNIEXPORT jint JNICALL Java_com_magusview_android_CameraSdk_acquireFrame(JNIEnv*, jclass) {
    releaseFrame();
    if (!camera) return unexpected;
    int hr = Toupnam_PullImagePtr(camera, &heldFrame);
    if (FAILED(hr)) { releaseFrame(); return hr; }
    // Reject malformed or oversized frames before allocating memory or narrowing dimensions.
    const uint64_t bytes = static_cast<uint64_t>(heldFrame.width) * heldFrame.height * 4;
    const uint64_t sourceStride = (static_cast<uint64_t>(heldFrame.width) * 3 + 3) & ~uint64_t(3);
    const uint64_t sourceBytes = sourceStride * heldFrame.height;
    if (!heldFrame.id || !heldFrame.buf || heldFrame.width == 0 || heldFrame.height == 0
            || heldFrame.width > 16384 || heldFrame.height > 16384
            || bytes > 64u * 1024u * 1024u
            // NAM 5.33015 returns length=0 for decoded CHD50 frames on Windows.
            // In decoded mode the configured format and dimensions define the byte count.
            || (heldFrame.length != 0 && heldFrame.length < sourceBytes)) {
        releaseFrame();
        return invalidArgument;
    }
    width = static_cast<int>(heldFrame.width);
    height = static_cast<int>(heldFrame.height);
    return hr;
}
JNIEXPORT void JNICALL Java_com_magusview_android_CameraSdk_releaseFrame(JNIEnv*, jclass) {
    releaseFrame();
}
JNIEXPORT jint JNICALL Java_com_magusview_android_CameraSdk_pull(JNIEnv* env, jclass, jobject pixels) {
    const int64_t bytes = static_cast<int64_t>(width) * height * 4;
    auto* data = static_cast<uint8_t*>(env->GetDirectBufferAddress(pixels));
    if (!camera || !heldFrame.id || !heldFrame.buf || !data || bytes <= 0
            || env->GetDirectBufferCapacity(pixels) < bytes) return invalidArgument;
    const size_t sourceStride = (static_cast<size_t>(width) * 3 + 3) & ~size_t(3);
    for (int y = 0; y < height; ++y) {
        const uint8_t* source = heldFrame.buf + static_cast<size_t>(y) * sourceStride;
        uint8_t* target = data + static_cast<size_t>(y) * width * 4;
        for (int x = 0; x < width; ++x) {
            target[x * 4] = source[x * 3 + 2];
            target[x * 4 + 1] = source[x * 3 + 1];
            target[x * 4 + 2] = source[x * 3];
            target[x * 4 + 3] = 255;
        }
    }
    return 0;
}
JNIEXPORT jint JNICALL Java_com_magusview_android_CameraSdk_event(JNIEnv*, jclass) {
    return static_cast<jint>(lastEvent.exchange(0));
}
static bool editableParameter(int para) {
    switch (para) {
        case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8:
        case 9: case 10: case 11: case 12: case 18: case 19: case 20: case 21:
        case 25: case 26: case 27: return true;
        default: return false;
    }
}
JNIEXPORT jintArray JNICALL Java_com_magusview_android_CameraSdk_control(JNIEnv* env, jclass, jint para) {
    jint values[] = {1, 0, 0, 0, 0, invalidArgument};
    if (camera && editableParameter(para)) {
        ToupnamDevice device{};
        int hr = Toupnam_query_Device(camera, &device);
        if (hr >= 0) {
            const auto& r = device.range[para];
            values[0] = r.idisable; values[1] = r.imin; values[2] = r.imax; values[3] = r.idef;
            int value = 0;
            hr = Toupnam_get_Para(camera, para, &value);
            values[4] = value;
        }
        values[5] = hr;
    }
    jintArray result = env->NewIntArray(6);
    if (result) env->SetIntArrayRegion(result, 0, 6, values);
    return result;
}
JNIEXPORT jint JNICALL Java_com_magusview_android_CameraSdk_setControl(JNIEnv*, jclass, jint para, jint value) {
    if (!camera || !editableParameter(para)) return invalidArgument;
    ToupnamDevice device{};
    int result = Toupnam_query_Device(camera, &device);
    if (result < 0) return result;
    const auto& r = device.range[para];
    if (para == 27) {
        unsigned columns = static_cast<unsigned>(r.imax) >> 16;
        unsigned rows = static_cast<unsigned>(r.imax) & 0xffffu;
        if (r.idisable || columns == 0 || rows == 0 || value < 0
                || static_cast<uint64_t>(value) >= static_cast<uint64_t>(columns) * rows) return invalidArgument;
        return Toupnam_put_Para(camera, para, value);
    }
    if (r.idisable || value < r.imin || value > r.imax || (para == 26 && value > 2)
            || (para == 12 && value > 1)) return invalidArgument;
    return Toupnam_put_Para(camera, para, value);
}
JNIEXPORT jint JNICALL Java_com_magusview_android_CameraSdk_pullPreview(JNIEnv* env, jclass, jobject pixels, jint outWidth, jint outHeight) {
    if (!camera || !heldFrame.id || !heldFrame.buf || outWidth <= 0 || outHeight <= 0
            || outWidth > width || outHeight > height) return invalidArgument;
    const int64_t bytes = static_cast<int64_t>(outWidth) * outHeight * 4;
    auto* data = static_cast<uint8_t*>(env->GetDirectBufferAddress(pixels));
    if (!data || env->GetDirectBufferCapacity(pixels) < bytes) return invalidArgument;
    const size_t stride = (static_cast<size_t>(width) * 3 + 3) & ~size_t(3);
    // Sample pixel centers directly from BGR: avoid converting/copying a full 4K
    // RGBA image just to shrink it again on the phone's display.
    std::vector<int> columns(outWidth);
    for (int x = 0; x < outWidth; ++x) columns[x] = ((2LL * x + 1) * width / (2LL * outWidth)) * 3;
    for (int y = 0; y < outHeight; ++y) {
        const int sourceY = (2LL * y + 1) * height / (2LL * outHeight);
        const uint8_t* source = heldFrame.buf + static_cast<size_t>(sourceY) * stride;
        uint8_t* target = data + static_cast<size_t>(y) * outWidth * 4;
        for (int x = 0; x < outWidth; ++x) {
            const uint8_t* pixel = source + columns[x];
            target[x * 4] = pixel[2]; target[x * 4 + 1] = pixel[1];
            target[x * 4 + 2] = pixel[0]; target[x * 4 + 3] = 255;
        }
    }
    return 0;
}
JNIEXPORT jint JNICALL Java_com_magusview_android_CameraSdk_record(JNIEnv* env, jclass, jstring path) {
    if (!camera) return unexpected;
    if (!path) return Toupnam_Record(camera, nullptr);
    const char* value = env->GetStringUTFChars(path, nullptr);
    if (!value) return invalidArgument;
    int result = Toupnam_Record(camera, value);
    env->ReleaseStringUTFChars(path, value);
    return result;
}
}
