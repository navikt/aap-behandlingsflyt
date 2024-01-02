package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.EndringType
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.behandling.Årsak
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.pliktkort.Pliktkort
import no.nav.aap.behandlingsflyt.mottak.DokumentType
import no.nav.aap.behandlingsflyt.mottak.MottaDokumentService
import no.nav.aap.behandlingsflyt.mottak.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.mottak.pliktkort.MottakAvPliktkortRepository
import no.nav.aap.behandlingsflyt.mottak.pliktkort.UbehandletPliktkort
import no.nav.aap.behandlingsflyt.prosessering.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sak.sakRepository
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(SakHendelsesHåndterer::class.java)

class SakHendelsesHåndterer(connection: DBConnection) {

    private val sakRepository = sakRepository(connection)
    private val behandlingRepository = behandlingRepository(connection)
    private val låsRepository = TaSkriveLåsRepository(connection)
    private val mottaDokumentService = MottaDokumentService(
        mottattDokumentRepository = MottattDokumentRepository(connection),
        pliktkortRepository = MottakAvPliktkortRepository(connection)
    )

    fun håndtere(key: Saksnummer, hendelse: SakHendelse): BehandlingId? {
        return when (hendelse) {
            is DokumentMottattSakHendelse -> {
                håndtere(key, hendelse)
            }

            else -> {
                finnEnRelevantBehandling(key)
            }
        }
    }

    fun håndtere(key: Saksnummer, hendelse: DokumentMottattSakHendelse): BehandlingId? {
        val sakSkrivelås = låsRepository.låsSak(key)
        val relevantBehandling = finnEnRelevantBehandling(key)
        val behandlingSkrivelås = låsRepository.låsBehandling(relevantBehandling)

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
        låsRepository.verifiserSkrivelås(sakSkrivelås)
        låsRepository.verifiserSkrivelås(behandlingSkrivelås)
        return relevantBehandling
    }

    private fun finnEnRelevantBehandling(key: Saksnummer): BehandlingId {
        val sak = sakRepository.hent(key)

        val sisteBehandlingOpt = behandlingRepository.finnSisteBehandlingFor(sak.id)

        val sisteBehandling = if (sisteBehandlingOpt != null && !sisteBehandlingOpt.status().erAvsluttet()) {
            sisteBehandlingOpt
        } else {
            // Har ikke behandling så oppretter en
            behandlingRepository.opprettBehandling(
                sak.id,
                listOf(Årsak(EndringType.MOTTATT_SØKNAD))
            ) // TODO: Reeltsett oppdatere denne
        }
        return sisteBehandling.id
    }
}