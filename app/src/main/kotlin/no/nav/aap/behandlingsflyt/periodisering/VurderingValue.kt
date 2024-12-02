package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling

class VurderingValue(val type: VurderingType, val årsaker: List<ÅrsakTilBehandling>)