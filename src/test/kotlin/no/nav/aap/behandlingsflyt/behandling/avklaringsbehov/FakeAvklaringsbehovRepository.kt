package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovOperasjonerRepository
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class FakeAvklaringsbehovRepository : AvklaringsbehovRepository, AvklaringsbehovOperasjonerRepository {

    private val behovPerBehandling = HashMap<BehandlingId, MutableList<Avklaringsbehov>>()

    override fun opprett(behandlingId: BehandlingId, definisjon: Definisjon, funnetISteg: StegType) {
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

    override fun endre(avklaringsbehov: Avklaringsbehov) {
    }
}