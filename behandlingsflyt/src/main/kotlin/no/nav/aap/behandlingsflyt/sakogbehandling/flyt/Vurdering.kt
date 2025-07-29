package no.nav.aap.behandlingsflyt.sakogbehandling.flyt

import no.nav.aap.komponenter.type.Periode

class Vurdering(val type: VurderingType, val årsaker: List<Vurderingsbehov>, val periode: Periode)