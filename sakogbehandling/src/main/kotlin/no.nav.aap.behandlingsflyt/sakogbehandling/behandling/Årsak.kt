package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.flyt.ÅrsakTilBehandling

data class Årsak(val type: ÅrsakTilBehandling, val periode: Periode? = null)
