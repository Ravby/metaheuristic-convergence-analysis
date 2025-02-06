package si.um.feri.lpm;

import java.util.HashMap;
import java.util.Map;

public class BenchmarkManager {
    private static Map<BenchmarkId, BenchmarkInfo> benchmarks = new HashMap<>();
    private static BenchmarkManager instance = new BenchmarkManager();

    private BenchmarkManager() {

        BenchmarkInfo cec2024cmop = new BenchmarkInfo("CEC2024CMOP", BenchmarkId.CEC2024CMOP, 15, 30, 1000, new int[]{30}, new int[]{200000}, new String[]{"DESDE", "CCEMT", "IMTCMO", "CCPTEA", "MTCMMO"}); //15 files for each algorithm
        BenchmarkInfo cec2024 = new BenchmarkInfo("CEC2024", BenchmarkId.CEC2024, 29, 25, 1000, new int[]{30}, new int[]{300000}, new String[]{"L-SRTDE", "RDE", "BlockEA", "mLSAHDE-RL", "jSOa", "IEACOP"}); //29 files for each algorithm
        BenchmarkInfo cec2022 = new BenchmarkInfo("CEC2022", BenchmarkId.CEC2022, 12, 30, 16, new int[]{10, 20}, new int[]{200000, 1000000}, new String[]{"EA4eigN100-10", "NL-SHADE-LBC", "NL-SHADE-RSP-MID", "S-LSHADE-DP", "jSObinexpEig", "MTT-SHADE", "IUMOEAII", "IMPML-SHADE", "NLSOMACLP", "ZOCMAES", "OMCSOMA", "Co-PPSO", "SPHH-Ensemble"}); //24 files for each algorithm
        BenchmarkInfo cec2021 = new BenchmarkInfo("CEC2021", BenchmarkId.CEC2021, 10, 30, 16, new int[]{10, 20}, new int[]{200000, 1000000}, new String[]{"DEDMNA", "APGSK_IMODE", "MadDE", "RB_IPOP_CMAES", "jDE21", "NL-SHADE-RSP", "SOMA-CLP", "MLS-LSHADE", "LSHADE"}); //100 files for each algorithm
        BenchmarkInfo cec2017 = new BenchmarkInfo("CEC2017", BenchmarkId.CEC2017, 30, 51, 14, new int[]{10, 30, 50, 100}, new int[]{100000, 300000, 500000, 1000000}, new String[]{"EBOwithCMAR", "jSO", "LSHADE-cnEpSin", "LSHADE-SPACMA", "DES", "MM-OED", "IDEbestNsize", "RB-IPOP-CMA-ES", "MOS-SOCO2013", "MOS-SOCO2011", "PPSO", "DYYPO", "TLBO-FL"}); //120 files for each algorithm (MaxFEs = 10000 * D)

        benchmarks.put(cec2024cmop.id, cec2024cmop);
        benchmarks.put(cec2024.id, cec2024);
        benchmarks.put(cec2022.id, cec2022);
        benchmarks.put(cec2021.id, cec2021);
        benchmarks.put(cec2017.id, cec2017);
    }

    public static BenchmarkManager getInstance() {
        return instance;
    }

    public static Map<BenchmarkId, BenchmarkInfo> getAll() {
        return benchmarks;
    }

    public static BenchmarkInfo get(BenchmarkId id) {
        return benchmarks.get(id);
    }
}