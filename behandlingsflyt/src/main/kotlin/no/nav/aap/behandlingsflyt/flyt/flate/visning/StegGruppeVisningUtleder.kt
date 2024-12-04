package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

sealed interface StegGruppeVisningUtleder {

    fun skalVises(behandlingId: BehandlingId): Boolean

    fun gruppe(): StegGruppe
}