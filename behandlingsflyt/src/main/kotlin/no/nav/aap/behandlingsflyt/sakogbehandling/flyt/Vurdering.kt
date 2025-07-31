package no.nav.aap.behandlingsflyt.sakogbehandling.flyt

import no.nav.aap.komponenter.type.Periode

class Vurdering(val type: VurderingType, val vurderingsbehov: List<Vurderingsbehov>, val periode: Periode)