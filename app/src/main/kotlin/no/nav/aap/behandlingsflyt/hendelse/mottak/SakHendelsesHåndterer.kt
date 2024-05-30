package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.SakSkrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(SakHendelsesHåndterer::class.java)

class SakHendelsesHåndterer(connection: DBConnection) {

    private val sakOgBehandlingService = SakOgBehandlingService(connection)
    private val låsRepository = TaSkriveLåsRepository(connection)
    private val grunnlagKopierer = GrunnlagKopierer(connection)
    private val mottaDokumentService = MottaDokumentService(
        mottattDokumentRepository = MottattDokumentRepository(connection)
    )

    fun håndtere(key: Saksnummer, hendelse: SakHendelse): BehandlingId? {
        val sakSkrivelås = låsRepository.låsSak(key)
        val beriketBehandling = sakOgBehandlingService.finnEllerOpprettBehandling(
            key,
            utledÅrsaker(hendelse)
        )
        val relevantBehandling = beriketBehandling.behandling

        if (beriketBehandling.skalKopierFraSisteBehandling()) {
            grunnlagKopierer.overfør(requireNotNull(beriketBehandling.sisteAvsluttedeBehandling), relevantBehandling.id)
        }

        if (hendelse is DokumentMottattSakHendelse) {
            håndtere(key, hendelse, sakSkrivelås, relevantBehandling)
        }
        if (hendelse is AktivitetsmeldingMottattSakHendelse) {
            log.info("Mottatt melding fra TorsHammer, ignorer for nå")
        }
        låsRepository.verifiserSkrivelås(sakSkrivelås)
        return relevantBehandling.id
    }

    private fun utledÅrsaker(hendelse: SakHendelse): List<Årsak> {
        return when (hendelse) {
            is DokumentMottattSakHendelse -> when (hendelse.strukturertDokument.brevkode) {
                Brevkode.SØKNAD -> listOf(Årsak(EndringType.MOTTATT_SØKNAD))
                Brevkode.PLIKTKORT -> listOf(
                    Årsak(
                        EndringType.MOTTATT_MELDEKORT,
                        hendelse.strukturertDokument.periode()
                    )
                )

                Brevkode.UKJENT -> TODO()
            }

            is AktivitetsmeldingMottattSakHendelse -> {
                listOf(
                    Årsak(
                        EndringType.MOTTATT_AKTIVITETSMELDING,
                        Periode(hendelse.hammer.dato, hendelse.hammer.dato)
                    )
                )
            }

            else -> {
                listOf()
            }
        }
    }

    fun håndtere(
        key: Saksnummer,
        hendelse: DokumentMottattSakHendelse,
        sakSkrivelås: SakSkrivelås,
        relevantBehandling: Behandling
    ) {
        val behandlingSkrivelås = låsRepository.låsBehandling(relevantBehandling.id)

        log.info("Mottatt dokument av typen {} på sak {}", hendelse.strukturertDokument.brevkode, key)

        mottaDokumentService.håndterMottattDokument(
            journalpostId = hendelse.journalpost,
            sakId = sakSkrivelås.id,
            mottattTidspunkt = hendelse.mottattTidspunkt,
            brevkode = hendelse.strukturertDokument.brevkode,
            strukturertDokument = hendelse.strukturertDokument
        )
        låsRepository.verifiserSkrivelås(behandlingSkrivelås)
    }

}

