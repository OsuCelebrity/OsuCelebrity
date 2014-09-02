package me.reddev.OsuCelebrity.Twitch;

import java.util.LinkedList;
import java.util.Queue;

import me.reddev.OsuCelebrity.Osu.OsuBeatmap;

public class TwitchRequest
{
	Queue<OsuBeatmap> _requestedBeatmaps = new LinkedList<OsuBeatmap>();
	
	/**
	 * Adds a new beatmap to the request queue. If exists, adds to the end
	 * @param beatmap The beatmap object to request
	 */
	public void AddRequest(OsuBeatmap beatmap)
	{
		if(_requestedBeatmaps.contains(beatmap))
			_requestedBeatmaps.remove(beatmap);
		_requestedBeatmaps.add(beatmap);
	}
	
	/**
	 * Deletes all pending requests
	 */
	public void ClearRequests()
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
	
	public Queue<OsuBeatmap> getRequestedBeatmaps()
	{
		return _requestedBeatmaps;
	}
}
