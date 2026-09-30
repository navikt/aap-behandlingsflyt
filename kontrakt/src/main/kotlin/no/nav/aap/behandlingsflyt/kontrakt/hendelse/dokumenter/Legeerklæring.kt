package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

public sealed interface Legeerklæring : Melding

public data class LegeerklæringV0(val beskrivelse: String? = null) : Legeerklæring