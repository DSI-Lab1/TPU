import java.io.IOException;


public class MainTestTopPHUI {
    public static void main(String[] args) throws IOException {

        //String input = "src/data_test.txt";
        //String input = "src/Chess_negative.txt";
        //String input = "src/accidents_negative.txt";
        //String input = "src/mushroom_negative.txt";
        String input = "src/retail_negative.txt";
        //String input = "src/pumsb_negative.txt";
        //String input = "src/kosarak_negative.txt";


        String output = ".//output.txt";



        int topN = 10;
        int minPeriodicity = 1;  // minimum periodicity parameter (a number of transactions)
        int maxPeriodicity = 5000;  // maximum periodicity parameter (a number of transactions)
        int minAveragePeriodicity = 5;  // minimum average periodicity (a number of transactions)
        int maxAveragePeriodicity = 500;  // maximum average periodicity (a number of transactions)
        boolean debug = true; //for debug the correct of the code

        //the number of items that itemSets should contain
        int minimumLength = 1;
        int maximumLength = Integer.MAX_VALUE;

        boolean eucsPrune = true;

        AlgoTopPHUI topk = new AlgoTopPHUI(topN, minPeriodicity, maxPeriodicity, minAveragePeriodicity, maxAveragePeriodicity, debug);

        topk.setMinimumLength(minimumLength);
        topk.setMaximumLength(maximumLength);

        topk.runAlgorithm(input, output, eucsPrune);
        topk.printStats();
    }

}
