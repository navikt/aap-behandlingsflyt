package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarHelseinstitusjonLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingerDto
import no.nav.aap.behandlingsflyt.help.avklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryInstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class AvklarHelseinstitusjonLøserTest {

    private val behandlingRepository = InMemoryBehandlingRepository
    private val helseinstitusjonRepository = InMemoryInstitusjonsoppholdRepository(
        Clock.fixed(
            LocalDateTime.of(2025, 5, 1, 12, 0).toInstant(
                ZoneOffset.UTC
            ), ZoneId.systemDefault()
        )
    )
    private val løser: AvklarHelseinstitusjonLøser by lazy {
        AvklarHelseinstitusjonLøser(behandlingRepository, helseinstitusjonRepository)
    }

    @Test
    fun `forBehov returnerer AVKLAR_HELSEINSTITUSJON`() {
        assertThat(løser.forBehov()).isEqualTo(Definisjon.AVKLAR_HELSEINSTITUSJON)
    }

    @Test
    fun `skal håndtere flere vurderinger med forskjellige perioder`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id
        val førsteMai = 1 mai 2025
        val andreJuni = 2 juni 2025
        val andreJuli = 2 juli 2025

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

        val resultat = løser.løs(avklaringsbehovKontekst { this.behandling = behandling }, løsning)

        assertThat(resultat.begrunnelse).contains("Periode 1", "Periode 2", "Periode 3")

        val lagredeVurderinger =
            helseinstitusjonRepository.hentHvisEksisterer(behandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
        assertThat(lagredeVurderinger).hasSize(3)
        assertThat(lagredeVurderinger[0].periode.fom).isEqualTo(førsteMai)
        assertThat(lagredeVurderinger[1].periode.fom).isEqualTo(andreJuni)
        assertThat(lagredeVurderinger[2].periode.fom).isEqualTo(andreJuli)
    }

    @Test
    fun `skal håndtere vurdering med alle felter satt til null`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id

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

        løser.løs(avklaringsbehovKontekst { this.behandling = behandling }, løsning)

        val lagredeVurderinger =
            helseinstitusjonRepository.hentHvisEksisterer(behandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
        assertThat(lagredeVurderinger).hasSize(1)
        assertThat(lagredeVurderinger[0].forsoergerEktefelle).isNull()
        assertThat(lagredeVurderinger[0].harFasteUtgifter).isNull()
        assertThat(lagredeVurderinger[0].faarFriKostOgLosji).isFalse()
    }

    @Test
    fun `skal kunne oppdatere kun en del av eksisterende vurdering`() {
        val (_, behandling, revurdering) = opprettInMemorySakOgRevurdering()
        val forrigeBehandlingId = behandling.id
        val nåværendeBehandlingId = revurdering.id

        helseinstitusjonRepository.lagreHelseVurdering(
            forrigeBehandlingId, listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "Gammel vurdering",
                    periode = Periode(1 mai 2025, 1 juni 2025),
                    behandlingId = forrigeBehandlingId
                )
            )
        )

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

        løser.løs(avklaringsbehovKontekst { this.behandling = revurdering }, løsning)

        // Den nye vurderingen skal være lagret
        val lagredeVurderinger =
            helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
        assertThat(lagredeVurderinger.any {
            it.begrunnelse == "Oppdatering av liten del" &&
                    it.vurdertIBehandling == nåværendeBehandlingId
        }).isTrue()
    }

    @Test
    fun `skal returnere korrekt begrunnelse sammensatt av alle vurderinger`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

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

        val resultat = løser.løs(avklaringsbehovKontekst { this.behandling = behandling }, løsning)

        assertThat(resultat.begrunnelse).isEqualTo("Første begrunnelse Andre begrunnelse")
    }

    // -------------------------------------------------------------------------
    // Ingen tidligere vurderinger
    // -------------------------------------------------------------------------

    @Test
    fun `skal lagre ny vurdering når det ikke finnes tidligere vurderinger`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id

        val løsning = lagLøsning(
            lagHelseinstitusjonVurderingDto(
                begrunnelse = "Ny vurdering",
                periode = Periode(1 mai 2025, 1 august 2025)
            )
        )

        val resultat = løser.løs(avklaringsbehovKontekst { this.behandling = behandling }, løsning)

        assertThat(resultat.begrunnelse).isEqualTo("Ny vurdering")

        val lagredeVurderinger =
            helseinstitusjonRepository.hentHvisEksisterer(behandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
        assertThat(lagredeVurderinger).hasSize(1)
        assertThat(lagredeVurderinger[0].begrunnelse).isEqualTo("Ny vurdering")
        assertThat(lagredeVurderinger[0].faarFriKostOgLosji).isTrue()
        assertThat(lagredeVurderinger[0].periode).isEqualTo(Periode(1 mai 2025, 1 august 2025))
    }

    @Test
    fun `skal bruke eksisterende vurderinger på nåværende behandling når de finnes`() {
        val (_, behandling, revurdering) = opprettInMemorySakOgRevurdering()
        val forrigeBehandlingId = behandling.id
        val nåværendeBehandlingId = revurdering.id

        helseinstitusjonRepository.lagreHelseVurdering(
            forrigeBehandlingId, listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "Eksisterende vurdering",
                    periode = Periode(1 januar 2025, 31 desember 2025),
                    behandlingId = nåværendeBehandlingId
                )
            )
        )

        val løsning = lagLøsning(
            lagHelseinstitusjonVurderingDto(
                begrunnelse = "Ny vurdering",
                periode = Periode(1 mai 2025, 1 august 2025)
            )
        )

        løser.løs(avklaringsbehovKontekst { this.behandling = revurdering }, løsning)

        val lagredeVurderinger =
            helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
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
        val (_, behandling, revurdering) = opprettInMemorySakOgRevurdering()
        val forrigeBehandlingId = behandling.id
        val nåværendeBehandlingId = revurdering.id

        helseinstitusjonRepository.lagreHelseVurdering(
            forrigeBehandlingId, listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "Gammel vurdering",
                    periode = Periode(1 mai 2025, 1 august 2025),
                    behandlingId = forrigeBehandlingId
                )
            )
        )

        val løsning = lagLøsning(
            lagHelseinstitusjonVurderingDto(
                begrunnelse = "Ny vurdering revurdering",
                faarFriKostOgLosji = false,
                forsoergerEktefelle = true,
                harFasteUtgifter = true,
                periode = Periode(1 juni 2025, 1 juli 2025)
            )
        )

        løser.løs(avklaringsbehovKontekst { this.behandling = revurdering }, løsning)

        val lagredeVurderinger =
            helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
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
        val (_, behandling, revurdering) = opprettInMemorySakOgRevurdering()
        val forrigeBehandlingId = behandling.id
        val nåværendeBehandlingId = revurdering.id

        helseinstitusjonRepository.lagreHelseVurdering(
            forrigeBehandlingId, listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "Gammel vurdering hele perioden",
                    periode = Periode(1 mai 2025, 1 august 2025),
                    behandlingId = forrigeBehandlingId
                )
            )
        )

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

        løser.løs(avklaringsbehovKontekst { this.behandling = revurdering }, løsning)

        // Skal kun inneholde de to nye vurderingene, ikke den gamle som overlapper
        val lagredeVurderinger =
            helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
        assertThat(lagredeVurderinger).hasSize(2)
        assertThat(lagredeVurderinger.map { it.begrunnelse }).containsExactlyInAnyOrder(
            "Første del - reduksjon",
            "Andre del - ikke reduksjon"
        )
        assertThat(lagredeVurderinger.all { it.vurdertIBehandling == nåværendeBehandlingId }).isTrue()
    }

    @Test
    fun `skal beholde gamle vurderinger som ikke overlapper med nye`() {
        val (_, behandling, revurdering) = opprettInMemorySakOgRevurdering()
        val forrigeBehandlingId = behandling.id
        val nåværendeBehandlingId = revurdering.id

        helseinstitusjonRepository.lagreHelseVurdering(
            forrigeBehandlingId, listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "Vurdering opphold 1",
                    periode = Periode(1 mai 2025, 1 juni 2025),
                    behandlingId = forrigeBehandlingId
                ), lagHelseinstitusjonVurdering(
                    begrunnelse = "Vurdering opphold 2",
                    periode = Periode(1 august 2025, 1 desember 2025),
                    behandlingId = forrigeBehandlingId
                )
            )
        )

        // Ny vurdering som bare overlapper med første periode
        val løsning = lagLøsning(
            lagHelseinstitusjonVurderingDto(
                begrunnelse = "Ny vurdering opphold 1",
                faarFriKostOgLosji = false,
                harFasteUtgifter = true,
                periode = Periode(1 mai 2025, 1 juni 2025)
            )
        )

        løser.løs(avklaringsbehovKontekst { this.behandling = revurdering }, løsning)

        val lagredeVurderinger =
            helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
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
        val (_, behandling) = opprettInMemorySakOgBehandling()

        val resultat = løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
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
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id

        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
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

        val lagret =
            helseinstitusjonRepository.hentHvisEksisterer(behandlingId)?.helseoppholdvurderinger?.vurderinger?.firstOrNull()
        assertThat(lagret?.forsoergerEktefelle).isNull()
        assertThat(lagret?.harFasteUtgifter).isNull()
        assertThat(lagret?.faarFriKostOgLosji).isFalse()
    }

    // -------------------------------------------------------------------------
    // Validering: første opphold
    // -------------------------------------------------------------------------

    @Test
    fun `skal kaste exception hvis første reduksjonsvurdering starter for tidlig`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id

        lagreOppholdMedTomVurdering(behandlingId, lagInstitusjonsopphold(fra = 1 januar 2025, til = 31 desember 2025))

        val exception = assertThrows<UgyldigForespørselException> {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
                lagLøsning(
                    lagHelseinstitusjonVurderingDto(
                        begrunnelse = "For tidlig reduksjon",
                        periode = Periode(1 februar 2025, 1 juni 2025) // starter for tidlig. Fom = 1/2, tidligste = 1/5
                    )
                )
            )
        }
        assertThat(exception.message).contains("Reduksjonsvurdering starter for tidlig.")
    }

    @Test
    fun `skal ikke kaste exception når reduksjon starter nøyaktig på tidligste dato for første opphold`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id

        // Opphold fra 1/1/2025, tidligste reduksjonsdato = 1/5/2025
        lagreOppholdMedTomVurdering(behandlingId, lagInstitusjonsopphold(fra = 1 januar 2025, til = 31 desember 2025))

        assertDoesNotThrow {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
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
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id

        lagreOppholdMedTomVurdering(behandlingId, lagInstitusjonsopphold(fra = 1 januar 2025, til = 31 desember 2025))

        // forsørger = true → ikke reduksjonsvurdering → ingen validering av dato
        assertDoesNotThrow {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
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
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id

        lagreOppholdMedTomVurdering(behandlingId, lagInstitusjonsopphold(fra = 1 januar 2025, til = 31 desember 2025))

        assertDoesNotThrow {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
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
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id
        // Grunnlag uten opphold
        helseinstitusjonRepository.lagreHelseVurdering(behandlingId, listOf())
        helseinstitusjonRepository.lagreOpphold(behandlingId, emptyList())

        assertDoesNotThrow {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
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
        val (_, behandling) = opprettInMemorySakOgBehandling()

        assertDoesNotThrow {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
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
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id

        lagreOppholdMedTomVurdering(behandlingId, lagInstitusjonsopphold(fra = 1 januar 2025, til = 31 desember 2025))

        assertDoesNotThrow {
            løser.løs(avklaringsbehovKontekst { this.behandling = behandling }, lagLøsning())
        }
    }

    // -------------------------------------------------------------------------
    // Validering: påfølgende opphold
    // -------------------------------------------------------------------------

    @Test
    fun `skal kaste exception hvis reduksjon ved nytt opphold starter for tidlig`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id

        lagreOppholdMedTomVurdering(
            behandlingId,
            lagInstitusjonsopphold(fra = 1 januar 2025, til = 1 august 2025, institusjonsnavn = "Sykehus 1"),
            lagInstitusjonsopphold(
                fra = 1 desember 2025,
                til = 1 juli 2026,
                kategori = Oppholdstype.D,
                orgnr = "123456789",
                institusjonsnavn = "Sykehus 2"
            ),
        )

        val exception = assertThrows<UgyldigForespørselException> {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
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
        assertThat(exception.message).contains("Reduksjonsvurdering starter for tidlig.")
    }

    @Test
    fun `skal ikke kaste exception når andre opphold er innen 3 måneder fra forrige - umiddelbar reduksjon tillatt`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id

        // Opphold 1: januar-august 2025, Opphold 2: desember 2025-juli 2026
        // desember 2025 er 4 mnd etter august 2025 → IKKE innen 3 måneder → validering aktiv
        // Men her bruker vi opphold som ER innen 3 måneder
        helseinstitusjonRepository.lagreOpphold(
            behandlingId, listOf(
                lagInstitusjonsopphold(
                    fra = 1 januar 2025,
                    til = 1 august 2025,
                    kategori = Oppholdstype.D,
                    orgnr = "111",
                    institusjonsnavn = "Sykehus 1"
                ),
                lagInstitusjonsopphold(
                    fra = 1 oktober 2025,
                    til = 1 april 2026,
                    kategori = Oppholdstype.D,
                    orgnr = "222",
                    institusjonsnavn = "Sykehus 2"
                )
            )
        )

        assertDoesNotThrow {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
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
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id

        lagreOppholdMedTomVurdering(
            behandlingId,
            lagInstitusjonsopphold(fra = 1 januar 2025, til = 1 august 2025, institusjonsnavn = "Sykehus 1"),
            lagInstitusjonsopphold(
                fra = 1 desember 2025,
                til = 1 juli 2026,
                kategori = Oppholdstype.D,
                orgnr = "123456789",
                institusjonsnavn = "Sykehus 2"
            ),
        )

        // Opphold 2 starter 1/12/2025, tidligste reduksjonsdato = 1/4/2026
        assertDoesNotThrow {
            løser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
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
    // Justering av sluttdato på opphold
    // -------------------------------------------------------------------------

    @Test
    fun `løpende opphold forkortet til konkret dato med tom ny vurdering`() {
        val (_, behandling, revurdering) = opprettInMemorySakOgRevurdering()
        val forrigeBehandlingId = behandling.id
        val nåværendeBehandlingId = revurdering.id

        // Forrige behandling: opphold løpende (tom = 2999-01-01), vurdering fra 1/6 til 2999-01-01
        val løpendeTom = 1 januar 2999
        helseinstitusjonRepository.lagreOpphold(
            forrigeBehandlingId, listOf(
                lagInstitusjonsopphold(fra = 1 februar 2025, til = løpendeTom)
            )
        )
        helseinstitusjonRepository.lagreHelseVurdering(
            forrigeBehandlingId, listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "reduksjon løpende",
                    periode = Periode(1 juni 2025, løpendeTom),
                    behandlingId = forrigeBehandlingId
                )
            )
        )

        // Nåværende behandling: samme opphold, men tom endret til konkret dato
        val nyTom = 1 august 2025
        helseinstitusjonRepository.lagreOpphold(
            nåværendeBehandlingId, listOf(
                lagInstitusjonsopphold(fra = 1 februar 2025, til = nyTom)
            )
        )
        helseinstitusjonRepository.lagreHelseVurdering(nåværendeBehandlingId, emptyList())

        løser.løs(
            avklaringsbehovKontekst { this.behandling = revurdering },
            lagLøsning()
        )

        val lagrede =
            helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
        assertThat(lagrede).hasSize(1)
        assertThat(lagrede[0].periode.tom).isEqualTo(nyTom)
        assertThat(lagrede[0].vurdertIBehandling).isEqualTo(forrigeBehandlingId)
    }

    @Test
    fun `løpende opphold forkortet til konkret dato - vedtatt vurdering klippes og reassigneres`() {
        val (_, behandling, revurdering) = opprettInMemorySakOgRevurdering()
        val forrigeBehandlingId = behandling.id
        val nåværendeBehandlingId = revurdering.id

        // Forrige behandling: opphold løpende (tom = 2999-01-01), vurdering fra 1/6 til 2999-01-01
        val løpendeTom = 1 januar 2999
        helseinstitusjonRepository.lagreOpphold(
            forrigeBehandlingId, listOf(
                lagInstitusjonsopphold(fra = 1 februar 2025, til = løpendeTom)
            )
        )
        helseinstitusjonRepository.lagreHelseVurdering(
            forrigeBehandlingId, listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "reduksjon løpende",
                    periode = Periode(1 juni 2025, løpendeTom),
                    behandlingId = forrigeBehandlingId
                )
            )
        )

        // Nåværende behandling: samme opphold, men tom endret til konkret dato
        val nyTom = 1 august 2025

        helseinstitusjonRepository.lagreOpphold(
            nåværendeBehandlingId, listOf(
                lagInstitusjonsopphold(fra = 1 februar 2025, til = nyTom)
            )
        )
        helseinstitusjonRepository.lagreHelseVurdering(nåværendeBehandlingId, emptyList())

        løser.løs(
            avklaringsbehovKontekst { this.behandling = revurdering },
            lagLøsning(
                lagHelseinstitusjonVurderingDto(
                    begrunnelse = "ny reduksjon etter forkortelse",
                    periode = Periode(1 juni 2025, nyTom)
                )
            )
        )

        val lagrede =
            helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
        assertThat(lagrede).hasSize(1)
        assertThat(lagrede[0].periode.tom).isEqualTo(nyTom)
        assertThat(lagrede[0].vurdertIBehandling).isEqualTo(nåværendeBehandlingId)
    }

    @Test
    fun `opphold forkortet - vedtatt reduksjonsvurdering klippes til ny sluttdato og får ny behandlingId`() {
        val (_, behandling, revurdering) = opprettInMemorySakOgRevurdering()
        val forrigeBehandlingId = behandling.id
        val nåværendeBehandlingId = revurdering.id

        // Forrige: opphold og vurdering fom=2025-06-02 tom=2025-10-01
        val gammelTom = 1 oktober 2025
        helseinstitusjonRepository.lagreOpphold(
            forrigeBehandlingId, listOf(
                lagInstitusjonsopphold(fra = 1 februar 2025, til = gammelTom)
            )
        )
        helseinstitusjonRepository.lagreHelseVurdering(
            forrigeBehandlingId, listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "reduksjon gammel periode",
                    periode = Periode(1 juni 2025, gammelTom),
                    behandlingId = forrigeBehandlingId
                )
            )
        )

        // Nåværende: opphold forkortet til 1/8
        val nyTom = 1 august 2025
        helseinstitusjonRepository.lagreOpphold(
            nåværendeBehandlingId, listOf(
                lagInstitusjonsopphold(fra = 1 februar 2025, til = nyTom)
            )
        )

        løser.løs(
            avklaringsbehovKontekst { this.behandling = revurdering },
            lagLøsning(
                lagHelseinstitusjonVurderingDto(
                    begrunnelse = "ny vurdering etter forkortelse",
                    periode = Periode(1 juni 2025, nyTom)
                )
            )
        )

        val lagrede =
            helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
        assertThat(lagrede.none { it.periode.tom.isAfter(nyTom) })
            .`as`("Ingen vurderinger skal strekke seg forbi ny sluttdato").isTrue()
        assertThat(lagrede.all { it.vurdertIBehandling == nåværendeBehandlingId }).isTrue()
    }

    @Test
    fun `opphold forlenget - vedtatt reduksjonsvurdering forlenges til ny sluttdato og får ny behandlingId`() {
        val (_, behandling, revurdering) = opprettInMemorySakOgRevurdering()
        val forrigeBehandlingId = behandling.id
        val nåværendeBehandlingId = revurdering.id

        // Forrige: opphold og vurdering tom=2025-08-01
        val gammelTom = 1 august 2025
        helseinstitusjonRepository.lagreOpphold(
            forrigeBehandlingId, listOf(
                lagInstitusjonsopphold(fra = 1 februar 2025, til = gammelTom)
            )
        )
        helseinstitusjonRepository.lagreHelseVurdering(
            forrigeBehandlingId, listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "reduksjon gammel periode",
                    periode = Periode(1 juni 2025, gammelTom),
                    behandlingId = forrigeBehandlingId
                )
            )
        )

        // Nåværende: opphold forlenget til 1/11
        val nyTom = 1 november 2025
        helseinstitusjonRepository.lagreOpphold(
            nåværendeBehandlingId, listOf(
                lagInstitusjonsopphold(fra = 1 februar 2025, til = nyTom)
            )
        )
        løser.løs(
            avklaringsbehovKontekst { this.behandling = revurdering },
            lagLøsning(
                lagHelseinstitusjonVurderingDto(
                    begrunnelse = "ny vurdering etter forlengelse",
                    periode = Periode(1 juni 2025, nyTom)
                )
            )
        )

        val lagrede =
            helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
        val vurdering = lagrede.find { it.begrunnelse == "ny vurdering etter forlengelse" }
        assertThat(vurdering).isNotNull
        assertThat(vurdering!!.periode.tom).isEqualTo(nyTom)
        assertThat(vurdering.vurdertIBehandling).isEqualTo(nåværendeBehandlingId)
    }

    @Test
    fun `uendret opphold - vedtatt vurdering beholdes med opprinnelig behandlingId`() {
        val (_, behandling, revurdering) = opprettInMemorySakOgRevurdering()
        val forrigeBehandlingId = behandling.id
        val nåværendeBehandlingId = revurdering.id

        val uendretTom = 1 august 2025
        helseinstitusjonRepository.lagreOpphold(
            forrigeBehandlingId, listOf(
                lagInstitusjonsopphold(fra = 1 februar 2025, til = uendretTom)
            )
        )
        helseinstitusjonRepository.lagreHelseVurdering(
            forrigeBehandlingId, listOf(
                lagHelseinstitusjonVurdering(
                    begrunnelse = "vedtatt reduksjon",
                    periode = Periode(1 juni 2025, uendretTom),
                    behandlingId = forrigeBehandlingId
                )
            )
        )

        helseinstitusjonRepository.lagreOpphold(
            nåværendeBehandlingId, listOf(
                lagInstitusjonsopphold(fra = 1 februar 2025, til = uendretTom)
            )
        )

        løser.løs(
            avklaringsbehovKontekst { this.behandling = revurdering },
            lagLøsning(
                lagHelseinstitusjonVurderingDto(
                    begrunnelse = "ny vurdering uendret opphold",
                    periode = Periode(1 juni 2025, uendretTom)
                )
            )
        )

        val lagrede =
            helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
        // Ny vurdering overskriver den gamle for samme periode
        assertThat(lagrede.find { it.begrunnelse == "ny vurdering uendret opphold" }).isNotNull
        assertThat(lagrede.none { it.vurdertIBehandling == forrigeBehandlingId })
            .`as`("Vedtatt vurdering for samme periode er erstattet av ny").isTrue()
    }

    @Test
    fun `ingen forrige behandling - nåværendeGrunnlag ignoreres og ny vurdering lagres direkte`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val behandlingId = behandling.id

        lagreOppholdMedTomVurdering(
            behandlingId,
            lagInstitusjonsopphold(
                fra = 1 februar 2025,
                til = 1 august 2025,
                orgnr = "13",
                institusjonsnavn = "Grønnlia Omsorgsopphold"
            )
        )

        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            lagLøsning(
                lagHelseinstitusjonVurderingDto(
                    begrunnelse = "ny vurdering uten forrige",
                    periode = Periode(1 juni 2025, 1 august 2025)
                )
            )
        )

        val lagrede =
            helseinstitusjonRepository.hentHvisEksisterer(behandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
        assertThat(lagrede).hasSize(1)
        assertThat(lagrede[0].begrunnelse).isEqualTo("ny vurdering uten forrige")
        assertThat(lagrede[0].vurdertIBehandling).isEqualTo(behandlingId)
    }

    // -------------------------------------------------------------------------
    // Hjelpemetoder
    // -------------------------------------------------------------------------

    private fun lagreOppholdMedTomVurdering(behandlingId: BehandlingId, vararg opphold: Institusjonsopphold) {
        helseinstitusjonRepository.lagreHelseVurdering(behandlingId, emptyList())
        helseinstitusjonRepository.lagreOpphold(behandlingId, opphold.toList())
    }

    private fun lagLøsning(vararg vurderinger: HelseinstitusjonVurderingDto) =
        AvklarHelseinstitusjonLøsning(HelseinstitusjonVurderingerDto(vurderinger.toList()))

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
        vurdertAv = Bruker("saksbehandler"),
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

    private fun lagInstitusjonsopphold(
        fra: java.time.LocalDate,
        til: java.time.LocalDate,
        institusjonstype: Institusjonstype = Institusjonstype.HS,
        kategori: Oppholdstype = Oppholdstype.H,
        orgnr: String = "987654321",
        institusjonsnavn: String = "Test Helseinstitusjon",
    ) = Institusjonsopphold(
        institusjonstype = institusjonstype,
        kategori = kategori,
        startdato = fra,
        sluttdato = til,
        orgnr = orgnr,
        institusjonsnavn = institusjonsnavn,
    )

}

