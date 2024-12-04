package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.adapter.UbehandletPliktkort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.UbehandletDialogmelding
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.UbehandletLegeerklæring
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.pliktkort.Pliktkort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.Søknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.adapter.UbehandletSøknad
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDateTime

class MottaDokumentService(
    private val mottattDokumentRepository: MottattDokumentRepository,
) {

    fun mottattDokument(
        referanse: InnsendingReferanse,
        sakId: SakId,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        kanal: Kanal,
        strukturertDokument: StrukturertDokument<*>
    ) {
        mottattDokumentRepository.lagre(
            MottattDokument(
                referanse = referanse,
                sakId = sakId,
                mottattTidspunkt = mottattTidspunkt,
                type = brevkategori,
                status = Status.MOTTATT,
                behandlingId = null,
                strukturertDokument = strukturertDokument,
                kanal = kanal
            )
        )
    }

    fun mottattDokument(
        referanse: InnsendingReferanse,
        sakId: SakId,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        kanal: Kanal,
        strukturertDokument: UnparsedStrukturertDokument
    ) {
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

    fun pliktkortSomIkkeErBehandlet(sakId: SakId): Set<UbehandletPliktkort> {
        val ubehandledePliktkort =
            mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, InnsendingType.PLIKTKORT)

        return ubehandledePliktkort.map {
            UbehandletPliktkort(
                it.referanse.asJournalpostId,
                it.strukturerteData<Pliktkort>()!!.data.timerArbeidPerPeriode
            )
        }.toSet()
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
        val søknad = requireNotNull(mottattDokument.strukturerteData<Søknad>()).data

        val mottattDato = mottattDokument.mottattTidspunkt.toLocalDate()
        return UbehandletSøknad(
            mottattDokument.referanse.asJournalpostId,
            Periode(mottattDato, mottattDato),
            søknad.student.erStudent(),
            søknad.student.skalGjennopptaStudie(),
            søknad.harYrkesskade(),
            søknad.oppgitteBarn
        )
    }

    fun knyttTilBehandling(sakId: SakId, behandlingId: BehandlingId, referanse: InnsendingReferanse) {
        mottattDokumentRepository.oppdaterStatus(referanse, behandlingId, sakId, Status.BEHANDLET)
    }
}