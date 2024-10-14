package no.nav.aap.behandlingsflyt.behandling.etannetsted


import no.nav.aap.behandlingsflyt.dbtestdata.MockConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertFalse

class EtAnnetStedUtlederServiceTest {
    val utlederService = EtAnnetStedUtlederService(MockConnection().toDBConnection())

    @Test
    fun `Oppholder seg på inst mellom søknads tidspunkt og behandlingstidspunkt`() {
        val input = EtAnnetStedInput(
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
            emptyList()
        )

        val res = utlederService.utledBehov(input)
        assert(res.harBehov())
    }

    @Test
    fun `Delvis overlappende barnetillegg trenger avklaring`() {
        val input = EtAnnetStedInput(
            institusjonsOpphold = listOf(
                Segment(
                    Periode(
                        LocalDate.now().minusMonths(5),
                        LocalDate.now()
                    ),
                    Institusjon(
                        Institusjonstype.HS,
                        Oppholdstype.D,
                        "123",
                        "test"
                    )
                )
            ),
            listOf(
                BarnetilleggPeriode(
                    Periode(
                        LocalDate.now().minusMonths(5).minusDays(1),
                        LocalDate.now().minusMonths(4)
                    ),
                    setOf(
                        Ident("123")
                    )
                )
            )
        )

        val res = utlederService.utledBehov(input)
        assert(res.harBehov())
    }


    @Test
    fun `Delvis overlappende barnetillegg trenger ikke avklaring`() {
        val input = EtAnnetStedInput(
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
            listOf(
                BarnetilleggPeriode(
                    Periode(
                        LocalDate.now().minusMonths(5).minusDays(1),
                        LocalDate.now().minusMonths(2)
                    ),
                    setOf(
                        Ident("123")
                    )
                )
            )
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehov()).isFalse
    }

    @Test
    fun `ingen opphold trengs ingen avklaring`() {
        val input = EtAnnetStedInput(
            institusjonsOpphold = emptyList(),
            emptyList()
        )

        val res = utlederService.utledBehov(input)
        assertFalse(res.harBehov())
    }

    @Test
    fun `barnetillegg gjennom hele periodern gir ingen avklaring`() {
        val input = EtAnnetStedInput(
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
            listOf(
                BarnetilleggPeriode(
                    Periode(
                        LocalDate.now().minusMonths(6),
                        LocalDate.now()
                    ),
                    setOf(
                        Ident("123")
                    )
                )
            )
        )

        val res = utlederService.utledBehov(input)
        assertFalse(res.harBehov())
    }
}