package in.HridayKh.hCounterBot.model;

import java.util.ArrayList;
import java.util.List;

public class UserAndStats {

	public String username;
	public String[] scannedComments; // list of scanned ranges in format: a:b, c:d, ...
	public long totalComments;
	public long totalH;
	public long totalNonH;
	public double hScore;

	public UserAndStats(String username, String[] scannedComments, long totalComments, long totalH,
			long totalNonH, double hScore) {
		this.username = username;
		this.scannedComments = scannedComments;
		this.totalComments = totalComments;
		this.totalH = totalH;
		this.totalNonH = totalNonH;
		this.hScore = hScore;
	}

	public String user() {
		return username;
	}

	public List<String[]> scannedComments() {
		List<String[]> ranges = new ArrayList<>();
		for (String range : scannedComments) {
			String[] parts = range.split(":");
			if (parts.length != 2)
				continue;

			parts[0] = parts[0].trim().toLowerCase();
			parts[1] = parts[1].trim().toLowerCase();
			ranges.add(new String[] {
					parts[0].startsWith("t1_") ? parts[0] : "t1_" + parts[0],
					parts[1].startsWith("t1_") ? parts[1] : "t1_" + parts[1]
			});
		}
		this.scannedComments = ranges.toArray(new String[0]);
		return ranges;
	}

	public void updateScannedComments(String[] updatedRanges) {
		this.scannedComments = updatedRanges;
	}

	public long totalComments() {
		return totalComments;
	}

	public long totalH() {
		return totalH;
	}

	public long totalNonH() {
		return totalNonH;
	}

	public double hScore() {
		return hScore;
	}

	public void updateStats(long totalComments, long totalH, long totalNonH, double hScore) {
		this.totalComments = totalComments;
		this.totalH = totalH;
		this.totalNonH = totalNonH;
		this.hScore = hScore;
	}

}
