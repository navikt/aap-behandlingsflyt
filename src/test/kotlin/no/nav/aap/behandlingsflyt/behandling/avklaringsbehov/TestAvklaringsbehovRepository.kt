package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class TestAvklaringsbehovRepository : AvklaringsbehovRepository, AvklaringsbehovOperasjonerRepository {

    private val behovPerBehandling = HashMap<BehandlingId, MutableList<Avklaringsbehov>>()

    override fun leggTilAvklaringsbehov(
        behandlingId: BehandlingId,
        definisjoner: List<Definisjon>,
        funnetISteg: StegType
    ) {
        val avklaringsbehov = behovPerBehandling.getOrDefault(behandlingId, mutableListOf())
        for (definisjon in definisjoner) {
            if (avklaringsbehov.any { it.definisjon == definisjon }) {
                // DO nothing
            } else {
                leggTilAvklaringsbehov(behandlingId, definisjon, funnetISteg)
            }
        }
    }

    override fun leggTilAvklaringsbehov(behandlingId: BehandlingId, definisjon: Definisjon, funnetISteg: StegType) {
        val avklaringsbehov = behovPerBehandling.getOrDefault(behandlingId, mutableListOf())

        avklaringsbehov.add(
            Avklaringsbehov(
                id = avklaringsbehov.size.toLong() + 1,
                definisjon = definisjon,
                funnetISteg = funnetISteg,
                kreverToTrinn = null
            )
        )
    }

    override fun løs(behandlingId: BehandlingId, definisjon: Definisjon, begrunnelse: String, kreverToTrinn: Boolean?) {

    }

    override fun hent(behandlingId: BehandlingId): Avklaringsbehovene {
        return Avklaringsbehovene(this, behandlingId)
    }

    override fun hentBehovene(behandlingId: BehandlingId): List<Avklaringsbehov> {
        return behovPerBehandling.getOrDefault(behandlingId, listOf())
    }

    override fun kreverToTrinn(avklaringsbehovId: Long, kreverToTrinn: Boolean) {

    }

    override fun opprettAvklaringsbehovEndring(
        avklaringsbehovId: Long,
        status: Status,
        begrunnelse: String,
        opprettetAv: String
    ) {

    }
}