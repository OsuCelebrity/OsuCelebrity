package me.reddev.OsuCelebrity.Osu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
		
		Type listType = new TypeToken<Collection<OsuBeatmap>>(){}.getType();
		List<OsuBeatmap> listedMaps = (new Gson()).fromJson(requestResponse, listType);
		return (listedMaps.size() >= 1 ? listedMaps.get(0) : null);
	}
	
	/**
	 * Gets an Osu! beatmap set using a specified ID
	 * @param beatmapsetID The beatmap set ID
	 * @return A list of assosciated beatmap objects, or list of null if none found
	 */
	public static List<OsuBeatmap> GetBeatmapSet(int beatmapsetID)
	{
		String requestResponse = postRequest(String.format("/get_beatmaps?s=%s", beatmapsetID));
		if(requestResponse.isEmpty())
			return null;
		
		Type listType = new TypeToken<Collection<OsuBeatmap>>(){}.getType();
		return (new Gson()).fromJson(requestResponse, listType);
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
