package me.reddev.OsuCelebrity.Twitch;

import java.util.LinkedList;
import java.util.Queue;

import org.tillerino.osuApiModel.OsuApiBeatmap;

public class TwitchRequest
{
	private Queue<OsuApiBeatmap> _requestedBeatmaps = new LinkedList<OsuApiBeatmap>();
	
	/**
	 * Adds a new beatmap to the request queue. If exists, adds to the end
	 * @param beatmap The beatmap object to request
	 */
	public void AddRequest(OsuApiBeatmap beatmap)
	{
		if(_requestedBeatmaps.contains(beatmap))
			_requestedBeatmaps.remove(beatmap);
		_requestedBeatmaps.add(beatmap);
	}
	
	/**
	 * Deletes all pending requests
	 */
	public void clearRequests()
	{
		_requestedBeatmaps.clear();
	}
	
	/**
	 * @return The amount of requested beatmaps in the queue
	 */
	public int getRequestCount()
	{
		return _requestedBeatmaps.size();
	}
	
	public Queue<OsuApiBeatmap> getRequestedBeatmaps()
	{
		return _requestedBeatmaps;
	}
}
