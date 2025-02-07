package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.InnhentetSykdomsOpplysninger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingDto

data class SykdomGrunnlagDto(
    val skalVurdereYrkesskade: Boolean,
    val opplysninger: InnhentetSykdomsOpplysninger,

    @Deprecated("kan være mer enn 1, bruk sykdomsvurderinger")
    val sykdomsvurdering: SykdomsvurderingDto?,

    val sykdomsvurderinger: List<SykdomsvurderingDto>,
    val historikkSykdomsvurderinger: List<SykdomsvurderingDto>,
    val gjeldendeVedtatteSykdomsvurderinger: List<SykdomsvurderingDto>,
)