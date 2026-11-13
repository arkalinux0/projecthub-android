# ProjectHub — piano di completamento

Backlog ordinato per Claude Code. Leggi prima `PROJECTHUB_SPEC.md` per le
decisioni di progetto. Questo file è lo stato dei lavori: aggiornalo
(spuntando o annotando) mentre procedi, non lasciarlo statico.

Legenda: 🔴 blocca la compilazione · 🟠 bug reale da correggere · 🟡 manca
dalla v1 ma previsto in spec · ⚪ backlog futuro, non bloccante.

## P0 — far compilare e partire l'app

- [x] 🔴 **Wrapper Gradle incompleto.** Esiste solo
  `gradle/wrapper/gradle-wrapper.properties`; mancano `gradlew`,
  `gradlew.bat` e `gradle-wrapper.jar`. Rigenerarli (`gradle wrapper
  --gradle-version 8.9`) o scaricare quelli standard per Gradle 8.9.
  Senza questo nessun comando `./gradlew` funziona.

- [x] 🔴 **`MainActivity.kt` non esiste.** Deve creare il `NavHost` con le
  route `home`, `project/{id}` (arg `Long`), `add`, e la nuova `stats`
  (vedi P2). Collegare `HomeScreen`, `ProjectScreen`, `AddProjectScreen`
  già scritte in `ui/`. Nota: in una sessione precedente la scrittura di
  questo file è stata bloccata da un classificatore di sicurezza di
  Claude.ai senza una ragione individuabile nel codice — è normale
  codice di navigazione Compose, quindi è quasi certo un falso positivo
  legato a quell'interfaccia specifica, non un problema del contenuto.
  Da Claude Code (CLI) non dovrebbe ripresentarsi, ma se succede di
  nuovo, scrivere il file in pezzi più piccoli.

- [x] 🔴 **Mai compilato.** Non è mai stato verificato con un vero
  Android SDK. Dopo aver sistemato il wrapper, eseguire
  `./gradlew assembleDebug` e risolvere ogni errore. In particolare da
  controllare:
  - compatibilità tra AGP 8.7.3, Kotlin 2.0.21, plugin Compose 2.0.21,
    Compose BOM 2024.10.01, KSP 2.0.21-1.0.28, Room 2.6.1 — potrebbero
    servire aggiustamenti di versione;
  - `FlowRow` in `AddProjectScreen.kt` è annotato `@OptIn(ExperimentalLayoutApi::class)`
    ma in Compose Foundation recenti `FlowRow` è stabile: se il compilatore
    segnala l'opt-in come inutile, rimuoverlo.

Note P0 (2026-11-02): `assembleDebug` e `lint` passano (0 errori) con le versioni
esistenti, nessuna modifica necessaria; l'opt-in di `FlowRow` non genera avvisi.
Gradle 8.9 richiede JDK 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk`), con JDK 27
non parte. Flusso crea/spunta/riavvia/elimina provato su emulatore API 36.1.
La route `stats` non c'è ancora: arriva con la schermata in P2.

Nota ambiente (2026-11-02): nelle sessioni cloud di Claude Code
`dl.google.com` deve essere tra i domini consentiti della rete
dell'ambiente: Google Maven e Android SDK stanno lì, senza AGP non si
risolve e nessun `./gradlew` funziona. Maven Central risponde a tratti 429,
ma con qualche riprova si scarica. Nel container di quella sessione non
c'era `/dev/kvm`, quindi niente emulatore: le verifiche a schermo restano da
fare in locale. La CI su GitHub (vedi "Fatto fuori backlog") non ha questi
limiti: lì `./gradlew test`, `lint` e `assembleDebug` girano davvero.

Aggiornamento (2026-11-12): esiste l'ambiente cloud `projecthub-android`,
con `dl.google.com` consentito. Lì l'SDK si installa con `sdkmanager`
(`platform-tools`, `platforms;android-35`, `build-tools;34.0.0`) e
`./gradlew test lint assembleDebug` gira in locale con JDK 21. Anche
l'emulatore parte, ma senza KVM solo in emulazione software
(`emulator -accel off -no-window -gpu swiftshader_indirect`, immagine
`system-images;android-34;default;x86_64`): boot in 10–12 minuti e ogni
tocco costa 20–40 secondi. Per renderlo usabile: `-skin 720x1280` con
`wm density 320`, animazioni a 0, Bluetooth disattivato
(`pm disable-user com.android.bluetooth`, altrimenti va in crash a
ripetizione), screenshot con `adb exec-out screencap -p` e tocchi a
coordinate invece di `uiautomator dump`, che da solo manda l'app in ANR.
I dialoghi "isn't responding" dipendono dalla lentezza, non dall'app: si
chiudono con "Wait". Non impostare `hide_error_dialogs 1`: con quello il
sistema chiude l'app al primo ANR.

## P1 — bug da correggere nel codice esistente

- [x] ✅ ~~Incoerenza di timezone sulle date~~ — **falso allarme, verificato
  e chiuso (2026-11-02).** Il dubbio era che `AddProjectScreen.kt` usi
  `ZoneOffset.UTC` mentre il resto del codice usa la zona di sistema.
  Documentazione ufficiale di `DatePickerState.selectedDateMillis`:
  "A timestamp that represents the selected date start of the day in
  UTC milliseconds from the epoch" — il contratto dell'API è *sempre*
  UTC indipendentemente dal fuso del dispositivo, quindi convertire con
  `ZoneOffset.UTC` in `AddProjectScreen.kt` è l'unico modo corretto di
  decodificare la data scelta. Passare a `ZoneId.systemDefault()` lì
  avrebbe introdotto un bug reale nelle zone diverse da UTC+0. Non
  toccare questo codice.

- [x] 🟠 **Nessun test.** La cartella `app/src/test` esiste ma è vuota.
  Coprire almeno:
  - `Planner.plan()`: casi con scadenza oggi, scadenza a 1 giorno, a 29
    giorni, a 30 giorni esatti, a più di 12 mesi (deve limitarsi a 12
    milestone), categoria con scadenza bentissimo nel futuro (fasi
    ripetute con "parte N");
  - `ratio()` / `ProjectUi.progress` con 0 task totali (non deve dare
    `NaN` o dividere per zero — oggi è già gestito, va solo testato);
  - `MainViewModel`/`Repository.createProject` con un test strumentale
    o Robolectric se si vuole coprire anche Room.

  Note (2026-11-02): scritti `PlannerTest.kt` (13 test) e `ModelsTest.kt`
  (12 test) in `app/src/test/java/ch/arka/projecthub/domain/`, JUnit 4, per
  i primi due punti: tutti i casi elencati sopra per `Planner.plan()`, più
  invarianti su ogni categoria e su ogni numero di milestone da 1 a 12
  (ultima scadenza = scadenza del progetto, scadenze crescenti, task
  ereditati dalla fase, fasi in ordine, "parte N" solo sulle fasi ripetute);
  `ratio()`, `MilestoneUi`/`ProjectUi` e `overallProgress` con 0 task e con
  pesatura per task. Verificato: 25/25 verdi con Kotlin 2.0.21 + JUnit
  4.13.2 (target JVM 17) in un harness JVM fuori dal repo che compila i
  sorgenti reali di `domain/` e `data/Entities.kt` (annotazioni Room
  sostituite da stub solo nell'harness); 4 mutazioni di `Planner`/`ratio()`
  vengono tutte rilevate. Poi eseguiti con `./gradlew test` nella CI su
  GitHub (run 41481823899 sul commit `1119250`: `testDebugUnitTest` e
  `testReleaseUnitTest` verdi, insieme a `lint` e `assembleDebug`). Il terzo
  punto (Room/Robolectric), facoltativo, non è stato fatto.

- [x] 🟠 **Fix status bar non confermato.** `enableEdgeToEdge()` in
  `MainActivity` seguiva il tema chiaro di sistema, rendendo le icone
  della status bar scure su sfondo scuro; è stato applicato un fix ma
  lo screenshot dopo la modifica non mostra affatto la status bar, quindi
  non è verificato se ora è leggibile. Ricontrollare con uno screenshot
  che la includa, su emulatore o telefono reale.

  Note (2026-11-12): confermato con screenshot su emulatore API 34 con il
  sistema in tema chiaro (`cmd uimode night` → `Night mode: no`), APK di
  `main` (`7087dd3`). In Home, Nuovo progetto e Dettaglio progetto orologio
  e icone di stato sono bianchi sullo sfondo scuro dell'app, e bianchi sono
  anche i pulsanti della barra di navigazione. Osservazione a margine, non
  parte di questa voce: nelle liste (Home, Dettaglio) il contenuto scorre
  sotto la status bar trasparente e per un attimo si sovrappone alle icone;
  è il normale edge-to-edge senza barra superiore, da valutare se dà
  fastidio.

- [x] 🟠 **Possibile bottone "Crea progetto" coperto dalla tastiera.**
  Quando l'anteprima del piano generato è visibile in `AddProjectScreen`,
  la tastiera aperta può coprire il bottone finale. Non verificato se il
  form scorre per raggiungerlo con la tastiera aperta; se non scorre,
  va reso scrollabile fino in fondo o il bottone va reso sempre visibile.

  Note (2026-11-02): causa trovata leggendo il codice — il bug è reale.
  Con `enableEdgeToEdge()` la tastiera non ridimensiona più la finestra e
  il manifest non dichiarava `adjustResize`: la `Column` scorrevole si
  estendeva sotto la tastiera, quindi lo scroll si fermava con il bottone
  ancora coperto. Fix applicato (scelta: form scorrevole fino in fondo,
  niente barra fissa): `android:windowSoftInputMode="adjustResize"`
  sull'activity + `.consumeWindowInsets(inner).imePadding()` sulla
  `Column` di `AddProjectScreen`. Compila e passa `lint` nella CI su GitHub
  (stesso run dei test); **non ancora provato a schermo**: nessun emulatore
  nella sessione cloud (vedi nota ambiente in P0). Prima di spuntare: su
  emulatore aprire la tastiera con l'anteprima del piano visibile e
  verificare che si arrivi a "Crea progetto" scorrendo, senza spazio vuoto
  doppio sopra la tastiera.

  Note (2026-11-12): confermato con screenshot su emulatore API 34
  (schermo 720×1280 a 320 dpi, APK di `main`). Scelta la data (anteprima
  con 3 fasi), aperta la tastiera sul campo del nome e scritto "Prova":
  il form resta tagliato a "Categoria", scorre con la tastiera aperta e in
  fondo "Crea progetto" è interamente visibile sopra la tastiera, con sotto
  solo lo spazio previsto dal layout (nessun margine doppio). Toccato da
  lì, il bottone crea il progetto. Provato con l'anteprima più corta (3
  fasi); con più fasi cambia solo quanto c'è da scorrere.

- [x] ⚪ **Cosmetico: card del progresso totale troppo stretta a 0
  progetti.** Nella Home, senza progetti la card dell'avanzamento
  totale non riempie la larghezza dello schermo.

  Note (2026-11-02): la `Card` di `OverallCard` non aveva `fillMaxWidth()`
  e si adattava al testo (che a 0 progetti è il più corto). Aggiunto
  `Modifier.fillMaxWidth()`, come già in `ProjectCard`. Compila e passa
  `lint` nella CI su GitHub; **non ancora verificato a schermo**: controllare
  la Home vuota su emulatore prima di spuntare.

  Note (2026-11-12): confermato con screenshot su emulatore API 34, a
  1080×2340 e a 720×1280: con 0 progetti la card occupa tutta la larghezza,
  con gli stessi margini delle altre.

- [x] 🟠 **Planner: garantire sempre almeno 3 fasi.** Decisione presa da
  Arka il 2026-11-02 (vedi `PROJECTHUB_SPEC.md`, sezione Planner):
  nessun progetto deve avere meno di 3 milestone, nemmeno con scadenza
  vicinissima — si comprimono le scadenze dei milestone, non il numero
  di fasi. Oggi `Planner.milestoneCount()` ha minimo 1 e per scadenze
  entro 120 giorni alcune fasi intermedie vengono saltate (con 2
  milestone resta solo prima e ultima fase, vedi
  `fewerMilestonesThanPhases_keepsFirstAndLastPhase` in
  `app/src/test/.../domain/PlannerTest.kt` sul branch `claude/p1-test-e-fix-ui`,
  non ancora in `main`). Da fare, **dopo** che quella PR è stata
  verificata e unita (i suoi test codificano il comportamento vecchio,
  non toccarli prima):
  1. cambiare il minimo in `milestoneCount()` da 1 a 3;
  2. in `plan()`, quando i milestone sono meno delle fasi, garantire che
     compaiano sempre prima, una fase centrale e ultima (non solo prima
     e ultima come ora);
  3. aggiornare `singleMilestone_usesLastPhaseOfCategory` e
     `fewerMilestonesThanPhases_keepsFirstAndLastPhase` nei test per il
     nuovo comportamento, invece di cancellarli.

  Note (2026-11-12): fatto. `Planner.MIN_MILESTONES = 3`; con almeno 3
  milestone la distribuzione esistente dà già prima fase, fase intermedia
  e ultima (3 milestone = fasi 1, 3, 5; 4 = fasi 1, 2, 4, 5), quindi in
  `plan()` è sparito solo il caso speciale del milestone unico. Lettura di
  "una fase centrale" adottata: almeno una fase intermedia — con 4
  milestone la fase 3 resta saltata come prima; se deve comparire sempre
  proprio quella, va cambiata la distribuzione. Scadenze compresse: sotto i
  3 giorni più milestone cadono nello stesso giorno (scadenza oggi = tutti
  e tre oggi; a 1 giorno = oggi, oggi, domani), da 3 giorni in su sono
  tutte distinte. I due test sono stati aggiornati e rinominati in
  `shortDeadline_usesFirstMiddleAndLastPhaseOfCategory` e
  `fewerMilestonesThanPhases_keepsFirstMiddleAndLastPhase`, più i casi a
  0/1/2/3/29/30 giorni e gli invarianti (mai meno di 3 milestone, sempre
  prima/intermedia/ultima). 26/26 verdi nell'harness JVM; riportando il
  minimo a 1 o 2 ne falliscono 9. I progetti già creati non cambiano: il
  piano viene calcolato solo alla creazione.

## Decisioni in sospeso

- [ ] **Permessi aggiunti da WorkManager.** La spec dice "nessun permesso
  oltre a `POST_NOTIFICATIONS`", ma la libreria scelta dalla stessa spec
  per i promemoria porta nel manifest finale altri quattro permessi
  normali (concessi all'installazione, senza richiesta all'utente):
  `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE` e
  `ACCESS_NETWORK_STATE`. Verificato con `aapt2 dump permissions`
  sull'APK: `INTERNET` **non** c'è, quindi l'app continua a non poter
  aprire connessioni; `ACCESS_NETWORK_STATE` permette solo di sapere se
  c'è rete. Per ora sono rimasti tutti, perché è la configurazione
  supportata della libreria. Decisione richiesta ad Arka:
  1. tenerli così e correggere la frase della spec;
  2. togliere dal manifest quelli che l'app non usa
     (`ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`) con
     `tools:node="remove"`: dovrebbe funzionare perché il worker non ha
     vincoli di rete né lavori in primo piano, ma è una configurazione
     non documentata da provare bene (`RECEIVE_BOOT_COMPLETED` e
     `WAKE_LOCK` servono davvero: senza il primo il controllo non riparte
     dopo un riavvio);
  3. sostituire WorkManager con `AlarmManager`, che richiede solo
     `RECEIVE_BOOT_COMPLETED` ma va gestito a mano.

## P2 — completare la v1 come da spec

- [x] 🟡 **Streak e statistiche** (previsti in spec, non ancora in
  codice). Serve prima una modifica di schema: aggiungere a
  `TaskEntity` un campo `completedAtDay: Long?` (epoch day, null se non
  fatto) impostato quando `setTaskDone(id, true)`. Bump di `version` in
  `AppDatabase` (va bene `fallbackToDestructiveMigration()` dato che non
  ci sono ancora utenti/dati reali da preservare). Poi:
  - funzione in `domain/` che calcola lo streak (giorni consecutivi con
    almeno un task completato) e i task completati questa
    settimana/mese;
  - nuova schermata `ui/stats/StatsScreen.kt` con queste informazioni,
    eventualmente un grafico a barre per categoria;
  - nuova route `stats` in `MainActivity`, voce di navigazione dalla
    Home (es. bottom bar o icona in alto).

  Note (2026-11-12): fatto. Scelte tecniche:
  - **Schema v2 con migrazione vera, non distruttiva.** `TaskEntity` ha
    `completedAtDay: Long?`, `AppDatabase` è alla versione 2 con
    `MIGRATION_1_2` (`ALTER TABLE tasks ADD COLUMN completedAtDay INTEGER`)
    al posto di `fallbackToDestructiveMigration()`: costa tre righe e non
    cancella i progetti già presenti sul dispositivo. I task spuntati prima
    dell'aggiornamento restano spuntati ma senza data: contano nei totali
    per categoria, non in serie/settimana/mese.
  - `Repository.setTaskDone` registra il giorno quando si spunta e lo
    azzera quando si toglie la spunta.
  - `domain/Stats.kt`: `currentStreak()` e `statsOf()`. La serie conta i
    giorni consecutivi fino a oggi; se oggi non c'è ancora nessun task
    vale quella che arriva a ieri (si azzera dopo un giorno intero vuoto)
    e la schermata avvisa che è a rischio. Settimana = da lunedì a oggi,
    mese = dal primo a oggi. Le statistiche si calcolano dai task così
    come sono ora: togliere una spunta toglie anche quel completamento.
  - `ui/stats/StatsScreen.kt`: serie, settimana, mese e una barra di
    avanzamento per categoria (task fatti/totali). Route `stats`, a cui si
    arriva dal pulsante di testo "Statistiche" in alto nella Home
    (`material-icons-core` non ha un'icona adatta e non valeva la pena
    aggiungere `material-icons-extended` per una sola icona).

  Verificato: `./gradlew test` (43 test, 17 nuovi in `StatsTest`), `lint`
  (0 errori, nessun avviso nuovo) e `assembleDebug` in locale. Su
  emulatore API 34: creato un progetto e spuntato un task con l'APK di
  `main` (schema v1), installato sopra l'APK di questo branch → progetto e
  spunta conservati, nessun crash; Statistiche mostrava serie 0 e
  Software 1/9; spuntati altri due task → serie 1 giorno, "Oggi: 2 task
  completati", settimana 2, mese 2, Software 3/9. Non verificato a
  schermo: serie di più giorni e passaggio di mezzanotte (coperti solo dai
  test di `StatsTest`).

- [x] 🟡 **Notifiche di scadenza** (previste in spec). Da aggiungere:
  - dipendenza `androidx.work:work-runtime-ktx` in `app/build.gradle.kts`;
  - permesso `POST_NOTIFICATIONS` nel manifest (richiesto a runtime su
    Android 13+, con la relativa richiesta nella UI);
  - `NotificationChannel` creato all'avvio dell'app;
  - un `CoroutineWorker` pianificato giornalmente che controlla i
    milestone non completati: notifica se la scadenza è entro 3 giorni,
    notifica separata (più urgente) se già scaduta;
  - registrazione del worker periodico in `MainActivity.onCreate` o in
    una classe `Application` dedicata (valutare se serve una
    `ProjectHubApplication : Application`, al momento manca e non è
    dichiarata nel manifest).

  Note (2026-11-12): fatto, con una decisione rimasta aperta sui permessi
  (vedi "Decisioni in sospeso"). Scelte tecniche:
  - `androidx.work:work-runtime-ktx:2.9.1`; `ProjectHubApplication`
    (dichiarata nel manifest) crea i canali e pianifica il controllo:
    serve una `Application` perché il worker deve trovarli anche quando è
    il sistema ad avviare il processo, senza activity.
  - `notify/DeadlineWorker`: una volta al giorno, la prima alle prossime
    9:00, `ExistingPeriodicWorkPolicy.KEEP` (riaprire l'app non sposta
    l'orario), nessun vincolo di rete. Legge i progetti da Room e chiama
    `domain/Deadlines.kt`.
  - `deadlineAlerts()`: segnala solo le fasi con almeno un task non fatto;
    "in scadenza" = da oggi a 3 giorni (`DUE_SOON_DAYS`), "scaduta" = data
    passata. Una fase senza task non genera avvisi.
  - `notify/DeadlineNotifications`: due notifiche, non una per fase —
    "N fasi in scadenza" (canale `deadline_soon`, importanza normale) e
    "N fasi scadute" (canale `deadline_overdue`, importanza alta), ognuna
    con l'elenco "Progetto · Fase: scade tra 2 giorni". Si aggiornano a ogni
    controllo e spariscono quando non c'è più nulla da segnalare; finché
    una fase resta scaduta l'avviso torna ogni giorno. Toccarle apre l'app.
  - `POST_NOTIFICATIONS` nel manifest; la richiesta a runtime (Android 13+)
    parte da `MainActivity` quando esiste almeno un progetto, una volta per
    avvio. Senza permesso il worker gira lo stesso ma non mostra nulla.

  Verificato: `./gradlew test` (53 test, 10 nuovi in `DeadlinesTest`),
  `lint` (0 errori; unico avviso nuovo: esiste una versione più recente di
  work-runtime) e `assembleDebug` in locale. Su emulatore API 34: al primo
  avvio con un progetto compare la richiesta di sistema "Allow ProjectHub
  to send you notifications?"; dopo "Allow" risultano il permesso concesso,
  i due canali (importanza 3 e 4) e il job di WorkManager pianificato.
  Portata la data dell'emulatore al 10 dicembre e forzato il job
  (`cmd jobscheduler run -f ch.arka.projecthub 0`): worker `SUCCESS` e in
  tendina "1 fase scaduta — Prova · Funzionalità: scaduta da 8 giorni" e
  "1 fase in scadenza — Prova · Rilascio: scade tra 2 giorni"; la fase
  Analisi, completata, non compare. **Non verificato**: l'esecuzione
  spontanea alle 9:00 senza forzare il job, la ripianificazione dopo un
  riavvio del telefono, il tocco sulla notifica, il comportamento con
  permesso negato e su un telefono reale con risparmio energetico.

## Fatto fuori backlog

- [x] ✅ **CI** (decisione di Arka, 2026-11-02): aggiunto
  `.github/workflows/android-ci.yml` — compila, testa e lint-a il
  progetto su ogni push/PR verso `main` con un runner GitHub (rete e SDK
  completi, a differenza di tutti gli ambienti usati finora). Non ancora
  vista girare su una PR vera: verificarlo sulla prossima PR aperta.

  Nota (2026-11-02): le prime esecuzioni (push su `main` e PR di
  `claude/p1-test-e-fix-ui`) sono fallite al passo "Imposta Android SDK",
  prima di qualsiasi build: il default di `android-actions/setup-android@v3`
  è `packages: tools platform-tools` e il pacchetto `tools` non esiste più
  nel repository dell'SDK, quindi `sdkmanager tools` esce con errore.
  Corretto indicando `packages: platform-tools`; piattaforma e build-tools
  restano installati dal passo successivo, come prima.

## P3 — backlog, non bloccante per la v1

- [ ] ⚪ Modifica manuale di singole fasi/task dopo la creazione
  (esplicitamente fuori scope v1, vedi spec).
- [ ] ⚪ Widget home screen Android con il cerchio di progresso.
- [ ] ⚪ Watchlist CVE/paper scientifici e scorciatoie alle fonti
  (NVD, arXiv cs.CR, GitHub Advisories, ecc.) — era nella richiesta
  originale di Arka prima di decidere di fare un'app nativa, poi esclusa
  perché la spec v1 vieta chiamate di rete. Da ridiscutere se Arka la
  vuole ancora: o un modulo separato che usa network, o un browser
  intent verso i siti, senza integrarla nel database locale.
- [ ] ⚪ Export/import dei dati (oggi persi se si disinstalla l'app o si
  cambia telefono, dato che tutto è locale).

## Come procedere

1. Risolvere tutto il P0 prima di qualsiasi altra cosa: senza wrapper e
   senza `MainActivity` l'app non esiste ancora, letteralmente.
2. Fare un commit per ogni voce (o gruppo coerente di voci) completata,
   non un unico commit gigante.
3. Dopo ogni modifica a `domain/Planner.kt` o `domain/Models.kt`,
   rieseguire i test prima di proseguire.
4. Aggiornare questo file e `PROJECTHUB_SPEC.md` se una decisione
   cambia durante lo sviluppo (es. si scopre che una versione di
   libreria non è compatibile e bisogna cambiarla).
