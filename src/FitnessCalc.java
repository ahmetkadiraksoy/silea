import java.io.File;
import java.util.ArrayList;

public class FitnessCalc {
    // Calculate individuals' fitness by comparing it to our candidate solution
    static double getFitness(Individual individual, String examples_train, String examples_test, int no_of_attributes, ArrayList<Integer> quantization_levels, int no_of_conditions, String model_filename, boolean verbose, int max_error) {
        SILEA silea = new SILEA();

        ArrayList<Integer> quantization_levels_temp = new ArrayList<>();

        // find parameter values
//        int no_of_non_zeros = 0;
        for (int i = 0; i < individual.size(); i+=5) {
            int value = 1;
            if (individual.getGene(i+0) == 1)
                value += 16;
            if (individual.getGene(i+1) == 1)
                value += 8;
            if (individual.getGene(i+2) == 1)
                value += 4;
            if (individual.getGene(i+3) == 1)
                value += 2;
            if (individual.getGene(i+4) == 1)
                value += 1;

            if (value == 1)
                quantization_levels_temp.add(value);
            else
                quantization_levels_temp.add(value * 10);
        }

        silea.train(examples_train, quantization_levels_temp, no_of_conditions, model_filename + "_" + individual.getId(), verbose, true);
        double accuracy_1 = silea.test(model_filename + "_" + individual.getId(), examples_test, max_error, verbose, true);

        silea.train(examples_test, quantization_levels_temp, no_of_conditions, model_filename + "_" + individual.getId(), verbose, true);
        double accuracy_2 = silea.test(model_filename + "_" + individual.getId(), examples_train, max_error, verbose, true);

        double accuracy = (accuracy_1 + accuracy_2) / 2.0;

        new File(model_filename + "_" + individual.getId()).delete();

        return Math.round(accuracy);
    }
}
