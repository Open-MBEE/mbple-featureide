# mbple-featureide
Extension of FeatureIDE for SysML v2.

## Quick Start

1. Run as **Eclipse Application**
2. Press **CTRL+ALT+I** to open the plugin dialog.
3. On first use, configure the servers in the **"Configure SysML v2 Repository"** dialog (Step 1).

## Configuration

All settings are stored in `config.properties` and persist across sessions.

| Key | Description | Default |
|---|---|---|
| `sysml_api.flask_url` | URL of the Flask middleware server | `http://127.0.0.1:5000` |
| `sysml_api.repo_url` | URL of the SysML v2 API repository | `http://sysml2.intercax.com:9000` |
| `sysml_api.page_size` | Elements fetched per page | `512` |
| `sysml_api.project_id` | UUID of the selected SysML v2 project | *(empty)* |
| `sysml_api.commit_id` | UUID of the selected commit | *(empty)* |

The bundled `config.properties` file (in the plugin root) holds the default values.  
User-saved values are written to the bundle data area or `~/.mbplefeatureide/config.properties` as a fallback.

## Configuration Dialog

Open via **CTRL+ALT+I** → the dialog guides you through three steps:

1. **Step 1 – Server Configuration:** Enter the Flask server URL and SysML v2 repository URL, then click **Get Projects**.
2. **Step 2 – Select Project:** Pick a project from the list (or paste a UUID directly into the *Project ID* field).
3. **Step 3 – Select Commit:** Pick a commit (or paste a UUID into the *Commit ID* field).

The **OK** button activates once both IDs are set (either by selection or from a saved configuration).  
Clicking **OK** saves all values to `config.properties` for the next session.

## Dependencies

- Java 21
- Eclipse PDE (Plug-in Development Environment)
- `json-20210307.jar` (bundled under `lib/`)
- Flask middleware server for the SysML v2 API bridge
