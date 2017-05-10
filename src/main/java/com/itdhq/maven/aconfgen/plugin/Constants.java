package com.itdhq.maven.aconfgen.plugin;

import org.apache.maven.plugins.annotations.Parameter;

public class Constants {

    @Parameter
    private String path;

    @Parameter
    private String[] models;

    public String getPath() {
        return path;
    }

    public String[] getModels() {
        return models;
    }
}
