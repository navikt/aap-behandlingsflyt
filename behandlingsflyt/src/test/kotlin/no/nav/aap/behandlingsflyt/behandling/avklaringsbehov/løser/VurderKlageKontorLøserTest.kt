package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageKontorLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagebehandlingKontorRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontorLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class VurderKlageKontorLøserTest {
    private val klagebehandlingKontorRepositoryMock = mockk<KlagebehandlingKontorRepository>()
    private val påklagetBehandlingRepositoryMock = mockk<PåklagetBehandlingRepository>()

    @AfterEach
    fun afterEach() {
        checkUnnecessaryStub(klagebehandlingKontorRepositoryMock, påklagetBehandlingRepositoryMock)
    }

    @Test
    fun `løs skal returnere et løsningsresultat når vilkårSomOmgjøres inneholder gyldige hjemler`() {
        every { påklagetBehandlingRepositoryMock.hentPåklagetVedtakstype(any()) } returns PåklagetVedtakType.KELVIN_BEHANDLING
        every { klagebehandlingKontorRepositoryMock.lagre(any(), any()) } returns Unit

        val vurderKlageKontorLøser = lagLøser()

        val løsning = vurderKlageKontorLøser.løs(
            kontekst = lagAvklaringsbehovKontekst(), løsning = VurderKlageKontorLøsning(
                klagevurderingKontor = KlagevurderingKontorLøsningDto(
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
        every { påklagetBehandlingRepositoryMock.hentPåklagetVedtakstype(any()) } returns PåklagetVedtakType.KELVIN_BEHANDLING
        every { klagebehandlingKontorRepositoryMock.lagre(any(), any()) } returns Unit

        val vurderKlageKontorLøser = lagLøser()

        val løsning = VurderKlageKontorLøsning(
            klagevurderingKontor = KlagevurderingKontorLøsningDto(
                begrunnelse = "Begrunnelse for klage",
                notat = "Notat",
                innstilling = KlageInnstilling.OMGJØR,
                vilkårSomOpprettholdes = emptyList(),
                vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_31)
            )
        )

        val exception = assertThrows<UgyldigForespørselException> {
            vurderKlageKontorLøser.løs(
                kontekst = lagAvklaringsbehovKontekst(), løsning = løsning
            )
        }

        assertThat(exception.message).contains("FOLKETRYGDLOVEN_11_3")
    }

    @ParameterizedTest
    @EnumSource(
        KlageInnstilling::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["OMGJØR", "DELVIS_OMGJØR"]
    )
    fun `løs skal avvise omgjøring av tilbakekreving før vurderingen lagres`(innstilling: KlageInnstilling) {
        every { påklagetBehandlingRepositoryMock.hentPåklagetVedtakstype(any()) } returns PåklagetVedtakType.TILBAKEKREVING

        val exception = assertThrows<UgyldigForespørselException> {
            lagLøser().løs(
                kontekst = lagAvklaringsbehovKontekst(),
                løsning = lagLøsning(innstilling)
            )
        }

        assertThat(exception.message)
            .isEqualTo("Omgjøring støttes ikke for tilbakekreving. Opprett manuell sak i Porten.")
        verify(exactly = 0) { klagebehandlingKontorRepositoryMock.lagre(any(), any()) }
    }

    @Test
    fun `løs skal tillate opprettholdelse av tilbakekreving`() {
        every { påklagetBehandlingRepositoryMock.hentPåklagetVedtakstype(any()) } returns PåklagetVedtakType.TILBAKEKREVING
        every { klagebehandlingKontorRepositoryMock.lagre(any(), any()) } returns Unit

        val resultat = lagLøser().løs(
            kontekst = lagAvklaringsbehovKontekst(),
            løsning = lagLøsning(KlageInnstilling.OPPRETTHOLD)
        )

        assertThat(resultat.begrunnelse).isEqualTo("Begrunnelse for klage")
    }

    private fun lagLøser() = VurderKlageKontorLøser(
        klagebehandlingKontorRepositoryMock,
        påklagetBehandlingRepositoryMock
    )

    private fun lagLøsning(innstilling: KlageInnstilling) = VurderKlageKontorLøsning(
        klagevurderingKontor = KlagevurderingKontorLøsningDto(
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

    private fun lagAvklaringsbehovKontekst(): AvklaringsbehovKontekst = AvklaringsbehovKontekst(
        bruker = Bruker("12345678901"), kontekst = FlytKontekst(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Klage
        )
    )
}