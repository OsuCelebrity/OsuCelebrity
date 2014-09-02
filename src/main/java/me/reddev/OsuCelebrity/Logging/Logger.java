package me.reddev.OsuCelebrity.Logging;

import me.reddev.OsuCelebrity.Constants.Constants;

public class Logger
{
	private static String info = "INFO", warning = "WARNING", error = "ERROR",
			fatal = "FATAL";

	/**
	 * Outputs an information message
	 * @param message The information message
	 */
	public static void Info(String message)
	{
		output(info, message);
	}

	/**
	 * Outputs a warning message
	 * @param message The warning message
	 */
	public static void Warning(String message)
	{
		output(warning, message);
	}

	/**
	 * Outputs an error message
	 * @param message The error message
	 */
	public static void Error(String message)
	{
		output(error, message);
	}

	/**
	 * Outputs a fatal message
	 * @param message The fatal message
	 */
	public static void Fatal(String message)
	{
		output(fatal, message);
	}

	/**
	 * Outputs a message to the standard output and log
	 * @param header The type of output
	 * @param message The message to output
	 */
	private static void output(String header, String message)
	{
		System.out.println(String.format("[%s] [%s] %s", Constants.APP_NAME,
				header, message));
	}
}
