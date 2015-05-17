package teaselib.util.jni;

import teaselib.TeaseLib;

public abstract class NativeObject {

	private final long nativeObject;
	
	protected NativeObject(long nativeObject)
	{
		this.nativeObject = nativeObject;
	}
	
	protected native void disposeNativeObject();
	
	@Override
	protected void finalize() throws Throwable {
		try {
			disposeNativeObject();
		} catch (Throwable t) {
			TeaseLib.log(this, t);
		}
		super.finalize();
	}

}
