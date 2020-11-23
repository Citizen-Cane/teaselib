#pragma once

#include <algorithm>
#include <map>

#include <jni.h>

#include "JNIObject.h"

template<typename T> class JNIObjectSibling : public JNIObject<jobject>
{
public:
	JNIObjectSibling(JNIEnv *env, jobject jthis)
		: JNIObject(env, jthis)
	{
		add(jthis, static_cast<T*>(this));
	}

	virtual ~JNIObjectSibling()
	{
		dispose(jthis);
	}

	static T* get(jobject jthis)
	{
		return objects[jthis];
	}

	static void disposeAll()
	{
		std::for_each(objects.begin(), objects.end(), [](typename ObjectMap::value_type& object)
		{
			delete object->second;
		});
	}
protected:
	typedef std::map<jobject, T*> ObjectMap;
	static ObjectMap objects;

	bool add(jobject object, T *t)
	{
		const bool newObject = dispose(object);
		objects[object] = t;
		return  newObject;
	}

	bool dispose(jobject object)
	{
		auto o = objects.find(object);
		const bool newObject = o == objects.end();
		if (!newObject)
		{
			objects.erase(o);
		}
		return newObject;
	}

	bool empty()
	{
		return objects.empty();
	}
};
