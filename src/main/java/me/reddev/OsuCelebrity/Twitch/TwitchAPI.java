package me.reddev.OsuCelebrity.Twitch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import lombok.extern.slf4j.Slf4j;
import me.reddev.OsuCelebrity.Constants.Constants;

@Slf4j
public class TwitchAPI
{
	public interface TwitchApiSettings {
		String getTwitchClientId();
		String getTwitchToken();
	}
	
	private TwitchApiSettings _settings;
	
	private TwitchAPI(TwitchApiSettings settings) {
		super();
		this._settings = settings;
	}

	/**
	 * Sends a request to the Twitch API server with POST queries
	 * @param uri The URL, relative to the API base
	 * @param queries The POST queries
	 * @return A JSON response by the server
	 */
	private String postRequest(String uri, String... queries)
	{
		try
		{
			//Connects queries into POST string
			String urlParameters = join(queries, "&");
			URL url = new URL(Constants.TWITCH_API_ROOT+uri);
			URLConnection conn = url.openConnection();
	
			//Add API headers
			conn.setRequestProperty("Client-ID", 
					_settings.getTwitchClientId());
			conn.setRequestProperty("Accept", 
					"application/vnd.twitchtv.v2+json");
			conn.setRequestProperty("Authorization", 
					"OAuth: "+_settings.getTwitchToken());
			conn.setDoOutput(true);
	
			OutputStreamWriter writer = new OutputStreamWriter(
					conn.getOutputStream());
	
			writer.write(urlParameters);
			writer.flush();
	
			String line, output = "";
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
	
			while ((line = reader.readLine()) != null) {
				//Concatenate read lines
			    output += line;
			}
			writer.close();
			reader.close(); 
			
			return output;
		}
		catch (IOException ex)
		{
			log.error(String.format("Twitch API raised %s", ex.getMessage()));
			return "";
		}
	}
	
	private static String join(String r[], String d)
	{
	        if (r.length == 0) return "";
	        StringBuilder sb = new StringBuilder();
	        int i;
	        for(i=0;i<r.length-1;i++)
	            sb.append(r[i]+d);
	        return sb.toString()+r[i];
	}
}
