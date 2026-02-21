package org.openmbee.mbplefeatureide;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.IEditorPart;

import de.ovgu.featureide.fm.ui.editors.FeatureModelEditor;
import de.ovgu.featureide.fm.core.base.IFeatureModel;

public class ActiveEditorUtil {
    public static FeatureModelEditor getActiveFeatureModelEditor() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorPart editor = page.getActiveEditor();

        if (editor instanceof FeatureModelEditor) {
            return (FeatureModelEditor) editor;
        }

        return null;
    }

    public static IFeatureModel getActiveFeatureModel() {
        FeatureModelEditor editor = getActiveFeatureModelEditor();
        if (editor != null) {
            return editor.getOriginalFeatureModel();
        }
        return null;
    }
}
