package no.nav.aap.behandlingsflyt.flate.behandling

import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.domene.behandling.Avslagsårsak
import no.nav.aap.behandlingsflyt.domene.behandling.Utfall

data class VilkårsperiodeDTO(
    val periode: no.nav.aap.behandlingsflyt.domene.Periode,
    val utfall: Utfall,
    val manuellVurdering: Boolean,
    val begrunnelse: String?,
    val avslagsårsak: Avslagsårsak?
)
