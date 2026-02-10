package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentEnkelLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurderingDTO
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass

@ParameterizedClass
@MethodSource("unleashTestDataSource")
class StudentFlytTest(val unleashGateway: KClass<UnleashGateway>) : AbstraktFlytOrkestratorTest(unleashGateway) {
    @Test
    fun `innvilge som student, revurdering ordinær`() {
        val fom = 24 november 2025
        val periode = Periode(fom, fom.plusYears(3))

        val sykestipendPeriode = Periode(fom, fom.plusDays(14))

        val person = TestPersoner.STANDARD_PERSON()

        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
            søknad = TestSøknader.SØKNAD_STUDENT
        )

        val avbruttStudieDato = 1 oktober 2025

        behandling = behandling
            .løsAvklaringsBehov(
                AvklarStudentEnkelLøsning(
                    studentvurdering = StudentVurderingDTO(
                        begrunnelse = "...",
                        harAvbruttStudie = true,
                        godkjentStudieAvLånekassen = true,
                        avbruttPgaSykdomEllerSkade = true,
                        harBehovForBehandling = true,
                        avbruttStudieDato = avbruttStudieDato,
                        avbruddMerEnn6Måneder = true
                    ),
                )
            )
            .medKontekst {
                val vilkår = repositoryProvider.provide<VilkårsresultatRepository>().hent(this.behandling.id)

                val studentVilkår = vilkår.finnVilkår(Vilkårtype.STUDENT)
                studentVilkår.tidslinje().assertTidslinje(
                    Segment(Periode(fom, avbruttStudieDato.plusMonths(6).minusDays(1))) { vurdering ->
                        assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
                    },
                    Segment(Periode(avbruttStudieDato.plusMonths(6), Tid.MAKS)) { vurdering ->
                        assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                        assertThat(vurdering.avslagsårsak).isEqualTo(Avslagsårsak.VARIGHET_OVERSKREDET_STUDENT)
                    }
                )
            }
            .løsRefusjonskrav()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsSykestipend(listOf(sykestipendPeriode))
            .medKontekst {
                val vilkår = repositoryProvider.provide<VilkårsresultatRepository>().hent(this.behandling.id)
                val syksetipendVilkår = vilkår.finnVilkår(Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING)
                assertThat(syksetipendVilkår.harPerioderMedIkkeOppfylt()).isTrue
            }
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                val vilkår = repositoryProvider.provide<VilkårsresultatRepository>().hent(behandling.id)
                val v = vilkår.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
                assertThat(v.harPerioderSomErOppfylt()).isFalse
            }
            .assertRettighetstype(
                Periode(
                    sykestipendPeriode.tom.plusDays(1), // Virkningstidspunkt
                    avbruttStudieDato.plusMonths(6).minusDays(1)
                ) to RettighetsType.STUDENT
            )

        // Revurdering
        val relevanteVurderingsbehov = listOf(Vurderingsbehov.REVURDER_STUDENT)

        sak.opprettManuellRevurdering(
            relevanteVurderingsbehov,
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
                val vilkår = repositoryProvider.provide<VilkårsresultatRepository>().hent(this.behandling.id)

                val studentVilkår = vilkår.finnVilkår(Vilkårtype.STUDENT)
                studentVilkår.tidslinje().assertTidslinje(
                    Segment(Periode(fom, Tid.MAKS)) { vurdering ->
                        assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                        assertThat(vurdering.avslagsårsak).isEqualTo(Avslagsårsak.IKKE_RETT_PA_STUDENT)
                    }
                )

            }
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs { "Skal vurderes for ordinær dersom ikke oppfylt student" }
                    .containsExactlyInAnyOrder(Definisjon.AVKLAR_SYKDOM)
            }
            .løsSykdom(sak.rettighetsperiode.fom)
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs { "Skal vurderes for ordinær dersom ikke oppfylt student" }
                    .containsExactlyInAnyOrder(Definisjon.AVKLAR_BISTANDSBEHOV)
            }
            .løsBistand(sak.rettighetsperiode.fom)
            .løsSykdomsvurderingBrev()
            .løsSykestipend()
            .medKontekst {
                val vilkår = repositoryProvider.provide<VilkårsresultatRepository>().hent(this.behandling.id)
                val v = vilkår.finnVilkår(Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING)
                assertThat(v.harPerioderMedIkkeOppfylt()).isFalse
            }
            .foreslåVedtak()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FATTE_VEDTAK)
            }
            .fattVedtak()
            .medKontekst {
                val vilkår = repositoryProvider.provide<VilkårsresultatRepository>().hent(this.behandling.id)
                val v = vilkår.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
                assertThat(v.harPerioderSomErOppfylt()).isTrue
            }
            .assertRettighetstype(
                Periode(
                    fom,
                    if (unleashGateway.objectInstance?.isEnabled(BehandlingsflytFeature.LagreVedtakIFatteVedtak) == true) {
                        fom.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)
                    } else {
                        fom.plusDays(15).plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)
                    }
                ) to RettighetsType.BISTANDSBEHOV
            )
    }
}

