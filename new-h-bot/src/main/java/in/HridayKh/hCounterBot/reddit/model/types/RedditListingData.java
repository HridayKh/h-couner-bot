package in.HridayKh.hCounterBot.reddit.model.types;

/*
{
        "after": null,
        "dist": 0,
        "modhash": null,
        "geo_filter": "",
        "children": [],
        "before": null
}
*/
public class RedditListingData<T> {
	public String after;
	public int dist;
	public String modhash;
	public String geo_filter;
	public RedditThing<T>[] children;
	public String before;
}
