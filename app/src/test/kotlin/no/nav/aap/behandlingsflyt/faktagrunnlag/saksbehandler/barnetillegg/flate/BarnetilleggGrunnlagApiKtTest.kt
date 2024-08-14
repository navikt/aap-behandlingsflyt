package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.flate

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.BarnVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.ManuelleBarnService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.ManuelleBarnVurdeirng
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.ManuellebarnVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.ManueltBarnVurdeirng
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Personinfo
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

/*

class BarnetilleggGrunnlagApiKtTest {

    val manuellebarnVurderingRepository: ManuellebarnVurderingRepository = mockk()
    val barnRepository: BarnRepository = mockk()
    val pdlMock = mockk<PersoninfoGateway>()

    val manuelleBarnService = ManuelleBarnService(manuellebarnVurderingRepository, barnRepository, pdlMock)

    @Nested
    inner class barnetilleggApi() {


        @Test
        fun `folkeregisterrte barn blir korrekt konvertert`() {
            val barnPeriode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
            val barn = mockk<Barn>(relaxed = true)
            every { barn.periodeMedRettTil() } returns barnPeriode

            every { barnRepository.hent(any()) } returns BarnGrunnlag(listOf(barn))

            every { pdlMock.hentPersoninfoForIdent(any(), any()) } returns
                    Personinfo(Ident("1"), "Kåre", "", "Klut")

            every { manuellebarnVurderingRepository.hentHvisEksisterer(any()) } returns null

            val actual = manuelleBarnService.samleManuelleBarnGrunnlag(mockk(), mockk())

            Assertions.assertThat(actual).isEqualTo(
                BarnetilleggGrunnlagDto(
                    folkeregistrerteBarn = listOf(FolkeregistrertBarnDto("Kåre Klut", Ident("1"), barnPeriode)),
                    vurdering = null,
                    manueltOppgitteBarn = listOf(
                        ManueltBarnDto("Pelle Potet", Ident("12345678912")),
                        ManueltBarnDto("Kåre Kålrabi", Ident("12121212121"))
                    )
                )
            )
        }

        @Test
        fun `manuelle barn blir korrekt konvertert`() {
            every { barnRepository.hent(any()) } returns BarnGrunnlag(emptyList())

            every { manuellebarnVurderingRepository.hentHvisEksisterer(any()) } returns null

            val actual = manuelleBarnService.samleManuelleBarnGrunnlag(mockk(), mockk())

            Assertions.assertThat(actual).isEqualTo(
                BarnetilleggGrunnlagDto(
                    folkeregistrerteBarn = emptyList(),
                    vurdering = null,
                    manueltOppgitteBarn = listOf(
                        ManueltBarnDto("Pelle Potet", Ident("12345678912")),
                        ManueltBarnDto("Kåre Kålrabi", Ident("12121212121"))
                    )
                )
            )
        }

        @Test
        fun `vurdering blir korrekt konvertert`() {
            every { barnRepository.hent(any()) } returns BarnGrunnlag(emptyList())


            every { manuellebarnVurderingRepository.hentHvisEksisterer(any()) } returns BarnVurderingGrunnlag(
                vurdering = ManuelleBarnVurdeirng(
                    barn = setOf(
                        ManueltBarnVurdeirng(
                            ident = Ident("2"),
                            begrunnelse = "begrunnelse",
                            skalBeregnesBarnetillegg = false,
                            perioder = emptyList()
                        )
                    )
                )
            )

            val actual = manuelleBarnService.samleManuelleBarnGrunnlag(mockk(), mockk())

            Assertions.assertThat(actual).isEqualTo(
                BarnetilleggGrunnlagDto(
                    folkeregistrerteBarn = emptyList(),
                    vurdering = ManuelleBarnVurderingDto(
                        listOf(
                            ManueltBarnVurderingDto(
                                ident = "2",
                                begrunnelse = "begrunnelse",
                                skalBeregnesBarnetillegg = false,
                                forsørgeransvarPerioder = emptyList()
                            )
                        )
                    ),
                    manueltOppgitteBarn = listOf(
                        ManueltBarnDto("Pelle Potet", Ident("12345678912")),
                        ManueltBarnDto("Kåre Kålrabi", Ident("12121212121"))
                    )
                )
            )
        }
    }
}
*/
