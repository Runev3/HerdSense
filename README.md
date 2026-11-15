# HerdSense

Android-app (Kotlin + Jetpack Compose) voor het opvolgen van een melkveestapel.

## Functies

- **Dashboard** – aantal koeien, voldoende / te weinig / niet gegeten, wat je morgen moet bijvullen per voersoort, niet-gegeten koeien en groepen (in heat / niet gegeten).
- **Kudde** – zoeken op werknummer, filteren op groep of voeropname, koe toevoegen.
- **Koe toevoegen** – oormerk nieten, oormerk scannen (barcode/QR via de camera) of intypen, leeftijd en groep invullen.
- **Voer** – vandaag gevreten, morgen bijvullen, voerkosten (dag / maand / koe), voorraad en inkoopadvies voor 7–60 dagen. Rantsoen, prijs en voorraad zijn aanpasbaar.
- **Meldingen** – koeien die niet gegeten hebben, met een **Zoek**-knop die het lampje op de oorsensor laat knipperen.
- **Koe-detail** – activiteit, temperatuur, gedrag (per dag / per uur), herkauwen en vreten in grafieken.

> De app draait nu op **demodata**. Het zoek-lampje is in de app gesimuleerd; koppeling met echte oorsensoren volgt.

## Bouwen

Vereist Android Studio (JDK 17+) en de Android SDK (compileSdk 34).

```bash
./gradlew assembleDebug
```

De APK staat daarna in `app/build/outputs/apk/debug/app-debug.apk`.
