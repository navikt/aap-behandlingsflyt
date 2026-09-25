package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.GjenstaaendeKvote
import no.nav.aap.arenaoppslag.kontrakt.migrering.KravResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArenaKontraktMappingTest {

    private val kravResponse = KravResponse(
        lopenr = 123456,
        aar = 2016,
        soknadsdato = LocalDate.of(2025, 1, 15),
        migreringsdato = LocalDate.of(2025, 12, 1),
        gjenstaaendeKvote = GjenstaaendeKvote(ordinaer = 150),
    )

    @Test
    fun `KravResponse mappes til ArenaKrav`() {
        assertThat(kravResponse.tilDomene()).isEqualTo(
            ArenaKrav(
                arenaSaksnummer = "2016-123456",
                søknadsdato = LocalDate.of(2025, 1, 15),
                migreringsdato = LocalDate.of(2025, 12, 1),
                gjenståendeKvoteOrdinær = 150,
            )
        )
    }

    @Test
    fun `KravResponse uten ordinær kvote mappes til null`() {
        val krav = kravResponse.copy(gjenstaaendeKvote = GjenstaaendeKvote(ordinaer = null)).tilDomene()

        assertThat(krav.gjenståendeKvoteOrdinær).isNull()
    }

    @Test
    fun `KravResponse uten søknadsdato feiler`() {
        assertThatThrownBy { kravResponse.copy(soknadsdato = null).tilDomene() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("søknadsdato")
    }

    @Test
    fun `KravResponse uten migreringsdato feiler`() {
        assertThatThrownBy { kravResponse.copy(migreringsdato = null).tilDomene() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("migreringsdato")
    }

    @Test
    fun `ArenaSakOppsummeringKontrakt mappes til ArenaSak`() {
        val sak = ArenaSakOppsummeringKontrakt(
            sakId = "123",
            lopenummer = 5,
            aar = 2023,
            antallVedtak = 1,
            statuskode = "AKTIV",
            statusnavn = "Aktiv",
            sakstype = "AAP",
            regDato = LocalDate.of(2023, 1, 1),
            avsluttetDato = null,
        )

        assertThat(sak.tilDomene()).isEqualTo(ArenaSak(saksnummer = "2023-5", statuskode = "AKTIV"))
    }

    @Test
    fun `HarHistorikkResponse mappes til ArenaHistorikk`() {
        assertThat(HarHistorikkResponse(true).tilDomene()).isEqualTo(ArenaHistorikk(harHistorikk = true))
        assertThat(HarHistorikkResponse(false).tilDomene()).isEqualTo(ArenaHistorikk(harHistorikk = false))
    }
}
