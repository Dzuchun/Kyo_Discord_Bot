package dzuchun.kyobot;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dzuchun.util.SavedObject;

public class DelayedMessage implements SavedObject {
	private static final Logger LOGGER = LoggerFactory.getILoggerFactory().getLogger("Del message");

	public DelayedMessage() {
	}

	public DelayedMessage(long publicationTimeIn, String channelNameIn, String messageIn, Type typeIn) {
		this.publicationTime = publicationTimeIn;
		this.channelName = channelNameIn;
		this.message = messageIn;
		this.type = typeIn;
		LOGGER.info("Creating delayed message to publicate at time {}", publicationTime);
	}

	private long publicationTime;

	public long getPublicationTime() {
		return publicationTime;
	}

	private String channelName;

	public String getChannelName() {
		return channelName;
	}

	private String message; // TODO change this, so it supports unsupported yet stuff

	public String getMessage() {
		return message;
	}

	private Type type;

	public Type getType() {
		return type;
	}

	@Override
	public void read(ObjectInputStream streamIn) throws IOException {
		this.publicationTime = streamIn.readLong();
		try {
			this.channelName = (String) streamIn.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			this.message = (String) streamIn.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			LOGGER.error("This may signalise, that you don't have JDA library properly installed.");
		}
		this.type = streamIn.readInt() == 0 ? Type.DELAYED : Type.SCHEDULED;
	}

	@Override
	public void save(ObjectOutputStream streamIn) throws IOException {
		streamIn.writeLong(publicationTime);
		streamIn.writeObject(channelName);
		streamIn.writeObject(message);
		streamIn.writeInt(type == Type.DELAYED ? 0 : 1);
	}

	@Override
	public String toString() {
		return String.format("DelayedMessage[time=%s(in %ss), type=%s, text=\"%s\", channel=%s]", publicationTime,
				(publicationTime - System.currentTimeMillis()) / 1000, type, message, channelName);
	}

	public enum Type {
		SCHEDULED, DELAYED
	}

}
