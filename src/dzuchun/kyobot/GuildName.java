package dzuchun.kyobot;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import dzuchun.util.Mapping;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public class GuildName extends Mapping<Long> {

	public Guild getGuild(JDA jda) {
		return jda.getGuildById(this.object);
	}

	@Override
	public String toString() {
		return String.format("GuildName[%s-%s]", this.name, this.object);
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
