#include "stdafx.h"

#include <sstream>
#include <vector>

using namespace std;

namespace teaselib {
    namespace strings{

        std::string utf8(const wchar_t* unicode)
        {
            if (*unicode == 0) return string();
            const size_t unicode_size = wcslen(unicode);
            const size_t utf8_size = WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, unicode, unicode_size, nullptr, 0, nullptr, nullptr);
            string utf8(utf8_size, 0);
            WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, unicode, unicode_size, &utf8[0], utf8_size, nullptr, nullptr);
            return utf8;
        }

        std::vector<std::string> split(const std::string & s, char delimiter)
        {
           std::vector<std::string> tokens;
           std::string token;
           std::istringstream tokenStream(s);
           while (std::getline(tokenStream, token, delimiter))
           {
              tokens.push_back(token);
           }
           return tokens;
        }

        std::string join(std::vector<std::string>::const_iterator& begin, std::vector<std::string>::const_iterator& end, const char delimiter)
        {
          string s;
           for (vector<string>::const_iterator p = begin;
                p != end; ++p) {
              s += *p;
              if (p != end - 1)
                s += delimiter;
           }
           return s;
        }

    }
}
