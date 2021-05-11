#pragma once

#include <string>
#include <vector>

#ifdef TEASELIBFRAMEWORK_EXPORTS
	#define TEASELIB_FRAMEWORK_EXPORT __declspec(dllexport)
#else
	#define TEASELIB_FRAMEWORK_EXPORT __declspec(dllimport) 
#endif


namespace teaselib {

	namespace strings {

		std::string TEASELIB_FRAMEWORK_EXPORT utf8(const wchar_t* unicode);
		std::vector<std::string> TEASELIB_FRAMEWORK_EXPORT split(const std::string& s, char delimiter);
		std::string TEASELIB_FRAMEWORK_EXPORT join(std::vector<std::string>::const_iterator& start, std::vector<std::string>::const_iterator& end, const char delimiter);
	}
}
