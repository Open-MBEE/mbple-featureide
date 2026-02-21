package org.openmbee.mbplefeatureide;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Persists and retrieves SysML v2 repository configuration.
 *
 * <p>
 * All values (URLs, project ID, commit ID, page size) are stored in a plain
 * {@code config.properties} file so they survive across sessions and can be
 * edited manually. The writable copy lives in the bundle's data area if
 * available, otherwise in {@code ~/.mbplefeatureide/config.properties}.
 * </p>
 */
public class SysmlServerPreferences {

    // ── Config file keys ──────────────────────────────────────────────────────

    private static final String KEY_FLASK_URL = "sysml_api.flask_url";
    private static final String KEY_REPO_URL = "sysml_api.repo_url";
    private static final String KEY_PAGE_SIZE = "sysml_api.page_size";
    private static final String KEY_PROJECT_ID = "sysml_api.project_id";
    private static final String KEY_COMMIT_ID = "sysml_api.commit_id";

    private static final String DEF_FLASK_URL = "http://127.0.0.1:5000";
    private static final String DEF_REPO_URL = "http://sysml2.intercax.com:9000";
    private static final int DEF_PAGE_SIZE = 512;

    // ── Eclipse preference keys (project / commit display name only) ───────────

    private static final String KEY_PROJECT_NAME = "sysml.project.name";
    private static final String KEY_COMMIT_NAME = "sysml.commit.name";

    // ── Config file (lazy-loaded, cached) ─────────────────────────────────────

    private static Properties configProps;
    private static File resolvedConfigFile;

    /**
     * Returns a writable config file path, trying in order:
     * <ol>
     * <li>Bundle data area ({@code getBundle().getDataFile(...)})</li>
     * <li>{@code ~/.mbplefeatureide/config.properties} (always writable
     * fallback)</li>
     * </ol>
     */
    private static File writableFile() {
        if (resolvedConfigFile != null) {
            return resolvedConfigFile;
        }
        try {
            File f = Activator.getDefault().getBundle().getDataFile("config.properties");
            if (f != null) {
                f.getParentFile().mkdirs();
                resolvedConfigFile = f;
                return resolvedConfigFile;
            }
        } catch (Exception ignored) {
        }

        // Fallback: user home
        File dir = new File(System.getProperty("user.home"), ".mbplefeatureide");
        dir.mkdirs();
        resolvedConfigFile = new File(dir, "config.properties");
        return resolvedConfigFile;
    }

    /**
     * Returns the loaded (and cached) Properties. Load priority (highest wins):
     * <ol>
     * <li>Hard-coded defaults</li>
     * <li>Bundled {@code config.properties}</li>
     * <li>Writable user copy</li>
     * </ol>
     */
    private static Properties config() {
        if (configProps != null) {
            return configProps;
        }
        configProps = new Properties();

        // 1. Hard-coded defaults
        configProps.setProperty(KEY_FLASK_URL, DEF_FLASK_URL);
        configProps.setProperty(KEY_REPO_URL, DEF_REPO_URL);
        configProps.setProperty(KEY_PAGE_SIZE, String.valueOf(DEF_PAGE_SIZE));
        configProps.setProperty(KEY_PROJECT_ID, "");
        configProps.setProperty(KEY_COMMIT_ID, "");

        // 2. Bundled config.properties
        try {
            URL bundled = Activator.getDefault().getBundle().getEntry("config.properties");
            if (bundled != null) {
                try (InputStream is = bundled.openStream()) {
                    configProps.load(is);
                }
            }
        } catch (Exception ignored) {
        }

        // 3. Writable user copy (highest priority)
        File saved = writableFile();
        if (saved.exists()) {
            try (InputStream is = new FileInputStream(saved)) {
                configProps.load(is);
            } catch (Exception ignored) {
            }
        }

        return configProps;
    }

    /** Persists the current in-memory configuration to disk. */
    public static void saveConfig() {
        File target = writableFile();
        try {
            target.getParentFile().mkdirs();
            try (FileOutputStream os = new FileOutputStream(target)) {
                config().store(os, "mbple-featureide plugin configuration (auto-saved)");
            }
        } catch (Exception ignored) {
        }
    }

    // ── Flask Server URL ──────────────────────────────────────────────────────

    public static String getServerUrl() {
        return config().getProperty(KEY_FLASK_URL, DEF_FLASK_URL);
    }

    public static void setServerUrl(String url) {
        config().setProperty(KEY_FLASK_URL, url == null ? DEF_FLASK_URL : url.trim());
    }

    // ── SysML v2 Repository URL ───────────────────────────────────────────────

    public static String getSysmlRepoUrl() {
        return config().getProperty(KEY_REPO_URL, DEF_REPO_URL);
    }

    public static void setSysmlRepoUrl(String url) {
        config().setProperty(KEY_REPO_URL, url == null ? DEF_REPO_URL : url.trim());
    }

    // ── Page size ─────────────────────────────────────────────────────────────

    public static int getPageSize() {
        try {
            return Integer.parseInt(
                    config().getProperty(KEY_PAGE_SIZE, String.valueOf(DEF_PAGE_SIZE)).trim());
        } catch (NumberFormatException e) {
            return DEF_PAGE_SIZE;
        }
    }

    // ── Project ───────────────────────────────────────────────────────────────

    public static String getProjectId() {
        return config().getProperty(KEY_PROJECT_ID, "");
    }

    public static String getProjectName() {
        return store().getString(KEY_PROJECT_NAME);
    }

    public static void setProject(String id, String name) {
        config().setProperty(KEY_PROJECT_ID, id == null ? "" : id.trim());
        store().setValue(KEY_PROJECT_NAME, name == null ? "" : name);
    }

    // ── Commit ────────────────────────────────────────────────────────────────

    public static String getCommitId() {
        return config().getProperty(KEY_COMMIT_ID, "");
    }

    public static String getCommitName() {
        return store().getString(KEY_COMMIT_NAME);
    }

    public static void setCommit(String id, String name) {
        config().setProperty(KEY_COMMIT_ID, id == null ? "" : id.trim());
        store().setValue(KEY_COMMIT_NAME, name == null ? "" : name);
    }

    // ── Eclipse preference store (display names only) ─────────────────────────

    private static IPreferenceStore store() {
        return Activator.getDefault().getPreferenceStore();
    }

    // ── Convenience ───────────────────────────────────────────────────────────

    /** Returns true only when all three required values are present. */
    public static boolean isConfigured() {
        return !getServerUrl().isEmpty()
                && !getProjectId().isEmpty()
                && !getCommitId().isEmpty();
    }
}
