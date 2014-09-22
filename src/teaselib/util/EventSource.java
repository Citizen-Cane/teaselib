package teaselib.util;

import java.util.ArrayList;
import java.util.List;

import teaselib.TeaseLib;

public class EventSource<S, T extends EventArgs> {
	private final List<Event<S, T>> delegates = new ArrayList<>();
	private final String name;
	
	public EventSource(String name)
	{
		this.name = name;
	}

	public synchronized void add(Event<S, T> delegate)
	{
		delegates.add(delegate);
	}
	
	public synchronized void remove(Event<S, T> delegate)
	{
		delegates.remove(delegate);
	}
	
	public synchronized void run(S sender, T eventArgs)
	{
		TeaseLib.log(getClass().getSimpleName() + " " + name + ", " + delegates.size() + " listeners " + eventArgs.toString() );
		for(Event<S, T> delegate : delegates)
		{
			try {
				delegate.run(sender, eventArgs);
			} catch (Throwable t) {
				TeaseLib.log(this, t);
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + name + "(" + hashCode() + ")";
	}
}
