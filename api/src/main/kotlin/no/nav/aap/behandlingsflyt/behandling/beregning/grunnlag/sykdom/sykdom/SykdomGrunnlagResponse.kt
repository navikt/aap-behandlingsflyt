package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.InnhentetSykdomsOpplysninger
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class SykdomGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val skalVurdereYrkesskade: Boolean,
    val opplysninger: InnhentetSykdomsOpplysninger,

    @Deprecated("kan være mer enn 1, bruk sykdomsvurderinger")
    val sykdomsvurdering: SykdomsvurderingResponse?,

    val sykdomsvurderinger: List<SykdomsvurderingResponse>,
    val historikkSykdomsvurderinger: List<SykdomsvurderingResponse>,
    val gjeldendeVedtatteSykdomsvurderinger: List<SykdomsvurderingResponse>,
)

data class SykdomsvurderingResponse(
    val begrunnelse: String,

    /** Hvis null, så gjelder den fra starten. */
    val vurderingenGjelderFra: LocalDate?,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erArbeidsevnenNedsatt: Boolean?,
    val harSkadeSykdomEllerLyte: Boolean,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    val yrkesskadeBegrunnelse: String?,
    val kodeverk: String? = null,
    val hoveddiagnose: String? = null,
    val bidiagnoser: List<String>? = emptyList(),
    val vurdertAv: VurdertAvResponse
)