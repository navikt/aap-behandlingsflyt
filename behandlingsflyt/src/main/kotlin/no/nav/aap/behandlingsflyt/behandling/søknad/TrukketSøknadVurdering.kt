package no.nav.aap.behandlingsflyt.behandling.søknad

import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant

data class TrukketSøknadVurdering(
    val journalpostId: JournalpostId,
    val begrunnelse: String,
    val vurdertAv: Bruker,
    val vurdert: Instant,
)