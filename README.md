# PattNG

v2rayNG fork for Iranians

**تغییرات v2rayNG:**

* اضافه شدن cipherSuites و فینگرپرینت unsafe در تنظیمات و شیرلینک.

**تغییرات Xray-core:**

* امکان اتصال به کانفیگ‌های غیر رمزنگاری شده برای آدرس‌های عمومی در VLESS و TROJAN

## کلاینت ساده Free VPN

این پروژه علاوه بر رابط کامل و پیشرفته v2rayNG، یک صفحه ورودی ساده هم دارد
(`SimpleVpnActivity`) که هنگام باز کردن اپ نمایش داده می‌شود: فقط لیست سرورها،
دکمه Refresh، انتخاب سرور، و Connect/Disconnect. رابط کامل قبلی حذف نشده و
همچنان در کد وجود دارد، فقط از صفحه اصلی در دسترس نیست.

### تغییر منبع سرورها (URL)

فایل: `V2rayNG/app/src/main/java/com/v2ray/ang/RemoteConfig.kt`

متغیر: `SERVER_LIST_URL`

مقدار این متغیر را به آدرس Raw فایل متنی سرورهای خودتان در گیت‌هاب تغییر دهید
(هر خط یک کانفیگ، مثلاً `vless://...`). سپس دوباره APK را بیلد بگیرید.

### تغییر زمان اعتبار Cache

همان فایل `RemoteConfig.kt`، متغیر: `REMOTE_DATA_EXPIRY_MINUTES`

این عدد، به دقیقه، مدت زمانی است که لیست دانلودشده — در صورت عدم دسترسی به
اینترنت برای دریافت لیست جدید — همچنان معتبر و قابل استفاده برای اتصال باقی
می‌ماند. بعد از این مدت، بدون یک Refresh موفق، اتصال جدید مجاز نیست.

مقدار `REQUEST_TIMEOUT_SECONDS` هم زمان Timeout درخواست دانلود لیست سرورهاست.

### گرفتن APK بدون Android Studio (GitHub Actions)

این پروژه از قبل یک Workflow کامل در مسیر `.github/workflows/build.yml` دارد که
APK را در GitHub Actions می‌سازد:

1. به تب **Actions** مخزن گیت‌هاب خودتان بروید.
2. روی Workflow با نام **Build APK** کلیک کنید.
3. روی **Run workflow** بزنید (این همان `workflow_dispatch` است) — یا صرفاً به
   شاخه `master` پوش کنید تا به‌صورت خودکار اجرا شود.
4. بعد از پایان اجرا، APKهای هر معماری (arm64-v8a، armeabi-v7a، x86، universal)
   به‌صورت Artifact در همان صفحه اجرای Workflow قابل دانلود هستند.

توجه: این Workflow برای امضای نسخه Release به Secretهای گیت‌هاب
(`APP_KEYSTORE_BASE64`، `APP_KEYSTORE_PASSWORD`، `APP_KEYSTORE_ALIAS`،
`APP_KEY_PASSWORD`) نیاز دارد. اگر این Secretها را در مخزن خودتان تنظیم نکرده‌اید،
باید آن‌ها را قبل از اجرای Workflow در Settings → Secrets and variables →
Actions مخزن اضافه کنید، یا مرحله امضا را برای بیلد Debug ساده متناسب با نیاز
خودتان تغییر دهید.

## حمایت

اگر کارهای بنده باعث دسترسی شما به اینترنت آزاد شده است ممنون میشم حمایتی هم از اینجانب انجام دهید

`USDT (BEP20)`: 0x76a768B53Ca77B43086946315f0BDF21156bF424

`USDT (TRC20)`: TU5gKvKqcXPn8itp1DouBCwcqGHMemBm8o

`TON (TON)`: UQAc-mZB3y7uxWHKiMmq0ORZEYgycWDWZ4V1k73HsXvTJx-i

@patterniha
