package org.gradle.plugins.compile

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.caching.configuration.BuildCacheConfiguration
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.toolchain.internal.JavaInstallationProbe
import org.gradle.jvm.toolchain.internal.LocalJavaInstallation
import org.slf4j.LoggerFactory
import java.io.File

class DefaultJavaInstallation(val current: Boolean, private val javaHome: File) : LocalJavaInstallation {
    private
    lateinit var name: String
    private
    lateinit var javaVersion: JavaVersion
    private
    lateinit var displayName: String

    override fun getName() = name
    override fun getDisplayName() = displayName
    override fun setDisplayName(displayName: String) { this.displayName = displayName }
    override fun getJavaVersion() = javaVersion
    override fun setJavaVersion(javaVersion: JavaVersion) { this.javaVersion = javaVersion }
    override fun getJavaHome() = javaHome
    override fun setJavaHome(javaHome: File) { throw UnsupportedOperationException("JavaHome cannot be changed") }
    val toolsJar: File? by lazy { Jvm.forHome(javaHome).toolsJar }

    override fun toString(): String = "${displayName} (${javaHome.absolutePath})"
}

open class AvailableJavaInstallations(project: Project, private val javaInstallationProbe: JavaInstallationProbe) {

    private
    companion object {
        private
        val java7HomePropertyName = "java7Home"
        private
        val testJavaHomePropertyName = "testJavaHome"
    }

    private
    val javaInstallations: Map<JavaVersion, DefaultJavaInstallation>
    private
    val logger = LoggerFactory.getLogger(AvailableJavaInstallations::class.java)
    val currentJavaInstallation: DefaultJavaInstallation
    val javaInstallationForTest: DefaultJavaInstallation

    private
    val java7HomeSet: Boolean

    init {
        val resolvedJava7Home = resolveJavaHomePath(java7HomePropertyName, project)
        java7HomeSet = resolvedJava7Home != null
        val javaHomesForCompilation = listOfNotNull(resolvedJava7Home)
        val javaHomeForTest = resolveJavaHomePath(testJavaHomePropertyName, project)
        javaInstallations = findJavaInstallations(javaHomesForCompilation)
        currentJavaInstallation = DefaultJavaInstallation(true, Jvm.current().javaHome).apply {
            javaInstallationProbe.current(this)
        }
        javaInstallationForTest = when (javaHomeForTest) {
            null -> currentJavaInstallation
            else -> detectJavaInstallation(javaHomeForTest)
        }
    }

    fun jdkForCompilation(javaVersion: JavaVersion) = when {
        javaVersion == currentJavaInstallation.javaVersion -> currentJavaInstallation
        javaInstallations.containsKey(javaVersion) -> javaInstallations[javaVersion]!!
        javaVersion <= currentJavaInstallation.javaVersion -> currentJavaInstallation
        else -> throw IllegalArgumentException("No Java installation found which supports Java version $javaVersion")
    }

    fun validateBuildCacheConfiguration(buildCacheConfiguration: BuildCacheConfiguration) {
        val jdkForCompilation = javaInstallations.values.firstOrNull()
        val validationErrors = mapOf(
            !java7HomeSet to "'java7Home' project or system property not set.",
            (java7HomeSet && jdkForCompilation?.displayName != "Oracle JDK 7") to
                "'java7Home' needs to point to an Oracle JDK 7, put points to ${jdkForCompilation?.displayName}.",
            (currentJavaInstallation.displayName != "Oracle JDK 8") to
                "Gradle needs to run with a Oracle 8 JDK, but has been started with ${currentJavaInstallation.displayName}."
        ).filterKeys { it }
        if (validationErrors.isNotEmpty()) {
            val message = (listOf("In order to have cache hits from the remote build cache, your environment needs to be configured accordingly!", "Problems found:") +
                validationErrors.values.map {
                    "    - $it"
                }).joinToString("\n")
            val remoteCache = buildCacheConfiguration.remote
            if (remoteCache != null && remoteCache.isEnabled) {
                throw GradleException(message)
            } else {
                logger.warn(message)
            }
        }
    }

    private
    fun findJavaInstallations(javaHomes: List<String>) =
        javaHomes.map(::detectJavaInstallation).associateBy { it.javaVersion }

    private
    fun detectJavaInstallation(javaHomePath: String) =
        DefaultJavaInstallation(false, File(javaHomePath)).apply {
            javaInstallationProbe.checkJdk(javaHome).configure(this)
        }

    private
    fun resolveJavaHomePath(propertyName: String, project: Project): String? = when {
        project.hasProperty(propertyName) -> project.property(propertyName) as String
        System.getProperty(propertyName) != null -> System.getProperty(propertyName)
        else -> null
    }
}
