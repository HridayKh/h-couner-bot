package in.HridayKh.hCounterBot.reddit.model.types;

/*
{
    "kind": "Listing"|"t1"|...,
    "data": {......}
}
*/
public class RedditListing<T> {
    public String kind;
    public RedditListingData<T> data;

}
