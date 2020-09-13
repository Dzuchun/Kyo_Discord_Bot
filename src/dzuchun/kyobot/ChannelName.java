package dzuchun.kyobot;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

public class ChannelName {

	public final String name;
	public final Long channelId;

	public ChannelName(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		this((String) stream.readObject(), stream.readLong());
	}

	public ChannelName(String nameIn, Long channelIdIn) {
		this.name = nameIn;
		this.channelId = channelIdIn;
	}

	public TextChannel getChannel(JDA jda) {
		return jda.getTextChannelById(this.channelId);
	}

	public void writeTo(ObjectOutputStream stream) throws IOException {
		stream.writeObject(this.name);
		stream.writeLong(this.channelId);
	}

	@Override
	public String toString() {
		TextChannel channel = this.getChannel(KyoBot.jda);
		return String.format("ChannelName[%s-[%s channel in %s]]", this.name, channel.getName(),
				channel.getGuild().getName());
	}
}
