package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dokument.mottak.DokumentType
import no.nav.aap.behandlingsflyt.dokument.mottak.MottaDokumentService
import no.nav.aap.behandlingsflyt.dokument.mottak.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.dokument.mottak.pliktkort.MottakAvPliktkortRepository
import no.nav.aap.behandlingsflyt.dokument.mottak.pliktkort.UbehandletPliktkort
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.pliktkort.Pliktkort
import no.nav.aap.behandlingsflyt.prosessering.SakSkrivelås
import no.nav.aap.behandlingsflyt.prosessering.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(SakHendelsesHåndterer::class.java)

class SakHendelsesHåndterer(connection: DBConnection) {

    private val sakOgBehandlingService = SakOgBehandlingService(connection)
    private val låsRepository = TaSkriveLåsRepository(connection)
    private val mottaDokumentService = MottaDokumentService(
        mottattDokumentRepository = MottattDokumentRepository(connection),
        pliktkortRepository = MottakAvPliktkortRepository(connection)
    )

    fun håndtere(key: Saksnummer, hendelse: SakHendelse): BehandlingId? {
        val sakSkrivelås = låsRepository.låsSak(key)
        val relevantBehandling = sakOgBehandlingService.finnEnRelevantBehandling(key)

        if (hendelse is DokumentMottattSakHendelse) {
            håndtere(key, hendelse, sakSkrivelås, relevantBehandling)
        }
        låsRepository.verifiserSkrivelås(sakSkrivelås)
        return relevantBehandling.id
    }

    fun håndtere(
        key: Saksnummer,
        hendelse: DokumentMottattSakHendelse,
        sakSkrivelås: SakSkrivelås,
        relevantBehandling: Behandling
    ) {
        val behandlingSkrivelås = låsRepository.låsBehandling(relevantBehandling.id)

        log.info("Mottatt dokument av typen {} på sak {}", hendelse.strukturertDokument.brevkode, key)

        when (hendelse.strukturertDokument.brevkode) {
            Brevkode.PLIKTKORT -> {
                val pliktkort = UbehandletPliktkort(
                    hendelse.journalpost,
                    (hendelse.strukturertDokument.data as Pliktkort).timerArbeidPerPeriode
                )
                mottaDokumentService.håndterMottattPliktkort(
                    sakId = sakSkrivelås.id,
                    journalpostId = hendelse.journalpost,
                    mottattTidspunkt = hendelse.mottattTidspunkt,
                    pliktKort = pliktkort
                )
            }

            Brevkode.SØKNAD -> {
                mottaDokumentService.håndterMottattDokument(
                    journalpostId = hendelse.journalpost,
                    sakId = sakSkrivelås.id,
                    mottattTidspunkt = hendelse.mottattTidspunkt,
                    dokumentType = DokumentType.SØKNAD
                )
            }

            else -> {
                throw IllegalArgumentException("Ukjent brevkode[${hendelse.strukturertDokument.brevkode}], vet ikke hvordan denne skal håndteres")
            }
        }
        låsRepository.verifiserSkrivelås(behandlingSkrivelås)
    }
}