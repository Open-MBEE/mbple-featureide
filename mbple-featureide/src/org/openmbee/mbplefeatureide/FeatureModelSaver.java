package org.openmbee.mbplefeatureide;

import java.nio.file.Path;
import java.nio.file.Paths;

import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.FMFormatManager;
import de.ovgu.featureide.fm.core.io.IPersistentFormat;

public class FeatureModelSaver {
    public static void saveFeatureModel(IFeatureModel featureModel, String filePath) {
        try {
            // Define the output path
            Path path = Paths.get(filePath);

            // Get the default format for saving
            IPersistentFormat<IFeatureModel> format = FMFormatManager.getDefaultFormat();

            // Save the feature model
            boolean success = FeatureModelManager.save(featureModel, path, format);

            if (success) {
                System.out.println("Feature model saved successfully to: " + filePath);
            } else {
                System.out.println("Failed to save the feature model.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
