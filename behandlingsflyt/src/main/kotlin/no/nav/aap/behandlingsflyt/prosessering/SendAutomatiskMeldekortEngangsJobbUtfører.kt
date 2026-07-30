package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.verdityper.dokument.Kanal
import kotlin.random.Random
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/** Engangsjobb som sender automatisk meldekort for én enkelt test-sak rett etter opprettelse via Dolly. */
class SendAutomatiskMeldekortEngangsJobbUtfører(
    private val underveisRepository: UnderveisRepository,
    private val flytJobbRepository: FlytJobbRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        if (Miljø.erProd()) {
            log.warn("SendAutomatiskMeldekortEngangsJobbUtfører skal ikke kjøre i produksjon")
            return
        }

        val sakId = SakId(input.sakId())
        val idag = LocalDate.now(clock)

        val ubesvarte = underveisRepository.hentUbesvarteMeldeperioderForDollyJobb(listOf(sakId), idag)
        log.info("Fant ${ubesvarte.values.sumOf { it.size }} ubesvarte meldeperioder for sak $sakId")

        ubesvarte.forEach { (id, perioder) ->
            perioder.forEach { sendMeldekort(id, it) }
        }
    }

    private fun sendMeldekort(sakId: SakId, periode: Periode) {
        val meldekort = MeldekortV0(
            harDuArbeidet = false,
            timerArbeidPerPeriode = periode.dager().map {
                ArbeidIPeriodeV0(
                    fraOgMedDato = it,
                    tilOgMedDato = it,
                    timerArbeid = 0.0,
                )
            },
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

    private fun journalpostReferanse() = InnsendingReferanse(
        type = InnsendingReferanse.Type.JOURNALPOST,
        verdi = Random.nextLong(1_000_000_000L, 9_999_999_999_999L).toString(),
    )

    companion object : ProvidersJobbSpesifikasjon {
        fun nyEngangsJobb(sakId: SakId): JobbInput =
            JobbInput(SendAutomatiskMeldekortEngangsJobbUtfører).forSak(sakId.toLong())

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører =
            SendAutomatiskMeldekortEngangsJobbUtfører(
                underveisRepository = repositoryProvider.provide(),
                flytJobbRepository = repositoryProvider.provide(),
            )

        override val type = "batch.dolly.sendautomatiskmeldekort.engangs"
        override val navn = "Send automatisk meldekort for enkelt test-sak"
        override val beskrivelse = "Sender umiddelbart meldekort (0 timer) for én test-sak etter opprettelse via Dolly."
        // Ingen cron — kjøres kun én gang ved opprettelse
    }
}
