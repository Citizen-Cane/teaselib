package teaselib.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class Message {

	private final Vector<String> message;

	public Message() {
		this.message = new Vector<String>();
	}

	/**
	 * @param message
	 *            The message to render, or null to display no message
	 */
	public Message(String message) {
		if (message != null) {
			this.message = new Vector<>(1);
			this.message.add(message);
		} else {
			this.message = new Vector<>();
		}
	}

	/**
	 * @param message
	 *            The message to render, or null or an empty vector to display
	 *            no message
	 */
	public Message(String ... message) {
		if (message == null) {
			this.message = new Vector<>();
		} else {
			this.message = new Vector<>();
			for(String s : message)
			{
				this.message.add(s);
			}
		}
	}

	/**
	 * @param message
	 *            The message to render, or null or an empty vector to display
	 *            no message
	 */
	public Message(Vector<String> message) {
		if (message == null) {
			this.message = new Vector<>();
		} else {
			this.message = message;
		}
	}

	public void add(String text) {
		if (text == null)
			throw new IllegalArgumentException();
		message.add(text);
	}

	public String toString() {
		if (message == null) {
			return "";
		} else if (message.isEmpty()) {
			return "";
		} else {
			final int s = message.size();
			StringBuilder format = new StringBuilder(message.elementAt(0));
			for (int i = 1; i < s; i++) {
				String previous = message.elementAt(i - 1);
				boolean newLine = previous.endsWith(",")
						|| previous.endsWith(".") || previous.endsWith("!")
						|| previous.endsWith("?");
				if (newLine) {
					format.append("\n\n");
				} else {
					format.append(" ");
				}
				String line = message.elementAt(i);
				format.append(line);
			}
			return format.toString();
		}
	}

	public List<String> getParagraphs() {
		List<String> paragraphs = new ArrayList<>();
		if (message != null) {
			if (!message.isEmpty()) {
				StringBuilder paragraph = null;
				for (Iterator<String> it = message.iterator(); it.hasNext();) {
					String line = it.next();
					if (paragraph == null) {
						paragraph = new StringBuilder();
					}
					paragraph.append(line);
					boolean ending = line.endsWith(",") || line.endsWith(".")
							|| line.endsWith("!") || line.endsWith("?");
					if (ending || !it.hasNext()) {
						paragraphs.add(paragraph.toString());
						paragraph = null;
					} else {
						paragraph.append(" ");
					}
				}
			}
		}
		return paragraphs;
	}

}
