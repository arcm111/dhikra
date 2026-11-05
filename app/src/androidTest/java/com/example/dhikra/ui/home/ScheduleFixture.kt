package com.example.dhikra.ui.home

import com.example.dhikra.data.ItemType
import com.example.dhikra.data.Library
import com.example.dhikra.data.LibraryEntry
import com.example.dhikra.data.Program
import com.example.dhikra.data.ProgramBundle
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.ProgramSection

/**
 * A fixed program for the schedule UI tests, independent of the app's default content: a long
 * "shurooq" section (a counted dhikr first, long texts further down) and a short "duha" section.
 */
object ScheduleFixture {

    fun bundle(): ProgramBundle = ProgramBundle(program, library)

    private val library = Library(
        listOf(
            LibraryEntry(
                id = "subhanallah_wabihamdihi",
                type = ItemType.DHIKR,
                title = "سبحان الله وبحمده",
                text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            ),
            LibraryEntry(id = "dua_al_ahd", type = ItemType.DUA, title = "دعاء العهد", text = DUA_AL_AHD),
            LibraryEntry(
                id = "hadith_nasai_5536",
                type = ItemType.HADITH,
                title = "الاستعاذة من علم لا ينفع",
                description = "سنن النسائي (٥٬٥٣٦)",
                text = "قَالَ رَسُولُ اللَّهِ صلى الله عليه وسلم \" اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ عِلْمٍ لاَ يَنْفَعُ وَمِنْ قَلْبٍ لاَ يَخْشَعُ وَمِنْ نَفْسٍ لاَ تَشْبَعُ وَمِنْ دُعَاءٍ لاَ يُسْمَعُ \"",
            ),
            LibraryEntry(
                id = "kahf_1_10",
                type = ItemType.QURAN,
                title = "سورة الكهف",
                description = "أول عشر آيات (١ - ١٠)",
                opening = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ",
                text = KAHF_1_10,
            ),
            LibraryEntry(
                id = "audhu_billahil_azeem",
                type = ItemType.DUA,
                title = "الاستعاذة بالله العظيم",
                text = "أعوذ بالله العظيم وبوجهه الكريم، وسلطانه القديم، من الشيطان الرجيم",
            ),
            LibraryEntry(
                id = "al_ikhlas",
                type = ItemType.QURAN,
                title = "سورة الإخلاص",
                opening = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
                text = "قُلْ هُوَ اللهُ أَحَدٌ، اللهُ الصَّمَدُ، لَمْ يَلِدْ وَلَمْ يُولَدْ، وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ.",
            ),
            LibraryEntry(
                id = "al_falaq",
                type = ItemType.QURAN,
                title = "سورة الفلق",
                text = "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ، مِن شَرِّ مَا خَلَقَ، وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ، وَمِن شَرِّ النَّفَّاثَاتِ في الْعُقَدِ، وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ.",
            ),
            LibraryEntry(
                id = "an_nas",
                type = ItemType.QURAN,
                title = "سورة الناس",
                text = "قُلْ أَعُوذُ بِرَبِّ النَّاسِ، مَلِكِ النَّاسِ، إِلَهِ النَّاسِ، مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ، الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ، مِنَ الْجِنَّةِ وَالنَّاسِ.",
            ),
            LibraryEntry(
                id = "ayat_al_kursi",
                type = ItemType.QURAN,
                title = "آية الكرسي",
                description = "سورة البقرة (٢٥٥)",
                text = AYAT_AL_KURSI,
            ),
        )
    )

    /** A step reading the library entry [entryId] (the step reuses the entry's id). */
    private fun step(entryId: String, type: ItemType, count: Int = 1) =
        ProgramItem(id = entryId, type = type, count = count, entryId = entryId)

    private val program = Program(
        name = "برنامج الاختبار",
        sections = listOf(
            ProgramSection(
                id = "fajr",
                title = "بعد صلاة الفجر",
                reminderMinutes = 5 * 60 + 15,
                items = listOf(ProgramItem(id = "fajr_awrad", type = ItemType.TASK, text = "أوراد الصلاة")),
            ),
            ProgramSection(
                id = "shurooq",
                title = "بعد فترة الشروق",
                icon = "☀️",
                reminderMinutes = 6 * 60 + 45,
                items = listOf(
                    ProgramItem(id = "tasbeeh_heading", type = ItemType.HEADING, title = "تسبيح الغدوّ والآصال"),
                    step("subhanallah_wabihamdihi", ItemType.DHIKR, count = 100),
                    step("dua_al_ahd", ItemType.DUA),
                    step("hadith_nasai_5536", ItemType.HADITH),
                    step("kahf_1_10", ItemType.QURAN),
                    ProgramItem(id = "morning_heading", type = ItemType.HEADING, title = "أذكار الصباح"),
                    step("audhu_billahil_azeem", ItemType.DUA),
                    step("al_ikhlas", ItemType.QURAN, count = 3),
                    step("al_falaq", ItemType.QURAN, count = 3),
                    step("an_nas", ItemType.QURAN, count = 3),
                    step("ayat_al_kursi", ItemType.QURAN),
                ),
            ),
            ProgramSection(
                id = "duha",
                title = "وقت الضحى",
                icon = "🌕",
                reminderMinutes = 9 * 60,
                items = listOf(
                    ProgramItem(id = "salat_duha", type = ItemType.TASK, text = "صلاة الضحى والدعاء بعدها"),
                    ProgramItem(id = "salat_shukr", type = ItemType.TASK, text = "صلاة شكر (ركعتين أو أكثر)"),
                ),
            ),
        ),
    )

    private const val DUA_AL_AHD =
        "لبيك اللهم لبيك، لبيك وسعديك، والخيرُ في يديك، ومنك وإليك، اللهم ما قلتُ من قول، أو حلفتُ من حلف، أو نذرتُ من نذر، فمشيئتُك بين يديه، ما شئتَ كان، ومالم تشأْ لم يكن، لا حولَ ولا قوةَ إلا بك، إنك على كل شيءٍ قديرٌ، اللهم وما صليتُ من صلاةٍ فعلى من صليتَ، وما لعنتُ من لعنةٍ فعلى من لعنتَ، إنك وليِّ في الدنيا والآخرة، توفني مسلماً، وألحقني بالصالحين، اللهم إني أسألك الرضا بعد القضاء، وبردَ العيش بعد الموت، ولذةَ النظر إلى وجهِك، والشوقَ إلى لقائك، في غيرِ ضرّاءَ مضِرةٍ، ولا فتنةٍ مضلّةٍ، وأعوذُ بك اللهم أن أَظْلِمَ أَوْ أُظْلَمَ، أو أَعْتَديَ أو يُعْتَدَى عليَّ، أو أكتسب خطيئةَ أو ذنباً لاتغفره، اللهم فاطرَ السماواتِ والأرضِ، عالمَ الغيبِ والشهادةِ، ذا الجلالِ والإكرامِ، فإني أَعهدُ إليك في هذه الحياةِ الدنيا، وأُشهدكَ وكفى باللهِ شهيداً أني أَشهدُ أن لا إله إلا أنت، وحدك لا شريكَ لك، لك الملكُ، ولك الحمدُ، وأنت على كل شيءٍ قديرٌ، وأَشهدُ أن محمد عبدُك ورسولُك، وأَشهد أن وعدك حقٌ، ولقاءك حقٌ، والجنةَ حقٌ، والساعةَ آتيةٌ لا ريبَ فيها، وأنك تبعث من في القبور، وأنك إن تكلني إلى نفسي تكلني إلى ضعفٍ وعورةٍ وذنبٍ وخطيئةٍ، وإني لا أثقُ إلا برحمتك، فاغفر لي ذنوبي كلها، إنه لا يغفر الذنوب إلا أنت، وتب عليَّ إنك أنت التوَّاب الرحيم"

    private const val KAHF_1_10 =
        "ٱلۡحَمۡدُ لِلَّهِ ٱلَّذِيٓ أَنزَلَ عَلَىٰ عَبۡدِهِ ٱلۡكِتَٰبَ وَلَمۡ يَجۡعَل لَّهُۥ عِوَجَاۜ (1) قَيِّمٗا لِّيُنذِرَ بَأۡسٗا شَدِيدٗا مِّن لَّدُنۡهُ وَيُبَشِّرَ ٱلۡمُؤۡمِنِينَ ٱلَّذِينَ يَعۡمَلُونَ ٱلصَّٰلِحَٰتِ أَنَّ لَهُمۡ أَجۡرًا حَسَنٗا (2) مَّٰكِثِينَ فِيهِ أَبَدٗا (3) وَيُنذِرَ ٱلَّذِينَ قَالُواْ ٱتَّخَذَ ٱللَّهُ وَلَدٗا (4) مَّا لَهُم بِهِۦ مِنۡ عِلۡمٖ وَلَا لِأٓبَآئِهِمۡۚ كَبُرَتۡ كَلِمَةٗ تَخۡرُجُ مِنۡ أَفۡوَٰهِهِمۡۚ إِن يَقُولُونَ إِلَّا كَذِبٗا (5) فَلَعَلَّكَ بَٰخِعٞ نَّفۡسَكَ عَلَىٰٓ ءَاثَٰرِهِمۡ إِن لَّمۡ يُؤۡمِنُواْ بِهَٰذَا ٱلۡحَدِيثِ أَسَفًا (6) إِنَّا جَعَلۡنَا مَا عَلَى ٱلۡأَرۡضِ زِينَةٗ لَّهَا لِنَبۡلُوَهُمۡ أَيُّهُمۡ أَحۡسَنُ عَمَلٗا (7) وَإِنَّا لَجَٰعِلُونَ مَا عَلَيۡهَا صَعِيدٗا جُرُزًا (8) أَمۡ حَسِبۡتَ أَنَّ أَصۡحَٰبَ ٱلۡكَهۡفِ وَٱلرَّقِيمِ كَانُواْ مِنۡ ءَايَٰتِنَا عَجَبًا (9) إِذۡ أَوَى ٱلۡفِتۡيَةُ إِلَى ٱلۡكَهۡفِ فَقَالُواْ رَبَّنَآ ءَاتِنَا مِن لَّدُنكَ رَحۡمَةٗ وَهَيِّئۡ لَنَا مِنۡ أَمۡرِنَا رَشَدٗا (10)"

    private const val AYAT_AL_KURSI =
        "الله لا إلَهَ إِلا هُوَ الْحَيُّ الْقَيُّومُ لاَ تَأْخُذُهُ سِنَةٌ وَلا نَوْمٌ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ مَن ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاء وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالأَرْضَ وَلاَ يَؤُودُهُ حِفْظُهُمَا وَهُوَ الْعَلِيُّ الْعَظِيمُ"
}
