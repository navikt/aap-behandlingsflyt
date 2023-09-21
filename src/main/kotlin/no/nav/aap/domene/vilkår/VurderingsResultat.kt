package no.nav.aap.domene.vilkår

import no.nav.aap.domene.behandling.Avslagsårsak
import no.nav.aap.domene.behandling.Beslutningstre
import no.nav.aap.domene.behandling.Utfall

class VurderingsResultat(val utfall: Utfall, val avslagsårsak: Avslagsårsak?, val beslutningstre: Beslutningstre) {
}
