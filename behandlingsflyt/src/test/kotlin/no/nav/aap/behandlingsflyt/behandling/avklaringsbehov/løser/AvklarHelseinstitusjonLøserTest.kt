package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarHelseinstitusjonLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Helseoppholdvurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdene
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingerDto
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
@MockKExtension.RequireParallelTesting
class AvklarHelseinstitusjonLøserTest {

    private val behandlingRepository = mockk<BehandlingRepository>()
    private val helseinstitusjonRepository = mockk<InstitusjonsoppholdRepository>()
    private val unleashGateway = mockk<UnleashGateway>()
    private val løser: AvklarHelseinstitusjonLøser by lazy {
        AvklarHelseinstitusjonLøser(behandlingRepository, helseinstitusjonRepository, unleashGateway)
    }

    @BeforeEach
    fun setup() {
        every { unleashGateway.isEnabled(any()) } returns true
    }

    @Test
    fun `skal lagre ny vurdering når det ikke finnes tidligere vurderinger`() {
        val behandlingId = BehandlingId(1L)
        val vurderingSlot = slot<List<HelseinstitusjonVurdering>>()

        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any(), capture(vurderingSlot)) } returns Unit
        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns null

        val løsning = AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    HelseinstitusjonVurderingDto(
                        begrunnelse = "Ny vurdering",
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = false,
                        periode = Periode(1 mai 2025, 1 august 2025)
                    )
                )
            )
        )

        val resultat = løser.løs(lagKontekst(behandlingId), løsning)

        assertThat(resultat.begrunnelse).isEqualTo("Ny vurdering")
        verify { helseinstitusjonRepository.lagreHelseVurdering(behandlingId, "12345678901", any()) }

        val lagredeVurderinger = vurderingSlot.captured
        assertThat(lagredeVurderinger).hasSize(1)
        assertThat(lagredeVurderinger[0].begrunnelse).isEqualTo("Ny vurdering")
        assertThat(lagredeVurderinger[0].faarFriKostOgLosji).isTrue()
        assertThat(lagredeVurderinger[0].periode).isEqualTo(Periode(1 mai 2025, 1 august 2025))
    }

    @Test
    fun `skal slå sammen nye vurderinger med eksisterende vurderinger fra tidligere behandling`() {
        val forrigeBehandlingId = BehandlingId(1L)
        val nåværendeBehandlingId = BehandlingId(2L)
        val vurderingSlot = slot<List<HelseinstitusjonVurdering>>()

        every { behandlingRepository.hent(nåværendeBehandlingId) } returns
                opprettBehandling(nåværendeBehandlingId, forrigeBehandlingId)

        val eksisterendeGrunnlag = opprettInstitusjonsoppholdGrunnlag(
            listOf(
                HelseinstitusjonVurdering(
                    begrunnelse = "Gammel vurdering",
                    faarFriKostOgLosji = true,
                    forsoergerEktefelle = false,
                    harFasteUtgifter = false,
                    periode = Periode(1 mai 2025, 1 august 2025),
                    vurdertIBehandling = forrigeBehandlingId,
                    vurdertAv = "saksbehandler-1",
                    vurdertTidspunkt = LocalDateTime.of(2025, 5, 1, 12, 0)
                )
            )
        )

        every { helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId) } returns null
        every { helseinstitusjonRepository.hentHvisEksisterer(forrigeBehandlingId) } returns eksisterendeGrunnlag
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any(), capture(vurderingSlot)) } returns Unit

        val løsning = AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    HelseinstitusjonVurderingDto(
                        begrunnelse = "Ny vurdering revurdering",
                        faarFriKostOgLosji = false,
                        forsoergerEktefelle = true,
                        harFasteUtgifter = true,
                        periode = Periode(1 juni 2025, 1 juli 2025)
                    )
                )
            )
        )

        løser.løs(lagKontekst(nåværendeBehandlingId), løsning)

        val lagredeVurderinger = vurderingSlot.captured
        assertThat(lagredeVurderinger).hasSize(3)

        // Assert for første del av gammel vurdering
        val gammelVurdering1 = lagredeVurderinger.find {
            it.begrunnelse == "Gammel vurdering" &&
                    it.periode == Periode(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 31))
        }
        assertThat(gammelVurdering1).isNotNull
        assertThat(gammelVurdering1!!.faarFriKostOgLosji).isTrue()
        assertThat(gammelVurdering1.forsoergerEktefelle).isFalse()
        assertThat(gammelVurdering1.harFasteUtgifter).isFalse()
        assertThat(gammelVurdering1.vurdertIBehandling).isEqualTo(forrigeBehandlingId)

        // Assert for ny vurdering
        val nyVurdering = lagredeVurderinger.find { it.begrunnelse == "Ny vurdering revurdering" }
        assertThat(nyVurdering).isNotNull
        assertThat(nyVurdering!!.periode).isEqualTo(Periode(1 juni 2025, 1 juli 2025))
        assertThat(nyVurdering.faarFriKostOgLosji).isFalse()
        assertThat(nyVurdering.vurdertIBehandling).isEqualTo(nåværendeBehandlingId)

        // Assert for andre del gammel vurdering
        val gammelVurdering2 = lagredeVurderinger.find {
            it.begrunnelse == "Gammel vurdering" &&
                    it.periode == Periode(LocalDate.of(2025, 7, 2), LocalDate.of(2025, 8, 1))
        }
        assertThat(gammelVurdering2).isNotNull
        assertThat(gammelVurdering2!!.faarFriKostOgLosji).isTrue()
        assertThat(gammelVurdering2.forsoergerEktefelle).isFalse()
        assertThat(gammelVurdering2.harFasteUtgifter).isFalse()
        assertThat(gammelVurdering2.vurdertIBehandling).isEqualTo(forrigeBehandlingId)
    }

    @Test
    fun `skal overskrive overlappende perioder med nye vurderinger`() {
        val forrigeBehandlingId = BehandlingId(1L)
        val nåværendeBehandlingId = BehandlingId(2L)
        val vurderingSlot = slot<List<HelseinstitusjonVurdering>>()

        every { behandlingRepository.hent(nåværendeBehandlingId) } returns
                opprettBehandling(nåværendeBehandlingId, forrigeBehandlingId)

        val eksisterendeGrunnlag = opprettInstitusjonsoppholdGrunnlag(
            listOf(
                HelseinstitusjonVurdering(
                    begrunnelse = "Gammel vurdering hele perioden",
                    faarFriKostOgLosji = true,
                    forsoergerEktefelle = false,
                    harFasteUtgifter = false,
                    periode = Periode(1 mai 2025, 1 august 2025),
                    vurdertIBehandling = forrigeBehandlingId,
                    vurdertAv = "saksbehandler-1",
                    vurdertTidspunkt = LocalDateTime.of(2025, 5, 1, 12, 0)
                )
            )
        )

        every { helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId) } returns null
        every { helseinstitusjonRepository.hentHvisEksisterer(forrigeBehandlingId) } returns eksisterendeGrunnlag
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any(), capture(vurderingSlot)) } returns Unit

        // Nye vurderinger som splitter den gamle perioden
        val løsning = AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    HelseinstitusjonVurderingDto(
                        begrunnelse = "Første del - reduksjon",
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = false,
                        periode = Periode(1 mai 2025, 1 juni 2025)
                    ),
                    HelseinstitusjonVurderingDto(
                        begrunnelse = "Andre del - ikke reduksjon",
                        faarFriKostOgLosji = false,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = false,
                        periode = Periode(2 juni 2025, 1 august 2025)
                    )
                )
            )
        )

        løser.løs(lagKontekst(nåværendeBehandlingId), løsning)

        val lagredeVurderinger = vurderingSlot.captured

        // Skal kun inneholde de to nye vurderingene, ikke den gamle som overlapper
        assertThat(lagredeVurderinger).hasSize(2)
        assertThat(lagredeVurderinger.map { it.begrunnelse }).containsExactlyInAnyOrder(
            "Første del - reduksjon",
            "Andre del - ikke reduksjon"
        )
        assertThat(lagredeVurderinger.all { it.vurdertIBehandling == nåværendeBehandlingId }).isTrue()
    }

    @Test
    fun `skal beholde gamle vurderinger som ikke overlapper med nye`() {
        val forrigeBehandlingId = BehandlingId(1L)
        val nåværendeBehandlingId = BehandlingId(2L)
        val vurderingSlot = slot<List<HelseinstitusjonVurdering>>()

        every { behandlingRepository.hent(nåværendeBehandlingId) } returns
                opprettBehandling(nåværendeBehandlingId, forrigeBehandlingId)

        val eksisterendeGrunnlag = opprettInstitusjonsoppholdGrunnlag(
            listOf(
                HelseinstitusjonVurdering(
                    begrunnelse = "Vurdering opphold 1",
                    faarFriKostOgLosji = true,
                    forsoergerEktefelle = false,
                    harFasteUtgifter = false,
                    periode = Periode(1 mai 2025, 1 juni 2025),
                    vurdertIBehandling = forrigeBehandlingId,
                    vurdertAv = "saksbehandler-1",
                    vurdertTidspunkt = LocalDateTime.of(2025, 5, 1, 12, 0)
                ),
                HelseinstitusjonVurdering(
                    begrunnelse = "Vurdering opphold 2",
                    faarFriKostOgLosji = false,
                    forsoergerEktefelle = true,
                    harFasteUtgifter = true,
                    periode = Periode(1 august 2025, 1 desember 2025),
                    vurdertIBehandling = forrigeBehandlingId,
                    vurdertAv = "saksbehandler-1",
                    vurdertTidspunkt = LocalDateTime.of(2025, 8, 1, 12, 0)
                )
            )
        )

        every { helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId) } returns null
        every { helseinstitusjonRepository.hentHvisEksisterer(forrigeBehandlingId) } returns eksisterendeGrunnlag
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any(), capture(vurderingSlot)) } returns Unit

        // Ny vurdering som bare overlapper med første periode
        val løsning = AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    HelseinstitusjonVurderingDto(
                        begrunnelse = "Ny vurdering opphold 1",
                        faarFriKostOgLosji = false,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = true,
                        periode = Periode(1 mai 2025, 1 juni 2025)
                    )
                )
            )
        )

        løser.løs(lagKontekst(nåværendeBehandlingId), løsning)

        val lagredeVurderinger = vurderingSlot.captured

        // Skal inneholde ny vurdering + den gamle som ikke overlapper
        assertThat(lagredeVurderinger).hasSize(2)
        assertThat(lagredeVurderinger.map { it.begrunnelse }).containsExactlyInAnyOrder(
            "Ny vurdering opphold 1",
            "Vurdering opphold 2"
        )

        // Verifiser at opphold 2 vurdering er bevart fra forrige behandling
        val opphold2Vurdering = lagredeVurderinger.find { it.begrunnelse == "Vurdering opphold 2" }
        assertThat(opphold2Vurdering!!.vurdertIBehandling).isEqualTo(forrigeBehandlingId)
    }

    @Test
    fun `skal håndtere flere vurderinger med forskjellige perioder`() {
        val behandlingId = BehandlingId(1L)
        val vurderingSlot = slot<List<HelseinstitusjonVurdering>>()
        val førsteMai = 1 mai 2025
        val andreJuni = 2 juni 2025
        val andreJuli = 2 juli 2025

        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns null
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any(), capture(vurderingSlot)) } returns Unit

        val løsning = AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    HelseinstitusjonVurderingDto(
                        begrunnelse = "Periode 1 - reduksjon",
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = false,
                        periode = Periode(førsteMai, 1 juni 2025)
                    ),
                    HelseinstitusjonVurderingDto(
                        begrunnelse = "Periode 2 - ikke reduksjon",
                        faarFriKostOgLosji = false,
                        forsoergerEktefelle = true,
                        harFasteUtgifter = true,
                        periode = Periode(andreJuni, 1 juli 2025)
                    ),
                    HelseinstitusjonVurderingDto(
                        begrunnelse = "Periode 3 - reduksjon igjen",
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = false,
                        periode = Periode(andreJuli, 1 august 2025)
                    )
                )
            )
        )

        val resultat = løser.løs(lagKontekst(behandlingId), løsning)

        assertThat(resultat.begrunnelse).contains("Periode 1", "Periode 2", "Periode 3")

        val lagredeVurderinger = vurderingSlot.captured
        assertThat(lagredeVurderinger).hasSize(3)
        assertThat(lagredeVurderinger[0].periode.fom).isEqualTo(førsteMai)
        assertThat(lagredeVurderinger[1].periode.fom).isEqualTo(andreJuni)
        assertThat(lagredeVurderinger[2].periode.fom).isEqualTo(andreJuli)
    }

    @Test
    fun `skal håndtere vurdering med alle felter satt til null`() {
        val behandlingId = BehandlingId(1L)
        val vurderingSlot = slot<List<HelseinstitusjonVurdering>>()

        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns null
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any(), capture(vurderingSlot)) } returns Unit

        val løsning = AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    HelseinstitusjonVurderingDto(
                        begrunnelse = "Vurdering med null-verdier",
                        faarFriKostOgLosji = false,
                        forsoergerEktefelle = null,
                        harFasteUtgifter = null,
                        periode = Periode(1 mai 2025, 1 august 2025)
                    )
                )
            )
        )

        løser.løs(lagKontekst(behandlingId), løsning)

        val lagredeVurderinger = vurderingSlot.captured
        assertThat(lagredeVurderinger).hasSize(1)
        assertThat(lagredeVurderinger[0].forsoergerEktefelle).isNull()
        assertThat(lagredeVurderinger[0].harFasteUtgifter).isNull()
        assertThat(lagredeVurderinger[0].faarFriKostOgLosji).isFalse()
    }

    @Test
    fun `skal kunne oppdatere kun en del av eksisterende vurdering`() {
        val forrigeBehandlingId = BehandlingId(1L)
        val nåværendeBehandlingId = BehandlingId(2L)
        val vurderingSlot = slot<List<HelseinstitusjonVurdering>>()

        every { behandlingRepository.hent(nåværendeBehandlingId) } returns
                opprettBehandling(nåværendeBehandlingId, forrigeBehandlingId)

        val eksisterendeGrunnlag = opprettInstitusjonsoppholdGrunnlag(
            listOf(
                HelseinstitusjonVurdering(
                    begrunnelse = "Gammel vurdering",
                    faarFriKostOgLosji = true,
                    forsoergerEktefelle = false,
                    harFasteUtgifter = false,
                    periode = Periode(1 mai 2025, 1 august 2025),
                    vurdertIBehandling = forrigeBehandlingId,
                    vurdertAv = "saksbehandler-1",
                    vurdertTidspunkt = LocalDateTime.of(2025, 5, 1, 12, 0)
                )
            )
        )

        every { helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId) } returns null
        every { helseinstitusjonRepository.hentHvisEksisterer(forrigeBehandlingId) } returns eksisterendeGrunnlag
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any(), capture(vurderingSlot)) } returns Unit

        // Oppdater kun en liten del av perioden
        val løsning = AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    HelseinstitusjonVurderingDto(
                        begrunnelse = "Oppdatering av liten del",
                        faarFriKostOgLosji = false,
                        forsoergerEktefelle = true,
                        harFasteUtgifter = true,
                        periode = Periode(1 juni 2025, 15 juni 2025)
                    )
                )
            )
        )

        løser.løs(lagKontekst(nåværendeBehandlingId), løsning)

        val lagredeVurderinger = vurderingSlot.captured

        // Den nye vurderingen skal være lagret
        assertThat(lagredeVurderinger.any {
            it.begrunnelse == "Oppdatering av liten del" &&
                    it.vurdertIBehandling == nåværendeBehandlingId
        }).isTrue()
    }

    @Test
    fun `skal returnere korrekt begrunnelse sammensatt av alle vurderinger`() {
        val behandlingId = BehandlingId(1L)

        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns null
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any(), any()) } returns Unit

        val løsning = AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    HelseinstitusjonVurderingDto(
                        begrunnelse = "Første begrunnelse",
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = false,
                        periode = Periode(1 mai 2025, 1 juni 2025)
                    ),
                    HelseinstitusjonVurderingDto(
                        begrunnelse = "Andre begrunnelse",
                        faarFriKostOgLosji = false,
                        forsoergerEktefelle = true,
                        harFasteUtgifter = true,
                        periode = Periode(2 juni 2025, 1 juli 2025)
                    )
                )
            )
        )

        val resultat = løser.løs(lagKontekst(behandlingId), løsning)

        assertThat(resultat.begrunnelse).isEqualTo("Første begrunnelse Andre begrunnelse")
    }

    @Test
    fun `skal kaste exception hvis første reduksjonsvurdering starter for tidlig`() {
        val behandlingId = BehandlingId(1L)
        val opphold = opprettInstitusjonsoppholdGrunnlag(emptyList())
        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns opphold
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)

        val vurdering = HelseinstitusjonVurderingDto(
            begrunnelse = "For tidlig reduksjon",
            faarFriKostOgLosji = true,
            forsoergerEktefelle = false,
            harFasteUtgifter = false,
            periode = Periode(1 februar 2025, 1 juni 2025) // Starter for tidlig
        )

        val løsning = AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(listOf(vurdering))
        )

        val exception = assertThrows<UgyldigForespørselException> {
            AvklarHelseinstitusjonLøser(behandlingRepository, helseinstitusjonRepository, unleashGateway)
                .løs(lagKontekst(behandlingId), løsning)
        }
        assertThat(exception.message).contains("Første reduksjonsvurdering starter for tidlig.")
    }

    @Test
    fun `skal kaste exception hvis reduksjon ved nytt opphold starter for tidlig`() {
        val behandlingId = BehandlingId(1L)
        val opphold = opprettToInstitusjonsoppholdGrunnlag(emptyList())
        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns opphold
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)

        val vurderinger = listOf(
            HelseinstitusjonVurderingDto(
                begrunnelse = "Reduksjon for Mungos Hospital",
                faarFriKostOgLosji = true,
                forsoergerEktefelle = false,
                harFasteUtgifter = false,
                periode = Periode(1 mai 2025, 1 august 2025)
            ),
            HelseinstitusjonVurderingDto(
                begrunnelse = "Reduksjon for Helgelandssykehus Dialyse, for tidlig",
                faarFriKostOgLosji = true,
                forsoergerEktefelle = false,
                harFasteUtgifter = false,
                periode = Periode(1 februar 2026, 1 juni 2026)
            )
        )
        val løsning = AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(vurderinger)
        )

        val exception = assertThrows<UgyldigForespørselException> {
            AvklarHelseinstitusjonLøser(behandlingRepository, helseinstitusjonRepository, unleashGateway)
                .løs(lagKontekst(behandlingId), løsning)
        }
        assertThat(exception.message).contains("Reduksjon ved nytt opphold starter for tidlig")
    }

    private fun lagKontekst(behandlingId: BehandlingId): AvklaringsbehovKontekst {
        return AvklaringsbehovKontekst(
            bruker = Bruker("12345678901"),
            kontekst = FlytKontekst(
                sakId = SakId(1L),
                behandlingId = behandlingId,
                forrigeBehandlingId = null,
                behandlingType = TypeBehandling.Førstegangsbehandling
            )
        )
    }

    private fun opprettBehandling(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId?
    ): Behandling {
        return Behandling(
            behandlingId,
            forrigeBehandlingId,
            BehandlingReferanse(UUID.randomUUID()),
            SakId(1L),
            TypeBehandling.Førstegangsbehandling,
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            versjon = 1L
        )
        /*return mockk {
            every { id } returns behandlingId
            every { this@mockk.forrigeBehandlingId } returns forrigeBehandlingId
        }*/
    }

    private fun opprettInstitusjonsoppholdGrunnlag(
        vurderinger: List<HelseinstitusjonVurdering>
    ): InstitusjonsoppholdGrunnlag {
        return InstitusjonsoppholdGrunnlag(
            oppholdene = Oppholdene(
                id = 1L,
                opphold = listOf(
                    Segment(
                        Periode(1 januar 2025, 31 desember 2025),
                        Institusjon(
                            navn = "Test Helseinstitusjon",
                            orgnr = "987654321",
                            type = Institusjonstype.HS,
                            kategori = Oppholdstype.H
                        )
                    )
                )
            ),
            helseoppholdvurderinger = Helseoppholdvurderinger(
                id = 1L,
                vurderinger = vurderinger,
                vurdertAv = "testSaksbehandler",
                vurdertTidspunkt = LocalDateTime.of(2025, 5, 1, 12, 0)
            ),
            soningsVurderinger = null
        )
    }

    private fun opprettToInstitusjonsoppholdGrunnlag(
        vurderinger: List<HelseinstitusjonVurdering>
    ): InstitusjonsoppholdGrunnlag {
        return InstitusjonsoppholdGrunnlag(
            oppholdene = Oppholdene(
                id = 1L,
                opphold = listOf(
                    Segment(
                        Periode(1 januar 2025, 1 august 2025),
                        Institusjon(
                            navn = "St. Mungos Hospital",
                            orgnr = "987654321",
                            type = Institusjonstype.HS,
                            kategori = Oppholdstype.H
                        )
                    ),
                    Segment(
                        Periode(1 desember 2025, 1 juli 2026),
                        Institusjon(
                            navn = "Helgelandssykehus Dialyse, Sandnessjøen",
                            orgnr = "123456789",
                            type = Institusjonstype.HS,
                            kategori = Oppholdstype.D
                        )
                    )
                )
            ),
            helseoppholdvurderinger = Helseoppholdvurderinger(
                id = 1L,
                vurderinger = vurderinger,
                vurdertAv = "testSaksbehandler",
                vurdertTidspunkt = LocalDateTime.of(2025, 5, 1, 12, 0)
            ),
            soningsVurderinger = null
        )
    }
}