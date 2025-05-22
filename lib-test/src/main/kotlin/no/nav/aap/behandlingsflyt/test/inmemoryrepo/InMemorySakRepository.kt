package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.*
import no.nav.aap.komponenter.type.Periode
import java.util.concurrent.atomic.AtomicLong

object InMemorySakRepository : SakRepository {

    private val idSeq = AtomicLong(10000)
    private val memory = HashMap<SakId, Sak>()
    private val lock = Object()

    override fun finnEllerOpprett(
        person: Person,
        periode: Periode
    ): Sak {
        synchronized(lock) {
            val eksisterendeSak = memory.values.filter { sak -> sak.person == person }
                .singleOrNull { sak -> sak.rettighetsperiode.overlapper(periode) }
            if (eksisterendeSak != null) {
                return eksisterendeSak
            } else {
                val id = SakId(idSeq.andIncrement)
                val sak =
                    Sak(
                        id = id,
                        saksnummer = Saksnummer.valueOf(id.id),
                        person = person,
                        rettighetsperiode = periode
                    )
                memory.put(id, sak)

                return sak
            }
        }
    }

    override fun finnSakerFor(person: Person): List<Sak> {
        synchronized(lock) {
            return memory.values.filter { sak -> sak.person == person }
        }
    }

    override fun finnSakerFor(person: Person, periode: Periode): List<Sak> {
        synchronized(lock) {
            return memory.values.filter { sak -> sak.person == person }
        }
    }

    override fun finnAlle(): List<Sak> {
        return memory.values.toList()
    }

    override fun hent(sakId: SakId): Sak {
        synchronized(lock) {
            return memory.getValue(sakId)
        }
    }

    override fun hent(saksnummer: Saksnummer): Sak {
        synchronized(lock) {
            return memory.values.single { sak -> sak.saksnummer == saksnummer }
        }
    }

    override fun finnSøker(saksnummer: Saksnummer): Person {
        TODO("Not yet implemented")
    }

    override fun finnSøker(sakId: SakId): Person {
        TODO("Not yet implemented")
    }

    override fun oppdaterRettighetsperiode(
        sakId: SakId,
        periode: Periode
    ) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
    }

    override fun oppdaterSakStatus(
        sakId: SakId,
        status: Status
    ) {
        synchronized(lock) {
            // ** Disclainer **
            // Benytter reflection her for å ikke kompromitere produksjonskode
            // Anser det som bedre enn å tilpasse produksjonskode til testing
            // da tilpassing av koden vil tilgjengliggjøre funksjonalitet på saken
            // som ikke er tiltenkt at man skal manipulere på.
            //
            // Før du vurderer å gjøre noe tilsvarende, ta en prat med tech-lead.

            val sak = memory.getValue(sakId)
            val field = sak::class.java.getDeclaredField("status")
            field.trySetAccessible()
            field.set(sak, status)
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
    }
}