package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate
import kotlin.String

data class ArbeidsopptrappingLøsningDto(
    override val begrunnelse: String,
    val reellMulighetTilOpptrapping: Boolean,
    val rettPaaAAPIOpptrapping: Boolean,
    override val fom: LocalDate,
    override val tom: LocalDate?,
) : LøsningForPeriode {
    fun toArbeidsopptrappingVurdering(bruker: Bruker, vurdertIBehandling: BehandlingId) =
        ArbeidsopptrappingVurdering(
            begrunnelse = begrunnelse,
            reellMulighetTilOpptrapping = reellMulighetTilOpptrapping,
            rettPaaAAPIOpptrapping = rettPaaAAPIOpptrapping,
            vurdertAv = bruker,
            opprettet = Instant.now(),
            fom = fom,
            vurdertIBehandling = vurdertIBehandling,
            tom = tom,
        )
}