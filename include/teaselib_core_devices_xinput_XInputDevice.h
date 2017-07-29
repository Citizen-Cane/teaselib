/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class teaselib_core_devices_xinput_XInputDevice */

#ifndef _Included_teaselib_core_devices_xinput_XInputDevice
#define _Included_teaselib_core_devices_xinput_XInputDevice
#ifdef __cplusplus
extern "C" {
#endif
#undef teaselib_core_devices_xinput_XInputDevice_MAX_PLAYERS
#define teaselib_core_devices_xinput_XInputDevice_MAX_PLAYERS 4L
#undef teaselib_core_devices_xinput_XInputDevice_VIBRATION_MIN_VALUE
#define teaselib_core_devices_xinput_XInputDevice_VIBRATION_MIN_VALUE 0L
#undef teaselib_core_devices_xinput_XInputDevice_VIBRATION_MAX_VALUE
#define teaselib_core_devices_xinput_XInputDevice_VIBRATION_MAX_VALUE 65535L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_DPAD_UP
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_DPAD_UP 1L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_DPAD_DOWN
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_DPAD_DOWN 2L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_DPAD_LEFT
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_DPAD_LEFT 4L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_DPAD_RIGHT
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_DPAD_RIGHT 8L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_START
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_START 16L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_BACK
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_BACK 32L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_LEFT_THUMB
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_LEFT_THUMB 64L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_RIGHT_THUMB
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_RIGHT_THUMB 128L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_LEFT_SHOULDER
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_LEFT_SHOULDER 256L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_RIGHT_SHOULDER
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_RIGHT_SHOULDER 512L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_GUIDE
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_GUIDE 1024L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_A
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_A 4096L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_B
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_B 8192L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_X
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_X 16384L
#undef teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_Y
#define teaselib_core_devices_xinput_XInputDevice_XINPUT_GAMEPAD_Y -32768L
#undef teaselib_core_devices_xinput_XInputDevice_ERROR_SUCCESS
#define teaselib_core_devices_xinput_XInputDevice_ERROR_SUCCESS 0L
#undef teaselib_core_devices_xinput_XInputDevice_ERROR_DEVICE_NOT_CONNECTED
#define teaselib_core_devices_xinput_XInputDevice_ERROR_DEVICE_NOT_CONNECTED 1167L
#undef teaselib_core_devices_xinput_XInputDevice_BATTERY_TYPE_DISCONNECTED
#define teaselib_core_devices_xinput_XInputDevice_BATTERY_TYPE_DISCONNECTED 0L
#undef teaselib_core_devices_xinput_XInputDevice_BATTERY_TYPE_WIRED
#define teaselib_core_devices_xinput_XInputDevice_BATTERY_TYPE_WIRED 1L
#undef teaselib_core_devices_xinput_XInputDevice_BATTERY_TYPE_ALKALINE
#define teaselib_core_devices_xinput_XInputDevice_BATTERY_TYPE_ALKALINE 2L
#undef teaselib_core_devices_xinput_XInputDevice_BATTERY_TYPE_NIMH
#define teaselib_core_devices_xinput_XInputDevice_BATTERY_TYPE_NIMH 3L
#undef teaselib_core_devices_xinput_XInputDevice_BATTERY_TYPE_UNKNOWN
#define teaselib_core_devices_xinput_XInputDevice_BATTERY_TYPE_UNKNOWN 255L
#undef teaselib_core_devices_xinput_XInputDevice_BATTERY_LEVEL_EMPTY
#define teaselib_core_devices_xinput_XInputDevice_BATTERY_LEVEL_EMPTY 0L
#undef teaselib_core_devices_xinput_XInputDevice_BATTERY_LEVEL_LOW
#define teaselib_core_devices_xinput_XInputDevice_BATTERY_LEVEL_LOW 1L
#undef teaselib_core_devices_xinput_XInputDevice_BATTERY_LEVEL_MEDIUM
#define teaselib_core_devices_xinput_XInputDevice_BATTERY_LEVEL_MEDIUM 2L
#undef teaselib_core_devices_xinput_XInputDevice_BATTERY_LEVEL_FULL
#define teaselib_core_devices_xinput_XInputDevice_BATTERY_LEVEL_FULL 3L
/*
 * Class:     teaselib_core_devices_xinput_XInputDevice
 * Method:    pollDevice
 * Signature: (ILjava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_teaselib_core_devices_xinput_XInputDevice_pollDevice
  (JNIEnv *, jclass, jint, jobject);

/*
 * Class:     teaselib_core_devices_xinput_XInputDevice
 * Method:    getBatteryInformation
 * Signature: (ILjava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_teaselib_core_devices_xinput_XInputDevice_getBatteryInformation
  (JNIEnv *, jclass, jint, jobject);

/*
 * Class:     teaselib_core_devices_xinput_XInputDevice
 * Method:    getCapabilities
 * Signature: (ILjava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_teaselib_core_devices_xinput_XInputDevice_getCapabilities
  (JNIEnv *, jclass, jint, jobject);

/*
 * Class:     teaselib_core_devices_xinput_XInputDevice
 * Method:    setVibration
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_teaselib_core_devices_xinput_XInputDevice_setVibration
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     teaselib_core_devices_xinput_XInputDevice
 * Method:    shutdownDevice
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_teaselib_core_devices_xinput_XInputDevice_shutdownDevice
  (JNIEnv *, jclass, jint);

#ifdef __cplusplus
}
#endif
#endif
