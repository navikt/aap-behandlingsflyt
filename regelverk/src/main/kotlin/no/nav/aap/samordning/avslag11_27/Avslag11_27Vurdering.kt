package no.nav.aap.samordning.avslag11_27

import java.time.Instant
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.misc.VurderingForKrav
import no.nav.aap.krav.Kravreferanse
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.samordning.Ytelse

data class Avslag11_27Vurdering (
    override val referanse: Kravreferanse,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
    override val vurdertAv: Bruker,

    val begrunnelse: String,
    val harAnnenFullYtelse: Boolean,
    val brukersYtelse: Ytelse? = null,
    /*  Kun for sykepenger */
    val harSykepengegrunnlagOver2G: Boolean? = null,
    val skalAvslås1127: Boolean,
): VurderingForKrav