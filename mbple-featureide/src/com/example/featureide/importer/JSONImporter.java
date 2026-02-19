package com.example.featureide.importer;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.impl.DefaultFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.impl.Feature;
import de.ovgu.featureide.fm.core.base.impl.FeatureStructure;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

public class JSONImporter implements IFeatureModelImporter {

    private final String apiUrl;

    public JSONImporter(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public JSONImporter() {
        this.apiUrl = "http://127.0.0.1:5000/api/query-features";
    }

    @Override
    public void importFeatures(IFeatureModel featureModel) throws Exception {
        System.out.println("[DEBUG] JSON Importer started.");

        // Create JSON payload
        JSONObject payload = new JSONObject();
        payload.put("server_url", "http://sysml2.intercax.com:9000");
        payload.put("project_id", "f8843221-5843-4626-90ea-866b24102333");
        payload.put("commit_id", "6ce6f484-f5a2-42be-b0fb-309b98cdc1a0");
        payload.put("response_kind", "features");
        payload.put("response_format", "json");

        // Fetch JSON data from the API
        URI uri = URI.create(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Send JSON payload
        connection.getOutputStream().write(payload.toString().getBytes());

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Failed to fetch JSON data: HTTP " + responseCode);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
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
            
            // Create the root feature and explicitly set it as the root in the feature model
            IFeatureModelFactory factory = DefaultFeatureModelFactory.getInstance();
            IFeature rootFeature = factory.createFeature(featureModel, rootObject.getString("name"));
            featureModel.addFeature(rootFeature);
            featureModel.getStructure().setRoot(rootFeature.getStructure());
            System.out.println("[DEBUG] Root feature created: " + rootFeature.getName());
            
            // Recursively add children to the root
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

    private void importFeatureTree(JSONObject node, IFeatureModel featureModel, IFeatureStructure parent) {
        String featureName = node.getString("name");
        System.out.println("[DEBUG] Importing feature: " + featureName);
        IFeatureModelFactory factory = DefaultFeatureModelFactory.getInstance();
        
        IFeature feature = factory.createFeature(featureModel, featureName);
        featureModel.addFeature(feature);

        if (parent != null) {
            parent.addChild(feature.getStructure());
            System.out.println("[DEBUG] Added feature '" + featureName + "(" + feature.getInternalId() + ")" + "' as a child of '" + parent.getFeature().getName() + "(" + parent.getFeature().getInternalId() + ")" + "'.");
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
