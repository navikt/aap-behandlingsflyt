package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.OppgittUtenlandsopplysningerRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.verdityper.dokument.JournalpostId
import java.util.concurrent.ConcurrentHashMap

object InMemoryOppgittUtenlandsopplysningerRepository : OppgittUtenlandsopplysningerRepository {
    private val data = ConcurrentHashMap<BehandlingId, UtenlandsOppholdData>()

    override fun hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId: BehandlingId): UtenlandsOppholdData? =
        data[behandlingId]

    // Sakstilknytning er ikke modellert i minnet
    override fun hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sakId: SakId): UtenlandsOppholdData? = null

    override fun lagreOppgittUtenlandsOppplysninger(
        behandlingId: BehandlingId,
        journalpostId: JournalpostId,
        utenlandsOppholdData: UtenlandsOppholdData
    ) {
        data[behandlingId] = utenlandsOppholdData
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        data[fraBehandling]?.let { data[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        data.remove(behandlingId)
    }
}
