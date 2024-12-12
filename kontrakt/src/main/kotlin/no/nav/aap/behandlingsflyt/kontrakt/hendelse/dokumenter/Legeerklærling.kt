package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

public sealed interface Legeerklærling : Melding


public class LegeerklæringV0(
    public val journalpostId: String
) : Legeerklærling