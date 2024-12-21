/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2022-2024
 *
 * @author: Viet-Man Le (v.m.le@tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.negator.fm;

import at.tugraz.ist.ase.hiconfit.cacdr_core.Assignment;
import at.tugraz.ist.ase.hiconfit.cacdr_core.CONNECTION_TYPE;
import at.tugraz.ist.ase.hiconfit.cacdr_core.ILogOpCreatable;
import at.tugraz.ist.ase.hiconfit.cacdr_core.LogOpCreator;
import at.tugraz.ist.ase.hiconfit.common.ChocoSolverUtils;
import at.tugraz.ist.ase.hiconfit.kb.core.KB;
import at.tugraz.ist.ase.hiconfit.negator.IAssignmentsNegatable;
import lombok.NonNull;
import lombok.Setter;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.nary.cnf.LogOp;

import java.util.LinkedList;
import java.util.List;

/**
 * No remove the translated constraints from the Choco model
 */
public class FMAssignmentsNegator implements IAssignmentsNegatable {

    @Setter
    private static ILogOpCreatable logOpCreator = new LogOpCreator();
    private static final CONNECTION_TYPE OR_CONNECTION_TYPE = CONNECTION_TYPE.OR;

    /**
     * Translates {@link Assignment}s to Choco constraints.
     * @param assignments the {@link Assignment}s to translate
     * @param kb the {@link KB}
     * @param chocoCstrs list of Choco constraints, to which the translated constraints are added
     * @param negChocoCstrs list of Choco constraints, to which the translated negative constraints are added
     */
    @Override
    public void negate(@NonNull List<Assignment> assignments, @NonNull KB kb,
                       @NonNull List<Constraint> chocoCstrs, List<Constraint> negChocoCstrs) {
        int startIdx = kb.getNumChocoConstraints();
        Model model = kb.getModelKB();

        // negates the assignments
        List<Assignment> negAssignments = new LinkedList<>();
        for (Assignment assignment : assignments) {
            String neg_value = assignment.getValue().equals("true") ? "false" : "true";
            negAssignments.add(new Assignment(assignment.getVariable(), neg_value));
        }

        LogOp logOp = logOpCreator.create(negAssignments, kb, OR_CONNECTION_TYPE);
        post(logOp, model, chocoCstrs, startIdx);

        // Negation of the translated constraints
        if (negChocoCstrs != null) {
            translateToNegation(logOp, model, negChocoCstrs);
        }
    }

    private static void post(LogOp logOp, Model model, List<Constraint> chocoCstrs, int startIdx) {
        model.addClauses(logOp); // add the translated constraints to the Choco kb

        int endIdx = model.getNbCstrs() - 1;
        if (startIdx <= endIdx) {
            List<Constraint> postedCstrs = ChocoSolverUtils.getConstraints(model, startIdx, endIdx);

            chocoCstrs.addAll(postedCstrs);

            // remove the posted constraints from the Choco model
            postedCstrs.forEach(model::unpost);
        }
    }

    /**
     * Translates {@link Assignment}s to Choco constraints.
     * @param assignment the {@link Assignment} to translate
     * @param kb the {@link KB}
     * @param chocoCstrs list of Choco constraints, to which the translated constraints are added
     * @param negChocoCstrs list of Choco constraints, to which the translated negative constraints are added
     */
    @Override
    public void negate(@NonNull Assignment assignment, @NonNull KB kb,
                       @NonNull List<Constraint> chocoCstrs, List<Constraint> negChocoCstrs) {
        int startIdx = kb.getNumChocoConstraints();
        Model model = kb.getModelKB();

        String neg_value = assignment.getValue().equals("true") ? "false" : "true";
        Assignment negAssignment = new Assignment(assignment.getVariable(), neg_value);

        LogOp logOp = logOpCreator.create(negAssignment, kb, OR_CONNECTION_TYPE);
        post(logOp, model, chocoCstrs, startIdx);

        // Negation of the translated constraints
        if (negChocoCstrs != null) {
            translateToNegation(logOp, model, negChocoCstrs);
        }
    }

    private void translateToNegation(LogOp logOp, Model model, List<Constraint> negChocoCstrs) {
        int startIdx = model.getNbCstrs();
        LogOp negLogOp = logOpCreator.createNegation(logOp, OR_CONNECTION_TYPE);
        post(negLogOp, model, negChocoCstrs, startIdx);
    }
}
