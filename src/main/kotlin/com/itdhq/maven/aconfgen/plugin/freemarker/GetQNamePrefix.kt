package com.itdhq.maven.aconfgen.plugin.freemarker

import freemarker.ext.beans.StringModel
import freemarker.template.TemplateMethodModelEx
import org.alfresco.repo.dictionary.NamespaceDAO
import org.alfresco.service.namespace.QName

class GetQNamePrefix(val namespaceDAO: NamespaceDAO) : TemplateMethodModelEx {

    override fun exec(arguments: MutableList<Any?>?): String =
            namespaceDAO.getPrefixes(((arguments!!.first() as StringModel).wrappedObject as QName).namespaceURI).first()

}