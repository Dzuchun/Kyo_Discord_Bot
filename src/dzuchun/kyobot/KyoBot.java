package dzuchun.kyobot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import dzuchun.util.ArrayHelper;
import dzuchun.util.SavedArrayList;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class KyoBot extends ListenerAdapter {

	private static final Logger LOGGER = LoggerFactory.getILoggerFactory().getLogger("Kyo");

	static JDA jda = null;

	private static final SavedArrayList<Long> ADMINISTRATORS = new SavedArrayList<Long>(new File("administrators.txt"),
			s -> {
				try {
					return s.readLong();
				} catch (IOException e1) {
					e1.printStackTrace();
					return -1l;
				}
			}, (s, l) -> {
				try {
					s.writeLong(l);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

	public static List<Long> getAdmins() {
		return ImmutableList.copyOf(ADMINISTRATORS);
	}

	private static String HELP_STRING = "";

	private static final SavedArrayList<GuildName> GUILD_NAMES = new SavedArrayList<GuildName>(
			new File("guild_names.txt"), s -> {
				try {
					return new GuildName(s);
				} catch (ClassNotFoundException | IOException e1) {
					e1.printStackTrace();
					return null;
				}
			}, (s, n) -> {
				try {
					n.writeTo(s);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

	private static final SavedArrayList<ChannelName> CHANNEL_NAMES = new SavedArrayList<ChannelName>(
			new File("channel_names.txt"), s -> {
				try {
					return new ChannelName(s);
				} catch (ClassNotFoundException | IOException e1) {
					e1.printStackTrace();
					return null;
				}
			}, (s, n) -> {
				try {
					n.writeTo(s);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

	public static void main(String[] args) throws InterruptedException, IOException {
		if (args.length < 1) {
			LOGGER.error("You must specify path to file with API token");
			System.exit(-1);
		}
		Scanner sc = new Scanner(System.in);
		try {
			Scanner tokenSearcher = new Scanner(new File(args[0]));
			final String TOKEN = tokenSearcher.nextLine();
			tokenSearcher.close();
			jda = JDABuilder.createDefault(TOKEN).addEventListeners(new KyoBot()).setActivity(Activity.listening("HTT"))
					.build();
			jda.awaitReady();
			ADMINISTRATORS.load();
			if (ADMINISTRATORS.isEmpty()) {
				LOGGER.warn("No administrator specified, please specify one if you want:");
				try {
					ADMINISTRATORS.add(sc.nextLong());
				} catch (InputMismatchException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
			LOGGER.info("Loaded administators:\n{}", ADMINISTRATORS.toString());
			loadHelpString();
			GUILD_NAMES.load();
			LOGGER.info("Loaded guild names:\n{}", GUILD_NAMES.toString());
			CHANNEL_NAMES.load();
			LOGGER.info("Loaded channel names:\n{}", CHANNEL_NAMES.toString());
		} catch (FileNotFoundException e) {
			LOGGER.error("No file at {}", args[0]);
			System.exit(-1);
		} catch (LoginException e) {
			LOGGER.error("Failed to log in: ");
			e.printStackTrace();
			System.exit(-1);
		}
		LOGGER.info("Kyo logged in!");
		sc.close();
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		super.onMessageReceived(event);
		if ((event.getChannelType() == ChannelType.PRIVATE) && !jda.getPrivateChannels().contains(event.getChannel())) {
			LOGGER.error("Hurray, recieved message in non-existent private channel");
		}
		User author = event.getAuthor();
		if ((event.getMessage().getContentDisplay().charAt(0) != '!')) {
			return;
		}
		LOGGER.debug("Recieved message:\n{}, in channel {}, type {}", event.getMessage(), event.getChannel(),
				event.getChannelType());
		if (author.isBot()) {
			LOGGER.debug("Message is from bot, ignoring...");
			return;
		}
		Long authorId = author.getIdLong();
		if (ADMINISTRATORS.contains(authorId)) {
			LOGGER.debug("Author is OP, executing...");
			String text = event.getMessage().getContentDisplay();
			String[] words = text.split(" ");
			if (words.length < 1) {
				return;
			}
			switch (words[0]) {
			case "!aOp":
				addOps(event.getMessage());
				break;
			case "!rOp":
				removeOps(event.getMessage());
				break;
			case "!aChN":
				if (words.length < 3) {
					this.displayHelp(author);
				} else {
					this.addChannelName(words[1], words[2]);
				}
				break;
			case "!rChN":
				if (words.length < 2) {
					this.displayHelp(author);
				} else {
					this.removeChannelName(words[1]);
				}
				break;
			case "!dChN":
				author.openPrivateChannel()
						.flatMap(channel -> channel.sendMessageFormat("All channel names:\n%s",
								ArrayHelper.iterableToString(CHANNEL_NAMES, name -> name.toString().concat("\n"))))
						.complete();
				break;
			case "!sDel":
				if (words.length < 4) {
					// TODO display syntax
					this.displayHelp(author);
				} else {
					this.sendDelayed(words[1], words[2], words[3], author);
				}
				break;
			case "!sSch":
				if (words.length < 4) {
					// TODO display syntax
					this.displayHelp(author);
				} else {
					this.sendScheduled(words[1], words[2], words[3], author);
				}
				break;
			case "!nMes":
				Message msg = event.getMessage();
				author.openPrivateChannel()
						.flatMap(channel -> channel
								.sendMessage(String.format("Message \"%s\" in chat \"%s\", from server \"%s\"",
										msg.getContentDisplay(), msg.getChannel().getName(), msg.getGuild().getName())))
						.complete();
				break;
			case "!intL":
				if (words.length < 2) {
					return;
				}
				String link = words[1];
				Coords coords = new Coords(link);
				if ((coords.guild == null) || (coords.channel == null) || (coords.message == null)) {
					author.openPrivateChannel().flatMap(channel -> channel.sendMessage("Invalid link")).complete();
				} else {
					author.openPrivateChannel()
							.flatMap(channel -> channel
									.sendMessage(String.format("Message \"%s\" in chat \"%s\", from server \"%s\"",
											coords.message.getContentDisplay(), coords.channel.getName(),
											coords.guild.getName())))
							.complete();
				}
				break;
			case "!stop":
				LOGGER.warn("Admin {}({}) asked me to stop. Stopping...", author.getName(), authorId);
				author.openPrivateChannel().flatMap(channel -> channel.sendMessage("K, shutting down...")).complete();
				stop();
				break;
			case "!help":
			default:
				LOGGER.debug("Displaying help to {}", authorId);
				this.displayHelp(author);
			}
		} else {
			LOGGER.debug("Author is not an OP, ignoring...");
			return;
		}
	}

	private void sendScheduled(String channelNameIn, String messageLinkIn, String dateIn, User authorIn) {
		Date date = null;
		try {
			date = new SimpleDateFormat("yyyy/MM/dd_hh:mm:ss").parse(dateIn);
		} catch (ParseException e) {
			LOGGER.warn("Specified date is invalid: {}, format - yyyy/MM/dd_hh:mm:ss", dateIn);
			return;
		}
		if (!CHANNEL_NAMES.anyMatches(n -> n.name.equals(channelNameIn))) {
			LOGGER.warn("Specified channel name does not exist: {}", channelNameIn);
			return;
		}
		TextChannel channel = CHANNEL_NAMES.getMatches(n -> n.name.equals(channelNameIn)).getChannel(jda);
		Coords coords = new Coords(messageLinkIn);
		if (coords.message == null) {
			LOGGER.warn("Invalid message link");
			return;
		}
		LOGGER.debug("Sending scheduled message {} to channel {} at date {}", coords.message, channel, date.toString());
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("Scheduled message from %s:\n", authorIn.getAsMention()));
		builder.append(coords.message.getContentDisplay());
		channel.sendMessage(builder.toString()).submitAfter(date.getTime() - System.currentTimeMillis(),
				TimeUnit.MILLISECONDS);
	}

	private void sendDelayed(String channelNameIn, String messageLinkIn, String delayIn, User authorIn) {
		long delay = -1;
		try {
			delay = Long.parseLong(delayIn);
		} catch (NumberFormatException e) {
			LOGGER.warn("Specified delay is not a number: {}", delayIn);
			return;
		}
		if (!CHANNEL_NAMES.anyMatches(n -> n.name.equals(channelNameIn))) {
			LOGGER.warn("Specified channel name does not exist: {}", channelNameIn);
			return;
		}
		TextChannel channel = CHANNEL_NAMES.getMatches(n -> n.name.equals(channelNameIn)).getChannel(jda);
		Coords coords = new Coords(messageLinkIn);
		if (coords.message == null) {
			LOGGER.warn("Invalid message link");
			return;
		}
		LOGGER.debug("Sending delayed message {} to channel {} with delay {}", coords.message, channel, delay);
		StringBuilder messageBuilder = new StringBuilder()
				.append(String.format("Delayed message from %s:\n", authorIn.getAsMention()));
		messageBuilder.append(coords.message.getContentDisplay());
		channel.sendMessage(messageBuilder.toString()).submitAfter(delay, TimeUnit.SECONDS);
	}

	private void removeChannelName(String nameIn) {
		CHANNEL_NAMES.removeIf(name -> name.name.equals(nameIn));
		LOGGER.info("Removed channels with name {}", nameIn);
	}

	private void addChannelName(String linkIn, String nameIn) {
		Coords coords = new Coords(linkIn);
		if (coords.channel == null) {
			LOGGER.warn("Link is invalid: {}", linkIn);
		} else {
			CHANNEL_NAMES.add(new ChannelName(nameIn, coords.channel.getIdLong()));
			LOGGER.info("Added channel {} to names as {}", coords.channel, nameIn);
		}
	}

	private static void addOps(Message message) {
		message.getMentionedUsers().forEach(user -> ADMINISTRATORS.add(user.getIdLong()));
		LOGGER.debug("Added ops {}", message.getMentionedUsers());
	}

	private static void removeOps(Message message) {
		ADMINISTRATORS.removeIf(id -> ArrayHelper.map(message.getMentionedUsers(), User::getIdLong).contains(id));
		LOGGER.debug("Removed ops {}", message.getMentionedUsers());
	}

	private static void stop() {
		jda.shutdownNow();
		try {
			ADMINISTRATORS.save();
		} catch (IOException e) {
			LOGGER.error("Failed to save admins:");
			e.printStackTrace();
		}
		try {
			GUILD_NAMES.save();
		} catch (IOException e) {
			LOGGER.error("Failed to save guild names:");
			e.printStackTrace();
		}
		try {
			CHANNEL_NAMES.save();
		} catch (IOException e) {
			LOGGER.error("Failed to save channel names:");
			e.printStackTrace();
		}
		LOGGER.info("Kyo wanna sleep...");
	}

	private static void loadHelpString() throws FileNotFoundException {
		File helpStringFile = new File("help.txt");
		if (!helpStringFile.exists()) {
			LOGGER.debug("\"help.txt\" file does not exist");
		} else {
			Scanner helpSearcher = new Scanner(helpStringFile);
			HELP_STRING = "";
			while (helpSearcher.hasNext()) {
				HELP_STRING += helpSearcher.nextLine() + "\n";
			}
			helpSearcher.close();
		}
	}

	private void displayHelp(User author) {
		if (HELP_STRING.isEmpty()) {
			LOGGER.debug("Help string is empty, can't display help");
			return;
		}
		author.openPrivateChannel().flatMap(channel -> channel.sendMessage(HELP_STRING)).queue();
	}
}
