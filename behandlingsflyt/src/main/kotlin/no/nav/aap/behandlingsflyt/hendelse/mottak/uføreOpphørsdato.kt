package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import java.time.LocalDate

/** Tilbakevirkende vedtak skal ikke gi automatisk opphør før dagen etter mottak. */
internal fun UførevedtakV0.opphørsdato(mottattDato: LocalDate): LocalDate =
    if (virkningsdato.isBefore(mottattDato)) mottattDato.plusDays(1) else virkningsdato
