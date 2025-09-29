package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.hendelse.mottak.HåndterMottattDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

private const val INNSENDINGSREFERANSE_TYPE = "innsendingsreferanse_type"
private const val INNSENDINGSREFERANSE_VERDI = "innsendingsreferanse_verdi"

class HåndterUbehandletDokumentJobbUtfører(
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val taSkrivelåsRepository: TaSkriveLåsRepository,
    private val håndterMottattDokumentService: HåndterMottattDokumentService
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val innsendingsreferanse = InnsendingReferanse(
            verdi = input.parameter(INNSENDINGSREFERANSE_VERDI),
            type = InnsendingReferanse.Type.valueOf(input.parameter(INNSENDINGSREFERANSE_TYPE))
        )
        val dokument = mottattDokumentRepository.hent(innsendingsreferanse)
        when (dokument.type) {
            InnsendingType.MELDEKORT -> håndterUbehandletMeldekort(dokument)
            else -> log.warn("Jobb ble opprettet for innsendingstype som ikke støttes")
        }
    }

    private fun håndterUbehandletMeldekort(dokument: MottattDokument) {
        val skrivelås = taSkrivelåsRepository.låsSak(dokument.sakId)

        val nyesteBehandling = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(dokument.sakId)
        requireNotNull(nyesteBehandling) {
            "Fant meldekort men ingen behandling for sak ${dokument.sakId}"
        }

        val åpenFørstegangsbehandling = nyesteBehandling.status().erÅpen() &&
                nyesteBehandling.typeBehandling() == TypeBehandling.Førstegangsbehandling

        if (åpenFørstegangsbehandling) {
            log.info("Fant ubehandlet meldekort med åpen førstegangsbehandling for sak ${dokument.sakId}. Ignorerer dokumentet")
        } else {
            log.info("Fant ubehandlet meldekort, men ingen åpen førstegangsbehandling. Oppretter ny behandling")
            val melding = dokument.ustrukturerteData()?.let { DefaultJsonMapper.fromJson<Melding>(it) }
            requireNotNull(melding) {
                "Klarte ikke parse meldekort som melding"
            }

            håndterMottattDokumentService.håndterMottatteDokumenter(
                dokument.sakId,
                dokument.referanse,
                dokument.mottattTidspunkt,
                dokument.type,
                melding
            )
        }
        taSkrivelåsRepository.verifiserSkrivelås(skrivelås)
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return HåndterUbehandletDokumentJobbUtfører(
                mottattDokumentRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                taSkrivelåsRepository = repositoryProvider.provide(),
                håndterMottattDokumentService = HåndterMottattDokumentService(repositoryProvider, gatewayProvider)
            )
        }

        fun nyJobb(sakId: SakId, innsendingsreferanse: InnsendingReferanse) =
            JobbInput(HåndterUbehandletDokumentJobbUtfører)
                .apply {
                    forSak(sakId.toLong())
                        .medParameter(INNSENDINGSREFERANSE_VERDI, innsendingsreferanse.verdi)
                        .medParameter(INNSENDINGSREFERANSE_TYPE, innsendingsreferanse.type.name)
                }

        override val type = "hendelse.HåndterUbehandletDokument"

        override val navn = "Håndter ubehandlet dokument"

        override val beskrivelse =
            "Håndterer ubehandlet dokument."

    }
}