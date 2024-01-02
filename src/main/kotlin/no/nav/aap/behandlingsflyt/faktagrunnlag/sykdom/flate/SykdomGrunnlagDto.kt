package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.flate

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.Periode

data class SykdomGrunnlagDto(
    val opplysninger: InnhentetSykdomsOpplysninger,
    val sykdomsvurdering: Sykdomsvurdering?,
    val erÅrsakssammenheng: Boolean?
)

data class YrkesskadeGrunnlagDto(
    val opplysninger: InnhentetSykdomsOpplysninger,
    val yrkesskadevurdering: Yrkesskadevurdering?,
)

data class InnhentetSykdomsOpplysninger(
    val oppgittYrkesskadeISøknad: Boolean,
    val innhentedeYrkesskader: List<RegistrertYrkesskade>,
)

data class RegistrertYrkesskade(val ref: String, val periode: Periode, val kilde: String)
