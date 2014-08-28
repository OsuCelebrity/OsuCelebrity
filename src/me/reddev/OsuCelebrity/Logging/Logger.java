package me.reddev.OsuCelebrity.Logging;

import me.reddev.OsuCelebrity.Constants.Constants;

public class Logger
{
	private static String info = "INFO", warning = "WARNING", error = "ERROR",
			fatal = "FATAL";

	public static void Info(String message)
	{
		output(info, message);
	}

	public static void Warning(String message)
	{
		output(warning, message);
	}

	public static void Error(String message)
	{
		output(error, message);
	}

	public static void Fatal(String message)
	{
		output(fatal, message);
	}

	private static void output(String header, String message)
	{
		System.out.println(String.format("[%s] [%s] %s", Constants.APP_NAME,
				header, message));
	}
}
