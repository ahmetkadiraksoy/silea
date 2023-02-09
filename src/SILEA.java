import java.io.*;
import java.util.*;

public class SILEA {
    public int generate_ga_train_test_files(String input_filename, String ga_train_input_filename, String ga_test_input_filename, double ga_train_portion) {
        int no_of_attributes = 0;

        try {
            BufferedWriter bw1;
            BufferedWriter bw2;
            bw1 = new BufferedWriter(new FileWriter(ga_train_input_filename));
            bw2 = new BufferedWriter(new FileWriter(ga_test_input_filename));

            BufferedReader inputFile = new BufferedReader(new FileReader(input_filename));
            String line;

            // read attribute types
            while (((line = inputFile.readLine()) != null)) {
                if (line.trim().length() != 0 && line.charAt(0) != '%' && line.charAt(0) == '@') {
                    bw1.write(line);
                    bw1.newLine();
                    bw2.write(line);
                    bw2.newLine();

                    String[] tokens = line.split("\\s+");

                    if (tokens[0].equalsIgnoreCase("@attribute"))
                        no_of_attributes++;

                    if (tokens[0].equalsIgnoreCase("@data"))
                        break;
                }
            }

            no_of_attributes--; // remove class

            // read examples
            ArrayList<String> examples = new ArrayList<>();

            // For each line in the input file
            while ((line = inputFile.readLine()) != null)
                if (line.trim().length() != 0 && line.charAt(0) != '%' && line.charAt(0) != '@')
                    examples.add(line);

//            Collections.shuffle(examples);

            int no_of_examples_for_training = (int) Math.ceil(examples.size() * ga_train_portion);
            for (int i = 0; i < no_of_examples_for_training; i++) {
                bw1.write(examples.get(i));
                bw1.newLine();
            }
            for (int i = no_of_examples_for_training; i < examples.size(); i++) {
                bw2.write(examples.get(i));
                bw2.newLine();
            }


            inputFile.close();
            bw1.close();
            bw2.close();
        } catch (IOException e2) {
            e2.printStackTrace();
        }

        return no_of_attributes;
    }

    public void read_model_file(String model_filename, ArrayList<Integer> quantization_levels, ArrayList<ArrayList<Double>> ranges, ArrayList<ArrayList<RuleObject2>> rules) {
        try {
            BufferedReader inputFile = new BufferedReader(new FileReader(model_filename));
            String inputFileCurrentLine;

            // read attribute types
            while (((inputFileCurrentLine = inputFile.readLine()) != null)) {
                if (inputFileCurrentLine.trim().length() != 0) {
                    if (inputFileCurrentLine.equals("***"))
                        break;
                    else
                        quantization_levels.add(Integer.parseInt(inputFileCurrentLine));
                }
            }

            // read ranges
            while (((inputFileCurrentLine = inputFile.readLine()) != null)) {
                if (inputFileCurrentLine.trim().length() != 0) {
                    if (inputFileCurrentLine.equals("***"))
                        break;
                    else {
                        ArrayList<Double> temp = new ArrayList<>();

                        if (inputFileCurrentLine.split(":").length > 1) {
                            String[] temp_array = inputFileCurrentLine.split(":")[1].split(",");

                            for (String s : temp_array) temp.add(Double.parseDouble(s));
                        }

                        ranges.add(temp);
                    }
                }
            }

            // read rules
            ArrayList<RuleObject2> rule = null;
            while (((inputFileCurrentLine = inputFile.readLine()) != null)) {
                if (inputFileCurrentLine.trim().length() != 0) {
                    if (!inputFileCurrentLine.equals("+++")) {
                        if (inputFileCurrentLine.split(":")[0].equals("condition"))
                            rule = new ArrayList<>();
                        else {
                            RuleObject2 temp2 = new RuleObject2();
                            temp2.occurrence = Integer.parseInt(inputFileCurrentLine.split(";")[0]);
                            temp2.rule = inputFileCurrentLine.split(";")[1].split(",");
                            rule.add(temp2);
                        }
                    }
                    else
                        rules.add(rule);
                }
            }

            inputFile.close();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public ArrayList<ArrayList<Integer>> get_nCr(int n, int r) {
        ArrayList<ArrayList<Integer>> combinations = new ArrayList<>();

        int[] res = new int[r];
        for (int i = 0; i < res.length; i++)
            res[i] = i + 1;

        boolean done = false;

        while (!done) {
            ArrayList<Integer> temp = new ArrayList<>();
            for (int i = 0; i < res.length; i++)
                temp.add(res[i]);
            combinations.add(temp);

            done = getNext(res, n, r);
        }

        return combinations;
    }

    public boolean getNext(int[] num, int n, int r) {
        int target = r - 1;
        num[target]++;
        if (num[target] > ((n - (r - target)) + 1)) {
            // Carry the One
            while (num[target] > ((n - (r - target)))) {
                target--;
                if (target < 0) {
                    break;
                }
            }
            if (target < 0) {
                return true;
            }
            num[target]++;
            for (int i = target + 1; i < num.length; i++) {
                num[i] = num[i - 1] + 1;
            }
        }
        return false;
    }

    public double test(String model_filename, String input_filename, int max_error, boolean verbose, boolean ga_on) {
        // parameters
        ArrayList<Integer> quantization_levels = new ArrayList<>();
        ArrayList<ArrayList<Double>> ranges = new ArrayList<>();
        ArrayList<Example> examples = new ArrayList<>();
        ArrayList<ArrayList<RuleObject2>> rules = new ArrayList<>();
        ArrayList<String> attribute_names = new ArrayList<>();
        int no_of_attributes;

        read_model_file(model_filename, quantization_levels, ranges, rules); // read the model

        no_of_attributes = quantization_levels.size(); // get the no of attributes

        // if user did not specify the error rate, set it to the number of attributes
        if (max_error == -1)
            max_error = no_of_attributes-1;

        read_attribute_names(input_filename, attribute_names);

        read_train_test_file(input_filename, examples); // read test file

        if (verbose)
            print_examples(examples);

        // print example size
        if (!ga_on) {
            System.out.println("No of examples read: " + examples.size());
            System.out.println("No of attributes: " + no_of_attributes);
            System.out.println();
            if (verbose)
                print_examples(examples);

            System.out.println("Quantizing...");
        }

        // set ranges
        if (!ga_on)
            System.out.println("Setting ranges...");
        set_ranges(examples, ranges);

        if (!ga_on && verbose) {
            System.out.println();
            print_examples(examples);
        }

        if (!ga_on) {
            System.out.println("Quantization levels:");
            System.out.println(Arrays.toString(quantization_levels.toArray()));
            System.out.println();

            System.out.println("Ranges:");
            for (int i = 0; i < ranges.size(); i++) {
                ArrayList<Double> range = ranges.get(i);
                System.out.print(i + " -> ");
                for (int j = 0; j < range.size(); j++) {
                    System.out.print(range.get(j));
                    if (j < range.size() - 1)
                        System.out.print(",");
                }
                System.out.println();
            }
            System.out.println();

            print_rules(rules, attribute_names);
        }

        // add the position of examples which are not classified yet in a map
        Map<Integer,Integer> examples_unclassified = new HashMap<>();
        for (int i = 0; i < examples.size(); i++)
            examples_unclassified.put(i, -1);

        // classify examples
        Map<String, confusion_matrix> confusion = new HashMap<>();
        int no_of_correctly_classified = 0;
        int no_of_incorrectly_classified = 0;
        int current_error = 0;

        while (examples_unclassified.size() > 0 && current_error <= max_error) {
            if (!ga_on && verbose) {
                System.out.println("Current error: " + current_error);
                System.out.println("======================");
            }

            for (int i = 0; i < examples.size(); i++) { // for each example
                if (examples_unclassified.containsKey(i)) { // if example is not classified
                    boolean is_example_classified = false;
                    String[] example_tokens = examples.get(i).tokens;

                    if (!ga_on && verbose)
                        System.out.println("example: " + Arrays.toString(example_tokens));

                    for (int j = rules.size()-1; j >= current_error; j--) { // for each condition (starting with the largest)
                        if (!ga_on && verbose)
                            System.out.println("\tcondition:" + (j+1));

                        ArrayList<RuleObject2> condition_rules = rules.get(j);

                        for (RuleObject2 condition_rule : condition_rules) { // for each rule
                            int no_of_match = 0;
                            String[] rule_tokens = condition_rule.rule;

                            if (!ga_on && verbose)
                                System.out.println("\t\trule: " + Arrays.toString(rule_tokens));

                            for (int l = 0; l < (rule_tokens.length - 1); l++) { // for each token except for the class
                                int position = Integer.parseInt(rule_tokens[l].split(":")[0]);
                                if (example_tokens[position].equals(rule_tokens[l].split(":")[1]))
                                    no_of_match++;
                            }

                            if (!ga_on && verbose)
                                System.out.println("\t\t\tno_of_match: " + no_of_match + "/" + (rule_tokens.length - 1));

                            if ((no_of_match + current_error) == (rule_tokens.length - 1)) { // if features match
                                examples_unclassified.remove(i);
                                is_example_classified = true;

                                // check if the class name exists in the confusion matrix
                                if (!confusion.containsKey(rule_tokens[rule_tokens.length - 1])) // if doesn't exist
                                    confusion.put(rule_tokens[rule_tokens.length - 1], new confusion_matrix()); // add new one

                                if (rule_tokens[rule_tokens.length - 1].equals(example_tokens[example_tokens.length - 1])) { // if classes match
                                    no_of_correctly_classified++;
                                    confusion.get(rule_tokens[rule_tokens.length - 1]).correct++;
                                    if (!ga_on && verbose)
                                        System.out.println("\t\t\t\texample classified correctly!");
                                } else {
                                    no_of_incorrectly_classified++;
                                    confusion.get(rule_tokens[rule_tokens.length - 1]).incorrect++;
                                    if (!ga_on && verbose)
                                        System.out.println("\t\t\t\texample classified incorrectly!");
                                }
                            }

                            if (is_example_classified)
                                break;
                        }

                        if (is_example_classified)
                            break;
                    }

                    if (!ga_on && verbose)
                        System.out.println();
                }
            }

            current_error++;
        }

        if (!ga_on) {
            System.out.println("Correctly classified: " + no_of_correctly_classified + "/" + examples.size() + " (" + (((double) no_of_correctly_classified / (double) examples.size()) * 100) + "%)");
            System.out.println("Incorrectly classified: " + no_of_incorrectly_classified + "/" + examples.size() + " (" + (((double) no_of_incorrectly_classified / (double) examples.size()) * 100) + "%)");
            System.out.println("Unclassified: " + examples_unclassified.size() + "/" + examples.size() + " (" + (((double) examples_unclassified.size() / (double) examples.size()) * 100) + "%)");
            System.out.println();

            for (Map.Entry<String, confusion_matrix> entry : confusion.entrySet()) {
                String key = entry.getKey();
                confusion_matrix value = entry.getValue();
                double rate = ((double)value.correct / (double)(value.correct + value.incorrect)) * 100;
                System.out.println(key + ": correct: " + value.correct + " incorrect: " + value.incorrect + " rate: " + rate + "%");
            }

            System.out.println();
        }

        return (((double) no_of_correctly_classified / (double) examples.size()) * 100);
    }

    public void read_attribute_names(String input_filename, ArrayList<String> attribute_names) {
        try {
            BufferedReader inputFile = new BufferedReader(new FileReader(input_filename));
            String line;

            // read attribute types
            while (((line = inputFile.readLine()) != null)) {
                if (line.trim().length() != 0 && line.charAt(0) != '%' && line.charAt(0) == '@') {
                    String[] tokens = line.split("\\s+");

                    if (tokens[0].equalsIgnoreCase("@data"))
                        break;

                    if (tokens[0].equalsIgnoreCase("@attribute"))
                        attribute_names.add(tokens[1]); // get attribute name
                }
            }

            inputFile.close();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public void read_parameters(String input_filename, ArrayList<Integer> quantization_levels, ArrayList<String> attribute_types, ArrayList<String> attribute_names) {
        try {
            BufferedReader inputFile = new BufferedReader(new FileReader(input_filename));
            String line;

            // read attribute types
            while (((line = inputFile.readLine()) != null)) {
                if (line.trim().length() != 0 && line.charAt(0) != '%' && line.charAt(0) == '@') {
                    String[] tokens = line.split("\\s+");

                    if (tokens[0].equalsIgnoreCase("@data"))
                        break;

                    if (tokens[0].equalsIgnoreCase("@attribute")) {
                        // set variable type to numeric or string
                        String type = tokens[tokens.length - 1].toLowerCase();
                        if (type.equals("numeric") || type.equals("real"))
                            attribute_types.add("numeric");
                        else
                            attribute_types.add("string");

                        // get attribute name
                        attribute_names.add(tokens[1]);
                    }
                }
            }

            // remove the class from attributes (which is the last entry)
            attribute_types.remove(attribute_types.size()-1);

            // regenerate missing quantization levels
            if (quantization_levels.size() == 1) {
                int x = quantization_levels.get(0);
                for (int i = 0; i < (attribute_types.size()-1); i++)
                    quantization_levels.add(x);
            }

            // Check if correct number of quantization levels are provided
            if (quantization_levels.size() != attribute_types.size()) {
                System.out.println("Number of quantization levels is incorrect!");
                System.exit(0);
            }

            // cancel quantizations for string attributes
            for (int i = 0; i < quantization_levels.size(); i++)
                if (quantization_levels.get(i) > 0 && attribute_types.get(i).equals("string"))
                    quantization_levels.set(i, 0);

            inputFile.close();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public void read_train_test_file(String input_filename, ArrayList<Example> examples) {
        try {
            BufferedReader inputFile = new BufferedReader(new FileReader(input_filename));
            String line;

            // skip attribute types
            while (((line = inputFile.readLine()) != null)) {
                if (line.trim().length() != 0 && line.charAt(0) != '%' && line.charAt(0) == '@') {
                    String[] tokens = line.split("\\s+");

                    if (tokens[0].equalsIgnoreCase("@data"))
                        break;
                }
            }

            // read examples
            // For each line in the input file
            while ((line = inputFile.readLine()) != null) {
                if (line.trim().length() != 0 && line.charAt(0) != '%' && line.charAt(0) != '@') {
                    Example temp = new Example();
                    temp.tokens = line.split(",");
                    examples.add(temp);
                }
            }

            inputFile.close();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public void train(String input_filename, ArrayList<Integer> quantization_levels, int no_of_conditions, String model_filename, boolean verbose, boolean ga_on) {
        ArrayList<ArrayList<Double>> ranges = new ArrayList<>();
        ArrayList<Integer> entropy_positions;
        ArrayList<String> attribute_types = new ArrayList<>();
        ArrayList<Example> examples = new ArrayList<>();
        ArrayList<String> attribute_names = new ArrayList<>();
        int no_of_attributes;

        // Read train/test file contents to ArrayList
        if (!ga_on)
            System.out.println("Reading examples...");
        read_attribute_names(input_filename, attribute_names);
        read_parameters(input_filename, quantization_levels, attribute_types, attribute_names);
        read_train_test_file(input_filename, examples);

        if (!ga_on)
            System.out.println();

        no_of_attributes = quantization_levels.size();

        // Print example size
        if (!ga_on) {
            System.out.println("No of examples read: " + examples.size());
            System.out.println("No of attributes: " + no_of_attributes);
            System.out.println();
            if (verbose)
                print_examples(examples);

            System.out.println("Quantizing...");
        }

        // Find ranges
        if (!ga_on)
            System.out.println("Calculating ranges...");
        //find_ranges_old_method(no_of_attributes, examples, quantization_levels, ranges);
        find_ranges_kmeans(no_of_attributes, examples, quantization_levels, ranges, attribute_types, ga_on, verbose);

        // Set ranges
        if (!ga_on)
            System.out.println("Setting ranges...");
        set_ranges(examples, ranges);

        // Find hash (the reason for finding the hash at this point is that after quantization, hash might change)
        for (Example example : examples)
            example.hash = String.join(",", Arrays.copyOfRange(example.tokens, 0, example.tokens.length - 1)).hashCode();

        // Sort examples
        if (!ga_on)
            System.out.println("Sorting examples...");

        Collections.sort(examples, (bo1, bo2) -> {
            if (bo1.hash > bo2.hash)
                return 1;
            else if (bo1.hash < bo2.hash)
                return -1;
            else
                return 0;
        });

        if (!ga_on)
            System.out.println();

        if (!ga_on && verbose) {
            System.out.println("After sorting examples:");
            print_examples(examples);
        }

        // Find examples with different classes
        remove_examples_with_different_classes(examples);

        // Calculate entropies
        if (!ga_on) {
            System.out.println("Calculating entropies...");
            System.out.println();
        }

        entropy_positions = calculate_entropies(no_of_attributes, examples, verbose, ga_on);

        if (!ga_on) {
            System.out.println("Feature order by entropy:");
            System.out.println(Arrays.toString(entropy_positions.toArray()));
            System.out.println();

            System.out.println("Attribute types:");
            System.out.print("[");

            for (int i = 0; i < attribute_types.size(); i++) {
                System.out.print(attribute_types.get(i));

                if (i < attribute_types.size() - 1)
                    System.out.print(", ");
            }

            System.out.print("]");
            System.out.println();
            System.out.println();
        }

        if (!ga_on)
            System.out.println("Ranges:");

        for (int i = 0; i < ranges.size(); i++) {
            if (!ga_on) {
                System.out.print(i + " -> ");
                System.out.println(Arrays.toString(ranges.get(i).toArray()));
            }

            // replace quantization levels in case they changed
            quantization_levels.set(i, ranges.get(i).size());
        }

        if (!ga_on) {
            System.out.println();

            System.out.println("Quantization levels:");
            System.out.println(Arrays.toString(quantization_levels.toArray()));
            System.out.println();

            // Print example size
            System.out.println("No of examples after pre-processing: " + examples.size());
            System.out.println();

            if (verbose) {
                System.out.println("Examples to be processed:");
                print_examples(examples);
            }

            System.out.println("Extracting rules...");
            System.out.println();
        }

        // Extract rules
        ArrayList<ArrayList<RuleObject2>> rules = new ArrayList<>();

        // add the position of examples which are not classified yet in a map
        Map<Integer,Integer> examples_unclassified = new HashMap<>();
        for (int i = 0; i < examples.size(); i++)
            examples_unclassified.put(i, -1);

        if (!ga_on && verbose) {
            System.out.println("No. of examples left to be classified: " + examples_unclassified.size());
            System.out.println();
        }

        while ((examples_unclassified.size() > 0) && (no_of_conditions <= no_of_attributes)) {
            Map<String, RuleObject> potential_list = new HashMap<>();
            Map<String, Integer> black_list = new HashMap<>(); // integer is not needed here

            if (!ga_on)
                System.out.println("Condition: "  + no_of_conditions);

            for (int i = 0; i < examples.size(); i++) { // for each example
                if (!ga_on && verbose) {
                    System.out.println("    Example:");
                    System.out.println("    " + Arrays.toString(examples.get(i).tokens));
                }

                String[] rule = new String[no_of_conditions+1]; // +1 is for class

                boolean skip = false;

                // add pre-rule
                for (int j = 0; j < (no_of_conditions - 1); j++) {
                    if (examples.get(i).tokens[entropy_positions.get(j)].equals("?")) {
                        skip = true;
                        break;
                    }

                    rule[j] = entropy_positions.get(j) + ":" + examples.get(i).tokens[entropy_positions.get(j)];
                }

                if (!skip) {
                    // add class
                    rule[no_of_conditions] = examples.get(i).tokens[no_of_attributes];

                    // add the remaining feature one at a time
                    if (!ga_on && verbose)
                        System.out.println("        Rules:");

                    for (int j = (no_of_conditions - 1); j < no_of_attributes; j++) {
                        if (!examples.get(i).tokens[entropy_positions.get(j)].equals("?")) { // skip if the feature being added is null
                            rule[no_of_conditions-1] = entropy_positions.get(j) + ":" + examples.get(i).tokens[entropy_positions.get(j)];

                            if (!ga_on && verbose)
                                System.out.println("        " + Arrays.toString(rule));

                            String[] rule_features = Arrays.copyOfRange(rule, 0, rule.length-1); // get the rule without class

                            if (!ga_on && verbose)
                                System.out.println("            Rule feature is: " + Arrays.toString(rule_features));

                            if (!black_list.containsKey(String.join(",", rule_features))) { // if the rule is not blacklisted
                                if (!ga_on && verbose)
                                    System.out.println("            Rule is not blacklisted!");

                                if (potential_list.containsKey(String.join(",", rule_features))) { // if the rule is in potentiallist
                                    if (!ga_on && verbose)
                                        System.out.println("            Rule is in potentiallist!");

                                    if (potential_list.get(String.join(",", rule_features)).rule_class.equals(examples.get(i).tokens[no_of_attributes])) { // if the class is the same
                                        potential_list.get(String.join(",", rule_features)).occurrence++;
                                        potential_list.get(String.join(",", rule_features)).classifies.put(i, -1);

                                        if (!ga_on && verbose)
                                            System.out.println("            Rule exists in potentiallist with same class, incrementing occurrence!");
                                    }
                                    else { // if the class is different, add to blacklist
                                        black_list.put(String.join(",", rule_features), null);
                                        potential_list.remove(String.join(",", rule_features));

                                        if (!ga_on && verbose)
                                            System.out.println("            Rule exists in potentiallist with different class, removing!");
                                    }
                                }
                                else { // add the rule
                                    RuleObject temp = new RuleObject();
                                    temp.occurrence = 1;
                                    temp.classifies.put(i, -1);
                                    temp.rule_class = examples.get(i).tokens[no_of_attributes];

                                    potential_list.put(String.join(",", rule_features), temp);

                                    if (!ga_on && verbose)
                                        System.out.println("            Rule is added to the potentiallist!");
                                }
                            }
                            else {
                                if (!ga_on && verbose)
                                    System.out.println("            Rule is blacklisted!");
                            }
                        }
                    }

                    if (!ga_on && verbose)
                        System.out.println();
                }
            }

            ArrayList<RuleObject2> current_rules = new ArrayList<>();
            for (String entry : potential_list.keySet()) {
                RuleObject2 temp = new RuleObject2();
                temp.occurrence = potential_list.get(entry).occurrence;
                for (int entry2 : potential_list.get(entry).classifies.keySet())
                    temp.classifies.put(entry2, -1);
                temp.rule = (entry + "," + potential_list.get(entry).rule_class).split(",");
                current_rules.add(temp);
            }

            // sort rules
            Collections.sort(current_rules, (bo1, bo2) -> {
                if (bo1.occurrence > bo2.occurrence)
                    return -1;
                else if (bo1.occurrence < bo2.occurrence)
                    return 1;
                else
                    return 0;
            });

            if (!ga_on && verbose) {
                System.out.println("Rules before removing redundant ones:");
                for (RuleObject2 current_rule : current_rules)
                    System.out.println(Arrays.toString(current_rule.rule) + " (" + current_rule.occurrence + ")");
                System.out.println();
            }

            int examples_unclassified_size_before = examples_unclassified.size();

            // classify_examples(examples_unclassified, current_rules);
            for (int i = 0; i < current_rules.size(); i++) { // for each rule
                int temp_size = examples_unclassified.size(); // find the current no of unclassified examples

                for (int entry : current_rules.get(i).classifies.keySet()) // remove each example that this rule classifies
                    examples_unclassified.remove(entry); // remove the examples that this rule can classify

                if ((temp_size - examples_unclassified.size()) == 0) { // if no examples were classified
                    current_rules.remove(i); // remove the rule
                    i--;
                }
            }

            if (!ga_on && verbose) {
                if (current_rules.size() > 0) {
                    System.out.println("    Rules(" + current_rules.size() + "):");
                    for (RuleObject2 current_rule : current_rules)
                        System.out.println("        " + Arrays.toString(current_rule.rule) + " (" + current_rule.occurrence + ")");
                    System.out.println();
                }
                System.out.println("    No. of examples classified by extracted rules: " + (examples_unclassified_size_before - examples_unclassified.size()));
                System.out.println("    No. of examples left to be classified: " + examples_unclassified.size());
                System.out.println();
            }

            rules.add(current_rules);

            no_of_conditions++;
        }

        // remove entries for rules which might have become empty
        for (int i = rules.size()-1; i >=0 ; i--) // for each condition
            if (rules.get(i).size() == 0) // if the condition doesn't have rule in it
                rules.remove(i);

        if (!ga_on) {
            System.out.println();
            System.out.println("No. of classified examples: " + (examples.size() - examples_unclassified.size()));
            System.out.println("No. of unclassified examples: " + examples_unclassified.size());
            System.out.println("Classification rate: " + (((double) (examples.size() - examples_unclassified.size()) / (double) examples.size()) * 100) + "%");
            System.out.println("Misclassification rate: " + (((double) (examples_unclassified.size()) / (double) examples.size()) * 100) + "%");
            System.out.println();
            System.out.println("Rules extracted (model style):");
            System.out.println();

            print_rules(rules, attribute_names);
        }

/*
		if (!ga_on && verbose) {
			System.out.println();
			System.out.println("Rules extracted (human-readable):");
			System.out.println();
			print_rules_human_readable(rules, attribute_types, ranges);
		}
*/

        if (!model_filename.equals(""))
            write_model_to_file(model_filename, quantization_levels, ranges, rules);
    }

    public void print_rules(ArrayList<ArrayList<RuleObject2>> rules, ArrayList<String> attribute_names) {
        for (int i = 0; i < rules.size(); i++) { // for each condition
            if (rules.get(i).size() > 0) { // if the condition has rules in it
                System.out.println("Condition: " + (i + 1));

                for (int j = 0; j < rules.get(i).size(); j++) { // for each rule in the condition
                    String[] all_rules = rules.get(i).get(j).rule;

                    System.out.print("[");

                    for (int k = 0; k < all_rules.length - 1; k++) {
                        String rule = all_rules[k];
                        int feature_number = Integer.parseInt(rule.split(":")[0]);
                        String feature_name = attribute_names.get(feature_number);
                        String feature_value = rule.split(":")[1];

                        System.out.print(feature_name + ":" + feature_value + ", ");
                    }

                    System.out.print(all_rules[all_rules.length-1]);
                    System.out.println("]");
                }

                System.out.println();
            }
        }
    }

    public void write_model_to_file(String model_filename, ArrayList<Integer> quantization_levels, ArrayList<ArrayList<Double>> ranges, ArrayList<ArrayList<RuleObject2>> rules) {
        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            fw = new FileWriter(model_filename);
            bw = new BufferedWriter(fw);

            for (Integer quantization_level : quantization_levels) {
                bw.write(Integer.toString(quantization_level));
                bw.newLine();
            }
            bw.write("***");
            bw.newLine();

            for (int i = 0; i < ranges.size(); i++) {
                bw.write(i + ":");
                for (int j = 0; j < ranges.get(i).size(); j++) {
                    bw.write(Double.toString(ranges.get(i).get(j)));
                    if (j < ranges.get(i).size() - 1)
                        bw.write(",");
                }
                bw.newLine();
            }
            bw.write("***");
            bw.newLine();

            for (int i = 0; i < rules.size(); i++) {
                bw.write("condition:" + (i+1));
                bw.newLine();

                for (int j = 0; j < rules.get(i).size(); j++) {
                    bw.write(Integer.toString(rules.get(i).get(j).occurrence));
                    bw.write(";");
                    for (int k = 0; k < rules.get(i).get(j).rule.length; k++) {
                        bw.write(rules.get(i).get(j).rule[k]);
                        if (k < rules.get(i).get(j).rule.length-1)
                            bw.write(",");
                    }
                    bw.newLine();
                }

                if (i < rules.size()-1) {
                    bw.write("+++");
                    bw.newLine();
                }
            }
            bw.write("+++");
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void print_rules_human_readable(ArrayList<ArrayList<RuleObject2>> rules, ArrayList<String> attribute_types, ArrayList<ArrayList<Double>> ranges) {
        for (int i = 0; i < rules.size(); i++) { // for each condition
            if (rules.get(i).size() > 0) { // if the condition has rules
                System.out.println("Condition: " + (i+1));
                for (int j = 0; j < rules.get(i).size(); j++) { // for each rule
                    System.out.print("IF [ ");
                    for (int k = 0; k < rules.get(i).get(j).rule.length-1; k++) { // for each attribute
                        int feature_pos = Integer.parseInt(rules.get(i).get(j).rule[k].split(":")[0]);

                        if (attribute_types.get(feature_pos).equals("string")) {
                            String feature_value = rules.get(i).get(j).rule[k].split(":")[1];

                            System.out.print("(attribute" + (feature_pos) + " = " + feature_value + ")");
                        }
                        else {
                            int feature_value = (int) Double.parseDouble(rules.get(i).get(j).rule[k].split(":")[1]);

                            double min_range = ranges.get(feature_pos).get(feature_value - 1);
                            double max_range = ranges.get(feature_pos).get(feature_value);

                            System.out.print("(" + min_range + " <= attribute" + (feature_pos) + " < " + max_range + ")");
                        }

                        if (k < (rules.get(i).get(j).rule.length-2))
                            System.out.print(" & ");
                    }
                    System.out.println(" ] -> class: " + rules.get(i).get(j).rule[rules.get(i).get(j).rule.length-1]);
                }
                System.out.println();
            }
        }
    }

    public void print_examples(ArrayList<Example> examples) {
        for (int i = 0; i < examples.size(); i++) {
            System.out.print(i + ": " + Arrays.toString(examples.get(i).tokens));
            System.out.println(" (" + examples.get(i).hash + ")");
        }
        System.out.println();
    }

    public void classify_examples(ArrayList<Example> examples_unclassified, ArrayList<RuleObject2> rules_current) {
        for (int i = 0; i < rules_current.size(); i++) { // for each rule
            boolean rule_classifies = false;

            String[] rule_tokens = rules_current.get(i).rule;

            for (int j = 0; j < examples_unclassified.size(); j++) {
                boolean example_classified = true;

                String[] example_tokens = examples_unclassified.get(j).tokens;

                for (int k = 0; k < (rule_tokens.length - 1); k++) {
                    int pos1 = Integer.parseInt(rule_tokens[k].split(":")[0]);
                    String test1 = rule_tokens[k].split(":")[1];
                    String test2 = example_tokens[pos1];

                    if (test2.equals("?") || !test1.equals(test2)) {
                        example_classified = false;
                        break;
                    }
                }

                if (example_classified) {
                    examples_unclassified.remove(j);
                    j--;
                    rule_classifies = true;
                }
            }

            if (!rule_classifies) {
                rules_current.remove(i);
                i--;
            }
        }
    }

    public void remove_examples_with_different_classes(ArrayList<Example> examples) {
        int pos = 0;
        while (pos < (examples.size() - 1)) {
            boolean repetition_found = false;

            LinkedListArray classes = new LinkedListArray();
            while (examples.get(pos).hash == examples.get(pos+1).hash) { // if features are the same
                repetition_found = true;
                classes.add(examples.get(pos).tokens[examples.get(pos).tokens.length - 1]);
                examples.remove(pos);

                if (pos == (examples.size() - 1))
                    break;
            }

            if (repetition_found) {
                classes.add(examples.get(pos).tokens[examples.get(pos).tokens.length - 1]);
                examples.get(pos).tokens[examples.get(pos).tokens.length - 1] = classes.get(0).text;
            }

            pos++;
        }
    }

    public ArrayList<Integer> calculate_entropies(int no_of_attributes, ArrayList<Example> examples, boolean verbose, boolean ga_on) {
        ArrayList<Double> entropies = new ArrayList<>(); // holds entropy values for each attribute
        ArrayList<Integer> entropy_positions = new ArrayList<>();

        for (int i = 0; i < no_of_attributes; i++) { // for each attribute
            //System.out.println("Calculating entropy for Attribute " + (i+1) + "...");

            Map<String, MapObject> instances = new HashMap<>(); // list of unique instances for each attribute

            for (Example example : examples) { // for each example
                String example_attribute = example.tokens[i]; // get the attribute value from example j
                String example_class = example.tokens[no_of_attributes]; // get the class from example j

                if (instances.containsKey(example_attribute)) { // if exists
                    instances.get(example_attribute).occurrence++;

                    if (instances.get(example_attribute).classes.containsKey(example_class)) {
                        int x = instances.get(example_attribute).classes.get(example_class);
                        x++;
                        instances.get(example_attribute).classes.put(example_class, x);
                    } else
                        instances.get(example_attribute).classes.put(example_class, 1);
                } else { // if doesn't exist
                    MapObject temp = new MapObject();
                    temp.classes.put(example_class, 1);
                    instances.put(example_attribute, temp);
                }
            }

            if (!ga_on && verbose) {
                for (String entry : instances.keySet()) {
                    System.out.println(entry + " (" + instances.get(entry).occurrence + ")");

                    for (String entry2 : instances.get(entry).classes.keySet())
                        System.out.println("    " + entry2 + " (" + instances.get(entry).classes.get(entry2) + ")");

                    System.out.println();
                }
            }

            double entropy = 0;
            for (String entry : instances.keySet()) {
                double without_rate = 0;
                for (String entry2 : instances.get(entry).classes.keySet()) {
                    double term = instances.get(entry).classes.get(entry2)/((double)instances.get(entry).occurrence);
                    without_rate = without_rate + ((-1) * term * (Math.log(term) / Math.log(2)));
                }
                without_rate = without_rate * (instances.get(entry).occurrence /((double)examples.size()));
                entropy = entropy + without_rate;
            }
            entropies.add(entropy);
        }

        // Print entropies
        if (!ga_on) {
            System.out.println("Entropies:");
            for (int i = 0; i < entropies.size(); i++)
                System.out.println(i + " -> [" + entropies.get(i) + "]");
            System.out.println();
        }

        // Sort entropies list
        // Initialize entropy positions array
        for (int i = 0; i < entropies.size(); i++)
            entropy_positions.add(i);

        for (int i = 0; i < entropies.size(); i++) {
            for (int j = i+1; j < entropies.size(); j++) {
                if (entropies.get(j) < entropies.get(i)) {
                    double temp = entropies.get(j);
                    entropies.set(j, entropies.get(i));
                    entropies.set(i, temp);

                    int temp2 = entropy_positions.get(j);
                    entropy_positions.set(j, entropy_positions.get(i));
                    entropy_positions.set(i, temp2);
                }
            }
        }

        return entropy_positions;
    }

    public void find_ranges_old_method(int no_of_attributes, ArrayList<Example> examples, ArrayList<Integer> quantization_levels, ArrayList<ArrayList<Double>> ranges) {
        ArrayList<Double> mins = new ArrayList<>();
        ArrayList<Double> maxs = new ArrayList<>();

        // initialize mins and maxs
        for (int i = 0; i < no_of_attributes; i++) {
            mins.add(Double.MAX_VALUE);
            maxs.add(0.0);
        }

        // Find mins and maxs
        for (int j = 0; j < no_of_attributes; j++) { // for each attribute
            if (quantization_levels.get(j) > 0) {
                for (Example example : examples) { // for each example
                    if (!example.tokens[j].equals("?")) {
                        double x = Double.parseDouble(example.tokens[j]);

                        if (x < mins.get(j))
                            mins.set(j, x);

                        if (x > maxs.get(j))
                            maxs.set(j, x);
                    }
                }
            }
        }

        // Calculate ranges
        for (int j = 0; j < no_of_attributes; j++) { // for each attribute
            ArrayList<Double> range = new ArrayList<>();

            if (quantization_levels.get(j) > 0) {
                // get quantization level
                int quantization;
                if (quantization_levels.size() == 1)
                    quantization = quantization_levels.get(0);
                else
                    quantization = quantization_levels.get(j);

                double min = mins.get(j);
                double max = maxs.get(j);

                double difference = (max - min) / (double) quantization;

                range.add(min);
                for (int i = 0; i < (quantization - 1); i++)
                    range.add(min + (difference * (i+1)));
                range.add(max);

                ranges.add(range);
            }
            else
                ranges.add(range);
        }
    }

    public void find_ranges_kmeans(int no_of_attributes, ArrayList<Example> examples, ArrayList<Integer> quantization_levels, ArrayList<ArrayList<Double>> ranges, ArrayList<String> attribute_types, boolean ga_on, boolean verbose) {
        // for each numeric attribute
        for (int i = 0; i < no_of_attributes; i++) {
            if (verbose)
                System.out.println("Calculating ranges for attribute: " + (i+1));
            if (quantization_levels.get(i) > 0) { // if the feature is numeric
                ArrayList<String> entries = new ArrayList<>();

                // for each example, get the values for the current feature
                for (Example example : examples)
                    if (!example.tokens[i].equals("?"))
                        entries.add(example.tokens[i]);

                // sort the values
                Collections.sort(entries, (bo1, bo2) -> {
                    if (Double.parseDouble(bo1) > Double.parseDouble(bo2))
                        return 1;
                    else if (Double.parseDouble(bo1) < Double.parseDouble(bo2))
                        return -1;
                    else
                        return 0;
                });

                // remove repetitions
                for (int j = 0; j < entries.size() - 1; j++) {
                    if (entries.get(j).equals(entries.get(j + 1))) {
                        entries.remove(j);
                        j--;
                    }
                }

                if (entries.size() <= 1) { // if unique values is only 1
                    quantization_levels.set(i, 0);
                    attribute_types.set(i, "string");
                    ranges.add(new ArrayList<>());
                }
                else {
                    // create array to be passed
                    double[][] _data;
                    _data = new double[entries.size()][];
                    for (int j = 0; j < entries.size(); j++) {
                        _data[j] = new double[1];
                        _data[j][0] = Double.parseDouble(entries.get(j));
                    }

                    KMeans KM = new KMeans(_data, entries.size());
                    KM.clustering(quantization_levels.get(i), 100, ga_on, verbose); // clusters, maximum 10 iterations
                    if (verbose)
                        KM.printResults();

                    ranges.add(KM.getRanges());
                }
            }
        }
    }

    public void set_ranges(ArrayList<Example> examples, ArrayList<ArrayList<Double>> ranges) {
        for (int j = 0; j < ranges.size(); j++) { // for each attribute
            if (ranges.get(j).size() > 0) { // if feature is not string
                for (Example example : examples) { // for each example
                    if (!example.tokens[j].equals("?")) {
                        ArrayList<Double> range = ranges.get(j);
                        boolean found = false;
                        for (int k = 1; k < (range.size() - 1); k++) { // for each range
                            if (Double.parseDouble(example.tokens[j]) <= range.get(k)) {
                                example.tokens[j] = Integer.toString(k);
                                found = true;
                                break;
                            }
                        }

                        if (!found)
                            example.tokens[j] = Integer.toString(range.size() - 1);
                    }
                }
            }
        }
    }

    public void output_arraylist(ArrayList<String> input) {
        System.out.println("OUTPUT");
        System.out.println("======");

        for (String s : input) {
            String[] tokens = s.split(",");
            for (String token : tokens) System.out.print(token + "  ");
            System.out.println();
        }

        System.out.println("======");
    }
}

class confusion_matrix {
    int correct = 0;
    int incorrect = 0;
}
