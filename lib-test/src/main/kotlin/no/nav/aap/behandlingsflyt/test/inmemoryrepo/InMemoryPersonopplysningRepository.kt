package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.personopplysninger.Personopplysning
import no.nav.aap.personopplysninger.PersonopplysningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryPersonopplysningRepository : PersonopplysningRepository {
    private val personopplysninger = ConcurrentHashMap<BehandlingId, Personopplysning>()
    private val lock = Any()

    fun hentHvisEksisterer(behandlingId: BehandlingId): PersonopplysningGrunnlag? {
        synchronized(lock) {
            val personopplysning = personopplysninger[behandlingId]

            return if (personopplysning != null) {
                PersonopplysningGrunnlag(
                    brukerPersonopplysning = personopplysning,
                )
            } else {
                null
            }
        }
    }

    override fun hentBrukerPersonOpplysningHvisEksisterer(behandlingId: BehandlingId): Personopplysning? {
        return synchronized(lock) {
            personopplysninger[behandlingId]
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        personopplysning: Personopplysning
    ) {
        synchronized(lock) {
            personopplysninger[behandlingId] = personopplysning
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        synchronized(lock) {
            require(fraBehandling != tilBehandling)

            personopplysninger[fraBehandling]?.let { personopplysning ->
                personopplysninger[tilBehandling] = personopplysning
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            personopplysninger.remove(behandlingId)
        }
    }
}
