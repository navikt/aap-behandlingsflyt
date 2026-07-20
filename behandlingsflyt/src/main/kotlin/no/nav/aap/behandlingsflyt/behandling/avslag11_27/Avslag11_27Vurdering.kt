package no.nav.aap.behandlingsflyt.behandling.avslag11_27

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.VurderingForKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant

class Avslag11_27Vurdering (
    override val referanse: Kravreferanse,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,

    val begrunnelse: String,
    val harAnnenFullYtelse: Boolean,
    val brukersYtelse: Ytelse? = null,
    /*  Kun for sykepenger */
    val harSykepengegrunnlagOver2G: Boolean? = null,
    val skalAvslås1127: Boolean,
    val vurdertAv: Bruker,
): VurderingForKrav
