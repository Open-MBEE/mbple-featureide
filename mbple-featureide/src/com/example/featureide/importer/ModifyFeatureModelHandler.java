package com.example.featureide.importer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ModifyFeatureModelHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // Pass the desired importer (e.g., CSVImporter) to the FeatureModelModifier
        IFeatureModelImporter importer = new CSVImporter();
        FeatureModelModifier modifier = new FeatureModelModifier(importer);

        // Call the import method
        modifier.importFeaturesAndRefresh();
        return null;
    }
}
