package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageNayLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNayLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.help.avklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class VurderKlageNayLøserTest {
    private val klagebehandlingNayRepositoryMock = mockk<KlagebehandlingNayRepository>()
    private val påklagetBehandlingRepositoryMock = mockk<PåklagetBehandlingRepository>()

    @AfterEach
    fun afterEach() {
        checkUnnecessaryStub(klagebehandlingNayRepositoryMock, påklagetBehandlingRepositoryMock)
    }

    @Test
    fun `løs skal returnere et løsningsresultat når vilkårSomOmgjøres inneholder gyldige hjemler`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        every { påklagetBehandlingRepositoryMock.hentPåklagetVedtakstype(any()) } returns PåklagetVedtakType.KELVIN_BEHANDLING
        every { klagebehandlingNayRepositoryMock.lagre(any(), any()) } returns Unit

        val vurderKlageNayLøser = lagLøser()

        val løsning = vurderKlageNayLøser.løs(
            kontekst = avklaringsbehovKontekst { this.behandling = behandling },
            løsning = VurderKlageNayLøsning(
                klagevurderingNay = KlagevurderingNayLøsningDto(
                    begrunnelse = "Begrunnelse for klage",
                    notat = "Notat",
                    innstilling = KlageInnstilling.OMGJØR,
                    vilkårSomOpprettholdes = emptyList(),
                    vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
                )
            )
        )

        assertThat(løsning.begrunnelse).isEqualTo("Begrunnelse for klage")
    }

    @Test
    fun `løs skal kaste exception når vilkårSomOmgjøres inneholder hjemler som ikke kan mappes via tilÅrsak`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        every { påklagetBehandlingRepositoryMock.hentPåklagetVedtakstype(any()) } returns PåklagetVedtakType.KELVIN_BEHANDLING
        every { klagebehandlingNayRepositoryMock.lagre(any(), any()) } returns Unit

        val vurderKlageNayLøser = lagLøser()

        val løsning = VurderKlageNayLøsning(
            klagevurderingNay = KlagevurderingNayLøsningDto(
                begrunnelse = "Begrunnelse for klage",
                notat = "Notat",
                innstilling = KlageInnstilling.OMGJØR,
                vilkårSomOpprettholdes = emptyList(),
                vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_31)
            )
        )

        val exception = assertThrows<UgyldigForespørselException> {
            vurderKlageNayLøser.løs(
                kontekst = avklaringsbehovKontekst { this.behandling = behandling },
                løsning = løsning
            )
        }

        assertThat(exception.message).contains("§ 11-31")
    }

    @ParameterizedTest
    @EnumSource(
        KlageInnstilling::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["OMGJØR", "DELVIS_OMGJØR"]
    )
    fun `løs skal avvise omgjøring av tilbakekreving før vurderingen lagres`(innstilling: KlageInnstilling) {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        every { påklagetBehandlingRepositoryMock.hentPåklagetVedtakstype(any()) } returns PåklagetVedtakType.TILBAKEKREVING

        val exception = assertThrows<UgyldigForespørselException> {
            lagLøser().løs(
                kontekst = avklaringsbehovKontekst { this.behandling = behandling },
                løsning = lagLøsning(innstilling)
            )
        }

        assertThat(exception.message)
            .isEqualTo("Omgjøring støttes ikke for tilbakekreving. Opprett manuell sak i Porten.")
        verify(exactly = 0) { klagebehandlingNayRepositoryMock.lagre(any(), any()) }
    }

    @Test
    fun `løs skal tillate opprettholdelse av tilbakekreving`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        every { påklagetBehandlingRepositoryMock.hentPåklagetVedtakstype(any()) } returns PåklagetVedtakType.TILBAKEKREVING
        every { klagebehandlingNayRepositoryMock.lagre(any(), any()) } returns Unit

        val resultat = lagLøser().løs(
            kontekst = avklaringsbehovKontekst { this.behandling = behandling },
            løsning = lagLøsning(KlageInnstilling.OPPRETTHOLD)
        )

        assertThat(resultat.begrunnelse).isEqualTo("Begrunnelse for klage")
    }

    private fun lagLøser() = VurderKlageNayLøser(
        klagebehandlingNayRepositoryMock,
        påklagetBehandlingRepositoryMock
    )

    private fun lagLøsning(innstilling: KlageInnstilling) = VurderKlageNayLøsning(
        klagevurderingNay = KlagevurderingNayLøsningDto(
            begrunnelse = "Begrunnelse for klage",
            notat = "Notat",
            innstilling = innstilling,
            vilkårSomOpprettholdes = when (innstilling) {
                KlageInnstilling.OMGJØR -> emptyList()
                KlageInnstilling.OPPRETTHOLD, KlageInnstilling.DELVIS_OMGJØR ->
                    listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
            },
            vilkårSomOmgjøres = when (innstilling) {
                KlageInnstilling.OPPRETTHOLD -> emptyList()
                KlageInnstilling.OMGJØR, KlageInnstilling.DELVIS_OMGJØR ->
                    listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
            }
        )
    )
}