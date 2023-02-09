import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Population {
    Individual[] individuals;
    int max_threads;

    // Create a population
    public Population(int populationSize, boolean initialise, int max_threads_f, String examples_train, String examples_test, int no_of_attributes, ArrayList<Integer> quantization_levels, int no_of_conditions, String model_filename, boolean verbose, int max_error) {
        individuals = new Individual[populationSize];
        max_threads = max_threads_f;

        // Initialise population
        if (initialise) {
            // Loop and create individuals
            for (int i = 0; i < size(); i++) {
                Individual newIndividual = new Individual(examples_train, examples_test, no_of_attributes, quantization_levels, no_of_conditions, model_filename, verbose, max_error, i);
                newIndividual.generateIndividual();
                saveIndividual(i, newIndividual);
            }
        }
    }

    public Individual getIndividual(int index) { return individuals[index]; }

    public Individual getFittest() {
        ArrayList<GetFitnessThread> threads = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(max_threads);

        for (int i = 0; i < size(); i++)
            threads.add(new GetFitnessThread(individuals[i]));

        for (int i = 0; i < size(); i++)
            executor.execute(threads.get(i)); //calling execute method of ExecutorService

        executor.shutdown();
        while (!executor.isTerminated()) {}

        for (int i = 0; i < size(); i++)
            individuals[i] = threads.get(i).getIndividual();

        Individual fittest = individuals[0];

        // loop trough individuals, find maximum fitness and return that
        for (int i = 0; i < size(); i++)
            if (fittest.getFitness() < individuals[i].getFitness())
                fittest = individuals[i];

        return fittest;
    }

    public int size() { return individuals.length; }

    public void saveIndividual(int index, Individual indiv) {
        individuals[index] = indiv;
    }
}

class GetFitnessThread extends Thread {
    private Thread t;
    private Individual current;

    GetFitnessThread(Individual current) { this.current = current; }

    public void run() {
        @SuppressWarnings("unused")
        double temp = current.getFitness();
    }

    public Individual getIndividual() {
        return current;
    }

    public void start () {
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }
}
