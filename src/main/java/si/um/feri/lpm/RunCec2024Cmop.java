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

public class RunCec2024Cmop {

    public static void main(String[] args) {

        Benchmark.printInfo = false;
        boolean displayRatingCharts = true;

        BenchmarkInfo cec2024cmop = BenchmarkManager.get(BenchmarkId.CEC2024CMOP);

        String projectDirectory = System.getProperty("user.dir");
        File projectDirFile = new File(projectDirectory);
        String algorithmResultsDir = projectDirFile + File.separator + "results_files" + File.separator + cec2024cmop.name;
        String experimentalResultsDir = projectDirFile + File.separator + "experimental_results" + File.separator + cec2024cmop.name;

        ArrayList<NumberAlgorithm> players = new ArrayList<>();

        DummyAlgorithm.FileFormat fileFormat = DummyAlgorithm.FileFormat.CEC_RESULTS_FORMAT;

        players.add(new DummyAlgorithm("CCEMT", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("CCPTEA", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("DESDE", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("IMTCMO", algorithmResultsDir, fileFormat));
        players.add(new DummyAlgorithm("MTCMMO", algorithmResultsDir, fileFormat));

        HashMap<String, ArrayList<String>> playerRatings;
        playerRatings = new HashMap<>();
        for (int k = 0; k < cec2024cmop.k; k++) {
            Cec2024CmopStoredBenchmark cec2024CmopBenchmark = new Cec2024CmopStoredBenchmark(k);
            cec2024CmopBenchmark.setDisplayRatingCharts(false);
            cec2024CmopBenchmark.setDisplayAdvancedStats(false);
            cec2024CmopBenchmark.addAlgorithms(players);
            cec2024CmopBenchmark.run(cec2024cmop.runs);
            TournamentResults tournamentResults = cec2024CmopBenchmark.getTournamentResults();
            ArrayList<Player> playerResults = tournamentResults.getPlayers();
            for (Player player : playerResults) {
                if (!playerRatings.containsKey(player.getId())) {
                    playerRatings.put(player.getId(), new ArrayList<>());
                }
                playerRatings.get(player.getId()).add(player.getGlicko2Rating().getRating() + " " + player.getGlicko2Rating().getRatingDeviation());
            }
            //save final results
            if (k + 1 == cec2024cmop.k) {
                tournamentResults.saveToFile(experimentalResultsDir + File.separator + cec2024cmop.name + "_final_results");
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

        for(NumberAlgorithm player : players) {
            ((DummyAlgorithm)player).resetRunNumbers();
        }

        //save whole convergence graphs
        Cec2024CmopStoredBenchmark cec2024CmopStoredBenchmark = new Cec2024CmopStoredBenchmark();
        cec2024CmopStoredBenchmark.setDisplayRatingCharts(false);
        cec2024CmopStoredBenchmark.setDisplayAdvancedStats(false);
        cec2024CmopStoredBenchmark.addAlgorithms(players);
        cec2024CmopStoredBenchmark.wholeConvergenceGraph = true;
        cec2024CmopStoredBenchmark.run(cec2024cmop.runs);
        TournamentResults tournamentResults = cec2024CmopStoredBenchmark.getTournamentResults();
        tournamentResults.saveToFile(experimentalResultsDir + File.separator + cec2024cmop.name +"_whole_convergence_graph");

    }
}
