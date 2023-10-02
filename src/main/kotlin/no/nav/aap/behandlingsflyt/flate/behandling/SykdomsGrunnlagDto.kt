package no.nav.aap.behandlingsflyt.flate.behandling

import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.domene.Periode

data class SykdomsGrunnlagDto(
    val opplysninger: InnhentetSykdomsOpplysninger,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurdering: Sykdomsvurdering?
)

data class InnhentetSykdomsOpplysninger(
    val oppgittYrkesskadeISøknad: Boolean,
    val innhentedeYrkesskader: List<RegistrertYrkesskade>
)

data class RegistrertYrkesskade(val ref: String, val periode: no.nav.aap.behandlingsflyt.domene.Periode, val kilde: String)
