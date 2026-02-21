package org.openmbee.mbplefeatureide;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.event.FeatureIDEEvent;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.ui.editors.FeatureModelEditor;
import org.eclipse.ui.PlatformUI;

public class FeatureModelModifier {
    private final IFeatureModelImporter importer;
    private final FeatureModelManager featureModelManager;

    /**
     * Constructor that accepts a FeatureModelManager instance.
     *
     * @param importer            The feature model importer.
     * @param featureModelManager The FeatureModelManager instance.
     */
    public FeatureModelModifier(IFeatureModelImporter importer, FeatureModelManager featureModelManager) {
        this.importer = importer;
        this.featureModelManager = featureModelManager;
    }

    /**
     * Constructor for default usage without a pre-supplied FeatureModelManager.
     *
     * @param importer The feature model importer.
     */
    public FeatureModelModifier(IFeatureModelImporter importer) {
        this(importer, null);
    }

    public void importFeaturesAndRefresh() {
        try {
            // Access the FeatureModelEditor instance
            FeatureModelEditor editor = (FeatureModelEditor) PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow()
                .getActivePage()
                .getActiveEditor();

            System.out.println("[DEBUG] Editor instance acquired.");

            // Use supplied FeatureModelManager or get it from the editor
            FeatureModelManager fmManager = featureModelManager != null ? featureModelManager : editor.getFeatureModelManager();
            System.out.println("[DEBUG] FeatureModelManager acquired.");

            // Get the current feature model
            IFeatureModel featureModel = fmManager.getObject();
            System.out.println("[DEBUG] Initial Feature Model HashCode: " + featureModel.hashCode());
            System.out.println("[DEBUG] Features in model before import:");
            featureModel.getStructure().getFeaturesPreorder().forEach(feature -> 
                System.out.println("  - " + feature.getName())
            );

            // Import features using the selected importer
            importer.importFeatures(featureModel);
            System.out.println("[DEBUG] Features imported successfully.");

            // Set the root of the imported feature tree as the root in the FeatureIDE feature model
            IFeatureStructure importedRoot = featureModel.getStructure().getRoot();
            if (importedRoot != null) {
                featureModel.getStructure().setRoot(importedRoot);
                System.out.println("[DEBUG] Imported root feature set as the root of the feature model: " + importedRoot.getFeature().getName());
            } else {
                System.out.println("[WARNING] No imported root feature found to set as the root.");
            }

            // Display the features after import
            System.out.println("[DEBUG] Features in model after import:");
            featureModel.getStructure().getFeaturesPreorder().forEach(feature -> 
                System.out.println("  - " + feature.getName() + "(" + feature.getInternalId() + ")")
            );

            // Synchronize changes with FeatureModelManager
            fmManager.overwrite();
            System.out.println("[DEBUG] FeatureModelManager updated using overwrite().");

            // Fire a structure change event
            FeatureIDEEvent event = new FeatureIDEEvent(featureModel, FeatureIDEEvent.EventType.STRUCTURE_CHANGED);
            fmManager.fireEvent(event);
            System.out.println("[DEBUG] Feature model structure change event fired.");
            fmManager.fireEvent(new FeatureIDEEvent(featureModel, FeatureIDEEvent.EventType.MODEL_DATA_CHANGED));
            System.out.println("[DEBUG] Feature model data change event fired.");

            // Display the features 
            System.out.println("[DEBUG] Features in model before saving:");
            featureModel.getStructure().getFeaturesPreorder().forEach(feature -> 
            	System.out.println("  - " + feature.getName() + "(" + feature.getInternalId() + ")")
            );
            
            
            // Save the model using the correct logic
            boolean saveSuccessful = FeatureModelManager.save(
                featureModel,
                featureModel.getSourceFile(),
                fmManager.getFormat()
            );

            if (saveSuccessful) {
                System.out.println("[DEBUG] Feature model saved successfully to file: " + featureModel.getSourceFile());
            } else {
                System.out.println("[ERROR] Failed to save the feature model.");
            }

            // Refresh the editor to reflect changes
            editor.readModel(fmManager.getObject().toString());
            System.out.println("[DEBUG] Editor reinitialized with updated model.");

            // Verify the model is updated in the editor
            System.out.println("[DEBUG] Verifying updated model in editor:");
            editor.getFeatureModelManager()
                  .getObject()
                  .getStructure()
                  .getFeaturesPreorder()
                  .forEach(feature -> System.out.println("  - " + feature.getName()));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ERROR] An error occurred during feature import: " + e.getMessage());
        }
    }
}
