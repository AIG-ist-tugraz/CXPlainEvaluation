/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2024
 *
 * @author: Viet-Man Le (vietman.le@ist.tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.app;

import at.tugraz.ist.ase.hiconfit.CXPlain;
import at.tugraz.ist.ase.hiconfit.app.cli.AppConfig;
import at.tugraz.ist.ase.hiconfit.cacdr.checker.ChocoConsistencyChecker;
import at.tugraz.ist.ase.hiconfit.cacdr.eval.CAEvaluator;
import at.tugraz.ist.ase.hiconfit.cacdr_core.Requirement;
import at.tugraz.ist.ase.hiconfit.cacdr_core.reader.SolutionReader;
import at.tugraz.ist.ase.hiconfit.common.LoggerUtils;
import at.tugraz.ist.ase.hiconfit.common.MailService;
import at.tugraz.ist.ase.hiconfit.common.cfg.TomlConfigLoader;
import at.tugraz.ist.ase.hiconfit.common.cli.CmdLineOptions;
import at.tugraz.ist.ase.hiconfit.eval.PerformanceEvaluator;
import at.tugraz.ist.ase.hiconfit.fm.FMCausalExplanationModel;
import at.tugraz.ist.ase.hiconfit.fm.FMCausalExplanationModelFactory;
import at.tugraz.ist.ase.hiconfit.fm.core.AbstractRelationship;
import at.tugraz.ist.ase.hiconfit.fm.core.CTConstraint;
import at.tugraz.ist.ase.hiconfit.fm.core.Feature;
import at.tugraz.ist.ase.hiconfit.fm.factory.FeatureModels;
import at.tugraz.ist.ase.hiconfit.fm.parser.FeatureModelParserException;
import at.tugraz.ist.ase.hiconfit.kb.core.Constraint;
import at.tugraz.ist.ase.hiconfit.kb.fm.FMKB;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static at.tugraz.ist.ase.hiconfit.CXPlain.TIMER_CXPLAIN;
import static at.tugraz.ist.ase.hiconfit.app.ConsoleUtils.printMessage;
import static at.tugraz.ist.ase.hiconfit.cacdr.checker.ChocoConsistencyChecker.TIMER_SOLVER;
import static at.tugraz.ist.ase.hiconfit.cacdr.eval.CAEvaluator.*;
import static at.tugraz.ist.ase.hiconfit.common.IOUtils.checkAndCreateFolder;
import static java.lang.System.out;

/**
 * Evaluation of the CXPlain algorithm.
 */
@Slf4j
public class CXPlainEvaluation {

    public static void main(String[] args) throws FeatureModelParserException, IOException {
        val programTitle = "CXPlain Evaluation";
        val usage = "Usage: java -jar cxplain_eval.jar [options]";

        // Parse command line arguments
//        val cmdLineOptions = CmdLineOptionsFactory.getInstance(programTitle, usage);
        val cmdLineOptions = CmdLineOptions.withCfg(programTitle, usage);
        cmdLineOptions.parseArgument(args);

        if (cmdLineOptions.isHelp()) {
            cmdLineOptions.printUsage();
            System.exit(0);
        }

        cmdLineOptions.printWelcome();

        // Read configurations
        val appConfFile = cmdLineOptions.getConfFile() == null ? AppConfig.defaultConfigFile_CXPlainEvaluation : cmdLineOptions.getConfFile();

        val cfg = TomlConfigLoader.loadConfig(appConfFile, AppConfig.class);

        printConf(cfg);
        MailService mailService;
        if (cfg.getEmailAddress() != null && cfg.getEmailPass() != null) {
            mailService = new MailService(cfg.getEmailAddress(), cfg.getEmailPass());
        } else {
            mailService = null;
        }

        BufferedWriter resultWriter;
        if (cfg.isPrintResult()) {
            resultWriter = new BufferedWriter(new FileWriter(cfg.getOutputFolder() + "result.txt"));
        } else {
            resultWriter = null;
        }
        LoggerUtils.setUseThreadInfo(false);

        // check the output folder
        checkAndCreateFolder(cfg.getOutputFolder());

        // warm up
        evaluate(cfg, null);

        // TODO - Migrate to hiconfit-core
        Dictionary<String, Dictionary<String, Dictionary<String, Double>>> results = evaluate(cfg, resultWriter);

        List<String> namKBs = cfg.getFullnameKBs().reversed();
        // print results for each measure
        printMessage("=========================================", resultWriter);
        printMessage("Results solver runtime:", resultWriter);
        printResultTable(namKBs, resultWriter, cfg.getSizeSCONFs(), results, "solver_runtime");
        printMessage("Results CXPlain runtime:", resultWriter);
        printResultTable(namKBs, resultWriter, cfg.getSizeSCONFs(), results, "cxplain_runtime");
        printMessage("Results Consistency checks:", resultWriter);
        printResultTable(namKBs, resultWriter, cfg.getSizeSCONFs(), results, "consistency_checks");
        printMessage("Results Solver calls:", resultWriter);
        printResultTable(namKBs, resultWriter, cfg.getSizeSCONFs(), results, "solver_calls");

        if (mailService != null) {
            mailService.sendMail(cfg.getEmailAddress(), cfg.getEmailAddress(), "DONE CXPLain evaluation", "DONE CXPLain evaluation");
        }
    }

    private static Dictionary<String, Dictionary<String, Dictionary<String, Double>>>
        evaluate(AppConfig cfg, BufferedWriter resultWriter) throws FeatureModelParserException, IOException {

        Dictionary<String, Dictionary<String, Dictionary<String, Double>>> results = new Hashtable<>();
        // loop through all variant feature models
        for (String fullnameKB : cfg.getFullnameKBs()) {

            printMessage("=========================================", resultWriter);
            val fmFile = new File(cfg.getKbPath() + fullnameKB);
            val fm = FeatureModels.fromFile(fmFile);
            printMessage("\tEvaluating " + fmFile.getName() + "...", resultWriter);

            // add fullnameKB to results
            results.put(fullnameKB, new Hashtable<>());

            for (int size : cfg.getSizeSCONFs()) {
                printMessage("\t----------------------------------------", resultWriter);
                printMessage("\t\tSize " + size + "...", resultWriter);

                // measures
                List<Double> solver_runtimes = new ArrayList<>();
                List<Double> cxplain_runtimes = new ArrayList<>();
                List<Double> consistency_checks = new ArrayList<>();
                List<Double> solver_calls = new ArrayList<>();

                for (int i = 1; i <= cfg.getNumConfs(); i++) {
//                    for (int j = 1; j <= cfg.getNumVariants(); j++) {

                    val confFile = new File(cfg.getConfPath() + cfg.getNameKB(fullnameKB) + String.format("/valid_conf_%d.txt", i));
                    val sconfFolder = new File(cfg.getSconfPath() + cfg.getNameKB(fullnameKB));

                    // get all file names in the sconf folder
                    // filter by "sconf_" + i + "_" + size + "_"
                    int finalI = i;
                    val sconfFiles = Arrays.stream(Objects.requireNonNull(sconfFolder.listFiles()))
                            .filter(file -> file.getName().contains(String.format("sconf_%d_%d_", finalI, size)))
                            .toArray(File[]::new);

                    // loop through all sconf files
                    for (File sconfFile : sconfFiles) {
//                        val sconfFile = new File(cfg.getSconfPath() + cfg.getNameKB(fullnameKB) + String.format("/sconf_%d_%d_%d.txt", i, size, j));

                        // check exist files
                        if (!sconfFile.exists()) {
                            printMessage("\t\tNo sconf: " + sconfFile.getName(), resultWriter);
                            continue;
                        }

                        printMessage("\t\tEvaluating " + confFile.getName() + " and " + sconfFile.getName() + "...", resultWriter);

                        // read configuration and sconf
                        val fmKB = new FMKB<>(fm, false);
                        SolutionReader reader = new SolutionReader(fmKB);
                        Requirement conf = reader.read(sconfFile);
                        Requirement sconf = reader.read(sconfFile);
                        Requirement userRequirement = Requirement.requirementBuilder().assignments(List.of()).build();

                        // CHECK CONSISTENCY
                        val factory = FMCausalExplanationModelFactory.getInstance(fm, sconf, userRequirement, conf);
                        val diagModel = (FMCausalExplanationModel<Feature, AbstractRelationship<Feature>, CTConstraint>)factory.createModel();

//                        System.out.println("\tNumber of constraints: " + diagModel.getAllConstraints().size());

                        val checker = new ChocoConsistencyChecker(diagModel);

                        val REQ = diagModel.getREQ();
                        val CF = diagModel.getCF();
                        val CONF = diagModel.getCONF();
                        val NSCONF = diagModel.getNSCONF();

                        PerformanceEvaluator.reset();
                        setCommonTimer(TIMER_SOLVER);
                        setCommonTimer(TIMER_CXPLAIN);

                        val cxPlain = new CXPlain(checker);

                        CAEvaluator.reset();
                        Set<Constraint> explanation = cxPlain.findExplanation(REQ, CF, CONF, NSCONF);

                        double solver_runtime = (double) totalCommonTimer(TIMER_SOLVER) / 1_000_000_000.0;
                        double cxplain_runtime = (double) totalCommonTimer(TIMER_CXPLAIN) / 1_000_000_000.0;
                        double cc = getCounter(COUNTER_CONSISTENCY_CHECKS).getValue();
                        double sc = getCounter(COUNTER_CHOCO_SOLVER_CALLS).getValue();

                        printMessage("\t\t\tExplanation: " + explanation, resultWriter);
                        printMessage("\t\t\tSolver runtime: " + solver_runtime, resultWriter);
                        printMessage("\t\t\tMergeFM runtime: " + cxplain_runtime, resultWriter);
                        printMessage("\t\t\tConsistency checks: " + cc, resultWriter);
                        printMessage("\t\t\tSolver calls: " + sc, resultWriter);

                        solver_runtimes.add(solver_runtime);
                        cxplain_runtimes.add(cxplain_runtime);
                        consistency_checks.add(cc);
                        solver_calls.add(sc);
                    }
                }

                // average measures
                double avg_solver_runtime = solver_runtimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double avg_cxplain_runtime = cxplain_runtimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double avg_cc = consistency_checks.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double avg_sc = solver_calls.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                printMessage("\t\t-----------------------------------", resultWriter);
                printMessage("\t\tAverage solver runtime: " + avg_solver_runtime, resultWriter);
                printMessage("\t\tAverage CXPlain runtime: " + avg_cxplain_runtime, resultWriter);
                printMessage("\t\tAverage Consistency checks: " + avg_cc, resultWriter);
                printMessage("\t\tAverage Solver calls: " + avg_sc, resultWriter);

                // store results
                String strSize = Integer.toString(size);
                Dictionary<String, Double> sizeResults = new Hashtable<>();
                sizeResults.put("solver_runtime", avg_solver_runtime);
                sizeResults.put("cxplain_runtime", avg_cxplain_runtime);
                sizeResults.put("consistency_checks", avg_cc);
                sizeResults.put("solver_calls", avg_sc);
                results.get(fullnameKB).put(strSize, sizeResults);
            }
        }
        return results;
    }

    private static void printResultTable(List<String> namKBs,
                                         BufferedWriter resultWriter,
                                         List<Integer> sizeSCONFs,
                                         Dictionary<String, Dictionary<String, Dictionary<String, Double>>> results,
                                         String key) {
        StringBuilder header = new StringBuilder();
        namKBs.forEach(nameKB -> header.append("\t").append(nameKB));
        printMessage(header.toString(), resultWriter);

        sizeSCONFs.forEach(size -> {
            StringBuilder line = new StringBuilder("\tSize ").append(size).append(":");
            namKBs.forEach(nameKB -> {
                Double value = results.get(nameKB).get(Integer.toString(size)).get(key);
                line.append(" ").append(value);
            });
            printMessage(line.toString(), resultWriter);
        });
    }

    private static void printConf(AppConfig config) {
        out.println("Configurations:");
        out.println("\tnameKBs: " + config.getFullnameKBs());
        out.println("\tkbPath: " + config.getKbPath());
        out.println("\tconfPath: " + config.getConfPath());
        out.println("\tsconfPath: " + config.getSconfPath());
        out.println("\tsizeSCONFs: " + config.getSizeSCONFs());
        out.println("\tnumConfs: " + config.getNumConfs());
        out.println("\toutputFolder: " + config.getOutputFolder());
        out.println("\tprintResult: " + config.isPrintResult());
    }
}
