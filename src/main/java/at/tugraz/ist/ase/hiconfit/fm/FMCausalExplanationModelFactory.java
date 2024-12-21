/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2023-2024
 *
 * @author: Viet-Man Le (vietman.le@ist.tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.fm;

import at.tugraz.ist.ase.hiconfit.cacdr_core.Requirement;
import at.tugraz.ist.ase.hiconfit.cacdr_core.Solution;
import at.tugraz.ist.ase.hiconfit.cdrmodel.AbstractCDRModel;
import at.tugraz.ist.ase.hiconfit.cdrmodel.fm.factory.FMRequirementCdrModelFactory;
import at.tugraz.ist.ase.hiconfit.fm.core.AbstractRelationship;
import at.tugraz.ist.ase.hiconfit.fm.core.CTConstraint;
import at.tugraz.ist.ase.hiconfit.fm.core.Feature;
import at.tugraz.ist.ase.hiconfit.fm.core.FeatureModel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Factory for creating a causal explanation model for feature models
 * + C = CONF + REQ + KB
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
public class FMCausalExplanationModelFactory extends FMRequirementCdrModelFactory {

    private Requirement SCONF;
    private Solution configuration;

    public FMCausalExplanationModelFactory(@NonNull FeatureModel<Feature, AbstractRelationship<Feature>, CTConstraint> featureModel,
                                           Requirement SCONF,
                                           Requirement requirement,
                                           Solution configuration) {
        super(featureModel, requirement, true);

        this.SCONF = SCONF;
        this.configuration = configuration;
    }

    public static FMCausalExplanationModelFactory getInstance(@NonNull FeatureModel<Feature, AbstractRelationship<Feature>, CTConstraint> featureModel,
                                                              Requirement SCONF,
                                                              Requirement requirement,
                                                              Solution configuration) {
        return new FMCausalExplanationModelFactory(featureModel, SCONF, requirement, configuration);
    }

    @Override
    public AbstractCDRModel createModel() {
        checkArgument(requirement != null, "Requirement cannot be null");
        checkArgument(SCONF != null, "SCONF cannot be null");
        checkArgument(configuration != null, "Configuration cannot be null");

        FMCausalExplanationModel<Feature, AbstractRelationship<Feature>, CTConstraint> cdrModel
                = new FMCausalExplanationModel<>(featureModel, SCONF, requirement, configuration);
        cdrModel.initialize();

        return cdrModel;
    }
}
