package si.um.feri.lpm;

import org.um.feri.ears.algorithms.NumberAlgorithm;
import org.um.feri.ears.benchmark.SOBenchmark;
import org.um.feri.ears.problems.*;

public class Cec2024StoredBenchmark extends SOBenchmark<NumberSolution<Double>, NumberSolution<Double>, DoubleProblem, NumberAlgorithm> {

    static final BenchmarkInfo cec2024 = BenchmarkManager.get(BenchmarkId.CEC2024);

    int cutpoint; // represents the kth number of evaluations in the results file

    public boolean wholeConvergenceGraph = false;

    public Cec2024StoredBenchmark() {
        this(cec2024.k - 1);
    }

    public Cec2024StoredBenchmark(int cutpoint) {
        this.cutpoint = cutpoint;
        this.drawLimit = 1e-8; //minimum error
        name = cec2024.name + " Stored benchmark";
        maxEvaluations = 200000;
        maxIterations = 0;
    }

    @Override
    protected void addTask(DoubleProblem p, StopCriterion stopCriterion, int eval, long time, int maxIterations) {
        tasks.add(new Task<>(p, stopCriterion, eval, time, maxIterations));
    }

    @Override
    public void initAllProblems() {

        for (int p = 0; p < cec2024.numberOfProblems; p++) {
            for (int d = 0; d < cec2024.dimensions.length; d++) {
                if (wholeConvergenceGraph) {
                    for (int cutpoint = 0; cutpoint < cec2024.k; cutpoint++) {
                        addTask(new DummyProblem(cec2024.name + "_" + (p + 1) + "_" + cec2024.dimensions[d] + "k" + (cutpoint), false), stopCriterion, cec2024.evaluations[d], 0, maxIterations);
                    }
                } else {
                    addTask(new DummyProblem(cec2024.name + "_" + (p + 1) + "_" + cec2024.dimensions[d] + "k" + (cutpoint), false), stopCriterion, cec2024.evaluations[d], 0, maxIterations);
                }
            }
        }
    }
}
