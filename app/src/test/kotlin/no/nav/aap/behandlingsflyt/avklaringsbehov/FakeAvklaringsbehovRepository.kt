package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate

class FakeAvklaringsbehovRepository : AvklaringsbehovRepository, AvklaringsbehovOperasjonerRepository {

    private val behovPerBehandling = HashMap<BehandlingId, MutableList<Avklaringsbehov>>()

    override fun opprett(behandlingId: BehandlingId, definisjon: Definisjon, funnetISteg: StegType, frist: LocalDate?) {
        val avklaringsbehov = behovPerBehandling.getOrDefault(behandlingId, mutableListOf())

        avklaringsbehov.add(
            Avklaringsbehov(
                id = avklaringsbehov.size.toLong() + 1,
                definisjon = definisjon,
                funnetISteg = funnetISteg,
                kreverToTrinn = null
            )
        )

        behovPerBehandling[behandlingId] = avklaringsbehov
    }

    override fun hentAvklaringsbehovene(behandlingId: BehandlingId): Avklaringsbehovene {
        return Avklaringsbehovene(this, behandlingId)
    }

    override fun hent(behandlingId: BehandlingId): List<Avklaringsbehov> {
        return behovPerBehandling.getOrDefault(behandlingId, listOf())
    }

    override fun kreverToTrinn(avklaringsbehovId: Long, kreverToTrinn: Boolean) {
    }

    override fun endre(avklaringsbehovId: Long, endring: Endring) {
    }
}