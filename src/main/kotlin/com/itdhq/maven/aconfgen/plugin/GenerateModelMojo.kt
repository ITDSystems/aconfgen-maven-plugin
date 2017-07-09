package com.itdhq.maven.aconfgen.plugin

import org.activiti.engine.ProcessEngineConfiguration
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration
import org.activiti.engine.repository.ProcessDefinition
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter

class ProcessItem
{
    @Parameter
    lateinit var source: File

    @Parameter
    lateinit var output: File

}


@Mojo(name = "generate-model", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
class GenerateModelMojo: AbstractMojo() {

    @Parameter
    private lateinit var processes: List<ProcessItem>

    override fun execute() {
        val processEngine = StandaloneProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:activiti;DB_CLOSE_DELAY=1000")
                .setJdbcUsername("sa")
                .setJdbcPassword("")
                .setJdbcDriver("org.h2.Driver")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .buildProcessEngine()

        val repositoryService = processEngine.repositoryService

        processes.forEach {
            val deployment = repositoryService.createDeployment()
                    .addInputStream(it.source.name, FileInputStream(it.source))
                    .deploy()
            val processDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.id)
                    .singleResult()
            generateModel(processDefinition, it.output)
        }
    }

    fun generateModel(processDefinition: ProcessDefinition, output: File)
    {
        // FIXME: not implemented
        output.parentFile.mkdirs()
        val writer = FileWriter(output)
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><model name=\"acme:contentModel\" xmlns=\"http://www.alfresco.org/model/dictionary/1.0\"></model>")
        writer.close()
    }
}