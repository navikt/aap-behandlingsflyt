package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentReferanse
import java.time.LocalDateTime

class DokumentRekkefølge(val referanse: MottattDokumentReferanse, val mottattTidspunkt: LocalDateTime) :
    Comparable<DokumentRekkefølge> {


    override fun compareTo(other: DokumentRekkefølge): Int {
        return mottattTidspunkt.compareTo(other.mottattTidspunkt)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DokumentRekkefølge

        if (referanse != other.referanse) return false
        if (mottattTidspunkt != other.mottattTidspunkt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = referanse.hashCode()
        result = 31 * result + mottattTidspunkt.hashCode()
        return result
    }
}