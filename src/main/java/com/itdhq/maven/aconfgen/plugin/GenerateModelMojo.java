package com.itdhq.maven.aconfgen.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;

@Mojo (name = "gen-model", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GenerateModelMojo extends AbstractAconfgenMojo {

    @Parameter
    ProcessModel[] processModels;

    public void execute() throws MojoExecutionException, MojoFailureException {
        for (ProcessModel model : processModels) {
            File modelPath;
            try {
                modelPath = getFile(model.getInput());
            }
            catch (FileNotFoundException e) {
                getLog().error("File could not be found " + model.getInput());
                break;
            }
            if (isValid(modelPath)) {

            }
        }
    }

    private File getFile(String path) throws FileNotFoundException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException("Could not find process file " + path);
        }
        return file;
    }

    private boolean isValid(File model) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema xsd = schemaFactory.newSchema(getClass().getResource("/schemas/BPMN20.xsd"));
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder parser = documentBuilderFactory.newDocumentBuilder();
            Document xml = parser.parse(model);
            Validator validator = xsd.newValidator();
            validator.validate(new DOMSource(xml));
        }
        catch (SAXException e) {
            getLog().error("Model is not valid " + model.getAbsolutePath());
            e.printStackTrace();
            return false;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void processCommonModels() {

    }
}
