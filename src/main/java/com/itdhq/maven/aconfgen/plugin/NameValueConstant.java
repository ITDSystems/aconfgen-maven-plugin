package com.itdhq.maven.aconfgen.plugin;

public class NameValueConstant {

    private String name;
    private String value;

    public NameValueConstant(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
