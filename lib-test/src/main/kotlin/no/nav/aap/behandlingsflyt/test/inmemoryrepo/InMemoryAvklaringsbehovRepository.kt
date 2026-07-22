package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovForSak
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOperasjonerRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.*

object InMemoryAvklaringsbehovRepository : AvklaringsbehovRepository,
    AvklaringsbehovOperasjonerRepository {

    private val idSeq = AtomicLong(10000)
    private val endringSeq = AtomicLong(0)
    private val memory = HashMap<BehandlingId, AvklaringsbehovHolder>()
    private val lock = Any()

    override fun hentAvklaringsbehovene(behandlingId: BehandlingId): Avklaringsbehovene {
        return Avklaringsbehovene(this, behandlingId)
    }

    override fun hent(behandlingId: BehandlingId): List<Avklaringsbehov> {
        synchronized(lock) {
            ensureDefault(behandlingId)
            return memory.getValue(behandlingId).avklaringsbehovene
        }
    }

    override fun hentAlleAvklaringsbehovForSak(behandlingIder: List<BehandlingId>): List<AvklaringsbehovForSak> {
        return emptyList()
    }

    override fun opprett(
        behandlingId: BehandlingId,
        definisjon: Definisjon,
        funnetISteg: StegType,
        frist: LocalDate?,
        begrunnelse: String,
        grunn: ÅrsakTilSettPåVent?,
        endretAv: Bruker,
        perioderSomIkkeErTilstrekkeligVurdert: Set<Periode>?,
        perioderVedtaketBehøverVurdering: Set<Periode>?
    ) {
        synchronized(lock) {
            ensureDefault(behandlingId)
            val avklaringsbehov = memory.getValue(behandlingId)

            val eksisterendeBehov = avklaringsbehov.hentBehov(definisjon)
            val endring = lagretEndring(
                Endring(
                    status = Status.OPPRETTET,
                    begrunnelse = begrunnelse,
                    grunn = grunn,
                    endretAv = endretAv,
                    frist = frist,
                    perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert,
                    perioderVedtaketBehøverVurdering = perioderVedtaketBehøverVurdering
                )
            )
            if (eksisterendeBehov == null) {
                avklaringsbehov.leggTilBehov(
                    definisjon,
                    funnetISteg,
                    endring
                )
            } else {
                avklaringsbehov.endre(eksisterendeBehov.id, endring)
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
        endreAvklaringsbehov(avklaringsbehovId, endring)
    }

    override fun endreVentepunkt(
        avklaringsbehovId: Long,
        endring: Endring,
        funnetISteg: StegType
    ) {
        oppdaterFunnetISteg(avklaringsbehovId, funnetISteg)
        endreAvklaringsbehov(avklaringsbehovId, endring)
    }

    private fun endreAvklaringsbehov(
        avklaringsbehovId: Long,
        endring: Endring
    ) {
        synchronized(lock) {
            val avklaringsbehov =
                memory.values.single { it.avklaringsbehovene.any { avklaringsbehov -> avklaringsbehov.id == avklaringsbehovId } }

            avklaringsbehov.endre(avklaringsbehovId, lagretEndring(endring))
        }
    }

    private fun lagretEndring(endring: Endring): Endring {
        return endring.copy(tidsstempel = LocalDateTime.now().plusNanos(endringSeq.incrementAndGet()))
    }

    private fun oppdaterFunnetISteg(avklaringsbehovId: Long, funnetISteg: StegType) {
        val holder = memory.values
            .single { avklaringsbehovHolder ->
                avklaringsbehovHolder.avklaringsbehovene
                    .find { it.id == avklaringsbehovId } != null
            }

        val avklaringsbehov = holder.avklaringsbehovene
            .single { it.id == avklaringsbehovId }


        holder.avklaringsbehovene.removeIf { it.id == avklaringsbehovId }
        holder.avklaringsbehovene.add(
            Avklaringsbehov(
                avklaringsbehovId,
                avklaringsbehov.definisjon,
                avklaringsbehov.historikk(),
                funnetISteg,
                avklaringsbehov.erTotrinn() && !avklaringsbehov.definisjon.kreverToTrinn
            )
        )
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
    }

    override fun slett(behandlingId: BehandlingId) {
    }

    private class AvklaringsbehovHolder(val avklaringsbehovene: MutableList<Avklaringsbehov>) {
        fun hentBehov(definisjon: Definisjon): Avklaringsbehov? {
            return avklaringsbehovene.singleOrNull { avklaringsbehov -> avklaringsbehov.definisjon == definisjon }
        }

        fun leggTilBehov(
            definisjon: Definisjon,
            funnetISteg: StegType,
            endring: Endring,
        ) {
            val avklaringsbehov = Avklaringsbehov(
                idSeq.andIncrement, definisjon,
                mutableListOf(endring),
                funnetISteg = funnetISteg,
                kreverToTrinn = false
            )
            avklaringsbehovene.add(avklaringsbehov)
        }

        fun endre(avklaringsbehovId: Long, endring: Endring) {
            val avklaringsbehov = avklaringsbehovene.single { it.id == avklaringsbehovId }

            // Erstatt med kopi
            avklaringsbehovene.removeIf { it.id == avklaringsbehovId }

            avklaringsbehovene.add(
                Avklaringsbehov(
                    id = avklaringsbehov.id,
                    definisjon = avklaringsbehov.definisjon,
                    historikk = avklaringsbehov.historikk() + endring,
                    funnetISteg = avklaringsbehov.funnetISteg,
                    kreverToTrinn = avklaringsbehov.erTotrinn()
                )
            )
        }
    }
}
