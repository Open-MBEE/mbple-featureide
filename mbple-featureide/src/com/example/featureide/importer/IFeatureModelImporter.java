package com.example.featureide.importer;

import de.ovgu.featureide.fm.core.base.IFeatureModel;

public interface IFeatureModelImporter {
    void importFeatures(IFeatureModel featureModel) throws Exception;
}
