package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.OppholdVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBarnetilleggRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryInstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class InstitusjonsoppholdUtlederServiceTest {

    val utlederService = InstitusjonsoppholdUtlederService(
        InMemoryBarnetilleggRepository,
        InMemoryInstitusjonsoppholdRepository,
        InMemorySakRepository,
        InMemoryBehandlingRepository
    )

    @BeforeEach
    fun reset() {
        InMemoryBarnetilleggRepository.reset()
        InMemoryInstitusjonsoppholdRepository.reset()
    }

    // --- Hjelpefunksjoner ---

    private fun hsOpphold(fom: LocalDate, tom: LocalDate) = Segment(
        Periode(fom, tom),
        Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "Helgelandssykehuset")
    )

    private fun foOpphold(fom: LocalDate, tom: LocalDate) = Segment(
        Periode(fom, tom),
        Institusjon(Institusjonstype.FO, Oppholdstype.S, "456", "fengsel")
    )

    private fun hsInput(fom: LocalDate, tom: LocalDate) = InstitusjonsoppholdInput(
        institusjonsOpphold = listOf(
            Segment(
                Periode(fom, tom),
                Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "Helgelandssykehuset")
            )
        ),
        soningsvurderinger = emptyList(),
        barnetillegg = emptyList(),
        helsevurderinger = emptyList(),
        rettighetsperiode = Periode(fom.minusYears(1), tom.plusYears(2))
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
        vurdertAv = "ident",
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = emptyList(),
                soningsvurderinger = emptyList(),
                barnetillegg = emptyList(),
                helsevurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = emptyList(),
                barnetillegg = emptyList(),
                helsevurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = emptyList(),
                barnetillegg = emptyList(),
                helsevurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = emptyList(),
                barnetillegg = emptyList(),
                helsevurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = emptyList(),
                barnetillegg = emptyList(),
                helsevurderinger = listOf(
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = emptyList(),
                barnetillegg = emptyList(),
                helsevurderinger = listOf(
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = emptyList(),
                barnetillegg = emptyList(),
                helsevurderinger = listOf(
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(oppholdFom, tom)),
                soningsvurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = emptyList(),
                barnetillegg = emptyList(),
                helsevurderinger = emptyList(),
                rettighetsperiode = rettighetsperiode()
            )
        )
        assertThat(res.harBehovForAvklaring()).isTrue
        val helseVurdering = res.perioderTilVurdering.segmenter().first().verdi.helse
        assertThat(helseVurdering?.vurdering).isEqualTo(OppholdVurdering.UAVKLART)
    }

    // --- Barnetillegg ---

    @Test
    fun `barnetillegg gjennom hele oppholdet fjerner avklaringsbehov`() {
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = emptyList(),
                barnetillegg = listOf(barnPeriode(fom.minusDays(1), tom.plusDays(1))),
                helsevurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(fom, tom)),
                soningsvurderinger = emptyList(),
                barnetillegg = listOf(barnPeriode(fom.minusDays(1), fom.plusMonths(1))),
                helsevurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(
                    hsOpphold(forsteOppholdFom, forsteOppholdTom),
                    hsOpphold(andreOppholdFom, andreOppholdTom),
                ),
                soningsvurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(
                    hsOpphold(forsteOppholdFom, forsteOppholdTom),
                    hsOpphold(andreOppholdFom, andreOppholdTom),
                ),
                soningsvurderinger = emptyList(),
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
    fun `soningsopphold uten vurdering gir avklaring`() {
        val fom = LocalDate.now().minusMonths(3)
        val tom = LocalDate.now().minusMonths(1)
        val res = utlederService.utledBehov(
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(foOpphold(fom, tom)),
                soningsvurderinger = emptyList(),
                barnetillegg = emptyList(),
                helsevurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(foOpphold(fom, tom)),
                soningsvurderinger = listOf(
                    Soningsvurdering(skalOpphøre = true, begrunnelse = "soner", fraDato = fom)
                ),
                barnetillegg = emptyList(),
                helsevurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(foOpphold(fom, tom)),
                soningsvurderinger = listOf(
                    Soningsvurdering(skalOpphøre = false, begrunnelse = "frigang", fraDato = fom)
                ),
                barnetillegg = emptyList(),
                helsevurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(foOpphold(fom, tom)),
                soningsvurderinger = listOf(
                    Soningsvurdering(skalOpphøre = true, begrunnelse = "soner", fraDato = fom),
                    Soningsvurdering(skalOpphøre = false, begrunnelse = "frigang", fraDato = fom.plusWeeks(2))
                ),
                barnetillegg = emptyList(),
                helsevurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(oppholdFom, oppholdTom)),
                soningsvurderinger = emptyList(),
                barnetillegg = emptyList(),
                helsevurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(hsOpphold(oppholdFom, oppholdTom)),
                soningsvurderinger = emptyList(),
                barnetillegg = emptyList(),
                helsevurderinger = emptyList(),
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
            InstitusjonsoppholdInput(
                institusjonsOpphold = listOf(
                    foOpphold(fom, tom),
                    hsOpphold(fom, tom),
                ),
                soningsvurderinger = listOf(
                    Soningsvurdering(skalOpphøre = true, begrunnelse = "soner", fraDato = fom)
                ),
                barnetillegg = emptyList(),
                helsevurderinger = emptyList(),
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

        val input = InstitusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "opphold1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "opphold2")),
            ),
            soningsvurderinger = emptyList(),
            barnetillegg = emptyList(),
            helsevurderinger = emptyList(),
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

        val input = InstitusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "opphold1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "opphold2")),
            ),
            soningsvurderinger = emptyList(),
            barnetillegg = emptyList(),
            helsevurderinger = emptyList(),
            rettighetsperiode = Periode(fom1.minusYears(1), tom2.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        // Første gir avklaring, andre er for kort og for langt unna
        assertThat(res.perioderTilVurdering.segmenter()).hasSize(1)
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

        val input = InstitusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "opphold1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "opphold2")),
                Segment(Periode(fom3, tom3), Institusjon(Institusjonstype.HS, Oppholdstype.D, "789", "opphold3")),
            ),
            soningsvurderinger = emptyList(),
            barnetillegg = emptyList(),
            helsevurderinger = emptyList(),
            rettighetsperiode = Periode(fom1.minusYears(1), tom3.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.perioderTilVurdering.segmenter()).hasSize(3)
    }

    @Test
    fun `nøyaktig 3 måneder mellom opphold - andre opphold er ikke innenfor grensen`() {
        // Betingelsen er segment.fom.isBefore(forrigeTom.plusMonths(3))
        // Hvis fom2 == forrigeTom + 3 mnd er det IKKE før, dvs. ikke innenfor
        val fom1 = LocalDate.now().minusMonths(10)
        val tom1 = LocalDate.now().minusMonths(5)
        val fom2 = tom1.plusMonths(3)            // nøyaktig 3 mnd etter → ikke innenfor
        val tom2 = fom2.plusMonths(1)

        val input = InstitusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "opphold1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "opphold2")),
            ),
            soningsvurderinger = emptyList(),
            barnetillegg = emptyList(),
            helsevurderinger = emptyList(),
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

        val input = InstitusjonsoppholdInput(
            institusjonsOpphold = listOf(
                Segment(Periode(fom1, tom1), Institusjon(Institusjonstype.HS, Oppholdstype.D, "123", "opphold1")),
                Segment(Periode(fom2, tom2), Institusjon(Institusjonstype.HS, Oppholdstype.D, "456", "opphold2")),
            ),
            soningsvurderinger = emptyList(),
            barnetillegg = emptyList(),
            helsevurderinger = emptyList(),
            rettighetsperiode = Periode(fom1.minusYears(1), tom2.plusYears(2))
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.perioderTilVurdering.segmenter()).hasSize(2)
    }
}