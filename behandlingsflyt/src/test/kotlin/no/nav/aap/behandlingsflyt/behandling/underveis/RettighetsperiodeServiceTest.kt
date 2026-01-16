package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

private val NÅ = LocalDate.now()

class RettighetsperiodeServiceTest {
    @Test
    fun `skal utlede riktig rettighetsperiode for arbeidssøker`() {
        val rettighetsperioder = RettighetsperiodeService().beregn(NÅ)
        val forventetPeriode = Periode(NÅ, NÅ.plusMonths(6))

        assertEquals(forventetPeriode, rettighetsperioder.arbeidssøkerPeriode)
    }

    @Test
    fun `skal utlede riktig rettighetsperiode for student`() {
        val rettighetsperioder = RettighetsperiodeService().beregn(NÅ)
        val forventetPeriode = Periode(NÅ, NÅ.plusMonths(6))

        assertEquals(forventetPeriode, rettighetsperioder.studentPeriode)
    }

    @Test
    fun `skal utlede riktig rettighetsperiode for overgang til uføre`() {
        val rettighetsperioder = RettighetsperiodeService().beregn(NÅ)
        val forventetPeriode = Periode(NÅ, NÅ.plusMonths(8))

        assertEquals(forventetPeriode, rettighetsperioder.overgangUførePeriode)
    }

    @Test
    fun `skal utlede riktig maksdato for rettighet arbeidssøker`() {
        val maksdatoForArbeidssøker = RettighetsperiodeService().utledMaksdatoForRettighet(RettighetsType.ARBEIDSSØKER, NÅ)
        val forventetMaksdato = NÅ.plusMonths(6)

        assertEquals(forventetMaksdato, maksdatoForArbeidssøker)
    }

    @Test
    fun `skal utlede riktig maksdato for rettighet student`() {
        val maksdatoForStudent = RettighetsperiodeService().utledMaksdatoForRettighet(RettighetsType.STUDENT, NÅ)
        val forventetMaksdato = NÅ.plusMonths(6)

        assertEquals(forventetMaksdato, maksdatoForStudent)
    }

    @Test
    fun `skal utlede riktig maksdato for rettighet overgang til uføre`() {
        val maksdatoForOvergangUføre = RettighetsperiodeService().utledMaksdatoForRettighet(RettighetsType.VURDERES_FOR_UFØRETRYGD, NÅ)
        val forventetMaksdato = NÅ.plusMonths(8)

        assertEquals(forventetMaksdato, maksdatoForOvergangUføre)
    }
}
