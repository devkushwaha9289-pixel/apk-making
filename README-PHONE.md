# Phone se APK banao (GitHub Actions, free)

1. github.com par account banao -> New repository -> **Private** -> naam `battleapp`.
2. Repo me **Add file > Upload files**: is zip ko extract karke `BattleApp` folder ke andar ki SAARI files/folders upload karo
   (`.github` folder zaroor, ye hidden hota hai - Files app me "show hidden files" ON karo). Commit karo.
3. `config.properties` me BASE_URL / APP_NAME / APP_ID pehle edit kar lo (GitHub me hi pencil icon se).
4. **Actions** tab > "Build APK" > **Run workflow**. 5-10 min lagte hain.
5. Run khatam hone par neeche **Artifacts > apk-and-keystore** download karo (zip) -> andar APK.
6. IMPORTANT: artifact me `release.keystore` + `keystore.properties` bhi hain. Dono repo me upload kar do
   (private repo) taaki agli builds usi key se sign hon, warna update install nahi hoga.
