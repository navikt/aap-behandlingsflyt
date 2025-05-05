package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOperasjonerRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

object InMemoryAvklaringsbehovRepository : AvklaringsbehovRepository,
    AvklaringsbehovOperasjonerRepository {

    private val idSeq = AtomicLong(10000)
    private val memory = HashMap<BehandlingId, AvklaringsbehovHolder>()
    private val lock = Object()

    override fun hentAvklaringsbehovene(behandlingId: BehandlingId): Avklaringsbehovene {
        return Avklaringsbehovene(this, behandlingId)
    }

    override fun hent(behandlingId: BehandlingId): List<Avklaringsbehov> {
        synchronized(lock) {
            ensureDefault(behandlingId)
            return memory.getValue(behandlingId).avklaringsbehovene
        }
    }

    override fun opprett(
        behandlingId: BehandlingId,
        definisjon: Definisjon,
        funnetISteg: StegType,
        frist: LocalDate?,
        begrunnelse: String,
        grunn: ÅrsakTilSettPåVent?,
        endretAv: String
    ) {
        synchronized(lock) {
            ensureDefault(behandlingId)
            val avklaringsbehov = memory.getValue(behandlingId)

            val eksisterendeBehov = avklaringsbehov.hentBehov(definisjon)
            if (eksisterendeBehov == null) {
                avklaringsbehov.leggTilBehov(definisjon, funnetISteg, frist, begrunnelse, grunn, endretAv)
            } else {
                eksisterendeBehov.historikk.add(
                    Endring(
                        status = Status.OPPRETTET,
                        begrunnelse = begrunnelse,
                        grunn = grunn,
                        endretAv = endretAv,
                        frist = frist
                    )
                )
            }
        }
    }

    private fun ensureDefault(behandlingId: BehandlingId) {
        if (memory[behandlingId] == null) {
            memory[behandlingId] = AvklaringsbehovHolder(mutableListOf())
        }
    }

    override fun kreverToTrinn(avklaringsbehovId: Long, kreverToTrinn: Boolean) {

    }

    override fun endre(
        avklaringsbehovId: Long,
        endring: Endring
    ) {
    }

    override fun endreVentepunkt(
        avklaringsbehovId: Long,
        endring: Endring,
        funnetISteg: StegType
    ) {
    }

    override fun endreSkrivBrev(
        avklaringsbehovId: Long,
        endring: Endring,
        funnetISteg: StegType
    ) {
    }

    private class AvklaringsbehovHolder(val avklaringsbehovene: MutableList<Avklaringsbehov>) {
        fun hentBehov(definisjon: Definisjon): Avklaringsbehov? {
            return avklaringsbehovene.singleOrNull { avklaringsbehov -> avklaringsbehov.definisjon == definisjon }
        }

        fun leggTilBehov(
            definisjon: Definisjon,
            funnetISteg: StegType,
            frist: LocalDate?,
            begrunnelse: String,
            venteÅrsak: ÅrsakTilSettPåVent?,
            endretAv: String
        ) {
            val avklaringsbehov = Avklaringsbehov(
                idSeq.andIncrement, definisjon,
                mutableListOf(
                    Endring(
                        status = Status.OPPRETTET,
                        begrunnelse = begrunnelse,
                        grunn = venteÅrsak,
                        endretAv = endretAv,
                        frist = frist
                    )
                ),
                funnetISteg = funnetISteg,
                kreverToTrinn = false
            )
            avklaringsbehovene.add(avklaringsbehov)
        }

        fun harBehovMedId(avklaringsbehovId: Long): Boolean {
            return avklaringsbehovene.any { it.id == avklaringsbehovId }
        }

        fun hentBehov(avklaringsbehovId: Long): Avklaringsbehov {
            return avklaringsbehovene.single { avklaringsbehov -> avklaringsbehov.id == avklaringsbehovId }
        }
    }
}