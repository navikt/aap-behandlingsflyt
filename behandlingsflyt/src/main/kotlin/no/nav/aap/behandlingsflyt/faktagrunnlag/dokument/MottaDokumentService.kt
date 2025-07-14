package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.adapter.UbehandletMeldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.UbehandletDialogmelding
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.UbehandletLegeerklæring
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.adapter.UbehandletSøknad
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Oppfølgingsoppgave
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Søknad
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class MottaDokumentService(
    private val mottattDokumentRepository: MottattDokumentRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        mottattDokumentRepository = repositoryProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun mottattDokument(
        referanse: InnsendingReferanse,
        sakId: SakId,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        kanal: Kanal,
        strukturertDokument: StrukturerteData?
    ) {
        log.info("Lagrer mottatt dokument. Referanse: ${referanse}, SakId: $sakId, Brevkategori: $brevkategori, MottattTidspunkt: $mottattTidspunkt, Kanal: $kanal")
        mottattDokumentRepository.lagre(
            MottattDokument(
                referanse = referanse,
                sakId = sakId,
                mottattTidspunkt = mottattTidspunkt,
                type = brevkategori,
                kanal = kanal,
                status = Status.MOTTATT,
                behandlingId = null,
                strukturertDokument = strukturertDokument
            )
        )
    }

    fun meldekortSomIkkeErBehandlet(sakId: SakId): Set<UbehandletMeldekort> {
        val ubehandledeMeldekort =
            mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, InnsendingType.MELDEKORT)

        return ubehandledeMeldekort
            .map {
                UbehandletMeldekort.fraKontrakt(
                    meldekort = meldekort(it),
                    journalpostId = it.referanse.asJournalpostId,
                    mottattTidspunkt = it.mottattTidspunkt
                )
            }
            .toSet()
    }

    private fun meldekort(dokument: MottattDokument): Meldekort {
        return requireNotNull(dokument.strukturerteData<Meldekort>()?.data)
    }

    fun aktivitetskortSomIkkeErBehandlet(sakId: SakId): Set<InnsendingId> {
        val ubehandledeAktivitetskort =
            mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, InnsendingType.AKTIVITETSKORT)

        return ubehandledeAktivitetskort
            .map { it.referanse.asInnsendingId }
            .toSet()
    }

    fun søknaderSomIkkeHarBlittBehandlet(sakId: SakId): Set<UbehandletSøknad> {
        val ubehandledeSøknader =
            mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, InnsendingType.SØKNAD)

        return ubehandledeSøknader.map { mapSøknad(it) }.toSet()
    }

    fun legeerklæringerSomIkkeHarBlittBehandlet(sakId: SakId): Set<UbehandletLegeerklæring> {
        val ubehandledeLegeerklæringer =
            mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, InnsendingType.LEGEERKLÆRING)
        return ubehandledeLegeerklæringer.map { mapLegeerklæring(it) }.toSet()
    }

    fun dialogmeldingerSomIkkeHarBlittBehandlet(sakId: SakId): Set<UbehandletDialogmelding> {
        val ubehandledeDialogmeldinger =
            mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, InnsendingType.DIALOGMELDING)
        return ubehandledeDialogmeldinger.map { mapDialogmelding(it) }.toSet()
    }

    private fun mapLegeerklæring(mottattDokument: MottattDokument): UbehandletLegeerklæring {
        val mottattDato = mottattDokument.mottattTidspunkt.toLocalDate()
        return UbehandletLegeerklæring(
            mottattDokument.referanse.asJournalpostId,
            Periode(mottattDato, mottattDato)
        )
    }

    private fun mapDialogmelding(mottattDokument: MottattDokument): UbehandletDialogmelding {
        val mottattDato = mottattDokument.mottattTidspunkt.toLocalDate()
        return UbehandletDialogmelding(
            mottattDokument.referanse.asJournalpostId,
            Periode(mottattDato, mottattDato)
        )
    }

    private fun mapSøknad(mottattDokument: MottattDokument): UbehandletSøknad {
        val søknad =
            requireNotNull(mottattDokument.strukturerteData<Søknad>()).data

        val mottattDato = mottattDokument.mottattTidspunkt.toLocalDate()

        return UbehandletSøknad.fraKontrakt(
            søknad = søknad,
            mottattDato = mottattDato,
            journalPostId = mottattDokument.referanse.asJournalpostId,
        )
    }

    fun knyttTilBehandling(sakId: SakId, behandlingId: BehandlingId, referanse: InnsendingReferanse) {
        mottattDokumentRepository.oppdaterStatus(referanse, behandlingId, sakId, Status.BEHANDLET)
    }

    fun hentOppfølgingsBehandlingDokument(behandlingId: BehandlingId): BehandletOppfølgingsOppgave {
        val uthentede = mottattDokumentRepository.hentDokumenterAvType(behandlingId, InnsendingType.OPPFØLGINGSOPPGAVE)

        require(uthentede.size == 1) { "Forventer kun ett dokument per behandling." }

        val dokument = uthentede.first().strukturerteData<Oppfølgingsoppgave>()!!.data

        return BehandletOppfølgingsOppgave.fraDokument(dokument)
    }
}