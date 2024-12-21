/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2022-2024
 *
 * @author: Viet-Man Le (v.m.le@tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.negator;

import at.tugraz.ist.ase.hiconfit.cacdr_core.Solution;
import at.tugraz.ist.ase.hiconfit.kb.core.Constraint;
import at.tugraz.ist.ase.hiconfit.kb.core.KB;
import lombok.NonNull;

public interface ISolutionNegatable {
    Constraint negate(@NonNull Solution solution, @NonNull KB kb);
}
