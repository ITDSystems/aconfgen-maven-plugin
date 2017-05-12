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

    private final String CONSTRAINT = "CONSTRAINT";

    private ApplicationContext context;

    private String packageName;

    private String className;

    private DictionaryDAO dictionaryDAO;

    private NamespaceDAO namespaceDAO;

    private Map<String, String> uriCache = new HashMap<String, String>();

    private Map<QName, Collection<String>> constraintsCache = new HashMap<QName, Collection<String>>();

    private Set<NameParamsConstant> qNameConstants;

    private Set<NameValueConstant> stringConstants;

    private Map<String, String> uriResolver = new HashMap<String, String>();

    private Set<QName> dependencyConstraints = new HashSet<QName>();

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

            qNameConstants = new HashSet<NameParamsConstant>();
            stringConstants = new HashSet<NameValueConstant>();

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
                uriResolver.put(getPrefix(modelName.getPrefixString()), modelName.getNamespaceURI());

                for (NamespaceDefinition namespaceDefinition : compiledModel.getModelDefinition().getNamespaces()) {
                    stringConstants.add(createUri(modelName, namespaceDefinition.getUri()));
                    stringConstants.add(createPrefix(modelName));
                }

                for (ConstraintDefinition constraintDefinition : compiledModel.getConstraints()) {
                    Constraint constraint = constraintDefinition.getConstraint();
                    String prefix = getPrefix(constraint.getShortName());
                    String localName = getLocalName(constraint.getShortName());
                    String uri = uriResolver.get(getPrefix(constraint.getShortName()));
                    QName constraintQName = QName.createQName(uri, localName);
                    if (!constraintsCache.containsKey(constraintQName) || dependencyConstraints.contains(constraintQName)) {
                        qNameConstants.add(createQNameConstant(uri, prefix, localName, CONSTRAINT));
                        if (constraint.getType().equals("LIST")) {
                            Collection<String> values = (Collection<String>) constraint.getParameters()
                                    .get("allowedValues");
                            constraintsCache.put(constraintQName, values);
                        }
                    }

                }


                for (TypeDefinition typeDefinition : compiledModel.getTypes()) {
                    processProperties(typeDefinition.getProperties());

                    for (QName qName : typeDefinition.getAssociations().keySet()) {
                        if (checkIfAllowed(qName)) {
                            qNameConstants.add(createQNameConstant(qName, ASSOCIATION));
                        }
                    }
                    for (AspectDefinition aspectDefinition : typeDefinition.getDefaultAspects()) {
                        if (checkIfAllowed(aspectDefinition.getName())) {
                            processProperties(aspectDefinition.getProperties());

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

    private void processProperties(Map<QName, PropertyDefinition> properties) {
        for (QName qName : properties.keySet()) {
            if (checkIfAllowed(qName)) {
                qNameConstants.add(createQNameConstant(qName, PROPERTY));
                PropertyDefinition property = properties.get(qName);
                for (ConstraintDefinition constraintDefinition : property.getConstraints()) {
                    Constraint constraint = constraintDefinition.getConstraint();
                    String uri = uriResolver.get(getPrefix(constraint.getShortName()));
                    QName constraintQName = QName.createQName(uri, getLocalName(constraint.getShortName()));
                    if (constraintsCache.containsKey(constraintQName)) {
                        for (String value : constraintsCache.get(constraintQName)) {
                            stringConstants.add(createPropertyValues(qName, value));
                        }
                    }
                }
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
        String name = getPrefix(qName.getPrefixString()).toUpperCase() + "_URI";
        uriCache.put(uri, name);
        return new NameValueConstant(name, uri);
    }

    private NameValueConstant createPrefix(QName qName) {
        String name = getPrefix(qName.getPrefixString());
        return new NameValueConstant(name.toUpperCase() + "_PREFIX", name);
    }

    private NameValueConstant createPropertyValues(QName qName, String value) {
        String name = createName(qName, PROPERTY);
        String postfix = value.replace(' ', '_').replace('-', '_');
        if (value.equals("")) {
            name += "_EMPTY";
        }
        else {
            name += "_" + postfix.toUpperCase();
        }
        return new NameValueConstant(name, value);
    }

    private String getPrefix(String line) {
        if (line.contains(":")) {
            return line.substring(0, line.indexOf(':'));
        }
        return line;
    }

    private String getLocalName(String line) {
        if (line.contains(":")) {
            return line.substring(line.indexOf(':') + 1);
        }
        return line;
    }

    private NameParamsConstant createQNameConstant(QName qName, String prefix) {
        String name = createName(qName, prefix);
        String uri;
        if (uriCache.containsKey(qName.getNamespaceURI())) {
            uri = uriCache.get(qName.getNamespaceURI());
        }
        else
            uri = "\"" + qName.getNamespaceURI() + "\"";
        String localName = qName.getLocalName();
        if (localName.contains(":")) {
            localName = getLocalName(localName);
        }
        return new NameParamsConstant(name, uri, localName);
    }

    private NameParamsConstant createQNameConstant(String uri, String prefix, String localName, String type) {
        StringBuilder name = new StringBuilder(type + "_" + prefix.toUpperCase() + "_");
        String[] words = localName.split("(?=\\p{Lu})");
        for (String word : words) {
            name.append(word.toUpperCase()).append("_");
        }
        String varName = name.substring(0, name.length() - 1);
        if (uriCache.containsKey(uri)) {
            uri = uriCache.get(uri);
        }
        else {
            uri = "\"" + uri + "\"";
        }
        return new NameParamsConstant(varName, uri, localName);
    }

    private String createName(QName qName, String prefix) {
        String prefixString = qName.getPrefixString();
        String shortName = getLocalName(prefixString);
        String[] words = shortName.split("(?=\\p{Lu})");
        StringBuilder name = new StringBuilder(prefix + "_" + getPrefix(prefixString)
                .toUpperCase() + "_");
        for (String word : words) {
            name.append(word.toUpperCase()).append("_");
        }
        return name.substring(0, name.length() - 1);
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
                        uriResolver.put(getPrefix(constraintDefinition.getName().getPrefixString()),
                                constraintDefinition.getName().getNamespaceURI());
                        QName constraintQName = QName.createQName(constraintDefinition.getName().getNamespaceURI(),
                                getLocalName(constraint.getShortName()));
                        if (!dependencyConstraints.contains(constraintQName)) {
                            dependencyConstraints.add(constraintQName);
                        }
                        constraintsCache.put(constraintQName, values);
                    }
                }
                dictionaryDAO.putModel(compiledModel.getM2Model());
            }
        }
    }
}

