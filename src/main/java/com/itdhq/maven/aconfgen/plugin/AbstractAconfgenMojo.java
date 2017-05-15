package com.itdhq.maven.aconfgen.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractAconfgenMojo extends AbstractMojo {

    private String[] DEFAULT_MODELS = new String[]{};

    @Parameter(readonly = true, defaultValue = "${project}")
    protected MavenProject project;

    @Parameter
    private String[] models;

    protected String[] getModels() {
        if (models != null && models.length > 0) {
            return models;
        }
        return DEFAULT_MODELS;
    }

    protected abstract void processCommonModels();
}
