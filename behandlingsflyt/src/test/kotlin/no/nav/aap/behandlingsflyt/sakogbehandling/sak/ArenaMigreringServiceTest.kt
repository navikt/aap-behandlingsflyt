package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakMedVedtakResponse
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakPerson
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryArenaMigreringRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class ArenaMigreringServiceTest {
    private val apiInternGateway: ApiInternGateway = mockk()

    @BeforeEach
    fun setup() {
        InMemoryArenaMigreringRepository.reset()
    }

    @Test
    fun `henter arenasak med vedtak og lagrer den på migreringen`() {
        val sakId = SakId(1)
        val saksnummerArena = "2018-123456"
        InMemoryArenaMigreringRepository.lagre(
            ArenaMigrering(
                sakId = sakId,
                saksnummerArena = saksnummerArena,
                ident = "12345678910",
                migrertTidspunkt = LocalDateTime.now(),
            )
        )
        val respons = arenaSakMedVedtak(saksnummerArena)
        every { apiInternGateway.hentArenaSakMedVedtak(saksnummerArena) } returns respons

        val resultat = ArenaMigreringService(apiInternGateway, InMemoryArenaMigreringRepository)
            .hentOgLagreArenaSakMedVedtak(sakId, saksnummerArena)

        assertThat(resultat).isEqualTo(respons)
        assertThat(InMemoryArenaMigreringRepository.hentForSakHvisEksisterer(sakId)?.arenaSakData)
            .isEqualTo(respons)
        verify(exactly = 1) { apiInternGateway.hentArenaSakMedVedtak(saksnummerArena) }
    }

    @Test
    fun `feiler når saken ikke er migrert fra arena`() {
        val sakId = SakId(2)
        val saksnummerArena = "2018-123456"
        every { apiInternGateway.hentArenaSakMedVedtak(saksnummerArena) } returns
                arenaSakMedVedtak(saksnummerArena)

        assertThrows<IllegalArgumentException> {
            ArenaMigreringService(apiInternGateway, InMemoryArenaMigreringRepository)
                .hentOgLagreArenaSakMedVedtak(sakId, saksnummerArena)
        }
    }

    private fun arenaSakMedVedtak(saksnummerArena: String) = ArenaSakMedVedtakResponse(
        sakId = saksnummerArena,
        opprettetAar = 2018,
        lopenr = 123456,
        person = ArenaSakPerson(
            personId = 1,
            fodselsnummer = "12345678910",
            fornavn = "Test",
            etternavn = "Testesen",
        ),
        statuskode = "AKTIV",
        statusnavn = "Aktiv",
        registrertDato = LocalDateTime.of(2018, 1, 1, 10, 0, 0),
        avsluttetDato = null,
        vedtak = listOf(),
    )
}
