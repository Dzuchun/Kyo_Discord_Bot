package dzuchun.kyobot;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

public class GuildName {
	public final String name;
	public final Long guildId;

	public GuildName(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		this((String) stream.readObject(), stream.readLong());
	}

	public GuildName(String nameIn, Long guildIdIn) {
		this.name = nameIn;
		this.guildId = guildIdIn;
	}

	public TextChannel getChannel(JDA jda) {
		return jda.getTextChannelById(this.guildId);
	}

	public void writeTo(ObjectOutputStream stream) throws IOException {
		stream.writeObject(this.name);
		stream.writeLong(this.guildId);
	}

	@Override
	public String toString() {
		return String.format("GuildName[%s-%s]", this.name, this.guildId);
	}
}
