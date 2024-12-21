/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2024
 *
 * @author: Viet-Man Le (v.m.le@tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.negator.kb;

import at.tugraz.ist.ase.hiconfit.cacdr_core.Assignment;
import at.tugraz.ist.ase.hiconfit.cacdr_core.Solution;
import at.tugraz.ist.ase.hiconfit.common.LoggerUtils;
import at.tugraz.ist.ase.hiconfit.kb.core.Constraint;
import at.tugraz.ist.ase.hiconfit.kb.core.IIntVarKB;
import at.tugraz.ist.ase.hiconfit.kb.core.KB;
import at.tugraz.ist.ase.hiconfit.negator.ISolutionNegatable;
import com.google.common.base.Joiner;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class KBSolutionNegator implements ISolutionNegatable {

    protected KBAssignmentsNegator negator = new KBAssignmentsNegator();

    @Override
    public Constraint negate(@NonNull Solution solution, @NonNull KB kb) {
        // check if the KB is a IIntVarKB
        checkArgument(kb instanceof IIntVarKB, "The KB must be a IIntVarKB");

        log.trace("{}Translating solution [solution={}] >>>", LoggerUtils.tab(), solution);
        // gets List<String> variables from solution
        List<String> variables = solution.getAssignments().stream()
                .map(Assignment::getVariable)
                .toList();

        String constraint_str = Joiner.on(" and ").join(solution.getAssignments());
        constraint_str = "not(" + constraint_str + ")";
        Constraint constraint = new Constraint(constraint_str, variables);

        negator.negate(solution.getAssignments(), kb,
                constraint.getChocoConstraints(), constraint.getNegChocoConstraints());

        // copy the generated constraints to Solution
        constraint.getChocoConstraints().forEach(solution::addChocoConstraint);
        constraint.getNegChocoConstraints().forEach(solution::addNegChocoConstraint);

        // remove the translated constraints from the Choco model
        // TODO - should move out to the configurator class
//        kb.getModelKB().unpost(kb.getModelKB().getCstrs());

        log.debug("{}Translated solution [solution={}] >>>", LoggerUtils.tab(), solution);
        return constraint;
    }
}
