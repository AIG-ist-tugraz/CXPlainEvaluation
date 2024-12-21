/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2024
 *
 * @author: Viet-Man Le (vietman.le@ist.tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.kb;

import at.tugraz.ist.ase.hiconfit.cacdr_core.Requirement;
import at.tugraz.ist.ase.hiconfit.cacdr_core.Solution;
import at.tugraz.ist.ase.hiconfit.cdrmodel.AbstractCDRModel;
import at.tugraz.ist.ase.hiconfit.cdrmodel.kb.factory.KBRequirementCdrModelFactory;
import at.tugraz.ist.ase.hiconfit.kb.core.KB;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Factory for creating a causal explanation model, in which:
 * + C = CONF + REQ + CF
 * + B = NSCONF
 * Characteristics:
 * + hasNegativeConstraints = false
 * + rootConstraints = true - root constraint in C
 * + cfInConflicts = true
 * + reversedConstraintsOrder = false
 * Output model can be used for the following algorithms: CXPlain
 */
@Getter
@Setter
public class KBCausalExplanationModelFactory extends KBRequirementCdrModelFactory {

    private Requirement SCONF;
    private Solution configuration;

    public KBCausalExplanationModelFactory(@NonNull KB kb,
                                           Requirement SCONF,
                                           Requirement requirement,
                                           Solution configuration) {
        super(kb, requirement, true, false);

        this.SCONF = SCONF;
        this.configuration = configuration;
    }

    public static KBCausalExplanationModelFactory getInstance(@NonNull KB kb,
                                                              Requirement SCONF,
                                                              Requirement requirement,
                                                              Solution configuration) {
        return new KBCausalExplanationModelFactory(kb, SCONF, requirement, configuration);
    }

    @Override
    public AbstractCDRModel createModel() {
        checkArgument(SCONF != null, "SCONF cannot be null");
        checkArgument(requirement != null, "Requirement cannot be null");
        checkArgument(configuration != null, "Configuration cannot be null");

        KBCausalExplanationModel diagModel
            = new KBCausalExplanationModel(kb, SCONF, requirement, configuration);
        diagModel.initialize();

        return diagModel;
    }
}
