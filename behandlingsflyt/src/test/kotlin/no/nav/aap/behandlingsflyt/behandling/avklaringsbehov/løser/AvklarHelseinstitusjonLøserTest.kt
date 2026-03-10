package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.checkUnnecessaryStub
import io.mockk.every
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
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

class AvklarHelseinstitusjonLøserTest {

    private val behandlingRepository = mockk<BehandlingRepository>()
    private val helseinstitusjonRepository = mockk<InstitusjonsoppholdRepository>()
    private val løser: AvklarHelseinstitusjonLøser by lazy {
        AvklarHelseinstitusjonLøser(behandlingRepository, helseinstitusjonRepository)
    }

    @AfterEach
    fun afterEach() {
        checkUnnecessaryStub(behandlingRepository, helseinstitusjonRepository)
    }

    @Test
    fun `forBehov returnerer AVKLAR_HELSEINSTITUSJON`() {
        assertThat(løser.forBehov()).isEqualTo(Definisjon.AVKLAR_HELSEINSTITUSJON)
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
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), capture(vurderingSlot)) } returns Unit

        val løsning = AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Periode 1 - reduksjon",
                        periode = Periode(førsteMai, 1 juni 2025)
                    ),
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Periode 2 - ikke reduksjon",
                        faarFriKostOgLosji = false,
                        forsoergerEktefelle = true,
                        harFasteUtgifter = true,
                        periode = Periode(andreJuni, 1 juli 2025)
                    ),
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Periode 3 - reduksjon igjen",
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
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), capture(vurderingSlot)) } returns Unit

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
                lagHelseinstitusjonVurdering(
                    begrunnelse = "Gammel vurdering",
                    periode = Periode(1 mai 2025, 1 juni 2025),
                    behandlingId = forrigeBehandlingId
                )
            )
        )

        every { helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId) } returns null
        every { helseinstitusjonRepository.hentHvisEksisterer(forrigeBehandlingId) } returns eksisterendeGrunnlag
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), capture(vurderingSlot)) } returns Unit

        // Oppdater kun en liten del av perioden
        val løsning = AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Oppdatering av liten del",
                        periode = Periode(1 juni 2025, 15 juni 2025)
                    )
                )
            )
        )

        løser.løs(lagKontekst(nåværendeBehandlingId), løsning)

        // Den nye vurderingen skal være lagret
        val lagredeVurderinger = vurderingSlot.captured
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
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any()) } returns Unit

        val løsning = AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Første begrunnelse",
                        periode = Periode(1 mai 2025, 1 juni 2025)
                    ),
                    lagHelseinstitusjonVurderingDto(
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

    // -------------------------------------------------------------------------
    // Ingen tidligere vurderinger
    // -------------------------------------------------------------------------

    @Test
    fun `skal lagre ny vurdering når det ikke finnes tidligere vurderinger`() {
        val behandlingId = BehandlingId(1L)
        val vurderingSlot = slot<List<HelseinstitusjonVurdering>>()

        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), capture(vurderingSlot)) } returns Unit
        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns null

        val løsning = lagLøsning(
            lagHelseinstitusjonVurderingDto(
                begrunnelse = "Ny vurdering",
                periode = Periode(1 mai 2025, 1 august 2025)
            )
        )

        val resultat = løser.løs(lagKontekst(behandlingId), løsning)

        assertThat(resultat.begrunnelse).isEqualTo("Ny vurdering")
        verify { helseinstitusjonRepository.lagreHelseVurdering(behandlingId, any()) }

        val lagredeVurderinger = vurderingSlot.captured
        assertThat(lagredeVurderinger).hasSize(1)
        assertThat(lagredeVurderinger[0].begrunnelse).isEqualTo("Ny vurdering")
        assertThat(lagredeVurderinger[0].faarFriKostOgLosji).isTrue()
        assertThat(lagredeVurderinger[0].periode).isEqualTo(Periode(1 mai 2025, 1 august 2025))
    }

    @Test
    fun `skal bruke eksisterende vurderinger på nåværende behandling når de finnes`() {
        val forrigeBehandlingId = BehandlingId(1L)
        val nåværendeBehandlingId = BehandlingId(2L)
        val vurderingSlot = slot<List<HelseinstitusjonVurdering>>()

        val eksisterendeGrunnlag = opprettGrunnlag(
            listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "Eksisterende vurdering",
                    periode = Periode(1 januar 2025, 31 desember 2025),
                    behandlingId = nåværendeBehandlingId
                )
            )
        )

        every { behandlingRepository.hent(nåværendeBehandlingId) } returns opprettBehandling(
            nåværendeBehandlingId,
            forrigeBehandlingId
        )
        every { helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId) } returns null
        every { helseinstitusjonRepository.hentHvisEksisterer(forrigeBehandlingId) } returns eksisterendeGrunnlag
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), capture(vurderingSlot)) } returns Unit

        val løsning = lagLøsning(
            lagHelseinstitusjonVurderingDto(
                begrunnelse = "Ny vurdering",
                periode = Periode(1 mai 2025, 1 august 2025)
            )
        )

        løser.løs(lagKontekst(nåværendeBehandlingId), løsning)

        val lagredeVurderinger = vurderingSlot.captured
        val nyVurdering = lagredeVurderinger.find { it.begrunnelse == "Ny vurdering" }
        assertThat(nyVurdering).isNotNull
        // Eksisterende vurdering skal beholdes i delen som ikke overlapper
        val gammelVurdering = lagredeVurderinger.find { it.begrunnelse == "Eksisterende vurdering" }
        assertThat(gammelVurdering).isNotNull
    }

    // -------------------------------------------------------------------------
    // Slå sammen med forrige behandling
    // -------------------------------------------------------------------------

    @Test
    fun `skal slå sammen nye vurderinger med eksisterende vurderinger fra tidligere behandling`() {
        val forrigeBehandlingId = BehandlingId(1L)
        val nåværendeBehandlingId = BehandlingId(2L)
        val vurderingSlot = slot<List<HelseinstitusjonVurdering>>()

        every { behandlingRepository.hent(nåværendeBehandlingId) } returns
                opprettBehandling(nåværendeBehandlingId, forrigeBehandlingId)

        val eksisterendeGrunnlag = opprettGrunnlag(
            listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "Gammel vurdering",
                    periode = Periode(1 mai 2025, 1 august 2025),
                    behandlingId = forrigeBehandlingId
                )
            )
        )

        every { helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId) } returns null
        every { helseinstitusjonRepository.hentHvisEksisterer(forrigeBehandlingId) } returns eksisterendeGrunnlag
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), capture(vurderingSlot)) } returns Unit

        val løsning = lagLøsning(
            lagHelseinstitusjonVurderingDto(
                begrunnelse = "Ny vurdering revurdering",
                faarFriKostOgLosji = false,
                forsoergerEktefelle = true,
                harFasteUtgifter = true,
                periode = Periode(1 juni 2025, 1 juli 2025)
            )
        )

        løser.løs(lagKontekst(nåværendeBehandlingId), løsning)

        val lagredeVurderinger = vurderingSlot.captured
        assertThat(lagredeVurderinger).hasSize(3)

        val gammelVurdering1 = lagredeVurderinger.find {
            it.begrunnelse == "Gammel vurdering" && it.periode == Periode(1 mai 2025, 31 mai 2025)
        }
        assertThat(gammelVurdering1).isNotNull
        assertThat(gammelVurdering1!!.vurdertIBehandling).isEqualTo(forrigeBehandlingId)

        val nyVurdering = lagredeVurderinger.find { it.begrunnelse == "Ny vurdering revurdering" }
        assertThat(nyVurdering).isNotNull
        assertThat(nyVurdering!!.periode).isEqualTo(Periode(1 juni 2025, 1 juli 2025))
        assertThat(nyVurdering.vurdertIBehandling).isEqualTo(nåværendeBehandlingId)

        val gammelVurdering2 = lagredeVurderinger.find {
            it.begrunnelse == "Gammel vurdering" && it.periode == Periode(2 juli 2025, 1 august 2025)
        }
        assertThat(gammelVurdering2).isNotNull
        assertThat(gammelVurdering2!!.vurdertIBehandling).isEqualTo(forrigeBehandlingId)
    }

    @Test
    fun `skal overskrive overlappende perioder med nye vurderinger`() {
        val forrigeBehandlingId = BehandlingId(1L)
        val nåværendeBehandlingId = BehandlingId(2L)
        val vurderingSlot = slot<List<HelseinstitusjonVurdering>>()

        every { behandlingRepository.hent(nåværendeBehandlingId) } returns
                opprettBehandling(nåværendeBehandlingId, forrigeBehandlingId)

        val eksisterendeGrunnlag = opprettGrunnlag(
            listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "Gammel vurdering hele perioden",
                    periode = Periode(1 mai 2025, 1 august 2025),
                    behandlingId = forrigeBehandlingId
                )
            )
        )

        every { helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId) } returns null
        every { helseinstitusjonRepository.hentHvisEksisterer(forrigeBehandlingId) } returns eksisterendeGrunnlag
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), capture(vurderingSlot)) } returns Unit

        // Nye vurderinger som splitter den gamle perioden
        val løsning = lagLøsning(
            lagHelseinstitusjonVurderingDto(
                begrunnelse = "Første del - reduksjon",
                periode = Periode(1 mai 2025, 1 juni 2025)
            ),
            lagHelseinstitusjonVurderingDto(
                begrunnelse = "Andre del - ikke reduksjon",
                faarFriKostOgLosji = false,
                periode = Periode(2 juni 2025, 1 august 2025)
            )
        )

        løser.løs(lagKontekst(nåværendeBehandlingId), løsning)

        // Skal kun inneholde de to nye vurderingene, ikke den gamle som overlapper
        val lagredeVurderinger = vurderingSlot.captured
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

        val eksisterendeGrunnlag = opprettGrunnlag(
            listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "Vurdering opphold 1",
                    periode = Periode(1 mai 2025, 1 juni 2025),
                    behandlingId = forrigeBehandlingId
                ),
                lagHelseinstitusjonVurdering(
                    begrunnelse = "Vurdering opphold 2",
                    periode = Periode(1 august 2025, 1 desember 2025),
                    behandlingId = forrigeBehandlingId
                )
            )
        )

        every { helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId) } returns null
        every { helseinstitusjonRepository.hentHvisEksisterer(forrigeBehandlingId) } returns eksisterendeGrunnlag
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), capture(vurderingSlot)) } returns Unit

        // Ny vurdering som bare overlapper med første periode
        val løsning = lagLøsning(
            lagHelseinstitusjonVurderingDto(
                begrunnelse = "Ny vurdering opphold 1",
                faarFriKostOgLosji = false,
                harFasteUtgifter = true,
                periode = Periode(1 mai 2025, 1 juni 2025)
            )
        )

        løser.løs(lagKontekst(nåværendeBehandlingId), løsning)

        val lagredeVurderinger = vurderingSlot.captured
        assertThat(lagredeVurderinger).hasSize(2)
        assertThat(lagredeVurderinger.map { it.begrunnelse }).containsExactlyInAnyOrder(
            "Ny vurdering opphold 1",
            "Vurdering opphold 2"
        )
        assertThat(lagredeVurderinger.find { it.begrunnelse == "Vurdering opphold 2" }!!.vurdertIBehandling)
            .isEqualTo(forrigeBehandlingId)
    }

    // -------------------------------------------------------------------------
    // Begrunnelse i LøsningsResultat
    // -------------------------------------------------------------------------

    @Test
    fun `skal returnere begrunnelse sammensatt av alle vurderinger`() {
        val behandlingId = BehandlingId(1L)

        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns null
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any()) } returns Unit

        val resultat = løser.løs(
            lagKontekst(behandlingId),
            lagLøsning(
                lagHelseinstitusjonVurderingDto(
                    begrunnelse = "Første begrunnelse",
                    periode = Periode(1 mai 2025, 1 juni 2025)
                ),
                lagHelseinstitusjonVurderingDto(
                    begrunnelse = "Andre begrunnelse",
                    faarFriKostOgLosji = false,
                    forsoergerEktefelle = true,
                    harFasteUtgifter = true,
                    periode = Periode(2 juni 2025, 1 juli 2025)
                )
            )
        )

        assertThat(resultat.begrunnelse).isEqualTo("Første begrunnelse Andre begrunnelse")
    }

    // -------------------------------------------------------------------------
    // Null-felter
    // -------------------------------------------------------------------------

    @Test
    fun `skal håndtere vurdering der forsoergerEktefelle og harFasteUtgifter er null`() {
        val behandlingId = BehandlingId(1L)
        val vurderingSlot = slot<List<HelseinstitusjonVurdering>>()

        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns null
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), capture(vurderingSlot)) } returns Unit

        løser.løs(
            lagKontekst(behandlingId),
            lagLøsning(
                HelseinstitusjonVurderingDto(
                    begrunnelse = "Vurdering med null-verdier",
                    faarFriKostOgLosji = false,
                    forsoergerEktefelle = null,
                    harFasteUtgifter = null,
                    periode = Periode(1 mai 2025, 1 august 2025)
                )
            )
        )

        val lagret = vurderingSlot.captured.single()
        assertThat(lagret.forsoergerEktefelle).isNull()
        assertThat(lagret.harFasteUtgifter).isNull()
        assertThat(lagret.faarFriKostOgLosji).isFalse()
    }

    // -------------------------------------------------------------------------
    // Validering: første opphold
    // -------------------------------------------------------------------------

    @Test
    fun `skal kaste exception hvis første reduksjonsvurdering starter for tidlig`() {
        val behandlingId = BehandlingId(1L)
        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns opprettGrunnlag(emptyList())
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)

        val exception = assertThrows<UgyldigForespørselException> {
            løser.løs(
                lagKontekst(behandlingId),
                lagLøsning(
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "For tidlig reduksjon",
                        periode = Periode(1 februar 2025, 1 juni 2025) // starter for tidlig. Fom = 1/2, tidligste = 1/5
                    )
                )
            )
        }
        assertThat(exception.message).contains("Første reduksjonsvurdering starter for tidlig.")
    }

    @Test
    fun `skal ikke kaste exception når reduksjon starter nøyaktig på tidligste dato for første opphold`() {
        val behandlingId = BehandlingId(1L)
        // Opphold fra 1/1/2025, tidligste reduksjonsdato = 1/5/2025
        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns opprettGrunnlag(emptyList())
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any()) } returns Unit

        assertDoesNotThrow {
            løser.løs(
                lagKontekst(behandlingId),
                lagLøsning(
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Reduksjon på grensen",
                        periode = Periode(1 mai 2025, 1 august 2025) // nøyaktig tidligsteReduksjonsdato
                    )
                )
            )
        }
    }

    @Test
    fun `vurdering med forsørger er ikke reduksjonsvurdering og valideres ikke`() {
        val behandlingId = BehandlingId(1L)
        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns opprettGrunnlag(emptyList())
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any()) } returns Unit

        // forsørger = true → ikke reduksjonsvurdering → ingen validering av dato
        assertDoesNotThrow {
            løser.løs(
                lagKontekst(behandlingId),
                lagLøsning(
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Forsørger - tidlig",
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = true,
                        periode = Periode(1 februar 2025, 1 juni 2025) // ville vært for tidlig hvis reduksjon
                    )
                )
            )
        }
    }

    @Test
    fun `vurdering med faste utgifter er ikke reduksjonsvurdering og valideres ikke`() {
        val behandlingId = BehandlingId(1L)
        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns opprettGrunnlag(emptyList())
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any()) } returns Unit

        assertDoesNotThrow {
            løser.løs(
                lagKontekst(behandlingId),
                lagLøsning(
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Faste utgifter - tidlig",
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = true,
                        periode = Periode(1 februar 2025, 1 juni 2025)
                    )
                )
            )
        }
    }

    @Test
    fun `ingen validering når opphold-liste er tom`() {
        val behandlingId = BehandlingId(1L)
        // Grunnlag uten opphold
        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns
                InstitusjonsoppholdGrunnlag(oppholdene = Oppholdene(id = 1L, opphold = emptyList()))
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any()) } returns Unit

        assertDoesNotThrow {
            løser.løs(
                lagKontekst(behandlingId),
                lagLøsning(
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Ingen validering",
                        periode = Periode(1 februar 2025, 1 juni 2025)
                    )
                )
            )
        }
    }

    @Test
    fun `ingen validering når grunnlag er null`() {
        val behandlingId = BehandlingId(1L)
        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns null
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any()) } returns Unit

        assertDoesNotThrow {
            løser.løs(
                lagKontekst(behandlingId),
                lagLøsning(
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Ingen validering",
                        periode = Periode(1 februar 2025, 1 juni 2025)
                    )
                )
            )
        }
    }

    @Test
    fun `ingen validering når nyeVurderinger er tom`() {
        val behandlingId = BehandlingId(1L)
        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns opprettGrunnlag(emptyList())
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any()) } returns Unit

        assertDoesNotThrow {
            løser.løs(lagKontekst(behandlingId), lagLøsning())
        }
    }

    // -------------------------------------------------------------------------
    // Validering: påfølgende opphold
    // -------------------------------------------------------------------------

    @Test
    fun `skal kaste exception hvis reduksjon ved nytt opphold starter for tidlig`() {
        val behandlingId = BehandlingId(1L)
        val opphold = opprettToOppholdGrunnlag(emptyList())
        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns opphold
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)

        val exception = assertThrows<UgyldigForespørselException> {
            løser.løs(
                lagKontekst(behandlingId),
                lagLøsning(
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Reduksjon opphold 1",
                        periode = Periode(1 mai 2025, 1 august 2025)
                    ),
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Reduksjon opphold 2 for tidlig",
                        periode = Periode(1 februar 2026, 1 juni 2026) // fom = 1/2, tidligste = 1/4
                    )
                )
            )
        }
        assertThat(exception.message).contains("Reduksjon ved nytt opphold starter for tidlig")
    }

    @Test
    fun `skal ikke kaste exception når andre opphold er innen 3 måneder fra forrige - umiddelbar reduksjon tillatt`() {
        val behandlingId = BehandlingId(1L)
        // Opphold 1: januar-august 2025, Opphold 2: desember 2025-juli 2026
        // desember 2025 er 4 mnd etter august 2025 → IKKE innen 3 måneder → validering aktiv
        // Men her bruker vi opphold som ER innen 3 måneder
        val grunnlag = InstitusjonsoppholdGrunnlag(
            oppholdene = Oppholdene(
                id = 1L,
                opphold = listOf(
                    Segment(
                        Periode(1 januar 2025, 1 august 2025),
                        Institusjon(Institusjonstype.HS, Oppholdstype.D, "111", "Sykehus 1")
                    ),
                    Segment(
                        // starter 2 mnd etter forrige → innen 3 mnd → ingen datovalidering
                        Periode(1 oktober 2025, 1 april 2026),
                        Institusjon(Institusjonstype.HS, Oppholdstype.D, "222", "Sykehus 2")
                    )
                )
            ),
            helseoppholdvurderinger = null,
            soningsVurderinger = null
        )

        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns grunnlag
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any()) } returns Unit

        assertDoesNotThrow {
            løser.løs(
                lagKontekst(behandlingId),
                lagLøsning(
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Reduksjon opphold 1",
                        periode = Periode(1 mai 2025, 1 august 2025)
                    ),
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Reduksjon opphold 2 tidlig - ok pga innen 3 mnd",
                        periode = Periode(1 november 2025, 1 april 2026) // tidlig, men tillatt
                    )
                )
            )
        }
    }

    @Test
    fun `skal ikke kaste exception når reduksjon for andre opphold starter nøyaktig på tidligste dato`() {
        val behandlingId = BehandlingId(1L)
        val opphold = opprettToOppholdGrunnlag(emptyList())
        every { helseinstitusjonRepository.hentHvisEksisterer(behandlingId) } returns opphold
        every { behandlingRepository.hent(behandlingId) } returns opprettBehandling(behandlingId, null)
        every { helseinstitusjonRepository.lagreHelseVurdering(any(), any()) } returns Unit

        // Opphold 2 starter 1/12/2025, tidligste reduksjonsdato = 1/4/2026
        assertDoesNotThrow {
            løser.løs(
                lagKontekst(behandlingId),
                lagLøsning(
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Reduksjon opphold 1",
                        periode = Periode(1 mai 2025, 1 august 2025)
                    ),
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "Reduksjon opphold 2 på grensen",
                        periode = Periode(1 april 2026, 1 juli 2026) // nøyaktig tidligsteReduksjonsdato
                    )
                )
            )
        }
    }

    // -------------------------------------------------------------------------
    // Hjelpemetoder
    // -------------------------------------------------------------------------

    private fun lagLøsning(vararg vurderinger: HelseinstitusjonVurderingDto) =
        AvklarHelseinstitusjonLøsning(HelseinstitusjonVurderingerDto(vurderinger.toList()))

    private fun lagKontekst(behandlingId: BehandlingId) = AvklaringsbehovKontekst(
        bruker = Bruker("12345678901"),
        kontekst = FlytKontekst(
            sakId = SakId(1L),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling
        )
    )

    private fun opprettBehandling(behandlingId: BehandlingId, forrigeBehandlingId: BehandlingId?) = Behandling(
        behandlingId,
        forrigeBehandlingId,
        BehandlingReferanse(UUID.randomUUID()),
        SakId(1L),
        TypeBehandling.Førstegangsbehandling,
        årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
        versjon = 1L
    )

    private fun lagHelseinstitusjonVurdering(
        begrunnelse: String,
        periode: Periode,
        behandlingId: BehandlingId,
        faarFriKostOgLosji: Boolean = true,
        forsoergerEktefelle: Boolean = false,
        harFasteUtgifter: Boolean = false,
    ) = HelseinstitusjonVurdering(
        begrunnelse = begrunnelse,
        faarFriKostOgLosji = faarFriKostOgLosji,
        forsoergerEktefelle = forsoergerEktefelle,
        harFasteUtgifter = harFasteUtgifter,
        periode = periode,
        vurdertIBehandling = behandlingId,
        vurdertAv = "saksbehandler",
        vurdertTidspunkt = LocalDateTime.of(2025, 1, 1, 12, 0)
    )

    private fun lagHelseinstitusjonVurderingDto(
        begrunnelse: String,
        periode: Periode,
        faarFriKostOgLosji: Boolean = true,
        forsoergerEktefelle: Boolean = false,
        harFasteUtgifter: Boolean = false,
    ) = HelseinstitusjonVurderingDto(
        begrunnelse = begrunnelse,
        faarFriKostOgLosji = faarFriKostOgLosji,
        forsoergerEktefelle = forsoergerEktefelle,
        harFasteUtgifter = harFasteUtgifter,
        periode = periode
    )

    private fun opprettGrunnlag(vurderinger: List<HelseinstitusjonVurdering>) = InstitusjonsoppholdGrunnlag(
        oppholdene = Oppholdene(
            id = 1L,
            opphold = listOf(
                Segment(
                    Periode(1 januar 2025, 31 desember 2025),
                    Institusjon(Institusjonstype.HS, Oppholdstype.H, "987654321", "Test Helseinstitusjon")
                )
            )
        ),
        helseoppholdvurderinger = Helseoppholdvurderinger(
            id = 1L,
            vurderinger = vurderinger,
            vurdertTidspunkt = LocalDateTime.of(2025, 5, 1, 12, 0)
        ),
        soningsVurderinger = null
    )

    private fun opprettToOppholdGrunnlag(vurderinger: List<HelseinstitusjonVurdering>) =
        InstitusjonsoppholdGrunnlag(
            oppholdene = Oppholdene(
                id = 1L,
                opphold = listOf(
                    Segment(
                        Periode(1 januar 2025, 1 august 2025),
                        Institusjon(Institusjonstype.HS, Oppholdstype.H, "987654321", "Sykehus 1")
                    ),
                    Segment(
                        Periode(1 desember 2025, 1 juli 2026),
                        Institusjon(Institusjonstype.HS, Oppholdstype.D, "123456789", "Sykehus 2")
                    )
                )
            ),
            helseoppholdvurderinger = Helseoppholdvurderinger(
                id = 1L,
                vurderinger = vurderinger,
                vurdertTidspunkt = LocalDateTime.of(2025, 5, 1, 12, 0)
            ),
            soningsVurderinger = null
        )
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
            vurdertTidspunkt = LocalDateTime.of(2025, 5, 1, 12, 0)
        ),
        soningsVurderinger = null
    )
}