package io.github.arcm111.dhikra.data

/**
 * The app's built-in program, the starter «الأذكار اليومية» ([STARTER_ID]) followed on first launch,
 * with its library: the morning and evening adhkar (from the open dataset
 * Morning-And-Evening-Adhkar-DB, MIT), the adhkar after each prayer and before sleep, as single texts
 * and as groups of texts read one after the other. The program only references its texts, and the
 * Quran passages are themselves references to their ayat: their text comes from the bundled Quran
 * text.
 *
 * The content is the program store file `default_program.json` (the app's own store format, see
 * [ProgramJson.encodeStore]), bundled as a Java resource so the tests and the previews read it too.
 * It is made by `scripts/starter/build_starter.py`; after changing it, bump [VERSION].
 */
object DefaultProgram {

    /**
     * Version of this default content. On the next launch after data seeded from an older version,
     * the built-in programs are replaced by these (and added if missing), see [ProgramRepository.load].
     */
    const val VERSION = 9

    /**
     * Id of the program of older versions: «برنامج المرحلة الثانية», built in up to version 8 (now
     * kept in `backup/second-stage-program/`), and the single program of the oldest storage. It is
     * no longer built in: where it was seeded, it stays as one of the user's own programs.
     */
    const val PROGRAM_ID = "default"

    /** Id of the starter program «الأذكار اليومية»: the one a first launch follows. */
    const val STARTER_ID = "starter"

    private const val RESOURCE = "/default_program.json"

    private val builtIn: ProgramStore by lazy {
        val json = DefaultProgram::class.java.getResourceAsStream(RESOURCE)
            ?.use { it.readBytes().decodeToString() }
            ?: error("Missing $RESOURCE")
        ProgramJson.decodeStore(json)
    }

    /** The built-in program with its library, followed: what a first launch starts with. */
    fun store(): ProgramStore = builtIn

    /** The built-in programs (the starter alone). */
    fun programs(): List<Program> = builtIn.programs

    /** The built-in program [id], if there is one. */
    fun program(id: String): Program? = builtIn.programs.firstOrNull { it.id == id }

    /** The program a first launch follows (the starter) with the library. */
    fun bundle(): ProgramBundle = builtIn.bundle

    fun library(): Library = builtIn.library

    fun create(): Program = builtIn.active
}
