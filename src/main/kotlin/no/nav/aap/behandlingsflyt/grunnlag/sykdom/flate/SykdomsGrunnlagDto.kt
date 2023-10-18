package no.nav.aap.behandlingsflyt.grunnlag.sykdom.flate

import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.domene.Periode

data class SykdomsGrunnlagDto(
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
