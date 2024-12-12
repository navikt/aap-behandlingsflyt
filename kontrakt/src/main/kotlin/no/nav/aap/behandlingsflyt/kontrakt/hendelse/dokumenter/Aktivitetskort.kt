package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import java.time.LocalDate

/**
 * Denne har ingen implementasjoner, siden aktivitetskort lages internt i Kelvin.
 */
public sealed interface Aktivitetskort : Melding

public data class AktivitetskortV0(val fraOgMed: LocalDate, val tilOgMed: LocalDate) : Aktivitetskort