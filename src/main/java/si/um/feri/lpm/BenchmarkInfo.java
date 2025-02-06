package si.um.feri.lpm;

public class BenchmarkInfo {
    public String name;
    public BenchmarkId id;
    public int numberOfProblems;
    public int k;
    public int runs;
    public int[] dimensions;
    public int[] evaluations;
    public String[] algorithms;

    public BenchmarkInfo(String name, BenchmarkId id, int numberOfProblems, int runs, int k, int[] dimensions, int[] evaluations, String[] algorithms) {
        this.name = name;
        this.id = id;
        this.numberOfProblems = numberOfProblems;
        this.k = k;
        this.runs = runs;
        this.dimensions = dimensions;
        this.evaluations = evaluations;
        this.algorithms = algorithms;
    }
}
