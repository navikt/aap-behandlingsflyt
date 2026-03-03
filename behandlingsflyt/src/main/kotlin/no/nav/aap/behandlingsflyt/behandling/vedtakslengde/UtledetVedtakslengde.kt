package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import java.time.LocalDate

sealed class UtledetVedtakslengde {
    data object Manuell : UtledetVedtakslengde()
    data class Automatisk(val nySluttdato: LocalDate) : UtledetVedtakslengde()
}