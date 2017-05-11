package com.itdhq.maven.aconfgen.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractAconfgenMojo extends AbstractMojo {

    @Parameter
    private String[] models;

    protected String[] getModels() {
        if (models != null && models.length > 0) {
            return models;
        }
        return null;
    }

    protected abstract void processCommonModels();
}
