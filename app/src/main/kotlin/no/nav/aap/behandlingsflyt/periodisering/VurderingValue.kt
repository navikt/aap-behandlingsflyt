package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.verdityper.flyt.VurderingType
import no.nav.aap.verdityper.flyt.ÅrsakTilBehandling

class VurderingValue(val type: VurderingType, val årsaker: List<ÅrsakTilBehandling>)