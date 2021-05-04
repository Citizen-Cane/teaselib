#pragma once

#ifdef TEASELIBFRAMEWORK_EXPORTS
	#define TEASELIB_FRAMEWORK_EXPORT __declspec(dllexport)
#else
	#define TEASELIB_FRAMEWORK_EXPORT __declspec(dllimport) 
#endif
