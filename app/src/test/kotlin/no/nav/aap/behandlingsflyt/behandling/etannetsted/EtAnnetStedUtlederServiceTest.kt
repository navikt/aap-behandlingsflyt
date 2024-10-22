package no.nav.aap.behandlingsflyt.behandling.etannetsted


import no.nav.aap.behandlingsflyt.dbtestdata.MockConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EtAnnetStedUtlederServiceTest {
    val connection = MockConnection().toDBConnection()
    val utlederService = EtAnnetStedUtlederService(
        BarnetilleggRepository(connection),
        InstitusjonsoppholdRepository(connection)
    )

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
            soningsvurderinger = emptyList(),
            barnetillegg = emptyList(),
            helsevurderinger = emptyList()
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isTrue
    }

    @Test
    fun `soner noe, det krever avklaring`() {
        val soningsstart = LocalDate.now().minusMonths(5)
        val input = EtAnnetStedInput(
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
            soningsvurderinger = emptyList(),
            barnetillegg = emptyList(),
            helsevurderinger = emptyList()
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isTrue
    }

    @Test
    fun `soner noe, det krever avklaring men ikke etter at det har blitt vurdert`() {
        val soningsstart = LocalDate.now().minusMonths(5)
        val input = EtAnnetStedInput(
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
            helsevurderinger = emptyList()
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isFalse
    }

    @Test
    fun `Delvis overlappende barnetillegg trenger avklaring`() {
        val input = EtAnnetStedInput(
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
            soningsvurderinger = emptyList(),
            barnetillegg = listOf(
                BarnetilleggPeriode(
                    Periode(
                        LocalDate.now().minusMonths(5).minusDays(1),
                        LocalDate.now().minusMonths(4)
                    ),
                    setOf(
                        Ident("123")
                    )
                )
            ),
            helsevurderinger = emptyList()
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isTrue
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
            soningsvurderinger = emptyList(),
            barnetillegg = listOf(
                BarnetilleggPeriode(
                    Periode(
                        LocalDate.now().minusMonths(5).minusDays(1),
                        LocalDate.now().minusMonths(2)
                    ),
                    setOf(
                        Ident("123")
                    )
                )
            ),
            helsevurderinger = emptyList()
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isFalse
    }

    @Test
    fun `Opphold mindre enn 3 måneder etter forrige trigger ikke behov før et opphold trigger reduksjon`() {
        val innleggelsesperiode = Periode(
            LocalDate.now().minusMonths(12),
            LocalDate.now().minusMonths(5)
        )
        val input = EtAnnetStedInput(
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
            soningsvurderinger = emptyList(),
            barnetillegg = emptyList(),
            helsevurderinger = listOf(
                HelseinstitusjonVurdering(
                    periode = innleggelsesperiode,
                    begrunnelse = "lagt inn med kost og losji",
                    faarFriKostOgLosji = true,
                    forsoergerEktefelle = false,
                    harFasteUtgifter = false
                )
            )
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isTrue

        assertThat(res.perioderTilVurdering.segmenter()).hasSize(2)
    }

    @Test
    fun `ingen opphold trengs ingen avklaring`() {
        val input = EtAnnetStedInput(
            institusjonsOpphold = emptyList(),
            soningsvurderinger = emptyList(),
            barnetillegg = emptyList(),
            helsevurderinger = emptyList()
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isFalse
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
            soningsvurderinger = emptyList(),
            barnetillegg = listOf(
                BarnetilleggPeriode(
                    Periode(
                        LocalDate.now().minusMonths(6),
                        LocalDate.now()
                    ),
                    setOf(
                        Ident("123")
                    )
                )
            ),
            helsevurderinger = emptyList()
        )

        val res = utlederService.utledBehov(input)
        assertThat(res.harBehovForAvklaring()).isFalse
    }
}