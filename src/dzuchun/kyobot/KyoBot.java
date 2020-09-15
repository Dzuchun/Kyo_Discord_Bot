package dzuchun.kyobot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import dzuchun.util.ArrayHelper;
import dzuchun.util.SavedArrayList;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.MessageBuilder;
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
		if (args.length < 1 || (args.length < 2 && args[0].equals("_"))) {
			LOGGER.error("You must specify path to file with API token, or \"_\" and specify it as second argument");
			System.exit(-1);
		}
		try {
			String TOKEN = null;
			if (!args[0].equals("_")) {
				Scanner tokenSearcher = new Scanner(new File(args[0]));
				TOKEN = tokenSearcher.nextLine();
				tokenSearcher.close();
			} else {
				LOGGER.info("Specify API token: ");
				TOKEN = args[1];
			}
			jda = JDABuilder.createDefault(TOKEN).addEventListeners(new KyoBot()).setActivity(Activity.listening("HTT"))
					.build();
			jda.awaitReady();
			ADMINISTRATORS.load();
			if (ADMINISTRATORS.isEmpty()) {
				LOGGER.warn("No administrator specified");
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
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		super.onMessageReceived(event);
		if ((event.getChannelType() == ChannelType.PRIVATE) && !jda.getPrivateChannels().contains(event.getChannel())) {
			LOGGER.error("Hurray, recieved message in non-existent private channel");
		}
		Message message = event.getMessage();
		String content = message.getContentDisplay();
		if ((content.charAt(0) != '!') || content.length() < 2) {
			return;
		}

		BotCommand command = BotCommand.get(content.split(" ")[0].substring(1));

		command = command == null ? BotCommand.help : command;

		if (command.shouldIgnore(message)) {
			LOGGER.info("Ignoring");
			return;
		}

		Message response = command.process(message);
		if (response != null) {
			event.getAuthor().openPrivateChannel().flatMap(channel -> channel.sendMessage(response)).submit();
		} else {
			LOGGER.info("Response is null, so I don't send it :)");
		}
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

	public static boolean ifAdmin(User user) {
		return ADMINISTRATORS.contains(user.getIdLong());
	}

	public static boolean ifAdmin(Message message) {
		return ADMINISTRATORS.contains(message.getAuthor().getIdLong());
	}

	public static String[] getWords(Message messageIn) {
		return messageIn.getContentDisplay().split(" ");
	}

	enum BotCommand {
		aOp("!aOp <mentions>, example: !aOp @Dzuchun") {

			@Override
			Message process(Message messageIn) {
				List<User> mentions = messageIn.getMentionedUsers();
				mentions.forEach(user -> ADMINISTRATORS.add(user.getIdLong()));
				String response = String.format("Added ops %s", mentions);
				LOGGER.debug(response);
				return new MessageBuilder().append(response).build();
			}

		},
		rOp("!rOp <mentions>, example: !rOp @Code_Bullet :)") {

			@Override
			Message process(Message messageIn) {
				List<User> mentions = messageIn.getMentionedUsers();
				ADMINISTRATORS.removeIf(id -> ArrayHelper.map(mentions, User::getIdLong).contains(id)); // TODO
																										// optimize
																										// map
				String response = String.format("Added ops %s", mentions);
				LOGGER.debug(response);
				return new MessageBuilder().append(response).build();
			}

		},
		aChN("!aCh <message link> <name>, example: !aCh https://discordapp.com/channels/a1/a2/a3 news, where a1, a2, a3 are IDs. Note: multiple channels can be assigned to a single name") {

			@Override
			Message process(Message messageIn) {
				String[] words = getWords(messageIn);
				if (words.length < 3) {
					return null;
				}
				String link = words[1];
				Coords coords = new Coords(link);
				if (coords.channel == null) {
					LOGGER.warn("Link is invalid: {}", link);
					return new MessageBuilder().append("Invalid link:").append(link).build();
				} else {
					String name = words[2];
					CHANNEL_NAMES.add(new ChannelName(name, coords.channel.getIdLong()));
					String response = String.format("Added channel %s to names as %s", coords.channel, name);
					LOGGER.info(response);
					return new MessageBuilder().append(response).build();
				}
			}

		},
		rChN("!rCh <name>, example: !rCh news. Note: multiple channels can be assigned to a single name") {

			@Override
			Message process(Message messageIn) {
				String[] words = getWords(messageIn);
				if (words.length < 2) {
					return null;
				}
				String name = words[1];
				CHANNEL_NAMES.removeIf(chName -> chName.name.equals(name));
				String response = String.format("Removed channels with name %s", name);
				LOGGER.info(response);
				return new MessageBuilder().append(response).build();
			}

		},
		dChN("!dChN") {

			@Override
			Message process(Message messageIn) {
				MessageBuilder res = new MessageBuilder();
				res.append("Channel names:\n");
				for (ChannelName name : CHANNEL_NAMES) {
					res.append(name.toString()).append("\n");
				}
				return res.build();
			}

		},
		sDel("!sDel <channel name> <message link> <delay>, example: !sDel news https://discordapp.com/channels/a1/a2/a3 10, where a1, a2, a3 are IDs.") {

			@Override
			Message process(Message messageIn) {
				String[] words = getWords(messageIn);
				if (words.length < 4) {
					return null;
				}
				long delay = -1;
				try {
					delay = Long.parseLong(words[3]);
				} catch (NumberFormatException e) {
					String response = String.format("Specified delay is not a number: %s", words[3]);
					LOGGER.warn(response);
					return new MessageBuilder().append(response).build();
				}
				if (!CHANNEL_NAMES.anyMatches(n -> n.name.equals(words[1]))) {
					String response = String.format("Specified channel name does not exist: %s", words[1]);
					LOGGER.warn(response);
					return new MessageBuilder().append(response).build();
				}
				TextChannel channel = CHANNEL_NAMES.getMatches(n -> n.name.equals(words[1])).getChannel(jda);
				Coords coords = new Coords(words[2]);
				if (coords.message == null) {
					String response = String.format("Invalid message link: %s", words[2]);
					LOGGER.warn(response);
					return new MessageBuilder().append(response).build();
				}
				StringBuilder messageBuilder = new StringBuilder()
						.append(String.format("Delayed message from %s:\n", messageIn.getAuthor().getAsMention()));
				messageBuilder.append(coords.message.getContentDisplay());
				channel.sendMessage(messageBuilder.toString()).submitAfter(delay, TimeUnit.SECONDS); // TODO repair
																										// mentions

				String response = String.format("Sending delayed message %s to channel %s with delay %s",
						coords.message, channel, delay);
				LOGGER.debug(response);
				return new MessageBuilder().append(response).build();
			}

		},
		sSch("!sSch <channel name> <message link> <date>, example: !sDel news https://discordapp.com/channels/a1/a2/a3 2020/04/02_07:45:34, where a1, a2, a3 are IDs.") {

			@Override
			Message process(Message messageIn) {
				String[] words = getWords(messageIn);
				Date date = null;
				try {
					date = new SimpleDateFormat("yyyy/MM/dd_hh:mm:ss").parse(words[3]);
				} catch (ParseException e) {
					String response = String.format("Specified date is invalid: %s, format - yyyy/MM/dd_hh:mm:ss",
							words[3]);
					LOGGER.warn(response);
					return new MessageBuilder().append(response).build();
				}
				if (!CHANNEL_NAMES.anyMatches(n -> n.name.equals(words[1]))) {
					String response = String.format("Specified channel name does not exist: %s", words[1]);
					LOGGER.warn(response);
					return new MessageBuilder().append(response).build();
				}
				TextChannel channel = CHANNEL_NAMES.getMatches(n -> n.name.equals(words[1])).getChannel(jda);
				Coords coords = new Coords(words[2]);
				if (coords.message == null) {
					String response = String.format("Invalid message link: %s", words[2]);
					LOGGER.warn(response);
					return new MessageBuilder().append(response).build();
				}
				StringBuilder builder = new StringBuilder();
				builder.append(String.format("Scheduled message from %s:\n", messageIn.getAuthor().getAsMention()));
				builder.append(coords.message.getContentDisplay());
				long delay = date.getTime() - System.currentTimeMillis();
				channel.sendMessage(builder.toString()).submitAfter(delay, TimeUnit.MILLISECONDS); // TODO repair
																									// mentions
				String response = String.format("Sending scheduled message %s to channel %s at date %s (in %s seconds)",
						coords.message, channel, date.toString(), delay / 1000);
				LOGGER.debug(response);
				return new MessageBuilder().append(response).build();
			}

		},
		help("!help, or anything started with \"!\", but not recognized as command") {

			@Override
			Message process(Message messageIn) {
				return new MessageBuilder().append(KyoBot.HELP_STRING).build();
			}
		},
		syn("!syn <command main literal>, example: !syn aOp") {

			@Override
			Message process(Message messageIn) {
				String[] words = KyoBot.getWords(messageIn);
				if (words.length < 2 || BotCommand.get(words[1]) == null) {
					return null;
				}
				return new MessageBuilder().append("Syntax: ").append(BotCommand.get(words[1]).syntax).build();
			}

		},
		stop("!stop") {

			@Override
			Message process(Message messageIn) {
				messageIn.getAuthor().openPrivateChannel().flatMap(
						channel -> channel.sendMessage(new MessageBuilder().append("K, shutting down...").build()))
						.complete(); // TODO change this
				try {
					KyoBot.ADMINISTRATORS.save();
				} catch (IOException e) {
					LOGGER.error("Failed to save admins:");
					e.printStackTrace();
				}
				try {
					KyoBot.GUILD_NAMES.save();
				} catch (IOException e) {
					LOGGER.error("Failed to save guild names:");
					e.printStackTrace();
				}
				try {
					KyoBot.CHANNEL_NAMES.save();
				} catch (IOException e) {
					LOGGER.error("Failed to save channel names:");
					e.printStackTrace();
				}
				JDA jda = KyoBot.jda;
				jda.shutdownNow();
				LOGGER.info("Kyo wanna sleep...");
				return null;
			}

		};

		abstract Message process(Message messageIn);

		@Nullable
		public static BotCommand get(String name) {
			try {
				return BotCommand.valueOf(name);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}

		public final String syntax;

		private BotCommand(String syntaxIn) {
			this.syntax = syntaxIn;
		}

		boolean shouldIgnore(Message messageIn) {
			return !KyoBot.ifAdmin(messageIn);
		}
	}
}
