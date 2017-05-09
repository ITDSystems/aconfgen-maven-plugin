package com.itdhq.maven.aconfgen.plugin;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.alfresco.repo.dictionary.*;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.*;
import java.util.*;

@Mojo(name = "gen-const", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GenerateConstantsMojo extends AbstractMojo {

    private ApplicationContext context;

    private String packageName;

    private String className;

    @Parameter
    private String model;

    @Parameter
    private String path;

    @Parameter (defaultValue = "${project.build.directory}/generated-sources/aconfgen/")
    private File outputDirectory;

    private Set<String> getExcludedModels() {
        return new HashSet<String>(Arrays.asList(
                new String[] {
                        NamespaceService.WORKFLOW_MODEL_1_0_URI,
                        NamespaceService.WEBDAV_MODEL_1_0_URI,
                        NamespaceService.SYSTEM_MODEL_1_0_URI,
                        NamespaceService.SECURITY_MODEL_1_0_URI,
                        NamespaceService.LINKS_MODEL_1_0_URI,
                        NamespaceService.EXIF_MODEL_1_0_URI,
                        NamespaceService.FORUMS_MODEL_1_0_URI,
                        NamespaceService.EMAILSERVER_MODEL_URI,
                        NamespaceService.DICTIONARY_MODEL_1_0_URI,
                        NamespaceService.DATALIST_MODEL_1_0_URI,
                        NamespaceService.CONTENT_MODEL_1_0_URI,
                        NamespaceService.BPM_MODEL_1_0_URI,
                        NamespaceService.AUDIO_MODEL_1_0_URI,
                        NamespaceService.APP_MODEL_1_0_URI
                }
        ));
    }

    public void execute() throws MojoExecutionException, MojoFailureException {

        setNames();

        context = new ClassPathXmlApplicationContext("data-model-context.xml");
        DictionaryDAO dictionaryDAO = (DictionaryDAO) context.getBean("dictionaryDAO");
        NamespaceDAO namespaceDAO = (NamespaceDAO) context.getBean("namespaceDAO");

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(model);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        M2Model model = M2Model.createModel(inputStream);
        CompiledModel compiledModel = model.compile(dictionaryDAO, namespaceDAO, true);

        List<TemplateVariable> templateVariables = new ArrayList<TemplateVariable>();

        for (TypeDefinition typeDefinition : compiledModel.getTypes()) {
            for (QName qName : typeDefinition.getProperties().keySet()) {
                if (!getExcludedModels().contains(qName.getNamespaceURI())) {
                    templateVariables.add(createVariable(qName, "PROP"));
                }
            }
            for (AspectDefinition aspectDefinition : typeDefinition.getDefaultAspects()) {
                if (!getExcludedModels().contains(aspectDefinition.getName().getNamespaceURI())) {
                    for (QName qName : aspectDefinition.getProperties().keySet()) {
                        if (!getExcludedModels().contains(qName.getNamespaceURI())) {
                            templateVariables.add(createVariable(qName, "PROP"));
                        }
                    }
                }
            }
        }
        try {
            createConstantsFile(templateVariables);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setNames() {
        int splitIndex = path.lastIndexOf('.');
        packageName = path.substring(0, splitIndex);
        className = path.substring(splitIndex + 1, path.length());
    }

    private TemplateVariable createVariable(QName qName, String prefix) {
        String[] words = qName.getLocalName().split("(?=\\p{Lu})");
        int splitIndex = qName.getPrefixString().indexOf(':');
        String name = prefix + "_" + qName.getPrefixString().substring(0, splitIndex).toUpperCase() + "_";
        for (String word : words) {
            name += word.toUpperCase() + "_";
        }
        name = name.substring(0, name.length() - 1);
        return new TemplateVariable(name, qName.getNamespaceURI(), qName.getLocalName());

    }

    private void createConstantsFile(List<TemplateVariable> templateVariables) throws IOException, TemplateException {
        Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(this.getClass(), "/templates/");
        configuration.setDefaultEncoding("UTF-8");
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("packageName", packageName);
        input.put("name", className);
        input.put("templateVariables", templateVariables);
        packageName = packageName.replace('.', '/');
        outputDirectory = new File(outputDirectory + "/" + packageName);
        outputDirectory.mkdirs();
        Template template = configuration.getTemplate("constants-class.ftl");
        Writer fileWriter = new FileWriter(new File(outputDirectory + "/" + className + ".java"));
        template.process(input, fileWriter);
        fileWriter.close();
    }
}

