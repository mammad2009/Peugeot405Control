# Peugeot405Control

برنامه‌ی کنترل ماکت پنوماتیک پژو ۴۰۵ از طریق بلوتوث (HC-05).

## مراحل برنامه
1. **صفحه‌ی اتصال** – فقط یک دکمه‌ی اتصال به ماژول بلوتوث با انیمیشن ظاهر می‌شود. بعد از اتصال موفق، دکمه محو می‌شود.
2. **ریموت پژو** – با انیمیشن وسط صفحه ظاهر می‌شود. بعد از زدن «باز» ریموت کوچک می‌شود و می‌رود داخل منوی برنامه.
3. **منوی اصلی** – کنترل جلو / عقب / هر دو، میباخ، ارتفاع فابریک. ریموت را از دکمه‌ی قفل بالای صفحه دوباره باز کن.

## فرمان‌هایی که به ماژول فرستاده می‌شود
| دکمه | فرمان |
|---|---|
| ریموت: قفل | `K` |
| ریموت: باز | `U` |
| هر دو بالا | `2` سپس `Z` |
| هر دو پایین | `1` سپس `X` |
| ارتفاع فابریک | `1` سپس `X` (اگر میباخ روشن باشد `N`) |
| جلو / عقب / میباخ | مثل قبل (`F B 2 1 R L Z X M N 0`) |

همه‌ی فرمان‌ها از تب «فرمان‌ها» داخل برنامه قابل ویرایش هستند.


## PEUGEOT 405 Edition updates
- Replaced the old layered vehicle-photo stage with an animated Peugeot lion / PEUGEOT 405 branded stage.
- Added a Persian/English language switch in Settings and saved the choice on the device.
- Added English labels for the primary navigation, connection status, and command editor.
- Kept the Bluetooth control and Arduino command logic intact.

### Build on GitHub
Push this project to your GitHub repository. The included GitHub Actions workflow builds a debug APK on each push (or via **Actions → Build APK → Run workflow**). Download the `Peugeot405Control-apk` artifact from the completed workflow.
