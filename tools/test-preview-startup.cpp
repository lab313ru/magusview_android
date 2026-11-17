// Host regression test of the actual JNI bridge using a deterministic fake camera SDK.
// No camera or Android device is needed.
#define NOMINMAX
#define TOUPNAM_NOIMPORTS
#include <cassert>
#include <iostream>
#include "../app/src/main/jni/camera.cpp"

static bool started = false, ready = false;
static unsigned returned = 0;
static std::vector<unsigned char> frameBytes(640 * 480 * 4, 42);
static ToupnamImagePtr nextFrame{1, 640, 480, static_cast<unsigned>(frameBytes.size()), frameBytes.data()};
static std::vector<unsigned char> destination(frameBytes.size());
static jlong capacity = 0;
static unsigned configuredFlags = 0;
static unsigned controlWrites = 0;

extern "C" {
const char* __stdcall Toupnam_Version() { return "fake"; }
void __stdcall Toupnam_PriFlag(unsigned flags, unsigned) { configuredFlags = flags; }
void __stdcall Toupnam_Init(PTOUPNAM_EVENT_CALLBACK, void*) {}
unsigned __stdcall Toupnam_Enum(ToupnamDevice[], unsigned) { return 0; }
HToupnam __stdcall Toupnam_Open(const char*) { return reinterpret_cast<HToupnam>(1); }
void __stdcall Toupnam_Close(HToupnam) { assert(heldFrame.id == 0); }
HRESULT __stdcall Toupnam_put_Para(HToupnam, unsigned, int) { ++controlWrites; return 0; }
HRESULT __stdcall Toupnam_get_Para(HToupnam, unsigned, int* value) { *value = 50; return 0; }
HRESULT __stdcall Toupnam_query_Device(HToupnam, ToupnamDevice* device) {
    *device = {};
    for (auto& range : device->range) { range.imin = 0; range.imax = 100; range.idef = 50; }
    device->range[4].idisable = 1;
    device->range[27].imax = (7 << 16) | 7;
    return 0;
}
HRESULT __stdcall Toupnam_Record(HToupnam, const char*) { return 0; }
HRESULT __stdcall Toupnam_StartPullModeWithCallback(HToupnam, void*) { started = true; return 0; }
HRESULT __stdcall Toupnam_PullImagePtr(HToupnam, ToupnamImagePtr* frame) {
    assert(started);
    if (!ready) return static_cast<int>(0x8000000au); // E_PENDING before decoder output
    *frame = nextFrame;
    return 0;
}
HRESULT __stdcall Toupnam_PushImagePtr(HToupnam, unsigned id) {
    assert(id == nextFrame.id);
    ++returned;
    return 0;
}
}

int main() {
    Java_com_magusview_android_CameraSdk_enableWifi(nullptr, nullptr);
    assert((configuredFlags & TOUPNAM_PRIFLAG_BROADCAST) != 0);
    assert((configuredFlags & TOUPNAM_PRIFLAG_RTPOVERRTSP) == 0);
    camera = reinterpret_cast<HToupnam>(1);
    assert(Java_com_magusview_android_CameraSdk_setControl(nullptr, nullptr, 26, 2) == 0);
    assert(controlWrites == 1);
    assert(Java_com_magusview_android_CameraSdk_setControl(nullptr, nullptr, 26, 3) == invalidArgument); // No calibration.
    assert(Java_com_magusview_android_CameraSdk_setControl(nullptr, nullptr, 23, 1) == invalidArgument); // No network changes.
    assert(Java_com_magusview_android_CameraSdk_setControl(nullptr, nullptr, 4, 50) == invalidArgument); // Unsupported.
    assert(Java_com_magusview_android_CameraSdk_setControl(nullptr, nullptr, 1, 101) == invalidArgument);
    assert(controlWrites == 1); // Rejected values never reach the SDK.
    assert(Java_com_magusview_android_CameraSdk_setControl(nullptr, nullptr, 27, 48) == 0);
    assert(Java_com_magusview_android_CameraSdk_setControl(nullptr, nullptr, 27, 49) == invalidArgument);
    assert(controlWrites == 2); // Grid dimensions are not a valid linear zone maximum.
    assert(Java_com_magusview_android_CameraSdk_setControl(nullptr, nullptr, 41, 75) == invalidArgument);
    assert(Java_com_magusview_android_CameraSdk_setControl(nullptr, nullptr, 41, 101) == invalidArgument);
    assert(controlWrites == 2); // Preview-only zoom never writes camera parameters.
    // Reproduce startup with unavailable initial dimensions; stream starts anyway.
    assert(width == 0 && height == 0);
    assert(Java_com_magusview_android_CameraSdk_start(nullptr, nullptr) == 0);
    assert(Java_com_magusview_android_CameraSdk_acquireFrame(nullptr, nullptr) < 0);
    assert(returned == 0);
    ready = true;
    assert(Java_com_magusview_android_CameraSdk_acquireFrame(nullptr, nullptr) == 0);
    assert(width == 640 && height == 480 && returned == 0);

    JNINativeInterface_ functions{};
    functions.GetDirectBufferAddress = [](JNIEnv*, jobject) -> void* { return destination.data(); };
    functions.GetDirectBufferCapacity = [](JNIEnv*, jobject) -> jlong { return capacity; };
    JNIEnv env{&functions};
    capacity = destination.size() - 1;
    assert(Java_com_magusview_android_CameraSdk_pull(&env, nullptr, nullptr) == invalidArgument);
    assert(destination[0] == 0); // Undersized destination was not touched.
    capacity = destination.size();
    assert(Java_com_magusview_android_CameraSdk_pull(&env, nullptr, nullptr) == 0);
    assert(destination[0] == 42 && destination[3] == 255);
    Java_com_magusview_android_CameraSdk_releaseFrame(nullptr, nullptr);
    Java_com_magusview_android_CameraSdk_releaseFrame(nullptr, nullptr);
    assert(returned == 1); // Exactly one return, even when cleanup is repeated.

    nextFrame.width = 320; nextFrame.height = 240;
    assert(Java_com_magusview_android_CameraSdk_acquireFrame(nullptr, nullptr) == 0);
    assert(width == 320 && height == 240); // Adapt to a resolution change.
    Java_com_magusview_android_CameraSdk_releaseFrame(nullptr, nullptr);
    assert(returned == 2);

    // Two 1-pixel rows, each padded from 3 to 4 bytes. Verify channel order and stride.
    frameBytes[0] = 10; frameBytes[1] = 20; frameBytes[2] = 30; frameBytes[3] = 99;
    frameBytes[4] = 40; frameBytes[5] = 50; frameBytes[6] = 60; frameBytes[7] = 99;
    nextFrame.width = 1; nextFrame.height = 2; nextFrame.length = 8;
    assert(Java_com_magusview_android_CameraSdk_acquireFrame(nullptr, nullptr) == 0);
    assert(Java_com_magusview_android_CameraSdk_pull(&env, nullptr, nullptr) == 0);
    assert(destination[0] == 30 && destination[1] == 20 && destination[2] == 10 && destination[3] == 255);
    assert(destination[4] == 60 && destination[5] == 50 && destination[6] == 40 && destination[7] == 255);
    // Preview downsampling uses pixel centers and respects padded BGR rows.
    capacity = 3;
    assert(Java_com_magusview_android_CameraSdk_pullPreview(&env, nullptr, nullptr, 1, 1) == invalidArgument);
    assert(destination[0] == 30); // A rejected copy must not write any bytes.
    capacity = destination.size();
    assert(Java_com_magusview_android_CameraSdk_pullPreview(&env, nullptr, nullptr, 0, 1) == invalidArgument);
    assert(Java_com_magusview_android_CameraSdk_pullPreview(&env, nullptr, nullptr, 2, 1) == invalidArgument);
    assert(Java_com_magusview_android_CameraSdk_pullPreview(&env, nullptr, nullptr, 1, 1) == 0);
    assert(destination[0] == 60 && destination[1] == 50 && destination[2] == 40 && destination[3] == 255);
    // Full-resolution photo remains available from the same held frame.
    assert(Java_com_magusview_android_CameraSdk_pull(&env, nullptr, nullptr) == 0);
    assert(destination[0] == 30 && destination[4] == 60);
    Java_com_magusview_android_CameraSdk_releaseFrame(nullptr, nullptr);
    assert(returned == 3);

    nextFrame.length = 0; // Actual CHD50 decoded-frame metadata: length is unspecified.
    assert(Java_com_magusview_android_CameraSdk_acquireFrame(nullptr, nullptr) == 0);
    assert(Java_com_magusview_android_CameraSdk_pull(&env, nullptr, nullptr) == 0);
    Java_com_magusview_android_CameraSdk_releaseFrame(nullptr, nullptr);
    assert(returned == 4);

    nextFrame.length = 1;
    assert(Java_com_magusview_android_CameraSdk_acquireFrame(nullptr, nullptr) == invalidArgument);
    assert(returned == 5 && heldFrame.id == 0); // Malformed frames are returned too.
    nextFrame.length = frameBytes.size();
    nextFrame.width = 0xffffffffu; nextFrame.height = 0xffffffffu;
    assert(Java_com_magusview_android_CameraSdk_acquireFrame(nullptr, nullptr) == invalidArgument);
    assert(returned == 6);
    nextFrame.width = 640; nextFrame.height = 480;
    assert(Java_com_magusview_android_CameraSdk_acquireFrame(nullptr, nullptr) == 0);
    Java_com_magusview_android_CameraSdk_close(nullptr, nullptr);
    assert(returned == 7 && camera == nullptr && width == 0 && height == 0);
    std::cout << "PASS: default transport, delayed frame, BGR channels, padded rows, unspecified length, bounds, resize, cleanup\n";
}
