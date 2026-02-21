package org.openmbee.mbplefeatureide;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * Handler for the "Configure SysML v2 Repository" command.
 *
 * <p>
 * Opens {@link SysmlServerConfigDialog} so the user can enter the Flask
 * server URL, browse projects and commits, and verify the PLEML model
 * availability. On OK the selected values are persisted via
 * {@link SysmlServerPreferences}.
 * </p>
 */
public class ConfigureSysmlServerHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SysmlServerConfigDialog dialog = new SysmlServerConfigDialog(Display.getDefault().getActiveShell());

        int result = dialog.open();

        if (result == IDialogConstants.OK_ID) {
            MessageDialog.openInformation(
                    Display.getDefault().getActiveShell(),
                    "Configuration Saved",
                    "SysML v2 repository configuration has been saved:\n\n"
                            + "Server URL: " + dialog.getServerUrl() + "\n"
                            + "Project:    " + dialog.getSelectedProjectName() + "\n"
                            + "Commit:     " + dialog.getSelectedCommitName());
        }

        return null;
    }
}
