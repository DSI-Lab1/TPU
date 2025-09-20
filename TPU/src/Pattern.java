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
	 * @param prefix this.prefix = buffer ，实现了一个字符拼接，加上X.item后表示项集X
	 * @param length 表示项集X的前缀prefix长度
	 * @param X X的效用列表，其中X.item代表项集X中的最后一个项目
	 * @param idx idx表示该项集X是第几个候选高效用项集，用于对该模式进行排序
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
