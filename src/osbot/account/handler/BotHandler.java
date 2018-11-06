package osbot.account.handler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import osbot.account.AccountStage;
import osbot.account.AccountStatus;
import osbot.account.global.Config;
import osbot.bot.BotController;
import osbot.database.DatabaseUtilities;
import osbot.settings.CliArgs;
import osbot.settings.OsbotController;
import osbot.tables.AccountTable;

public class BotHandler {

	/**
	 * Queries {@code tasklist} if the process ID {@code pid} is running.
	 * 
	 * @param pid
	 *            the PID to check
	 * @return {@code true} if the PID is running, {@code false} otherwise
	 */
	public static boolean isProcessIdRunningOnWindows(int pid) {
		try {
			Runtime runtime = Runtime.getRuntime();
			String cmds[] = { "cmd", "/c", "tasklist /FI \"PID eq " + pid + "\"" };
			Process proc = runtime.exec(cmds);

			InputStream inputstream = proc.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
			String line;
			while ((line = bufferedreader.readLine()) != null) {
				// Search the PID matched lines single line for the sequence: " 1300 "
				// if you find it, then the PID is still running.
				// System.out.println("Current pids: "+line);
				if (line.contains(" " + pid + " ")) {
					return true;
				}
			}

			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Cannot query the tasklist for some reason.");
		}

		return false;

	}

	/**
	 * Checks the processes
	 */
	public static void checkProcesses() {
		Iterator<Integer> it = BotController.getJavaPIDsWindows().iterator();

		while (it.hasNext()) {
			int nextPid = it.next();

			if (!isProcessIdRunningOnWindows(nextPid)) {
				System.out.println("Removed pid: " + nextPid + " from the processes list, was no longer running");
				it.remove();
			}
		}

	}

	/**
	 * Runs a bot
	 * 
	 * @param bot
	 */
	public static void runBot(OsbotController bot) {
		if (bot == null) {
			System.out.println("Invalid bot");
			return;
		}
		AccountTable account = bot.getAccount();

		// bot.addArguments(CliArgs.DEBUG, false, 5005);
		bot.addArguments(CliArgs.LOGIN, true, Config.OSBOT_USERNAME, Config.OSBOT_PASSWORD);
		bot.addArguments(CliArgs.DATA, false, 0);
		bot.addArguments(CliArgs.WORLD, false, account.getWorld());

		// if (!account.getScript().equalsIgnoreCase(AccountStage.TUT_ISLAND.name())) {
		bot.addArguments(CliArgs.ALLOW, false, "norandoms");
		// }

		if (account.hasUsernameAndPasswordAndBankpin()) {
			bot.addArguments(CliArgs.BOT, true, account.getEmail(), account.getPassword(), account.getBankPin());
		} else if (account.hasUsernameAndPassword()) {
			bot.addArguments(CliArgs.BOT, true, account.getEmail(), account.getPassword(), "0000");
		}
		bot.addArguments(CliArgs.MEM, false, 2048);
		if (account.hasValidProxy()) {
			bot.addArguments(CliArgs.PROXY, true, account.getProxyIp(), account.getProxyPort(),
					account.getProxyUsername(), account.getProxyPassword());
		}
		if (account.hasScript()) {
			String accountStatus = bot.getAccount().getStatus().name().replaceAll("_", "-");
			bot.addArguments(CliArgs.SCRIPT, true, account.getScript(),
					account.getEmail() + "_" + account.getPassword() + "_" + bot.getPidId() + "_" + accountStatus);
		}
		bot.runBot(false);
	}

	public static void runMule(OsbotController bot, String toTradeWith) {
		if (bot == null) {
			System.out.println("Invalid bot");
			return;
		}
		AccountTable account = bot.getAccount();

		// bot.addArguments(CliArgs.DEBUG, false, 5005);
		bot.addArguments(CliArgs.LOGIN, true, Config.OSBOT_USERNAME, Config.OSBOT_PASSWORD);
		bot.addArguments(CliArgs.DATA, false, 0);
		bot.addArguments(CliArgs.WORLD, false, 394);

		// if (!account.getScript().equalsIgnoreCase(AccountStage.TUT_ISLAND.name())) {
		bot.addArguments(CliArgs.ALLOW, false, "norandoms");
		// }

		if (account.hasUsernameAndPasswordAndBankpin()) {
			bot.addArguments(CliArgs.BOT, true, account.getEmail(), account.getPassword(), account.getBankPin());
		} else if (account.hasUsernameAndPassword()) {
			bot.addArguments(CliArgs.BOT, true, account.getEmail(), account.getPassword(), "0000");
		}
		bot.addArguments(CliArgs.MEM, false, 2048);
		if (account.hasValidProxy()) {
			bot.addArguments(CliArgs.PROXY, true, account.getProxyIp(), account.getProxyPort(),
					account.getProxyUsername(), account.getProxyPassword());
		}
		if (account.hasScript()) {
			String accountStatus = bot.getAccount().getStage().name().replaceAll("_", "-");
			bot.addArguments(CliArgs.SCRIPT, true, "MULE_TRADING", account.getEmail() + "_" + account.getPassword()
					+ "_" + bot.getPidId() + "_" + accountStatus + "_" + toTradeWith);
		}
		bot.runBot(true);
	}

	/**
	 * Returns the amount of bots that are currently active
	 * 
	 * @return
	 */
	public static int getAmountOfBotsActive() {
		return BotController.getJavaPIDsWindows().size();

	}

	/**
	 * Kill all bots
	 */
	public static void killAllBots() {
		for (int pid : BotController.getJavaPIDsWindows()) {
			BotController.killProcess(pid);
		}
	}

	/**
	 * Gets the mule partner
	 * 
	 * @param bot
	 * @return
	 */
	public static OsbotController getMulePartner(OsbotController bot) {
		for (int b = 0; b < BotController.getBots().size(); b++) {
			OsbotController osbot2 = BotController.getBots().get(b);

			if (!osbot2.getAccount().getUsername().equalsIgnoreCase(bot.getAccount().getUsername())
					&& osbot2.getAccount().getTradeWithOther().equalsIgnoreCase(bot.getAccount().getTradeWithOther())) {
				return osbot2;
			}
		}
		return null;
	}

	/**
	 * Handling with running the mules
	 * 
	 * Setting them up to yet
	 */
	public static void handleMules() {
		OsbotController availableMule = null;

		// If accounts are not loaded yet
		if (BotController.getBots().size() == 0) {
			System.out.println("[MULE TRADING] Couldn't trade yet, accounts weren't loaded yet");
		}

		for (int i = 0; i < BotController.getBots().size(); i++) {
			OsbotController osbot = BotController.getBots().get(i);

			// Looking for a mule account and that is currently available ('NULL')
			if (osbot.getAccount().getStatus() == AccountStatus.MULE
					&& osbot.getAccount().getTradeWithOther() == null) {
				availableMule = osbot;
				System.out.println("[MULE TRADING] Found an account! " + availableMule.getAccount().getUsername());
			}
		}

		// Must have a mule available to continue
		if (BotController.getBots().size() > 0 && availableMule == null) {
			System.out.println("[MULE TRADING] Couldn't find mule to trade with, is not available");
			return;
		}

		for (int i = 0; i < BotController.getBots().size(); i++) {
			OsbotController osbot = BotController.getBots().get(i);

			// If an account wants to mule
			if (osbot.getAccount().getStage() == AccountStage.MULE_TRADING
					&& !osbot.getAccount().getUsername().equalsIgnoreCase(availableMule.getAccount().getUsername())) {

				// Setting in database that the mule is trading with the workers name
				DatabaseUtilities.setTradingWith(osbot.getAccount().getUsername(), availableMule.getId());

				System.out.println("[MULE TRADING] Starting mule: " + availableMule.getAccount().getUsername()
						+ " to trade with: " + osbot.getAccount().getUsername());

				// Setting in database that worker is trading with mule
				DatabaseUtilities.setTradingWith(availableMule.getAccount().getUsername(), osbot.getId());

				System.out.println("[MULE TRADING] Starting worker: " + osbot.getAccount().getUsername()
						+ " to trade with mule: " + availableMule.getAccount().getUsername());

				break;
			}
		}

	}

	/**
	 * Handlig all the bots, deciding how many should be open etc.
	 */
	public static void handleBots() {
		// Will check the PID processes if they are still running or not, when not, they
		// get deleted in the PID list
		BotHandler.checkProcesses();

		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar calendar2 = Calendar.getInstance();
		calendar2.setTime(new Date());

		System.out.println("[BOT HANDLER MANAGEMENT] Bots currently active: " + getAmountOfBotsActive());

		for (int i = 0; i < BotController.getBots().size(); i++) {
			OsbotController osbot = BotController.getBots().get(i);

			// Running mules
			if (osbot != null && osbot.getAccount().getTradeWithOther() != null
					&& osbot.getAccount().getTradeWithOther().length() > 0
					&& !BotController.containsInPidList(osbot.getPidId())) {
				runMule(osbot, osbot.getAccount().getTradeWithOther());
				System.out.println("Running mule trading " + osbot.getAccount().getUsername());

				try {
					Thread.sleep(15000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		if (getAmountOfBotsActive() < Config.MAX_BOTS_OPEN) {

			for (int i = 0; i < BotController.getBots().size(); i++) {
				OsbotController osbot = BotController.getBots().get(i);

				if (osbot != null
						&& (osbot.getAccount().getStatus() == AccountStatus.AVAILABLE
								|| osbot.getAccount().getStatus() == AccountStatus.WALKING_STUCK)
						&& osbot.getAccount().getStage() != AccountStage.UNKNOWN
						&& osbot.getAccount().getStage() != AccountStage.MULE_TRADING
						&& !BotController.containsInPidList(osbot.getPidId())
						&& getAmountOfBotsActive() < Config.MAX_BOTS_OPEN) {

					if (!calendar2.after(osbot.getAccount().getDate())) {
						System.out.println(
								"Skipping " + osbot.getAccount().getUsername() + " because has currently a break");
						continue;
					}

					runBot(osbot);
					System.out.println("[BOT HANDLER MANAGEMENT] Running bot name: " + osbot.getAccount().getStage()
							+ " " + osbot.getAccount().getUsername());
					try {
						Thread.sleep(15000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (osbot == null) {
					System.out.println(osbot + " got null");
				} else if (getAmountOfBotsActive() >= Config.MAX_BOTS_OPEN) {
					System.out.println(
							"[BOT HANDLER MANAGEMENT] Maximum amount of bots currently online for this machine reached");
				}
			}
		}
	}
}
