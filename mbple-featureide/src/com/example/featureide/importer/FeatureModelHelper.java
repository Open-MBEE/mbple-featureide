package com.example.featureide.importer;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

public class FeatureModelHelper {
    public static void updateFeatureModel(FeatureModelManager fmManager, IFeatureModel modifiedModel) {
        try {
            java.lang.reflect.Method method = FeatureModelManager.class.getDeclaredMethod("setVariableObject", IFeatureModel.class);
            method.setAccessible(true);
            method.invoke(fmManager, modifiedModel);
            fmManager.overwrite();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
