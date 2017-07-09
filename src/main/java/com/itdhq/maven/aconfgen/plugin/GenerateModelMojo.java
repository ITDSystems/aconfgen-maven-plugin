package com.itdhq.maven.aconfgen.plugin;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.*;
import java.util.List;
import java.util.UUID;

@Mojo (name = "gen-model", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GenerateModelMojo extends AbstractAconfgenMojo {

    @Parameter
    ProcessModel[] processModels;

    private ApplicationContext context;

    public void execute() throws MojoExecutionException, MojoFailureException {
        context = new ClassPathXmlApplicationContext("activiti-context.xml");
        RepositoryService repositoryService = (RepositoryService) context.getBean("repositoryService");
        for (ProcessModel model : processModels) {
            Deployment deployment;
            try {
                InputStream resource = new FileInputStream(model.getInput());
                String name = UUID.randomUUID().toString();
                deployment = repositoryService.createDeployment()
                        .addInputStream(name, resource).name(name).deploy();
            } catch (IOException e) {
                getLog().error("Model not found " + model.getInput());
                break;
            }
            getLog().info(deployment.getDeploymentTime().toString());
            getLog().info(String.valueOf(repositoryService.createProcessDefinitionQuery().count()));
            List<ProcessDefinition> definition = repositoryService
                    .createProcessDefinitionQuery().deploymentId(deployment.getId()).list();
            for (ProcessDefinition process : definition) {
                BpmnModel bpmnModel = repositoryService.getBpmnModel(process.getId());
                getLog().info(bpmnModel.getMainProcess().getName());
            }
        }
    }

    @Override
    protected void processCommonModels() {

    }
}