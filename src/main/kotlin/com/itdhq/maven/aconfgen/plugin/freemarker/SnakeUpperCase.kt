package com.itdhq.maven.aconfgen.plugin.freemarker

import freemarker.template.TemplateMethodModelEx

// Original idea: http://www.programcreek.com/2011/03/java-method-for-spliting-a-camelcase-string/
fun stringSnakeUpperCase(s: String): String =
        s.split("[\\W_]".toRegex())
                .flatMap { w -> w.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])".toRegex()) }
                .joinToString(separator = "_", transform = String::toUpperCase)

class SnakeUpperCase : TemplateMethodModelEx {
    override fun exec(arguments: MutableList<Any?>?): String =
            stringSnakeUpperCase(arguments!!.first().toString())
}