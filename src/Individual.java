import java.util.ArrayList;

public class Individual {
    private byte[] genes;
    private double fitness = 0;
    private boolean has_run = false;
    private String examples_train;
    private String examples_test;
    private int no_of_conditions;
    private boolean verbose;
    private int id;
    private int max_error;
    private ArrayList<Integer> quantization_levels;
    private String model_filename;
    private int no_of_attributes;

    public Individual(String examples_train, String examples_test, int no_of_attributes, ArrayList<Integer> quantization_levels, int no_of_conditions, String model_filename, boolean verbose, int max_error, int id) {
        this.genes = new byte[no_of_attributes * 5];
        this.examples_train = examples_train;
        this.examples_test = examples_test;
        this.no_of_conditions = no_of_conditions;
        this.verbose = verbose;
        this.id = id;
        this.max_error = max_error;
        this.quantization_levels = quantization_levels;
        this.model_filename = model_filename;
        this.no_of_attributes = no_of_attributes;
    }

    // Create a random individual
    public void generateIndividual() {
        for (int i = 0; i < size(); i++) {
            byte gene = (byte) Math.round(Math.random());
            genes[i] = gene;
        }
    }

    public byte[] getGeneArray() {
        return genes;
    }

    public void setGeneArray(byte[] input) {
        this.genes = input;
    }

    public void setId(int id) {
        this.id = id;
    }

    public byte getGene(int index) {
        return genes[index];
    }

    public void setGene(int index, byte value) {
        genes[index] = value;
    }

    public int size() {
        return genes.length;
    }

    public int getId() { return id; }

    public double getFitness() {
        if (!has_run) {
            fitness = FitnessCalc.getFitness(this, examples_train, examples_test, no_of_attributes, quantization_levels, no_of_conditions, model_filename, verbose, max_error);
            has_run = true;
        }
        return fitness;
    }

    @Override
    public String toString() {
        String geneString = "";

        for (int i = 0; i < size(); i++)
            geneString += getGene(i);

        return geneString;
    }
}
