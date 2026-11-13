package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.OppgittUtenlandsopplysningerRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.verdityper.dokument.JournalpostId
import java.util.concurrent.ConcurrentHashMap

object InMemoryOppgittUtenlandsopplysningerRepository : OppgittUtenlandsopplysningerRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, UtenlandsOppholdData>()

    override fun hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId: BehandlingId): UtenlandsOppholdData? =
        grunnlag[behandlingId]

    override fun hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sakId: SakId): UtenlandsOppholdData? =
        InMemoryBehandlingRepository.hentAlleFor(sakId, TypeBehandling.entries)
            .filter { InMemoryAvbrytRevurderingRepository.hentHvisEksisterer(it.id) == null }
            .sortedByDescending { it.opprettetTidspunkt }
            .firstNotNullOfOrNull { grunnlag[it.id] }

    override fun lagreOppgittUtenlandsOppplysninger(
        behandlingId: BehandlingId,
        journalpostId: JournalpostId,
        utenlandsOppholdData: UtenlandsOppholdData
    ) {
        grunnlag[behandlingId] = utenlandsOppholdData.copy(
            utenlandsOpphold = utenlandsOppholdData.utenlandsOpphold.orEmpty().toList()
        )
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Opplysningene hentes fra siste relevante behandling i saken.
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}
