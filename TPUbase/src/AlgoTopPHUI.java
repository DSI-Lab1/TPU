import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import java.util.PriorityQueue;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class AlgoTopPHUI {

    // variable for statistics
    public double maxMemory = 0; // the maximum memory usage
    public long startTimestamp = 0; // the time the algorithm started
    public long startTimestampPha2 = 0;
    public long endTimestamp = 0; // the time the algorithm terminated
    public int PHUIsCount = 0; // the number of HUI generated
    public long candidateCount = 0;


    /**
     * Map to remember the TWU, support and largest periodicity of single item
     */
    Map<Integer, ItemInfo> mapItemToItemInfo;

    long minUtility = 0;

    boolean debug;//for debug
    int topkstatic;// for top-k
    int minPeriodicity;  //minPer
    int maxPeriodicity;  //maxPer
    int minAveragePeriodicity; //minAvg
    int maxAveragePeriodicity;  //maxAvg

    public AlgoTopPHUI() {}

    public AlgoTopPHUI(int top, int minPer, int maxPer, int minAvg, int maxAvg, boolean debug) {
        this.topkstatic = top;
        this.minPeriodicity = minPer;
        this.maxPeriodicity = maxPer;
        this.minAveragePeriodicity = minAvg;
        this.maxAveragePeriodicity = maxAvg;
        this.debug = debug;
        if (debug) {
            System.out.println("topkstatic = ： " + topkstatic);
            /*System.out.println("minPeriodicity的值为： " + minPeriodicity);
            System.out.println("maxPeriodicity的值为： " + maxPeriodicity);
            System.out.println("minAveragePeriodicity的值为： " + minAveragePeriodicity);
            System.out.println("maxAveragePeriodicity的值为： " + maxAveragePeriodicity);
            System.out.println("debug的值为： " + debug);*/
        }
    }

    //the number of items that the itemSets should contain
    int minimumLength;
    int maximumLength;

    // writer to write the output file
    BufferedWriter writer = null;

    PriorityQueue<Pattern> kPatterns = new PriorityQueue<Pattern>();

    final int BUFFERS_SIZE = 200;
    private int[] itemsetBuffer = null;


    /**
     * mapFMAP is a Threshold Raising Strategy used in second scan the database.
     * The mapFMAP structure: [key: item a , [key: another item b , value: Item)]] ,where the Item : [TWU(ab),U(ab)]
     * where the itemSets ab should firstly satisfy the condition of the periodicity
     */
    Map<Integer, Map<Integer, Item>> mapFMAP = null;

    //===================== FHN ===========================
    Set<Integer> negativeItems = null;
    //====================================================

    long piuRaiseValue = 0;
    long PCUDRaiseValue = 0;

    boolean EUCS_PRUNE = false;

    /**
     * the database size (number of transactions)
     */
    int databaseSize = 0;
    double supportPruningThreshold = 0;
    double supportPruningThresholdminper = 0;


    /**
     * the class Pair
     * 属性：item , utility
     * 方法：toString() , 返回值为  [item,utility]
     */
    class Pair {
        int item;
        int utility;

        Pair(int item, int utility) {
            this.item = item;
            this.utility = utility;
        }

        public String toString() {
            return "[" + item + "," + utility + "]";
        }
    }

    //ItemInfo:  support, twu, largestPeriodicity, smallestPeriodicity, lastSeenTransaction of single item
    class ItemInfo {
        int support = 0;
        Integer twu = 0;
        int largestPeriodicity = 0;
        int smallestPeriodicity = Integer.MAX_VALUE;
        int lastSeenTransaction = 0;
    }


    String inputFile;

    /**
     * Run the algorithm
     *
     * @param input：
     * @param output：
     * @param eucsPrune：
     * @throws IOException
     */
    public void runAlgorithm(String input, String output, boolean eucsPrune) throws IOException {

        //System.out.println("======== Start Algorithm TopK New =========");
        maxMemory = 0;
        itemsetBuffer = new int[BUFFERS_SIZE];
        this.EUCS_PRUNE = eucsPrune;

        /**
         * PIU is a Threshold Raising Strategy , which is an improved version of RIU , used in first scan the database
         * The PIU structure: [key: item a , value: U(a)] ,
         * where the item a should firstly satisfy the condition of the periodicity
         */
        Map<Integer, Long> PIU = new HashMap<Integer, Long>();

        //===================== FHN ===========================
        negativeItems = new HashSet<Integer>();
        //====================================================

        inputFile = input;
        if (EUCS_PRUNE)
            mapFMAP = new HashMap<Integer, Map<Integer, Item>>();

        startTimestamp = System.currentTimeMillis();
        writer = new BufferedWriter(new FileWriter(output));

        // We create a map to store the information of each item
        mapItemToItemInfo = new HashMap<Integer, ItemInfo>();

        BufferedReader myInput = null;
        databaseSize = 0;
        String thisLine;
        long sumOfTransactionLength = 0;  // for debugging

        // first scan the database
        try {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
            while ((thisLine = myInput.readLine()) != null) {
                if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#'
                        || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                    continue;
                }

                databaseSize++;
                String[] split = thisLine.split(":");
                String[] items = split[0].split(" ");
                String[] utilityValues = split[2].split(" ");
                int transactionUtility = Integer.parseInt(split[1]);
                sumOfTransactionLength += items.length;

                for (int i = 0; i < items.length; i++) {
                    Integer item = Integer.parseInt(items[i]);
                    Integer itemUtility = Integer.parseInt(utilityValues[i]);
                    //================== FHN ====================
                    if (itemUtility < 0) {
                        negativeItems.add(item);
                    } else {
                        // set the PIU value.  the structure of the PIU :   [a , U(a)]
                        Long real = PIU.get(item);
                        real = (real == null) ? itemUtility : itemUtility + real;
                        PIU.put(item, real);
                    }
                    //================== FHN ====================

                    // ItemInfo:  support, largestPeriodicity, smallestPeriodicity, lastSeenTransaction
                    // we also add 1 to the support of the item
                    ItemInfo itemInfo = mapItemToItemInfo.get(item);
                    if (itemInfo == null) {
                        itemInfo = new ItemInfo();
                        mapItemToItemInfo.put(item, itemInfo);
                        itemInfo.twu = transactionUtility;
                    } else {
                        //calculate the twu of single item
                        itemInfo.twu += transactionUtility;
                    }
                    // increase support
                    itemInfo.support++;


                    // **** PHM ***********
                    // calculate periodicity
                    int periodicity = databaseSize - itemInfo.lastSeenTransaction;
                    // update periodicity of this item
                    if (itemInfo.largestPeriodicity < periodicity) {
                        itemInfo.largestPeriodicity = periodicity;
                    }
                    itemInfo.lastSeenTransaction = databaseSize;

                    //IF IT IS not the first time that we see the item, we update its minimum periodicity
                    if (itemInfo.support != 1 && periodicity < itemInfo.smallestPeriodicity) {
                        itemInfo.smallestPeriodicity = periodicity;
                    }
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                myInput.close();
            }
        }


        supportPruningThreshold = (((double) databaseSize) / ((double) maxAveragePeriodicity)) - 1d;
        supportPruningThresholdminper = (((double) databaseSize) / ((double) minAveragePeriodicity)) - 1d;


        // **** PHM ***********
        for (Entry<Integer, ItemInfo> entry : mapItemToItemInfo.entrySet()) {
            ItemInfo itemInfo = entry.getValue();

            // calculate the last period
            int periodicity = databaseSize - itemInfo.lastSeenTransaction;

            // update periodicity of this item
            if (itemInfo.largestPeriodicity < periodicity) {
                itemInfo.largestPeriodicity = periodicity;
            }
            /*if (entry.getKey()==1043) {
                System.out.println("=====================");
                System.out.println(" item : " + entry.getKey()
                        + "\tavgPer: " + (databaseSize / (double) (itemInfo.support + 1))
                        + "\tminPer: " + itemInfo.smallestPeriodicity
                        + "\tmaxPer: " + itemInfo.largestPeriodicity
                        + "\tTWU: " + itemInfo.twu
                        + "\tsup.: " + itemInfo.support
                );
            }*/
        }
        //Up to now, the first database scan is complete and the ItemInfo of single item was calculated.

        if (debug) {
            System.out.println("Number of transactions : " + databaseSize);
            System.out.println("Average transaction length : " + sumOfTransactionLength / (double) databaseSize);
            System.out.println("Number of items : " + mapItemToItemInfo.size());
            System.out.println("Average pruning threshold  (|D| / maxAvg $) - 1): " + supportPruningThreshold);
        }

        // Raising threshold by real item utility
        raisingThresholdPIU(PIU, topkstatic);

        if (debug) {
            System.out.println("raising PIU: " + minUtility + ", top-k: " + topkstatic + ", item count: " + mapItemToItemInfo.keySet().size());
            piuRaiseValue = minUtility;
        }

        List<UtilityList> listOfUtilityLists = new ArrayList<UtilityList>();
        Map<Integer, UtilityList> mapItemToUtilityList = new HashMap<Integer, UtilityList>();
        for (Integer item : mapItemToItemInfo.keySet()) {
            // if the item is promising  (TWU >= minutility)  and is frequent
            ItemInfo itemInfo = mapItemToItemInfo.get(item);
            if (itemInfo.support >= supportPruningThreshold && // satisfy the maximum average period prune condition
                    itemInfo.largestPeriodicity <= maxPeriodicity && //satisfy the maximum period prune condition
                    itemInfo.twu >= minUtility) { //satisfy the TWU prune condition

                // create an empty Utility List that we will fill later.
                UtilityList uList = new UtilityList(item);
                mapItemToUtilityList.put(item, uList);
                // add the item to the list of high TWU items
                listOfUtilityLists.add(uList);
                ///*************** PHM ****************
                // set the periodicity
                uList.largestPeriodicity = itemInfo.largestPeriodicity;
                uList.smallestPeriodicity = itemInfo.smallestPeriodicity;
                ///*************** END PHM ****************
            }
        }

        //Collections.sort(listOfUtilityLists, new UtilComparator());
        Collections.sort(listOfUtilityLists, new Comparator<UtilityList>() {
            public int compare(UtilityList o1, UtilityList o2) {
                // compare the TWU of the items
                return compareItems(o1.item, o2.item);
            }
        });

        /*if (debug) {
            System.out.println("排序规则，负项在前，正项在后，TWU升序排列:");
            for (UtilityList listOfUtilityList : listOfUtilityLists) {
                System.out.print(listOfUtilityList.item + ":");
                System.out.print("twu(" + listOfUtilityList.item + ") = " + mapItemToItemInfo.get(listOfUtilityList.item).twu + ",");
            }
            System.out.println();
            System.out.println("----------按照TWU排序完毕----------");
        }*/

        //second scan the database
        try {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
            int tid = 0;
            while ((thisLine = myInput.readLine()) != null) {

                if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#'
                        || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@') {
                    continue;
                }

                String[] split = thisLine.split(":");
                String[] items = split[0].split(" ");
                String[] utilityValues = split[2].split(" ");
                int remainingUtility = 0;
                long newTWU = 0; // NEW OPTIMIZATION

                List<Pair> revisedTransaction = new ArrayList<Pair>();
                for (int i = 0; i < items.length; i++) {
                    // Pair(int item, int utility) {   }
                    Pair pair = new Pair(Integer.parseInt(items[i]), Integer.parseInt(utilityValues[i]));
                    ItemInfo itemInfo = mapItemToItemInfo.get(pair.item);
                    // if the item has enough utility
                    if (itemInfo.support >= supportPruningThreshold &&
                            itemInfo.largestPeriodicity <= maxPeriodicity &&
                            itemInfo.twu >= minUtility) {
                        // add it
                        revisedTransaction.add(pair);
                        // only consider the  positive item.
                        if (!negativeItems.contains(pair.item)) {
                            remainingUtility += pair.utility;
                            newTWU += pair.utility; // NEW OPTIMIZATION
                        }
                    }
                }

                // sort the transaction
                Collections.sort(revisedTransaction, new Comparator<Pair>() {
                    public int compare(Pair o1, Pair o2) {
                        return compareItems(o1.item, o2.item);
                    }
                });

                //System.out.println("Tid: "+ (tid+1) +", "+"revisedTransaction: "+ revisedTransaction);

                for (int i = 0; i < revisedTransaction.size(); i++) {
                    Pair pair = revisedTransaction.get(i);

                    // subtract the utility of this item from the remaining utility
                    // ================ FHN (MODIF) ==================
                    // if not a negative item
                    if ((pair.utility > 0) && remainingUtility != 0) {
                        //================================================
                        remainingUtility = remainingUtility - pair.utility;
                    }

                    //System.out.println("test= "+pair.item +",  util: "+pair.utility +",  remain-util: "+remainingUtility);

                    UtilityList utilityListOfItem = mapItemToUtilityList.get(pair.item);        // item = UtilityList
                    // Add a new Element to the utility list of this item corresponding to this transaction
                    if (pair.utility > 0) {  //[tid,pu,nu,ru]
                        Element element = new Element(tid, pair.utility, 0, remainingUtility);
                        utilityListOfItem.addElement(element);// 所有行加起来就是该项的效用列表
                    } else {
                        Element element = new Element(tid, 0, pair.utility, remainingUtility);
                        utilityListOfItem.addElement(element);
                    }


                    /*
                     * i represents the index of the ith item in the revisedTransaction
                     * pair represents the ith item in the revisedTransaction, which is processed line by line here
                     * newTWU represent the TU of the revisedTransaction
                     * tid : start with 0 , for calculate the period of the 2-itemSets
                     * 这里构建了EUCS结构
                     * */
                    if (EUCS_PRUNE)
                        // 这里已经引入周期性约束条件
                        updateEUCSprune(i, pair, revisedTransaction, newTWU, tid);


                }

                tid++;// increase tid number for next transaction 控制遍历到哪行事务
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                myInput.close();
            }
        }

        // calculate the last period of 2-itemSets.
        for (Entry<Integer, Map<Integer, Item>> entry1 : mapFMAP.entrySet()) {
            Map<Integer, Item> entry2 = entry1.getValue();
            for (Entry<Integer, Item> entry3 : entry2.entrySet()) {
                Item value = entry3.getValue();
                int period = databaseSize - value.lastSeenTransaction;
                if (value.largestPeriodicity < period) {
                    value.largestPeriodicity = period;
                }
                /*if (debug) {
                    System.out.println(" 2-itemSets : " + "[" + entry1.getKey() + " " + entry3.getKey() + "]"
                            + "\tavgPer: " + (databaseSize / (double) (value.support + 1))
                            + "\tminPer: " + value.smallestPeriodicity
                            + "\tmaxPer: " + value.largestPeriodicity
                            + "\tTWU: " + value.twu
                            + "\tsup.: " + value.support
                            + "\tUtility.: " + value.utility
                    );
                }*/
            }
        }


        if (EUCS_PRUNE) {
            removeEntry(); // remove the itemSets that not satisfy the twu pruning, maxPer pruning and avgPer pruning.
        }


        PIU.clear();

        startTimestampPha2 = System.currentTimeMillis();

        // **** Release the memory for the maps ****
        mapItemToItemInfo = null;

        mapItemToUtilityList = null;

        checkMemory();
        // call the THUI mining function

        thui(itemsetBuffer, 0, null, listOfUtilityLists);
        checkMemory();

        writeResultTofile();
        writer.close();

        endTimestamp = System.currentTimeMillis();
        kPatterns.clear();
    }

    /**
     * update EUCS prune
     *
     * @param i                  : the index of the ith item in the revisedTransaction
     * @param pair               : the ith item in the revisedTransaction, which is processed line by line here
     * @param revisedTransaction :
     * @param newTWU             : the TU of the revisedTransaction
     * @param tid                : start with 0 , for calculate the period of the 2-itemSets
     */
    public void updateEUCSprune(int i, Pair pair, List<Pair> revisedTransaction, long newTWU, int tid) {
        int Tid = tid + 1; // because the tid start with 0, in order to calculate the period, we should start with 1.
        int period = 0;

        Map<Integer, Item> mapFMAPItem = mapFMAP.get(pair.item);
        if (mapFMAPItem == null) {
            mapFMAPItem = new HashMap<Integer, Item>();
            mapFMAP.put(pair.item, mapFMAPItem);
        }

        for (int j = i + 1; j < revisedTransaction.size(); j++) {
            if (pair.item == revisedTransaction.get(j).item)
                continue;//kosarak dataset has duplicate items
            Pair pairAfter = revisedTransaction.get(j);
            Item twuItem = mapFMAPItem.get(pairAfter.item);
            if (twuItem == null) {
                twuItem = new Item();
            }

            twuItem.support++;
            // calculate the period of the 2-itemSets.
            period = Tid - twuItem.lastSeenTransaction;
            // update the largestPeriodicity
            if (twuItem.largestPeriodicity < period) {
                twuItem.largestPeriodicity = period;
            }
            // if it is not the first time that we see this 2-itemSets, we update the minimum period of this itemSets.
            if (twuItem.support != 1 && period < twuItem.smallestPeriodicity) {
                twuItem.smallestPeriodicity = period;
            }
            twuItem.twu += newTWU;
            twuItem.utility += (long) pair.utility + pairAfter.utility;
            twuItem.lastSeenTransaction = Tid;
            mapFMAPItem.put(pairAfter.item, twuItem);
        }
    }


    /**
     * Method to compare items by their TWU
     *
     * @param item1 an item
     * @param item2 another item
     * @return 0 if the same item, >0 if item1 is larger than item2,  <0 otherwise
     */
    private int compareItems(int item1, int item2) {
        //====================== FHN =======================
        Boolean item1IsNegative = negativeItems.contains(item1);
        Boolean item2IsNegative = negativeItems.contains(item2);
        if (!item1IsNegative && item2IsNegative) {
            return 1;
        } else if (item1IsNegative && !item2IsNegative) {
            return -1;
        }
        //=============================================

//		// used the TWU-ascending order; if the same, use the lexical order
        int compare = mapItemToItemInfo.get(item1).twu - mapItemToItemInfo.get(item2).twu;
        return (compare == 0) ? item1 - item2 : compare;

        // used the TWU-descending order; if the same, use the lexical order
//		int compare = mapItemToTWU.get(item2) - mapItemToTWU.get(item1);
//		return (compare == 0) ? item1 - item2 : compare;
    }

//	/**
//	 * The THUI algorithm
//	 *
//	 * @param prefix
//	 * @param prefixLength
//	 * @param pUL
//	 * @param ULs
//	 * @throws IOException
//	 */

    int mmm = 0;
    private void thui(int[] prefix, int prefixLength, UtilityList pUL, List<UtilityList> ULs) throws IOException {
        // thui(itemsetBuffer, 0, null, listOfUtilityLists);

        int patternSize = prefixLength + 1;

        for (int i = 0; i < ULs.size(); i++) {
            //last item is a single item, and hence no extension
            checkMemory();
            UtilityList X = ULs.get(i);

            // If the sum of the remaining utilities for pX
            // is higher than minUtility, we explore extensions of pX.
            // (this is the pruning condition)

            double averagePeriodicity = (double) databaseSize / ((double) X.getSupport() + 1);

            if (X.getUtils() >= minUtility
                    && averagePeriodicity <= maxAveragePeriodicity
                    && averagePeriodicity >= minAveragePeriodicity
                    && X.smallestPeriodicity >= minPeriodicity
                    && X.largestPeriodicity <= maxPeriodicity
            ) {
                if (patternSize >= minimumLength && patternSize <= maximumLength) {
                    save(prefix, prefixLength, X, averagePeriodicity, X.smallestPeriodicity, X.largestPeriodicity);
                }
            }
            //if(X.sumIPutils + X.sumRutils >= minUtility && X.sumIPutils + X.sumINutils > 0){
            if (X.sumIPutils + X.sumRutils >= minUtility) {
                if (patternSize < maximumLength) {
                    //the utility value of zero cases can be safely ignored, as it is unlikely to generate a HUI;
                    // besides the lowest min utility will be 1
                    if (EUCS_PRUNE) {
                        Map<Integer, Item> mapTWUF = mapFMAP.get(X.item);
                        if (mapTWUF == null)
                            continue;
                    }

                    //System.out.println("item= "+ X.item +", util: "+X.getUtils());

                    List<UtilityList> exULs = new ArrayList<UtilityList>();
                    for (int j = i + 1; j < ULs.size(); j++) {
                        UtilityList Y = ULs.get(j);

                        if (EUCS_PRUNE) {
                            Map<Integer, Item> mapTWUF = mapFMAP.get(X.item);
                            if (mapTWUF != null) {
                                Item twuItem = mapTWUF.get(Y.item);
                                if (twuItem != null) {
                                    Long twuF = twuItem.twu;
                                    if (twuF == null || twuF < minUtility) {
                                        continue;
                                    }
                                    int supportF = twuItem.support;
                                    if (supportF < supportPruningThreshold) {
                                        continue;
                                    }
                                }
                            }
                        }

                        candidateCount++;

                        // call the function
                        UtilityList exul = construct2(pUL, X, Y);

                        if (exul != null)
                            exULs.add(exul);

                    }

                    prefix[prefixLength] = X.item;
                    //call the THUI function
                    thui(prefix, prefixLength + 1, X, exULs);
                }
            }
        }
    }

    /**
     * This method constructs the utility list of pXY
     *
     * @param P  :  the utility list of prefix P.
     * @param px : the utility list of pX
     * @param py : the utility list of pY
     *           minUtility : the minimum utility threshold
     * @return the utility list of pXY
     */

    private UtilityList construct2(UtilityList P, UtilityList px, UtilityList py) {
        // create an empty utility list for pXY
        UtilityList pxyUL = new UtilityList(py.item);

        int lastTid = -1;  // IMPORTANT BECAUSE TIDS STARTS AT ZERO...!!
        //== new optimization - LA-prune  == /
        // Initialize the sum of total utility
        long totalUtility = px.sumIPutils + px.sumRutils;
        // ================================================
        long totalSupport = px.getSupport();
        // for each element in the utility list of pX
        for (Element ex : px.elements) {
            // do a binary search to find element ey in py with tid = ex.tid
            Element ey = findElementWithTID(py, ex.tid);
            if (ey == null) {
                //== new optimization - LA-prune == /
                totalUtility -= (ex.iputils + ex.rutils);
                if (totalUtility < minUtility) {
                    return null;
                }
                // decrease the support by one transaction
                totalSupport -= 1;
                if (totalSupport < supportPruningThreshold) {
                    return null;
                }
                // =============================================== /
                continue;
            }
            // if the prefix p is null
            if (P == null) {
                // ********** PHM *************
                // check the periodicity
                int periodicity = ex.tid - lastTid;

                if (periodicity > maxPeriodicity) {
                    return null;
                }
                if (periodicity >= pxyUL.largestPeriodicity) {
                    pxyUL.largestPeriodicity = periodicity;
                }
                lastTid = ex.tid;

                // IMPORTANT DO NOT COUNT THE FIRST PERIOD FOR MINIMUM UTILITY
                if (pxyUL.elements.size() > 0 && periodicity < pxyUL.smallestPeriodicity) {
                    pxyUL.smallestPeriodicity = periodicity;
                }
                // Create the new element
                Element eXY = new Element(ex.tid, ex.iputils + ey.iputils, ex.inutils + ey.inutils, ey.rutils);
                // add the new element to the utility list of pXY
                pxyUL.addElement(eXY);

            } else {
                // find the element in the utility list of p wih the same tid
                Element e = findElementWithTID(P, ex.tid);
                if (e != null) {
                    // ********** PHM *************
                    // check the periodicity
                    int periodicity = ex.tid - lastTid;
                    if (periodicity > maxPeriodicity) {
                        return null;
                    }
                    if (periodicity >= pxyUL.largestPeriodicity) {
                        pxyUL.largestPeriodicity = periodicity;
                    }
                    lastTid = ex.tid;

                    // IMPORTANT DO NOT COUNT THE FIRST PERIOD FOR MINIMUM UTILITY
                    if (pxyUL.elements.size() > 0 && periodicity < pxyUL.smallestPeriodicity) {
                        pxyUL.smallestPeriodicity = periodicity;
                    }
                    // ********** END PHM *************

                    // Create new element
                    Element eXY = new Element(ex.tid, ex.iputils + ey.iputils - e.iputils,
                            ex.inutils + ey.inutils - e.inutils,
                            ey.rutils);
                    // add the new element to the utility list of pXY
                    pxyUL.addElement(eXY);
                }
            }
        }
        // ********** PHM *************
        // check the periodicity
        int periodicity = (databaseSize - 1) - lastTid;  // Need -1 because tids starts at zero
//		if(P==null && px.item == 4 && py.item == 2){
//			System.out.println("period : " + periodicity);
//		}

        if (periodicity > maxPeriodicity) {
            return null;
        }
        if (periodicity >= pxyUL.largestPeriodicity) {
            pxyUL.largestPeriodicity = periodicity;
        }

        if (pxyUL.getSupport() < supportPruningThreshold) {
            return null;
        }

        // WE DO NOT UPDATE THE MINIMUM PERIOD
//		if(pxyUL.smallestPeriodicity > maxAveragePeriodicity){
//			return null;
//		}

        // return the utility list of pXY.
        return pxyUL;
    }


    /**
     * Do a binary search to find the element with a given tid in a utility list
	 * @param ulist the utility list
	 * @param tid  the tid
	 * @return  the element or null if none has the tid.
	 */
    private Element findElementWithTID(UtilityList ulist, int tid) {
        List<Element> list = ulist.elements;

        // perform a binary search to check if  the subset appears in  level k-1.
        int first = 0;
        int last = list.size() - 1;

        // the binary search
        while (first <= last) {
            int middle = (first + last) >>> 1; // divide by 2

            if (list.get(middle).tid < tid) {
                first = middle + 1;  //  the itemset compared is larger than the subset according to the lexical order
            } else if (list.get(middle).tid > tid) {
                last = middle - 1; //  the itemset compared is smaller than the subset  is smaller according to the lexical order
            } else {
                return list.get(middle);
            }
        }
        return null;
    }


    /**
	 * Write result to file
	 *
	 * @throws IOException
	 */
    public void writeResultTofile() throws IOException {

        if (kPatterns.size() == 0) {
            return;
        }
        List<Pattern> lp = new ArrayList<Pattern>();
        do {
            PHUIsCount++;
            Pattern pattern = kPatterns.poll();

            lp.add(pattern);
        } while (kPatterns.size() > 0);

//		Collections.sort(lp, new Comparator<Pattern>() {
//			public int compare(Pattern o1, Pattern o2) {
//				return comparePatterns(o1, o2);
//				// return comparePatternsIdx(o1, o2);
//			}
//		});

        for (Pattern pattern : lp) {
            StringBuilder buffer = new StringBuilder();

            buffer.append(pattern.prefix.toString());
            // write utility
            buffer.append(" #UTIL: ");
            buffer.append(pattern.utility);
            // write support
            //buffer.append(",  #sup: " + pattern.sup);

            buffer.append(", #minPer: " + pattern.minPeriodicity);
            buffer.append(", #maxPer: " + pattern.maxPeriodicity);
            buffer.append(", #avgPer: " + pattern.averagePeriodicity);
            writer.write(buffer.toString());
            writer.newLine();
        }

        writer.close();
    }


//	/**
//	 * compare patterns
//	 *
//	 * @param item1
//	 * @param item2
//	 * @return
//	 */
//	private int comparePatterns(Pattern item1, Pattern item2) {
//		//int compare = (int) (Integer.parseInt(item1.split(" ")[0]) - Integer.parseInt(item2.split(" ")[0]));
//		int i1 = (int)Integer.parseInt(item1.prefix.split(" ")[0]);
//		int i2 = (int)Integer.parseInt(item2.prefix.split(" ")[0]);
//		
//		int compare = (int) (mapItemToTWU.get(i1) - mapItemToTWU.get(i2));
//		return compare;
//	}

//	private int comparePatternsIdx(Pattern item1, Pattern item2) {
//		int compare = item1.idx - item2.idx;
//		return compare;
//	}
//
//	private double getObjectSize(Object object) throws IOException {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		ObjectOutputStream oos = new ObjectOutputStream(baos);
//		oos.writeObject(object);
//		oos.close();
//		double maxMemory = baos.size() / 1024d / 1024d;
//		return maxMemory;
//	}


    /**
     * raising threshold PIU
     * Saved in PIU are single positive items and their utility values in the database.
     * For example, [a , U(a)] , can be used as a threshold raising strategy
     * when the number of positive items in the database is greater than or equal to topK
     *
     * @param map ,which is PIU . Map<Integer, Long> PIU = new HashMap<Integer, Long>();
     * @param k   topK
     */
    public void raisingThresholdPIU(Map<Integer, Long> map, int k) {

        List<Map.Entry<Integer, Long>> list = new LinkedList<Map.Entry<Integer, Long>>(map.entrySet());
        int count = 0;// record the number of item that satisfy the period.
        // Since the list is already ordered, as long as the value of count is k,
        //    it means that the utility value of the current item is the utility value of the topK item that satisfies periodicity

        // desc
        Collections.sort(list, new Comparator<Map.Entry<Integer, Long>>() {
            public int compare(Map.Entry<Integer, Long> value1, Map.Entry<Integer, Long> value2) {
                return (value2.getValue()).compareTo(value1.getValue());
            }
        });

        System.out.println(" supportPruningThreshold : " + supportPruningThreshold
                + "\tsupportPruningThresholdminper: " + supportPruningThresholdminper
        );
        for (int i = 0; i < list.size(); i++) {
            ItemInfo itemInfo = mapItemToItemInfo.get(list.get(i).getKey());
            if (itemInfo.support >= supportPruningThreshold &&
                    itemInfo.support <= supportPruningThresholdminper &&
                    itemInfo.largestPeriodicity <= maxPeriodicity &&
                    itemInfo.smallestPeriodicity >= minPeriodicity) {
                count++;
                // raising threshold minUtility
                if (count == k) {
                    minUtility = list.get(i).getValue();
                    break;
                }
            }
            /*if (debug) {
                System.out.println(" item : " + list.get(i).getKey()
                        + "\tsupport: " + itemInfo.support
                        + "\tminPer: " + itemInfo.smallestPeriodicity
                        + "\tmaxPer: " + itemInfo.largestPeriodicity
                        + "\tutility: " + list.get(i).getValue()
                );
            }*/
        }
        list = null;
    }


    /**
     * Remove entry
     */
    private void removeEntry() {
        for (Entry<Integer, Map<Integer, Item>> entry : mapFMAP.entrySet()) {
            for (Iterator<Map.Entry<Integer, Item>> it = entry.getValue().entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, Item> entry2 = it.next();
                Item itemSetInfo = entry2.getValue();
                if (itemSetInfo.twu < minUtility ||  // remove the itemSets that not satisfy the twu pruning, maxPer pruning and avgPer pruning.
                        itemSetInfo.largestPeriodicity > maxPeriodicity ||
                        itemSetInfo.support < supportPruningThreshold) {
                    it.remove();
                }
            }
        }
    }



    /**
     * Save function
     *
     * @param prefix
     * @param length
     * @param X
     */
    private void save(int[] prefix, int length, UtilityList X, double averagePeriodicity, double minPeriodicity, double maxPeriodicity) {
        kPatterns.add(new Pattern(prefix, length, X, candidateCount, averagePeriodicity, minPeriodicity, maxPeriodicity));

        if (kPatterns.size() > topkstatic) {
            if (X.getUtils() >= minUtility) {
                do {
                    kPatterns.poll();
                } while (kPatterns.size() > topkstatic);
            }

            minUtility = kPatterns.peek().utility;
        }
    }

    /**
     * Check memory
     * 检查当前程序的内存使用情况，并更新最大内存使用量。
     */
    private void checkMemory() {
        double currentMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024d / 1024d;
        if (currentMemory > maxMemory) {
            maxMemory = currentMemory;
        }
    }

    public void printStats() throws IOException {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.00");
        System.out.println("=============  TopPHUIs v1.0 - STATS =============");

        System.out.println(" Total time: " + (endTimestamp - startTimestamp) / 1000.0 + " s");
        System.out.println(" Max memory: " + df.format(maxMemory) + " MB");

        System.out.println(" PHUIs count: " + PHUIsCount);
        System.out.println(" Candidates: " + candidateCount);
        System.out.println(" Final minimum utility: " + minUtility);

        System.out.println("minPeriodicity: " + minPeriodicity);
        System.out.println("maxPeriodicity: " + maxPeriodicity);
        System.out.println("minAveragePeriodicity: " + minAveragePeriodicity);
        System.out.println("maxAveragePeriodicity: " + maxAveragePeriodicity);

        //print the name of the input file
        File f = new File(inputFile);
        String tmp = f.getName();
        tmp = tmp.substring(0, tmp.lastIndexOf('.'));
        System.out.println(" Dataset: " + tmp);

        //get the current time of the computer
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
        System.out.println(" End time " + timeStamp);
        System.out.println("==========================================");

    }

    /**
     * This method let the user specify a minimum length for patterns to be found.
     * By the default, the minimum length is 1.
     *
     * @param minimumLength an integer indicating a minimum number of items
     */
    public void setMinimumLength(int minimumLength) {
        this.minimumLength = minimumLength;
    }

    public void setMaximumLength(int maximumLength) {
        this.maximumLength = maximumLength;
    }


}
