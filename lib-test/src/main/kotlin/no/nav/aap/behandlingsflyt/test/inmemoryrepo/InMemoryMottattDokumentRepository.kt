package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.DokumentRekkefølge
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.json.DefaultJsonMapper

object InMemoryMottattDokumentRepository : MottattDokumentRepository {
    private val memory = mutableListOf<MottattDokument>()
    private val lock = Object()


    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        synchronized(lock) {
            memory
                .filter { it.behandlingId == fraBehandling }
                .forEach {
                    memory.add(
                        MottattDokument(
                            referanse = it.referanse,
                            sakId = it.sakId,
                            behandlingId = tilBehandling,
                            mottattTidspunkt = it.mottattTidspunkt,
                            type = it.type,
                            kanal = it.kanal,
                            status = it.status,
                            strukturertDokument = it.strukturertDokument,
                        )
                    )

                }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            memory.removeIf { it.behandlingId == behandlingId }
        }
    }

    override fun lagre(mottattDokument: MottattDokument) {
        synchronized(lock) {
            memory.add(mottattDokument)
        }
    }

    override fun oppdaterStatus(
        dokumentReferanse: InnsendingReferanse,
        behandlingId: BehandlingId,
        sakId: SakId,
        status: Status
    ) {
        synchronized(lock) {
            memory
                .filter { it.referanse == dokumentReferanse && it.sakId == sakId }
                .forEach {
                    memory.remove(it)
                    memory.add(
                        MottattDokument(
                            referanse = it.referanse,
                            sakId = it.sakId,
                            behandlingId = behandlingId,
                            mottattTidspunkt = it.mottattTidspunkt,
                            type = it.type,
                            kanal = it.kanal,
                            status = status,
                            strukturertDokument = it.strukturertDokument
                        )
                    )
                }
        }
    }

    override fun hentUbehandledeDokumenterAvType(
        sakId: SakId,
        dokumentType: InnsendingType
    ): Set<MottattDokument> {
        synchronized(lock) {
            return memory.filter { it.type == dokumentType && it.sakId == sakId && it.status == Status.MOTTATT }.toSet()
        }
    }

    override fun hentDokumentRekkefølge(
        sakId: SakId,
        type: InnsendingType
    ): Set<DokumentRekkefølge> {
        synchronized(lock) {
            return memory
                .filter { it.type == type && it.sakId == sakId && it.status == Status.BEHANDLET }
                .map { DokumentRekkefølge(it.referanse, it.mottattTidspunkt) }.toSet()
        }
    }

    override fun hentDokumenterAvType(
        sakId: SakId,
        type: InnsendingType
    ): Set<MottattDokument> {
        synchronized(lock) {
            return memory.filter { it.type == type && it.sakId == sakId }.toSet()
        }
    }

    override fun hentDokumenterAvType(
        behandlingId: BehandlingId,
        type: InnsendingType
    ): Set<MottattDokument> {
        synchronized(lock) {
            return memory.filter { it.type == type && it.behandlingId == behandlingId }.toSet()
        }
    }

    override fun hentDokumenterAvType(
        behandlingId: BehandlingId,
        typer: List<InnsendingType>
    ): Set<MottattDokument> {
        synchronized(lock) {
            return memory.filter { it.type in typer && it.behandlingId == behandlingId }.toSet()
        }
    }
}