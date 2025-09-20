class Element {
	/** tid  (transaction id)**/
	public final int tid;
	
	/** itemset positive utility */
	public final int iputils; 
	
	/** itemset negative utility */
	public final int inutils; 
	
	/** remaining positive utility */
	public final int rutils; 
	
	/**
	 * Constructor.
	 *
	 * @param tid  the transaction id
	 * @param iputils  the positive utility of this itemset in this tid
	 * @param inutils  the negative utility of this itemset in this tid
	 * @param rutils  the remaining positive utility of this itemset in this tid
	 */
	public Element(int tid, int iputils, int inutils, int rutils){
		this.tid = tid;
		this.iputils = iputils;
		this.inutils = inutils;
		this.rutils = rutils;
	}

	public void print(int depth) {
		for (int i = 0; i < depth; i++)
			System.out.print("\t");
		System.out.println("\t" + tid + " iputils: " + iputils + " inutils: " + inutils + " rutils: " + rutils);
	}
}
