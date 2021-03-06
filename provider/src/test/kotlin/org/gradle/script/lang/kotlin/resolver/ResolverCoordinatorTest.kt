package org.gradle.script.lang.kotlin.resolver

import org.gradle.script.lang.kotlin.fixtures.assertInstanceOf
import org.gradle.script.lang.kotlin.fixtures.withInstanceOf

import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies
import org.jetbrains.kotlin.script.ScriptContents

import org.junit.Test

import java.io.File

class ResolverCoordinatorTest {

    @Test
    fun `given an environment with a 'getScriptSectionTokens' entry, when no buildscript change, it will not try to retrieve the model`() {

        val environment =
            environmentWithGetScriptSectionTokensReturning(
                "buildscript" to sequenceOf(""),
                "plugins" to sequenceOf(""))

        val action1 = resolverActionFor(environment, null)
        withInstanceOf<ResolverAction.RequestNew>(action1) {
            val action2 = resolverActionFor(environment, scriptDependencies())
            assertInstanceOf<ResolverAction.ReturnPrevious>(action2)
        }
    }

    @Test
    fun `given an environment with a 'getScriptSectionTokens' entry, when buildscript changes, it will try to retrieve the model again`() {

        val env1 = environmentWithGetScriptSectionTokensReturning("buildscript" to sequenceOf("foo"))
        val env2 = environmentWithGetScriptSectionTokensReturning("buildscript" to sequenceOf("bar"))

        val action1 = resolverActionFor(env1, null)
        withInstanceOf<ResolverAction.RequestNew>(action1) {
            val action2 = resolverActionFor(env2, scriptDependencies())
            assertInstanceOf<ResolverAction.RequestNew>(action2)
        }
    }

    @Test
    fun `given an environment with a 'getScriptSectionTokens' entry, when plugins block changes, it will try to retrieve the model again`() {

        val env1 = environmentWithGetScriptSectionTokensReturning("plugins" to sequenceOf("foo"))
        val env2 = environmentWithGetScriptSectionTokensReturning("plugins" to sequenceOf("bar"))

        val action1 = resolverActionFor(env1, null)
        withInstanceOf<ResolverAction.RequestNew>(action1) {
            val action2 = resolverActionFor(env2, scriptDependencies())
            assertInstanceOf<ResolverAction.RequestNew>(action2)
        }
    }

    @Test
    fun `given an environment lacking a 'getScriptSectionTokens' entry, it will always try to retrieve the model`() {

        val environment = emptyMap<String, Any?>()
        val action1 = resolverActionFor(environment, null)
        withInstanceOf<ResolverAction.RequestNew>(action1) {
            val action2 = resolverActionFor(environment, scriptDependencies())
            assertInstanceOf<ResolverAction.RequestNew>(action2)
        }
    }

    private
    fun resolverActionFor(environment: Map<String, Any?>, previousDependencies: KotlinScriptExternalDependencies?) =
        ResolverCoordinator.selectNextActionFor(EmptyScriptContents, environment, previousDependencies)

    private
    fun ResolverAction.RequestNew.scriptDependencies() =
        KotlinBuildScriptDependencies(emptyList(), emptyList(), emptyList(), buildscriptBlockHash)

    private
    fun environmentWithGetScriptSectionTokensReturning(vararg sections: Pair<String, Sequence<String>>) =
        environmentWithGetScriptSectionTokens { _, section -> sections.find { it.first == section }?.second ?: emptySequence() }

    private
    fun environmentWithGetScriptSectionTokens(function: (CharSequence, String) -> Sequence<String>) =
        mapOf<String, Any?>("getScriptSectionTokens" to function)
}

private
object EmptyScriptContents : ScriptContents {
    override val file: File? = null
    override val text: CharSequence? = ""
    override val annotations: Iterable<Annotation> = emptyList()
}
