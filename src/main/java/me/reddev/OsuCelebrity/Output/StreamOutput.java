package me.reddev.OsuCelebrity.Output;

import lombok.extern.slf4j.Slf4j;
import me.reddev.OsuCelebrity.Osu.OsuApplication;

@Slf4j
public class StreamOutput implements Runnable
{
	public interface StreamOutputSettings {
		String getStreamOutputPath();
	}
	
	private OsuApplication _application;
	private StreamOutputSettings _settings;
	
	public StreamOutput(OsuApplication application, StreamOutputSettings settings)
	{
		this._application = application;
		this._settings = settings;
	}
	
	public void start()
	{
		Thread outputThread = new Thread(this);
		outputThread.setName("StreamOutput");
		//(outputThread).start();
	}
	
	public void run()
	{
		while(true)
		{
			try {
				//TODO: Write output code
				Thread.sleep(100000);
			} catch (InterruptedException e) {
				log.error("interrupted thread sleep");
			}
		}
	}
}
