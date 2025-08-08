package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.DokumentRekkefølge
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryMeldekortRepository : MeldekortRepository {
    private val meldekort = HashMap<BehandlingId, Set<Meldekort>>()


    override fun hent(behandlingId: BehandlingId): MeldekortGrunnlag {
        return hentHvisEksisterer(behandlingId)
            ?: throw IllegalStateException("Fant ikke meldekort for behandling $behandlingId")
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MeldekortGrunnlag? {
        return meldekort[behandlingId]?.let {
            MeldekortGrunnlag(
                meldekortene = it,
                rekkefølge = it.sortedBy { it.mottattTidspunkt }.map {
                    DokumentRekkefølge(
                        referanse = InnsendingReferanse(it.journalpostId),
                        mottattTidspunkt = it.mottattTidspunkt
                    )
                }.toSet()
            )
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        meldekortene: Set<Meldekort>
    ) {
        synchronized(this) {
            meldekort[behandlingId] = meldekortene
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(this) {
            meldekort.remove(behandlingId)
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) = synchronized(this) {

    }
}