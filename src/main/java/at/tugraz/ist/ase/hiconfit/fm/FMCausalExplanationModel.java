/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2022-2024
 *
 * @author: Viet-Man Le (v.m.le@tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.fm;

import at.tugraz.ist.ase.hiconfit.cacdr_core.Requirement;
import at.tugraz.ist.ase.hiconfit.cacdr_core.Solution;
import at.tugraz.ist.ase.hiconfit.cdrmodel.fm.FMRequirementCdrModel;
import at.tugraz.ist.ase.hiconfit.common.LoggerUtils;
import at.tugraz.ist.ase.hiconfit.fm.core.AbstractRelationship;
import at.tugraz.ist.ase.hiconfit.fm.core.CTConstraint;
import at.tugraz.ist.ase.hiconfit.fm.core.Feature;
import at.tugraz.ist.ase.hiconfit.fm.core.FeatureModel;
import at.tugraz.ist.ase.hiconfit.kb.core.Constraint;
import at.tugraz.ist.ase.hiconfit.negator.ISolutionNegatable;
import at.tugraz.ist.ase.hiconfit.negator.fm.FMSolutionNegator;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * An extension class of {@link FMRequirementCdrModel} for a causal explanation task of feature models, in which:
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
public class FMCausalExplanationModel<F extends Feature, R extends AbstractRelationship<F>, C extends CTConstraint>
        extends FMRequirementCdrModel<F, R, C> {

    // outputs
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
    protected ISolutionNegatable negator = new FMSolutionNegator();

    /**
     * A constructor
     * On the basic of a given {@link FeatureModel}, it creates
     * corresponding variables and constraints for the model.
     *
     * @param fm a {@link FeatureModel}
     */
    public FMCausalExplanationModel(@NonNull FeatureModel<F, R, C> fm,
                                    @NonNull Requirement SCONF, // v2
                                    @NonNull Requirement requirement,
                                    @NonNull Solution configuration // v2
    ) {
        super(fm, requirement, false, true, true, false);

        this.SCONF = SCONF;
        this.configuration = configuration;
    }

    /**
     * This function creates a Choco models, variables, constraints
     * for a corresponding feature models. Besides, test cases are
     * also translated to Choco constraints.
     */
    @Override
    public void initialize() {
        log.debug("{}Initializing FMModel for {} >>>", LoggerUtils.tab(), getName());
        LoggerUtils.indent();

        // prepare constraints for possibly faulty constraints
        // translates configuration to Choco constraints
        log.trace("{}Translating configuration to Choco constraints", LoggerUtils.tab());
        List<Constraint> constraints = solutionTranslator.translateToList(configuration, fmkb);
        List<Constraint> C = new LinkedList<>(constraints);
        CONF.addAll(constraints);

        // constraints from requirement
        log.trace("{}Translating requirement to Choco constraints", LoggerUtils.tab());
        constraints = solutionTranslator.translateToList(requirement, fmkb);

        List<Constraint> copiedConstraints = new LinkedList<>();
        constraints.forEach(c -> copiedConstraints.add(c.withConstraint(c.getConstraint() + " [copied]")));

        // add requirement to C
        C.addAll(copiedConstraints);
        REQ.addAll(copiedConstraints);

        // constraints from feature model
        log.trace("{}Adding constraints from feature model", LoggerUtils.tab());
        constraints = new LinkedList<>();
        constraints.add(fmkb.getRootConstraint());
        constraints.addAll(fmkb.getConstraintList());
        Collections.reverse(constraints);

        C.addAll(constraints);
        CF.addAll(constraints);

        this.setPossiblyFaultyConstraints(C);

        log.trace("{}Adding correct constraints", LoggerUtils.tab());
        // SCONF
        log.trace("{}Translating SCONF to Choco constraints", LoggerUtils.tab());
        Constraint constraint = negator.negate(SCONF, fmkb);
        this.setCorrectConstraints(List.of(constraint));
        NSCONF.add(constraint);

        // remove all Choco constraints
        model.unpost(model.getCstrs());

        LoggerUtils.outdent();
        log.debug("{}<<< Model {} initialized", LoggerUtils.tab(), getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object clone() throws CloneNotSupportedException {
        FMCausalExplanationModel<F, R, C> clone = (FMCausalExplanationModel<F, R, C>) super.clone();

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
        negator = null;
    }
}
