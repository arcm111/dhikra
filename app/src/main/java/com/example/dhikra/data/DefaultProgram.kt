package com.example.dhikra.data

/**
 * The default daily program, «برنامج المرحلة الثانية من السلوك إلى الله في طريق التزكية», used on
 * first launch and restorable from the settings, with its library: the Quran passages, hadiths,
 * duas and adhkar it reads, as single texts and as groups of texts read one after the other. The
 * program only references them, and the Quran passages are themselves references to their ayat:
 * their text comes from the bundled Quran text.
 *
 * The content is the program store file `default_program.json` (the app's own store format, see
 * [ProgramJson.encodeStore]), bundled as a Java resource so the tests and the previews read it too.
 * To change the defaults, export the program from the app (or copy `files/programs.json` from a
 * debug build), replace the file and bump [VERSION].
 */
object DefaultProgram {

    /**
     * Version of this default content. Data seeded from an older version is replaced by these
     * defaults on the next launch, see [ProgramRepository.load].
     */
    const val VERSION = 5

    /** Id of the default program among the user's programs: the one a newer [VERSION] replaces. */
    const val PROGRAM_ID = "default"

    private const val RESOURCE = "/default_program.json"

    private val store: ProgramStore by lazy {
        val json = DefaultProgram::class.java.getResourceAsStream(RESOURCE)
            ?.use { it.readBytes().decodeToString() }
            ?: error("Missing $RESOURCE")
        ProgramJson.decodeStore(json)
    }

    fun bundle(): ProgramBundle = ProgramBundle(create(), library())

    fun library(): Library = store.library

    fun create(): Program = store.programs.first { it.id == PROGRAM_ID }
}
