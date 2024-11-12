package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import kotlin.reflect.full.primaryConstructor

class DynamiskStegGruppeVisningService(private val connection: DBConnection) {

    private val utledere = mutableMapOf<StegGruppe, StegGruppeVisningUtleder>()

    init {
        StegGruppeVisningUtleder::class.sealedSubclasses.forEach { utleder ->
            val visningUtleder = utleder.constructors
                .find { it.parameters.singleOrNull()?.type?.classifier == DBConnection::class }!!
                .call(connection)
            utledere[visningUtleder.gruppe()] = visningUtleder
        }
    }

    fun skalVises(gruppe: StegGruppe, behandlingId: BehandlingId): Boolean {
        if (!gruppe.skalVises) {
            return false
        }
        if (gruppe.obligatoriskVisning) {
            return true
        }

        val utleder = utledere.getValue(gruppe)

        return utleder.skalVises(behandlingId)
    }
}