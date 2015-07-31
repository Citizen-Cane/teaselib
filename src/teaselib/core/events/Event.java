package teaselib.core.events;

public abstract class Event<S, E extends EventArgs>
{
	private Throwable error = null;
	
	protected void setError(Throwable t)
	{
		this.error = t;
	}
	
	public Throwable getError()
	{
		return error;
	}

	public abstract void run(S sender, E eventArgs);
}
