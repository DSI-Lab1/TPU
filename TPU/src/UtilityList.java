import java.util.ArrayList;
import java.util.List;

class UtilityList {
	// the last item of the itemset represented by this utility list
	public Integer item;
	
	// the sum of iPutil values
	public int sumIPutils = 0;
	
	// the sum of iNutil values
	public int sumINutils = 0;
	
	// the sum of rutil values
	public int sumRutils = 0;
	
	// the list of elements in this utility list
	public List<Element> elements = new ArrayList<Element>();

	int largestPeriodicity = 0;
	int smallestPeriodicity = Integer.MAX_VALUE;

	/**
	 * Constructor
	 * 
	 * @param item the last item of the itemset represented by this utility-list
	 */
	public UtilityList(int item) {
		this.item = item;
	}

	/**
	 * the real utility of this itemset
	 * 
	 * @return
	 */
	public int getUtils() {
		return (sumIPutils + sumINutils);
	}

	/**
	 * Method to add an element to this utility-list and update the sums at the same time.
	 */
	public void addElement(Element element){
		sumIPutils += element.iputils;
		sumINutils += element.inutils;
		sumRutils += element.rutils;
		elements.add(element);
	}
	public int getSupport() {
		return elements.size();
	}
}
