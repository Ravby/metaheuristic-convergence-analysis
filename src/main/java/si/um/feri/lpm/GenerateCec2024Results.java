package si.um.feri.lpm;

import org.um.feri.ears.algorithms.NumberAlgorithm;
import org.um.feri.ears.algorithms.so.ao.AO;
import org.um.feri.ears.algorithms.so.avoa.AVOA;
import org.um.feri.ears.algorithms.so.de.lshade.LSHADE;
import org.um.feri.ears.algorithms.so.gaoa.GAOA;
import org.um.feri.ears.algorithms.so.gwo.GWO;
import org.um.feri.ears.algorithms.so.random.RandomSearch;
import org.um.feri.ears.algorithms.so.rsa.RSA;
import org.um.feri.ears.algorithms.so.scso.SCSO;
import org.um.feri.ears.algorithms.so.ssa.SSA;
import org.um.feri.ears.problems.*;
import org.um.feri.ears.problems.unconstrained.cec2017.*;
import org.um.feri.ears.util.Util;
import org.um.feri.ears.util.random.RNG;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class GenerateCec2024Results {

    public static void main(String[] args) {

        RNG.setSeed(System.currentTimeMillis()); //Uniform random initialization within the search space. Random seed is based on time, Matlab users can use rand ('state', sum(100*clock))

        BenchmarkInfo cec2024 = BenchmarkManager.get(BenchmarkId.CEC2024);

        int dimension = 30;
        ArrayList<CEC2017> problems = new ArrayList<>();

        //CEC 2024 - use CEC 2017 with 30 dimensions
        problems.add(new F1(dimension));
        problems.add(new F2(dimension));
        problems.add(new F3(dimension));
        problems.add(new F4(dimension));
        problems.add(new F5(dimension));
        problems.add(new F6(dimension));
        problems.add(new F7(dimension));
        problems.add(new F8(dimension));
        problems.add(new F9(dimension));
        problems.add(new F10(dimension));
        problems.add(new F11(dimension));
        problems.add(new F12(dimension));
        problems.add(new F13(dimension));
        problems.add(new F14(dimension));
        problems.add(new F15(dimension));
        problems.add(new F16(dimension));
        problems.add(new F17(dimension));
        problems.add(new F18(dimension));
        problems.add(new F19(dimension));
        problems.add(new F20(dimension));
        problems.add(new F21(dimension));
        problems.add(new F22(dimension));
        problems.add(new F23(dimension));
        problems.add(new F24(dimension));
        problems.add(new F25(dimension));
        problems.add(new F26(dimension));
        problems.add(new F27(dimension));
        problems.add(new F28(dimension));
        problems.add(new F29(dimension));

        ArrayList<NumberAlgorithm> algorithms = new ArrayList<>();

        //All parameters set according to paper/source code
        algorithms.add(new LSHADE());
        algorithms.add(new GWO());
        algorithms.add(new RandomSearch());
        algorithms.add(new SSA());
        algorithms.add(new GAOA());
        algorithms.add(new RSA());
        algorithms.add(new SCSO());
        algorithms.add(new AO());
        algorithms.add(new AVOA());

        for (NumberAlgorithm algorithm : algorithms) {
            System.out.println(algorithm.getId());
            long start = System.nanoTime();
            for (CEC2017 p : problems)
                createResultsFiles(cec2024, algorithm, p, 10 * dimension, cec2024.runs, cec2024.evaluations[0]);
            long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
            System.out.println("Duration for algorithm: " + duration + " s");
        }

    }

    private static void createResultsFiles(BenchmarkInfo info, NumberAlgorithm algorithm, CEC2017 problem, int resolution, int numberOfRuns, int numberOfEvaluations) {

        int numberOfCutpoints = numberOfEvaluations / resolution;
        if(info.k != numberOfCutpoints)
            throw new IllegalArgumentException("Number of cutpoints does not match!");

        Task<NumberSolution<Double>, DoubleProblem> task;
        System.out.println(problem.getName());
        long start = System.nanoTime();

        EvaluationStorage.Evaluation[][] evaluations = new EvaluationStorage.Evaluation[numberOfRuns][numberOfCutpoints];

        for (int run = 0; run < numberOfRuns; run++) {
            System.out.println("Run num: " + (run + 1));
            try {
                task = new Task<>(problem, StopCriterion.EVALUATIONS, numberOfEvaluations, 0, 0);
                task.enableEvaluationHistory();
                task.setStoreEveryNthEvaluation(1);
                algorithm.execute(task);
                ArrayList<EvaluationStorage.Evaluation> allEvaluations = task.getEvaluationHistory();

                int index = 0;
                double bestFitnessInCutpoint = allEvaluations.get(0).fitness; // store the best found solution until the current cutpoint
                for (int j = 1; j < numberOfEvaluations; j++) {

                    if (task.problem.isFirstBetter(allEvaluations.get(j).fitness, bestFitnessInCutpoint, 0))
                        bestFitnessInCutpoint = allEvaluations.get(j).fitness;

                    if ((j + 1) % resolution == 0) {
                        allEvaluations.get(j).fitness = bestFitnessInCutpoint;
                        evaluations[run][index++] = allEvaluations.get(j);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        StringBuilder sb = new StringBuilder();

        for (int cutpoint = 0; cutpoint < numberOfCutpoints; cutpoint++) {
            for (int run = 0; run < numberOfRuns; run++) {
                double errorValue = evaluations[run][cutpoint].fitness - problem.getGlobalOptima()[0]; // error value - difference between the best found solution and the global optimum
                sb.append(errorValue).append(" ");
            }
            sb.append("\n");
        }

        long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
        System.out.println("Duration for problem: " + duration + " s");

        String projectDirectory = System.getProperty("user.dir");
        File projectDirFile = new File(projectDirectory);
        String algorithmResultsDir = projectDirFile + File.separator + "results_files" + File.separator + info.name;

        String fileName = algorithm.getId() + "_" + info.name + "_" + problem.getFunctionNumber() + "_" + problem.getNumberOfDimensions() + ".txt";
        Util.writeToFile(algorithmResultsDir + File.separator + fileName, sb.toString());
    }
}
