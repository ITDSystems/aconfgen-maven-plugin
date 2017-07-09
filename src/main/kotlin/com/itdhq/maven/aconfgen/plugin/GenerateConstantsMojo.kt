package com.itdhq.maven.aconfgen.plugin.kotlin

import com.itdhq.maven.aconfgen.plugin.freemarker.GetPropertyAllowedValues
import com.itdhq.maven.aconfgen.plugin.freemarker.GetQNamePrefix
import com.itdhq.maven.aconfgen.plugin.freemarker.SnakeUpperCase
import freemarker.template.Configuration
import org.alfresco.repo.dictionary.DictionaryDAO
import org.alfresco.repo.dictionary.M2Model
import org.alfresco.repo.dictionary.NamespaceDAO
import org.alfresco.service.namespace.QName
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.model.fileset.FileSet
import org.apache.maven.shared.model.fileset.util.FileSetManager
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.util.*

class ConstantsItem {

    @Parameter
    lateinit var qualifiedName: String

    @Parameter
    lateinit var models: FileSet
}

@Mojo(name = "generate-constants", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
class GenerateConstantsMojo : AbstractMojo() {

    @Parameter(defaultValue = "\${project.build.directory}/generated-sources/aconfgen/")
    private lateinit var outputDirectory: File

    @Parameter
    private lateinit var constants: List<ConstantsItem>

    @Parameter(defaultValue = "\${project}")
    private val project: MavenProject? = null


    private val configuration = Configuration()

    private lateinit var namespaceDAO: NamespaceDAO
    private lateinit var dictionaryDAO: DictionaryDAO

    override fun execute() {
        val context = ClassPathXmlApplicationContext("data-model-context.xml")
        dictionaryDAO = context.getBean("dictionaryDAO") as DictionaryDAO
        namespaceDAO = context.getBean("namespaceDAO") as NamespaceDAO

        loadFreeMarkerConfig()

        try {
            constants.forEach { generateConstants(it, dictionaryDAO) }
        } catch (e: Throwable) {
            log.error("Failed to generate constants: ${e.message}", e)
        }

        project!!.addCompileSourceRoot(outputDirectory.absolutePath)
    }

    private fun loadFreeMarkerConfig() {
        configuration.setClassForTemplateLoading(this.javaClass, "/templates/")
        configuration.defaultEncoding = "UTF-8"

        configuration.setSharedVariable("get_qname_ns_prefix", GetQNamePrefix(namespaceDAO))
        configuration.setSharedVariable("snake_upper_case", SnakeUpperCase())
        configuration.setSharedVariable("get_prop_allowed_values", GetPropertyAllowedValues())
    }

    private fun generateConstants(item: ConstantsItem, dictionaryDAO: DictionaryDAO) {
        val fileSetManager = FileSetManager()

        val queue = fileSetManager.getIncludedFiles(item.models)
                .map { FileInputStream(File(item.models.directory, it)) }
                .mapTo(LinkedList<M2Model>()) { M2Model.createModel(it) }

        val loadedModels = LinkedList<QName>()

        var counter = 0
        while (queue.isNotEmpty()) {
            if (counter == queue.size)
                throw Exception("Failed to load models. Please check that all dependencies specified.")

            val model = queue.pop()

            try {
                loadedModels.add(dictionaryDAO.putModel(model))
                counter = 0
            } catch (e: Exception) {
                queue.add(model)
                counter++
            }
        }

        val pos = item.qualifiedName.lastIndexOf('.')
        if (pos == -1)
            throw Exception("Qualified name ${item.qualifiedName} is invalid")

        val packageName = item.qualifiedName.substring(0, pos)
        val className = item.qualifiedName.substring(pos + 1)

        var dest = packageName.split('.').fold(outputDirectory, { acc, s -> File(acc, s) })
        dest.mkdirs()

        dest = File(dest, "${className}.java")

        val templateModel = hashMapOf(
                "packageName" to packageName,
                "className" to className,
                "namespaces" to loadedModels.flatMap { dictionaryDAO.getModel(it).namespaces },
                "types" to loadedModels.flatMap(dictionaryDAO::getTypes),
                "properties" to loadedModels.flatMap(dictionaryDAO::getProperties),
                "associations" to loadedModels.flatMap(dictionaryDAO::getAssociations)
        )

        configuration.getTemplate("constants-class.ftl").process(templateModel, FileWriter(dest))
    }

}