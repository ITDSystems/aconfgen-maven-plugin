package com.itdhq.maven.aconfgen.tests.freemarker

import com.itdhq.maven.aconfgen.plugin.freemarker.stringSnakeUpperCase
import io.kotlintest.matchers.shouldEqual
import io.kotlintest.specs.FunSpec


class SnakeUpperCaseTest : FunSpec() {

    init {
        test("Simple tests")
        {
            stringSnakeUpperCase("myProperty") shouldEqual "MY_PROPERTY"
            stringSnakeUpperCase("myComplexProperty") shouldEqual "MY_COMPLEX_PROPERTY"
        }

        test("Uppercase test")
        {
            stringSnakeUpperCase("propertyWithUPPERCASE") shouldEqual "PROPERTY_WITH_UPPERCASE"
        }

        test("Underscore tests")
        {
            stringSnakeUpperCase("my_property") shouldEqual "MY_PROPERTY"
            stringSnakeUpperCase("underscore_with_UPPERCASE") shouldEqual "UNDERSCORE_WITH_UPPERCASE"
        }

        test("Whitespace test")
        {
            stringSnakeUpperCase("some value") shouldEqual "SOME_VALUE"
        }

        test("Numbers test")
        {
            stringSnakeUpperCase("one 2 three") shouldEqual "ONE_2_THREE"
        }

        test("Complex tests")
        {
            stringSnakeUpperCase("space and_underscore") shouldEqual "SPACE_AND_UNDERSCORE"
            stringSnakeUpperCase("space and_underscore one 2_three") shouldEqual "SPACE_AND_UNDERSCORE_ONE_2_THREE"
        }
    }
}