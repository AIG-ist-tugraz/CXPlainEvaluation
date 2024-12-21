/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2022-2024
 *
 * @author: Viet-Man Le (vietman.le@ist.tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.negator.kb;

import at.tugraz.ist.ase.hiconfit.cacdr_core.Assignment;
import at.tugraz.ist.ase.hiconfit.common.ChocoSolverUtils;
import at.tugraz.ist.ase.hiconfit.kb.core.IIntVarKB;
import at.tugraz.ist.ase.hiconfit.kb.core.KB;
import at.tugraz.ist.ase.hiconfit.negator.IAssignmentsNegatable;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

import java.util.List;

/**
 * DON'T remove the translated constraints from the Choco model
 * TODO: Migrate to hiconfit-core
 */
public class KBAssignmentsNegator implements IAssignmentsNegatable {

    /**
     * Translates {@link Assignment}s to Choco constraints.
     * @param assignments the {@link Assignment}s to translate
     * @param kb the {@link KB}
     * @param chocoCstrs list of Choco constraints, to which the translated constraints are added
     * @param negChocoCstrs list of Choco constraints, to which the translated negative constraints are added
     */
    @Override
    public void negate(@NonNull List<Assignment> assignments, @NonNull KB kb, @NonNull List<Constraint> chocoCstrs, List<Constraint> negChocoCstrs) {
        // check if the KB is a IIntVarKB
        Preconditions.checkArgument(kb instanceof IIntVarKB, "The KB must be a IIntVarKB");

        int startIdx = kb.getNumChocoConstraints();
        createNegateAndPost(assignments, kb);

        afterPostingAndUnpost(chocoCstrs, kb, startIdx);

        // TODO - negation
        // Negation of the translated constraints
//        if (negChocoCstrs != null) {
//            translateToNegation(logOp, model, negChocoCstrs);
//        }
    }

    private static void afterPostingAndUnpost(List<Constraint> chocoCstrs, KB kb, int startIdx) {
        Model model = kb.getModelKB();

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
    public void negate(@NonNull Assignment assignment, @NonNull KB kb, @NonNull List<Constraint> chocoCstrs, List<Constraint> negChocoCstrs) {
        // check if the KB is a IIntVarKB
        Preconditions.checkArgument(kb instanceof IIntVarKB, "The KB must be a IIntVarKB");

        int startIdx = kb.getNumChocoConstraints();
        createNegateAndPost(assignment, kb); // add the translated constraints to the Choco model

        afterPostingAndUnpost(chocoCstrs, kb, startIdx);

        // TODO - negation
        // Negation of the translated constraints
//        if (negChocoCstrs != null) {
//            translateToNegation(logOp, model, negChocoCstrs);
//        }
    }

    private boolean isCorrectAssignment(String varName, IntVar var, String value, int chocoValue) {
        return var != null && (!value.equals("NULL"))
                && (chocoValue != -1);
    }

    private void createNegateAndPost(@NonNull List<Assignment> assignments, @NonNull KB kb) {
        for (Assignment assign: assignments) {
            createNegateAndPost(assign, kb);
        }
    }

    private void createNegateAndPost(@NonNull Assignment assignment, @NonNull KB kb) {
        String varName = assignment.getVariable();
        IntVar var = ((IIntVarKB)kb).getIntVar(varName);
        String value = assignment.getValue();
        int chocoValue = ((IIntVarKB)kb).getIntValue(varName, value);

        if (isCorrectAssignment(varName, var, value, chocoValue)) {
            // negate
            chocoValue = (chocoValue == 0) ? 1 : 0;

            kb.getModelKB().arithm(var, "=", chocoValue).post();
        }
    }

//    private void translateToNegation(LogOp logOp, Model model, List<Constraint> negChocoCstrs) {
//        LogOp negLogOp = createNegation(logOp);
//        int startIdx = model.getNbCstrs();
//        model.addClauses(negLogOp);
//
//        negChocoCstrs.addAll(ChocoSolverUtils.getConstraints(model, startIdx, model.getNbCstrs() - 1));
//    }
//
//    public LogOp createNegation(@NonNull LogOp logOp) {
//        return LogOp.nand(logOp);
//    }
}
