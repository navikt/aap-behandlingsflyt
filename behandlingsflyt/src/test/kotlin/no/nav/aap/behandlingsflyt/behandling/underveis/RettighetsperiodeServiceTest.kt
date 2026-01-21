package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import java.time.Clock
import java.time.LocalDate

private val dagensDato = LocalDate.now(Clock.systemDefaultZone())

class RettighetsperiodeServiceTest {
    @Test
    fun `skal utlede riktig rettighetsperiode for arbeidssøker`() {
        val rettighetsperioder = RettighetsperiodeService().beregn(dagensDato)
        val forventetPeriode = Periode(dagensDato, dagensDato.plusMonths(6))

        assertThat(rettighetsperioder.arbeidssøkerPeriode).isEqualTo(forventetPeriode)
    }

    @Test
    fun `skal utlede riktig rettighetsperiode for student`() {
        val rettighetsperioder = RettighetsperiodeService().beregn(dagensDato)
        val forventetPeriode = Periode(dagensDato, dagensDato.plusMonths(6))

        assertThat(rettighetsperioder.studentPeriode).isEqualTo(forventetPeriode)
    }

    @Test
    fun `skal utlede riktig rettighetsperiode for overgang til uføre`() {
        val rettighetsperioder = RettighetsperiodeService().beregn(dagensDato)
        val forventetPeriode = Periode(dagensDato, dagensDato.plusMonths(8))

        assertThat(rettighetsperioder.overgangUførePeriode).isEqualTo(forventetPeriode)
    }

    @Test
    fun `skal utlede riktig maksdato for rettighet arbeidssøker`() {
        val maksdatoForArbeidssøker = RettighetsperiodeService().utledMaksdatoForRettighet(RettighetsType.ARBEIDSSØKER, dagensDato)
        val forventetMaksdato = dagensDato.plusMonths(6)

        assertThat(maksdatoForArbeidssøker).isEqualTo(forventetMaksdato)
    }

    @Test
    fun `skal utlede riktig maksdato for rettighet student`() {
        val maksdatoForStudent = RettighetsperiodeService().utledMaksdatoForRettighet(RettighetsType.STUDENT, dagensDato)
        val forventetMaksdato = dagensDato.plusMonths(6)

        assertThat(maksdatoForStudent).isEqualTo(forventetMaksdato)
    }

    @Test
    fun `skal utlede riktig maksdato for rettighet overgang til uføre`() {
        val maksdatoForOvergangUføre = RettighetsperiodeService().utledMaksdatoForRettighet(RettighetsType.VURDERES_FOR_UFØRETRYGD, dagensDato)
        val forventetMaksdato = dagensDato.plusMonths(8)

        assertThat(maksdatoForOvergangUføre).isEqualTo(forventetMaksdato)
    }
}
