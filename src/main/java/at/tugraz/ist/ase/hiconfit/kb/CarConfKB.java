/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2022-2024
 *
 * @author: Viet-Man Le (v.m.le@tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.kb;

import at.tugraz.ist.ase.hiconfit.common.LoggerUtils;
import at.tugraz.ist.ase.hiconfit.kb.core.*;
import at.tugraz.ist.ase.hiconfit.kb.core.builder.IntVarConstraintBuilder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Car Configuration Knowledge Base
 * Gerhard Friedrich, "Elimination of spurious explanations"
 */
@Slf4j
public class CarConfKB extends KB implements IIntVarKB {
    public CarConfKB(boolean hasNegativeConstraints) {
        super("Car Configuration Problem", "", hasNegativeConstraints);

        reset(hasNegativeConstraints);
    }

    @Override
    public void reset(boolean hasNegativeConstraints) {
        log.trace("{}Creating CarConfKB >>>", LoggerUtils.tab());
        LoggerUtils.indent();

        modelKB = new Model(name);
        variableList = new LinkedList<>();
        domainList = new LinkedList<>();
        constraintList = new LinkedList<>();
        defineVariables();
        defineConstraints(hasNegativeConstraints);

        LoggerUtils.outdent();
        log.debug("{}<<< Created CarConfKB", LoggerUtils.tab());
    }

    public void defineVariables (){
        log.trace("{}Defining variables >>", LoggerUtils.tab());
        LoggerUtils.indent();

        List<String> varNames = List.of("biz-park", "rec-park", "video", "sensor", "GSM-radio", "easy-parking", "free-com");

        IntStream.range(0, varNames.size()).forEachOrdered(i -> {
            String varName = varNames.get(i);
            Domain domain = Domain.builder()
                    .name(varName)
                    .values(List.of("n", "y"))
                    .build();
            domainList.add(domain);

            IntVar intVar = this.modelKB.intVar(varName, domainList.get(i).getIntValues());
            Variable var = IntVariable.builder()
                    .name(varName)
                    .domain(domainList.get(i))
                    .chocoVar(intVar).build();
            variableList.add(var);
        });

        LoggerUtils.outdent();
        log.trace("{}<<< Created variables", LoggerUtils.tab());
    }

    public void defineConstraints(boolean hasNegativeConstraints) {
        log.trace("{}Defining constraints >>>", LoggerUtils.tab());
        LoggerUtils.indent();

        // rec-park <-> video
        // <=> (rec-park = "y" and video = "y") or (rec-park = "n" and video = "n")
        int startIdx = modelKB.getNbCstrs();
        org.chocosolver.solver.constraints.Constraint chocoConstraint = modelKB.or(
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(1)).getChocoVar(), "=", 1) ,
                        modelKB.arithm(((IntVariable)variableList.get(2)).getChocoVar(), "=", 1)) ,
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(1)).getChocoVar(), "=", 0) ,
                        modelKB.arithm(((IntVariable)variableList.get(2)).getChocoVar(), "=", 0)));
        Constraint constraint = IntVarConstraintBuilder.build("rec-park <-> video", List.of(variableList.get(1).getName(), variableList.get(2).getName()), modelKB, chocoConstraint, startIdx, hasNegativeConstraints);
        constraintList.add(constraint);

        // (biz-park /\ !rec-park -> sensor) /\ !(rec-park /\ sensor)
        // <=> (biz-park = "y" and rec-park = "y" and sensor = "n") or
        // <=> (biz-park = "y" and rec-park = "n" and sensor = "y") or
        // <=> (biz-park = "n" and rec-park = "y" and sensor = "n") or
        // <=> (biz-park = "n" and rec-park = "n" and sensor = "n") or
        // <=> (biz-park = "n" and rec-park = "n" and sensor = "y") or
        startIdx = modelKB.getNbCstrs();
        chocoConstraint = modelKB.or(
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(0)).getChocoVar(), "=", 1) ,
                        modelKB.arithm(((IntVariable)variableList.get(1)).getChocoVar(), "=", 1) ,
                        modelKB.arithm(((IntVariable)variableList.get(3)).getChocoVar(), "=", 0)) ,
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(0)).getChocoVar(), "=", 1) ,
                        modelKB.arithm(((IntVariable)variableList.get(1)).getChocoVar(), "=", 0) ,
                        modelKB.arithm(((IntVariable)variableList.get(3)).getChocoVar(), "=", 1)) ,
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(0)).getChocoVar(), "=", 0) ,
                        modelKB.arithm(((IntVariable)variableList.get(1)).getChocoVar(), "=", 1) ,
                        modelKB.arithm(((IntVariable)variableList.get(3)).getChocoVar(), "=", 0)) ,
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(0)).getChocoVar(), "=", 0) ,
                        modelKB.arithm(((IntVariable)variableList.get(1)).getChocoVar(), "=", 0) ,
                        modelKB.arithm(((IntVariable)variableList.get(3)).getChocoVar(), "=", 0)) ,
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(0)).getChocoVar(), "=", 0) ,
                        modelKB.arithm(((IntVariable)variableList.get(1)).getChocoVar(), "=", 0) ,
                        modelKB.arithm(((IntVariable)variableList.get(3)).getChocoVar(), "=", 1)));
        constraint = IntVarConstraintBuilder.build("(biz-park /\\ !rec-park -> sensor) /\\ !(rec-park /\\ sensor)", List.of(variableList.get(0).getName(), variableList.get(1).getName(), variableList.get(3).getName()), modelKB, chocoConstraint, startIdx, hasNegativeConstraints);
        constraintList.add(constraint);

        // (video or sensor) <-> easy-parking
        // <=> (video = "n" and sensor = "n" and easy-parking = "n") or
        // <=> (video = "y" and sensor = "n" and easy-parking = "y") or
        // <=> (video = "n" and sensor = "y" and easy-parking = "y") or
        // <=> (video = "y" and sensor = "y" and easy-parking = "y")
        startIdx = modelKB.getNbCstrs();
        chocoConstraint = modelKB.or(
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(2)).getChocoVar(), "=", 0) ,
                        modelKB.arithm(((IntVariable)variableList.get(3)).getChocoVar(), "=", 0) ,
                        modelKB.arithm(((IntVariable)variableList.get(5)).getChocoVar(), "=", 0)) ,
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(2)).getChocoVar(), "=", 1) ,
                        modelKB.arithm(((IntVariable)variableList.get(3)).getChocoVar(), "=", 0) ,
                        modelKB.arithm(((IntVariable)variableList.get(5)).getChocoVar(), "=", 1)) ,
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(2)).getChocoVar(), "=", 0) ,
                        modelKB.arithm(((IntVariable)variableList.get(3)).getChocoVar(), "=", 1) ,
                        modelKB.arithm(((IntVariable)variableList.get(5)).getChocoVar(), "=", 1)) ,
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(2)).getChocoVar(), "=", 1) ,
                        modelKB.arithm(((IntVariable)variableList.get(3)).getChocoVar(), "=", 1) ,
                        modelKB.arithm(((IntVariable)variableList.get(5)).getChocoVar(), "=", 1)));
        constraint = IntVarConstraintBuilder.build("(video or sensor) <-> easy-parking", List.of(variableList.get(2).getName(), variableList.get(3).getName(), variableList.get(5).getName()), modelKB, chocoConstraint, startIdx, hasNegativeConstraints);
        constraintList.add(constraint);

        //  biz-park <-> GSM-radio
        // <=> (biz-park = "y" and GSM-radio = "y") or
        // <=> (biz-park = "n" and GSM-radio = "n")
        startIdx = modelKB.getNbCstrs();
        chocoConstraint = modelKB.or(
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(0)).getChocoVar(), "=", 1) ,
                        modelKB.arithm(((IntVariable)variableList.get(4)).getChocoVar(), "=", 1)) ,
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(0)).getChocoVar(), "=", 0) ,
                        modelKB.arithm(((IntVariable)variableList.get(4)).getChocoVar(), "=", 0)));
        constraint = IntVarConstraintBuilder.build("biz-park <-> GSM-radio", List.of(variableList.get(0).getName(), variableList.get(4).getName()), modelKB, chocoConstraint, startIdx, hasNegativeConstraints);
        constraintList.add(constraint);

        // GSM-radio <-> free-com
        // <=> (GSM-radio = "y" and free-com = "y") or
        // <=> (GSM-radio = "n" and free-com = "n")
        startIdx = modelKB.getNbCstrs();
        chocoConstraint = modelKB.or(
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(4)).getChocoVar(), "=", 1) ,
                        modelKB.arithm(((IntVariable)variableList.get(6)).getChocoVar(), "=", 1)) ,
                modelKB.and(modelKB.arithm(((IntVariable)variableList.get(4)).getChocoVar(), "=", 0) ,
                        modelKB.arithm(((IntVariable)variableList.get(6)).getChocoVar(), "=", 0)));
        constraint = IntVarConstraintBuilder.build("GSM-radio <-> free-com", List.of(variableList.get(4).getName(), variableList.get(6).getName()), modelKB, chocoConstraint, startIdx, hasNegativeConstraints);
        constraintList.add(constraint);

        LoggerUtils.outdent();
        log.trace("{}<<< Created constraints", LoggerUtils.tab());
    }

    @Override
    public IntVar[] getIntVars() {
        org.chocosolver.solver.variables.Variable[] vars = getModelKB().getVars();

        return Arrays.stream(vars).map(v -> (IntVar) v).toArray(IntVar[]::new);
    }

    @Override
    public IntVar getIntVar(@NonNull String variable) {
        Variable var = getVariable(variable);

        return ((IntVariable) var).getChocoVar();
    }

    // Choco value
    @Override
    public int getIntValue(@NonNull String var, @NonNull String value) {
        Domain domain = getDomain(var);

        return domain.getChocoValue(value);
    }
}
