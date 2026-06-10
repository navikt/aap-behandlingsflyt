package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.TestAutomatiskMeldekortSakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import no.nav.aap.verdityper.dokument.Kanal
import kotlin.random.Random
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class SendAutomatiskMeldekortJobbUtfører(
    private val automatiskMeldekortSakRepository: TestAutomatiskMeldekortSakRepository,
    private val behandlingService: BehandlingService,
    private val rettighetstypeRepository: RettighetstypeRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository,
    private val flytJobbRepository: FlytJobbRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        if (Miljø.erProd()) {
            log.warn("SendAutomatiskMeldekortJobbUtfører skal ikke kjøre i produksjon")
            return
        }

        val idag = LocalDate.now(clock)
        val saker = automatiskMeldekortSakRepository.hentAlle()

        log.info("Behandler ${saker.size} saker for automatisk meldekort")

        saker.forEach { sendMeldekortHvisAktiv(it, idag) }
    }

    private fun sendMeldekortHvisAktiv(sakId: SakId, idag: LocalDate) {
        fullførteMeldeperioder(sakId, idag).forEach { sendMeldekort(sakId, it) }
    }

    private fun fullførteMeldeperioder(sakId: SakId, idag: LocalDate): List<Periode> =
        behandlingService.finnSisteYtelsesbehandlingFor(sakId)
            ?.takeIf { harAktivRettighet(it.id, idag) }
            ?.let { sisteBehandling ->
                meldeperiodeRepository.hentFørsteMeldeperiode(sisteBehandling.id)
                    ?.let { førsteMeldeperiode ->
                        meldeperiodeRepository
                            .hentMeldeperioder(sisteBehandling.id, Periode(førsteMeldeperiode.fom, idag))
                            .filter { it.tom < idag }
                            .ifEmpty {
                                log.info("Sak $sakId har ingen fullførte meldeperioder frem til $idag, hopper over")
                                emptyList()
                            }
                    }
                    ?: tomListeMedLogg("Sak $sakId har ingen fastsatt meldeperiode, hopper over")
            }
            ?: tomListeMedLogg("Sak $sakId har ingen aktiv ytelsesbehandling på $idag, hopper over")

    private fun sendMeldekort(sakId: SakId, periode: Periode) {
        val meldekort = MeldekortV0(
            harDuArbeidet = false,
            timerArbeidPerPeriode = listOf(
                ArbeidIPeriodeV0(
                    fraOgMedDato = periode.fom,
                    tilOgMedDato = periode.tom,
                    timerArbeid = 0.0,
                )
            ),
        )

        val jobb = HendelseMottattHåndteringJobbUtfører.nyJobb(
            sakId = sakId,
            dokumentReferanse = journalpostReferanse(),
            brevkategori = InnsendingType.MELDEKORT,
            kanal = Kanal.DIGITAL,
            melding = meldekort,
            mottattTidspunkt = LocalDateTime.now(clock),
        )

        flytJobbRepository.leggTil(jobb)
        log.info("Opprettet automatisk meldekort-jobb for sak $sakId (periode ${periode.fom}–${periode.tom})")
    }

    private fun harAktivRettighet(behandlingId: BehandlingId, idag: LocalDate): Boolean {
        val grunnlag = rettighetstypeRepository.hentHvisEksisterer(behandlingId) ?: return false
        return grunnlag.rettighetstypeTidslinje.begrensetTil(Periode(idag, idag)).isNotEmpty()
    }

    private fun journalpostReferanse() = InnsendingReferanse(
        type = InnsendingReferanse.Type.JOURNALPOST,
        verdi = Random.nextLong(1_000_000_000L, 9_999_999_999_999L).toString(),
    )

    private fun tomListeMedLogg(melding: String): List<Periode> {
        log.info(melding)
        return emptyList()
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører =
            SendAutomatiskMeldekortJobbUtfører(
                automatiskMeldekortSakRepository = repositoryProvider.provide(),
                behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
                rettighetstypeRepository = repositoryProvider.provide(),
                meldeperiodeRepository = repositoryProvider.provide(),
                flytJobbRepository = repositoryProvider.provide(),
            )

        override val type = "batch.SendAutomatiskMeldekort"
        override val navn = "Send automatisk meldekort for test-saker"
        override val beskrivelse = "Sender ukentlig meldekort (0 timer) for test-saker registrert via Dolly."

        /** Kjøres mandag kl. 07:00 */
        override val cron = CronExpression.createWithoutSeconds("0 7 * * 1")
    }
}
