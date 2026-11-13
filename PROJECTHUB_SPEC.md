# ProjectHub — specifica v1

App Android nativa (Kotlin + Jetpack Compose) per gestire obiettivi personali,
divisi automaticamente in fasi mensili e task settimanali, con grafici di
avanzamento a cerchio per progetto e per categoria.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Room (SQLite locale) — nessun account, nessun backend
- Navigation Compose
- WorkManager per i promemoria di scadenza
- minSdk 26, targetSdk 35
- Nessuna dipendenza di rete

## Modello dati

**Project**: id, nome, obiettivo (testo libero), categoria, data creazione,
scadenza finale.

**Milestone** (fase): id, projectId, titolo, posizione (ordine), scadenza.
Generate automaticamente alla creazione del progetto, non modificabili
singolarmente in v1 (si può solo eliminare l'intero progetto).

**Task**: id, milestoneId, projectId, titolo, fatto (bool), data di
completamento (`completedAtDay`, epoch day, null se non fatto; per streak e
statistiche). Introdotta con lo schema v2 del database tramite migrazione
non distruttiva: i task spuntati prima non hanno data.

## Categorie e modello di fasi

Enum `Category` con 5 valori, ognuno con colore e lista ordinata di `Phase`
(titolo + task di default):

1. **Scuola** — Pianificazione → Studio e ricerca → Svolgimento → Revisione → Consegna
2. **Security** — Fondamenta → Studio teorico → Pratica in lab → Automazione e tooling → Writeup
3. **Hardware** — Progetto e componenti → Prototipo → Integrazione → Rifinitura → Completamento
4. **Software** — Analisi → Nucleo → Funzionalità → Qualità → Rilascio
5. **Personale** — Partenza → Costruire l'abitudine → Metà strada → Spinta finale → Traguardo

## Planner (scomposizione automatica)

Logica deterministica, nessuna IA:

- numero di milestone = giorni tra oggi e scadenza / 30, arrotondato su,
  **minimo 3** (deciso il 2026-11-02: un progetto non deve mai avere meno di
  3 fasi, nemmeno con scadenza vicinissima — si comprimono le scadenze dei
  milestone, non il numero di fasi), massimo 12;
- le fasi del modello della categoria vengono distribuite uniformemente sui
  milestone generati (se i milestone sono più delle fasi, le fasi si
  ripetono con suffisso "parte N"; se sono meno, compaiono comunque
  almeno la prima, una intermedia e l'ultima fase: 3 milestone = fasi 1, 3,
  5; 4 milestone = fasi 1, 2, 4, 5);
- le scadenze dei milestone sono equidistanti tra data creazione e scadenza
  finale, l'ultima coincide sempre con la scadenza del progetto; con meno
  di 3 giorni a disposizione più milestone cadono nello stesso giorno;
- ogni milestone eredita i task di default della sua fase.

## Calcolo progresso

- Progresso milestone = task fatti / task totali del milestone.
- Progresso progetto = task fatti / task totali di tutti i milestone.
- Progresso categoria (per la home) = task fatti / task totali di tutti i
  progetti di quella categoria.
- Progresso totale dashboard = task fatti / task totali su tutti i progetti.

## Schermate

1. **Home**
   - Cerchio grande con il progresso totale + task fatti/totali + numero
     progetti/completati.
   - Progetti raggruppati per categoria, ognuna con intestazione colorata e
     percentuale di categoria.
   - Ogni progetto: cerchio piccolo, nome, milestone corrente, giorni alla
     scadenza (colore secondo urgenza: normale / giallo ≤7gg / rosso scaduto).
   - FAB per creare un nuovo progetto.

2. **Dettaglio progetto**
   - Cerchio grande con percentuale e task fatti/totali.
   - Nome, obiettivo, scadenza con colore di urgenza.
   - Lista milestone in ordine, ognuno con barra di progresso e task
     spuntabili.
   - Eliminazione progetto con conferma.

3. **Nuovo progetto**
   - Nome, obiettivo (facoltativo), scelta categoria (chip), scelta scadenza
     (date picker, non nel passato).
   - Anteprima live del piano generato (milestone + numero task) prima di
     confermare.

4. **Statistiche** (dalla Home, pulsante "Statistiche")
   - Streak: giorni consecutivi con almeno un task completato. Se oggi non
     è ancora stato completato nulla vale la serie che arriva fino a ieri:
     si azzera solo dopo un giorno intero senza task.
   - Task completati questa settimana (da lunedì) / questo mese (dal primo).
   - Barre di avanzamento per categoria (task fatti / totali).
   - Contano solo i task ancora spuntati: togliere la spunta toglie anche
     il completamento dalle statistiche.

## Notifiche

- WorkManager con controllo giornaliero (alle 9:00): se un milestone scade
  entro 3 giorni ed ha task non completati, notifica locale.
- Se un milestone è scaduto e non completo, notifica di allerta separata,
  su un canale a importanza alta; torna ogni giorno finché resta scaduto.
- Una notifica per tipo con l'elenco delle fasi, non una per fase.
- `POST_NOTIFICATIONS` (Android 13+) è il solo permesso dichiarato
  dall'app, chiesto a runtime quando esiste almeno un progetto. Il
  manifest finale ne contiene altri quattro portati da WorkManager
  (`WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`,
  `ACCESS_NETWORK_STATE`) — concessi all'installazione, non richiesti
  all'utente. Decisione di Arka (2026-11-13): restano, perché nessuno
  apre connessioni (**mai `INTERNET`**, verificato con `aapt2 dump
  permissions` sull'APK reale) e sostituirli costerebbe più di quanto
  valga. La garanzia di spec è "nessuna chiamata di rete", non "nessun
  permesso oltre `POST_NOTIFICATIONS`".

## Fuori scope per v1

- Modifica manuale di singole fasi/task dopo la creazione.
- Sincronizzazione multi-dispositivo o account.
- Widget home screen (eventuale v2).
- Qualsiasi chiamata di rete.

## Struttura repository proposta

```
projecthub-android/
  app/src/main/java/ch/arka/projecthub/
    data/        (Entities, Database/DAO, Repository)
    domain/      (Category, Planner, Models, Stats)
    notify/      (WorkManager worker + scheduling)
    ui/
      theme/
      components/   (DonutChart, barre, chip categoria)
      home/
      project/
      add/
      stats/
    MainActivity.kt
    MainViewModel.kt
  app/src/test/java/...   (test su Planner e calcolo progresso)
```

## Note di continuità

Questo file è il riferimento per Claude Code: contiene tutte le decisioni
prese finora. Chi riprende il lavoro deve leggerlo prima di scrivere codice,
e aggiornarlo se una decisione cambia.
