package CausalMulticast;

import java.io.Serializable;

public class Message implements Serializable {

    public static final int NORMAL_MSG   = 0;
    public static final int JOIN_MSG     = 1;
    public static final int JOIN_USR_MSG = 2;

    private VectorClock vectorClock;

    int type;
    String text;
    String origin;

    public Message() {
    }

    public Message(int type, String text, String origin) {
        this.type = type;
        this.text = text;
        this.origin = origin;
    }

    public int getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getOrigin() {
        return origin;
    }

    public VectorClock getVectorClock() {
        return vectorClock;
    }

    public void setVectorClock(VectorClock vectorClock) {
        this.vectorClock = new VectorClock();
        this.vectorClock.setVector(vectorClock.getVector());
    }

    @Override
    public String toString() {
        return "Message{" +
                ", text='" + text + '\'' +
                ", origin='" + origin + '\'' +
                ", vectorClock=" + vectorClock +
                '}';
    }
}
