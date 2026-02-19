package com.example.featureide.importer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

public class ImportFeaturesHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // Present the user with a dialog to select the importer
        String[] options = {"JSON Importer", "CSV Importer"};
        MessageDialog dialog = new MessageDialog(
                Display.getDefault().getActiveShell(),
                "Select Importer",
                null,
                "Choose the feature importer to use:",
                MessageDialog.QUESTION,
                options,
                0
        );

        int selectedOption = dialog.open();

        IFeatureModelImporter importer;
        if (selectedOption == 0) {
            importer = new JSONImporter();
            System.out.println("JSON Importer selected.");
        } else if (selectedOption == 1) {
            importer = new CSVImporter();
            System.out.println("CSV Importer selected.");
        } else {
            System.out.println("No importer selected. Aborting.");
            return null;
        }

        // Modify the feature model using the selected importer
        FeatureModelModifier modifier = new FeatureModelModifier(importer);
        try {
            modifier.importFeaturesAndRefresh();
        } catch (Exception e) {
            throw new ExecutionException("Failed to import features.", e);
        }

        return null;
    }
}
