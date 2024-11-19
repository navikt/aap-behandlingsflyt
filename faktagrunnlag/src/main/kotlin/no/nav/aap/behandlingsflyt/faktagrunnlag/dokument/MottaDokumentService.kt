package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.InnsendingId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.adapter.UbehandletPliktkort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.UbehandletLegeerklæring
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.pliktkort.Pliktkort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.Søknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.adapter.UbehandletSøknad
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Kanal
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.LocalDateTime

class MottaDokumentService(
    private val mottattDokumentRepository: MottattDokumentRepository,
) {

    fun mottattDokument(
        referanse: MottattDokumentReferanse,
        sakId: SakId,
        mottattTidspunkt: LocalDateTime,
        brevkode: Brevkode,
        kanal: Kanal,
        strukturertDokument: StrukturertDokument<*>
    ) {
        mottattDokumentRepository.lagre(
            MottattDokument(
                referanse = referanse,
                sakId = sakId,
                mottattTidspunkt = mottattTidspunkt,
                type = brevkode,
                status = Status.MOTTATT,
                behandlingId = null,
                strukturertDokument = strukturertDokument,
                kanal = kanal
            )
        )
    }

    fun mottattDokument(
        referanse: MottattDokumentReferanse,
        sakId: SakId,
        mottattTidspunkt: LocalDateTime,
        brevkode: Brevkode,
        kanal: Kanal,
        strukturertDokument: UnparsedStrukturertDokument
    ) {
        mottattDokumentRepository.lagre(
            MottattDokument(
                referanse = referanse,
                sakId = sakId,
                mottattTidspunkt = mottattTidspunkt,
                type = brevkode,
                kanal = kanal,
                status = Status.MOTTATT,
                behandlingId = null,
                strukturertDokument = strukturertDokument
            )
        )
    }

    fun pliktkortSomIkkeErBehandlet(sakId: SakId): Set<UbehandletPliktkort> {
        val ubehandledePliktkort =
            mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, Brevkode.PLIKTKORT)

        return ubehandledePliktkort.map {
            UbehandletPliktkort(
                it.referanse.asJournalpostId,
                it.strukturerteData<Pliktkort>()!!.data.timerArbeidPerPeriode
            )
        }.toSet()
    }

    fun aktivitetskortSomIkkeErBehandlet(sakId: SakId): Set<InnsendingId> {
        val ubehandledeAktivitetskort = mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, Brevkode.AKTIVITETSKORT)

        return ubehandledeAktivitetskort
            .map { it.referanse.asInnsendingId }
            .toSet()
    }

    fun søknaderSomIkkeHarBlittBehandlet(sakId: SakId): Set<UbehandletSøknad> {
        val ubehandledeSøknader =
            mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, Brevkode.SØKNAD)

        return ubehandledeSøknader.map { mapSøknad(it) }.toSet()
    }

    fun legeerklæringerSomIkkeHarBlittBehandlet(sakId: SakId) : Set<UbehandletLegeerklæring> {
        val ubehandledeLegeerklæringer = mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, Brevkode.LEGEERKLÆRING_MOTTATT)
        return ubehandledeLegeerklæringer.map { mapLegeerklæring(it) }.toSet()
    }

    private fun mapLegeerklæring(mottattDokument: MottattDokument): UbehandletLegeerklæring {
        val mottattDato = mottattDokument.mottattTidspunkt.toLocalDate()
        return UbehandletLegeerklæring(
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

    fun knyttTilBehandling(sakId: SakId, behandlingId: BehandlingId, referanse: MottattDokumentReferanse) {
        mottattDokumentRepository.oppdaterStatus(referanse, behandlingId, sakId, Status.BEHANDLET)
    }
}