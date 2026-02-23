package org.openmbee.mbplefeatureide;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Early startup that hooks "Import Feature Tree" into the FeatureIDE
 * FeatureModelEditor GEF context menu.
 *
 * <p>
 * Imports are restricted to org.eclipse.ui, org.eclipse.jface and
 * org.eclipse.core.runtime — all of which are in Require-Bundle.
 * No FeatureIDE or org.osgi compile dependency is needed; GEF classes are
 * loaded at runtime via the editor part's own classloader.
 * </p>
 */
public class MbpleStartup implements IStartup {

    private static final String EDITOR_ID = "de.ovgu.featureide.fm.ui.editors.FeatureModelEditor";
    private static final String SEPARATOR_ID = "org.openmbee.mbplefeatureide.importSeparator";

    @Override
    public void earlyStartup() {
        Display.getDefault().asyncExec(() -> {
            IWorkbench wb = PlatformUI.getWorkbench();
            for (IWorkbenchWindow win : wb.getWorkbenchWindows()) {
                win.getPartService().addPartListener(partListener);
                // Editors restored before earlyStartup ran won't fire partOpened.
                // Scan currently open pages and hook any already-open FeatureModelEditor.
                if (win.getActivePage() != null) {
                    for (IEditorPart ed : win.getActivePage().getEditors()) {
                        if (EDITOR_ID.equals(ed.getSite().getId())) {
                            IWorkbenchPartReference ref = win.getActivePage().getReference(ed);
                            if (ref != null) {
                                Display.getDefault().asyncExec(
                                        () -> hookContextMenu(ref));
                            }
                        }
                    }
                }
            }
            wb.addWindowListener(new IWindowListener() {
                @Override
                public void windowOpened(IWorkbenchWindow w) {
                    w.getPartService().addPartListener(partListener);
                }

                @Override
                public void windowActivated(IWorkbenchWindow w) {
                }

                @Override
                public void windowDeactivated(IWorkbenchWindow w) {
                }

                @Override
                public void windowClosed(IWorkbenchWindow w) {
                }
            });
        });
    }

    private final IPartListener2 partListener = new IPartListener2() {
        @Override
        public void partOpened(IWorkbenchPartReference ref) {
            if (!EDITOR_ID.equals(ref.getId()))
                return;
            // Two asyncExecs: first waits for part init, second for GEF viewer init.
            Display.getDefault().asyncExec(() -> Display.getDefault().asyncExec(() -> hookContextMenu(ref)));
        }

        @Override
        public void partActivated(IWorkbenchPartReference r) {
        }

        @Override
        public void partBroughtToTop(IWorkbenchPartReference r) {
        }

        @Override
        public void partClosed(IWorkbenchPartReference r) {
        }

        @Override
        public void partDeactivated(IWorkbenchPartReference r) {
        }

        @Override
        public void partHidden(IWorkbenchPartReference r) {
        }

        @Override
        public void partVisible(IWorkbenchPartReference r) {
        }

        @Override
        public void partInputChanged(IWorkbenchPartReference r) {
        }
    };

    private void hookContextMenu(IWorkbenchPartReference ref) {
        try {
            Object part = ref.getPart(false);
            if (part == null)
                return;

            // Use the editor's own classloader (FeatureIDE bundle) to load
            // GEF — no compile-time imports of FeatureIDE or GEF needed.
            ClassLoader cl = part.getClass().getClassLoader();
            Class<?> viewerClass = cl.loadClass("org.eclipse.gef.GraphicalViewer");

            // Try adapter directly on the editor part.
            Object viewer = ((IAdaptable) part).getAdapter(viewerClass);

            if (viewer == null) {
                System.err.println("mbple-featureide: GraphicalViewer adapter is null");
                return;
            }

            Object contextMenu = viewerClass.getMethod("getContextMenu").invoke(viewer);
            if (!(contextMenu instanceof MenuManager)) {
                System.err.println("mbple-featureide: contextMenu class = "
                        + (contextMenu == null ? "null" : contextMenu.getClass().getName()));
                return;
            }

            ((MenuManager) contextMenu).addMenuListener(new IMenuListener() {
                @Override
                public void menuAboutToShow(IMenuManager manager) {
                    if (manager.find(SEPARATOR_ID) == null) {
                        manager.add(new Separator(SEPARATOR_ID));
                        manager.add(createImportAction());
                    }
                }
            });
            System.err.println("mbple-featureide: Import Feature Tree hooked OK");

        } catch (Exception e) {
            System.err.println("mbple-featureide: hookContextMenu failed: " + e);
        }
    }

    private static Action createImportAction() {
        return new Action("Import Feature Tree") {
            @Override
            public void run() {
                try {
                    new ImportFeaturesHandler().execute(null);
                } catch (Exception e) {
                    System.err.println("mbple-featureide: Import failed: " + e);
                }
            }
        };
    }
}
