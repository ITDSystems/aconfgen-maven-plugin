package ru.itdsystems.aconfgen.plugin;

import org.alfresco.repo.dictionary.M2Namespace;
import org.alfresco.repo.dictionary.M2Property;
import org.alfresco.repo.dictionary.M2Type;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.alfresco.repo.dictionary.M2Model;

import java.io.*;
import java.util.List;

/**
 * Created by Kirill Mayboroda on 30.04.17.
 */
@Mojo(name = "gen-const", defaultPhase = LifecyclePhase.POST_CLEAN, threadSafe = true)
public class GenerateConstantsMojo extends AbstractMojo {

    @Parameter
    private String model;

    public void execute() throws MojoExecutionException, MojoFailureException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(model);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        M2Model model = M2Model.createModel(inputStream);
        List<M2Type> types = model.getTypes();
        for (M2Type type : types) {
            for (M2Property property : type.getProperties()) {
                getLog().info(model.getNamespaces().get(0).getUri() + " " + property.getName());
            }
        }
    }
}

