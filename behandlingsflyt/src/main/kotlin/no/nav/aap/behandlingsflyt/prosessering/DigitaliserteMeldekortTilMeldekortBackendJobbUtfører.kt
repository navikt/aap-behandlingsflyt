package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.meldekort.kontrakt.Periode
import no.nav.aap.meldekort.kontrakt.sak.BehandslingsflytUtfyllingRequest
import no.nav.aap.meldekort.kontrakt.sak.TimerArbeidetDto
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.verdityper.dokument.JournalpostId

class DigitaliserteMeldekortTilMeldekortBackendJobbUtfører(
    private val meldekortGateway: MeldekortGateway,
    private val sakRepository: SakRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val mottaDokumentService: MottaDokumentService
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val journalpostId = input.payload<JournalpostId>()
        val sak = sakRepository.hent(SakId(input.sakId()))

        val journalpost = mottattDokumentRepository.hent(InnsendingReferanse(journalpostId))
        val ubehandletMeldekort = mottaDokumentService.tilUbehandletMeldekort(journalpost)

        if (ubehandletMeldekort.timerArbeidPerPeriode.isEmpty()) return

        val request = BehandslingsflytUtfyllingRequest(
            saksnummer = sak.saksnummer.toString(),
            ident = sak.person.aktivIdent().identifikator,
            sakenGjelderFor = sak.rettighetsperiode.somKontraktperiode,
            periode = Periode(
                fom = ubehandletMeldekort.timerArbeidPerPeriode.minOf { it.periode.fom },
                tom = ubehandletMeldekort.timerArbeidPerPeriode.maxOf { it.periode.tom }
            ),
            harDuJobbet = ubehandletMeldekort.harDuArbeidet,
            dager = ubehandletMeldekort.timerArbeidPerPeriode.map {
                TimerArbeidetDto(it.periode.fom, it.timerArbeid.antallTimer.toDouble())
            }
        )
        meldekortGateway.sendTimerArbeidetIPeriode(request)
    }

    companion object : ProvidersJobbSpesifikasjon {
        override val navn = "DigitaliserteMeldekortTilMeldekortBackend"
        override val type = "flyt.digitaliserteMeldekortTilMeldekortBackend"
        override val beskrivelse = """
            Push informasjon til meldekort-backend slik at vi får fjernet varsling om 
            allerede innsendte meldekort som er blitt digitalisert i behandlingsflyt
            """.trimIndent()

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): JobbUtfører {
            return DigitaliserteMeldekortTilMeldekortBackendJobbUtfører(
                gatewayProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                MottaDokumentService(repositoryProvider)
            )
        }

        fun nyJobb(
            sakId: SakId,
            behandlingId: BehandlingId,
        ) = JobbInput(DigitaliserteMeldekortTilMeldekortBackendJobbUtfører).apply {
            forBehandling(sakId.toLong(), behandlingId.toLong())
        }
    }

    private val no.nav.aap.komponenter.type.Periode.somKontraktperiode: Periode
        get() = Periode(fom, tom)
}