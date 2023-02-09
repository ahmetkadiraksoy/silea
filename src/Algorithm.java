import java.util.ArrayList;

public class Algorithm {
    ///////////////////
    // GA parameters //
    ///////////////////
    private static final double uniformRate = 0.5;
    private static final double mutationRate = 0.05; // originally this was 0.015
    private static final int tournamentSize = 50;
    private static final boolean elitism = true;

    /////////////////////////
    // Evolve a population //
    /////////////////////////
    public static Population evolvePopulation(Population pop, int max_threads, String examples_train, String examples_test, int no_of_attributes, ArrayList<Integer> quantization_levels, int no_of_conditions, String model_filename, boolean verbose, int max_error) {
        Population newPopulation = new Population(pop.size(), false, max_threads, examples_train, examples_test, no_of_attributes, quantization_levels, no_of_conditions, model_filename, verbose, max_error);

        // Keep our best individual
        if (elitism) {
            newPopulation.saveIndividual(0, pop.getFittest());
            newPopulation.individuals[0].setId(0);
        }

        // Crossover population
        int elitismOffset;
        if (elitism)
            elitismOffset = 1;
        else
            elitismOffset = 0;

        // Loop over the population size and create new individuals with
        // crossover
        for (int i = elitismOffset; i < pop.size(); i++) {
            Individual indiv1 = tournamentSelection(pop, max_threads, examples_train, examples_test, no_of_attributes, quantization_levels, no_of_conditions, model_filename, verbose, max_error);
            Individual indiv2 = tournamentSelection(pop, max_threads, examples_train, examples_test, no_of_attributes, quantization_levels, no_of_conditions, model_filename, verbose, max_error);
            Individual newIndiv = crossover(indiv1, indiv2, examples_train, examples_test, no_of_attributes, quantization_levels, no_of_conditions, model_filename, verbose, max_error);
            newPopulation.saveIndividual(i, newIndiv);
            newPopulation.individuals[i].setId(i);
        }

        // Mutate population
        for (int i = elitismOffset; i < newPopulation.size(); i++)
            mutate(newPopulation.getIndividual(i));

        return newPopulation;
    }

    ///////////////////////////
    // Crossover individuals //
    ///////////////////////////
    private static Individual crossover(Individual indiv1, Individual indiv2, String examples_train, String examples_test, int no_of_attributes, ArrayList<Integer> quantization_levels, int no_of_conditions, String model_filename, boolean verbose, int max_error) {
        Individual newSol = new Individual(examples_train, examples_test, no_of_attributes, quantization_levels, no_of_conditions, model_filename, verbose, max_error, 0);
        // Loop through genes
        for (int i = 0; i < indiv1.size(); i++) {
            // Crossover
            if (Math.random() <= uniformRate)
                newSol.setGene(i, indiv1.getGene(i));
            else
                newSol.setGene(i, indiv2.getGene(i));
        }
        return newSol;
    }

    //////////////////////////
    // Mutate an individual //
    //////////////////////////
    private static void mutate(Individual indiv) {
        // Loop through genes
        for (int i = 0; i < indiv.size(); i++) {
            if (Math.random() <= mutationRate) {
                // Create random gene
                byte gene = (byte) Math.round(Math.random());
                indiv.setGene(i, gene);
            }
        }
    }

    //////////////////////////////////////
    // Select individuals for crossover //
    //////////////////////////////////////
    private static Individual tournamentSelection(Population pop, int max_threads, String examples_train, String examples_test, int no_of_attributes, ArrayList<Integer> quantization_levels, int no_of_conditions, String model_filename, boolean verbose, int max_error) {
        // Create a tournament population
        Population tournament = new Population(pop.size(), false, max_threads, examples_train, examples_test, no_of_attributes, quantization_levels, no_of_conditions, model_filename, verbose, max_error);

        // For each place in the tournament get a random individual
        for (int i = 0; i < pop.size(); i++) {
            int randomId = (int) (Math.random() * pop.size());
            tournament.saveIndividual(i, pop.getIndividual(randomId));
        }

        // Get the fittest
        Individual fittest = tournament.getFittest();

        return fittest;
    }
}
