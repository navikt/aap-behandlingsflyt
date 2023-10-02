package no.nav.aap.behandlingsflyt.flate.behandling

import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårstype

data class VilkårDTO(val vilkårstype: Vilkårstype, val perioder: List<VilkårsperiodeDTO>)
