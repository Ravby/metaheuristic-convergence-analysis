package si.um.feri.lpm;

import org.um.feri.ears.algorithms.DummyAlgorithm;
import org.um.feri.ears.algorithms.NumberAlgorithm;
import org.um.feri.ears.benchmark.Benchmark;
import org.um.feri.ears.statistic.rating_system.Player;
import org.um.feri.ears.statistic.rating_system.RatingType;
import org.um.feri.ears.statistic.rating_system.glicko2.TournamentResults;
import org.um.feri.ears.visualization.rating.RatingIntervalPlot;
import org.um.feri.ears.util.Util;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class RunCec2024 {

    public static void main(String[] args) {

        Benchmark.printInfo = false;
        boolean displayRatingCharts = true;

        BenchmarkInfo cec2024 = BenchmarkManager.get(BenchmarkId.CEC2024);

        String projectDirectory = System.getProperty("user.dir");
        File projectDirFile = new File(projectDirectory);
        String algorithmResultsDir = projectDirFile + File.separator + "results_files" + File.separator + cec2024.name;
        String experimentalResultsDir = projectDirFile + File.separator + "experimental_results" + File.separator + cec2024.name;

        ArrayList<NumberAlgorithm> players = new ArrayList<>();

        DummyAlgorithm.FileFormat fileFormat = DummyAlgorithm.FileFormat.CEC_RESULTS_FORMAT;

        players.add(new DummyAlgorithm("jSOa", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("L-SRTDE", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("BlockEA", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("iEACOP", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("mLSHADE-RL", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("RDE", algorithmResultsDir, fileFormat));

        //additional algorithms, which didn't compete in the official competition
        //reference algorithms
        players.add(new DummyAlgorithm("LSHADE*","LSHADE", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("GWO*","GWO", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("RS*","RS", algorithmResultsDir, fileFormat));
        //new highly cited with source code
        players.add(new DummyAlgorithm("SFOA*","SFOA", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("APO*","APO", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("HOA*","HOA", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("PO*","PO", algorithmResultsDir, fileFormat));



        HashMap<String, ArrayList<String>> playerRatings;
        playerRatings = new HashMap<>();
        for (int k = 0; k < cec2024.k; k++) {
            Cec2024StoredBenchmark cec2024Benchmark = new Cec2024StoredBenchmark(k);
            cec2024Benchmark.setDisplayRatingCharts(false);
            cec2024Benchmark.setDisplayAdvancedStats(false);
            cec2024Benchmark.addAlgorithms(players);
            cec2024Benchmark.run(10); //cec2024.runs
            TournamentResults tournamentResults = cec2024Benchmark.getTournamentResults();
            ArrayList<Player> playerResults = tournamentResults.getPlayers();
            for (Player player : playerResults) {
                if (!playerRatings.containsKey(player.getId())) {
                    playerRatings.put(player.getId(), new ArrayList<>());
                }
                playerRatings.get(player.getId()).add(player.getGlicko2Rating().getRating() + " " + player.getGlicko2Rating().getRatingDeviation());
            }
            //save final results
            if (k + 1 == cec2024.k) {
                tournamentResults.saveToFile(experimentalResultsDir + File.separator + cec2024.name + "_final_results");
                if (displayRatingCharts) {
                    RatingIntervalPlot.displayChart(tournamentResults.getPlayers(), RatingType.GLICKO2, "Rating Interval");
                }
            }

        }
        //save rating interval bands
        StringBuilder sb = new StringBuilder();
        for (String playerId : playerRatings.keySet()) {
            ArrayList<String> rating = playerRatings.get(playerId);

            sb.append(playerId).append("\n");
            for (String r : rating) {
                sb.append(r).append("\n");
            }
            Util.writeToFile(experimentalResultsDir + File.separator + playerId + "_rating_interval_band.txt", sb.toString());
            sb.setLength(0);
        }

        //run only last cutpoint
        Cec2024StoredBenchmark cec2024BenchmarkFinalResults = new Cec2024StoredBenchmark(cec2024.k - 1);
        cec2024BenchmarkFinalResults.setDisplayRatingCharts(false);
        cec2024BenchmarkFinalResults.setDisplayAdvancedStats(false);
        cec2024BenchmarkFinalResults.addAlgorithms(players);
        cec2024BenchmarkFinalResults.run(10); //cec2024.runs
        TournamentResults tournamentFinalResults = cec2024BenchmarkFinalResults.getTournamentResults();
        tournamentFinalResults.saveToFile(experimentalResultsDir + File.separator + cec2024.name + "_final_results");


        for(NumberAlgorithm player : players) {
            ((DummyAlgorithm)player).resetRunNumbers();
        }

        //save whole convergence graphs
        int cutpointStep = 10;  // every 10th of the 1000 cutpoints
        int cutpointsUsed = (cec2024.k + cutpointStep - 1) / cutpointStep;

        Cec2024StoredBenchmark cec2024StoredBenchmark = new Cec2024StoredBenchmark();
        cec2024StoredBenchmark.setDisplayRatingCharts(false);
        cec2024StoredBenchmark.setDisplayAdvancedStats(false);
        cec2024StoredBenchmark.addAlgorithms(players);
        cec2024StoredBenchmark.wholeConvergenceGraph = true;
        cec2024StoredBenchmark.cutpointStep = cutpointStep;
        cec2024StoredBenchmark.run(10); //cec2024.runs
        TournamentResults tournamentResults = cec2024StoredBenchmark.getTournamentResults();
        tournamentResults.saveToFile(experimentalResultsDir + File.separator + cec2024.name + "_" + cutpointsUsed + "CP_whole_convergence_graph");
    }
}
