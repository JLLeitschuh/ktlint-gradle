package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import java.io.File
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findParameterByName
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * An abstraction for invoking ktlint across all breaking changes between versions
 */
internal sealed interface KtLintInvocation {
    fun invokeLint(file: File, cb: (LintError, Boolean) -> Unit)
    fun invokeFormat(file: File, cb: (LintError, Boolean) -> Unit): String
}

sealed interface KtLintInvocationFactory

/**
 * Implementation for invoking ktlint prior to 0.46.0
 * Does not use reflection because the API is the same as the version of ktlint this project is compiled against
 */
internal class LegacyParamsInvocation(
    private val editorConfigPath: String?,
    private val ruleSets: Set<com.pinterest.ktlint.core.RuleSet>,
    private val userData: Map<String, String>,
    private val debug: Boolean
) : KtLintInvocation {
    companion object Factory : KtLintInvocationFactory {
        fun initialize(
            editorConfigPath: String?,
            ruleSets: Set<com.pinterest.ktlint.core.RuleSet>,
            userData: Map<String, String>,
            debug: Boolean
        ): KtLintInvocation = LegacyParamsInvocation(
            editorConfigPath = editorConfigPath,
            ruleSets = ruleSets,
            userData = userData,
            debug = debug
        )
    }

    private fun buildParams(file: File, cb: (LintError, Boolean) -> Unit): com.pinterest.ktlint.core.KtLint.Params {
        val script = !file.name.endsWith(".kt", ignoreCase = true)
        return com.pinterest.ktlint.core.KtLint.Params(
            fileName = file.absolutePath,
            text = file.readText(),
            ruleSets = ruleSets,
            userData = userData,
            debug = debug,
            editorConfigPath = editorConfigPath,
            script = script,
            cb = cb
        )
    }

    override fun invokeLint(file: File, cb: (LintError, Boolean) -> Unit) {
        com.pinterest.ktlint.core.KtLint.lint(buildParams(file, cb))
    }

    override fun invokeFormat(file: File, cb: (LintError, Boolean) -> Unit): String {
        return com.pinterest.ktlint.core.KtLint.format(buildParams(file, cb))
    }
}

/**
 * Implementation for invoking ktlint 0.46.x
 */
@OptIn(FeatureInAlphaState::class)
internal class ExperimentalParamsInvocation(
    private val editorConfigPath: String?,
    private val ruleSets: Set<com.pinterest.ktlint.core.RuleSet>,
    private val userData: Map<String, String>,
    private val debug: Boolean
) : KtLintInvocation {
    companion object Factory : KtLintInvocationFactory {
        fun initialize(
            editorConfigPath: String?,
            ruleSets: Set<com.pinterest.ktlint.core.RuleSet>,
            userData: Map<String, String>,
            debug: Boolean
        ): KtLintInvocation = ExperimentalParamsInvocation(
            editorConfigPath = editorConfigPath,
            ruleSets = ruleSets,
            userData = userData,
            debug = debug
        )
    }

    private val ctor: KFunction<*> by lazy {
        experimentalParamsClass?.kotlin?.primaryConstructor!!
    }
    private val fileNameParam: KParameter by lazy { ctor.findParameterByName("fileName")!! }
    private val textParam: KParameter by lazy { ctor.findParameterByName("text")!! }
    private val ruleSetsParam: KParameter by lazy { ctor.findParameterByName("ruleSets")!! }
    private val cbParam: KParameter by lazy { ctor.findParameterByName("cb")!! }
    private val scriptParam: KParameter by lazy { ctor.findParameterByName("script")!! }
    private val editorConfigPathParam: KParameter by lazy { ctor.findParameterByName("editorConfigPath")!! }
    private val debugParam: KParameter by lazy { ctor.findParameterByName("debug")!! }
    private val editorConfigOverrideParam: KParameter by lazy { ctor.findParameterByName("editorConfigOverride")!! }
    private val editorConfigOverride: Any by lazy { userDataToEditorConfigOverride(userData) }

    private fun buildParams(
        file: File,
        cb: (LintError, Boolean) -> Unit
    ): com.pinterest.ktlint.core.KtLint.ExperimentalParams {
        val script = !file.name.endsWith(".kt", ignoreCase = true)
        return ctor.callBy(
            mapOf(
                fileNameParam to file.absolutePath,
                textParam to file.readText(),
                ruleSetsParam to ruleSets,
                cbParam to cb,
                scriptParam to script,
                editorConfigPathParam to editorConfigPath,
                debugParam to debug,
                editorConfigOverrideParam to editorConfigOverride
            )
        ) as com.pinterest.ktlint.core.KtLint.ExperimentalParams
    }

    override fun invokeLint(file: File, cb: (LintError, Boolean) -> Unit) {
        com.pinterest.ktlint.core.KtLint.lint(buildParams(file, cb))
    }

    override fun invokeFormat(file: File, cb: (LintError, Boolean) -> Unit): String {
        return com.pinterest.ktlint.core.KtLint.format(buildParams(file, cb))
    }
}

private fun getCodeStyle(styleName: String): Any {
    return try {
        Class.forName("com.pinterest.ktlint.core.api.DefaultEditorConfigProperties\$CodeStyleValue")
            .getDeclaredField(styleName).get(null)
    } catch (e: ClassNotFoundException) {
        (Class.forName("com.pinterest.ktlint.core.api.editorconfig.CodeStyleValue").enumConstants as Array<Enum<*>>).first {
            it.name == styleName
        }
    }
}

private fun getEditorConfigPropertyClass(): Class<*> {
    return try {
        Class.forName("com.pinterest.ktlint.core.api.UsesEditorConfigProperties\$EditorConfigProperty")
    } catch (e: ClassNotFoundException) {
        Class.forName("com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty")
    }
}

@Suppress("UnnecessaryOptInAnnotation")
@OptIn(FeatureInAlphaState::class)
private fun userDataToEditorConfigOverride(userData: Map<String, String>): Any {
    val defaultEditorConfigPropertiesClass =
        Class.forName("com.pinterest.ktlint.core.api.DefaultEditorConfigProperties")
    val defaultEditorConfigProperties = defaultEditorConfigPropertiesClass.kotlin.objectInstance
    val codeStyle = getCodeStyle(if (userData["android"]?.toBoolean() == true) "android" else "official")
    val editorConfigOverrideClass = Class.forName("com.pinterest.ktlint.core.api.EditorConfigOverride")
    val editorConfigOverride = editorConfigOverrideClass.kotlin.primaryConstructor!!.call()
    val addMethod = editorConfigOverrideClass.getDeclaredMethod("add", getEditorConfigPropertyClass(), Any::class.java).apply {
        isAccessible = true
    }
    val codeStyleSetProperty = defaultEditorConfigPropertiesClass.kotlin.memberProperties.first { it.name == "codeStyleSetProperty" }
    if (!userData["disabled_rules"].isNullOrBlank()) {
        val disabledRulesProperty =
            defaultEditorConfigPropertiesClass.kotlin.memberProperties.firstOrNull { it.name == "ktlintDisabledRulesProperty" }
                ?: defaultEditorConfigPropertiesClass.kotlin.memberProperties.first { it.name == "disabledRulesProperty" }
        addMethod.invoke(
            editorConfigOverride,
            disabledRulesProperty.getter.call(defaultEditorConfigProperties),
            userData["disabled_rules"]
        )
    }
    addMethod.invoke(editorConfigOverride, codeStyleSetProperty.getter.call(defaultEditorConfigProperties), codeStyle)
    return editorConfigOverride
}

/**
 * Implementation for invoking ktlint 0.47.x
 */
@OptIn(FeatureInAlphaState::class)
internal class ExperimentalParamsProviderInvocation(
    private val editorConfigPath: String?,
    private val ruleProviders: Set<Any>,
    private val userData: Map<String, String>,
    private val debug: Boolean
) : KtLintInvocation {
    companion object Factory : KtLintInvocationFactory {
        fun initialize(
            editorConfigPath: String?,
            ruleProviders: Set<Any>,
            userData: Map<String, String>,
            debug: Boolean
        ): ExperimentalParamsProviderInvocation =
            ExperimentalParamsProviderInvocation(editorConfigPath, ruleProviders, userData, debug)
    }

    private val ctor: KFunction<*> by lazy {
        experimentalParamsClass?.kotlin?.primaryConstructor!!
    }
    private val fileNameParam: KParameter by lazy { ctor.findParameterByName("fileName")!! }
    private val textParam: KParameter by lazy { ctor.findParameterByName("text")!! }
    private val ruleProvidersParam: KParameter by lazy { ctor.findParameterByName("ruleProviders")!! }
    private val cbParam: KParameter by lazy { ctor.findParameterByName("cb")!! }
    private val scriptParam: KParameter by lazy { ctor.findParameterByName("script")!! }
    private val editorConfigPathParam: KParameter by lazy { ctor.findParameterByName("editorConfigPath")!! }
    private val debugParam: KParameter by lazy { ctor.findParameterByName("debug")!! }
    private val editorConfigOverrideParam: KParameter by lazy { ctor.findParameterByName("editorConfigOverride")!! }
    private val editorConfigOverride: Any by lazy { userDataToEditorConfigOverride(userData) }

    private fun buildParams(
        file: File,
        cb: (LintError, Boolean) -> Unit
    ): com.pinterest.ktlint.core.KtLint.ExperimentalParams {
        val script = !file.name.endsWith(".kt", ignoreCase = true)
        return ctor.callBy(
            mapOf(
                fileNameParam to file.absolutePath,
                textParam to file.readText(),
                ruleProvidersParam to ruleProviders,
                cbParam to cb,
                scriptParam to script,
                editorConfigPathParam to editorConfigPath,
                debugParam to debug,
                editorConfigOverrideParam to editorConfigOverride
            )
        ) as com.pinterest.ktlint.core.KtLint.ExperimentalParams
    }

    override fun invokeLint(file: File, cb: (LintError, Boolean) -> Unit) {
        com.pinterest.ktlint.core.KtLint.lint(buildParams(file, cb))
    }

    override fun invokeFormat(file: File, cb: (LintError, Boolean) -> Unit): String {
        return com.pinterest.ktlint.core.KtLint.format(buildParams(file, cb))
    }
}

/**
 * Implementation for invoking ktlint >= 0.48
 * This should be the long term API.
 * We can't compile against this version though, since it is compiled on a newer version of kotlin than gradle embeds.
 */
internal class RuleEngineInvocation(
    private val engine: Any,
    private val lintMethod: KFunction<*>,
    private val formatMethod: KFunction<*>
) : KtLintInvocation {
    companion object Factory : KtLintInvocationFactory {
        fun initialize(ruleProviders: Set<Any>, userData: Map<String, String>): RuleEngineInvocation {
            val editorConfigOverride = userDataToEditorConfigOverride(userData)
            val ctor = ruleEngineClass!!.kotlin.primaryConstructor
            val engine = ctor!!.callBy(
                mapOf(
                    ctor.findParameterByName("ruleProviders")!! to ruleProviders,
                    ctor.findParameterByName("editorConfigOverride")!! to editorConfigOverride
                )
            )
            val lintMethod = engine::class.memberFunctions.first {
                it.name == "lint" && it.parameters.map { it.name }.containsAll(setOf("code", "filePath", "callback"))
            }
            val formatMethod = engine::class.memberFunctions.first {
                it.name == "format" && it.parameters.map { it.name }.containsAll(setOf("code", "filePath", "callback"))
            }
            return RuleEngineInvocation(engine, lintMethod, formatMethod)
        }
    }

    private val lintCodeParam: KParameter by lazy { lintMethod.findParameterByName("code")!! }
    private val lintFilePathParam: KParameter by lazy { lintMethod.findParameterByName("filePath")!! }
    private val lintCallbackParam: KParameter by lazy { lintMethod.findParameterByName("callback")!! }

    override fun invokeLint(file: File, cb: (LintError, Boolean) -> Unit) {
        lintMethod.callBy(
            mapOf(
                lintMethod.instanceParameter!! to engine,
                lintCodeParam to file.readText(),
                lintFilePathParam to file.absoluteFile.toPath(),
                lintCallbackParam to { le: LintError -> cb.invoke(le, false) }
            )
        )
    }

    private val formatCodeParam: KParameter by lazy { formatMethod.findParameterByName("code")!! }
    private val formatFilePathParam: KParameter by lazy { formatMethod.findParameterByName("filePath")!! }
    private val formatCallbackParam: KParameter by lazy { formatMethod.findParameterByName("callback")!! }

    override fun invokeFormat(file: File, cb: (LintError, Boolean) -> Unit): String {
        return formatMethod.callBy(
            mapOf(
                formatMethod.instanceParameter!! to engine,
                formatCodeParam to file.readText(),
                formatFilePathParam to file.absoluteFile.toPath(),
                formatCallbackParam to cb
            )
        ) as String
    }
}

/**
 * detect the params class for ktlint < 0.46
 */
private val legacyParamsClass: Class<*>? by lazy {
    try {
        Class.forName("com.pinterest.ktlint.core.KtLint\$Params")
    } catch (e: Exception) {
        null
    }
}

/**
 * detect the params class for ktlint 0.46.x and 0.47.x
 */
private val experimentalParamsClass: Class<*>? by lazy {
    try {
        Class.forName("com.pinterest.ktlint.core.KtLint\$ExperimentalParams")
    } catch (e: Exception) {
        null
    }
}

/**
 * detect the RuleEngine class for ktlint >= 0.48.0
 */
private val ruleEngineClass: Class<*>? by lazy {
    try {
        Class.forName("com.pinterest.ktlint.core.KtLintRuleEngine")
    } catch (e: Exception) {
        null
    }
}

internal fun selectInvocation(): KtLintInvocationFactory? {
    if (legacyParamsClass != null) {
        return LegacyParamsInvocation
    }
    if (ruleEngineClass != null) {
        return RuleEngineInvocation
    }
    return experimentalParamsClass?.let {
        val ctor = it.kotlin.primaryConstructor
        if (ctor?.findParameterByName("ruleProviders") != null) {
            // ktlint = 0.47.x
            ExperimentalParamsProviderInvocation
        } else {
            // ktlint = 0.46.x
            ExperimentalParamsInvocation
        }
    }
}
