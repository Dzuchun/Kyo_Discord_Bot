package dzuchun.kyobot;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import dzuchun.util.SavedObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class StreamingChannel implements SavedObject {

	private long channelId;

	public long getId() {
		return channelId;
	}

	private VoiceChannel channel;

	public VoiceChannel getChannel(JDA jda) {
		if (channel == null) {
			channel = jda.getVoiceChannelById(channelId);
		}
		return channel;
	}

	public StreamingChannel() {
	}

	public StreamingChannel(VoiceChannel channelIn) {
		this(channelIn.getIdLong());
		this.channel = channelIn;
	}

	public StreamingChannel(long idIn) {
		this.channelId = idIn;
	}

	@Override
	public void read(ObjectInputStream streamIn) throws IOException {
		channelId = streamIn.readLong();
	}

	@Override
	public void save(ObjectOutputStream streamIn) throws IOException {
		streamIn.writeLong(channelId);
	}

}
