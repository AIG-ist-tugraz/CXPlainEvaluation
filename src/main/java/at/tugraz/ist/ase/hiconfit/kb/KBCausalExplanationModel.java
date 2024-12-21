/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2022-2024
 *
 * @author: Viet-Man Le (v.m.le@tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.kb;

import at.tugraz.ist.ase.hiconfit.cacdr_core.Requirement;
import at.tugraz.ist.ase.hiconfit.cacdr_core.Solution;
import at.tugraz.ist.ase.hiconfit.cdrmodel.kb.KBRequirementCdrModel;
import at.tugraz.ist.ase.hiconfit.common.LoggerUtils;
import at.tugraz.ist.ase.hiconfit.kb.core.Constraint;
import at.tugraz.ist.ase.hiconfit.kb.core.KB;
import at.tugraz.ist.ase.hiconfit.negator.ISolutionNegatable;
import at.tugraz.ist.ase.hiconfit.negator.kb.KBSolutionNegator;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * An extension class of {@link KBRequirementCdrModel} for causal explanation tasks of kbs, in which:
 * + C = CONF + REQ + CF
 * + B = NSCONF
 * Characteristics:
 * + hasNegativeConstraints = false
 * + rootConstraints = true - root constraint in C
 * + cfInConflicts = true
 * + reversedConstraintsOrder = false
 * Output model can be used for the following algorithms: CXPlain
 */
@Slf4j
public class KBCausalExplanationModel extends KBRequirementCdrModel {

    @Getter
    protected Set<Constraint> REQ = new LinkedHashSet<>();
    @Getter
    protected Set<Constraint> CF = new LinkedHashSet<>();
    @Getter
    protected Set<Constraint> CONF = new LinkedHashSet<>();
    @Getter
    protected Set<Constraint> NSCONF = new LinkedHashSet<>();

    protected Requirement SCONF;
    protected Solution configuration;

    @Setter
    protected ISolutionNegatable negator = new KBSolutionNegator();

    public KBCausalExplanationModel(@NonNull KB kb,
                                    Requirement SCONF,
                                    Requirement requirement,
                                    Solution configuration) {
        super(kb, requirement, true, false);

        this.SCONF = SCONF;
        this.configuration = configuration;
    }

    @Override
    public void initialize() {
        log.debug("{}Initializing KBCausalExplanationModel for {} >>>", LoggerUtils.tab(), getName());
        LoggerUtils.indent();

        // prepare constraints for possibly faulty constraints
        // translates configuration to Choco constraints
        log.trace("{}Translating configuration to Choco constraints", LoggerUtils.tab());
        List<Constraint> constraints = translator.translateToList(configuration, kb);
        List<Constraint> C = new LinkedList<>(constraints);
        CONF.addAll(constraints);

        // constraints from requirement
        log.trace("{}Translating requirement to Choco constraints", LoggerUtils.tab());
        constraints = translator.translateToList(requirement, kb);

        List<Constraint> copiedConstraints = new LinkedList<>();
        constraints.forEach(c -> copiedConstraints.add(c.withConstraint(c.getConstraint() + " [copied]")));

        // add requirement to C
        C.addAll(copiedConstraints);
        REQ.addAll(copiedConstraints);

        // constraints from kb
        log.trace("{}Adding constraints from kb", LoggerUtils.tab());
        constraints = new LinkedList<>(kb.getConstraintList());
        Collections.reverse(constraints);
        C.addAll(constraints);
        CF.addAll(constraints);

        this.setPossiblyFaultyConstraints(C);

        // sets correct constraints to super class
        log.trace("{}Adding correct constraints", LoggerUtils.tab());
        // SCONF
        log.trace("{}Translating SCONF to Choco constraints", LoggerUtils.tab());
        Constraint constraint = negator.negate(SCONF, kb);
        this.setCorrectConstraints(List.of(constraint));
        NSCONF.add(constraint);

        // remove all Choco constraints, cause we just need variables and test cases
        model.unpost(model.getCstrs());

        LoggerUtils.outdent();
        log.debug("{}<<< Model {} initialized", LoggerUtils.tab(), getName());
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        KBCausalExplanationModel clone = (KBCausalExplanationModel) super.clone();

        clone.configuration = (Solution) configuration.clone();
        clone.SCONF = (Requirement) SCONF.clone();

        return clone;
    }

    @Override
    public void dispose() {
        super.dispose();
        REQ.clear();
        CF.clear();
        CONF.clear();
        NSCONF.clear();
        SCONF = null;
        configuration = null;
    }
}

