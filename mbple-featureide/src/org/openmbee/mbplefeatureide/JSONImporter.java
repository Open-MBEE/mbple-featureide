package org.openmbee.mbplefeatureide;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.impl.DefaultFeatureModelFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * Fetches a feature tree from the Flask server's {@code /api/query-features}
 * endpoint and imports it into the FeatureIDE feature model.
 *
 * <p>
 * Three constructors are available:
 * </p>
 * <ul>
 * <li>{@link #JSONImporter()} – uses hardcoded default values (backwards
 * compatibility).</li>
 * <li>{@link #JSONImporter(String)} – custom Flask endpoint URL, uses hardcoded
 * project/commit.</li>
 * <li>{@link #JSONImporter(String, String, String)} – full configuration
 * supplied at runtime,
 * typically read from {@link SysmlServerPreferences} by
 * {@code ImportFeaturesHandler}.</li>
 * </ul>
 */
public class JSONImporter implements IFeatureModelImporter {

    private final String apiUrl;
    private final String serverUrl;
    private final String projectId;
    private final String commitId;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Full constructor – all values are supplied explicitly.
     *
     * @param serverUrl Base URL of the SysML v2 API server (forwarded to Flask).
     * @param projectId Project UUID.
     * @param commitId  Commit UUID.
     */
    public JSONImporter(String serverUrl, String projectId, String commitId) {
        this.apiUrl = SysmlServerPreferences.getServerUrl() + "/api/query-features";
        this.serverUrl = serverUrl;
        this.projectId = projectId;
        this.commitId = commitId;
    }

    /**
     * Constructor with a custom Flask endpoint URL.
     * Falls back to the hardcoded project/commit defaults.
     */
    public JSONImporter(String apiUrl) {
        this.apiUrl = apiUrl;
        this.serverUrl = "http://sysml2.intercax.com:9000";
        this.projectId = "f8843221-5843-4626-90ea-866b24102333";
        this.commitId = "6ce6f484-f5a2-42be-b0fb-309b98cdc1a0";
    }

    /**
     * No-arg constructor for backwards compatibility.
     * Uses the local Flask server and hardcoded demo project/commit.
     */
    public JSONImporter() {
        this("http://127.0.0.1:5000/api/query-features");
    }

    // ── IFeatureModelImporter ─────────────────────────────────────────────────

    @Override
    public void importFeatures(IFeatureModel featureModel) throws Exception {
        System.out.println("[DEBUG] JSON Importer started.");
        System.out.println("[DEBUG] Flask endpoint: " + apiUrl);
        System.out.println("[DEBUG] SysML server:   " + serverUrl);
        System.out.println("[DEBUG] Project ID:     " + projectId);
        System.out.println("[DEBUG] Commit ID:      " + commitId);

        // Build JSON payload
        JSONObject payload = new JSONObject();
        payload.put("server_url", serverUrl);
        payload.put("project_id", projectId);
        payload.put("commit_id", commitId);
        payload.put("response_kind", "features");
        payload.put("response_format", "json");

        // POST to Flask
        URI uri = URI.create(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(60_000);
        connection.setDoOutput(true);
        connection.getOutputStream().write(payload.toString().getBytes("UTF-8"));

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Failed to fetch JSON data: HTTP " + responseCode);
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONArray jsonTree = new JSONArray(response.toString());

        System.out.println("[DEBUG] Parsing JSON tree...");
        if (jsonTree.length() > 0) {
            JSONObject rootObject = jsonTree.getJSONObject(0);

            IFeatureModelFactory factory = DefaultFeatureModelFactory.getInstance();
            IFeature rootFeature = factory.createFeature(featureModel, rootObject.getString("name"));
            featureModel.addFeature(rootFeature);
            featureModel.getStructure().setRoot(rootFeature.getStructure());
            System.out.println("[DEBUG] Root feature created: " + rootFeature.getName());

            if (rootObject.has("children")) {
                JSONArray children = rootObject.getJSONArray("children");
                for (int i = 0; i < children.length(); i++) {
                    importFeatureTree(children.getJSONObject(i), featureModel, rootFeature.getStructure());
                }
            }
            System.out.println("[DEBUG] Root feature import completed.");
        } else {
            System.out.println("[WARNING] JSON tree is empty.");
        }
    }

    // ── Recursive import helper ───────────────────────────────────────────────

    private void importFeatureTree(JSONObject node, IFeatureModel featureModel, IFeatureStructure parent) {
        String featureName = node.getString("name");
        System.out.println("[DEBUG] Importing feature: " + featureName);
        IFeatureModelFactory factory = DefaultFeatureModelFactory.getInstance();

        IFeature feature = factory.createFeature(featureModel, featureName);
        featureModel.addFeature(feature);

        if (parent != null) {
            parent.addChild(feature.getStructure());
            System.out.println("[DEBUG] Added feature '" + featureName
                    + " (" + feature.getInternalId() + ")" + "' as a child of '"
                    + parent.getFeature().getName()
                    + " (" + parent.getFeature().getInternalId() + ")'.");
        } else {
            featureModel.getStructure().setRoot(feature.getStructure());
            System.out.println("[DEBUG] Added feature '" + featureName + "' as the root feature.");
        }

        if (node.has("children")) {
            JSONArray children = node.getJSONArray("children");
            for (int i = 0; i < children.length(); i++) {
                importFeatureTree(children.getJSONObject(i), featureModel, feature.getStructure());
            }
        }
    }
}
