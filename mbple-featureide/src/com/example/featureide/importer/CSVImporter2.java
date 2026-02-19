package com.example.featureide.importer;

import java.io.BufferedReader;
import java.io.FileReader;

import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.impl.Feature;
import de.ovgu.featureide.fm.core.base.impl.FeatureStructure;

public class CSVImporter2 implements IFeatureModelImporter {

    @Override
    public void importFeatures(IFeatureModel featureModel) throws Exception {
        // Open file dialog
        Shell shell = new Shell();
        FileDialog fileDialog = new FileDialog(shell);
        fileDialog.setFilterExtensions(new String[]{"*.csv"});
        String filePath = fileDialog.open();

        if (filePath == null) {
            System.out.println("Import cancelled: No file selected.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 0) {
                    String featureName = parts[0].trim();
                    boolean isMandatory = parts.length > 1 && "mandatory".equalsIgnoreCase(parts[1].trim());

                    if (featureModel.getFeature(featureName) == null) {
                        IFeature newFeature = new Feature(featureModel, featureName);
                        FeatureStructure newFeatureStructure = new FeatureStructure(newFeature);
                        featureModel.getStructure().getRoot().addChild(newFeatureStructure);

                        if (isMandatory) {
                            newFeatureStructure.setMandatory(true);
                        }

                        System.out.println("Added feature: " + featureName + ", mandatory: " + isMandatory);
                    } else {
                        System.out.println("Feature already exists: " + featureName);
                    }
                }
            }
        }
    }
}
