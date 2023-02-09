import java.io.File;
import java.util.ArrayList;

public class GA {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public String execute(String ga_train_input_filename, String ga_test_input_filename, int no_of_attributes, ArrayList<Integer> quantization_levels, int no_of_conditions, String model_filename, boolean verbose, int max_error, int iteration, int population_size, int max_threads) {
        // Create an initial population
        Population myPop = new Population(population_size, true, max_threads, ga_train_input_filename, ga_test_input_filename, no_of_attributes, quantization_levels, no_of_conditions, model_filename, verbose, max_error);

        ArrayList<String> solutions = new ArrayList<>();

        String solution;

        while (true) {
            solution = myPop.getFittest().toString();

            // Add the solution to the arraylist
            solutions.add(solution);

            print_the_solution(solution, no_of_attributes, myPop);

            // Check loop break condition
            int no = 0;
            if (solutions.size() >= iteration) {
                String test = solutions.get(solutions.size() - 1);
                for (int i = 1; i < iteration; i++)
                    if (solutions.get((solutions.size() - 1) - i).equals(test))
                        no++;

                if (no >= (iteration - 1))
                    break;
            }

            myPop = Algorithm.evolvePopulation(myPop, max_threads, ga_train_input_filename, ga_test_input_filename, no_of_attributes, quantization_levels, no_of_conditions, model_filename, verbose, max_error);
        }

        // final
        System.out.println();
        System.out.println("Final result:");
        print_the_solution(solution, no_of_attributes, myPop);
        System.out.println();

        return solution;
    }

    public void print_the_solution(String solution, int no_of_attributes, Population myPop) {
        // Print the solution
        int no_of_selected = 0;
        System.out.print(ANSI_RED + "Solution: " + ANSI_RESET);
        String tokens[] = solution.split("");

        // print bits in color
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals("0"))
                System.out.print(ANSI_CYAN + tokens[i] + ANSI_RESET);
            else {
                no_of_selected++;
                System.out.print(ANSI_BLUE + tokens[i] + ANSI_RESET);
            }

            if ((i+1)%5 == 0)
                System.out.print(" ");
        }

        //System.out.println(" " + ANSI_RED + "(" + no_of_selected + "/" + no_of_attributes + ")" + " " + myPop.getFittest().getFitness() + "%" + ANSI_RESET);
        System.out.println(" " + ANSI_RED + myPop.getFittest().getFitness() + "%" + ANSI_RESET);
    }
}
