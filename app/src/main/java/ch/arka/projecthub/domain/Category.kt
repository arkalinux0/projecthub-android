package ch.arka.projecthub.domain

/** Una fase-modello: titolo del milestone + task di default. */
data class Phase(val title: String, val tasks: List<String>)

/**
 * Categorie di progetto. Ognuna ha un colore e un modello di fasi
 * usato dal Planner per scomporre l'obiettivo finale.
 * Le fasi sono in ordine cronologico: la prima è l'avvio, l'ultima la chiusura.
 */
enum class Category(
    val label: String,
    val colorHex: Long,
    val phases: List<Phase>,
) {
    SCUOLA(
        "Scuola", 0xFF4AA3FF,
        listOf(
            Phase("Pianificazione", listOf("Raccogliere requisiti e consegne", "Dividere il lavoro in blocchi", "Fissare le scadenze intermedie")),
            Phase("Studio e ricerca", listOf("Leggere materiale di base", "Fare schemi e appunti", "Segnare i dubbi da chiarire")),
            Phase("Svolgimento", listOf("Scrivere la prima bozza", "Fare gli esercizi/pratica", "Chiedere feedback")),
            Phase("Revisione", listOf("Correggere gli errori", "Ripassare i punti deboli", "Prova generale")),
            Phase("Consegna", listOf("Controllo finale", "Consegnare / sostenere", "Annotare cosa migliorare")),
        ),
    ),
    SECURITY(
        "Security", 0xFFFF6B7A,
        listOf(
            Phase("Fondamenta", listOf("Definire scope e ambiente di lab", "Raccogliere risorse e documentazione", "Preparare VM/rete isolata")),
            Phase("Studio teorico", listOf("Studiare la teoria della tecnica", "Leggere paper / advisory correlati", "Prendere appunti riutilizzabili")),
            Phase("Pratica in lab", listOf("Riprodurre il caso nel lab", "Documentare ogni passaggio", "Variare il caso e confrontare")),
            Phase("Automazione e tooling", listOf("Scrivere uno script di supporto", "Testare su casi diversi", "Pulire e commentare il codice")),
            Phase("Writeup", listOf("Scrivere il writeup", "Aggiungere contromisure/difese", "Pubblicare o archiviare")),
        ),
    ),
    HARDWARE(
        "Hardware", 0xFFFFB454,
        listOf(
            Phase("Progetto e componenti", listOf("Definire requisiti e budget", "Scegliere i componenti", "Ordinare quello che manca")),
            Phase("Prototipo", listOf("Montare su breadboard/banco", "Verificare alimentazione e connessioni", "Fare i primi test")),
            Phase("Integrazione", listOf("Assemblare nel case/telaio", "Configurare il sistema operativo", "Testare tutto insieme")),
            Phase("Rifinitura", listOf("Risolvere i difetti emersi", "Ottimizzare consumi/temperature", "Etichettare e documentare")),
            Phase("Completamento", listOf("Test finale prolungato", "Backup della configurazione", "Foto e documentazione finale")),
        ),
    ),
    SOFTWARE(
        "Software", 0xFF36D1B7,
        listOf(
            Phase("Analisi", listOf("Scrivere cosa deve fare il programma", "Scegliere stack e struttura", "Creare repository e scheletro")),
            Phase("Nucleo", listOf("Implementare la funzione principale", "Scrivere i primi test", "Fare il primo commit funzionante")),
            Phase("Funzionalità", listOf("Aggiungere le funzioni secondarie", "Gestire errori e casi limite", "Rifattorizzare dove serve")),
            Phase("Qualità", listOf("Completare test e lint", "Scrivere il README", "Provare su un'installazione pulita")),
            Phase("Rilascio", listOf("Taggare la versione", "Pubblicare / distribuire", "Raccogliere idee per la prossima")),
        ),
    ),
    PERSONALE(
        "Personale", 0xFFB48CFF,
        listOf(
            Phase("Partenza", listOf("Scrivere l'obiettivo in una frase", "Definire come misuro il successo", "Fare il primo passo concreto")),
            Phase("Costruire l'abitudine", listOf("Fissare un orario ricorrente", "Tenere traccia dei progressi", "Rimuovere un ostacolo")),
            Phase("Metà strada", listOf("Fare il punto su cosa funziona", "Correggere il piano", "Premiare il progresso")),
            Phase("Spinta finale", listOf("Concentrarsi sulle priorità", "Eliminare le distrazioni", "Preparare il traguardo")),
            Phase("Traguardo", listOf("Completare l'obiettivo", "Fare il bilancio", "Decidere il prossimo obiettivo")),
        ),
    );

    companion object {
        fun fromName(name: String): Category =
            entries.firstOrNull { it.name == name } ?: PERSONALE
    }
}
