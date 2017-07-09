package com.itdhq.maven.aconfgen.plugin.freemarker

import freemarker.ext.beans.StringModel
import freemarker.template.TemplateMethodModelEx
import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint
import org.alfresco.service.cmr.dictionary.ConstraintDefinition
import org.alfresco.service.cmr.dictionary.PropertyDefinition

class GetPropertyAllowedValues : TemplateMethodModelEx {

    override fun exec(arguments: MutableList<Any?>?): List<String> {
        val propDef = (arguments!!.first() as StringModel).wrappedObject as PropertyDefinition
        val lvs = propDef.constraints.map(ConstraintDefinition::getConstraint).filterIsInstance<ListOfValuesConstraint>()

        return when (lvs.size) {
            0 -> emptyList<String>()
            1 -> lvs.first().allowedValues
            else -> throw Exception("Property ${propDef.name.prefixString} has more that one list-of-values constraint")
        }
    }

}