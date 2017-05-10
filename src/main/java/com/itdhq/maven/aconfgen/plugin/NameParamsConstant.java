package com.itdhq.maven.aconfgen.plugin;

public class NameParamsConstant {

    private String URI;
    private String name;
    private String localName;

    public NameParamsConstant(String name, String URI, String localName) {
        this.localName = localName;
        this.name = name;
        this.URI = URI;
    }

    public String getURI() {
        return URI;
    }

    public String getName() {
        return name;
    }

    public String getLocalName() {
        return localName;
    }
}
