package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentEnkelLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plusHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurderingDTO
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StudentFlytTest : AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `innvilge som student, revurdering ordinær`() {
        val fom = 24 november 2025
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
            søknad = TestSøknader.SØKNAD_STUDENT
        )

        behandling = behandling
            .løsAvklaringsBehov(
                AvklarStudentEnkelLøsning(
                    studentvurdering = StudentVurderingDTO(
                        begrunnelse = "...",
                        harAvbruttStudie = true,
                        godkjentStudieAvLånekassen = true,
                        avbruttPgaSykdomEllerSkade = true,
                        harBehovForBehandling = true,
                        avbruttStudieDato = fom,
                        avbruddMerEnn6Måneder = true
                    ),
                )
            )
            .løsRefusjonskrav()
            .løsForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                val vilkår = dataSource.transaction { VilkårsresultatRepositoryImpl(it).hent(behandling.id) }
                val v = vilkår.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
                assertThat(v.harPerioderSomErOppfylt()).isTrue
            }
            .assertRettighetstype(
                Periode(fom, fom.plusHverdager(Hverdager(130)).minusDays(1)) to RettighetsType.STUDENT,
            )

        // Revurdering
        var revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_STUDENT),
        )
            .løsAvklaringsBehov(
                AvklarStudentEnkelLøsning(
                    studentvurdering = StudentVurderingDTO(
                        begrunnelse = "...",
                        harAvbruttStudie = false,
                        godkjentStudieAvLånekassen = null,
                        avbruttPgaSykdomEllerSkade = null,
                        harBehovForBehandling = null,
                        avbruttStudieDato = null,
                        avbruddMerEnn6Måneder = null,
                    )
                )
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs { "Skal vurderes for ordinær dersom ikke oppfylt student" }
                    .containsExactlyInAnyOrder(Definisjon.AVKLAR_SYKDOM)
            }
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand(sak.rettighetsperiode.fom)
            .løsSykdomsvurderingBrev()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FATTE_VEDTAK)
            }
            .fattVedtak()
            .medKontekst {
                val vilkår = dataSource.transaction { VilkårsresultatRepositoryImpl(it).hent(behandling.id) }
                val v = vilkår.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
                assertThat(v.harPerioderSomErOppfylt()).isTrue
            }
            .assertRettighetstype(
                Periode(fom, fom.plusHverdager(Hverdager(130)).minusDays(1)) to RettighetsType.BISTANDSBEHOV,
            )

    }

}