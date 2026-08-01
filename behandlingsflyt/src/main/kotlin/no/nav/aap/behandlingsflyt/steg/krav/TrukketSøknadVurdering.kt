package no.nav.aap.behandlingsflyt.steg.krav

import java.time.Instant
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId

data class TrukketSøknadVurdering(
    val journalpostId: JournalpostId,
    val begrunnelse: String,
    val skalTrekkes: Boolean,
    val vurdertAv: Bruker,
    val vurdert: Instant,
)