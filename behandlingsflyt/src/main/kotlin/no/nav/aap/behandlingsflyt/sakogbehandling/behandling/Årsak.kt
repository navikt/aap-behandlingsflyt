package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.komponenter.type.Periode

data class Årsak(val type: ÅrsakTilBehandling, val periode: Periode? = null)
