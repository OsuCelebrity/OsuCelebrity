package me.reddev.OsuCelebrity.Twitch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import me.reddev.OsuCelebrity.Constants.Constants;
import me.reddev.OsuCelebrity.Constants.Settings;
import me.reddev.OsuCelebrity.Logging.Logger;

public class TwitchAPI
{
	private static String postRequest(String uri, String... queries)
	{
		try
		{
			String urlParameters = join(queries, "&");
			URL url = new URL(Constants.TWITCH_API_ROOT+uri);
			URLConnection conn = url.openConnection();
	
			conn.setRequestProperty("Client-ID", 
					Constants.TWITCH_CLIENT_ID);
			conn.setRequestProperty("Accept", 
					"application/vnd.twitchtv.v2+json");
			conn.setRequestProperty("Authorization", 
					"OAuth: "+Settings.TWITCH_TOKEN);
			conn.setDoOutput(true);
	
			OutputStreamWriter writer = new OutputStreamWriter(
					conn.getOutputStream());
	
			writer.write(urlParameters);
			writer.flush();
	
			String line, output = "";
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
	
			while ((line = reader.readLine()) != null) {
			    output += line;
			}
			writer.close();
			reader.close(); 
			
			return output;
		}
		catch (IOException ex)
		{
			Logger.Error(String.format("Twitch API raised %s", ex.getMessage()));
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
