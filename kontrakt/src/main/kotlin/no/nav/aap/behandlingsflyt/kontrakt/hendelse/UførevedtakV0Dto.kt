package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import java.time.LocalDate

public data class UførevedtakDto (
    val resultat: UførevedtakResultat,
    val virkningsdato: LocalDate
)
