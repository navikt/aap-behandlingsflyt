package no.nav.aap.behandlingsflyt.behandling.barnepensjon

import no.nav.aap.komponenter.verdityper.Beløp
import java.time.YearMonth

data class BarnepensjonGrunnlagDto(
    val harTilgangTilÅSaksbehandle : Boolean,
    val vurdering: BarnepensjonVurderingDto?,
)

data class BarnepensjonVurderingDto(
    val perioder: List<BarnepensjonVurderingPeriodeDto>,
    val begrunnelse: String,
)

data class BarnepensjonVurderingPeriodeDto(
    val fom: YearMonth,
    val tom: YearMonth,
    val månedsbeløp: Beløp,
)