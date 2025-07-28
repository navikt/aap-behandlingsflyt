package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.RelatertPersonopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.RelatertePersonopplysninger
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object InMemoryPersonopplysningRepository : PersonopplysningRepository {
    private val personopplysninger = ConcurrentHashMap<BehandlingId, Personopplysning>()
    private val barnMap = ConcurrentHashMap<BehandlingId, Set<Barn>>()
    private val lock = Object()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): PersonopplysningGrunnlag? {
        synchronized(lock) {
            val personopplysning = personopplysninger[behandlingId]
            val barn = barnMap[behandlingId]

            return if (personopplysning != null) {
                val relatertePersonopplysninger = if (!barn.isNullOrEmpty()) {
                    RelatertePersonopplysninger(
                        id = 0,
                        personopplysninger = barn.map { RelatertPersonopplysning(
                            person = Person(
                                identifikator = UUID.randomUUID(),
                                identer = listOf(it.ident)
                            ),
                            fødselsdato = it.fødselsdato,
                            dødsdato = it.dødsdato
                        ) })

                } else {
                    null
                }

                PersonopplysningGrunnlag(
                    brukerPersonopplysning = personopplysning,
                    relatertePersonopplysninger = relatertePersonopplysninger
                )
            } else {
                null
            }
        }
    }

    override fun hentBrukerPersonOpplysningHvisEksisterer(behandlingId: BehandlingId): Personopplysning? {
        TODO("Not yet implemented")
    }

    override fun lagre(
        behandlingId: BehandlingId,
        personopplysning: Personopplysning
    ) {
        synchronized(lock) {
            personopplysninger[behandlingId] = personopplysning
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        barn: Set<Barn>
    ) {
        synchronized(lock) {
            if (barn.isNotEmpty()) {
                barnMap[behandlingId] = barn
            } else {
                barnMap.remove(behandlingId)
            }
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

            barnMap[fraBehandling]?.let { barn ->
                barnMap[tilBehandling] = barn
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            personopplysninger.remove(behandlingId)
            barnMap.remove(behandlingId)
        }
    }
}