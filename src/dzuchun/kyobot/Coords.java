package dzuchun.kyobot;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dzuchun.util.ArrayHelper;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.requests.RestAction;

public class Coords {
	private static final Logger LOGGER = LoggerFactory.getILoggerFactory().getLogger("Coords");
	public static final String VALID_BEGIN = "https://discordapp.com/channels/";
	public static final int VALID_BEGIC_LENGTH = VALID_BEGIN.length();

	public final String link;
	public final Guild guild;
	public final MessageChannel channel;
	public final Message message;

	public Coords(String linkIn) {
		this.link = linkIn;
		LOGGER.debug("Link provided to constructor: {}", linkIn);
		if (!linkIn.startsWith(VALID_BEGIN)) {
			this.guild = null;
			this.channel = null;
			this.message = null;
			LOGGER.warn("Link provided to constructor is not valid, should start with \"{}\"", VALID_BEGIN);
		} else {
			List<String> coords = Arrays.asList(this.link.substring(VALID_BEGIN.length()).split("/"));
			boolean isPrivate = false;
			if ((coords.size() > 0) && coords.get(0).startsWith("@")) {
				isPrivate = true;
				coords.set(0, "-1");
			}
			List<Long> ids = null;
			try {
				ids = ArrayHelper.map(coords, Long::parseLong);
			} catch (NumberFormatException e) {
				LOGGER.warn("Link provided to constructor is invalid:");
				e.printStackTrace();
				this.guild = null;
				this.channel = null;
				this.message = null;
				return;
			}
			LOGGER.debug("Ids:{}", ArrayHelper.iterableToString(ids, l -> l.toString()));
			if ((ids.size() >= 1) && !isPrivate) {
				this.guild = KyoBot.jda.getGuildById(ids.get(0));
			} else {
				this.guild = null;
			}
			if (ids.size() >= 2) {
				this.channel = isPrivate ? KyoBot.jda.getPrivateChannelById(ids.get(1))
						: this.guild.getTextChannelById(ids.get(1));
			} else {
				this.channel = null;
			}
			if ((ids.size() >= 3) && (this.channel != null)) {
				RestAction<Message> rest = this.channel.retrieveMessageById(ids.get(2));
				Message dummy = null;
				try { // TODO optimize
					dummy = rest.complete();
				} catch (RuntimeException e) {
					LOGGER.error("Failed to retrieve message:");
					e.printStackTrace();
				}
				this.message = dummy;
			} else {
				this.message = null;
			}
		}
	}
}
