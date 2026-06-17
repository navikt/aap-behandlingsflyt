package no.nav.aap.behandlingsflyt.behandling.avslag11_27

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant

class Avslag11_27Vurdering (
    val journalpostId: JournalpostId,
    val begrunnelse: String,
    val harAnnenFullYtelse: Boolean,
    val brukersYtelse: Ytelse? = null,
    val harSykepengegrunnlagOver2G: Boolean? = null, // Kun for sykepenger
    val skalAvslås1127: Boolean,
    val vurdertIBehandling: BehandlingId,
    val vurdertTidspunkt: Instant,
    val vurdertAv: Bruker,
)
