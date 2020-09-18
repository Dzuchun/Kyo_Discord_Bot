package dzuchun.kyobot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import dzuchun.util.Administrator;
import dzuchun.util.ArrayHelper;
import dzuchun.util.SavedArrayList;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class KyoBot extends ListenerAdapter {

	private static final Logger LOGGER = LoggerFactory.getILoggerFactory().getLogger("Kyo");

	static JDA jda = null;

	private static final SavedArrayList<Administrator> ADMINISTRATORS = new SavedArrayList<Administrator>(
			new File("administrators.txt"), Administrator.class);

	public static List<Administrator> getAdmins() {
		return ImmutableList.copyOf(ADMINISTRATORS);
	}

	private static String HELP_STRING = "";

	private static final SavedArrayList<GuildName> GUILD_NAMES = new SavedArrayList<GuildName>(
			new File("guild_names.txt"), GuildName.class);

	private static final SavedArrayList<ChannelName> CHANNEL_NAMES = new SavedArrayList<ChannelName>(
			new File("channel_names.txt"), ChannelName.class);

	private static final SavedArrayList<DelayedMessage> DELAYED_MESSAGES = new SavedArrayList<>(
			new File("delayed_messages.txt"), DelayedMessage.class);
	private static final Object DELAYED_MESSAGES_LOCK = new Object();
	private static final Long DEFAULT_WAIT_TIME = 60000l;

	static class DelayedMessagesThread extends Thread {
		private Object MONITOR;

		public boolean keepRunning = true;

		{
			this.setName("DelMessages-Thread");
			this.setDaemon(true);
		}

		@Override
		public void run() {
			MONITOR = new Object();
			while (keepRunning) {
				LOGGER.debug("tick!");
				long currentTime = System.currentTimeMillis();
				long sleepTime = DEFAULT_WAIT_TIME;
				List<DelayedMessage> expired = new ArrayList<>();
				for (DelayedMessage delM : DELAYED_MESSAGES) {
					if (delM.getPublicationTime() <= currentTime) {
						expired.add(delM);
						String reqName = delM.getChannelName();
						LOGGER.debug("Sending message {}:", delM);
						int n = 0;
						for (ChannelName chN : CHANNEL_NAMES) {
							if (chN.getName().equals(reqName)) {
								TextChannel channel = chN.getChannel(jda);
								channel.sendMessage(delM.getMessage()).complete();
								LOGGER.info("Sent to {}", channel);
								n++;
							}
						}
						LOGGER.debug("Sent to all channels named {}, {} in total", reqName, n);
					} else if (delM.getPublicationTime() - currentTime < sleepTime) {
						sleepTime = delM.getPublicationTime() - currentTime;
					}

				}
				synchronized (DELAYED_MESSAGES_LOCK) {
					DELAYED_MESSAGES.removeAll(expired);
				}
				try {
					synchronized (MONITOR) {
						LOGGER.info("Going to sleep for {}s", sleepTime / 1000);
						MONITOR.wait(sleepTime);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static final DelayedMessagesThread DELAYED_MESSAGES_THREAD = new DelayedMessagesThread();

	public static void main(String[] args) throws InterruptedException {
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
//				LOGGER.info("Specify API token: ");
				TOKEN = args[1];
			}
			jda = JDABuilder.createDefault(TOKEN).addEventListeners(new KyoBot()).setActivity(Activity.listening("HTT"))
					.build();
			jda.awaitReady();
			try {
				ADMINISTRATORS.load();
				if (ADMINISTRATORS.isEmpty()) {
					LOGGER.warn("No administrators specified");
				} else {
					LOGGER.info("Loaded administators:\n{}", ADMINISTRATORS.toString());
				}
			} catch (IOException e) {
				LOGGER.error("Could not load administrators:");
				e.printStackTrace();
				Thread.sleep(10);
				System.exit(-2);
			}
			loadHelpString();
			try {
				GUILD_NAMES.load();
				if (GUILD_NAMES.isEmpty()) {
					LOGGER.info("No guild names loaded");
				} else {
					LOGGER.info("Loaded guild names:\n{}", GUILD_NAMES.toString());
				}
			} catch (IOException e) {
				LOGGER.error("Could not load guild names:");
				e.printStackTrace();
				Thread.sleep(10);
			}
			try {
				CHANNEL_NAMES.load();
				if (CHANNEL_NAMES.isEmpty()) {
					LOGGER.info("No channel names loaded");
				} else {
					LOGGER.info("Loaded channel names:\n{}", CHANNEL_NAMES.toString());
				}
			} catch (IOException e) {
				LOGGER.error("Could not load channel names:");
				e.printStackTrace();
				Thread.sleep(10);
			}
			try {
				DELAYED_MESSAGES.load();
				if (DELAYED_MESSAGES.isEmpty()) {
					LOGGER.info("No delayed messages loaded");
				} else {
					LOGGER.info("Loaded delayed messages:\n{}", DELAYED_MESSAGES.toString());
				}
			} catch (IOException e) {
				LOGGER.error("Could not load delayed messages:");
				e.printStackTrace();
				Thread.sleep(10);
			}
			DELAYED_MESSAGES_THREAD.start();
			LOGGER.info("Started delayed messages thread");
		} catch (LoginException e) {
			LOGGER.error("Failed to log in: ");
			e.printStackTrace();
			System.exit(-3);
		} catch (FileNotFoundException e) {
			LOGGER.error("File not found:");
			e.printStackTrace();
			System.exit(-4);
		}
		LOGGER.info("Kyo logged in!");
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		super.onMessageReceived(event);
//		if ((event.getChannelType() == ChannelType.PRIVATE) && !jda.getPrivateChannels().contains(event.getChannel())) {
//			LOGGER.error("Hurray, recieved message in non-existent private channel");
//		}
		Message message = event.getMessage();
		String content = message.getContentDisplay();
		if ((content.charAt(0) != '!') || content.length() < 2) {
			return;
		}

		BotCommand command = BotCommand.get(content.split(" ")[0].substring(1));

		command = command == null ? BotCommand.help : command;

		if (command.shouldIgnore(message)) {
			LOGGER.debug("Ignoring");
			return;
		}

		Message response = command.process(message);
		if (response != null) {
			event.getAuthor().openPrivateChannel().flatMap(channel -> channel.sendMessage(response)).submit();
		} else {
			LOGGER.debug("Response is null, so I don't send it :)");
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
		return ADMINISTRATORS.anyMatches(a -> a.id() == user.getIdLong());
	}

	public static boolean ifAdmin(Message message) {
		return ADMINISTRATORS.anyMatches(a -> a.id() == message.getAuthor().getIdLong());
	}

	public static String[] getWords(Message messageIn) {
		return messageIn.getContentDisplay().split(" ");
	}

	enum BotCommand {
		aOp("!aOp <mentions>, example: !aOp @Dzuchun") {

			@Override
			Message process(Message messageIn) {
				List<User> mentions = messageIn.getMentionedUsers();
				mentions.forEach(user -> ADMINISTRATORS.add(new Administrator(user.getIdLong())));
				String response = String.format("Added ops %s", mentions);
				LOGGER.debug(response);
				return new MessageBuilder().append(response).build();
			}

		},
		rOp("!rOp <mentions>, example: !rOp @Code_Bullet :)") {

			@Override
			Message process(Message messageIn) {
				List<User> mentions = messageIn.getMentionedUsers();
				List<Long> ids = ArrayHelper.map(mentions, User::getIdLong);
				ADMINISTRATORS.removeIf(a -> ids.contains(a.id()));
				String response = String.format("Added ops %s", mentions);
				LOGGER.debug(response);
				return new MessageBuilder().append(response).build();
			}

		},
		aChN("!aCh <message link> <name>, example: !aCh https://discordapp.com/channels/a1/a2/a3 news, where a1, a2, a3 are IDs. Note:[WIP] multiple channels can be assigned to a single name") {

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
		rChN("!rCh <name>, example: !rCh news. Note:[WIP] multiple channels can be assigned to a single name") {

			@Override
			Message process(Message messageIn) {
				String[] words = getWords(messageIn);
				if (words.length < 2) {
					return null;
				}
				String name = words[1];
				CHANNEL_NAMES.removeIf(chName -> chName.getName().equals(name));
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
		sDel("!sDel <channel name> <message link> <delay>, example: !sDel news https://discordapp.com/channels/a1/a2/a3 10, where a1, a2, a3 are IDs. WARN: known bug, links to messages from private chats are not recognised.") {

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
				if (!CHANNEL_NAMES.anyMatches(n -> n.getName().equals(words[1]))) {
					String response = String.format("Specified channel name does not exist: %s", words[1]);
					LOGGER.warn(response);
					return new MessageBuilder().append(response).build();
				}
				TextChannel channel = CHANNEL_NAMES.getMatches(n -> n.getName().equals(words[1])).getChannel(jda);
				Coords coords = new Coords(words[2]);
				if (coords.message == null) {
					String response = String.format("Invalid message link: %s", words[2]);
					LOGGER.warn(response);
					return new MessageBuilder().append(response).build();
				}
				StringBuilder messageBuilder = new StringBuilder()
						.append(String.format("Delayed message from %s:\n", messageIn.getAuthor().getAsMention()));
				messageBuilder.append(coords.message.getContentDisplay());
				String response = String.format("Sending delayed message %s to channel %s with delay %s",
						coords.message, channel, delay);
				Message message = new MessageBuilder().append(response).build();
//				channel.sendMessage(messageBuilder.toString()).submitAfter(delay, TimeUnit.SECONDS); // TODO repair mentions
				synchronized (DELAYED_MESSAGES) {
					DELAYED_MESSAGES.add(new DelayedMessage(System.currentTimeMillis() + delay, words[1],
							messageBuilder.toString(), DelayedMessage.Type.DELAYED));
				}
				synchronized (DELAYED_MESSAGES_THREAD.MONITOR) {
					DELAYED_MESSAGES_THREAD.MONITOR.notifyAll();
				}
				LOGGER.debug(response);
				return message;
			}

		},
		rDel("!rDel [number of delayed message in list]") {

			@Override
			Message process(Message messageIn) {
				String[] words = getWords(messageIn);
				MessageBuilder builder = new MessageBuilder();
				if (DELAYED_MESSAGES.isEmpty()) {
					builder.append("No delayed messages");
				} else {
					if (words.length < 2) {
						int i = 1;
						builder.append("Displaying list of delayed messages:\n");
						for (DelayedMessage delM : DELAYED_MESSAGES) {
							builder.append(String.format("%s) %s\n", i, delM.toString()));
						}
					} else {
						try {
							int number = Integer.parseInt(words[1]);
							DelayedMessage dMes = DELAYED_MESSAGES.get(number - 1);
							DELAYED_MESSAGES.remove(dMes);
							builder.append("Removed delayed message:\n").append(dMes);
						} catch (NumberFormatException e) {
							builder.append("Could not resolve number. Syntax:\n" + syntax);
						} catch (IndexOutOfBoundsException e) {
							builder.append(
									String.format("Please, specify number from 1 to %s", DELAYED_MESSAGES.size()));
						}
					}
				}
				return builder.build();
			}

		},
		sSch("!sSch <channel name> <message link> <date>, example: !sDel news https://discordapp.com/channels/a1/a2/a3 2020/04/02_07:45:34, where a1, a2, a3 are IDs. WARN: known bug, links to messages from private chats are not recognised.") {

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
				if (!CHANNEL_NAMES.anyMatches(n -> n.getName().equals(words[1]))) {
					String response = String.format("Specified channel name does not exist: %s", words[1]);
					LOGGER.warn(response);
					return new MessageBuilder().append(response).build();
				}
				TextChannel channel = CHANNEL_NAMES.getMatches(n -> n.getName().equals(words[1])).getChannel(jda);
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
				String response = String.format("Sending scheduled message %s to channel %s at date %s (in %s seconds)",
						coords.message, channel, date.toString(), delay / 1000);
				Message message = new MessageBuilder().append(response).build();
//				channel.sendMessage(builder.toString()).submitAfter(delay, TimeUnit.MILLISECONDS); // TODO repair mentions
				synchronized (DELAYED_MESSAGES_LOCK) {
					DELAYED_MESSAGES.add(new DelayedMessage(date.getTime(), words[1], builder.toString(),
							DelayedMessage.Type.SCHEDULED));
				}
				synchronized (DELAYED_MESSAGES_THREAD.MONITOR) {
					DELAYED_MESSAGES_THREAD.MONITOR.notifyAll();
				}
				LOGGER.debug(response);
				return message;
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
						.complete(); // TODO
										// change
										// this
				try {
					KyoBot.ADMINISTRATORS.save();
					LOGGER.info("Saved admins");
				} catch (IOException e) {
					LOGGER.error("Failed to save admins:");
					e.printStackTrace();
				}
				try {
					KyoBot.GUILD_NAMES.save();
					LOGGER.info("Saved guild names");
				} catch (IOException e) {
					LOGGER.error("Failed to save guild names:");
					e.printStackTrace();
				}
				try {
					KyoBot.CHANNEL_NAMES.save();
					LOGGER.info("Saved channel names");
				} catch (IOException e) {
					LOGGER.error("Failed to save channel names:");
					e.printStackTrace();
				}
				try {
					KyoBot.DELAYED_MESSAGES.save();
					LOGGER.info("Saved delayed messages");
				} catch (IOException e) {
					LOGGER.error("Failed to save delayed messages:");
					e.printStackTrace();
				}
				DELAYED_MESSAGES_THREAD.keepRunning = false;
				synchronized (DELAYED_MESSAGES_THREAD.MONITOR) {
					DELAYED_MESSAGES_THREAD.MONITOR.notifyAll();
				}
				LOGGER.info("Stopped DelMessages-Thread");
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
