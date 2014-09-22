package teaselib.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class Message {

	private final Vector<String> message;

	public Message()
	{
		this.message = new Vector<String>();
	}

	public Message(String message)
	{
		this.message = new Vector<>(1);
		this.message.add(message);
	}

	public Message(Vector<String> message)
	{
		this.message = message;
	}
	
	public void add(String text)
	{
		if (text == null) throw new IllegalArgumentException();
		message.add(text);
	}

	public String toString()
	{
		final int s = message.size();
		if (s == 0)
		{
			return "";
		}
		else
		{
			StringBuilder format = new StringBuilder(message.elementAt(0));
			for(int i = 1; i < s; i++)
			{
				String last = message.elementAt(i - 1);
				boolean newLine =
						last.endsWith(",") ||
						last.endsWith(".") ||
						last.endsWith("!") ||
						last.endsWith("?");
				if (newLine)
				{
					format.append("\n\n");
				}
				else
				{
					format.append(" ");
				}
				String line = message.elementAt(i);
				format.append(line);
			}
			return format.toString();
		}
	}

	public List<String> getParagraphs()
	{
		List<String> paragraphs = new ArrayList<>();
		StringBuilder paragraph = null;
		for(Iterator<String> it = message.iterator(); it.hasNext() ; )
		{
			String line = it.next();
			if (paragraph == null)
			{
				paragraph = new StringBuilder(); 
			}
			paragraph.append(line);
			boolean ending =
					line.endsWith(",") ||
					line.endsWith(".") ||
					line.endsWith("!") ||
					line.endsWith("?");
			if (ending || !it.hasNext())
			{
				paragraphs.add(paragraph.toString());
				paragraph = null;
			}
			else
			{
				paragraph.append(" ");
			}
		}
		return paragraphs;
	}
	
}
