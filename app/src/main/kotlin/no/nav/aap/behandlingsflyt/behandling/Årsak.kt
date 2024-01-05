package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.verdityper.Periode

data class Årsak(val type: EndringType, val periode: Periode? = null)
