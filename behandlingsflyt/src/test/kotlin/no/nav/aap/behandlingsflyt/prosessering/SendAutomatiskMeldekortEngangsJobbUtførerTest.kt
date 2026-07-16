package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.SetSystemProperty
import java.time.LocalDate

@SetSystemProperty(key = "NAIS_CLUSTER_NAME", value = "LOCAL")
class SendAutomatiskMeldekortEngangsJobbUtførerTest {

    private val sak = opprettInMemorySak()
    private val sakId = sak.id
    private val jobbInput = JobbInput(SendAutomatiskMeldekortEngangsJobbUtfører).forSak(sakId.toLong())

    @Test
    fun `sender meldekort for ubesvart meldeperiode`() {
        val idag = LocalDate.of(2026, 6, 9)
        val periode = Periode(idag.minusDays(14), idag.minusDays(1))
        InMemoryUnderveisRepository.settUbesvarte(sakId, listOf(periode))

        lagUtfører(idag).utfør(jobbInput)

        assertThat(InMemoryFlytJobbRepository.hentJobberForSak(sakId.toLong())).isNotEmpty()
    }

    @Test
    fun `sender ikke meldekort når det ikke finnes ubesvarte meldeperioder`() {
        val idag = LocalDate.of(2026, 6, 9)

        lagUtfører(idag).utfør(jobbInput)

        assertThat(InMemoryFlytJobbRepository.hentJobberForSak(sakId.toLong())).isEmpty()
    }

    @Test
    fun `har ingen cron og reschedules dermed ikke`() {
        assertThat(JobbInput(SendAutomatiskMeldekortEngangsJobbUtfører).erScheduledOppgave()).isFalse()
    }

    private fun lagUtfører(idag: LocalDate) = SendAutomatiskMeldekortEngangsJobbUtfører(
        underveisRepository = InMemoryUnderveisRepository,
        flytJobbRepository = InMemoryFlytJobbRepository,
        clock = fixedClock(idag),
    )
}
