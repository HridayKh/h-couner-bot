package in.HridayKh.hCounterBot.reddit.model.types;

/*
{
    "kind": "Listing"|"t1"|...,
    "data": {......}
}
*/
public class RedditThing<T> {
    public String kind;
    public T data;

}
