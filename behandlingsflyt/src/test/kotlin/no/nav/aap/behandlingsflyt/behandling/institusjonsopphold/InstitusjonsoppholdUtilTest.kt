package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InstitusjonsoppholdUtilTest {

    private fun opphold(fom: LocalDate, tom: LocalDate, navn: String = "Testsykehus") = Segment(
        Periode(fom, tom),
        Institusjon(Institusjonstype.HS, Oppholdstype.H, "111222333", navn)
    )

    private val kysthaven = opphold(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 9, 1), "Kysthaven")
    private val solglottInnenTreMåneder = opphold(LocalDate.of(2025, 11, 1), LocalDate.of(2026, 2, 1), "Solgløtt")
    private val solglottEtterTreMåneder = opphold(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 4, 1), "Solgløtt")

    private fun datoerSortert(
        opphold: List<Segment<Institusjon>>,
        harInnvilgetReduksjon: (SammenhengendeOppholdGruppe) -> Boolean = { false },
    ): List<LocalDate> =
        beregnTidligsteReduksjonsdatoPerKjede(opphold, harInnvilgetReduksjon)
            .entries.sortedBy { it.key.periode.fom }.map { it.value }

    // -------------------------------------------------------------------------
    // lagOppholdId
    // -------------------------------------------------------------------------

    @Test
    fun `lagOppholdId - kombinerer institusjonsnavn og fom`() {
        val id = lagOppholdId("Teststykehus", 15 januar 2026)
        assertThat(id).isEqualTo("Teststykehus::2026-01-15")
    }

    @Test
    fun `lagOppholdId - to like navn men ulik fom gir ulike ider`() {
        val id1 = lagOppholdId("Teststykehus", 1 januar 2026)
        val id2 = lagOppholdId("Teststykehus", 15 juni 2026)
        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `lagOppholdId - to ulike navn men lik fom gir ulike ider`() {
        val id1 = lagOppholdId("Sykehus A", 1 januar 2026)
        val id2 = lagOppholdId("Sykehus B", 1 januar 2026)
        assertThat(id1).isNotEqualTo(id2)
    }

    // -------------------------------------------------------------------------
    // beregnTidligsteReduksjonsdatoPerKjede - reduksjonsdatoer for kjeder hvor avstand mellom kjeder er kortere eller lengre enn 3 måneder
    // -------------------------------------------------------------------------

    @Test
    fun `første kjede får alltid fire måneder etter innleggelsesmåneden`() {
        val datoer = datoerSortert(listOf(kysthaven))

        assertThat(datoer).containsExactly(LocalDate.of(2025, 6, 1))
    }

    @Test
    fun `1-månedsregel når neste kjede starter innen tre måneder og forrige kjede har reduksjon`() {
        val datoer = datoerSortert(listOf(kysthaven, solglottInnenTreMåneder), harInnvilgetReduksjon = { true })

        assertThat(datoer).containsExactly(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 12, 1))
    }

    @Test
    fun `3-månedersregel når neste kjede starter innen tre måneder men forrige kjede ikke har reduksjon`() {
        val datoer = datoerSortert(listOf(kysthaven, solglottInnenTreMåneder), harInnvilgetReduksjon = { false })

        assertThat(datoer).containsExactly(LocalDate.of(2025, 6, 1), LocalDate.of(2026, 3, 1))
    }

    @Test
    fun `3-månedersregel når neste kjede starter mer enn tre måneder etter forrige selv med reduksjon`() {
        val datoer = datoerSortert(listOf(kysthaven, solglottEtterTreMåneder), harInnvilgetReduksjon = { true })

        assertThat(datoer).containsExactly(LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 1))
    }

    @Test
    fun `tom liste gir tomt resultat for beregnTidligsteReduksjonsdatoPerKjede`() {
        assertThat(beregnTidligsteReduksjonsdatoPerKjede(emptyList())).isEmpty()
    }

    @Test
    fun `nøyaktig tre måneder mellom kjedene regnes som innen tre måneder`() {
        val nesteStarterPåGrensen = opphold(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 2, 1), "Solgløtt")

        val datoer = datoerSortert(listOf(kysthaven, nesteStarterPåGrensen), harInnvilgetReduksjon = { true })

        assertThat(datoer).containsExactly(LocalDate.of(2025, 6, 1), LocalDate.of(2026, 1, 1))
    }

    @Test
    fun `en dag over tre måneder mellom kjedene gir normal ventetid`() {
        val nesteStarterEnDagForSent = opphold(LocalDate.of(2025, 12, 2), LocalDate.of(2026, 2, 1), "Solgløtt")

        val datoer = datoerSortert(listOf(kysthaven, nesteStarterEnDagForSent), harInnvilgetReduksjon = { true })

        assertThat(datoer).containsExactly(LocalDate.of(2025, 6, 1), LocalDate.of(2026, 4, 1))
    }

    @Test
    fun `harInnvilgetReduksjon vurderes per forrige kjede når det er tre kjeder`() {
        val a = opphold(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 1), "A")
        val b = opphold(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 5, 1), "B")
        val c = opphold(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 7, 1), "C")

        val datoer = datoerSortert(
            listOf(a, b, c),
            harInnvilgetReduksjon = { it.periode.fom == a.periode.fom }
        )

        // B får 1-månedsregel fordi A har reduksjon, C får 3-månedersregel fordi B ikke har det
        assertThat(datoer).containsExactly(
            LocalDate.of(2025, 6, 1),
            LocalDate.of(2025, 5, 1),
            LocalDate.of(2025, 10, 1),
        )
    }

    @Test
    fun `rekkefølgen på input har ikke betydning`() {
        val datoer = datoerSortert(listOf(solglottInnenTreMåneder, kysthaven), harInnvilgetReduksjon = { true })

        assertThat(datoer).containsExactly(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 12, 1))
    }

    @Test
    fun `opphold som starter dagen etter forrige slutter blir én kjede`() {
        val a = opphold(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 4, 30), "A")
        val b = opphold(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 9, 1), "B")

        assertThat(datoerSortert(listOf(a, b))).containsExactly(LocalDate.of(2025, 6, 1))
    }

    @Test
    fun `opphold med én dags mellomrom blir to kjeder`() {
        val a = opphold(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 4, 30), "A")
        val b = opphold(LocalDate.of(2025, 5, 2), LocalDate.of(2025, 9, 1), "B")

        // Forrige kjede har ikke reduksjon, så B får 3-månedersregel
        assertThat(datoerSortert(listOf(a, b))).containsExactly(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 9, 1))
    }

    @Test
    fun `innleggelse første og siste dag i måneden gir samme reduksjonsdato`() {
        val førsteDag = datoerSortert(listOf(opphold(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 9, 1))))
        val sisteDag = datoerSortert(listOf(opphold(LocalDate.of(2025, 3, 31), LocalDate.of(2025, 9, 1))))

        assertThat(førsteDag).containsExactly(LocalDate.of(2025, 7, 1))
        assertThat(sisteDag).containsExactly(LocalDate.of(2025, 7, 1))
    }
}