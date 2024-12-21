/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2024
 *
 * @author: Viet-Man Le (v.m.le@tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit;

import at.tugraz.ist.ase.hiconfit.cacdr.checker.ChocoConsistencyChecker;
import at.tugraz.ist.ase.hiconfit.cacdr.eval.CAEvaluator;
import at.tugraz.ist.ase.hiconfit.cacdr_core.builder.RequirementBuilder;
import at.tugraz.ist.ase.hiconfit.fm.FMCausalExplanationModel;
import at.tugraz.ist.ase.hiconfit.fm.FMCausalExplanationModelFactory;
import at.tugraz.ist.ase.hiconfit.fm.builder.ConstraintBuilder;
import at.tugraz.ist.ase.hiconfit.fm.builder.FeatureBuilder;
import at.tugraz.ist.ase.hiconfit.fm.builder.RelationshipBuilder;
import at.tugraz.ist.ase.hiconfit.fm.core.AbstractRelationship;
import at.tugraz.ist.ase.hiconfit.fm.core.CTConstraint;
import at.tugraz.ist.ase.hiconfit.fm.core.Feature;
import at.tugraz.ist.ase.hiconfit.fm.core.FeatureModel;
import at.tugraz.ist.ase.hiconfit.fm.translator.ConfRuleTranslator;
import at.tugraz.ist.ase.hiconfit.kb.CarConfKB;
import at.tugraz.ist.ase.hiconfit.kb.KBCausalExplanationModel;
import at.tugraz.ist.ase.hiconfit.kb.KBCausalExplanationModelFactory;
import at.tugraz.ist.ase.hiconfit.kb.core.Constraint;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static at.tugraz.ist.ase.hiconfit.cacdr.eval.CAEvaluator.printPerformance;
import static org.junit.jupiter.api.Assertions.*;

class CXPlainTest {
    FeatureModel<Feature, AbstractRelationship<Feature>, CTConstraint> createSurveyFM() {
        val translator = new ConfRuleTranslator();
        val constraintBuilder = new ConstraintBuilder(translator);
        val fm = new FeatureModel<>("test", new FeatureBuilder(), new RelationshipBuilder(translator), constraintBuilder);

        val survey = fm.addRoot("survey", "survey");
        val pay = fm.addFeature("pay", "pay");
        val ABtesting = fm.addFeature("ABtesting", "ABtesting");
        val statistics = fm.addFeature("statistics", "statistics");
        val qa = fm.addFeature("qa", "qa");
        val license = fm.addFeature("license", "license");
        val nonlicense = fm.addFeature("nonlicense", "nonlicense");
        val multiplechoice = fm.addFeature("multiplechoice", "multiplechoice");
        val multiplemedia = fm.addFeature("multiplemedia", "multiplemedia");

        fm.addMandatoryRelationship(survey, pay);
        fm.addOptionalRelationship(survey, ABtesting);
        fm.addOptionalRelationship(survey, statistics);
        fm.addMandatoryRelationship(survey, qa);
        fm.addAlternativeRelationship(pay, List.of(license, nonlicense));
        fm.addOrRelationship(qa, List.of(multiplechoice, multiplemedia));

        fm.addExcludes(ABtesting, nonlicense);
        fm.addRequires(ABtesting, statistics);

        return fm;
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCXPlain1() {
        val fm = createSurveyFM();

        val requirement = "ABtesting=true";
        val sconf_string = "license=true";
        val conf_string = "pay=true,nonlicense=false,ABtesting=true,statistics=true,qa=true,multiplechoice=true,multiplemedia=false";

        val builder = new RequirementBuilder();
        val userRequirement = builder.build(requirement);
        val sconf = builder.build(sconf_string);
        val configuration = builder.build(conf_string);

        // CHECK CONSISTENCY
        val factory = FMCausalExplanationModelFactory.getInstance(fm, sconf, userRequirement, configuration);
        val diagModel = (FMCausalExplanationModel<Feature, AbstractRelationship<Feature>, CTConstraint>)factory.createModel();

        System.out.println("\tNumber of constraints: " + diagModel.getAllConstraints().size());

        val checker = new ChocoConsistencyChecker(diagModel);

        val REQ = diagModel.getREQ();
        val CF = diagModel.getCF();
        val CONF = diagModel.getCONF();
        val NSCONF = diagModel.getNSCONF();

        val cxPlain = new CXPlain(checker);

        CAEvaluator.reset();
        Set<Constraint> explanation = cxPlain.findExplanation(REQ, CF, CONF, NSCONF);

        System.out.println("=========================================");
        System.out.println("Explanation found by CXPlain:");
        System.out.println(explanation);
        printPerformance();

        assertEquals(5, explanation.size());
        val expectedExpStr = "[ABtesting=true [copied], excludes(ABtesting, nonlicense), alternative(pay, license, nonlicense), mandatory(survey, pay), survey = true]";
        assertEquals(expectedExpStr, explanation.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCXPlain2() {
        val fm = createSurveyFM();

        val requirement = "ABtesting=true";
        val sconf_string = "multiplechoice=true";
        val conf_string = "survey=true,pay=true,license=true,nonlicense=false,ABtesting=true,statistics=true,qa=true,multiplechoice=true,multiplemedia=false";

        val builder = new RequirementBuilder();
        val userRequirement = builder.build(requirement);
        val sconf = builder.build(sconf_string);
        val configuration = builder.build(conf_string);

        // CHECK CONSISTENCY
        val factory = FMCausalExplanationModelFactory.getInstance(fm, sconf, userRequirement, configuration);
        val diagModel = (FMCausalExplanationModel<Feature, AbstractRelationship<Feature>, CTConstraint>)factory.createModel();

        System.out.println("\tNumber of constraints: " + diagModel.getAllConstraints().size());

        val checker = new ChocoConsistencyChecker(diagModel);

        val REQ = diagModel.getREQ();
        val CF = diagModel.getCF();
        val CONF = diagModel.getCONF();
        val NSCONF = diagModel.getNSCONF();

        val cxPlain = new CXPlain(checker);

        CAEvaluator.reset();
        Set<Constraint> explanation = cxPlain.findExplanation(REQ, CF, CONF, NSCONF);

        System.out.println("=========================================");
        System.out.println("Explanation found by CXPlain:");
        System.out.println(explanation);
        printPerformance();

        assertEquals(4, explanation.size());
        val expectedExp = "[multiplemedia=false, or(qa, multiplechoice, multiplemedia), mandatory(survey, qa), survey = true]";
        assertEquals(expectedExp, explanation.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCXPlain3() {
        val fm = createSurveyFM();

        val requirement = "ABtesting=true";
        val sconf_string = "multiplemedia=false";
        val conf_string = "survey=true,pay=true,license=true,nonlicense=false,ABtesting=true,statistics=true,qa=true,multiplechoice=true,multiplemedia=false";

        val builder = new RequirementBuilder();
        val userRequirement = builder.build(requirement);
        val sconf = builder.build(sconf_string);
        val configuration = builder.build(conf_string);

        // CHECK CONSISTENCY
        val factory = FMCausalExplanationModelFactory.getInstance(fm, sconf, userRequirement, configuration);
        val diagModel = (FMCausalExplanationModel<Feature, AbstractRelationship<Feature>, CTConstraint>)factory.createModel();

        System.out.println("\tNumber of constraints: " + diagModel.getAllConstraints().size());

        val checker = new ChocoConsistencyChecker(diagModel);

        val REQ = diagModel.getREQ();
        val CF = diagModel.getCF();
        val CONF = diagModel.getCONF();
        val NSCONF = diagModel.getNSCONF();

        val cxPlain = new CXPlain(checker);

        CAEvaluator.reset();
        Set<Constraint> explanation = cxPlain.findExplanation(REQ, CF, CONF, NSCONF);

        System.out.println("=========================================");
        System.out.println("Explanation found by CXPlain:");
        System.out.println(explanation);
        printPerformance();

        assertEquals(1, explanation.size());
        val expectedExp = "[multiplemedia=false]";
        assertEquals(expectedExp, explanation.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCXPlain4() {
        val fm = createSurveyFM();

        val requirement = "ABtesting=true";
        val sconf_string = "license=true,statistics=true";
        val conf_string = "survey=true,pay=true,license=true,nonlicense=false,ABtesting=true,statistics=true,qa=true,multiplechoice=true,multiplemedia=false";

        val builder = new RequirementBuilder();
        val userRequirement = builder.build(requirement);
        val sconf = builder.build(sconf_string);
        val configuration = builder.build(conf_string);

        // CHECK CONSISTENCY
        val factory = FMCausalExplanationModelFactory.getInstance(fm, sconf, userRequirement, configuration);
        val diagModel = (FMCausalExplanationModel<Feature, AbstractRelationship<Feature>, CTConstraint>)factory.createModel();

        System.out.println("\tNumber of constraints: " + diagModel.getAllConstraints().size());

        val checker = new ChocoConsistencyChecker(diagModel);

        val REQ = diagModel.getREQ();
        val CF = diagModel.getCF();
        val CONF = diagModel.getCONF();
        val NSCONF = diagModel.getNSCONF();

        val cxPlain = new CXPlain(checker);

        CAEvaluator.reset();
        val explanation = cxPlain.findExplanation(REQ, CF, CONF, NSCONF);

        System.out.println("=========================================");
        System.out.println("Explanation found by CXPlain:");
        System.out.println(explanation);
        printPerformance();

        assertEquals(6, explanation.size());
        val expectedExp = "[ABtesting=true [copied], requires(ABtesting, statistics), excludes(ABtesting, nonlicense), alternative(pay, license, nonlicense), mandatory(survey, pay), survey = true]";
        assertEquals(expectedExp, explanation.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCXPlain5() {
        val fm = createSurveyFM();

        val requirement = "ABtesting=true";
        val sconf_string = "multiplemedia=false,license=true";
        val conf_string = "survey=true,pay=true,license=true,nonlicense=false,ABtesting=true,statistics=true,qa=true,multiplechoice=true,multiplemedia=false";

        val builder = new RequirementBuilder();
        val userRequirement = builder.build(requirement);
        val sconf = builder.build(sconf_string);
        val configuration = builder.build(conf_string);

        // CHECK CONSISTENCY
        val factory = FMCausalExplanationModelFactory.getInstance(fm, sconf, userRequirement, configuration);
        val diagModel = (FMCausalExplanationModel<Feature, AbstractRelationship<Feature>, CTConstraint>)factory.createModel();

        System.out.println("\tNumber of constraints: " + diagModel.getAllConstraints().size());

        val checker = new ChocoConsistencyChecker(diagModel);

        val REQ = diagModel.getREQ();
        val CF = diagModel.getCF();
        val CONF = diagModel.getCONF();
        val NSCONF = diagModel.getNSCONF();

        val cxPlain = new CXPlain(checker);

        CAEvaluator.reset();
        val explanation = cxPlain.findExplanation(REQ, CF, CONF, NSCONF);

        System.out.println("=========================================");
        System.out.println("Explanation found by CXPlain:");
        System.out.println(explanation);
        printPerformance();

        assertEquals(6, explanation.size());
        val expectedExp = "[multiplemedia=false, ABtesting=true [copied], excludes(ABtesting, nonlicense), alternative(pay, license, nonlicense), mandatory(survey, pay), survey = true]";
        assertEquals(expectedExp, explanation.toString());
    }

    @Test
    void testCXPlain6() {
        val kb = new CarConfKB(false);

        val requirement = "biz-park=y,rec-park=y";
        val sconf_string = "easy-parking=y";
        val conf_string = "biz-park=y,rec-park=y,video=y,sensor=n,GSM-radio=y,easy-parking=y,free-com=y";

        val builder = new RequirementBuilder();
        val userRequirement = builder.build(requirement);
        val sconf = builder.build(sconf_string);
        val configuration = builder.build(conf_string);

        // CHECK CONSISTENCY
        val factory = KBCausalExplanationModelFactory.getInstance(kb, sconf, userRequirement, configuration);
        val diagModel = (KBCausalExplanationModel)factory.createModel();

        System.out.println("\tNumber of constraints: " + diagModel.getAllConstraints().size());

        val checker = new ChocoConsistencyChecker(diagModel);

        val REQ = diagModel.getREQ();
        val CF = diagModel.getCF();
        val CONF = diagModel.getCONF();
        val NSCONF = diagModel.getNSCONF();

        val cxPlain = new CXPlain(checker);

        CAEvaluator.reset();
        val explanation = cxPlain.findExplanation(REQ, CF, CONF, NSCONF);

        System.out.println("=========================================");
        System.out.println("Explanation found by CXPlain:");
        System.out.println(explanation);
        printPerformance();

        assertEquals(3, explanation.size());
        String expectedExp = "[rec-park=y [copied], (video or sensor) <-> easy-parking, rec-park <-> video]";
        assertEquals(expectedExp, explanation.toString());
    }
}