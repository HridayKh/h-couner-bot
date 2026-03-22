package in.HridayKh.hCounterBot.model;

public class LeaderboardRow {
    public final int    rank;
    public final String username;
    public final String hScoreFormatted;
    public final String totalCommentsFormatted;
    public final String totalHFormatted;
    public final String totalNonHFormatted;
    public final String totalCharsFormatted;

    public LeaderboardRow(int rank, UserAndStats u) {
        this.rank                  = rank;
        this.username              = u.user();
        this.hScoreFormatted       = String.format("%.4f", u.hScore());
        this.totalCommentsFormatted = String.format("%,d", u.totalComments());
        this.totalHFormatted       = String.format("%,d", u.totalH());
        this.totalNonHFormatted    = String.format("%,d", u.totalNonH());
        this.totalCharsFormatted   = String.format("%,d", u.totalH() + u.totalNonH());
    }
}
