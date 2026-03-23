package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangufore

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom.SykdomsvurderingResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.InnhentetSykdomsOpplysninger
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class OvergangUføreGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    @Deprecated("Erstattes av nyeVurderinger")
    val vurdering: OvergangUføreVurderingResponse?,
    override val nyeVurderinger: List<OvergangUføreVurderingResponse>,
    @Deprecated("Erstattes av sisteVedtatteVurderinger")
    val gjeldendeVedtatteVurderinger: List<OvergangUføreVurderingResponse>,
    override val sisteVedtatteVurderinger: List<OvergangUføreVurderingResponse>,
    val historiskeVurderinger: List<OvergangUføreVurderingResponse>,
    val gjeldendeSykdsomsvurderinger: List<SykdomsvurderingResponse>,
    override val kanVurderes: List<Periode>,
    override val ikkeRelevantePerioder: List<Periode>,
    override val behøverVurderinger: List<Periode>,
    val perioderSomIkkeErTilstrekkeligVurdert: List<Periode>,
    val kvalitetssikretAv: VurdertAvResponse?,
    val uføreSøknadOpplysninger: UføreSøknadOpplysninger? = null,
): PeriodiserteVurderingerDto<OvergangUføreVurderingResponse>


data class UføreSøknadOpplysninger(
    val soknadsdato: LocalDate
)