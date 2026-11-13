package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikkGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemoryPersonopplysningForutgåendeRepository : PersonopplysningForutgåendeRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, PersonopplysningMedHistorikkGrunnlag>()
    private val nesteId = AtomicLong()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): PersonopplysningMedHistorikkGrunnlag? =
        grunnlag[behandlingId]

    override fun lagre(behandlingId: BehandlingId, personopplysning: PersonopplysningMedHistorikk) {
        grunnlag.compute(behandlingId) { _, eksisterende ->
            if (eksisterende?.brukerPersonopplysning == personopplysning) {
                eksisterende
            } else {
                PersonopplysningMedHistorikkGrunnlag(
                    personopplysning.copy(
                        id = nesteId.incrementAndGet(),
                        statsborgerskap = personopplysning.statsborgerskap.toList(),
                        folkeregisterStatuser = personopplysning.folkeregisterStatuser.toList(),
                        utenlandsAddresser = personopplysning.utenlandsAddresser.orEmpty().toList()
                    )
                )
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        grunnlag[fraBehandling]?.let { grunnlag[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}
