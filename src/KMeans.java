/** Class for kmeans clustering
* created by Keke Chen (keke.chen@wright.edu)
* For Cloud Computing Labs
* Feb. 2014
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class KMeans {
    // Data members
    private double[][] _data; // Array of all records in dataset
    private int[] _label;  // generated cluster labels
    private double[][] _centroids; // centroids: the center of clusters
    private int _nrows, _ndims = 1; // the number of rows and dimensions
    private int _numClusters; // the number of clusters;

    // Constructor; loads records from file <fileName>.
    public KMeans(double[][] _data_input, int _nrows_input) {
        _data = _data_input;
        _nrows = _nrows_input;
    }

    // Perform k-means clustering with the specified number of clusters and
    // Eucliden distance metric.
    // niter is the maximum number of iterations. If it is set to -1, the kmeans iteration is only terminated by the convergence condition.
    // centroids are the initial centroids. It is optional. If set to null, the initial centroids will be generated randomly.
    public void clustering(int numClusters, int niter, boolean ga_on, boolean verbose) {
//        System.out.println("flkqwnfkleqjfpo32fj3qpjfqpf");
//        System.out.println(numClusters + " " + _nrows);
//
//        if (numClusters > _nrows)
//            numClusters = _nrows;
//
//        System.out.println(numClusters + " " + _nrows);
//        System.out.println("flkqwnfkleqjfpo32fj3qpjfqpf");

        _numClusters = numClusters;


        // set centroids
        _centroids = new double[_numClusters][];

        double min = _data[0][0];
        double max = _data[_nrows-1][0];
        double difference = max - min;
        double rate = difference / (_numClusters - 1);

        for (int i = 0; i < numClusters; i++) {
            _centroids[i] = new double[_ndims];
            _centroids[i][0] = min + (i * rate);
        }

        // print centroids
        if (verbose) {
            System.out.println("\tInitialized centroids:");
            for (int i = 0; i < numClusters; i++)
                System.out.println("\t" + _centroids[i][0]);
            System.out.println();
        }


        double[][] c1 = _centroids;
        double threshold = 0.001;
        int round = 0;

        while (true) {
            // update _centroids with the last round results
            _centroids = c1;

            //assign record to the closest centroid
            _label = new int[_nrows];
            for (int i=0; i<_nrows; i++)
                _label[i] = closest(_data[i]);

            // recompute centroids based on the assignments
            c1 = updateCentroids();
            round ++;
            if ((niter >0 && round >=niter) || converge(_centroids, c1, threshold))
                break;
        }

        if (verbose)
            System.out.println("\tClustering converges at round " + round);
    }

    // find the closest centroid for the record v
    private int closest(double [] v) {
        double mindist = dist(v, _centroids[0]);
        int label =0;
        for (int i=1; i<_numClusters; i++){
            double t = dist(v, _centroids[i]);
            if (mindist>t) {
                mindist = t;
                label = i;
            }
        }
        return label;
    }

    // compute Euclidean distance between two vectors v1 and v2
    private double dist(double [] v1, double [] v2) {
        double sum=0;
        for (int i=0; i<_ndims; i++) {
            double d = v1[i]-v2[i];
            sum += d*d;
        }
        return Math.sqrt(sum);
    }

    // according to the cluster labels, recompute the centroids
    // the centroid is updated by averaging its members in the cluster.
    // this only applies to Euclidean distance as the similarity measure.

    private double[][] updateCentroids() {
        // initialize centroids and set to 0
        double [][] newc = new double [_numClusters][]; //new centroids
        int [] counts = new int[_numClusters]; // sizes of the clusters

        // intialize
        for (int i=0; i<_numClusters; i++) {
            counts[i] =0;
            newc[i] = new double [_ndims];
            for (int j=0; j<_ndims; j++)
                newc[i][j] =0;
        }

        for (int i=0; i<_nrows; i++) {
            int cn = _label[i]; // the cluster membership id for record i
            for (int j=0; j<_ndims; j++)
                newc[cn][j] += _data[i][j]; // update that centroid by adding the member data record
            counts[cn]++;
        }

        // finally get the average
        for (int i=0; i< _numClusters; i++)
            for (int j=0; j<_ndims; j++)
                newc[i][j]/= counts[i];

        return newc;
    }

    // check convergence condition
    // max{dist(c1[i], c2[i]), i=1..numClusters < threshold
    private boolean converge(double [][] c1, double [][] c2, double threshold) {
        // c1 and c2 are two sets of centroids
        double maxv = 0;
        for (int i=0; i< _numClusters; i++) {
            double d= dist(c1[i], c2[i]);
            if (maxv<d)
                maxv = d;
        }

        if (maxv <threshold)
            return true;
        else
            return false;
    }

    public ArrayList<Double> getRanges() {
        ArrayList<Double> range = new ArrayList<>();

        int pos = 0;
        int current_label = -1; // default

        // determine the number of labels
        Map<Integer,Integer> unique_labels = new HashMap<>();
        for (int i = 0; i < _nrows; i++)
            unique_labels.put(_label[i], -1);

        for (int i = 0; i < unique_labels.size(); i++) {
            current_label = _label[pos];
            double min = 0;
            double max = 0;

            min = _data[pos][0];

            while (pos < _nrows && current_label == _label[pos])
                pos++;
            max = _data[pos-1][0];

            if (i == 0 && min != max)
                range.add(min);
            range.add(max);
        }

        return range;
    }

    public void printResults() {
        System.out.println("\tLabel:");
        for (int i=0; i<_nrows; i++)
            System.out.println("\t" + _data[i][0] + "," + _label[i]);

        System.out.println();

        System.out.println("\tCentroids:");
        for (int i=0; i<_numClusters; i++) {
            for(int j=0; j<_ndims; j++)
                System.out.print("\t" + _centroids[i][j] + " ");
            System.out.println();
        }

        System.out.println();
    }
}
