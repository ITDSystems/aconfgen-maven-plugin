package com.itdhq.maven.aconfgen.plugin;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.alfresco.repo.dictionary.*;
import org.alfresco.service.cmr.dictionary.*;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
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
public class GenerateConstantsMojo extends AbstractAconfgenMojo {

    private final String PROPERTY = "PROP";

    private final String ASSOCIATION = "ASSOC";

    private ApplicationContext context;

    private String packageName;

    private String className;

    private DictionaryDAO dictionaryDAO;

    private NamespaceDAO namespaceDAO;

    private Map<String, String> uriCache = new HashMap<String, String>();

    private Map<QName, Collection<String>> constraintsCache = new HashMap<QName, Collection<String>>();

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/aconfgen/")
    private File outputDirectory;

    @Parameter
    private Constants[] constants;

    private Set<String> excludedModels = new HashSet<String>(Arrays.asList(
            new String[]{
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

    public void execute() throws MojoExecutionException, MojoFailureException {

        context = new ClassPathXmlApplicationContext("data-model-context.xml");
        dictionaryDAO = (DictionaryDAO) context.getBean("dictionaryDAO");
        namespaceDAO = (NamespaceDAO) context.getBean("namespaceDAO");

        processCommonModels();

        for (Constants constant : constants) {

            setNames(constant);

            Set<NameParamsConstant> qNameConstants = new HashSet<NameParamsConstant>();
            Set<NameValueConstant> stringConstants = new HashSet<NameValueConstant>();

            for (String path : constant.getModels()) {

                CompiledModel compiledModel;
                try {
                   compiledModel = getCompiledModel(path);
                }
                catch (FileNotFoundException e) {
                    getLog().error("Model " + path + " could not be read");
                    break;
                }

                QName modelName = compiledModel.getModelDefinition().getName();
                for (NamespaceDefinition namespaceDefinition : compiledModel.getModelDefinition().getNamespaces()) {
                    stringConstants.add(createUri(modelName, namespaceDefinition.getUri()));
                }

                for (ConstraintDefinition constraintDefinition : compiledModel.getConstraints()) {
                    Constraint constraint = constraintDefinition.getConstraint();
                    QName constraintQName = QName.createQName( constraintDefinition.getName().getNamespaceURI(),
                            constraint.getShortName());
                    if (!constraintsCache.containsKey(constraintQName)) {
                        qNameConstants.add(createQNameConstant(constraintQName, "CONSTRAINT"));
                        if (constraint.getType().equals("LIST")) {
                            Collection<String> values = (Collection<String>) constraint.getParameters()
                                    .get("allowedValues");
                            constraintsCache.put(constraintQName, values);
                        }
                    }
                }


                for (TypeDefinition typeDefinition : compiledModel.getTypes()) {
                    for (QName qName : typeDefinition.getProperties().keySet()) {
                        if (checkIfAllowed(qName)) {
                            qNameConstants.add(createQNameConstant(qName, PROPERTY));
                        }
                    }

                    for (QName qName : typeDefinition.getAssociations().keySet()) {
                        if (checkIfAllowed(qName)) {
                            qNameConstants.add(createQNameConstant(qName, ASSOCIATION));
                        }
                    }
                    for (AspectDefinition aspectDefinition : typeDefinition.getDefaultAspects()) {
                        if (checkIfAllowed(aspectDefinition.getName())) {
                            for (QName qName : aspectDefinition.getProperties().keySet()) {
                                if (checkIfAllowed(qName)) {
                                    qNameConstants.add(createQNameConstant(qName, PROPERTY));
                                }
                            }
                            for (QName qName : aspectDefinition.getAssociations().keySet()) {
                                if (checkIfAllowed(qName)) {
                                    qNameConstants.add(createQNameConstant(qName, ASSOCIATION));
                                }
                            }
                        }
                    }
                }
                dictionaryDAO.putModel(compiledModel.getM2Model());
            }
            try {
                generateConstants(qNameConstants, stringConstants);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private CompiledModel getCompiledModel(String path) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(path);
        M2Model model = M2Model.createModel(inputStream);
        return model.compile(dictionaryDAO, namespaceDAO, true);
    }

    private boolean checkIfAllowed(QName qName) {
        return !excludedModels.contains(qName.getNamespaceURI());
    }

    private void setNames(Constants constant) {
        String path = constant.getPath();
        if (path != null) {
            if (path.contains(".java")) {
                path.replace(".java", "");
            }
            int splitIndex = constant.getPath().lastIndexOf('.');
            packageName = path.substring(0, splitIndex);
            className = path.substring(splitIndex + 1, path.length());
        }
    }

    private NameValueConstant createUri(QName qName, String uri) {
        String name = qName.getPrefixString().substring(0, qName.getPrefixString().indexOf(':'))
                .toUpperCase() + "_URI";
        uriCache.put(uri, name);
        return new NameValueConstant(name, uri);
    }

    private NameParamsConstant createQNameConstant(QName qName, String prefix) {
        String prefixString = qName.getPrefixString();
        int splitIndex = prefixString.indexOf(':');
        String shortName = prefixString.substring(splitIndex + 1, prefixString.length());
        String[] words = shortName.split("(?=\\p{Lu})");
        StringBuilder name = new StringBuilder(prefix + "_" + prefixString.substring(0, splitIndex)
                .toUpperCase() + "_");
        for (String word : words) {
            name.append(word.toUpperCase()).append("_");
        }
        name = new StringBuilder(name.substring(0, name.length() - 1));
        String uri;
        if (uriCache.containsKey(qName.getNamespaceURI())) {
            uri = uriCache.get(qName.getNamespaceURI());
        }
        else
            uri = "\"" + qName.getNamespaceURI() + "\"";
        return new NameParamsConstant(name.toString(), uri, qName.getLocalName());


    }

    private void generateConstants(Set<NameParamsConstant> nameParamsConstants,
                                   Set<NameValueConstant> nameValueConstantSet) throws IOException, TemplateException {
        Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(this.getClass(), "/templates/");
        configuration.setDefaultEncoding("UTF-8");
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("packageName", packageName);
        input.put("name", className);
        input.put("properties", nameParamsConstants);
        input.put("uris", nameValueConstantSet);
        packageName = packageName.replace('.', '/');
        outputDirectory = new File(outputDirectory + "/" + packageName);
        outputDirectory.mkdirs();
        Template template = configuration.getTemplate("constants-class.ftl");
        Writer fileWriter = new FileWriter(new File(outputDirectory + "/" + className + ".java"));
        template.process(input, fileWriter);
        fileWriter.close();
    }

    @Override
    protected void processCommonModels() {
        String[] models = getModels();
        if (models != null) {
            for (String model : models) {
                CompiledModel compiledModel;
                try {
                    compiledModel = getCompiledModel(model);
                }
                catch (FileNotFoundException e) {
                    getLog().error("Model " + model + " could not be read");
                    break;
                }
                for (ConstraintDefinition constraintDefinition : compiledModel.getConstraints()) {
                    Constraint constraint = constraintDefinition.getConstraint();
                    if (constraint.getType().equals("LIST")) {
                        Collection<String> values = (Collection<String>) constraint.getParameters()
                                .get("allowedValues");
                        constraintsCache.put(constraintDefinition.getName(), values);
                    }
                }
                dictionaryDAO.putModel(compiledModel.getM2Model());
            }
        }
    }
}

