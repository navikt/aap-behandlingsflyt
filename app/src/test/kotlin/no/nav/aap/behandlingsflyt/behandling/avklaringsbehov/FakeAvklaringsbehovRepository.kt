package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate

class FakeAvklaringsbehovRepository : AvklaringsbehovRepository, AvklaringsbehovOperasjonerRepository {

    private val behovPerBehandling = HashMap<BehandlingId, MutableList<Avklaringsbehov>>()

    override fun opprett(
        behandlingId: BehandlingId,
        definisjon: Definisjon,
        funnetISteg: StegType,
        frist: LocalDate?,
        begrunnelse: String,
        grunn: ÅrsakTilSettPåVent?,
        endretAv: String
    ) {
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

    override fun endreVentepunkt(avklaringsbehovId: Long, endring: Endring, funnetISteg: StegType) {
    }
}