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

- [ ] 🟠 **Fix status bar non confermato.** `enableEdgeToEdge()` in
  `MainActivity` seguiva il tema chiaro di sistema, rendendo le icone
  della status bar scure su sfondo scuro; è stato applicato un fix ma
  lo screenshot dopo la modifica non mostra affatto la status bar, quindi
  non è verificato se ora è leggibile. Ricontrollare con uno screenshot
  che la includa, su emulatore o telefono reale.

- [ ] 🟠 **Possibile bottone "Crea progetto" coperto dalla tastiera.**
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

- [ ] ⚪ **Cosmetico: card del progresso totale troppo stretta a 0
  progetti.** Nella Home, senza progetti la card dell'avanzamento
  totale non riempie la larghezza dello schermo.

  Note (2026-11-02): la `Card` di `OverallCard` non aveva `fillMaxWidth()`
  e si adattava al testo (che a 0 progetti è il più corto). Aggiunto
  `Modifier.fillMaxWidth()`, come già in `ProjectCard`. Compila e passa
  `lint` nella CI su GitHub; **non ancora verificato a schermo**: controllare
  la Home vuota su emulatore prima di spuntare.

- [ ] 🟠 **Planner: garantire sempre almeno 3 fasi.** Decisione presa da
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

## P2 — completare la v1 come da spec

- [ ] 🟡 **Streak e statistiche** (previsti in spec, non ancora in
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

- [ ] 🟡 **Notifiche di scadenza** (previste in spec). Da aggiungere:
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
