package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.verdityper.flyt.StegGruppe
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

sealed interface StegGruppeVisningUtleder {

    fun skalVises(behandlingId: BehandlingId): Boolean

    fun gruppe(): StegGruppe
}