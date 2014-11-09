package teaselib;

public class Attitude
{
	public static String Prefix = "<attitude ";
	public static String Suffix = "/>";
	
	public static final boolean matches(String attitude)
	{
		return attitude.startsWith(Prefix) && attitude.endsWith(Suffix);
	}
	
	public static final String Neutral = "<attitude neutral/>";
	public static final String Reading = "<attitude reading/>";
}
