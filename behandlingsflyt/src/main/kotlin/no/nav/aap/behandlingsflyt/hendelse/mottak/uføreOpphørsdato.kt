package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Uførevedtak
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV1
import java.time.LocalDate

internal fun Uførevedtak.opphørsdato(mottattDato: LocalDate): LocalDate =
    if (!virkningsdato.isBefore(mottattDato)) virkningsdato else when (this) {
        is UførevedtakV0 -> mottattDato.plusDays(1)
        is UførevedtakV1 -> vedtaksdato
    }
