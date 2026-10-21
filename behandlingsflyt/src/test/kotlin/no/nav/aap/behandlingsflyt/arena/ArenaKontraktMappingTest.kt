package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.GjenstaaendeKvote
import no.nav.aap.arenaoppslag.kontrakt.migrering.KravResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaOppholdskravResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVilkar
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
        sisteFerdigBehandledeMeldekortperiode = Periode(LocalDate.of(2025, 11, 17), LocalDate.of(2025, 11, 30)),
    )

    @Test
    fun `KravResponse mappes til ArenaKrav`() {
        assertThat(kravResponse.tilDomene()).isEqualTo(
            ArenaKrav(
                arenaSaksnummer = "2016-123456",
                søknadsdato = LocalDate.of(2025, 1, 15),
                migreringsdato = LocalDate.of(2025, 12, 1),
                gjenståendeKvoteOrdinær = 150,
                sisteFerdigBehandledeMeldekortTilDato = LocalDate.of(2025, 11, 30),
            )
        )
    }

    @Test
    fun `KravResponse uten ordinær kvote mappes til null`() {
        val krav = kravResponse.copy(gjenstaaendeKvote = GjenstaaendeKvote(ordinaer = null)).tilDomene()

        assertThat(krav.gjenståendeKvoteOrdinær).isNull()
    }

    @Test
    fun `KravResponse uten siste meldekortperiode mappes til null`() {
        val krav = kravResponse.copy(sisteFerdigBehandledeMeldekortperiode = null).tilDomene()

        assertThat(krav.sisteFerdigBehandledeMeldekortTilDato).isNull()
    }

    @Test
    fun `KravResponse med siste meldekortperiode uten tilOgMedDato mappes til null`() {
        val periode = Periode(LocalDate.of(2025, 11, 17), null)
        val krav = kravResponse.copy(sisteFerdigBehandledeMeldekortperiode = periode).tilDomene()

        assertThat(krav.sisteFerdigBehandledeMeldekortTilDato).isNull()
    }

    @Test
    fun `KravResponse uten søknadsdato feiler`() {
        assertThatThrownBy { kravResponse.copy(soknadsdato = null).tilDomene() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("søknadsdato")
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

    @Test
    fun `ArenaOppholdskravResponse mappes til oppholdskrav`() {
        val respons = ArenaOppholdskravResponse(
            vedtakId = 7,
            vilkar = ArenaVilkar(
                id = 8,
                kode = "OPPHOLD",
                status = "V",
                begrunnelse = "Skal ikke brukes i vurderingen",
            ),
        )

        assertThat(respons.tilDomene()).isEqualTo(
            ArenaOppholdskrav(
                vedtakId = 7,
                vilkar = no.nav.aap.behandlingsflyt.arena.ArenaVilkar(
                    id = 8,
                    kode = "OPPHOLD",
                    status = "V",
                    begrunnelse = "Skal ikke brukes i vurderingen",
                ),
            )
        )
    }

    @Test
    fun `ArenaOppholdskravResponse uten vilkår beholder null`() {
        assertThat(ArenaOppholdskravResponse(vedtakId = 7, vilkar = null).tilDomene().vilkar).isNull()
    }
}
