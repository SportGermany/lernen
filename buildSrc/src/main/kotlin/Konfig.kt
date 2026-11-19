import org.gradle.api.JavaVersion

/** Gemeinsame Werte für alle Module. */
object Konfig {
    const val COMPILE_SDK = 35
    const val TARGET_SDK = 35
    const val MIN_SDK = 26
    val JAVA = JavaVersion.VERSION_17
    const val JVM_TARGET = "17"
}
