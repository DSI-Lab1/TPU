import java.io.Serializable;

public class Item implements Serializable {
	long twu = 0L;
	int utility = 0;
	int support = 0;
	int largestPeriodicity = 0;
	int smallestPeriodicity = Integer.MAX_VALUE;
	int lastSeenTransaction = 0;

	public String toString() {
		return String.valueOf(utility);
	}
}