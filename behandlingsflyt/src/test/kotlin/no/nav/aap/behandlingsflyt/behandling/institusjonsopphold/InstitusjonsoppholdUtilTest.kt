package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InstitusjonsoppholdUtilTest {

    private fun opphold(fom: LocalDate, tom: LocalDate) = Segment(
        Periode(fom, tom),
        Institusjon(Institusjonstype.HS, Oppholdstype.H, "111222333", "Teststykehus")
    )

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
    // beregnTidligsteReduksjonsdatoPerOpphold - tom liste
    // -------------------------------------------------------------------------

    @Test
    fun `tom liste gir tomt resultat`() {
        val resultat = beregnTidligsteReduksjonsdatoPerOpphold(emptyList())
        assertThat(resultat).isEmpty()
    }

    // -------------------------------------------------------------------------
    // beregnTidligsteReduksjonsdatoPerOpphold - ett opphold
    // -------------------------------------------------------------------------

    @Test
    fun `ett opphold - tidligste reduksjonsdato er første dag i innleggelsesmåned pluss 4 måneder`() {
        // Innlagt 15. januar → tidligste reduksjonsdato = 1. mai (1. jan + 4 mnd)
        val opphold = opphold(15 januar 2026, 31 desember 2026)

        val resultat = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold))

        assertThat(resultat).hasSize(1)
        assertThat(resultat[opphold]).isEqualTo(1 mai 2026)
    }

    @Test
    fun `ett opphold - innlagt første i måneden gir reduksjon fra første i måned 5`() {
        // Innlagt 1. mars → tidligste reduksjonsdato = 1. juli
        val opphold = opphold(1 mars 2026, 31 desember 2026)

        val resultat = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold))

        assertThat(resultat[opphold]).isEqualTo(1 juli 2026)
    }

    @Test
    fun `ett opphold - innlagt siste dag i måneden gir reduksjon fra første i måned 5`() {
        // Innlagt 31. januar → withDayOfMonth(1) = 1. jan → + 4 mnd = 1. mai
        val opphold = opphold(1 januar 2026, 31 desember 2026)

        val resultat = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold))

        assertThat(resultat[opphold]).isEqualTo(1 mai 2026)
    }

    // -------------------------------------------------------------------------
    // beregnTidligsteReduksjonsdatoPerOpphold - to opphold, kort mellomrom (≤ 3 måneder)
    // -------------------------------------------------------------------------

    @Test
    fun `to opphold - andre opphold starter dagen etter forrige slutter, reduksjon fra innleggelsesdato`() {
        // Opphold 1: jan–mars. Opphold 2: starter 1. april (dagen etter).
        val opphold1 = opphold(1 januar 2026, 31 mars 2026)
        val opphold2 = opphold(1 april 2026, 30 september 2026)

        val resultat = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold1, opphold2))

        assertThat(resultat[opphold1]).isEqualTo(1 mai 2026) // 1. jan + 4 mnd
        assertThat(resultat[opphold2]).isEqualTo(1 april 2026) // umiddelbar, innen 3 mnd
    }

    @Test
    fun `to opphold - nøyaktig 3 måneder mellom opphold regnes som innen 3 måneder`() {
        // Opphold 1 slutter 31. januar. Opphold 2 starter 30. april (= 31. jan + 3 mnd - 1 dag).
        // treMånederEtterForrigeUtskrivelse = 31. jan + 3 mnd = 30. april
        // erInnenTreMåneder = !startDato.isAfter(30. april) = !false = true
        val opphold1 = opphold(1 januar 2026, 31 januar 2026)
        val opphold2 = opphold(30 april 2026, 31 oktober 2026)

        val resultat = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold1, opphold2))

        assertThat(resultat[opphold2]).isEqualTo(30 april 2026) // umiddelbar
    }

    @Test
    fun `to opphold - 2 måneder mellom opphold gir umiddelbar reduksjon på andre opphold`() {
        val opphold1 = opphold(1 januar 2026, 28 februar 2026)
        val opphold2 = opphold(15 april 2026, 31 oktober 2026)

        val resultat = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold1, opphold2))

        assertThat(resultat[opphold2]).isEqualTo(15 april 2026) // umiddelbar
    }

    // -------------------------------------------------------------------------
    // beregnTidligsteReduksjonsdatoPerOpphold - to opphold, langt mellomrom (> 3 måneder)
    // -------------------------------------------------------------------------

    @Test
    fun `to opphold - mer enn 3 måneder mellom opphold gir normal ventetid på andre opphold`() {
        // Opphold 1 slutter 31. januar. Opphold 2 starter 1. juni (= > 3 mnd etter).
        val opphold1 = opphold(1 januar 2026, 31 januar 2026)
        val opphold2 = opphold(1 juni 2026, 31 desember 2026)

        val resultat = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold1, opphold2))

        assertThat(resultat[opphold1]).isEqualTo(1 mai 2026)  // 1. jan + 4 mnd
        assertThat(resultat[opphold2]).isEqualTo(1 oktober 2026) // 1. jun + 4 mnd
    }

    @Test
    fun `to opphold - nøyaktig 1 dag over 3 måneder mellom opphold gir normal ventetid`() {
        // Opphold 1 slutter 31. januar. treMånederEtter = 30. april. Opphold 2 starter 1. mai.
        val opphold1 = opphold(1 januar 2026, 31 januar 2026)
        val opphold2 = opphold(1 mai 2026, 31 desember 2026)

        val resultat = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold1, opphold2))

        assertThat(resultat[opphold2]).isEqualTo(1 september 2026) // 1. mai + 4 mnd
    }

    // -------------------------------------------------------------------------
    // beregnTidligsteReduksjonsdatoPerOpphold - fire opphold, kombinasjoner
    // -------------------------------------------------------------------------

    @Test
    fun `fire opphold - kort, langt, kort mellomrom mellom seg`() {
        // Opphold 1: jan–feb 2026
        // Opphold 2: april 2026 (innen 3 mnd fra feb → umiddelbar)
        // Opphold 3: des 2026   (mer enn 3 mnd fra aug → normal ventetid)
        // Opphold 4: jan 2027   (innen 3 mnd fra des → umiddelbar)
        val opphold1 = opphold(1 januar 2026,  28 februar 2026)
        val opphold2 = opphold(1 april 2026,  31 august 2026)
        val opphold3 = opphold(1 desember 2026, 31 desember 2026)
        val opphold4 = opphold(15 januar 2027, 30 juni 2027)

        val resultat = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold1, opphold2, opphold3, opphold4))

        assertThat(resultat[opphold1]).isEqualTo(1 mai 2026)   // 1. jan + 4 mnd
        assertThat(resultat[opphold2]).isEqualTo(1 april 2026)   // umiddelbar (innen 3 mnd fra feb)
        assertThat(resultat[opphold3]).isEqualTo(1 april 2027)   // 1. des + 4 mnd (mer enn 3 mnd fra aug)
        assertThat(resultat[opphold4]).isEqualTo(15 januar 2027)  // umiddelbar (innen 3 mnd fra des)
    }

    @Test
    fun `fire opphold - alle med langt mellomrom mellom seg gir normal ventetid for alle`() {
        val opphold1 = opphold(1 januar 2026,  31 januar 2026)
        val opphold2 = opphold(1 juni 2026,  30 juni 2026)
        val opphold3 = opphold(1 november 2026, 30 november 2026)
        val opphold4 = opphold(1 april 2027,  30 april 2027)

        val resultat = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold1, opphold2, opphold3, opphold4))

        assertThat(resultat[opphold1]).isEqualTo(1 mai 2026)   // 1. jan + 4 mnd
        assertThat(resultat[opphold2]).isEqualTo(1 oktober 2026)  // 1. jun + 4 mnd
        assertThat(resultat[opphold3]).isEqualTo(1 mars 2027)   // 1. nov + 4 mnd
        assertThat(resultat[opphold4]).isEqualTo(1 august 2027)   // 1. apr + 4 mnd
    }

    @Test
    fun `fire opphold - alle med kort mellomrom mellom seg gir umiddelbar reduksjon fra opphold 2`() {
        val opphold1 = opphold(1 januar 2026,  28 februar 2026)
        val opphold2 = opphold(1 april 2026,  31 mai 2026)
        val opphold3 = opphold(1 juli 2026,  31 august 2026)
        val opphold4 = opphold(1 oktober 2026, 31 desember 2026)

        val resultat = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold1, opphold2, opphold3, opphold4))

        assertThat(resultat[opphold1]).isEqualTo(1 mai 2026)  // 1. jan + 4 mnd
        assertThat(resultat[opphold2]).isEqualTo(1 april 2026)  // umiddelbar
        assertThat(resultat[opphold3]).isEqualTo(1 juli 2026)  // umiddelbar
        assertThat(resultat[opphold4]).isEqualTo(1 oktober 2026) // umiddelbar
    }

    @Test
    fun `fire opphold - langt mellomrom kun mellom opphold 2 og 3, ellers korte`() {
        // Opphold 1: jan–feb  → normal
        // Opphold 2: apr      → umiddelbar (innen 3 mnd fra feb)
        // Opphold 3: nov      → normal (mer enn 3 mnd fra mai)
        // Opphold 4: jan 2027 → umiddelbar (innen 3 mnd fra nov)
        val opphold1 = opphold(1 januar 2026,  28 februar 2026)
        val opphold2 = opphold(1 april 2026,  31 mai 2026)
        val opphold3 = opphold(1 november 2026, 30 november 2026)
        val opphold4 = opphold(1 januar 2027,  31 mars 2027)

        val resultat = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold1, opphold2, opphold3, opphold4))

        assertThat(resultat[opphold1]).isEqualTo(1 mai 2026)   // 1. jan + 4 mnd
        assertThat(resultat[opphold2]).isEqualTo(1 april 2026)   // umiddelbar
        assertThat(resultat[opphold3]).isEqualTo(1 mars 2027)   // 1. nov + 4 mnd
        assertThat(resultat[opphold4]).isEqualTo(1 januar 2027)   // umiddelbar
    }

    @Test
    fun `rekkefølge på input har ikke noe å si - usortert input gir samme resultat`() {
        val opphold1 = opphold(1 januar 2026, 31 januar 2026)
        val opphold2 = opphold(1 juni 2026, 30 juni 2026)

        val sortert   = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold1, opphold2))
        val usortert  = beregnTidligsteReduksjonsdatoPerOpphold(listOf(opphold2, opphold1))

        assertThat(sortert[opphold1]).isEqualTo(usortert[opphold1])
        assertThat(sortert[opphold2]).isEqualTo(usortert[opphold2])
    }
}