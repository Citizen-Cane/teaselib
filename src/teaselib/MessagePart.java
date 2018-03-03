package teaselib;

import teaselib.Message.Type;

public class MessagePart {
    public final Type type;
    public final String value;

    public MessagePart(String text) {
        text = text.trim();
        Type type = Message.determineType(text);
        if (type == Type.Keyword) {
            text = Message.keywordFrom(text);
        } else if (type == Type.DesktopItem) {
            // The keyword is optional,
            // it's just needed to show a file on the desktop
            text = removeKeyword(Message.ShowOnDesktop, text);
        } else if (type == Type.Delay) {
            text = removeKeyword(Message.Delay, text);
        } else if (type == Type.BackgroundSound) {
            text = removeKeyword(Message.BackgroundSound, text);
        }
        this.type = type;
        this.value = text;
    }

    private static String removeKeyword(String keyword, String text) {
        if (text.toLowerCase().startsWith(keyword.toLowerCase())) {
            text = text.substring(keyword.length()).trim();
        }
        return text;
    }

    public MessagePart(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public boolean isFile() {
        return Message.Type.isFile(type);
    }

    @Override
    public String toString() {
        return type.name() + "=" + value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MessagePart other = (MessagePart) obj;
        if (type != other.type)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
}