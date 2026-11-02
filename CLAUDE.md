# ProjectHub — istruzioni per Claude Code

App Android nativa (Kotlin + Compose + Room, solo locale, nessuna rete).

- Specifica di progetto e decisioni prese: `PROJECTHUB_SPEC.md` — leggerla
  prima di scrivere codice.
- Stato dei lavori e backlog ordinato: `TASKS.md` — comincia da lì,
  nell'ordine indicato (P0 prima di tutto, l'app non compila ancora).

## Comandi

```
./gradlew assembleDebug   # compila il debug APK
./gradlew test            # test JVM (domain/Planner, domain/Models)
./gradlew lint            # analisi statica Android
```

Il wrapper Gradle è incompleto (vedi TASKS.md, P0): se `./gradlew` non
esiste o non funziona, è il primo problema da risolvere.

## Convenzioni

- Commenti e stringhe utente in italiano, nomi di codice (classi,
  funzioni, variabili) in inglese, come nel codice già presente.
- Un commit per voce di backlog completata, non commit enormi.
- Dopo ogni modifica a `domain/`, far girare i test prima di proseguire.
- Aggiornare `TASKS.md` (spuntare o annotare) e `PROJECTHUB_SPEC.md` se
  una decisione di progetto cambia.
