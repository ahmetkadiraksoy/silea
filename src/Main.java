import java.io.File;
import java.util.*;

public class Main {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    public static void main (String[] args) {
        ////////////////
        // Parameters //
        ////////////////
        String input_filename = ""; // train or test file path
        String ga_train_input_filename = "";
        String ga_test_input_filename = "";
        String model_filename = "";
        ArrayList<Integer> quantization_levels = new ArrayList<>(); // holds quantization levels, it is either empty, or size=1 or size=attributesize
        int no_of_conditions = 1; // beginning no. of conditions. user can change
        int mode = 0; // 0 default, 1 train, 2 test
        int error = -1;
        int population_size = 50;
        int max_threads =  Runtime.getRuntime().availableProcessors() - 1;
        boolean ga_on = false;
        boolean verbose = false;
        int generation = 5;
        double ga_train_portion = 0.1;
        SILEA silea = new SILEA();

        //////////////////////////////////
        // Get attributes from the user //
        //////////////////////////////////
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i":
                case "--input":  // input filename
                    input_filename = args[i + 1];
                    i++;
                    break;
                case "-g":
                case "--generation":
                    generation = Integer.parseInt(args[i + 1]);
                    i++;
                    break;
                case "-gp":
                case "--ga-portion":
                    ga_train_portion = Double.parseDouble(args[i + 1]);
                    i++;
                    break;
                case "-p":
                case "--population":
                    population_size = Integer.parseInt(args[i + 1]);
                    i++;
                    break;
                case "-c":
                case "--condition":  // condition no
                    no_of_conditions = Integer.parseInt(args[i + 1]);
                    i++;
                    break;
                case "-e":
                case "--error":  // enable partial match
                    error = Integer.parseInt(args[i + 1]);
                    i++;
                    break;
                case "-m":
                case "--model-filename":
                    model_filename = args[i + 1];
                    i++;
                    break;
                case "-v":
                case "--verbose":
                    verbose = true;
                    break;
                case "-mt":
                    max_threads = Integer.parseInt(args[i + 1]);
                    i++;
                    break;
                case "-ga":
                    ga_on = true;
                    break;
                case "-q":
                case "--quantization":
                    String[] temp = args[i + 1].split(",");

                    for (String s : temp) quantization_levels.add(Integer.parseInt(s));

                    i++;
                    break;
                case "-t":
                case "--train":
                    mode = 1;
                    break;
                case "-T":
                case "--test":
                    mode = 2;
                    break;
                default:
                    System.out.println(ANSI_RED + "Unknown parameter '" + args[i] + "' " + ANSI_GREEN + "Type -h to see the help menu." + ANSI_RESET);
                    System.exit(0);
            }
        }

        ga_train_input_filename = "ga_train_" + input_filename.split("/")[input_filename.split("/").length-1];
        ga_test_input_filename = "ga_test_" + input_filename.split("/")[input_filename.split("/").length-1];

        if (quantization_levels.size() == 0)
            quantization_levels.add(0);

        ////////////////////////////////////////////////
        // Read train/test file contents to ArrayList //
        ////////////////////////////////////////////////
        if (mode == 1) { // train
            if (ga_on) {
                while (quantization_levels.size() > 0)
                    quantization_levels.remove(0);

                System.out.println("Running GA...");

                int no_of_attributes = silea.generate_ga_train_test_files(input_filename, ga_train_input_filename, ga_test_input_filename, ga_train_portion);
                String solution = new GA().execute(ga_train_input_filename, ga_test_input_filename, no_of_attributes, quantization_levels, no_of_conditions, model_filename, verbose, error, generation, population_size, max_threads);
                String[] solution_tokens = solution.split("");

                for (int i = 0; i < solution_tokens.length; i+=5) {
                    if (verbose) {
                        System.out.print(solution_tokens[i]);
                        System.out.print(solution_tokens[i + 1]);
                        System.out.print(solution_tokens[i + 2]);
                        System.out.print(solution_tokens[i + 3]);
                        System.out.print(solution_tokens[i + 4]);
                    }

                    int value = 1;
                    if (solution_tokens[i].equals("1"))
                        value += 16;
                    if (solution_tokens[i+1].equals("1"))
                        value += 8;
                    if (solution_tokens[i+2].equals("1"))
                        value += 4;
                    if (solution_tokens[i+3].equals("1"))
                        value += 2;
                    if (solution_tokens[i+4].equals("1"))
                        value += 1;

                    if (verbose)
                        System.out.println(" " + value);

                    if (value == 1)
                        quantization_levels.add(value);
                    else
                        quantization_levels.add(value * 10);
                }

                new File(ga_train_input_filename).delete();
                new File(ga_test_input_filename).delete();
            }

            silea.train(input_filename, quantization_levels, no_of_conditions, model_filename, verbose, false);
        }
        else if (mode == 2) { // test
            double accuracy = silea.test(model_filename, input_filename, error, verbose, false);
        }
        else {
            System.out.println("ERROR! Mode not provided!");
            System.exit(0);
        }
    }
}

class Example {
    int hash;
    String[] tokens;
}

class LinkedList {
    int occurrence = 1;
    String text = "";

    public void increment_occurrence() {
        this.occurrence++;
    }
}

class MapObject {
    int occurrence = 1;
    Map<String, Integer> classes = new HashMap<>();
}

class LinkedListArray {
    ArrayList<LinkedList> list = new ArrayList<>();

    public int size() {
        return list.size();
    }

    public void sort_linkedlist(int j) {
        for (int i = j; i > 0; i--) {
            if (list.get(i).occurrence > list.get(i-1).occurrence) {
                LinkedList temp = list.get(i);
                list.set(i, list.get(i-1));
                list.set(i-1, temp);
            }
        }
    }

    public void add(String text) {
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).text.equals(text)) {
                LinkedList temp = list.get(i);
                temp.increment_occurrence();
                list.set(i, temp);
                sort_linkedlist(i);
                found = true;
                break;
            }
        }

        if (!found) {
            LinkedList temp = new LinkedList();
            temp.text = text;
            list.add(temp);
        }
    }

    public LinkedList get(int i) {
        return list.get(i);
    }
}

class RuleObject {
    String rule_class;
    int occurrence;
    Map<Integer, Integer> classifies = new HashMap<>();
}

class RuleObject2 {
    String[] rule;
    int occurrence;
    Map<Integer, Integer> classifies = new HashMap<>();
}
