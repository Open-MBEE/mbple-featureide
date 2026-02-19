package com.example.featureide.importer;

import org.eclipse.ui.plugin.AbstractUIPlugin;

public class Activator extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "com.example.featureide.importer";
    private static Activator plugin;

    public Activator() {
        plugin = this;
    }

    public static Activator getDefault() {
        return plugin;
    }
}
