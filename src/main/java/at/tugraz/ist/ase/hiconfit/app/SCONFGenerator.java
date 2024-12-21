/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2024
 *
 * @author: Viet-Man Le (v.m.le@tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.app;

import at.tugraz.ist.ase.hiconfit.app.cli.AppConfig;
import at.tugraz.ist.ase.hiconfit.cacdr_core.Assignment;
import at.tugraz.ist.ase.hiconfit.cacdr_core.Requirement;
import at.tugraz.ist.ase.hiconfit.cacdr_core.Solution;
import at.tugraz.ist.ase.hiconfit.cacdr_core.reader.SolutionReader;
import at.tugraz.ist.ase.hiconfit.cacdr_core.writer.MultiLineTxtSolutionWriter;
import at.tugraz.ist.ase.hiconfit.common.MailService;
import at.tugraz.ist.ase.hiconfit.common.RandomUtils;
import at.tugraz.ist.ase.hiconfit.common.Utils;
import at.tugraz.ist.ase.hiconfit.common.cfg.TomlConfigLoader;
import at.tugraz.ist.ase.hiconfit.common.cli.CmdLineOptions;
import at.tugraz.ist.ase.hiconfit.fm.factory.FeatureModels;
import at.tugraz.ist.ase.hiconfit.fm.parser.FeatureModelParserException;
import at.tugraz.ist.ase.hiconfit.kb.fm.FMKB;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static at.tugraz.ist.ase.hiconfit.common.IOUtils.checkAndCreateFolder;

@Slf4j
public class SCONFGenerator {

    static String outputFileTemplate = "sconf_%s_%d_%d.txt";

    public static void main(String[] args) throws FeatureModelParserException, IOException {
        val programTitle = "SCONF Generator";
        val usage = "Usage: java -jar sconf_gen.jar [options]";

        // Parse command line arguments
//        val cmdLineOptions = new CmdLineOptionsWithCfg(null, programTitle, null, usage);
        val cmdLineOptions = CmdLineOptions.withCfg(programTitle, usage);
        cmdLineOptions.parseArgument(args);

        if (cmdLineOptions.isHelp()) {
            cmdLineOptions.printUsage();
            System.exit(0);
        }

        cmdLineOptions.printWelcome();

        // Read configurations
        val confFile = cmdLineOptions.getConfFile() == null ? AppConfig.defaultConfigFile_SCONFGenerator : cmdLineOptions.getConfFile();
//        val cfg = ConfigLoader.loadConfig(confFile);
        val cfg = TomlConfigLoader.loadConfig(confFile, AppConfig.class);

        printConf(cfg);
        MailService mailService;
        if (cfg.getEmailAddress() != null && cfg.getEmailPass() != null) {
            mailService = new MailService(cfg.getEmailAddress(), cfg.getEmailPass());
        } else {
            mailService = null;
        }

        val fmFile = new File(cfg.getKBFilepath());
        val confsFolder = new File(cfg.getConfPath());
        val outputFolder = cfg.getOutputFolder();

        // check the output folder
        checkAndCreateFolder(outputFolder);

        val featureModel = FeatureModels.fromFile(fmFile);
        val fmKB = new FMKB<>(featureModel, false);

        // read configuration files from the confs directory
        for (final File file : Objects.requireNonNull(confsFolder.listFiles())) {
            String fileName = file.getName();
            if (fileName.endsWith(".txt")) {
                System.out.println("=============================");
                System.out.println("Processing: " + fileName);

                SolutionReader reader = new SolutionReader(fmKB);
                Requirement configuration = reader.read(file);

                // extract the number of valid_conf_1.txt
                String confIndex = fileName.split("_")[2].replace(".txt", "");

                // select SCONF
                for (int size : cfg.getSizeSCONFs()) {
                    selectSCONF(configuration, size, cfg.getMaxCombinations(), outputFolder, confIndex);
                }
            }
        }

        if (mailService != null) {
            mailService.sendMail(cfg.getEmailAddress(), cfg.getEmailAddress(), "DONE sconf_gen.sh - " + cfg.getMachine(), "Sconf generation is done!");
        }
    }

    public static void selectSCONF(Requirement configuration,
                                   int sizeSCONF,
                                   int maxCombinations,
                                   String outputFolder,
                                   String confIndex) throws IOException {

        if (sizeSCONF <= configuration.size()) {

            Integer[] indexesArr = createIndexesArray(configuration.size());
            Set<Integer> targetSet = Sets.newHashSet(indexesArr);

            // generate variable_combinations
            Set<Set<Integer>> var_combinations = Sets.combinations(targetSet, sizeSCONF);

            System.out.println("-----------------------------");
            System.out.println("\tSize of SCONF: " + sizeSCONF);
            System.out.println("\t\tNumber of combinations: " + var_combinations.size());

            // select randomly maximum 100_000 variable combinations for each card
            int numSelectedVarComb = Math.min(var_combinations.size(), maxCombinations);

            // select variable_combinations
            List<Integer> selected_var_combinations = selectIndexes(numSelectedVarComb, var_combinations.size(), false);

            System.out.println("\t\tGet selected combinations");
            Set<Set<Integer>> selectedVarCombs = Utils.getSelectedCombinations(var_combinations, selected_var_combinations);
            System.out.println("\t\tNumber of selected combinations: " + selectedVarCombs.size());

            // foreach the selected combination
            int counter = 0;
            for (Set<Integer> var_comb : selectedVarCombs) {
                System.out.println("\t\t\t" + ++counter + " - " + var_comb);

                // get assignments from the configuration based on the selected indexes
                List<Assignment> SCONF = var_comb.stream()
                        .map(i -> configuration.getAssignment(i - 1))
                        .map(assignment -> {
                            try {
                                return (Assignment) assignment.clone();
                            } catch (CloneNotSupportedException e) {
                                throw new RuntimeException(e);
                            }
                        }).toList();

                String outputFile = String.format(outputFileTemplate, confIndex, sizeSCONF, counter);
                MultiLineTxtSolutionWriter writer = new MultiLineTxtSolutionWriter(outputFolder);
//                // order assignments in the SCONF according to assignments in configuration
//                List<Assignment> orderedSCONF = configuration.getAssignments().stream()
//                        .filter(SCONF::contains)
//                        .collect(Collectors.toList());

                // save
                writer.write(Solution.builder().assignments(SCONF).build(), outputFile);
            }
        }
    }

    private static Integer[] createIndexesArray(int numVar) {
        return IntStream.range(0, numVar).mapToObj(i -> i + 1).toArray(Integer[]::new);
    }

    /**
     * randomly select #numIndexes combinations from #indexesSize combinations
     * @param numIndexes the number of combinations to be selected
     * @param indexesSize the size of the list of combinations
     * @param sort        true if the selected indexes are sorted
     * @return list of combination indexes
     */
    private static List<Integer> selectIndexes(int numIndexes, int indexesSize, boolean sort) {
        List<Integer> selectedIndexes = new ArrayList<>();

        if (indexesSize > numIndexes) {
            int index;
            for (int i = 0; i < numIndexes; i++) {
                do {
                    index = RandomUtils.getRandomInt(indexesSize); // 0 - indexes.size() - 1
                } while (selectedIndexes.contains(index));
                selectedIndexes.add(index);

                if (sort) {
                    Collections.sort(selectedIndexes);
                }
            }
        } else {
            selectedIndexes = IntStream.range(0, indexesSize).boxed().collect(Collectors.toList());

            if (!sort) {
                Collections.shuffle(selectedIndexes);
            }
        }
        return selectedIndexes;
    }

    private static void printConf(AppConfig config) {
        System.out.println("Configurations:");
        System.out.println("\tnameKB: " + config.getNameKB());
        System.out.println("\tkbPath: " + config.getKbPath());
        System.out.println("\tconfPath: " + config.getConfPath());
        System.out.println("\tsizeSCONFs: " + config.getSizeSCONFs());
        System.out.println("\toutputFolder: " + config.getOutputFolder());
    }
}
