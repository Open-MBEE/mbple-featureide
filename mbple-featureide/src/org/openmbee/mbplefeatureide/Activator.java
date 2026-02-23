package org.openmbee.mbplefeatureide;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.openmbee.mbplefeatureide";
    private static final String EDITOR_ID = "de.ovgu.featureide.fm.ui.editors.FeatureModelEditor";
    private static final String SEPARATOR_ID = "org.openmbee.mbplefeatureide.importSeparator";

    private static Activator plugin;

    public Activator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        // Hook the Import Feature Tree entry into the FeatureIDE diagram context
        // menu. We defer to the UI thread via asyncExec because the workbench
        // may not be fully initialised when the bundle starts.
        Display.getDefault().asyncExec(() -> {
            if (!PlatformUI.isWorkbenchRunning())
                return;
            IWorkbench wb = PlatformUI.getWorkbench();
            // Register on already-open windows
            for (IWorkbenchWindow win : wb.getWorkbenchWindows()) {
                win.getPartService().addPartListener(PART_LISTENER);
                hookOpenEditors(win);
            }
            // Register on windows opened in the future
            wb.addWindowListener(new IWindowListener() {
                @Override
                public void windowOpened(IWorkbenchWindow w) {
                    w.getPartService().addPartListener(PART_LISTENER);
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

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static Activator getDefault() {
        return plugin;
    }

    // ── Feature diagram context-menu hook ─────────────────────────────────────

    /** Scans already-open editors in a window and hooks any FeatureModelEditor. */
    private static void hookOpenEditors(IWorkbenchWindow win) {
        if (win.getActivePage() == null)
            return;
        for (IEditorPart ed : win.getActivePage().getEditors()) {
            if (EDITOR_ID.equals(ed.getSite().getId())) {
                IWorkbenchPartReference ref = win.getActivePage().getReference(ed);
                if (ref != null) {
                    Display.getDefault().asyncExec(() -> hookContextMenu(ref));
                }
            }
        }
    }

    private static final IPartListener2 PART_LISTENER = new IPartListener2() {
        @Override
        public void partOpened(IWorkbenchPartReference ref) {
            if (!EDITOR_ID.equals(ref.getId()))
                return;
            // Two asyncExecs: first lets the part finish, second lets the GEF
            // viewer create its context menu.
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

    private static void hookContextMenu(IWorkbenchPartReference ref) {
        try {
            Object part = ref.getPart(false);
            if (part == null)
                return;

            // Load GEF's GraphicalViewer via the editor's own classloader
            // (FeatureIDE bundle depends on GEF so its classloader can resolve it).
            // No compile-time dependency on GEF or FeatureIDE is introduced.
            ClassLoader cl = part.getClass().getClassLoader();
            Class<?> viewerClass = cl.loadClass("org.eclipse.gef.GraphicalViewer");

            Object viewer = part.getClass()
                    .getMethod("getAdapter", Class.class)
                    .invoke(part, viewerClass);

            if (viewer == null) {
                // getActiveEditor() is protected on MultiPageEditorPart;
                // use reflection so visibility is not an issue.
                try {
                    java.lang.reflect.Method m = part.getClass().getDeclaredMethod("getActiveEditor");
                    m.setAccessible(true);
                    Object page = m.invoke(part);
                    if (page != null) {
                        viewer = page.getClass()
                                .getMethod("getAdapter", Class.class)
                                .invoke(page, viewerClass);
                    }
                } catch (NoSuchMethodException ignored) {
                    // Part is not a MultiPageEditorPart or has no active page.
                }
            }

            if (viewer == null) {
                System.err.println("mbple-featureide: GraphicalViewer adapter is null");
                return;
            }

            Object menu = viewerClass.getMethod("getContextMenu").invoke(viewer);
            if (!(menu instanceof MenuManager)) {
                System.err.println("mbple-featureide: contextMenu="
                        + (menu == null ? "null" : menu.getClass().getName()));
                return;
            }

            ((MenuManager) menu).addMenuListener(new IMenuListener() {
                @Override
                public void menuAboutToShow(IMenuManager mgr) {
                    if (mgr.find(SEPARATOR_ID) == null) {
                        mgr.add(new Separator(SEPARATOR_ID));
                        mgr.add(new Action("Import Feature Tree") {
                            @Override
                            public void run() {
                                try {
                                    new ImportFeaturesHandler().execute(null);
                                } catch (Exception e) {
                                    System.err.println("mbple-featureide: Import failed: " + e);
                                }
                            }
                        });
                    }
                }
            });
            System.err.println("mbple-featureide: Import Feature Tree hooked OK");

        } catch (Exception e) {
            System.err.println("mbple-featureide: hookContextMenu failed: " + e);
        }
    }
}
