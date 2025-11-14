package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate

data class ArbeidsopptrappingVurdering(
    val begrunnelse: String,
    val vurderingenGjelderFra: LocalDate,
    val reellMulighetTilOpptrapping: Boolean,
    val rettPaaAAPIOpptrapping: Boolean,
    val vurdertAv: String,
    val opprettetTid: Instant,
    val vurdertIBehandling: BehandlingId,
    val vurderingenGjelderTil: LocalDate?,
)