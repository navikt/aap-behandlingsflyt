package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.GjenstaaendeKvote
import no.nav.aap.arenaoppslag.kontrakt.migrering.KravResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaForutgaaendeMedlemskapResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaOppholdskravResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVedtaksperiodeResponse
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
    fun `ArenaVedtaksperiodeResponse mappes til vedtaksperiode`() {
        assertThat(ArenaVedtaksperiodeResponse(7, LocalDate.of(2024, 1, 1), null).tilDomene())
            .isEqualTo(ArenaVedtaksperiode(7, LocalDate.of(2024, 1, 1), null))
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
                vilkår = no.nav.aap.behandlingsflyt.arena.ArenaVilkår(
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
        assertThat(ArenaOppholdskravResponse(vedtakId = 7, vilkar = null).tilDomene().vilkår).isNull()
    }

    @Test
    fun `ArenaForutgaaendeMedlemskapResponse mappes til forutgående medlemskap`() {
        val respons = ArenaForutgaaendeMedlemskapResponse(
            vedtakId = 7,
            vilkar = ArenaVilkar(id = 8, kode = "MEDLSKAP5", status = "J", begrunnelse = "fritekst"),
        )

        assertThat(respons.tilDomene()).isEqualTo(
            ArenaForutgåendeMedlemskap(
                vedtakId = 7,
                vilkår = no.nav.aap.behandlingsflyt.arena.ArenaVilkår(
                    id = 8,
                    kode = "MEDLSKAP5",
                    status = "J",
                    begrunnelse = "fritekst",
                ),
            )
        )
    }

    @Test
    fun `ArenaForutgaaendeMedlemskapResponse uten vilkår beholder null`() {
        assertThat(ArenaForutgaaendeMedlemskapResponse(vedtakId = 7, vilkar = null).tilDomene().vilkår).isNull()
    }
}
