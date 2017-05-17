package com.itdhq.maven.aconfgen.plugin;

import org.apache.maven.plugins.annotations.Parameter;

public class ProcessModel {

    @Parameter
    private String input;

    @Parameter
    private String output;

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }
}
