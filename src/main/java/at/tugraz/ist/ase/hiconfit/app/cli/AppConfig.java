/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2024
 *
 * @author: Viet-Man Le (v.m.le@tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.app.cli;

import at.tugraz.ist.ase.hiconfit.common.cfg.BaseAppConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AppConfig extends BaseAppConfig {
    public static String defaultConfigFile_SCONFGenerator = "./conf/sconf_gen_arcade-game.cfg";
    public static String defaultConfigFile_CXPlainEvaluation = "./conf/cxplain_eval.cfg";

    @JsonProperty("nameKB")
    private String nameKB;

    @JsonProperty("kbPath")
    private String kbPath;

    @JsonProperty("confPath")
    private String confPath;

    @JsonProperty("sizeSCONFs")
    private List<Integer> sizeSCONFs;

    @JsonProperty("outputFolder")
    private String outputFolder;

    @JsonProperty("fullnameKBs")
    private List<String> fullnameKBs;

    @JsonProperty("sconfPath")
    private String sconfPath;

    @JsonProperty("numConfs")
    private int numConfs;

    @JsonProperty("maxCombinations")
    private int maxCombinations;

    @JsonProperty("machine")
    private String machine;

    @JsonProperty("emailAddress")
    private String emailAddress;

    @JsonProperty("emailPass")
    private String emailPass;

    @JsonProperty("printResult")
    private boolean printResult;

    public String getNameKB(String fullnameKB) {
        int index = fullnameKB.lastIndexOf('.');
        return (index != -1) ? fullnameKB.substring(0, index) : fullnameKB;
    }

    public String getKBFilepath() {
        return kbPath + nameKB;
    }
}
