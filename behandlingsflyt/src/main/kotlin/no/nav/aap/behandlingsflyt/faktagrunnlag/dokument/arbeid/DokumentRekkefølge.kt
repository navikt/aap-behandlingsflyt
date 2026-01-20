package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import java.time.LocalDateTime

data class DokumentRekkefølge(val referanse: InnsendingReferanse, val mottattTidspunkt: LocalDateTime) :
    Comparable<DokumentRekkefølge> {

    override fun compareTo(other: DokumentRekkefølge): Int {
        return mottattTidspunkt.compareTo(other.mottattTidspunkt)
    }
}