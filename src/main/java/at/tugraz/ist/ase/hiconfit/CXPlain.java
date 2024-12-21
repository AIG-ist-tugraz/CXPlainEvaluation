/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2021-2024
 *
 * @author: Viet-Man Le (v.m.le@tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit;

import at.tugraz.ist.ase.hiconfit.cacdr.algorithms.IConsistencyAlgorithm;
import at.tugraz.ist.ase.hiconfit.cacdr.checker.ChocoConsistencyChecker;
import at.tugraz.ist.ase.hiconfit.common.ConstraintUtils;
import at.tugraz.ist.ase.hiconfit.common.LoggerUtils;
import at.tugraz.ist.ase.hiconfit.kb.core.Constraint;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static at.tugraz.ist.ase.hiconfit.cacdr.eval.CAEvaluator.*;

/**
 * Implementation of CXPlain algorithm
 * //CXPlain Algorithm
 * //--------------------
 * //CXPlain(REQ, KB, CONF, NSCONF): EXP
 * //IF consistent(CONF ∪ KB ∪ REQ)
 * //  return CXP(Φ, CONF ∪ REQ ∪ KB, NSCONF);
 * //ELSE
 * //  print 'no explanation possible'
 * //  return Φ;
 * <p>
 * //func CXP(Δ, C={c1,c2, …, cq}, B): EXP
 * //IF (Δ != Φ AND inconsistent(B)) return Φ;
 * //IF singleton(C) return C;
 * //k=n/2;
 * //C1 <-- {c1, …, ck}; C2 <-- {ck+1, …, cq};
 * //CS1 <-- CXP(C2, C1, B ∪ C2);
 * //CS2 <-- CXP(CS1, C2, B ∪ CS1);
 * //return (CS1 ∪ CS2)
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
@Slf4j
public class CXPlain extends IConsistencyAlgorithm {

    // for evaluation
    public static final String TIMER_CXPLAIN = "Timer for CXPlain";
    public static final String COUNTER_CXPLAIN_CALLS = "The number of CXP calls";

    public CXPlain(@NonNull ChocoConsistencyChecker checker) {
        super(checker);
    }

    /**
     * //CXPlain(REQ, KB, CONF, NSCONF): EXP
     * //IF consistent(CONF ∪ KB ∪ REQ)
     * // return CXP(Φ, CONF ∪ REQ ∪ KB, NSCONF);
     * //ELSE
     * // print 'no explanation possible'
     * // return Φ;
     *
     * @param REQ user requirement
     * @param KB a knowledge base
     * @param CONF a configuration
     * @param NSCONF negative of subset of configuration
     * @return an explanation or an empty set
     */
    public Set<Constraint> findExplanation(@NonNull Set<Constraint> REQ,
                                           @NonNull Set<Constraint> KB,
                                           @NonNull Set<Constraint> CONF,
                                           @NonNull Set<Constraint> NSCONF) {
        log.debug("{}Identifying explanation for [REQ={}, KB={}, CONF={}, NSCONF={}] >>>", LoggerUtils.tab(), REQ, KB, CONF, NSCONF);
        LoggerUtils.indent();

        Set<Constraint> CONFwithREQ = Sets.union(CONF, REQ); incrementCounter(COUNTER_UNION_OPERATOR);
        Set<Constraint> CONFwithREQwithKB = Sets.union(CONFwithREQ, KB); incrementCounter(COUNTER_UNION_OPERATOR);

        //IF consistent(CONF ∪ KB ∪ REQ)
        if (checker.isConsistent(CONFwithREQwithKB)) {
            // return CXP(Φ, CONF ∪ REQ ∪ KB, NSCONF);
            incrementCounter(COUNTER_CXPLAIN_CALLS);

            start(TIMER_CXPLAIN);
            Set<Constraint> exp = cxp(Collections.emptySet(), CONFwithREQwithKB, NSCONF);
            stop(TIMER_CXPLAIN);

            LoggerUtils.outdent();
            log.debug("{}<<< Found explanation [exp={}]", LoggerUtils.tab(), exp);

            return exp;
        } else { //ELSE print 'no explanation possible' return Φ;
            LoggerUtils.outdent();
            log.debug("{}<<< No explanation possible", LoggerUtils.tab());

            return Collections.emptySet();
        }
    }

    /**
     * //func CXP(Δ, C={c1,c2, …, cq}, B): EXP
     * //IF (Δ != Φ AND inconsistent(B)) return Φ;
     * //IF singleton(C) return C;
     * //k=n/2;
     * //C1 <-- {c1, …, ck}; C2 <-- {ck+1, …, cq};
     * //CS1 <-- CXP(C2, C1, B ∪ C2);
     * //CS2 <-- CXP(CS1, C2, B ∪ CS1);
     * //return (CS1 ∪ CS2)
     *
     * @param D check to skip redundant consistency checks
     * @param C a consideration set of constraints
     * @param B background knowledge
     * @return an explanation or an empty set
     */
    private Set<Constraint> cxp(Set<Constraint> D, Set<Constraint> C, Set<Constraint> B) {
        log.debug("{}CXP [D={}, C={}, B={}] >>>", LoggerUtils.tab(), D, C, B);
        LoggerUtils.indent();

        //IF (Δ != Φ AND inconsistent(B)) return Φ;
        if ( !D.isEmpty() ) {
            incrementCounter(COUNTER_CONSISTENCY_CHECKS);
            if (!checker.isConsistent(B)) {
                LoggerUtils.outdent();
                log.debug("{}<<< return Φ", LoggerUtils.tab());

                return Collections.emptySet();
            }
        }

        // if singleton(C) return C;
        int q = C.size();
        if (q == 1) {
            LoggerUtils.outdent();
            log.debug("{}<<< return [{}]", LoggerUtils.tab(), C);

            return C;
        }

        // C1 = {c1..ck}; C2 = {ck+1..cq};
        Set<Constraint> C1 = new LinkedHashSet<>();
        Set<Constraint> C2 = new LinkedHashSet<>();
        ConstraintUtils.split(C, C1, C2);
        log.trace("{}Split C into [C1={}, C2={}]", LoggerUtils.tab(), C1, C2);

        // CS1 <-- QX(C2, C1, B ∪ C2);
        Set<Constraint> BwithC2 = Sets.union(B, C2); incrementCounter(COUNTER_UNION_OPERATOR);
        incrementCounter(COUNTER_LEFT_BRANCH_CALLS);
        incrementCounter(COUNTER_CXPLAIN_CALLS);
        Set<Constraint> CS1 = cxp(C2, C1, BwithC2);

        // CS2 <-- QX(CS1, C2, B ∪ CS1);
        Set<Constraint> BwithCS1 = Sets.union(B, CS1); incrementCounter(COUNTER_UNION_OPERATOR);
        incrementCounter(COUNTER_RIGHT_BRANCH_CALLS);
        incrementCounter(COUNTER_CXPLAIN_CALLS);
        Set<Constraint> CS2 = cxp(CS1, C2, BwithCS1);

        LoggerUtils.outdent();
        log.debug("{}<<< return [CS1={} ∪ CS2={}]", LoggerUtils.tab(), CS1, CS2);

        //return (CS1 ∪ CS2)
        incrementCounter(COUNTER_UNION_OPERATOR);
        return Sets.union(CS1, CS2);
    }
}
