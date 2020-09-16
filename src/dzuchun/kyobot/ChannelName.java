package dzuchun.kyobot;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import dzuchun.util.Mapping;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

public class ChannelName extends Mapping<Long> {

	public ChannelName() {
	}

	public ChannelName(String nameIn, Long id) {
		super(nameIn);
		this.object = id;
	}

	public TextChannel getChannel(JDA jda) {
		return jda.getTextChannelById(this.object);
	}

	@Override
	public String toString() {
		TextChannel channel = this.getChannel(KyoBot.jda);
		return String.format("ChannelName[%s-[%s channel in %s]]", this.name, channel.getName(),
				channel.getGuild().getName());
	}

	@Override
	public void read(ObjectInputStream streamIn) throws IOException {
		super.read(streamIn);
		this.object = streamIn.readLong();
	}

	@Override
	public void save(ObjectOutputStream streamIn) throws IOException {
		super.save(streamIn);
		streamIn.writeLong(this.object);
	}
}
