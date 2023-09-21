package no.nav.aap.flate.behandling

import no.nav.aap.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.avklaringsbehov.sykdom.Yrkesskadevurdering
import no.nav.aap.domene.Periode

data class SykdomsGrunnlagDto(
    val opplysninger: InnhentetSykdomsOpplysninger,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurdering: Sykdomsvurdering?
)

data class InnhentetSykdomsOpplysninger(
    val oppgittYrkesskadeISøknad: Boolean,
    val innhentedeYrkesskader: List<RegistrertYrkesskade>
)

data class RegistrertYrkesskade(val ref: String, val periode: Periode, val kilde: String)
