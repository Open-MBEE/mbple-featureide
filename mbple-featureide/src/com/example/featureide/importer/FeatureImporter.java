package com.example.featureide.importer;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.IFeatureModelStructure;
import de.ovgu.featureide.fm.core.base.impl.Feature;
import de.ovgu.featureide.fm.core.base.impl.FeatureModel;
import de.ovgu.featureide.fm.core.base.impl.FeatureStructure;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FeatureImporter {
    private final Shell shell;

    public FeatureImporter(Shell shell) {
        this.shell = shell;
    }

    public void importFeatures(FeatureModel featureModel) {
    	
    	System.out.println("Feature model type: " + featureModel.getClass().getName());

        // Open a file dialog to select the CSV file
        FileDialog fileDialog = new FileDialog(shell);
        fileDialog.setFilterExtensions(new String[]{"*.csv"});
        String filePath = fileDialog.open();

        if (filePath == null) {
            MessageDialog.openInformation(shell, "Import Cancelled", "No file selected.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            // Retrieve the root feature structure from the feature model
            IFeatureModelStructure structure = featureModel.getStructure();
            IFeatureStructure rootStructure = structure.getRoot();
            System.out.println("Root structure: " + rootStructure);


            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 0) {
                    String featureName = parts[0].trim();
                    boolean isMandatory = parts.length > 1 && "mandatory".equalsIgnoreCase(parts[1].trim());

                    // Check if the feature already exists
                    IFeature existingFeature = featureModel.getFeature(featureName);
                    if (existingFeature == null) {
                        // Create a new IFeature instance
                        IFeature newFeature = new Feature(featureModel, featureName);

                        // Create a FeatureStructure for the feature
                        IFeatureStructure newFeatureStructure = new FeatureStructure(newFeature);

                        // Add the new feature structure as a child of the root structure
                        System.out.println("Children before addition: " + rootStructure.getChildren());
                        rootStructure.addChild(newFeatureStructure);
                        System.out.println("Children after addition: " + rootStructure.getChildren());


                        // Set mandatory if needed
                        if (isMandatory) {
                            newFeatureStructure.setMandatory(true);
                        }
                    }
                }
            }

            // Save the feature model
            FeatureModelSaver.saveFeatureModel(featureModel, "C:\\Data\\MBPLE\\MBPLE-eclipse\\MBPLEDroneProductLine\\feature-model.xml");
            System.out.println("Feature model saved.");
            
            // Notify the user about the success
            MessageDialog.openInformation(shell, "Import Successful", "Features imported successfully!");
        } catch (IOException e) {
            MessageDialog.openError(shell, "Import Failed", "An error occurred while reading the file:\n" + e.getMessage());
        }
    }
}
