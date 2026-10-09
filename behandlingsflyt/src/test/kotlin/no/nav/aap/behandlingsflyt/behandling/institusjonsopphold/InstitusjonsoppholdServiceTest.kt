package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Helseoppholdvurderinger
import no.nav.aap.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Soningsvurderinger
import no.nav.aap.barnetillegg.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.OppholdVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.FakeUnleashBase
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBarnetilleggRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryInstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class InstitusjonsoppholdUtlederServiceTest {

    private val institusjonsoppholdRepository = InMemoryInstitusjonsoppholdRepository()

    val utlederService = InstitusjonsoppholdUtlederService(
        InMemoryBarnetilleggRepository,
        institusjonsoppholdRepository,
        InMemorySakRepository,
        InMemoryBehandlingRepository,
        FakeUnleashBase(mapOf(BehandlingsflytFeature.SammenhengendeInstitusjonsopphold to true))
    )

    // --- Hjelpefunksjoner ---

    private fun hsOpphold(fom: LocalDate, tom: LocalDate) = Segment(
        Periode(fom, tom),
        Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "Helgelandssykehuset")
    )

    private fun foOpphold(fom: LocalDate, tom: LocalDate) = Segment(
        Periode(fom, tom),
        Institusjon(Institusjonstype.FO, Oppholdstype.S, "456", "fengsel")
    )

    private fun institusjonsoppholdInput(
        institusjonsOpphold: List<Segment<Institusjon>> = emptyList(),
        soningsvurderinger: Any? = null,
        barnetillegg: List<BarnetilleggPeriode> = emptyList(),
        helsevurderinger: Any? = null,
        rettighetsperiode: Periode = rettighetsperiode()
    ) = InstitusjonsoppholdInput(
        institusjonsOpphold = institusjonsOpphold,
        soningsvurderinger = when (val vurderinger = soningsvurderinger) {
            null -> null
            is Soningsvurderinger -> vurderinger
            is List<*> -> Soningsvurderinger(
                vurderinger = vurderinger.filterIsInstance<Soningsvurdering>(),
                vurdertAv = Bruker("ident"),
                vurdertTidspunkt = LocalDateTime.now()
            )
            else -> error("Uventet type for soningsvurderinger")
        },
        barnetillegg = barnetillegg,
        helsevurderinger = when (val vurderinger = helsevurderinger) {
            null -> null
            is Helseoppholdvurderinger -> vurderinger
            is List<*> -> Helseoppholdvurderinger(
                id = null,
                vurderinger = vurderinger.filterIsInstance<HelseinstitusjonVurdering>(),
                vurdertTidspunkt = LocalDateTime.now()
            )
            else -> error("Uventet type for helsevurderinger")
        },
        rettighetsperiode = rettighetsperiode
    )

    private fun hsInput(fom: LocalDate, tom: LocalDate) = institusjonsoppholdInput(
        institusjonsOpphold = listOf(
            Segment(
                Periode(fom, tom),
                Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "Helgelandssykehuset")
            )
        ),
        rettighetsperiode = Periode(fom.minusYears(1), tom.plusYears(2))
    )

    private fun soningsvurderinger(
        vararg vurderinger: Soningsvurdering,
        vurdertAv: Bruker = Bruker("ident"),
        vurdertTidspunkt: LocalDateTime = LocalDateTime.now()
    ) = Soningsvurderinger(
        vurderinger = vurderinger.toList(),
        vurdertAv = vurdertAv,
        vurdertTidspunkt = vurdertTidspunkt
    )

    private fun helsevurderinger(
        vararg vurderinger: HelseinstitusjonVurdering,
        vurdertTidspunkt: LocalDateTime = LocalDateTime.now()
    ) = Helseoppholdvurderinger(
        id = null,
        vurderinger = vurderinger.toList(),
        vurdertTidspunkt = vurdertTidspunkt
    )

    private fun helsevurdering(
        fom: LocalDate,
        tom: LocalDate,
        faarFriKostOgLosji: Boolean = true,
        forsoergerEktefelle: Boolean = false,
        harFasteUtgifter: Boolean = false,
        vurdertTidspunkt: LocalDateTime = LocalDateTime.now().minusDays(1),
    ) = HelseinstitusjonVurdering(
        periode = Periode(fom, tom),
        begrunnelse = "begrunnelse",
        faarFriKostOgLosji = faarFriKostOgLosji,
        forsoergerEktefelle = forsoergerEktefelle,
        harFasteUtgifter = harFasteUtgifter,
        vurdertIBehandling = BehandlingId(1L),
        vurdertAv = Bruker("ident"),
        vurdertTidspunkt = vurdertTidspunkt,
    )

    private fun barnPeriode(fom: LocalDate, tom: LocalDate) = BarnetilleggPeriode(
        Periode(fom, tom),
        setOf(BarnIdentifikator.BarnIdent("barn1"))
    )

    private fun rettighetsperiode(
        fom: LocalDate = LocalDate.now().minusYears(1),
        tom: LocalDate = LocalDate.now().plusYears(2),
    ) = Periode(fom, tom)

    // --- Ingen opphold ---
    @Test
    fun `ingen opphold gir ingen avklaring`() {
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isFalse
    }

    // --- Helseinstitusjonsopphold: avklaringsbehov ---
    @Test
    fun `helseopphold over 4 måneder som er minst 2 måneder gammelt gir avklaring`() {
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                barnetillegg = emptyList(),
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isTrue
    }

    @Test
    fun `helseopphold kortere enn 4 måneder gir ikke avklaring`() {
        // Oppholdet varer bare 3 måneder - for kort til å trigge reduksjon
        val fom = LocalDate.now().minusMonths(3)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                barnetillegg = emptyList(),
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isFalse
    }

    @Test
    fun `helseopphold som ikke er minst 2 måneder gammelt gir ikke avklaring ennå`() {
        // Oppholdet er langt nok men har ikke vart i 2 måneder ennå
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now().plusMonths(5)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                barnetillegg = emptyList(),
                rettighetsperiode = rettighetsperiode(
                    fom = LocalDate.now().minusYears(1),
                    tom = LocalDate.now().plusYears(2)
                )
            )
        )
        assertThat(res.harBehovForAvklaring()).isFalse
    }

    // --- Helseinstitusjonsopphold: vurdert ---

    @Test
    fun `helseopphold vurdert med kost og losji gir avslått (skalGiReduksjon)`() {
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                barnetillegg = emptyList(),
                helsevurderinger = helsevurderinger(
                    helsevurdering(
                        fom,
                        tom,
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = false
                    )
                ),
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isFalse
        val helseVurdering = res.perioderTilVurdering.segmenter().first().verdi.helse
        assertThat(helseVurdering?.vurdering).isEqualTo(OppholdVurdering.AVSLÅTT)
    }

    @Test
    fun `helseopphold vurdert med forsørger gir godkjent (ingen reduksjon)`() {
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                barnetillegg = emptyList(),
                helsevurderinger = helsevurderinger(
                    helsevurdering(
                        fom,
                        tom,
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = true,
                        harFasteUtgifter = false
                    )
                ),
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isFalse
        val helseVurdering = res.perioderTilVurdering.segmenter().first().verdi.helse
        assertThat(helseVurdering?.vurdering).isEqualTo(OppholdVurdering.GODKJENT)
    }

    @Test
    fun `helseopphold vurdert med faste utgifter gir godkjent (ingen reduksjon)`() {
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                barnetillegg = emptyList(),
                helsevurderinger = helsevurderinger(
                    helsevurdering(
                        fom,
                        tom,
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = true
                    )
                ),
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isFalse
        val helseVurdering = res.perioderTilVurdering.segmenter().first().verdi.helse
        assertThat(helseVurdering?.vurdering).isEqualTo(OppholdVurdering.GODKJENT)
    }

    @Test
    fun `helseopphold ikke fri kost og losji gir godkjent (ingen reduksjon)`() {
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = null,
                barnetillegg = emptyList(),
                helsevurderinger = listOf(
                    helsevurdering(fom, tom, faarFriKostOgLosji = false)
                ),
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isFalse
        val helseVurdering = res.perioderTilVurdering.segmenter().first().verdi.helse
        assertThat(helseVurdering?.vurdering).isEqualTo(OppholdVurdering.GODKJENT)
    }

    // --- Gap-håndtering i helsevurderinger ---

    @Test
    fun `gap mellom oppholdsstart og første vurdering fylles inn med GODKJENT`() {
        val oppholdFom = LocalDate.now().minusMonths(5)
        val vurderingFom = LocalDate.now().minusMonths(4)
        val tom = LocalDate.now().minusMonths(1)

        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(oppholdFom, tom)),
                soningsvurderinger = null,
                barnetillegg = emptyList(),
                helsevurderinger = listOf(
                    helsevurdering(
                        vurderingFom,
                        tom,
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = false
                    )
                ),
                rettighetsperiode = rettighetsperiode()
            )
        )
        // Gapet (oppholdFom til vurderingFom - 1) skal være GODKJENT, ikke UAVKLART
        assertThat(res.harBehovForAvklaring()).isFalse
        val vurderinger = res.perioderTilVurdering.segmenter().map { it.verdi.helse?.vurdering }
        assertThat(vurderinger).doesNotContain(OppholdVurdering.UAVKLART)
        val gapMedGodkjent = res.perioderTilVurdering.segmenter().first { it.periode.fom == oppholdFom && it.periode.tom == vurderingFom.minusDays(1) }
        assertThat(gapMedGodkjent.verdi.helse?.vurdering).isEqualTo(OppholdVurdering.GODKJENT)
    }

    @Test
    fun `opphold uten noen vurdering forblir UAVKLART`() {
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = null,
                barnetillegg = emptyList(),
                helsevurderinger = null,
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isTrue
        val helseVurdering = res.perioderTilVurdering.segmenter().first().verdi.helse
        assertThat(helseVurdering?.vurdering).isEqualTo(OppholdVurdering.UAVKLART)
    }

    // --- Barnetillegg ---

    @Test
    fun `Delvis overlappende barnetillegg trenger avklaring`() {
        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(
                    Periode(
                        LocalDate.now().minusMonths(5),
                        LocalDate.now().plusMonths(1)
                    ),
                    Institusjon(
                        Institusjonstype.HS,
                        Oppholdstype.D,
                        "123",
                        "test"
                    )
                )
            ),
            soningsvurderinger = null,
            barnetillegg = listOf(
                BarnetilleggPeriode(
                    Periode(
                        LocalDate.now().minusMonths(5).minusDays(1),
                        LocalDate.now().minusMonths(4)
                    ),
                    setOf(
                        BarnIdentifikator.BarnIdent("123")
                    )
                )
            ),
            helsevurderinger = null,
            rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isTrue
    }

    @Test
    fun `barnetillegg gjennom hele oppholdet fjerner avklaringsbehov`() {
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = null,
                barnetillegg = listOf(barnPeriode(fom.minusDays(1), tom.plusDays(1))),
                helsevurderinger = null,
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isFalse
    }

    @Test
    fun `barnetillegg som kun dekker deler av oppholdet gir fortsatt avklaring`() {
        val fom = LocalDate.now().minusMonths(7)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = null,
                barnetillegg = listOf(barnPeriode(fom.minusDays(1), fom.plusMonths(1))),
                helsevurderinger = null,
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isTrue
    }

    // --- Andre opphold innen 3 måneder etter reduksjon (umiddelbarReduksjon) ---

    @Test
    fun `nytt opphold innen 3 måneder etter forrige som ga reduksjon gir umiddelbar avklaring`() {
        val forsteOppholdFom = LocalDate.now().minusMonths(10)
        val forsteOppholdTom = LocalDate.now().minusMonths(4)
        val andreOppholdFom = LocalDate.now().minusMonths(3)
        val andreOppholdTom = LocalDate.now().minusMonths(1)

        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(
                    hsOpphold(forsteOppholdFom, forsteOppholdTom),
                    hsOpphold(andreOppholdFom, andreOppholdTom),
                ),
                soningsvurderinger = null,
                barnetillegg = emptyList(),
                helsevurderinger = listOf(
                    helsevurdering(
                        forsteOppholdFom, forsteOppholdTom,
                        faarFriKostOgLosji = true, forsoergerEktefelle = false, harFasteUtgifter = false,
                        vurdertTidspunkt = LocalDateTime.now().minusMonths(8)
                    )
                ),
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isTrue
        assertThat(res.perioderTilVurdering.segmenter()).hasSize(2)

        // Andre opphold skal ha umiddelbarReduksjon = true
        val andreOpphold = res.perioderTilVurdering.segmenter()
            .first { it.periode.fom >= andreOppholdFom }
        assertThat(andreOpphold.verdi.helse?.umiddelbarReduksjon).isTrue
    }

    @Test
    fun `nytt opphold mer enn 3 måneder etter forrige gir ikke umiddelbar avklaring`() {
        val forsteOppholdFom = LocalDate.now().minusMonths(12)
        val forsteOppholdTom = LocalDate.now().minusMonths(7)
        // Mer enn 3 måneder siden forrige avsluttet
        val andreOppholdFom = LocalDate.now().minusMonths(3)
        val andreOppholdTom = LocalDate.now().minusMonths(1)

        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(
                    hsOpphold(forsteOppholdFom, forsteOppholdTom),
                    hsOpphold(andreOppholdFom, andreOppholdTom),
                ),
                soningsvurderinger = null,
                barnetillegg = emptyList(),
                helsevurderinger = listOf(
                    helsevurdering(
                        forsteOppholdFom, forsteOppholdTom,
                        faarFriKostOgLosji = true, forsoergerEktefelle = false, harFasteUtgifter = false,
                        vurdertTidspunkt = LocalDateTime.now().minusMonths(10)
                    )
                ),
                rettighetsperiode = rettighetsperiode()
            )
        )
        // Andre opphold er for kort (2 måneder) og ikke innen 3 måneder fra et godkjent
        val andreOpphold = res.perioderTilVurdering.segmenter()
            .firstOrNull { it.periode.fom >= andreOppholdFom }
        assertThat(andreOpphold?.verdi?.helse?.umiddelbarReduksjon ?: false).isFalse
    }

    // --- Soningsopphold ---

    @Test
    fun `soner noe, det krever avklaring`() {
        val soningsstart = LocalDate.now().minusMonths(5)
        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(
                    Periode(
                        soningsstart,
                        LocalDate.now().minusMonths(1)
                    ),
                    Institusjon(
                        Institusjonstype.FO,
                        Oppholdstype.S,
                        "123123123",
                        "test fengsel"
                    )
                ),
                Segment(
                    Periode(
                        soningsstart.plusMonths(1),
                        LocalDate.now()
                    ),
                    Institusjon(
                        Institusjonstype.HS,
                        Oppholdstype.D,
                        "321321321",
                        "test sykehuset"
                    )
                )
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isTrue
    }

    @Test
    fun `soner noe, det krever avklaring men ikke etter at det har blitt vurdert`() {
        val soningsstart = LocalDate.now().minusMonths(5)
        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(
                    Periode(
                        soningsstart,
                        LocalDate.now().minusMonths(1)
                    ),
                    Institusjon(
                        Institusjonstype.FO,
                        Oppholdstype.S,
                        "123123123",
                        "test fengsel"
                    )
                ),
                Segment(
                    Periode(
                        soningsstart.plusMonths(1),
                        LocalDate.now()
                    ),
                    Institusjon(
                        Institusjonstype.HS,
                        Oppholdstype.D,
                        "321321321",
                        "test sykehuset"
                    )
                )
            ),
            soningsvurderinger = listOf(
                Soningsvurdering(
                    skalOpphøre = true,
                    begrunnelse = "jobber ikke utenfor",
                    fraDato = soningsstart
                )
            ),
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isFalse
    }

    @Test
    fun `soningsopphold uten vurdering gir avklaring`() {
        val fom = LocalDate.now().minusMonths(3)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(foOpphold(fom, tom)),
                soningsvurderinger = null,
                barnetillegg = emptyList(),
                helsevurderinger = null,
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isTrue
        assertThat(res.perioderTilVurdering.segmenter().first().verdi.harUavklartSoningsopphold()).isTrue
    }

    @Test
    fun `soningsopphold vurdert til opphør gir ingen avklaring`() {
        val fom = LocalDate.now().minusMonths(3)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(foOpphold(fom, tom)),
                soningsvurderinger = listOf(
                    Soningsvurdering(skalOpphøre = true, begrunnelse = "soner", fraDato = fom)
                ),
                barnetillegg = emptyList(),
                helsevurderinger = null,
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isFalse
        val soningVurdering = res.perioderTilVurdering.segmenter().first().verdi.soning
        assertThat(soningVurdering?.vurdering).isEqualTo(OppholdVurdering.AVSLÅTT)
    }

    @Test
    fun `soningsopphold vurdert til ikke opphør gir ingen avklaring`() {
        val fom = LocalDate.now().minusMonths(3)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(foOpphold(fom, tom)),
                soningsvurderinger = listOf(
                    Soningsvurdering(skalOpphøre = false, begrunnelse = "frigang", fraDato = fom)
                ),
                barnetillegg = emptyList(),
                helsevurderinger = null,
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isFalse
        val soningVurdering = res.perioderTilVurdering.segmenter().first().verdi.soning
        assertThat(soningVurdering?.vurdering).isEqualTo(OppholdVurdering.GODKJENT)
    }

    @Test
    fun `flere soningsvurderinger - siste vurdering vinner`() {
        val fom = LocalDate.now().minusMonths(3)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(foOpphold(fom, tom)),
                soningsvurderinger = listOf(
                    Soningsvurdering(skalOpphøre = true, begrunnelse = "soner", fraDato = fom),
                    Soningsvurdering(skalOpphøre = false, begrunnelse = "frigang", fraDato = fom.plusWeeks(2))
                ),
                barnetillegg = emptyList(),
                helsevurderinger = null,
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isFalse
        val segmenter = res.perioderTilVurdering.segmenter()
        // Første periode: opphør
        assertThat(segmenter.first().verdi.soning?.vurdering).isEqualTo(OppholdVurdering.AVSLÅTT)
        // Siste periode: ikke opphør
        assertThat(segmenter.last().verdi.soning?.vurdering).isEqualTo(OppholdVurdering.GODKJENT)
    }

    // --- Rettighetsperiode-begrensning ---

    @Test
    fun `opphold begrenses til rettighetsperioden`() {
        val oppholdFom = LocalDate.now().minusMonths(5)
        val oppholdTom = LocalDate.now().plusMonths(5)
        val rettFom = LocalDate.now().minusMonths(2)
        val rettTom = LocalDate.now().plusMonths(2)

        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(oppholdFom, oppholdTom)),
                soningsvurderinger = null,
                barnetillegg = emptyList(),
                helsevurderinger = null,
                rettighetsperiode = Periode(rettFom, rettTom)
            ),
            begrensetTilRettighetsperiode = true
        )
        val perioder = res.perioderTilVurdering.segmenter().map { it.periode }
        assertThat(perioder).hasSize(1)
        assertThat(perioder).containsExactly(Periode(rettFom, rettTom))
    }

    @Test
    fun `uten rettighetsperiodebegrensning inkluderes perioder utenfor rettighetsperioden`() {
        val oppholdFom = LocalDate.now().minusMonths(5)
        val oppholdTom = LocalDate.now().plusMonths(5)
        val rettFom = LocalDate.now()
        val rettTom = LocalDate.now().plusMonths(2)

        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(oppholdFom, oppholdTom)),
                soningsvurderinger = null,
                barnetillegg = emptyList(),
                helsevurderinger = null,
                rettighetsperiode = Periode(rettFom, rettTom)
            ),
            begrensetTilRettighetsperiode = false
        )
        val minDato = res.perioderTilVurdering.segmenter().minOf { it.periode.fom }
        assertThat(minDato).isBefore(rettFom)
    }

    // --- Kombinasjon helse og soning ---

    @Test
    fun `avslått soning overstyrer uavklart helseopphold - ingen avklaring nødvendig`() {
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(
                    foOpphold(fom, tom),
                    hsOpphold(fom, tom),
                ),
                soningsvurderinger = listOf(
                    Soningsvurdering(skalOpphøre = true, begrunnelse = "soner", fraDato = fom)
                ),
                barnetillegg = emptyList(),
                helsevurderinger = null,
                rettighetsperiode = rettighetsperiode()
            )
        )
        // Avslått soning → harNoeUavklart() returnerer false selv om helse er UAVKLART
        assertThat(res.harBehovForAvklaring()).isFalse
    }

    // -------------------------------------------------------------------------
    // harOppholdSomVarerMinstFireMånederOgIkkeErForKort
    // -------------------------------------------------------------------------
    // fom_justert = segment.fom.withDayOfMonth(1).plusMonths(1)
    // Returner false hvis fom_justert > segment.tom  (for kort)
    // Returner true  hvis Periode(fom_justert, segment.tom).inneholder(fom_justert + 3 mnd)
    //   → oppholdet må vare minst 4 kalendermåneder
    // -------------------------------------------------------------------------

    @Test
    fun `opphold nøyaktig 4 måneder - første dag i innleggelsesmåneden - oppfyller minimumskravet`() {
        // Innlagt 1/1, fom_justert = 1/2, fom_justert+3 = 1/5, tom = 1/5 → inneholder grensen
        val fom = LocalDate.now().minusMonths(5).withDayOfMonth(1)
        val tom = fom.plusMonths(4)
        val res = utlederService.utledBehov(hsInput(fom, tom))
        assertThat(res.harBehovForAvklaring()).isTrue
    }

    @Test
    fun `opphold som slutter dagen før 4-månedersgrensen oppfyller ikke minimumskravet`() {
        // Innlagt 1/1, fom_justert = 1/2, fom_justert+3 = 1/5, tom = 30/4 → inneholder ikke grensen
        val fom = LocalDate.now().minusMonths(5).withDayOfMonth(1)
        val tom = fom.plusMonths(4).minusDays(1)
        val res = utlederService.utledBehov(hsInput(fom, tom))
        assertThat(res.harBehovForAvklaring()).isFalse
    }

    @Test
    fun `opphold som slutter før fom_justert er for kort - ikke avklaring`() {
        // Innlagt 20/1, fom_justert = 1/2, tom = 25/1 → fom_justert > tom
        val fom = LocalDate.now().minusMonths(5).withDayOfMonth(20)
        val tom = fom.plusDays(5)
        val res = utlederService.utledBehov(hsInput(fom, tom))
        assertThat(res.harBehovForAvklaring()).isFalse
    }

    @Test
    fun `opphold midt i måneden - fom_justert beregnes fra første dag i innleggelsesmåneden`() {
        // Innlagt 15/1 → fom_justert = 1/2 (ikke 15/2).
        // tom = 1/5 → Periode(1/2, 1/5).inneholder(1/5) = true
        val fom = LocalDate.now().minusMonths(5).withDayOfMonth(15)
        val tom = fom.withDayOfMonth(1).plusMonths(4)
        val res = utlederService.utledBehov(hsInput(fom, tom))
        assertThat(res.harBehovForAvklaring()).isTrue
    }

    @Test
    fun `Opphold mindre enn 3 måneder etter forrige trigger ikke behov før et opphold trigger reduksjon`() {
        val innleggelsesperiode = Periode(
            LocalDate.now().minusMonths(12),
            LocalDate.now().minusMonths(5)
        )
        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(
                    innleggelsesperiode,
                    Institusjon(
                        Institusjonstype.HS,
                        Oppholdstype.D,
                        "123123123",
                        "test"
                    )
                ),
                Segment(
                    Periode(
                        LocalDate.now().minusMonths(3),
                        LocalDate.now().minusMonths(1)
                    ),
                    Institusjon(
                        Institusjonstype.HS,
                        Oppholdstype.D,
                        "123123123",
                        "test"
                    )
                )

            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = listOf(
                HelseinstitusjonVurdering(
                    periode = innleggelsesperiode,
                    begrunnelse = "lagt inn med kost og losji",
                    faarFriKostOgLosji = true,
                    forsoergerEktefelle = false,
                    harFasteUtgifter = false,
                    vurdertIBehandling = BehandlingId(1L),
                    vurdertAv = Bruker("ident"),
                    vurdertTidspunkt = LocalDateTime.now().minusMonths(8)

                )
            ),
            rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isTrue

        assertThat(res.perioderTilVurdering.segmenter()).hasSize(2)
    }

    @Test
    fun `opphold som startet før rettighetsperioden skal trigge vurdering når oppholdet totalt er lenger enn 3 mnd`() {
        val innleggelsesperiode = Periode(
            LocalDate.now().minusMonths(3),
            LocalDate.now().plusMonths(5)
        )
        val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(
                    innleggelsesperiode,
                    Institusjon(
                        Institusjonstype.HS,
                        Oppholdstype.D,
                        "123123123",
                        "test"
                    )
                )
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = rettighetsperiode
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isTrue
        assertThat(res.perioderTilVurdering.segmenter()).hasSize(1)
    }

    // -------------------------------------------------------------------------
    // harOppholdSomVarerMerEnnFireMånederOgErMinstToMånederInnIOppholdet
    // -------------------------------------------------------------------------
    // I tillegg til minimumskravet over: oppholdStartDato.plusMonths(2) <= LocalDate.now()
    // oppholdStartDato = minDato() på hele oppholdUtenBarnetillegg-tidslinjen
    // -------------------------------------------------------------------------

    @Test
    fun `opphold er langt nok men startdato er under 2 måneder siden - ikke avklaring ennå`() {
        // Innlagt for 1 måned og 15 dager siden, slutter 5 måneder frem → langt nok,
        // men startdato + 2 mnd > i dag
        val fom = LocalDate.now().minusMonths(1).minusDays(15)
        val tom = fom.plusMonths(6)
        val res = utlederService.utledBehov(hsInput(fom, tom))
        assertThat(res.harBehovForAvklaring()).isFalse
    }

    @Test
    fun `opphold er langt nok og startdato er nøyaktig 2 måneder siden - avklaring trigges`() {
        // startdato + 2 mnd == i dag (grenseverdi <=)
        val fom = LocalDate.now().minusMonths(2)
        val tom = fom.plusMonths(6)
        val res = utlederService.utledBehov(hsInput(fom, tom))
        assertThat(res.harBehovForAvklaring()).isTrue
    }

    @Test
    fun `opphold er langt nok og startdato er mer enn 2 måneder siden - avklaring trigges`() {
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().plusMonths(1)
        val res = utlederService.utledBehov(hsInput(fom, tom))
        assertThat(res.harBehovForAvklaring()).isTrue
    }

    @Test
    fun `opphold er langt nok men slutter i fortiden og er under 2 måneder gammelt - ikke avklaring`() {
        val fom = LocalDate.now().minusMonths(1).minusDays(20)
        val tom = LocalDate.now().minusDays(5)
        val res = utlederService.utledBehov(hsInput(fom, tom))
        assertThat(res.harBehovForAvklaring()).isFalse
    }

    // -------------------------------------------------------------------------
    // mindreEnnTreMånederFraForrige
    // -------------------------------------------------------------------------

    @Test
    fun `andre opphold starter innen 3 måneder etter forrige avsluttes - avklaring selv om for kort alene`() {
        // Første opphold: langt nok (avklaring), avsluttet for 2 mnd siden
        // Andre opphold: bare 1 mnd langt (for kort alene), men starter 1 mnd etter forrige
        val fom1 = LocalDate.now().minusMonths(8)
        val tom1 = LocalDate.now().minusMonths(2)
        val fom2 = LocalDate.now().minusMonths(1).minusDays(15)
        val tom2 = LocalDate.now().minusDays(5)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "opphold1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "opphold2")),
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(fom1.minusYears(1), tom2.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isTrue
        // Begge perioder skal være med
        assertThat(res.perioderTilVurdering.segmenter()).hasSize(2)
    }

    @Test
    fun `andre opphold starter mer enn 3 måneder etter forrige - vurderes selvstendig`() {
        // Første opphold: langt nok (avklaring)
        // Andre opphold: for kort til å trigge avklaring alene, og > 3 mnd fra forrige
        val fom1 = LocalDate.now().minusMonths(10)
        val tom1 = LocalDate.now().minusMonths(5)
        val fom2 = LocalDate.now().minusMonths(1).minusDays(15) // 4+ mnd etter tom1
        val tom2 = LocalDate.now().minusDays(5)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "opphold1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "opphold2")),
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(fom1.minusYears(1), tom2.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        // Første gir avklaring, andre er for kort og for langt unna
        assertThat(res.perioderTilVurdering.segmenter().toList()).hasSize(1)
    }

    @Test
    fun `tre opphold - tredje innen 3 mnd fra andre selv om andre er innen 3 mnd fra første`() {
        // Kjeding: opphold 1 → avklaring, opphold 2 innen 3 mnd → avklaring,
        // opphold 3 innen 3 mnd fra opphold 2 → avklaring
        val fom1 = LocalDate.now().minusMonths(12)
        val tom1 = LocalDate.now().minusMonths(6)
        val fom2 = tom1.plusMonths(1)            // 1 mnd etter tom1 (< 3 mnd)
        val tom2 = fom2.plusMonths(1)
        val fom3 = tom2.plusMonths(1)            // 1 mnd etter tom2 (< 3 mnd)
        val tom3 = fom3.plusDays(20)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "opphold1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "opphold2")),
                Segment(Periode(fom3, tom3), Institusjon(Institusjonstype.HS, Oppholdstype.D, "789", "opphold3")),
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(fom1.minusYears(1), tom3.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.perioderTilVurdering.segmenter().toList()).hasSize(3)
    }

    @Test
    fun `nøyaktig 3 måneder mellom opphold - andre opphold er ikke innenfor grensen`() {
        // Betingelsen er segment.fom.isBefore(forrigeTom.plusMonths(3))
        // Hvis fom2 == forrigeTom + 3 mnd er det IKKE før, dvs. ikke innenfor
        val fom1 = LocalDate.now().minusMonths(10)
        val tom1 = LocalDate.now().minusMonths(5)
        val fom2 = tom1.plusMonths(3)            // nøyaktig 3 mnd etter → ikke innenfor
        val tom2 = fom2.plusMonths(1)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "opphold1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "opphold2")),
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(fom1.minusYears(1), tom2.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        // Andre opphold er for kort og ikke innenfor 3-månedersgrensen
        assertThat(res.perioderTilVurdering.segmenter()).hasSize(1)
    }

    @Test
    fun `dagen før 3-månedersgrensen - andre opphold er innenfor`() {
        // fom2 == forrigeTom.plusMonths(3).minusDays(1) → isBefore = true
        val fom1 = LocalDate.now().minusMonths(10)
        val tom1 = LocalDate.now().minusMonths(5)
        val fom2 = tom1.plusMonths(3).minusDays(1) // én dag innenfor grensen
        val tom2 = fom2.plusMonths(1)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "opphold1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "opphold2")),
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(fom1.minusYears(1), tom2.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.perioderTilVurdering.segmenter()).hasSize(2)
    }

    @Test
    fun `skal ikke måtte vurdere periode av opphold før rettighetsperiode startet`() {
        val innleggelsesperiode = Periode(
            LocalDate.now().minusMonths(3),
            LocalDate.now().plusMonths(5)
        )
        val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val inputMedVurdering = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(
                    innleggelsesperiode,
                    Institusjon(
                        Institusjonstype.HS,
                        Oppholdstype.D,
                        "123123123",
                        "test"
                    )
                )
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = listOf(
                HelseinstitusjonVurdering(
                    // vurdering kun for rettighetsperioden, ikke hele oppholdet
                    periode = rettighetsperiode,
                    begrunnelse = "vurder",
                    faarFriKostOgLosji = false,
                    forsoergerEktefelle = true,
                    harFasteUtgifter = true,
                    vurdertIBehandling = BehandlingId(1L),
                    vurdertAv = Bruker("ident"),
                    vurdertTidspunkt = LocalDateTime.now().plusMonths(1)
                )
            ),
            rettighetsperiode = rettighetsperiode
        )

        // selv om vurdering ikke dekker hele oppholdet kreves ikke ny manuell vurdering
        val res = utlederService.utledBehov(inputMedVurdering)
        assertThat(res.harBehovForAvklaring()).isFalse
        assertThat(res.perioderTilVurdering.segmenter()).hasSize(1)
    }

    @Test
    fun `Oppholder seg på inst mellom søknads tidspunkt og behandlingstidspunkt`() {
        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(
                    Periode(
                        LocalDate.now().minusMonths(5),
                        LocalDate.now().minusMonths(1)
                    ),
                    Institusjon(
                        Institusjonstype.HS,
                        Oppholdstype.D,
                        "123",
                        "test"
                    )
                )
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isTrue
    }

    @Test
    fun `gap i barnetillegg under helseopphold gir avklaring selv om senere barnetilleggperiode finnes`() {
        val helseFom = LocalDate.of(2025, 1, 1)
        val helseTom = LocalDate.of(2025, 5, 31)
        val gapFom = LocalDate.of(2025, 4, 1)
        val gapTom = LocalDate.of(2025, 5, 31)

        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(helseFom, helseTom)),
                soningsvurderinger = null,
                barnetillegg = listOf(
                    barnPeriode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31)),
                    barnPeriode(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31))
                ),
                helsevurderinger = null,
                rettighetsperiode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2028, 12, 31))
            )
        )

        assertThat(res.harBehovForAvklaring()).isTrue
        val avklaringssegmenterIGap = res.perioderTilVurdering.segmenter()
            .filter { it.periode.overlapper(Periode(gapFom, gapTom)) }
            .mapNotNull { it.verdi.helse?.vurdering }
        assertThat(avklaringssegmenterIGap).contains(OppholdVurdering.UAVKLART)
    }

    @Test
    fun `to sammenhengende barnetilleggperioder i helseopphold gir ikke falsk gap-avklaring`() {
        val helseFom = LocalDate.of(2025, 1, 1)
        val helseTom = LocalDate.of(2025, 5, 31)

        val res = utlederService.utledBehov(
            institusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(helseFom, helseTom)),
                soningsvurderinger = null,
                barnetillegg = listOf(
                    barnPeriode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31)),
                    barnPeriode(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 5, 31))
                ),
                helsevurderinger = null,
                rettighetsperiode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2028, 12, 31))
            )
        )

        assertThat(res.harBehovForAvklaring()).isFalse
    }

    // -------------------------------------------------------------------------
    // Ekte sammenhengende opphold (0 dagers gap) - kjedes for varighetskravet
    // -------------------------------------------------------------------------

    @Test
    fun `to korte sammenhengende opphold som til sammen ikke når 4 måneder gir ikke perioderSomTrengerVurdering`() {
        // Opphold 1 og 2 er ekte sammenhengende (0 dagers gap), men til sammen kun 2 måneder -
        // altfor kort til varighetskravet, og ingen nabo-kjede innen 3 mnd å smitte fra.
        val fom1 = LocalDate.now().minusMonths(6)
        val tom1 = fom1.plusMonths(1)
        val fom2 = tom1.plusDays(1) // 0 dagers gap -> ekte sammenhengende
        val tom2 = fom2.plusMonths(1)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "sykehus1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "sykehus2")),
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(fom1.minusYears(1), tom2.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isFalse
        assertThat(res.perioderTilVurdering.segmenter()).isEmpty()
    }

    @Test
    fun `to sammenhengende opphold som til sammen når 4 måneder gir avklaring selv om hvert er for kort alene`() {
        val fom1 = LocalDate.now().minusMonths(6)
        val tom1 = fom1.plusMonths(2)
        val fom2 = tom1.plusDays(1) // 0 dagers gap -> ekte sammenhengende
        val tom2 = fom2.plusMonths(2)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "sykehus1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "sykehus2")),
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(fom1.minusYears(1), tom2.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isTrue
    }

    // -------------------------------------------------------------------------
    // Fixpoint-propagering mellom ikke-sammenhengende kjeder (naboskap innen 3 mnd)
    // -------------------------------------------------------------------------

    @Test
    fun `kort isolert opphold med kun en annen kort ikke-kvalifisert nabo innen 3 mnd blir IKKE flagget`() {
        // To korte opphold (ikke sammenhengende, gap < 3 mnd),
        // ingen av dem er lange nok alene, og ingen kjede rundt dem er kvalifisert -> ingen propagering.
        val fom1 = LocalDate.now().minusMonths(5)
        val tom1 = fom1.plusMonths(2)
        val fom2 = tom1.plusMonths(1) // gap > 0 dager, men < 3 mnd
        val tom2 = fom2.plusMonths(2)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "sykehus1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "sykehus2")),
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(fom1.minusYears(1), tom2.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isFalse
        assertThat(res.perioderTilVurdering.segmenter()).isEmpty()
    }

    @Test
    fun `kvalifisert opphold smitter til kort nabo innen 3 mnd uavhengig av naboens egen varighet`() {
        // Opphold 1 er langt nok alene (kvalifisert). Opphold 2 er kort (1 mnd) og ikke sammenhengende
        // med opphold 1, men ligger innen 3 mnd -> skal smittes og flagges uavhengig av egen varighet.
        val fom1 = LocalDate.now().minusMonths(10)
        val tom1 = LocalDate.now().minusMonths(4)
        val fom2 = tom1.plusMonths(1) // innen 3 mnd, men reelt gap (ikke 0 dager)
        val tom2 = fom2.plusMonths(1)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "opphold1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "opphold2")),
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(fom1.minusYears(1), tom2.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.perioderTilVurdering.segmenter()).hasSize(2)
    }

    @Test
    fun `smitte propagerer transitivt over flere ikke-sammenhengende kjeder (kjede-effekt)`() {
        // opphold1 kvalifisert alene -> smitter opphold2 (innen 3mnd) -> smitter opphold3 (innen 3mnd fra opphold2)
        // selv om opphold3 IKKE er innen 3mnd fra opphold1 direkte.
        val fom1 = LocalDate.now().minusMonths(12)
        val tom1 = LocalDate.now().minusMonths(6)
        val fom2 = tom1.plusMonths(1)
        val tom2 = fom2.plusMonths(1)
        val fom3 = tom2.plusMonths(1)
        val tom3 = fom3.plusDays(20)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "opphold1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "opphold2")),
                Segment(Periode(fom3, tom3), Institusjon(Institusjonstype.HS, Oppholdstype.D, "789", "opphold3")),
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(fom1.minusYears(1), tom3.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.perioderTilVurdering.segmenter()).hasSize(3)
    }

    // -------------------------------------------------------------------------
    // Barnetillegg som splitter et fysisk sammenhengende opphold
    // -------------------------------------------------------------------------

    @Test
    fun `barnetillegg midt i et sammenhengende opphold gir ikke falsk gap-avklaring for noen av delene`() {
        // Ett fysisk sammenhengende institusjonsopphold, men barnetillegget dekker en kort periode
        // midt i oppholdet og splitter Boolean-tidslinjen i to deler uten barnetillegg.
        val oppholdFom = LocalDate.now().minusMonths(8)
        val oppholdTom = oppholdFom.plusMonths(5)
        val barnetilleggFom = oppholdFom.plusDays(32)
        val barnetilleggTom = oppholdFom.plusMonths(3)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(hsOpphold(oppholdFom, oppholdTom)),
            soningsvurderinger = null,
            barnetillegg = listOf(barnPeriode(barnetilleggFom, barnetilleggTom)),
            helsevurderinger = null,
            rettighetsperiode = Periode(oppholdFom.minusYears(1), oppholdTom.plusYears(2))
        )

        val res = utlederService.utledBehov(input)

        // Hele kjeden varer 5 mnd, lenger enn 4-måneders-kravet -> skal gi avklaring
        // på delene som ikke dekkes av barnetillegg, uavhengig av barnetillegg-splitten.
        assertThat(res.harBehovForAvklaring()).isTrue
        val periodeTilVurdering = res.perioderTilVurdering.segmenter().map { it.periode }
        assertThat(periodeTilVurdering).containsExactlyInAnyOrder(
            Periode(oppholdFom, barnetilleggFom.minusDays(1)),
            Periode(barnetilleggTom.plusDays(1), oppholdTom)
        )
        val vurderinger = res.perioderTilVurdering.segmenter().map { it.verdi.helse?.vurdering }
        assertThat(vurderinger).contains(OppholdVurdering.UAVKLART)
    }

    @Test
    fun `barnetillegg gjennom hele oppholdet gir ingen exception og ingen avklaringsbehov`() {
        // Regresjonstest for NPE/exception ved tom oppholdUtenBarnetillegg-tidslinje
        val fom = LocalDate.now().minusMonths(6)
        val tom = fom.plusMonths(5)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(hsOpphold(fom, tom)),
            soningsvurderinger = null,
            barnetillegg = listOf(barnPeriode(fom.minusDays(1), tom.plusDays(1))),
            helsevurderinger = null,
            rettighetsperiode = Periode(fom.minusYears(1), tom.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isFalse
    }

    // -------------------------------------------------------------------------
    // Kjede-basert matching av helsevurdering (helseoppholdPerioder bruker kjeder)
    // -------------------------------------------------------------------------

    @Test
    fun `vurdering som kun overlapper siste del av en sammenhengende kjede dekker hele kjeden`() {
        val fom1 = LocalDate.now().minusMonths(6)
        val tom1 = fom1.plusMonths(2)
        val fom2 = tom1.plusDays(1) // sammenhengende
        val tom2 = fom2.plusMonths(3)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "sykehus1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "sykehus2")),
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            // Vurderingen overlapper kun opphold 2 (den siste delen av kjeden)
            helsevurderinger = listOf(
                helsevurdering(
                    fom2, tom2,
                    faarFriKostOgLosji = true, forsoergerEktefelle = false, harFasteUtgifter = false
                )
            ),
            rettighetsperiode = Periode(fom1.minusYears(1), tom2.plusYears(2))
        )

        val res = utlederService.utledBehov(input)

        val vurderinger = res.perioderTilVurdering.segmenter().map { it.verdi.helse?.vurdering }
        assertThat(vurderinger).doesNotContain(OppholdVurdering.UAVKLART)
    }

    @Test
    fun `to opphold med reelt gap - ingen opphold til avklaring`() {
        val fom1 = LocalDate.now().minusMonths(8)
        val tom1 = fom1.plusMonths(2)
        val fom2 = tom1.plusMonths(4) // langt utenfor 3 mnd -> ikke sammenhengende, ikke naboeffekt
        val tom2 = fom2.plusMonths(2)

        val input = institusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "sykehus1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "sykehus2")),
            ),
            soningsvurderinger = null,
            barnetillegg = emptyList(),
            helsevurderinger = null,
            rettighetsperiode = Periode(fom1.minusYears(1), tom2.plusYears(2))
        )

        val res = utlederService.utledBehov(input)

        // Opphold1 skal IKKE ha noen vurdering/segment siden det verken er kvalifisert
        // alene eller smittet fra opphold2 (for langt unna)
        val opphold1Segmenter = res.perioderTilVurdering.segmenter().filter { it.periode.fom < fom2 }
        assertThat(opphold1Segmenter).isEmpty()
    }
}
