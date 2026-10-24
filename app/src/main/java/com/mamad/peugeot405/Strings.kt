package com.mamad.peugeot405

import androidx.compose.runtime.staticCompositionLocalOf

/** همه‌ی متن‌های برنامه به دو زبان فارسی و انگلیسی */
class Strings(val fa: Boolean) {
    private fun t(f: String, e: String) = if (fa) f else e

    // عمومی
    val appTitle = t("پژو ۴۰۵", "PEUGEOT 405")
    val appSubtitle = t("کنترل تعلیق پنوماتیک", "PNEUMATIC SUSPENSION")
    val close = t("بستن", "Close")
    val save = t("ذخیره", "Save")
    val cancel = t("لغو", "Cancel")
    val send = t("ارسال", "Send")

    // ناوبری
    val navControl = t("کنترل", "Control")
    val navCommands = t("فرمان‌ها", "Commands")
    val navSettings = t("تنظیمات", "Settings")

    // صفحه‌ی اتصال
    val connected = t("متصل شد", "Connected")
    val connecting = t("در حال اتصال...", "Connecting...")
    val connectModule = t("اتصال به ماژول", "Connect to module")
    val connectHint = t("بلوتوث ماژول را روشن کن و دکمه را لمس کن", "Power on the module and tap the button")
    val pickDevice = t("انتخاب دستگاه", "Select device")
    val demoMode = t("حالت آزمایشی", "Demo mode")

    // ریموت
    val remoteLock = t("قفل", "Lock")
    val remoteUnlock = t("باز", "Unlock")
    val enterMenu = t("ورود به منو", "Enter app")
    val remoteReady = t("ریموت آماده است", "Remote ready")
    val moduleReadyRemote = t("ماژول متصل شد — ریموت آماده است", "Module connected — remote ready")
    val noConnection = t("اتصال برقرار نیست", "Not connected")
    fun lockText(locked: Boolean?): String = when (locked) {
        true -> t("ماکت قفل شد", "Model locked")
        false -> t("ماکت باز شد", "Model unlocked")
        null -> moduleReadyRemote
    }

    // هدر
    val headerConnected = t("متصل", "Connected")
    val headerConnect = t("اتصال", "Connect")

    // کنترل
    val front = t("جلو", "Front")
    val rear = t("عقب", "Rear")
    val both = t("هر دو", "Both")
    val maybach = t("میباخ", "Maybach")
    val stop = t("توقف", "Stop")
    val factory = t("فابریک", "Factory")
    val stageTag = t("AIR SUSPENSION", "AIR SUSPENSION")

    // دیالوگ اتصال
    val dlgTitle = t("اتصال به HC-05", "Connect to HC-05")
    val disconnect = t("قطع اتصال", "Disconnect")
    val noBluetooth = t("این گوشی بلوتوث ندارد.", "This phone has no Bluetooth.")
    val noPermission = t("برنامه اجازه‌ی «دستگاه‌های نزدیک» (بلوتوث) را ندارد.", "The app does not have the \"Nearby devices\" (Bluetooth) permission.")
    val grantPermission = t("دادن اجازه", "Grant permission")
    val openAppSettings = t("باز کردن تنظیمات برنامه", "Open app settings")
    val btOff = t("بلوتوث گوشی خاموش است.", "Phone Bluetooth is off.")
    val turnOnBt = t("روشن کردن بلوتوث", "Turn on Bluetooth")
    val noPaired = t(
        "دستگاه جفت‌شده‌ای نیست. اول از تنظیمات بلوتوث گوشی، HC-05 را جفت کن (رمز 1234 یا 0000) و برگرد.",
        "No paired device. First pair the HC-05 in your phone's Bluetooth settings (PIN 1234 or 0000), then come back."
    )
    val unnamed = t("بدون نام", "Unnamed")
    val connectTip = t(
        "اگر وصل نشد: چراغ HC-05 باید تند چشمک بزند و به گوشی دیگری وصل نباشد.",
        "If it fails: the HC-05 LED must blink fast and it must not be connected to another phone."
    )

    // صفحه‌ی فرمان‌ها
    val customCommand = t("ارسال فرمان دلخواه", "Send custom command")
    val newCommand = t("فرمان جدید", "New command")
    val defaults = t("پیش‌فرض", "Defaults")
    val sentCommand = t("فرمان ارسالی", "Command sent")
    val editCommand = t("ویرایش فرمان", "Edit command")
    val buttonName = t("نام دکمه", "Button name")
    val groupName = t("اسم گروه (جدید بنویس یا انتخاب کن)", "Group name (type a new one or pick)")

    // صفحه‌ی تنظیمات
    val language = t("زبان", "Language")
    val languageHint = t("زبان برنامه را انتخاب کن", "Choose the app language")
    val connection = t("اتصال", "Connection")
    val showRemote = t("نمایش ریموت", "Show remote")
    val remoteSound = t("صدای ریموت (بیپ)", "Remote sound (beep)")
    val replayIntro = t("پخش دوباره‌ی انیمیشن ورود", "Replay intro animation")
    val about = t("درباره‌ی برنامه", "About")
    val aboutText = t(
        "کنترل ماکت پنوماتیک پژو ۴۰۵ از طریق بلوتوث (HC-05)",
        "Bluetooth (HC-05) controller for the pneumatic Peugeot 405 model"
    )

    // پیام‌های وضعیت
    fun status(code: String, arg: String): String = when (code) {
        "connecting" -> connecting
        "connected" -> t("متصل به $arg", "Connected to $arg")
        "failed" -> t("اتصال ناموفق: $arg", "Connection failed: $arg")
        "sendfail" -> t("ارسال ناموفق بود، اتصال قطع شد", "Send failed, connection lost")
        "needconn" -> t("اول به HC-05 وصل شو", "Connect to the HC-05 first")
        else -> t("متصل نیست", "Not connected")
    }
    val errPerm = t("اجازه‌ی بلوتوث داده نشده", "Bluetooth permission not granted")
    fun errNoResponse(m: String) = t(
        "HC-05 جواب نداد. روشن بودنش را چک کن و اینکه به گوشی دیگری وصل نباشد ($m)",
        "HC-05 did not respond. Check that it is powered on and not connected to another phone ($m)"
    )

    // نام دکمه‌ها و گروه‌ها (فقط برای فرمان‌های پیش‌فرضی که کاربر اسمشان را عوض نکرده)
    private val enLabels = mapOf(
        "f_up_s" to "Slow rise", "f_dn_s" to "Slow drop", "f_up_f" to "Fast rise", "f_dn_f" to "Fast drop",
        "r_up_s" to "Slow rise", "r_dn_s" to "Slow drop", "r_up_f" to "Fast rise", "r_dn_f" to "Fast drop",
        "m_on" to "Start Maybach", "m_off" to "Stop",
        "both_up" to "Both up", "both_dn" to "Both down", "factory" to "Factory height",
        "lock" to "Lock", "unlock" to "Unlock", "stop" to "Release button (stop)"
    )
    private val enGroups = mapOf(
        "جلو" to "Front", "عقب" to "Rear", "میباخ" to "Maybach", "سایر" to "Other",
        "هر دو" to "Both", "ریموت" to "Remote", "سیستمی" to "System"
    )

    fun cmdLabel(c: Cmd): String {
        if (fa) return c.label
        val d = DEFAULT_COMMANDS.firstOrNull { it.id == c.id } ?: return c.label
        return if (c.label == d.label) (enLabels[c.id] ?: c.label) else c.label
    }

    /** نام گروه برای نمایش */
    fun group(g: String): String = if (fa) g else (enGroups[g] ?: g)

    /** برعکس: از نام نمایشی به کلید ذخیره‌شده */
    fun groupKey(shown: String): String {
        if (fa) return shown
        return enGroups.entries.firstOrNull { it.value.equals(shown, ignoreCase = true) }?.key ?: shown
    }
}

val LocalStrings = staticCompositionLocalOf { Strings(true) }
