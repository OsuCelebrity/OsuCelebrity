package me.reddev.OsuCelebrity.Osu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.json.JSONArray;

import me.reddev.OsuCelebrity.Constants.Constants;
import me.reddev.OsuCelebrity.Constants.Settings;
import me.reddev.OsuCelebrity.Logging.Logger;

public class OsuAPI
{
	/**
	 * Gets an Osu! beatmap using a specified ID
	 * @param beatmapID The beatmap ID
	 * @return The beatmap object, or null if none found
	 */
	public static OsuBeatmap GetBeatmap(int beatmapID)
	{
		String requestResponse = postRequest(String.format("/get_beatmaps?b=%s", beatmapID));
		if(requestResponse.isEmpty())
			return null;
		
		JSONArray array = new JSONArray(requestResponse);
		
		
		return new OsuBeatmap();
	}
	
	public static List<OsuBeatmap> GetBeatmapSet(int beatmapsetID)
	{
		return null;
	}
	
	/**
	 * Sends a request to the Osu! API server
	 * @param uri The URL, relative to the API base
	 * @return A JSON response by the server
	 */
	private static String postRequest(String uri)
	{
		try
		{
			//Connects queries into POST string
			URL url = new URL(Constants.OSU_API_ROOT+uri+"&k="+Settings.OSU_KEY);
			URLConnection conn = url.openConnection();
	
			//Add API headers
			conn.setDoOutput(true);
	
			String line, output = "";
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
	
			while ((line = reader.readLine()) != null) {
				//Concatenate read lines
			    output += line;
			}
			reader.close(); 
			
			return output;
		}
		catch (IOException ex)
		{
			Logger.Error(String.format("Osu! API raised %s", ex.getMessage()));
			return "";
		}
	}
}
