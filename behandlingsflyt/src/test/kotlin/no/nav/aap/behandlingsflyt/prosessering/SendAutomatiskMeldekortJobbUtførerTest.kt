package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import org.assertj.core.api.Assertions.assertThat
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTestAutomatiskMeldekortSakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.JobbInput
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.SetSystemProperty
import java.time.LocalDate

@SetSystemProperty(key = "NAIS_CLUSTER_NAME", value = "LOCAL")
class SendAutomatiskMeldekortJobbUtførerTest {

    private val sak = opprettInMemorySak()
    private val sakId = sak.id
    private val jobbInput = JobbInput(SendAutomatiskMeldekortJobbUtfører).forSak(sakId.toLong())

    @Test
    fun `sender meldekort for ubesvart meldeperiode`() {
        val idag = LocalDate.of(2026, 6, 9)
        val periode = Periode(idag.minusDays(14), idag.minusDays(1))
        InMemoryTestAutomatiskMeldekortSakRepository.leggTil(sakId)
        InMemoryUnderveisRepository.settUbesvarte(sakId, listOf(periode))

        lagUtfører(idag).utfør(jobbInput)

        assertThat(InMemoryFlytJobbRepository.hentJobberForSak(sakId.toLong())).isNotEmpty()
    }

    @Test
    fun `sender ikke meldekort når det ikke finnes ubesvarte meldeperioder`() {
        val idag = LocalDate.of(2026, 6, 9)
        InMemoryTestAutomatiskMeldekortSakRepository.leggTil(sakId)

        lagUtfører(idag).utfør(jobbInput)

        assertThat(InMemoryFlytJobbRepository.hentJobberForSak(sakId.toLong())).isEmpty()
    }

    @Test
    fun `sender meldekort for flere saker`() {
        val idag = LocalDate.of(2026, 6, 9)
        val annenSakId = opprettInMemorySak().id
        val periode = Periode(idag.minusDays(14), idag.minusDays(1))
        InMemoryTestAutomatiskMeldekortSakRepository.leggTil(sakId)
        InMemoryTestAutomatiskMeldekortSakRepository.leggTil(annenSakId)
        InMemoryUnderveisRepository.settUbesvarte(sakId, listOf(periode))
        InMemoryUnderveisRepository.settUbesvarte(annenSakId, listOf(periode))

        lagUtfører(idag).utfør(jobbInput)

        assertThat(InMemoryFlytJobbRepository.hentJobberForSak(sakId.toLong())).isNotEmpty()
        assertThat(InMemoryFlytJobbRepository.hentJobberForSak(annenSakId.toLong())).isNotEmpty()
    }

    private fun lagUtfører(idag: LocalDate) = SendAutomatiskMeldekortJobbUtfører(
        automatiskMeldekortSakRepository = InMemoryTestAutomatiskMeldekortSakRepository,
        underveisRepository = InMemoryUnderveisRepository,
        flytJobbRepository = InMemoryFlytJobbRepository,
        clock = fixedClock(idag),
    )
}


