package com.gopu.arrow.puzzle.game.i18n

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Keeps user-visible text out of the Kotlin sources.
 *
 * The task's closing instruction - search the whole project for hard-coded
 * English and move it into resources - is a one-time action that quietly stops
 * being true. The way that regresses is mundane: someone adds a tooltip, writes
 * it inline because it is one word, and the string exists in exactly one
 * language. This reads the sources and fails the build on the shapes that would
 * produce one.
 *
 * Deliberately narrow. It looks only at the parameter positions whose value is
 * shown to a player, and it stays silent about everything else in a string
 * literal, so it does not need an allowlist to keep passing: Compose's animation
 * `label = "pressScale"`, `require` messages in the engine, and logcat tags are
 * all developer-facing and all left alone.
 */
class NoHardCodedUiTextTest {

    /**
     * Position -> what it means. Each is a place a player reads.
     *
     * `Text(` is a call rather than a parameter, so it is matched separately.
     * `label` is the loose one - it is also the name of a parameter on
     * `animateFloatAsState` and friends, whose values are developer-facing
     * animation keys - so it is filtered by [isProse].
     */
    private val userVisibleParameters = listOf(
        "contentDescription",
        "onClickLabel",
        "title",
        "subtitle",
        "primaryLabel",
        "secondaryLabel",
        "tertiaryLabel",
        "quaternaryLabel",
        "text",
        "label"
    )

    /**
     * Positions named `label` that are animation keys, not copy.
     *
     * Compose's animation API takes a `label` whose only job is to identify the
     * animation in a trace or a debugger. It is never rendered, so it stays
     * English, and this filter keeps the scan from needing an allowlist entry per
     * animation.
     */
    private val developerFacingLabelValues = setOf(
        "pressScale", "packAlpha", "packDot", "packProgress", "breath",
        "flicker", "backdrop", "twinkle", "floatingTile", "bob",
        "splashSky", "sky", "splashArrow", "pulse", "splashGem", "drift",
        "plaqueGlints", "glints", "splashLoader", "sweep", "progress", "status"
    )

    @Test
    fun noUserVisibleStringIsWrittenInlineInTheUiSources() {
        val offenders = mutableListOf<String>()

        for (file in uiSources()) {
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                val where = "${file.invariantSeparatorsPath}:${index + 1}"
                val trimmed = line.trimStart()
                if (trimmed.startsWith("//") || trimmed.startsWith("*")) return@forEachIndexed

                for (parameter in userVisibleParameters) {
                    for (match in Regex("\\b$parameter\\s*=\\s*\"([^\"]*)\"").findAll(line)) {
                        val literal = match.groupValues[1]
                        if (!isProse(literal)) continue
                        if (parameter == "label" && literal in developerFacingLabelValues) continue
                        offenders += "$where: $parameter = \"$literal\""
                    }
                }

                for (match in Regex("\\bText\\s*\\(\\s*\"([^\"]*)\"").findAll(line)) {
                    val literal = match.groupValues[1]
                    if (isProse(literal)) offenders += "$where: Text(\"$literal\")"
                }
            }
        }

        assertTrue(
            "User-visible text is hard-coded in the UI sources. Move it into " +
                "res/values/strings.xml and the other locales.\n" +
                offenders.joinToString("\n") { "  $it" },
            offenders.isEmpty()
        )
    }

    /**
     * Whether a literal is words a player could read.
     *
     * Interpolations and digits are stripped first, because a label assembled
     * from a number - `text = "$stars"`, `HowToRule("1", ...)` - is a value, not
     * copy, and is exactly what a `stringResource` call should *not* be for.
     */
    private fun isProse(literal: String): Boolean {
        val stripped = literal
            .replace(Regex("\\$\\{[^}]*}"), "")
            .replace(Regex("\\$[A-Za-z_][A-Za-z0-9_]*"), "")
            .replace(Regex("\\d+"), "")
        return stripped.isNotBlank()
    }

    @Test
    fun everyScreenComposesUnderTheLocaleProvider() {
        /*
         * A screen that renders `stringResource` has to be composed beneath
         * `LocalizedApp`, or it reads the activity's configuration and quietly
         * ignores the player's choice. The app shell is the single place that
         * composes screens, so checking it here catches the wiring being changed
         * back to an un-overridden context.
         */
        val activity = sourceFile("MainActivity.kt")
        assertTrue(
            "MainActivity must wrap the app in LocalizedApp",
            activity.readText().contains("LocalizedApp(")
        )
        assertTrue(
            "MainActivity must read the language from the repository",
            activity.readText().contains("progressRepository.languageTag")
        )

        val navigation = sourceFile("app/AppNavigation.kt")
        assertTrue(
            "Long-lived state must be keyed on LocalAppContext, not the " +
                "localised one, or a language change rebuilds the ad host",
            navigation.readText().contains("LocalAppContext")
        )
        assertTrue(
            "AppNavigation should not read LocalContext directly any more",
            !navigation.readText().contains("LocalContext.current")
        )
    }

    @Test
    fun theLauncherLabelComesFromAResource() {
        val manifest = File(projectRoot(), "app/src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml not found at $manifest", manifest.exists())
        val text = manifest.readText()
        assertTrue(
            "The launcher label should be @string/app_name so the launcher " +
                "follows the same resource as the rest of the app",
            text.contains("android:label=\"@string/app_name\"")
        )
        assertTrue(
            "The launcher label is still a hard-coded literal",
            !Regex("android:label=\"(?!@)[^\"]*\"").containsMatchIn(text)
        )
    }

    // ---------------------------------------------------------------- helpers

    /** Every Kotlin file under `src/main/java`, engine and UI alike. */
    private fun uiSources(): List<File> = sourceDirectory("src/main/java")
        .walkTopDown()
        .filter { it.extension == "kt" }
        .toList()

    private fun sourceFile(relative: String): File =
        File(sourceDirectory("src/main/java"), "com/gopu/arrow/puzzle/game/$relative")

    private fun sourceDirectory(relative: String): File {
        val candidates = listOf(
            File(relative),
            File("app/$relative"),
            File(System.getProperty("user.dir"), relative)
        )
        // Absolute, because the callers walk `parentFile` upwards to find the
        // project root and a relative File("src") has no parent to walk to.
        return candidates.firstOrNull { it.isDirectory }?.absoluteFile
            ?: candidates.first().absoluteFile
    }

    private fun projectRoot(): File {
        /*
         * `<root>/app/src/main/java` - five levels up from the source root, not
         * four. Walking one short lands on the module directory and the manifest
         * lookup below then silently fails.
         */
        val module = sourceDirectory("src/main/java")
        var candidate: File? = module
        while (candidate != null) {
            if (File(candidate, "app/src/main/AndroidManifest.xml").exists()) return candidate
            candidate = candidate.parentFile
        }
        throw AssertionError("Could not find the project root from $module")
    }
}