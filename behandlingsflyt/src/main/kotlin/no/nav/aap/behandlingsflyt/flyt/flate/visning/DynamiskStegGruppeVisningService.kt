package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import kotlin.reflect.KClass

class DynamiskStegGruppeVisningService(
    repositoryProvider: RepositoryProvider,
    gatewayProvider: GatewayProvider
) {

    private val utledere = mutableMapOf<StegGruppe, StegGruppeVisningUtleder>()

    private val tilgjengeligeArgs: Map<KClass<*>, Any> = mapOf(
        RepositoryProvider::class to repositoryProvider,
        GatewayProvider::class to gatewayProvider
    )

    init {
        StegGruppeVisningUtleder::class.sealedSubclasses.forEach { utleder ->
            val constructor = utleder.constructors
                .find { c -> c.parameters.all { it.type.classifier in tilgjengeligeArgs } }
                ?: error("Ingen passende konstruktør funnet for ${utleder.simpleName}")

            val args = constructor.parameters.associateWith {
                tilgjengeligeArgs[it.type.classifier] ?: error("Parameter må finnes i tilgjengeligeArgs")
            }
            val visningUtleder = constructor.callBy(args)
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