package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.flyt.EndringType

data class Årsak(val type: EndringType, val periode: Periode? = null)
