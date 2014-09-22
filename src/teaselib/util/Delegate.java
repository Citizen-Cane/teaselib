package teaselib.util;


public abstract class Delegate extends Event<Object, EventArgs> {

	public abstract void run();
	
	@Override
	public void run(Object sender, EventArgs args) {
		run();
	}
}
