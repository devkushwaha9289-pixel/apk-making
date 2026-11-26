# Battle App – native Android (Java) APK for your PHP + MySQL site

Website aur APK **same database** use karte hain. APK sab data `https://<site>/api/` se leta hai
(matches, wallet, images, notifications). Images cPanel ke `uploads/` se hi aati hain – APK me koi image store nahi hoti.

## 1. Website (cPanel) side – pehle ye karo
1. `website-backend/` ke saare files apni site ke folder me upload karo (config.php **mat** badlo). Naya folder: `api/`.
   Changed files: `api/*`, `includes/{bootstrap,helpers,gateway,migrate,push}.php`, `admin/{firebase,push,autopush}.php`.
2. Site ka koi bhi page ek baar kholo (database khud upgrade ho jata hai – `api_tokens` table banti hai).
3. Test: browser me `https://your-site/api/?a=config` kholo – JSON aana chahiye.

## 2. Firebase (push ke liye, ek baar)
1. Firebase console → **wahi project** jo website push me use hota hai → Project settings → *Add app → Android*.
   Package name = `config.properties` ka `APP_ID` (default `com.m7s.battle`). `google-services.json` **download karne ki zaroorat nahi**.
2. Us Android app ka **App ID** (`1:123…:android:abc…`) copy karo.
3. Admin panel → **Push / Firebase → 2b · Android app (APK) config** → App ID paste → Save.
   (Server key / service account pehle se connected honi chahiye – step 1 of same page.)

## 3. APK banao
`config.properties` edit karo: `BASE_URL`, `APP_NAME` (phone me icon ke neeche ka naam), `APP_ID`, `VERSION_*`.
Icon badalna ho: `bash set-icon.sh my-icon.png`.

**VPS (Ubuntu/Debian, 4 GB RAM): ** `bash build-vps.sh`
**Termux: ** `bash build-termux.sh` (best-effort; slow, 3 GB RAM chahiye. Na chale to VPS use karo.)

APK `dist/` me milti hai. Use cPanel me upload karke link Admin > Settings > APK link me daalo (website ka Download page wahi dikhayega).
`release.keystore` + `keystore.properties` ka backup rakho – update isi key se sign hona chahiye.

## 4. Push notifications (admin panel)
* **Auto push**: Admin > Auto Push – money added, withdrawal, match win/room/cancel, lucky draw… sab event par website aur APK dono ko apne aap jata hai (banner ke saath).
* **Manual push**: Admin > Send Push → *Send to devices*: All / **APK only** / Website only. Send dabane par **confirmation popup** aata hai (kitne devices, title, message) aur bhejne ke baad "✅ Confirmed: delivered to X of Y device(s)" dikhta hai. Latest pushes table me sent/failed bhi.
* User ke phone me Profile → Push notifications switch (ON/OFF) hai.
* Force update: Firebase page me *Minimum APK version code* badha do – purane APK update screen par ruk jayenge.

## UI: website jaisa (XML + Java)
APK ka look website ke `assets/css/app.css` se copy kiya gaya hai: same colors (`res/values/colors.xml`), same fonts (FBSans/Squada, `assets/fonts`),
same icons (website ke SVG paths -> `res/drawable/ic_*.xml`), same structure: **Earn / Play / Profile** tabs, welcome bar + coin pill + bell,
purple tabs, white match card (`res/layout/item_match.xml`), teal `.sec` boxes, wallet hero card, slot grid, join page, withdraw grid, scratch card, etc.
Layout XML: `res/layout/*.xml` ; shapes: `res/drawable/bg_*.xml` ; logic: `java/com/m7s/battle/*.java`.
Admin panel se jo images/text/settings badlte ho (logo, banner, game images, announcement, rules, ...) wo APK me bhi wahi aate hain.
Lottie animations website jaisi hi hain (assets/anim, lottie-android): Earn, Watch & Earn, Withdraw, Money Converter, Maintenance, Blocked. Abhi nahi: website ka Theme picker.

## APK me kya hai
Splash (server se naam/logo/maintenance/update check), Login/Register (+email OTP agar admin ne ON kiya), Forgot password,
Home (slider, ticker, games), Matches (Ongoing/Upcoming/Result), Match details (prize, breakup, room id/password sirf allowed time par, players, rules),
Slot picker + Join (wallet choose, game name/UID), Wallet (Add coin: online gateway / manual UPI+UTR, Withdraw, Money converter, Transactions),
Leaderboard, Lucky Draw, Scratch card, Refer & earn, Support tickets, Notifications, Profile (photo upload auto-compress), pages (About/Privacy/Refund), Logout.

## Abhi APK me nahi (website me hai)
Watch & Earn (AdMob/video), themes picker, slot-booking me multi-winner lucky draw display. Ye next step me add ho sakte hain.

## Zaroori note
Ye code is chat me compile/test nahi ho paya (yahan Android SDK/internet nahi tha). Pehli build me koi chhoti compile error aaye to error text bhej dena, turant fix kar dunga.


## Payment WebView
Online gateway ka payment page ab app ke andar WebView (`WebActivity`) me khulta hai: UPI apps (PhonePe/GPay/Paytm) `upi://` / `intent://` se khulte hain, gateway se wapas aate hi `gateway_return.php` ka result app me dikhta hai (Success / Verifying / Under review / Failed), aur wallet apne aap reload hota hai. Admin / Staff panel bhi isi WebView me khulta hai.
