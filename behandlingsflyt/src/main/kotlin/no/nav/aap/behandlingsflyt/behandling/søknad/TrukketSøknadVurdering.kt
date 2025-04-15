package no.nav.aap.behandlingsflyt.behandling.søknad

import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.util.*

data class TrukketSøknadVurdering(
    val journalpostId: JournalpostId,
    val begrunnelse: String,
    val vurdertAv: Bruker,
    val vurdert: Instant,
) {
    override fun equals(other: Any?): Boolean {
        return other is TrukketSøknadVurdering &&
                this.journalpostId == other.journalpostId &&
                this.begrunnelse == other.begrunnelse &&
                this.vurdertAv.ident == other.vurdertAv.ident &&
                this.vurdert == other.vurdert
    }

    override fun hashCode(): Int {
        return Objects.hash(journalpostId, begrunnelse, vurdertAv.ident, vurdert)
    }
}