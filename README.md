# Peugeot405Control

برنامه‌ی کنترل ماکت پنوماتیک پژو ۴۰۵ از طریق بلوتوث (HC-05).

## مراحل برنامه
1. **صفحه‌ی اتصال** – فقط یک دکمه‌ی اتصال به ماژول بلوتوث با انیمیشن ظاهر می‌شود. بعد از اتصال موفق، دکمه محو می‌شود.
2. **صفحه‌ی شروع انیمیشنی** – ابتدا MINI GARAGE MAMMAD و سپس لوگوی پژو و نام 405 نمایش داده می‌شود.
3. **ریموت پژو** – با انیمیشن وسط صفحه ظاهر می‌شود. بعد از زدن «باز» ریموت کوچک می‌شود و می‌رود داخل منوی برنامه.
4. **منوی اصلی** – کنترل جلو / عقب / هر دو، میباخ، ارتفاع فابریک؛ تصویر بزرگ خودرو از صفحه‌ی کنترل حذف شده است. ریموت را از دکمه‌ی قفل بالای صفحه دوباره باز کن.

## فرمان‌هایی که به ماژول فرستاده می‌شود
| دکمه | فرمان |
|---|---|
| ریموت: قفل | `K` |
| ریموت: باز | `U` |
| هر دو بالا | `W` |
| هر دو پایین | `D` |
| ارتفاع فابریک | `1` سپس `X` (اگر میباخ روشن باشد `N`) |
| جلو / عقب / میباخ | مثل قبل (`F B 2 1 R L Z X M N 0`) |

همه‌ی فرمان‌ها از تب «فرمان‌ها» داخل برنامه قابل ویرایش هستند.


## Build-log diagnosis and fix

The supplied GitHub Actions log fails during Kotlin compilation because the shared color
constants (`Bg`, `Panel`, `Silver`, `Steel`, `Cyan`, `Violet`, `Gold`) are declared more
than once in the GitHub revision (notably in `Theme.kt` and `CarStage.kt`). Kotlin then
reports conflicting declarations and many follow-on "overload resolution ambiguity" /
`Color.copy` errors.

In this package, the palette is centralized in `Theme.kt` and removed from `CarStage.kt`,
so these names have one declaration in this standalone project.

Important: the uploaded source ZIP and the Actions log are not the same revision. The log
also references `Intro.kt` and `Splash.kt`, which are not present in the uploaded ZIP.
If those files are still in your GitHub repository, there may be additional errors in
those files (the log specifically mentions `Intro.kt` uses of `maxWidth`). This ZIP fixes
the duplicate palette declarations in the supplied project, but does not claim to fix
source files that were not included in the ZIP.
