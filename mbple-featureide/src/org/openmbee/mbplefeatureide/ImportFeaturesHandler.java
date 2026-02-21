package org.openmbee.mbplefeatureide;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

public class ImportFeaturesHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // Present the user with a dialog to select the importer
        String[] options = { "JSON Importer", "CSV Importer", "SysML v2 Repository" };
        MessageDialog dialog = new MessageDialog(
                Display.getDefault().getActiveShell(),
                "Select Importer",
                null,
                "Choose the feature importer to use:",
                MessageDialog.QUESTION,
                options,
                0);

        int selectedOption = dialog.open();

        IFeatureModelImporter importer;
        if (selectedOption == 0) {
            importer = new JSONImporter();
            System.out.println("JSON Importer selected.");

        } else if (selectedOption == 1) {
            importer = new CSVImporter();
            System.out.println("CSV Importer selected.");

        } else if (selectedOption == 2) {
            // SysML v2 Repository importer – ensure we have a configuration
            if (!SysmlServerPreferences.isConfigured()) {
                System.out.println("No SysML v2 configuration found – opening configuration dialog.");
                SysmlServerConfigDialog configDialog = new SysmlServerConfigDialog(
                        Display.getDefault().getActiveShell());
                int result = configDialog.open();
                if (result != IDialogConstants.OK_ID) {
                    System.out.println("Configuration cancelled. Aborting import.");
                    return null;
                }
            }

            String serverUrl = SysmlServerPreferences.getServerUrl();
            String projectId = SysmlServerPreferences.getProjectId();
            String commitId = SysmlServerPreferences.getCommitId();

            System.out.println("SysML v2 Repository Importer selected.");
            System.out.println("  Server:  " + serverUrl);
            System.out.println("  Project: " + projectId);
            System.out.println("  Commit:  " + commitId);

            importer = new JSONImporter(serverUrl, projectId, commitId);

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
