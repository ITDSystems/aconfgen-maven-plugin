package ru.itdsystems.aconfgen.plugin;

import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.repo.dictionary.*;
import org.alfresco.service.namespace.QName;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * Created by Kirill Mayboroda on 30.04.17.
 */
@Mojo(name = "gen-const", defaultPhase = LifecyclePhase.POST_CLEAN, threadSafe = true)
public class GenerateConstantsMojo extends AbstractMojo {

    ApplicationContext context;

    @Parameter
    private String model;

    public void execute() throws MojoExecutionException, MojoFailureException {
        // Could not get context from alfresco jars.
        // Required contexts:
        // org.alfresco.alfresco-data-model-${alfresco.version}.jar/alfresco/data-model-stand-alone-context.xml
        // org.alfresco.alfresco-repository-${alfresco.version}.jar/alfresco/core-services-context.xml
        context = new ClassPathXmlApplicationContext("data-model-context.xml");
        DictionaryDAO dictionaryDAO = (DictionaryDAO) context.getBean("dictionaryDAO");
        NamespaceDAO namespaceDAO = (NamespaceDAO) context.getBean("namespaceDAO");

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
        CompiledModel compiledModel = model.compile(dictionaryDAO, namespaceDAO, true);
        Collection<TypeDefinition> typeDefinitions = compiledModel.getTypes();
        for (TypeDefinition typeDefinition : typeDefinitions) {
            for (QName qName : typeDefinition.getProperties().keySet()) {
                getLog().info(qName.toString());
            }
        }
    }
}

