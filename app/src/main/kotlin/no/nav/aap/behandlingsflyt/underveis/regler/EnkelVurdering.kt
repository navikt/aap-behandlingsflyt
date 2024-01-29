package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.vilkår.Vilkårtype

data class EnkelVurdering(val vilkår: Vilkårtype, val utfall: Utfall)
