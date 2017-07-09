package com.itdhq.aconfgen.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.repo.dictionary.M2Model;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class ModelTest extends TestCase {

    public ModelTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ModelTest.class);
    }

    public void testModel() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("data-model-context.xml");
        DictionaryDAO dictionaryDAO = (DictionaryDAO) context.getBean("dictionaryDAO");

        dictionaryDAO.putModel(M2Model.createModel(ModelTest.class.getResourceAsStream("/alvex-arbitrary-task-model.xml")));
    }
}
