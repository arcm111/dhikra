package com.example.dhikra.data

/**
 * The default daily program, «برنامج المرحلة الثانية من السلوك إلى الله في طريق التزكية», used on
 * first launch and restorable from the settings. Its Quran passages, hadiths, duas and adhkar are
 * the default library collections, as single texts and as groups of texts read one after the
 * other (e.g. «أدعية الحمد والشكر»); the program only references them. The Quran passages are
 * themselves references to their ayat: their text comes from the bundled Quran text.
 */
object DefaultProgram {

    /**
     * Version of this default content. Data seeded from an older version is replaced by these
     * defaults on the next launch, see [ProgramRepository.load].
     */
    const val VERSION = 4

    /** Id of the default program among the user's programs: the one a newer [VERSION] replaces. */
    const val PROGRAM_ID = "default"

    fun bundle(): ProgramBundle = ProgramBundle(create(), library())

    fun library(): Library = Library(
        entries = listOf(
            // Quran
            aalImran, kahf, ayatAlKursi, ikhlas, falaq, nas,
            // Hadith
            qiyamSunnah, qiyamVirtue, duhaVirtue,
            // Dua: opening and closing of the sessions
            audhu, salawat, asmaa, subhanakaLaIlaha, rabbanaAtina, khayrKulluh,
            // Dua: the night and Fajr
            nightDua, rabbJibril, nur, thabat, muqallib,
            // Dua: the morning
            ahd, baraka, shukr, fitan,
            // Dua: after the prayers
            ajirni, jubn, aini, khataya, kufrFaqr, qiniTabath, maQaddamtu, rabbiAini, ilmNafi, adudi,
            // Dua: Duha and thanks
            duhaTawba, hasbi, huda, ikfini, barikRizq, khayrMaSaalak, laSahl, ustur,
            lakalHamd, awzini, biNimatihi, alaKulliHal, nafsMutmainna, subhanakaAstaghfiruk,
            hamdanKathira, kamaYanbaghi,
            // Dua: sleep
            bedtime, bismikaAmutu, alimGhayb, khalaqtaNafsi, afiya, rabbSamawat, qini, bismikaRabbi, aslamtu,
            // Dhikr
            basmala, istighfar, takbir, tahmid, tasbih, tahlil, subhanAlMalik, subhanallahWabihamdihi, dayq,
            afterSalam, tahlilFull, istighfarHayy, fiveWords, istighfarAzeem, baqiyat, dailyIstighfar,
            kalimatArbaa, hamdHadana, kaffarah,
        ),
        groups = listOf(
            muawwidhat,
            jawamiGroup, adhanIqamaGroup, fajrSunnahGroup, morningGroup, afterPrayerGroup, duhaGroup,
            hamdGroup, sleepGroup,
            qiyamWakingGroup, tasbihSalatGroup, morningMajlisGroup, sleepTasbihGroup,
        ),
    )

    fun create(): Program = Program(
        id = PROGRAM_ID,
        name = "برنامج المرحلة الثانية",
        sections = listOf(
            duringTheDay(),
            nightPrayer(),
            afterFajr(),
            afterSunrise(),
            duhaTime(),
            afternoon(),
            afterIsha(),
            beforeSleep(),
            sessions(),
        ),
    )

    // ------------------------------------------------------------------------------ Program

    /** The Quran recitation this stage focuses on, and the istighfar of the whole day. */
    private fun duringTheDay() = ProgramSection(
        id = "day",
        title = "خلال اليوم",
        icon = "📿",
        note = "في هذه المرحلة يُركَّز على تلاوة القرآن الكريم: لا يقلّ عن ثلاثة أجزاء في اليوم، إما على ثلاثة مجالس (لكل مجلس جزء واحد) وإما على مجلسين (لكل مجلس جزء ونصف). ويُمنع منعاً باتاً الاستهتار في تلاوته.",
        items = listOf(
            heading(
                "day_quran", "ورد القرآن الكريم",
                "في الأوقات المستحبة: الفجر، والقيام، وأول الليل. يقرأ السالك براحة في التلاوة وتدبّر وخشوع وترتيل حتى يُفتح القلب للفهم.",
            ),
            task("day_juz_1", "تلاوة الجزء الأول من ورد اليوم"),
            task("day_juz_2", "تلاوة الجزء الثاني من ورد اليوم"),
            task("day_juz_3", "تلاوة الجزء الثالث من ورد اليوم"),
            heading("day_dhikr", "ذكر خلال اليوم"),
            read("day_istighfar", dailyIstighfar, count = 500, note = "لا يقلّ عن ٥٠٠ مرة، ويُسجَّل العدد اليومي مساءً"),
        ),
    )

    private fun nightPrayer() = ProgramSection(
        id = "qiyam",
        title = "قيام الليل",
        icon = "🌙",
        reminderMinutes = 4 * 60,
        note = "يبدأ قيام الليل من بعد المغرب حتى الفجر، وأفضل الأوقات وأكرمها عند الله عبادة الليل.",
        items = listOf(
            read("qiyam_sunnah", qiyamSunnah),
            read("qiyam_aal_imran", aalImran, note = "سنة نبوية: يقرأ قبل صلاة القيام"),
            read("qiyam_waking", qiyamWakingGroup),
            task("qiyam_salat", "الوضوء ثم صلاة قيام الليل"),
            read("qiyam_dua", nightDua),
            read("qiyam_virtue", qiyamVirtue),
            task("qiyam_wird", "ورد قرآن كريم (قراءة ما تيسر)"),
            task("qiyam_dhikr", "ذكر قيام الليل"),
            heading("qiyam_fajr", "عند الفجر"),
            read("qiyam_adhan", adhanIqamaGroup),
            task(
                "qiyam_majlis", "مجلس تلاوة القرآن الكريم مع الجماعة في مجلس الروضة الشريفة",
                note = "فجرًا: من الساعة 5:00 حتى الشروق (بتوقيت بلدك وفق توقيت المدينة المنوّرة) • مدة المجلس: نصف ساعة",
            ),
            read("qiyam_sunnat_fajr", fajrSunnahGroup),
        ),
    )

    private fun afterFajr() = ProgramSection(
        id = "fajr",
        title = "بعد صلاة الفجر",
        icon = "🕌",
        reminderMinutes = 5 * 60 + 15,
        note = "أذكار بعد الصلاة تقرأ بعد جميع الصلوات المفروضة",
        items = listOf(
            read("fajr_istighfar", istighfar, count = 3),
            read("fajr_salam", afterSalam),
            read("fajr_tasbih", tasbihSalatGroup),
            read("fajr_kursi", ayatAlKursi),
            read(
                "fajr_muawwidhat", muawwidhat,
                note = "تقرأ مرة واحدة بعد الصلوات، وثلاثاً بعد الفجر والمغرب",
            ),
            read("fajr_adiya", afterPrayerGroup),
            read("fajr_istighfar_hayy", istighfarHayy, count = 3),
            read("fajr_khams", fiveWords),
        ),
    )

    private fun afterSunrise() = ProgramSection(
        id = "shurooq",
        title = "بعد فترة الشروق",
        icon = "☀️",
        reminderMinutes = 6 * 60 + 45,
        items = listOf(
            read("shurooq_adiya", morningGroup),
            read("shurooq_kahf", kahf, note = "ورد تحصين: يقرأ يومياً بدون إهمال"),
            heading("shurooq_adhkar", "أذكار الصباح", "من قالها حين يصبح وحين يمسي كفته من كل شيء"),
            read("shurooq_audhu", audhu),
            read("shurooq_muawwidhat", muawwidhat),
            heading("shurooq_tasbeeh", "تسبيح الغدوّ والآصال", "بعد أذكار الصباح"),
            read("shurooq_tasbeeh_100", subhanallahWabihamdihi, count = 100),
            read(
                "shurooq_majlis", morningMajlisGroup,
                note = "لزوماً ولا بُدّ عليك تسجيل عدد الاستغفار اليومي وكتابته في الاستفتاء في مجموعة حملة الاستغفار",
            ),
        ),
    )

    private fun duhaTime() = ProgramSection(
        id = "duha",
        title = "وقت الضحى",
        icon = "🌕",
        reminderMinutes = 9 * 60,
        note = "يبدأ وقت صلاة الضحى بعد الشروق بـ20 دقيقة وينتهي قبل الظهر، وأفضل وقت: قبيل الظهر",
        items = listOf(
            read("duha_virtue", duhaVirtue),
            task("salat_duha", "صلاة الضحى", note = "عدد الركعات: من 2 إلى 8، وكل ركعتين بسلام • القراءة: لا سورة محددة"),
            read("duha_adiya", duhaGroup),
            read("duha_kalimat", kalimatArbaa),
            task("salat_shukr", "صلاة شكر (ركعتين أو أكثر)"),
            read("shukr_hamd", hamdGroup, note = "تقرأ بعد صلاة الشكر"),
        ),
    )

    private fun afternoon() = ProgramSection(
        id = "asr",
        title = "وقت الآصال",
        icon = "🌅",
        reminderMinutes = 16 * 60 + 30,
        note = "بين العصر والمغرب",
        items = listOf(
            heading("asr_adhkar", "أذكار المساء", "قبل المغرب. من قالها حين يصبح وحين يمسي كفته من كل شيء"),
            read("asr_audhu", audhu),
            read("asr_muawwidhat", muawwidhat),
            heading("asr_tasbeeh", "تسبيح الغدوّ والآصال", "بعد أذكار المساء"),
            read("asr_tasbeeh_100", subhanallahWabihamdihi, count = 100),
            task(
                "asr_majlis", "حضور مجلس تلاوة القرآن الكريم الثاني مع الجماعة",
                note = "مساءً الساعة 7 (بتوقيت بلدك وفق توقيت المدينة المنوّرة) • مدة المجلس: نصف ساعة",
            ),
        ),
    )

    private fun afterIsha() = ProgramSection(
        id = "isha",
        title = "بعد العشاء",
        icon = "⭐",
        reminderMinutes = 20 * 60 + 30,
        note = "ضروري جداً ومهم حضور مجلس الصلاة الإبراهيمية في الروضة الشريفة، ومن يقصّر من الملتزمين خاصة فهذا خطر شديد على سيره إلى الله تعالى",
        items = listOf(
            read(
                "isha_salawat", salawat, count = 100,
                note = "مع الجماعة في مجلس الصلاة على النبي ﷺ المسائي: من 9:00 حتى 9:30 مساءً (بتوقيت المدينة المنورة)",
            ),
            task(
                "isha_salawat_count",
                "كتابة العدد اليومي لمجلس الصلاة الإبراهيمية في الاستفتاء في مجموعة مجلس الروضة الشريفة",
                note = "اكتب العدد الأقرب لصلاتك على رسول الله ﷺ",
            ),
            task(
                "isha_istighfar_report",
                "تسجيل عدد الاستغفار اليومي، وإرسال التقرير اليومي لمرشدك الخاص، وكتابة العدد في الاستفتاء في مجموعة حملة الاستغفار",
            ),
        ),
    )

    private fun beforeSleep() = ProgramSection(
        id = "sleep",
        title = "قبل النوم",
        icon = "🌙",
        reminderMinutes = 22 * 60 + 30,
        note = "من تعذّر عليه حضور مجلس من المجالس الجماعية بسبب عذر طارئ فعليه بقضاء المجلس بمفرده في الوقت الذي يناسبه",
        items = listOf(
            task("sleep_surahs", "قراءة سور الإسراء والزمر والسجدة والملك (أو تكتفي بسورة الملك)"),
            read("sleep_awa", bedtime),
            read("sleep_tasbih", sleepTasbihGroup),
            task(
                "sleep_nafth",
                "النفث في الكفين بالمعوذات الثلاثة، ثلاث مرات ثُمَّ يَمْسَحُ بِهِمَا مَا اسْتَطَاعَ مِنْ جَسَدِهِ، يَبْدَأُ بِهِمَا عَلَى رَأْسِهِ وَوَجْهِهِ وَمَا أَقْبَلَ مِنْ جَسَدِهِ، يَفْعَلُ ذَلِكَ (٣ مَرَّاتٍ)",
            ),
            read("sleep_kursi", ayatAlKursi),
            task("sleep_baqara", "قراءة الآيتين من أواخر سورة البقرة"),
            task("sleep_kafirun", "قراءة سورة الكافرون"),
            read("sleep_adiya", sleepGroup),
        ),
    )

    /** Opening and closing of every Quran and dhikr session. */
    private fun sessions() = ProgramSection(
        id = "majalis",
        title = "استفتاحية المجالس وختمها",
        icon = "🤲",
        note = "لجميع مجالس القرآن الكريم والذكر",
        items = listOf(
            heading("open_heading", "الاستفتاحية"),
            read("open_audhu", audhu),
            read("open_basmala", basmala),
            read("open_salawat", salawat),
            read("open_ikhlas", ikhlas, count = 3),
            read("open_jawami", jawamiGroup, note = "اختر أحد هذه الأدعية أو قدر ما تشاء منها"),
            task("open_majlis", "ثم تذكر الله أو تتلو القرآن (حسب نوع مجلسك)"),
            heading("close_heading", "الختم"),
            read("close_salawat", salawat),
            read("close_dua", khayrKulluh),
            read("close_hamd", hamdHadana),
            read("close_kaffarah", kaffarah),
        ),
    )

    /** A step reading library [entry] [count] times. */
    private fun read(id: String, entry: LibraryEntry, count: Int = 1, note: String = "") =
        ProgramItem(id = id, type = entry.type, count = count, note = note, entryId = entry.id)

    /** A step reading the texts of [group], each as many times as the group says. */
    private fun read(id: String, group: LibraryGroup, note: String = "") =
        ProgramItem(id = id, type = group.type, note = note, groupId = group.id)

    private fun task(id: String, text: String, note: String = "") =
        ProgramItem(id = id, type = ItemType.TASK, text = text, note = note)

    private fun heading(id: String, title: String, text: String = "") =
        ProgramItem(id = id, type = ItemType.HEADING, title = title, text = text)

    private fun member(entry: LibraryEntry, count: Int = 1) = GroupItem(entry.id, count)

    // ------------------------------------------------------------------------------ Quran

    private val aalImran = quran(
        "aal_imran_190_200", "سورة آل عمران", QuranRef(3, 190, 200),
        description = "خواتيم السورة (١٩٠ - ٢٠٠)",
    )

    private val kahf = quran(
        "kahf_1_10", "سورة الكهف", QuranRef(18, 1, 10),
        description = "أول عشر آيات (١ - ١٠)",
    )

    private val ayatAlKursi = quran(
        "ayat_al_kursi", "آية الكرسي", QuranRef(2, 255),
        description = "سورة البقرة (٢٥٥)",
        opening = "أعُوذُ باللهِ مِنْ الشَّيْطَانِ الرَّجِيمِ",
    )

    private val ikhlas = quran("al_ikhlas", "سورة الإخلاص", QuranRef(112, 1, 4))

    private val falaq = quran("al_falaq", "سورة الفلق", QuranRef(113, 1, 5))

    private val nas = quran("an_nas", "سورة الناس", QuranRef(114, 1, 6))

    /** A Quran passage: only its ayat are named, the text comes from the bundled Quran text. */
    private fun quran(id: String, title: String, ref: QuranRef, description: String = "", opening: String = "") =
        LibraryEntry(
            id = id,
            type = ItemType.QURAN,
            title = title,
            text = "",
            description = description,
            opening = opening,
            quran = ref,
        )

    // ------------------------------------------------------------------------------ Hadith

    private val qiyamSunnah = LibraryEntry(
        id = "hadith_qiyam",
        type = ItemType.HADITH,
        title = "قيام النبي ﷺ من الليل",
        text = "ينام رسولُ اللهِ ﷺ حتى انتصَاف اللَّيلُ، أو قَبْله بقليلٍ، أو بَعدَه بقليلٍ، ثم يستيقظَ رسولُ اللهِ ﷺ فيجَلَس، فيمَسحَ النومَ عن وجهِه بيدِه، ثم يقرأَ العشرَ آياتٍ خواتيمَ سورةِ آل عمران، ثم يقومَ إلى شَنٍّ مُعلَّقة فتوضَّأ منها فأحسنَ وُضوءَه، ثم يقوم يُصلِّي.",
    )

    private val qiyamVirtue = LibraryEntry(
        id = "hadith_qiyam_ayat",
        type = ItemType.HADITH,
        title = "فضل القيام بالآيات",
        description = "﴿ كانوا قليلًا من الليل ما يهجعون ﴾ • المقنطرين: هم الذين يعطيهم الله أجرًا عظيماً",
        text = "عن عبدالله بن عمرو رضي الله عنه عن النبي ﷺ أنه قال: \"من قام بعشر آيات لم يكتب من الغافلين، ومن قام بمائة آية كُتب من القانتين، ومن قام بألف آية كُتب من المقنطرين\".",
    )

    private val duhaVirtue = LibraryEntry(
        id = "hadith_duha",
        type = ItemType.HADITH,
        title = "فضل صلاة الضحى",
        description = "تُعدل 360 صدقة عن كل مفصل",
        text = "قال النبي ﷺ: \"يُصبح على كلِّ سُلامَى (مفصل) من أحدكم صدقة، فكل تسبيحة صدقة... ويجزئ عن ذلك ركعتان يركعهما من الضحى\".",
    )

    // ------------------------------------------------------------------------------ Dua

    private val audhu = dua(
        "audhu_billahil_azeem", "الاستعاذة بالله العظيم",
        "أعوذ بالله العظيم وبوجهه الكريم، وسلطانه القديم، من الشيطان الرجيم",
    )

    private val salawat = dua(
        "salat_ibrahimiya", "الصلاة الإبراهيمية",
        "اللّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ، وَبَارِكْ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ كَمَا بَارَكْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ.",
    )

    private val asmaa = dua(
        "dua_asmaa", "اللهم إني أسألك بكل اسم هو لك",
        "اللّهُمَّ إِنِّي أَسْأَلُكَ بِكُلِّ اسْمٍ هُوَ لَكَ سَمَّيْتَ بِهِ نَفْسَكَ أَوْ أَنْزَلْتَهُ فِي كِتَابِكَ أَوْ عَلَّمْتَهُ أَحَدًا مِنْ خَلْقِكَ أَوْ اسْتَأْثَرْتَ بِهِ فِي عِلْمِ الغَيْبِ عِنْدَكَ أَنْ تَجْعَلَ القُرْآنَ العَظِيمَ رَبِيعَ قَلْبِي وَنُورَ صَدْرِي وَجِلَاءَ حُزْنِي وَذَهَابَ هَمِّي.",
    )

    private val subhanakaLaIlaha = dua(
        "dua_subhanaka_la_ilaha", "سبحانك اللهم وبحمدك، لا إله إلا أنت",
        "سُبْحَانَكَ اللّهُمَّ وَبِحَمْدِكَ، لَا إِلَهَ إِلَّا أَنْتَ، أَسْتَغْفِرُكَ وَأَتُوبُ إِلَيْكَ.",
    )

    private val rabbanaAtina = dua(
        "dua_rabbana_atina", "ربنا آتنا في الدنيا حسنة",
        "اللّهُمَّ رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ.",
    )

    /** One dua (it continues from paragraph to paragraph), read to close the sessions. */
    private val khayrKulluh = dua(
        "dua_khayr_kulluh", "دعاء الخير كله",
        description = "من ختم مجالس القرآن والذكر",
        text = listOf(
            "اللّهُمَّ إِنِّي أَسْأَلُكَ مِنَ الخَيْرِ كُلِّهِ عَاجِلِهِ وَآجِلِهِ مَا عَلِمْتُ مِنْهُ وَمَا لَمْ أَعْلَمْ، وَأَعُوذُ بِكَ مِنَ الشَّرِّ كُلِّهِ عَاجِلِهِ وَآجِلِهِ مَا عَلِمْتُ مِنْهُ وَمَا لَمْ أَعْلَمْ.",
            "اللّهُمَّ إِنِّي أَسْأَلُكَ الجَنَّةَ وَمَا قَرَّبَ إِلَيْهَا مِنْ قَوْلٍ أَوْ عَمَلٍ، وَأَعُوذُ بِكَ مِنَ النَّارِ وَمَا قَرَّبَ إِلَيْهَا مِنْ قَوْلٍ أَوْ عَمَلٍ.",
            "اللّهُمَّ إِنِّي أَسْأَلُكَ خَيْرَ مَا سَأَلَكَ بِهِ عَبْدُكَ وَنَبِيُّكَ مُحَمَّدٌ صَلَّى اللهُ عَلَيْهِ وَسَلَّمَ، وَأَعُوذُ بِكَ مِمَّا اسْتَعَاذَ مِنْهُ عَبْدُكَ وَنَبِيُّكَ مُحَمَّدٌ صَلَّى اللهُ عَلَيْهِ وَسَلَّمَ.",
            "وأسألك أن تجعل كل قضاء قضيته لي خيرا",
        ).joinToString("\n\n"),
    )

    private val nightDua = dua(
        "dua_qiyam", "دعاء النبي ﷺ من الليل",
        description = "عن عبدالله بن عباس رضي الله عنهما: كانَ النبيُّ ﷺ يَدْعُو مِنَ اللَّيْل",
        text = "اللهم لك الحمدُ، أنت نور السَّماوات والأرض ومَن فيهنَّ، ولك الحمدُ، أنت قيّم السَّماوات والأرض ومَن فيهنَّ، ولك الحمدُ، أنت ربّ السَّماوات والأرض ومَن فيهنَّ، أنت الحقُّ، ووعدك الحقُّ، وقولك الحقُّ، ولقاؤك الحقُّ، والجنة حقٌّ، والنار حقٌّ، والنَّبيون حقٌّ، والسَّاعة حقٌّ، اللهم لك أسلمتُ، وبك آمنتُ، وعليك توكَّلتُ، وإليك أنبتُ، وبك خاصمتُ، وإليك حاكمتُ، فاغفر لي ما قدَّمتُ وما أخَّرتُ، وما أسررتُ وما أعلنتُ، أنت إلهي، لا إله إلا أنت.",
    )

    private val rabbJibril = dua(
        "dua_rabb_jibril", "اللهم رب جبريل وميكائيل وإسرافيل",
        "اللَّهُمَّ رَبَّ جِبْرِيلَ، وَمِيكَائِيلَ، وَإِسْرَافِيلَ، فَاطِرَ السَّمَاوَاتِ وَالْأَرْضِ، عَالِمَ الْغَيْبِ وَالشَّهَادَةِ، أَنْتَ تَحْكُمُ بَيْنَ عِبَادِكَ فِيمَا كَانُوا فِيهِ يَخْتَلِفُونَ، اهْدِنِي لِمَا اخْتُلِفَ فِيهِ مِنَ الْحَقِّ بِإِذْنِكَ، إِنَّكَ تَهْدِي مَنْ تَشَاءُ إِلَى صِرَاطٍ مُسْتَقِيمٍ.",
    )

    private val nur = dua(
        "dua_nur", "اللهم اجعل في قلبي نوراً",
        "اللهم اجعل في قلبي نورًا، وفي لساني نورًا، وفي سمعي نورًا، وفي بصري نورًا، ومن فوقي نورًا، ومن تحتي نورًا، وعن يميني نورًا، وعن شمالي نورًا، ومن أمامي نورًا، ومن خلفي نورًا، واجعل لي نورًا.",
    )

    private val thabat = dua(
        "dua_thabat", "الثبات في الأمر والعزيمة على الرشد",
        "اللَّهُمَّ إِنِّي أَسْأَلُكَ الثَّبَاتَ فِي الأَمْرِ، وَالعَزِيمَةَ عَلَى الرُّشْدِ، وَأَسْأَلُكَ مُوجِبَاتِ رَحْمَتِكَ، وَعَزَائِمَ مَغْفِرَتِكَ، وَأَسْأَلُكَ شُكْرَ نِعْمَتِكَ، وَحُسْنَ عِبَادَتِكَ، وَأَسْأَلُكَ قَلْبًا سَلِيمًا، وَلِسَانًا صَادِقًا، وَأَسْأَلُكَ مِنْ خَيْرِ مَا تَعْلَمُ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا تَعْلَمُ، وَأَسْتَغْفِرُكَ لِمَا تَعْلَمُ، إِنَّكَ أَنْتَ عَلَّامُ الغُيُوبِ.",
    )

    private val muqallib = dua("dua_muqallib", "يا مقلب القلوب", "يا مقلب القلوب ثبت قلبي على دينك")

    private val ahd = dua(
        "dua_al_ahd", "دعاء العهد",
        description = "عن زيد بن ثابت رضي الله عنه أن رسول الله صلى الله عليه وسلم علّمه دعاءً وأمره أن يتعاهده ويتعاهد أهله به في كل يوم، قال: قل حين تصبح • رواه أحمد والطبراني والحاكم وقال: صحيح الإسناد، وحسنه الألباني في صحيح الترغيب والترهيب /274،275",
        text = DUA_AL_AHD,
    )

    private val baraka = dua(
        "dua_baraka", "دعاء البركة في الدار والرزق",
        description = "صحيح الجامع",
        text = "اللَّهُمَّ اِغْفِرْ ذَنْبِي، وَوَسِّعْ لِي فِي دَارِي، وَبَارِكْ لِي فِي رِزْقِي.",
    )

    private val shukr = dua(
        "dua_shukr", "شكر النعمة",
        description = "من قالها حين يُصبح فقد أدّى شُكر يومه",
        text = "اللَّهُمَّ مَا أَصْبَحَ بِي مِن نِعْمَةٍ أَو بِأَحَدٍ مِنْ خَلْقِكَ فَمِنْكَ وَحْدَكَ، لا شَرِيكَ لَكَ، فَلَكَ الحَمْدُ وَلَكَ الشُّكْرُ",
    )

    private val fitan = dua(
        "dua_fitan", "دعاء النبي ﷺ من فتن الدنيا",
        description = "روى الإمام الطبراني في معجمه الكبير، عن شداد بن أوس رضي الله عنه، قال: قال لي رسول الله صلى الله عليه وسلم: يا شداد بن أوس، إذا كنز الناس الذهب والفضة، فاكنز هؤلاء الكلمات • حديث صحيح صححه الألباني في السلسلة الصحيحة، وحسنه شعيب الأرناؤوط في تحقيق المسند",
        text = "اللهم إني أسألك الثبات في الأمر، والعزيمة على الرشد، وأسألك شكر نعمتك، وحسن عبادتك، وأسألك لسانا صادقا، وقلبا سليما، وأسألك من خير ما تعلم، وأعوذ بك من شر ما تعلم، وأستغفرك لما تعلم، إنك أنت علام الغيوب",
    )

    private val ajirni = dua("dua_ajirni", "اللهم أجرني من النار", "اللهم أجرني من النار")

    private val jubn = dua(
        "dua_jubn", "الاستعاذة من الجبن والبخل",
        "اللهم إني أعوذ بك من الجبن والبخل، وأعوذ بك من أنْ أُرَدَّ إِلى أَرْذَلِ العُمر، وأعوذ بك من فتنة الدنيا، وأعوذ بك من عذاب القبر",
    )

    private val aini = dua(
        "dua_aini", "اللهم أعني على ذكرك وشكرك",
        "اللهم أعنِّي على ذكرك وشكرك وحسن عبادتك. اللهم اجعل خير عُمري ءاخره، وخير عملي خواتمه، واجعل خير أيامي يومَ ألقاك.",
    )

    private val khataya = dua(
        "dua_khataya", "اللهم اغفر لي ذنوبي وخطاياي كلها",
        "اللهم اغفر لي ذنوبي وخَطايايَ كُلُّها، اللهم أنعشني واجبرني واهدني لصالح الأعمال والأخلاق، إنه لا يهدي لصالحها ولا يصرف سيئها إلا أنت",
    )

    private val kufrFaqr = dua(
        "dua_kufr_faqr", "الاستعاذة من الكفر والفقر",
        "اللهم إني أعوذُ بكَ مِن الكفر والفقر وعذاب القبر",
    )

    private val qiniTabath = dua(
        "dua_qini_tabath", "رب قني عذابك يوم تبعث",
        "رَبِّ قِنِي عَذَابَكَ يَومَ تَبْعَثُ، أَوْ (تَجْمَعُ) عِبَادَكَ",
    )

    private val maQaddamtu = dua(
        "dua_ma_qaddamtu", "اللهم اغفر لي ما قدمت وما أخرت",
        "اللَّهُمَّ اغْفِرْ لِي مَا قَدَّمْتُ وَمَا أَخَّرْتُ وَمَا أَسْرَرْتُ وَمَا أعْلَنْتُ وَمَا أَسْرَفْتُ وَمَا أَنْتَ أَعْلَمُ بِهِ مِنِّي أَنْتَ الْمُقَدِّمُ وَأَنْتَ الْمُؤَخِّرُ لَا إِلَهَ إِلَّا أَنْتَ",
    )

    private val rabbiAini = dua(
        "dua_rabbi_aini", "رب أعني ولا تعن علي",
        "ربِّ أَعِني ولا تُعِنْ عليَّ وانصُرني ولا تنصُرْ عليَّ وامكُرْ لي ولا تمكُرْ عليَّ واهدِني ويَسِّرِ لي الهُدى وانصُرني على من بغى عليَّ ربِّ اجعلْني لك شَكَّارًا لك ذَكَّارًا لك رهَّابًا لك مِطواعًا لك مُخْبِتا إليك أَوَّاهًا مُنيبًا ربِّ تقبَّلْ تَوْبتي واغسِلْ حَوْبَتِي وأَجِبْ دَعْوتي وثَبِّتْ حُجَّتي وَسَدِّدْ لساني واهدِ قلبي واسلُلْ سَخيمةَ صدْري",
    )

    private val ilmNafi = dua(
        "dua_ilm_nafi", "سؤال العلم النافع والرزق الطيب",
        description = "يقال بعد صلاة الصبح",
        text = "اللهم إني أسألك علما نافعا ورزقا طيبا وعملا متقبلا.",
    )

    private val adudi = dua(
        "dua_adudi", "اللهم أنت عضدي ونصيري",
        description = "تقال بعد صلاة الفجر",
        text = "اللَّهمَّ أنتَ عَضُدي ونصيري بِكَ أحولُ وبِكَ أصولُ وبِكَ أقاتِلُ",
    )

    private val duhaTawba = dua(
        "dua_duha_tawba", "اللهم اغفر لي وتب علي",
        "اللَّهُمَّ اغْفِرْ لِي، وَتُبْ عَلَيَّ، إِنَّكَ أَنْتَ التَّوَّابُ الرحيم",
    )

    private val hasbi = dua(
        "dua_hasbi", "حسبي الله لا إله إلا هو",
        "حسبي الله لا إله إلا هو، عليه توكلت وهو رب العرش العظيم.",
    )

    private val huda = dua("dua_huda", "اللهم إني أسألك الهدى والتقى", "اللهم إني أسألك الهُدى والتقى والعفاف والغنى.")

    private val ikfini = dua(
        "dua_ikfini", "اللهم اكفني بحلالك عن حرامك",
        "اللهم اكفني بحلالك عن حرامك، وأغنني بفضلك عمن سواك.",
    )

    private val barikRizq = dua(
        "dua_barik_rizq", "اللهم بارك لي في رزقي",
        "اللهم بارك لي في رزقي، وافتح لي أبواب فضلك، واجعلني من عبادك المتقين.",
    )

    private val khayrMaSaalak = dua(
        "dua_khayr_ma_saalak", "من خير ما سألك عبدك ونبيك محمد ﷺ",
        "اللهم إني أسألك من خير ما سألك عبدك ونبيك محمد ﷺ، وأعوذ بك من شر ما استعاذك منه عبدك ونبيك محمد ﷺ.",
    )

    private val laSahl = dua(
        "dua_la_sahl", "اللهم لا سهل إلا ما جعلته سهلاً",
        "اللهم لا سهل إلا ما جعلته سهلاً، وأنت تجعل الحزن إذا شئت سهلاً.\nاللهم اجعل همي الآخرة، وأصلح لي دنياي وآخرتي، ووفقني لما تحب وترضى.",
    )

    private val ustur = dua(
        "dua_ustur", "اللهم استر عوراتي",
        "اللهم استر عوراتي، وآمن روعاتي، واحفظني من بين يدي ومن خلفي.\nاللهم ارزقني رزقًا طيبًا، وعلمًا نافعًا، وعملًا متقبلًا.",
    )

    private val lakalHamd = dua(
        "dua_lakal_hamd", "اللهم لك الحمد كله",
        "اللَّهُمَّ لَكَ الحَمْدُ كُلُّهُ، وَلَكَ المُلْكُ كُلُّهُ، وَبِيَدِكَ الخَيْرُ كُلُّهُ، وَإِلَيْكَ يَرجِعُ الأَمْرُ كُلُّهُ",
    )

    private val awzini = dua(
        "dua_awzini", "رب أوزعني أن أشكر نعمتك",
        "رَبِّ أَوْزِعْنِي أَنْ أَشْكُرَ نِعْمَتَكَ الَّتِي أَنْعَمْتَ عَلَيَّ وَعَلَىٰ وَالِدَيَّ، وَأَنْ أَعْمَلَ صَالِحًا تَرْضَاهُ",
    )

    private val biNimatihi = dua(
        "dua_bi_nimatihi", "الحمد لله الذي بنعمته تتم الصالحات",
        "الحَمْدُ لِلَّهِ الَّذِي بِنِعْمَتِهِ تَتِمُّ الصَّالِحَات",
    )

    private val alaKulliHal = dua(
        "dua_ala_kulli_hal", "الحمد لله على كل حال",
        "الحَمْدُ لِلَّهِ عَلَى كُلِّ حَالٍ، وَأَعُوذُ بِاللَّهِ مِنْ حَالِ أَهْلِ النَّار",
    )

    private val nafsMutmainna = dua(
        "dua_nafs_mutmainna", "اللهم إني أسألك نفساً مطمئنة",
        "اللَّهُمَّ إِنِّي أَسْأَلُكَ نَفْسًا مُطْمَئِنَّةً تُؤْمِنُ بِلِقَائِكَ وَتَرْضَىٰ بِقَضَائِكَ وَتَقْنَعُ بِعَطَائِكَ",
    )

    private val subhanakaAstaghfiruk = dua(
        "dua_subhanaka_astaghfiruk", "سبحانك اللهم وبحمدك، أستغفرك وأتوب إليك",
        "سُبْحَانَكَ اللَّهُمَّ وَبِحَمْدِكَ، أَسْتَغْفِرُكَ وَأَتُوبُ إِلَيْكَ",
    )

    private val hamdanKathira = dua(
        "dua_hamdan_kathira", "الحمد لله حمداً كثيراً طيباً",
        "الحَمْدُ لِلَّهِ حَمْدًا كَثِيرًا طَيِّبًا مُبَارَكًا فِيه، مِلْءَ السَّمَاوَاتِ وَالأَرْضِ وَمِلْءَ مَا شِئْتَ مِنْ شَيْءٍ بَعْد.",
    )

    private val kamaYanbaghi = dua(
        "dua_kama_yanbaghi", "رب لك الحمد كما ينبغي لجلال وجهك",
        "رَبِّ لَكَ الحَمْدُ كَمَا يَنْبَغِي لِجَلاَلِ وَجْهِكَ وَعَظِيمِ سُلْطَانِكَ.",
    )

    private val bedtime = dua(
        "dua_awa_firash", "عند الأوي إلى الفراش",
        description = "وإذا أوى إلى فراشه قال",
        text = "الحمد لله الذي أطعمنا وسقانا وكفانا وآوانا فكم ممن لا كافي له ولا مؤوي.",
    )

    private val bismikaAmutu = dua("dua_bismika_amutu", "باسمك اللهم أموت وأحيا", "باسمك اللهم أموت وأحيا")

    private val alimGhayb = dua(
        "dua_alim_ghayb", "اللهم عالم الغيب والشهادة",
        "اللهم عالم الغيب والشهادة، فاطر السموات والأرض، رب كل شيء ومليكه أشهد أن لا إله إلا أنت أعوذ بك من شر نفسي، ومن شر الشيطان وشركه وأن أقترف على نفسي سوءًا أو أجرَّه إلى مسلم.",
    )

    private val khalaqtaNafsi = dua(
        "dua_khalaqta_nafsi", "اللهم خلقت نفسي وأنت توفاها",
        "اللهم خَلَقْتَ نفسي وأنت توفَّاها لك مماتها ومَحْياها، إن أحييتها فاحفظها وإن أمتها فاغفر لها",
    )

    private val afiya = dua("dua_afiya", "اللهم إني أسألك العافية", "اللهم إني أسألك العافية.")

    private val rabbSamawat = dua(
        "dua_rabb_samawat", "اللهم رب السموات السبع",
        "اللهم ربَّ السموات السبع، ورب العرش العظيم، ربنا ورب كل شيء، فالق الحب والنوى، ومُنزل التوراة والإنجيل والفرقان، أعوذ بك من شر كل شيء أنت آخذٌ بناصيته، اللهم أنت الأول فليس قبلك شيء، وأنت الآخر فليس بعدك شيء، وأنت الظاهر فليس فوقك شيء، وأنت الباطن فليس دونك شيء، اقض عنا الدَّيْنَ واغننا من الفقر",
    )

    private val qini = dua(
        "dua_qini", "اللهم قني عذابك يوم تبعث عبادك",
        description = "النبي ﷺ إذا أراد أنْ يرقُدَ وضع يده اليمنى تحت خدِّه الأيمنِ",
        text = "اللهم قني عذابك يوم تبعث عبادك.",
    )

    private val bismikaRabbi = dua(
        "dua_bismika_rabbi", "باسمك ربي وضعت جنبي",
        "باسمك ربي وضعت جنبي وبك أرفعه إن أمسكت نفسي فارحمها، وإن أرسلتها فاحفظها بما تحفظ به عبادك الصالحين",
    )

    private val aslamtu = dua(
        "dua_aslamtu", "اللهم أسلمت نفسي إليك",
        "اللهم إني أسلمت نفسي إليك، ووجهت وجهي إليك، وفوضت أمري إليك، وألجأت ظهري إليك، رغبة ورهبة إليك، لا ملجأ ولا منجا منك إلا إليك، آمنت بكتابك الذي أنزلت، ونبيك الذي أرسلت.",
    )

    // ------------------------------------------------------------------------------ Dhikr

    private val basmala = dhikr("basmala", "البسملة", "بسم الله الرحمن الرحيم")

    private val istighfar = dhikr("istighfar", "أستغفر الله", "أستغفر الله")

    private val takbir = dhikr("takbir", "الله أكبر", "الله أكبر")

    private val tahmid = dhikr("tahmid", "الحمد لله", "الحمد لله")

    private val tasbih = dhikr("tasbih", "سبحان الله", "سبحان الله")

    private val tahlil = dhikr("tahlil", "لا إله إلا الله", "لا إله إلا الله")

    private val subhanAlMalik = dhikr("subhan_al_malik", "سبحان الملك القدوس", "سُبحانَ المَلِك القُدُّوس")

    private val subhanallahWabihamdihi = dhikr("subhanallah_wabihamdihi", "سبحان الله وبحمده", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ")

    private val dayq = dhikr(
        "dayq_dunya_akhira", "الاستعاذة من ضيق الدنيا ويوم القيامة",
        "اللَّهُمَّ إني أعوذُ بِكَ من ضِيقِ الدُّنيا، وضِيقِ يَوْمِ القيامَةِ",
    )

    private val afterSalam = dhikr(
        "adhkar_baad_salam", "اللهم أنت السلام",
        description = "أذكار بعد الصلوات",
        text = "اللهمَّ أنتَ السَّلامُ، ومنك السَّلامُ، تبارَكْتَ يا ذا الجلالِ والإكرامِ لا إلهَ إلَّا اللهُ وحدَه لا شريكَ له، له المُلكُ، وله الحمدُ، وهو على كلِّ شيءٍ قديرٌ، اللهمَّ لا مانعَ لِما أعطَيْتَ، ولا مُعْطيَ لِما منَعْتَ، ولا ينفَعُ ذا الجَدِّ منك الجَدُّ لا إلهَ إلَّا اللهُ وحدَه لا شريكَ له، له المُلكُ وله الحمدُ وهو على كلِّ شيءٍ قديرٌ، لا حولَ ولا قوَّةَ إلَّا باللهِ، لا إلهَ إلَّا اللهُ، ولا نعبُدُ إلَّا إياهُ، له النِّعمةُ وله الفضلُ، وله الثَّناءُ الحسَنُ، لا إلهَ إلَّا اللهُ مُخلِصينَ له الدِّينَ ولو كرِهَ الكافرونَ.",
    )

    private val tahlilFull = dhikr(
        "tahlil_wahdahu", "لا إله إلا الله وحده لا شريك له",
        "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير",
    )

    private val istighfarHayy = dhikr(
        "istighfar_hayy_qayyum", "أستغفر الله الذي لا إله إلا هو الحي القيوم",
        description = "مَنْ قالَها (ثلاثًا) غُفِرتْ له ذُنوبُه، وإنْ كان فارًّا مِنَ الزَّحفِ • الراوي: عبدالله بن مسعود • المحدث: الحاكم • المصدر: المستدرك على الصحيحين • الصفحة أو الرقم: 1908 • خلاصة حكم المحدث: صحيح على شرط الشيخين",
        text = "أَستغفِرُ اللهَ الَّذي لا إلَهَ إلَّا هو الحيُّ القيُّومُ، وأَتوبُ إليه",
    )

    private val fiveWords = dhikr(
        "kalimat_khams", "الكلمات الخمس",
        description = "أورده النسائي في السنن الكبرى عن أبي هريرة: يعقدُهنَّ خمسًا بأصابعِه، ثم قال: من قالهن في يومٍ، أو في ليلةٍ، أو في شهرٍ، ثم مات في ذلك اليومِ، أو في تلك الليلةِ، أو في ذلك الشهرِ؛ غُفِرَ له ذنبُه • يقال بعد الصلوات أو يومياً قدر ما تشاء",
        text = "لا إله إلا اللهُ، والله أكبرُ.\nلا إله إلا اللهُ وحده.\nلا إله إلا اللهُ، ولا شريك له.\nلا إله إلا اللهُ، له الملك، وله الحمدُ.\nلا إله إلا اللهُ، ولا حولَ، ولا قوَّةَ إلا باللهِ.",
    )

    private val istighfarAzeem = dhikr("istighfar_azeem", "أستغفر الله العظيم وأتوب إليه", "أستغفر الله العظيم وأتوب إليه")

    private val baqiyat = dhikr(
        "baqiyat_salihat", "الباقيات الصالحات",
        "سبحان الله والحمد لله ولا إله إلا الله والله أكبر ولا حول ولا قوة إلا بالله العلي العظيم",
    )

    private val dailyIstighfar = dhikr(
        "istighfar_yawmi", "أستغفر الله العظيم الذي لا إله إلا هو",
        "أستغفر الله العظيم الذي لا إله إلا هو الحي القيوم وأتوب إليه",
    )

    private val kalimatArbaa = dhikr(
        "kalimat_arbaa", "الكلمات الأربع",
        description = "من الأذكار النبوية بعد صلاة الضحى",
        text = "سبحان الله، الحمد لله، لا إله إلا الله، الله أكبر.",
    )

    private val hamdHadana = dhikr(
        "hamd_hadana", "الحمد لله الذي هدانا لهذا",
        "الحمد لله الذي هدانا لهذا وما كنا لنهتدي لولا أن هدانا الله.",
    )

    private val kaffarah = dhikr(
        "kaffarat_majlis", "كفارة المجلس",
        "سبحانك اللهم وبحمدك نشهد أن لا إله إلا أنت نستغفرك ونتوب إليك",
    )

    private fun dua(id: String, title: String, text: String, description: String = "") =
        LibraryEntry(id = id, type = ItemType.DUA, title = title, text = text, description = description)

    private fun dhikr(id: String, title: String, text: String, description: String = "") =
        LibraryEntry(id = id, type = ItemType.DHIKR, title = title, text = text, description = description)

    // ------------------------------------------------------------------------------ Groups
    // Declared after the entries they list: object properties are initialized in order.

    private val muawwidhat = LibraryGroup(
        id = "group_muawwidhat",
        type = ItemType.QURAN,
        title = "المعوذات",
        description = "الإخلاص والفلق والناس: ثلاثاً بعد الفجر والمغرب، وفي أذكار الصباح والمساء",
        items = listOf(member(ikhlas, 3), member(falaq, 3), member(nas, 3)),
    )

    private val jawamiGroup = LibraryGroup(
        id = "group_jawami",
        type = ItemType.DUA,
        title = "الورد النبوي الجامع",
        description = "من جوامع أدعية المصطفى ﷺ",
        items = listOf(member(asmaa), member(salawat), member(subhanakaLaIlaha), member(rabbanaAtina)),
    )

    private val adhanIqamaGroup = LibraryGroup(
        id = "group_adhan_iqama",
        type = ItemType.DUA,
        title = "دعاء بين الأذان والإقامة",
        description = "كان ﷺ يقول بين الأذان والإقامة في صلاة الفجر",
        items = listOf(member(rabbJibril), member(nur)),
    )

    private val fajrSunnahGroup = LibraryGroup(
        id = "group_sunnat_fajr",
        type = ItemType.DUA,
        title = "دعاء بعد ركعتي سنة الفجر",
        description = "ركعتي سُنّة الفجر قبل صلاة الصبح",
        items = listOf(member(thabat), member(rabbJibril), member(muqallib)),
    )

    private val morningGroup = LibraryGroup(
        id = "group_sabah",
        type = ItemType.DUA,
        title = "أدعية الصباح",
        description = "تستفتح يومك بهذه الأدعية",
        items = listOf(member(ahd), member(baraka), member(shukr), member(fitan)),
    )

    private val afterPrayerGroup = LibraryGroup(
        id = "group_baad_salat",
        type = ItemType.DUA,
        title = "أدعية بعد الصلوات",
        description = "المأثورة عن النبي ﷺ",
        items = listOf(
            member(ajirni, 7), member(jubn), member(aini), member(khataya), member(kufrFaqr),
            member(qiniTabath), member(maQaddamtu), member(rabbiAini), member(ilmNafi), member(adudi),
        ),
    )

    private val duhaGroup = LibraryGroup(
        id = "group_duha",
        type = ItemType.DUA,
        title = "أدعية بعد صلاة الضحى",
        description = "الأذكار النبوية بعد صلاة الضحى",
        items = listOf(
            member(duhaTawba, 100), member(hasbi), member(huda), member(ikfini), member(barikRizq),
            member(khayrMaSaalak), member(laSahl), member(ustur),
        ),
    )

    private val hamdGroup = LibraryGroup(
        id = "group_hamd",
        type = ItemType.DUA,
        title = "أدعية الحمد والشكر",
        description = "أدعية الحمد والشكر للنبي ﷺ",
        items = listOf(
            member(lakalHamd), member(awzini), member(biNimatihi), member(shukr), member(alaKulliHal),
            member(nafsMutmainna), member(subhanakaAstaghfiruk), member(hamdanKathira), member(kamaYanbaghi),
        ),
    )

    private val sleepGroup = LibraryGroup(
        id = "group_nawm",
        type = ItemType.DUA,
        title = "أدعية النوم",
        items = listOf(
            member(bismikaAmutu), member(alimGhayb), member(khalaqtaNafsi), member(afiya), member(rabbSamawat),
            member(qini, 3), member(bismikaRabbi), member(aslamtu),
        ),
    )

    private val qiyamWakingGroup = LibraryGroup(
        id = "group_qiyam_waking",
        type = ItemType.DHIKR,
        title = "أذكار الاستيقاظ للقيام",
        description = "كان النبي ﷺ إذا هبّ من الليل يقولها عشراً عشراً، ثم يستفتح الصلاة",
        items = listOf(
            member(takbir, 10), member(tahmid, 10), member(subhanallahWabihamdihi, 10), member(subhanAlMalik, 10),
            member(istighfar, 10), member(tahlil, 10), member(dayq, 10),
        ),
    )

    private val tasbihSalatGroup = LibraryGroup(
        id = "group_tasbih_salat",
        type = ItemType.DHIKR,
        title = "التسبيح بعد الصلاة",
        description = "أو بالطريقة الثانية: سبحان الله، والحمد لله، والله أكبر، عشر مرات لكلٍّ منها",
        items = listOf(member(tasbih, 33), member(tahmid, 33), member(takbir, 33), member(tahlilFull)),
    )

    private val morningMajlisGroup = LibraryGroup(
        id = "group_majlis_sabah",
        type = ItemType.DHIKR,
        title = "أذكار مجلس الصباح",
        description = "مع الجماعة في مجلس الروضة الشريفة. يجب إرسال تلقين الذكر إلى المرشد المسؤول",
        items = listOf(member(istighfarAzeem, 100), member(tahlilFull, 100), member(baqiyat, 100)),
    )

    private val sleepTasbihGroup = LibraryGroup(
        id = "group_tasbih_nawm",
        type = ItemType.DHIKR,
        title = "تسبيحات النوم",
        items = listOf(member(takbir, 34), member(tasbih, 33), member(tahmid, 33)),
    )

    private const val DUA_AL_AHD =
        "لبيك اللهم لبيك، لبيك وسعديك، والخيرُ في يديك، ومنك وإليك، اللهم ما قلتُ من قول، أو حلفتُ من حلف، أو نذرتُ من نذر، فمشيئتُك بين يديه، ما شئتَ كان، ومالم تشأْ لم يكن، لا حولَ ولا قوةَ إلا بك، إنك على كل شيءٍ قديرٌ، اللهم وما صليتُ من صلاةٍ فعلى من صليتَ، وما لعنتُ من لعنةٍ فعلى من لعنتَ، إنك وليِّ في الدنيا والآخرة، توفني مسلماً، وألحقني بالصالحين، اللهم إني أسألك الرضا بعد القضاء، وبردَ العيش بعد الموت، ولذةَ النظر إلى وجهِك، والشوقَ إلى لقائك، في غيرِ ضرّاءَ مضِرةٍ، ولا فتنةٍ مضلّةٍ، وأعوذُ بك اللهم أن أَظْلِمَ أَوْ أُظْلَمَ، أو أَعْتَديَ أو يُعْتَدَى عليَّ، أو أكتسب خطيئةَ أو ذنباً لاتغفره، اللهم فاطرَ السماواتِ والأرضِ، عالمَ الغيبِ والشهادةِ، ذا الجلالِ والإكرامِ، فإني أَعهدُ إليك في هذه الحياةِ الدنيا، وأُشهدكَ وكفى باللهِ شهيداً أني أَشهدُ أن لا إله إلا أنت، وحدك لا شريكَ لك، لك الملكُ، ولك الحمدُ، وأنت على كل شيءٍ قديرٌ، وأَشهدُ أن محمد عبدُك ورسولُك، وأَشهد أن وعدك حقٌ، ولقاءك حقٌ، والجنةَ حقٌ، والساعةَ آتيةٌ لا ريبَ فيها، وأنك تبعث من في القبور، وأنك إن تكلني إلى نفسي تكلني إلى ضعفٍ وعورةٍ وذنبٍ وخطيئةٍ، وإني لا أثقُ إلا برحمتك، فاغفر لي ذنوبي كلها، إنه لا يغفر الذنوب إلا أنت، وتب عليَّ إنك أنت التوَّاب الرحيم"
}
