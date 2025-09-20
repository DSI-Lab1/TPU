public class Pattern implements Comparable<Pattern> {

	String prefix;
	long utility;
	int sup;

	// for sorting patterns in order of insertion
	long idx;

	double averagePeriodicity;
	double minPeriodicity;
	double maxPeriodicity;

	/**
	 * Pattern
	 *
	 * @param prefix
	 * @param length
	 * @param X
	 * @param idx
	 * @param averagePeriodicity
	 * @param minPeriodicity
	 * @param maxPeriodicity
	 */
	public Pattern(int[] prefix, int length, UtilityList X, long idx,double averagePeriodicity,double minPeriodicity, double maxPeriodicity) {
		String buffer = "";
		for (int i = 0; i < length; i++) {
			buffer += prefix[i];
			buffer += " ";
		}
		buffer += "" + X.item;
		this.prefix = buffer;
		this.idx = idx;

		this.utility = X.getUtils();
		this.sup = X.elements.size();
		this.averagePeriodicity = averagePeriodicity;
		this.minPeriodicity = minPeriodicity;
		this.maxPeriodicity = maxPeriodicity;

		// + X.sup; //X.sup for closed items
	}

	public String getPrefix() {
		return this.prefix;
	}

	public int compareTo(Pattern o) {
		if (o == this) {
			return 0;
		}
		
		long compare = this.utility - o.utility;
		if (compare != 0) {
			return (int) compare;
		}

		return this.hashCode() - o.hashCode();
	}

}
