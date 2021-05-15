// TeaseLibx360c.cpp : Defines the exported functions for the DLL application.
//

#include "stdafx.h"

#include "teaselib_core_devices_xinput_XInputDevice.h"


/*
XInput1_3.dll unnamed ordinals: https://gist.github.com/robindegen/9446175
Ordinal 100:
DWORD XInputGetStateEx(DWORD dwUserIndex, XINPUT_STATE *pState);
Ordinal 101:
DWORD XInputWaitForGuideButton(DWORD dwUserIndex, DWORD dwFlag, unKnown *pUnKnown);
Ordinal 102:
DWORD XInputCancelGuideButtonWait(DWORD dwUserIndex);
Ordinal 103:
DWORD XInputPowerOffController(DWORD dwUserIndex);
*/

HMODULE hXInputLib = NULL;
bool isInitialized = false;

typedef DWORD (WINAPI *XInputGetStateExProc)(DWORD dwUserIndex, XINPUT_STATE *pState);
LPCSTR XInputGetStateExOrdinal = (LPCSTR) 100;
XInputGetStateExProc XInputGetStateEx = NULL;

struct IUnknown;
typedef DWORD(WINAPI *XInputWaitForGuideButtonProc)(DWORD dwUserIndex, DWORD dwFlag, IUnknown **pUnk);
LPCSTR XInputWaitForGuideButtonOrdinal = (LPCSTR) 101;
XInputWaitForGuideButtonProc XInputWaitForGuideButton = NULL;

typedef DWORD(WINAPI *XInputCancelGuideButtonWaitProc)(DWORD dwUserIndex);
LPCSTR XInputCancelGuideButtonWaitOrdinal = (LPCSTR) 102;
XInputCancelGuideButtonWaitProc XInputCancelGuideButtonWaitEx = NULL;

typedef DWORD(WINAPI *XInputShutdownControllerProc)(DWORD dwUserIndex);
LPCSTR XInputShutdownControllerOrdinal = (LPCSTR) 103;
XInputShutdownControllerProc XInputShutdownController = NULL;

void initExtendedFunctions() {
	if (!isInitialized) {
		// Get module handle
		hXInputLib = LoadLibrary(L"xinput1_4.dll");
		if (hXInputLib == NULL) {
			// printf("Could not load xinput1_4.dll. Trying xinput1_3.dll...\n");
			hXInputLib = LoadLibrary(L"xinput1_3.dll");
		}
		if (hXInputLib == NULL) {
			return;
		}
		// Get ProcAdresses
		XInputGetStateEx = (XInputGetStateExProc)GetProcAddress(hXInputLib, XInputGetStateExOrdinal);
		XInputWaitForGuideButton = (XInputWaitForGuideButtonProc)GetProcAddress(hXInputLib, XInputWaitForGuideButtonOrdinal);
		XInputCancelGuideButtonWaitEx = (XInputCancelGuideButtonWaitProc)GetProcAddress(hXInputLib, XInputCancelGuideButtonWaitOrdinal);
		XInputShutdownController = (XInputShutdownControllerProc)GetProcAddress(hXInputLib, XInputShutdownControllerOrdinal);
	}
}


JNIEXPORT jint JNICALL Java_teaselib_core_devices_xinput_XInputDevice_pollDevice
(JNIEnv *env, jclass cls, jint dwUserIndex, jobject byteBuffer) {
    // the byte buffer must be allocatedDirect(16)'d in Java...
    void *bbuf = env->GetDirectBufferAddress(byteBuffer);
    // ... because we're going to write straight into it
    XINPUT_STATE *state = (XINPUT_STATE *)bbuf;
    ZeroMemory(state, sizeof(XINPUT_STATE));
	initExtendedFunctions();
	if (XInputGetStateEx) {
		return XInputGetStateEx(dwUserIndex, state);
	}
	else {
		return XInputGetState(dwUserIndex, state);
	}
}

JNIEXPORT jint JNICALL Java_teaselib_core_devices_xinput_XInputDevice_getBatteryInformation
(JNIEnv * env, jclass cls, jint dwUserIndex, jobject byteBuffer) {
	// the byte buffer must be allocatedDirect(2)'d in Java...
	void *bbuf = env->GetDirectBufferAddress(byteBuffer);
	// ... because we're going to write straight into it
	XINPUT_BATTERY_INFORMATION *batteryInfo = (XINPUT_BATTERY_INFORMATION *)bbuf;
	ZeroMemory(batteryInfo, sizeof(XINPUT_BATTERY_INFORMATION));
	return XInputGetBatteryInformation(dwUserIndex, BATTERY_DEVTYPE_GAMEPAD, batteryInfo);
}

JNIEXPORT jint JNICALL Java_teaselib_core_devices_xinput_XInputDevice_setVibration
(JNIEnv *env, jclass cls, jint dwUserIndex, jint leftMotor, jint rightMotor) {
    XINPUT_VIBRATION vib;
	vib.wLeftMotorSpeed = static_cast<WORD>(leftMotor);
    vib.wRightMotorSpeed = static_cast<WORD>(rightMotor);
    return XInputSetState(dwUserIndex, &vib);
}

JNIEXPORT jboolean JNICALL Java_teaselib_core_devices_xinput_XInputDevice_shutdownDevice
(JNIEnv *, jclass, jint dwUserIndex) {
	XINPUT_VIBRATION vib;
	vib.wLeftMotorSpeed = static_cast<WORD>(0);
	vib.wRightMotorSpeed = static_cast<WORD>(0);
	XInputSetState(dwUserIndex, &vib);
	initExtendedFunctions();
	if (XInputShutdownController) {
		XInputShutdownController(dwUserIndex);
		return true;
	}
	else {
		return false;
	}
}
