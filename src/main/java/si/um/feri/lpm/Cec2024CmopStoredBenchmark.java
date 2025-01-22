package si.um.feri.lpm;

import org.um.feri.ears.algorithms.NumberAlgorithm;
import org.um.feri.ears.benchmark.SOBenchmark;
import org.um.feri.ears.problems.*;

public class Cec2024CmopStoredBenchmark extends SOBenchmark<NumberSolution<Double>, NumberSolution<Double>, DoubleProblem, NumberAlgorithm> {

    static final BenchmarkInfo cec2024cmop = BenchmarkManager.get(BenchmarkId.CEC2024CMOP);

    int cutpoint; // represents the kth number of evaluations in the results file

    public boolean wholeConvergenceGraph = false;

    public Cec2024CmopStoredBenchmark() {
        this(cec2024cmop.k - 1);
    }

    public Cec2024CmopStoredBenchmark(int cutpoint) {
        this.cutpoint = cutpoint;
        this.drawLimit = 1e-8; //minimum error
        name = cec2024cmop.name + " Stored benchmark";
        maxEvaluations = 200000;
        maxIterations = 0;
    }

    @Override
    protected void addTask(DoubleProblem p, StopCriterion stopCriterion, int eval, long time, int maxIterations) {
        tasks.add(new Task<>(p, stopCriterion, eval, time, maxIterations));
    }

    @Override
    public void initAllProblems() {

        for (int p = 0; p < cec2024cmop.numberOfProblems; p++) {
            if (wholeConvergenceGraph) {
                for (int cutpoint = 0; cutpoint < cec2024cmop.k; cutpoint++) {
                    addTask(new DummyProblem(cec2024cmop.name + "_SDC" + (p + 1) + "k" + (cutpoint), false), stopCriterion, cec2024cmop.evaluations[0], 0, maxIterations);
                }
            } else {
                addTask(new DummyProblem(cec2024cmop.name + "_SDC" + (p + 1) + "k" + (cutpoint), false), stopCriterion, cec2024cmop.evaluations[0], 0, maxIterations);
            }
        }
    }
}
