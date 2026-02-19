package com.example.featureide.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.impl.Feature;
import de.ovgu.featureide.fm.core.base.impl.FeatureStructure;

public class CSVImporter implements IFeatureModelImporter {

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

        Map<String, IFeatureStructure> featureMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // Read header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    boolean isRoot = Boolean.parseBoolean(parts[2].trim());
                    String parentId = parts[3].trim();

                    // Create a new feature and its structure
                    IFeature feature = new Feature(featureModel, name);
                    IFeatureStructure featureStructure = new FeatureStructure(feature);
                    featureMap.put(id, featureStructure);

                    // Attach to parent if available, otherwise set as root
                    if (isRoot || parentId.isEmpty()) {
                        featureModel.getStructure().setRoot(featureStructure);
                        System.out.println("[DEBUG] Set root feature: " + name);
                    } else if (featureMap.containsKey(parentId)) {
                        featureMap.get(parentId).addChild(featureStructure);
                        System.out.println("[DEBUG] Added feature '" + name + "' as a child of '" + featureMap.get(parentId).getFeature().getName() + "'.");
                    } else {
                        System.out.println("[WARNING] Parent ID " + parentId + " not found for feature: " + name);
                    }
                }
            }
        }

        // Verify the structure
        System.out.println("[DEBUG] Final Feature Model Structure:");
        featureModel.getStructure().getFeaturesPreorder().forEach(feature ->
            System.out.println("  - " + feature.getName())
        );
    }
}
