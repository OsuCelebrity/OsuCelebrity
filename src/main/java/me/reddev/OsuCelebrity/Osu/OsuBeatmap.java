package me.reddev.OsuCelebrity.Osu;

public class OsuBeatmap
{
	public int approved;
	//TODO: Give some way for GSON to decode approved_date and last_update into date
	public String approved_date;
	public String last_update;
	public String artist;
	public int beatmap_id;
	public int beatmapset_id;
	public int bpm;
	public String creator;
	public double difficultyrating;
	public int diff_size;
	public int diff_overall;
	public int diff_approach;
	public int diff_drain;
	public int hit_length;
	public String source;
	public String title;
	public int total_length;
	public String version;
	public int mode;
	
	@Override
	public String toString()
	{
		return String.format("%s - %s[%s]", artist, title, version);
	}
}
