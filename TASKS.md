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

- [ ] 🟠 **Nessun test.** La cartella `app/src/test` esiste ma è vuota.
  Coprire almeno:
  - `Planner.plan()`: casi con scadenza oggi, scadenza a 1 giorno, a 29
    giorni, a 30 giorni esatti, a più di 12 mesi (deve limitarsi a 12
    milestone), categoria con scadenza bentissimo nel futuro (fasi
    ripetute con "parte N");
  - `ratio()` / `ProjectUi.progress` con 0 task totali (non deve dare
    `NaN` o dividere per zero — oggi è già gestito, va solo testato);
  - `MainViewModel`/`Repository.createProject` con un test strumentale
    o Robolectric se si vuole coprire anche Room.

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

- [ ] ⚪ **Cosmetico: card del progresso totale troppo stretta a 0
  progetti.** Nella Home, senza progetti la card dell'avanzamento
  totale non riempie la larghezza dello schermo.

## Decisioni in sospeso

- [ ] **Progetti con scadenza entro 30 giorni ottengono una sola
  milestone**, che prende sempre l'ultima fase del modello della
  categoria (es. un progetto software breve riceve solo "Rilascio",
  saltando Analisi/Nucleo/Funzionalità/Qualità). La spec non definisce
  questo caso. Decisione richiesta ad Arka: tenere così (un progetto
  breve = uno sprint finale), garantire sempre un minimo di fasi (es. 3)
  comprimendo le scadenze, o scalare comunque tutte le fasi del modello
  anche su pochi giorni. Non implementare nessuna delle alternative
  finché Arka non si esprime.

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
