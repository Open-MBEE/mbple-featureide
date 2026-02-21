package org.openmbee.mbplefeatureide;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A multi-step configuration dialog for the SysML v2 repository connection.
 *
 * <p>
 * Step 1 – Enter the Flask server URL and retrieve the list of projects.<br>
 * Step 2 – Select a project and retrieve its commits.<br>
 * Step 3 – Select a commit; the dialog automatically checks whether the commit
 * contains a PLEML feature model via the Flask server.
 * </p>
 *
 * <p>
 * Call {@link #open()} and check whether it returned
 * {@link IDialogConstants#OK_ID}.
 * Then use {@link #getServerUrl()}, {@link #getSelectedProjectId()},
 * {@link #getSelectedProjectName()}, {@link #getSelectedCommitId()}, and
 * {@link #getSelectedCommitName()} to retrieve the configured values.
 * </p>
 */
public class SysmlServerConfigDialog extends Dialog {

    // ── State returned to caller ──────────────────────────────────────────────
    private String serverUrl;
    private String sysmlRepoUrl;
    private String selectedProjectId;
    private String selectedProjectName;
    private String selectedCommitId;
    private String selectedCommitName;

    // ── Internal data lists ───────────────────────────────────────────────────
    /** Each entry: display label stored in parallel with ids/names lists. */
    private final List<String> projectIds = new ArrayList<>();
    private final List<String> projectNames = new ArrayList<>();
    private final List<String> commitIds = new ArrayList<>();
    private final List<String> commitNames = new ArrayList<>();

    // ── SWT widgets ───────────────────────────────────────────────────────────
    private Text serverUrlText;
    private Text sysmlRepoUrlText;

    private org.eclipse.swt.widgets.List projectList;
    private Button getProjectsButton;

    private org.eclipse.swt.widgets.List commitList;
    private Button getCommitsButton;

    private Label plemlStatusLabel;
    private Label plemlIconLabel;

    /** UUID ID text fields (editable, synced from list selection). */
    private Text projectIdText;
    private Text commitIdText;

    // ── Constructor ───────────────────────────────────────────────────────────

    public SysmlServerConfigDialog(Shell parentShell) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    // ── Dialog lifecycle ──────────────────────────────────────────────────────

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Configure SysML v2 Repository");
        shell.setMinimumSize(900, 700);
        shell.setSize(900, 1000);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        area.setLayout(new GridLayout(1, false));

        createServerSection(area);
        createSeparator(area);
        createProjectSection(area);
        createSeparator(area);
        createCommitSection(area);
        createSeparator(area);
        createPlemlSection(area);

        // Pre-fill from saved configuration
        serverUrlText.setText(SysmlServerPreferences.getServerUrl());
        sysmlRepoUrlText.setText(SysmlServerPreferences.getSysmlRepoUrl());
        projectIdText.setText(SysmlServerPreferences.getProjectId());
        commitIdText.setText(SysmlServerPreferences.getCommitId());
        // Sync selectedXxx fields so updateOkButton and okPressed see the values
        selectedProjectId = SysmlServerPreferences.getProjectId();
        selectedCommitId = SysmlServerPreferences.getCommitId();
        // Restore display names (stored in Eclipse prefs) so they survive a config-only
        // open→OK
        selectedProjectName = SysmlServerPreferences.getProjectName();
        selectedCommitName = SysmlServerPreferences.getCommitName();

        return area;
    }

    // ── Section builders ──────────────────────────────────────────────────────

    /** Step 1 – Flask Server URL, SysML v2 Repository URL, then "Get Projects". */
    private void createServerSection(Composite parent) {
        createSectionHeader(parent, "Step 1 – Server Configuration");

        Composite grid = new Composite(parent, SWT.NONE);
        grid.setLayout(new GridLayout(2, false));
        grid.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        // Row 1 – Flask server URL
        Label flaskLbl = new Label(grid, SWT.NONE);
        flaskLbl.setText("Flask Server URL:");

        serverUrlText = new Text(grid, SWT.BORDER | SWT.SINGLE);
        serverUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        serverUrlText.setMessage("e.g. http://127.0.0.1:5000");

        // Row 2 – SysML v2 Repository server URL
        Label sysmlLbl = new Label(grid, SWT.NONE);
        sysmlLbl.setText("SysML v2 Repository URL:");

        sysmlRepoUrlText = new Text(grid, SWT.BORDER | SWT.SINGLE);
        sysmlRepoUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        sysmlRepoUrlText.setMessage("e.g. http://sysml2.intercax.com:9000");

        // Row 3 – "Get Projects" button, right-aligned, spanning both columns
        new Label(grid, SWT.NONE); // spacer
        getProjectsButton = new Button(grid, SWT.PUSH);
        getProjectsButton.setText("Get Projects");
        getProjectsButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        getProjectsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onGetProjects();
            }
        });
    }

    /** Step 2 – Project list. */
    private void createProjectSection(Composite parent) {
        createSectionHeader(parent, "Step 2 – Select Project");

        Composite col = new Composite(parent, SWT.NONE);
        col.setLayout(new GridLayout(2, false));
        col.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        projectList = new org.eclipse.swt.widgets.List(col, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
        GridData listGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        listGd.heightHint = 600;
        projectList.setLayoutData(listGd);

        getCommitsButton = new Button(col, SWT.PUSH);
        getCommitsButton.setText("Get Commits");
        getCommitsButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        getCommitsButton.setEnabled(false);
        getCommitsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onGetCommits();
            }
        });

        projectList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = projectList.getSelectionIndex();
                if (idx >= 0) {
                    selectedProjectId = projectIds.get(idx);
                    selectedProjectName = projectNames.get(idx);
                    projectIdText.setText(selectedProjectId); // sync ID field
                    getCommitsButton.setEnabled(true);
                    // Clear stale commit data
                    commitList.removeAll();
                    commitIds.clear();
                    commitNames.clear();
                    selectedCommitId = null;
                    selectedCommitName = null;
                    commitIdText.setText("");
                    clearPlemlStatus();
                    updateOkButton();
                }
            }
        });

        // Row below list: Project ID field
        Composite idRow = new Composite(col, SWT.NONE);
        idRow.setLayout(new GridLayout(2, false));
        GridData idRowGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        idRowGd.horizontalSpan = 2;
        idRow.setLayoutData(idRowGd);

        Label projectIdLbl = new Label(idRow, SWT.NONE);
        projectIdLbl.setText("Project ID:");

        projectIdText = new Text(idRow, SWT.BORDER | SWT.SINGLE);
        GridData projectIdGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        projectIdGd.widthHint = convertWidthInCharsToPixels(40); // UUID = 36 chars
        projectIdText.setLayoutData(projectIdGd);
        projectIdText.setMessage("UUID of the selected project");
        // Manual edit updates selectedProjectId
        projectIdText.addModifyListener(e -> selectedProjectId = projectIdText.getText().trim());
    }

    /** Step 3 – Commit list. */
    private void createCommitSection(Composite parent) {
        createSectionHeader(parent, "Step 3 – Select Commit");

        commitList = new org.eclipse.swt.widgets.List(parent, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
        GridData listGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        listGd.heightHint = 85; // ~5 rows
        commitList.setLayoutData(listGd);

        commitList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = commitList.getSelectionIndex();
                if (idx >= 0) {
                    selectedCommitId = commitIds.get(idx);
                    selectedCommitName = commitNames.get(idx);
                    commitIdText.setText(selectedCommitId); // sync ID field
                    updateOkButton();
                    onCheckPleml(); // auto-trigger PLEML check
                }
            }
        });

        // Row below list: Commit ID field
        Composite idRow = new Composite(parent, SWT.NONE);
        idRow.setLayout(new GridLayout(2, false));
        idRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label commitIdLbl = new Label(idRow, SWT.NONE);
        commitIdLbl.setText("Commit ID:");

        commitIdText = new Text(idRow, SWT.BORDER | SWT.SINGLE);
        GridData commitIdGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        commitIdGd.widthHint = convertWidthInCharsToPixels(40);
        commitIdText.setLayoutData(commitIdGd);
        commitIdText.setMessage("UUID of the selected commit");
        // Manual edit updates selectedCommitId
        commitIdText.addModifyListener(e -> selectedCommitId = commitIdText.getText().trim());
    }

    /** PLEML check result area. */
    private void createPlemlSection(Composite parent) {
        createSectionHeader(parent, "PLEML Feature Model Check");

        Composite row = new Composite(parent, SWT.NONE);
        row.setLayout(new GridLayout(2, false));
        row.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        plemlIconLabel = new Label(row, SWT.NONE);
        plemlIconLabel.setText("   ");

        plemlStatusLabel = new Label(row, SWT.WRAP);
        plemlStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        plemlStatusLabel.setText("Select a commit to run the check.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void createSectionHeader(Composite parent, String text) {
        Label lbl = new Label(parent, SWT.NONE);
        lbl.setText(text);
        GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.verticalIndent = 6;
        lbl.setLayoutData(gd);
        // Make it bold
        org.eclipse.swt.graphics.FontData[] fd = lbl.getFont().getFontData();
        for (org.eclipse.swt.graphics.FontData d : fd) {
            d.setStyle(org.eclipse.swt.SWT.BOLD);
        }
        org.eclipse.swt.graphics.Font boldFont = new org.eclipse.swt.graphics.Font(lbl.getDisplay(), fd);
        lbl.setFont(boldFont);
        lbl.addDisposeListener(e -> boldFont.dispose());
    }

    private void createSeparator(Composite parent) {
        Label sep = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
        sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void clearPlemlStatus() {
        if (plemlIconLabel != null && !plemlIconLabel.isDisposed()) {
            plemlIconLabel.setText("   ");
            plemlStatusLabel.setText("Select a commit to run the check.");
        }
    }

    private void updateOkButton() {
        Button ok = getButton(IDialogConstants.OK_ID);
        if (ok != null) {
            // Enable when both ID fields are non-empty (set from list, config, or manual
            // entry)
            String projId = projectIdText != null ? projectIdText.getText().trim()
                    : (selectedProjectId != null ? selectedProjectId : "");
            String commitId = commitIdText != null ? commitIdText.getText().trim()
                    : (selectedCommitId != null ? selectedCommitId : "");
            ok.setEnabled(!projId.isEmpty() && !commitId.isEmpty());
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    private void onGetProjects() {
        String flaskUrl = serverUrlText.getText().trim();
        String repoUrl = sysmlRepoUrlText.getText().trim();
        if (flaskUrl.isEmpty()) {
            showError("Please enter the Flask server URL.");
            return;
        }
        if (repoUrl.isEmpty()) {
            showError("Please enter the SysML v2 Repository Server URL.");
            return;
        }

        projectList.removeAll();
        projectIds.clear();
        projectNames.clear();
        commitList.removeAll();
        commitIds.clear();
        commitNames.clear();
        selectedProjectId = null;
        selectedProjectName = null;
        selectedCommitId = null;
        selectedCommitName = null;
        clearPlemlStatus();
        getCommitsButton.setEnabled(false);
        updateOkButton();

        try {
            JSONObject payload = new JSONObject();
            payload.put("server_url", repoUrl);
            payload.put("page_size", String.valueOf(SysmlServerPreferences.getPageSize()));

            String body = httpPost(flaskUrl + "/api/projects", payload.toString());
            JSONArray arr = new JSONArray(body);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String id = obj.optString("id", obj.optString("@id", "unknown-id-" + i));
                String name = obj.optString("name", obj.optString("declaredName", id));
                projectIds.add(id);
                projectNames.add(name);
                projectList.add(name);
            }

            if (arr.length() == 0) {
                showInfo("No projects found on the server.");
            }
        } catch (Exception ex) {
            showError("Failed to retrieve projects:\n" + ex.getMessage());
        }
    }

    private void onGetCommits() {
        if (selectedProjectId == null) {
            showError("Please select a project first.");
            return;
        }

        String flaskUrl = serverUrlText.getText().trim();
        String repoUrl = sysmlRepoUrlText.getText().trim();
        commitList.removeAll();
        commitIds.clear();
        commitNames.clear();
        selectedCommitId = null;
        selectedCommitName = null;
        clearPlemlStatus();
        updateOkButton();

        try {
            JSONObject payload = new JSONObject();
            payload.put("server_url", repoUrl);
            payload.put("project_id", selectedProjectId);
            payload.put("page_size", String.valueOf(SysmlServerPreferences.getPageSize()));

            String body = httpPost(flaskUrl + "/api/commits", payload.toString());
            JSONArray arr = new JSONArray(body);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String id = obj.optString("id", obj.optString("@id", "unknown-id-" + i));
                String name = obj.optString("name", obj.optString("description", id));
                commitIds.add(id);
                commitNames.add(name);
                commitList.add(name + "  [" + id.substring(0, Math.min(8, id.length())) + "...]");
            }

            if (arr.length() == 0) {
                showInfo("No commits found for the selected project.");
            }
        } catch (Exception ex) {
            showError("Failed to retrieve commits:\n" + ex.getMessage());
        }
    }

    private void onCheckPleml() {
        if (selectedProjectId == null || selectedCommitId == null) {
            return;
        }

        String flaskUrl = serverUrlText.getText().trim();
        String repoUrl = sysmlRepoUrlText.getText().trim();
        plemlIconLabel.setText("⏳");
        plemlStatusLabel.setText("Checking...");

        try {
            JSONObject payload = new JSONObject();
            payload.put("server_url", repoUrl);
            payload.put("project_id", selectedProjectId);
            payload.put("commit_id", selectedCommitId);
            payload.put("page_size", String.valueOf(SysmlServerPreferences.getPageSize()));

            String body = httpPost(flaskUrl + "/api/check-pleml", payload.toString());
            JSONObject obj = new JSONObject(body);
            boolean hasPleml = obj.optBoolean("has_pleml", false);

            if (hasPleml) {
                plemlIconLabel.setText("✓");
                plemlStatusLabel.setText("PLEML feature model found in this commit.");
                plemlStatusLabel.setForeground(
                        plemlStatusLabel.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
            } else {
                plemlIconLabel.setText("✗");
                plemlStatusLabel.setText("No PLEML feature model found in this commit.");
                plemlStatusLabel.setForeground(
                        plemlStatusLabel.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
            }
        } catch (Exception ex) {
            plemlIconLabel.setText("!");
            plemlStatusLabel.setText("Check failed: " + ex.getMessage());
            plemlStatusLabel.setForeground(
                    plemlStatusLabel.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
        }
    }

    // ── OK / Cancel ───────────────────────────────────────────────────────────

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        // Enable OK only when both ID fields are populated.
        // createDialogArea already pre-filled the fields from config, so this
        // runs after all the text is set and correctly enables the button.
        updateOkButton();
    }

    @Override
    protected void okPressed() {
        // Read IDs from text fields (may be manually typed or set from list selection)
        selectedProjectId = projectIdText.getText().trim();
        selectedCommitId = commitIdText.getText().trim();

        // Persist all values to config.properties
        serverUrl = serverUrlText.getText().trim();
        sysmlRepoUrl = sysmlRepoUrlText.getText().trim();
        SysmlServerPreferences.setServerUrl(serverUrl);
        SysmlServerPreferences.setSysmlRepoUrl(sysmlRepoUrl);
        SysmlServerPreferences.setProject(selectedProjectId, selectedProjectName);
        SysmlServerPreferences.setCommit(selectedCommitId, selectedCommitName);
        SysmlServerPreferences.saveConfig();
        super.okPressed();
    }

    // ── Getters (usable after open() returns OK) ──────────────────────────────

    public String getServerUrl() {
        return serverUrl;
    }

    public String getSysmlRepoUrl() {
        return sysmlRepoUrl;
    }

    public String getSelectedProjectId() {
        return selectedProjectId;
    }

    public String getSelectedProjectName() {
        return selectedProjectName;
    }

    public String getSelectedCommitId() {
        return selectedCommitId;
    }

    public String getSelectedCommitName() {
        return selectedCommitName;
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    /**
     * HTTP POST with a JSON body; returns the response body as a String.
     *
     * @param endpoint fully qualified URL string
     * @param jsonBody serialised JSON to send as the request body
     * @return response body
     * @throws Exception on any I/O or HTTP error
     */
    private static String httpPost(String endpoint, String jsonBody) throws Exception {
        URI uri = URI.create(endpoint);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(30_000);
        conn.setDoOutput(true);

        byte[] bytes = jsonBody.getBytes("UTF-8");
        conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
        }

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP " + code + " from " + endpoint);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /**
     * HTTP GET; returns the response body as a String.
     *
     * @param endpoint fully qualified URL string
     * @return response body
     * @throws Exception on any I/O or HTTP error
     */
    private static String httpGet(String endpoint) throws Exception {
        URI uri = URI.create(endpoint);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(30_000);

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP " + code + " from " + endpoint);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    // ── Dialog helpers ────────────────────────────────────────────────────────

    private void showError(String message) {
        org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Error", message);
    }

    private void showInfo(String message) {
        org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Info", message);
    }
}
