package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentLøsningV2
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.PeriodisertStudentDto
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.test.oktober
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
@MethodSource("studentV2UnleashDataSource")
class StudentV2FlytTest(val unleashGateway: KClass<UnleashGateway>) : AbstraktFlytOrkestratorTest(unleashGateway) {

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun studentV2UnleashDataSource() = listOf(
            org.junit.jupiter.params.provider.Arguments.of(LokalUnleash::class),
        )
    }

    @Test
    fun `innvilge som student V2`() {
        val fom = 24 november 2025
        val sykestipendPeriode = Periode(fom, fom.plusDays(14))
        val person = TestPersoner.STANDARD_PERSON()

        var (_, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            søknad = TestSøknader.SØKNAD_STUDENT
        )

        val avbruttStudieDato = 1 oktober 2025
        val forventetVarighetSluttStudent = avbruttStudieDato.plusMonths(6)

        behandling = behandling
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs { "AVKLAR_STUDENT (V1) skal ikke opprettes når StudentV2 er påskrudd" }
                    .doesNotContain(Definisjon.AVKLAR_STUDENT)
            }
            .løsSykdomSomPotensieltOppfyltStudent(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs { "AVKLAR_STUDENT_V2 skal opprettes etter sykdom er løst med NEI_MEN_STUDENT og kvalitetssikring er gjort" }
                    .contains(Definisjon.AVKLAR_STUDENT_V2)
            }
            .løsAvklaringsBehov(
                AvklarStudentLøsningV2(
                    løsningerForPerioder = listOf(
                        PeriodisertStudentDto(
                            fom = fom,
                            begrunnelse = "...",
                            harAvbruttStudie = true,
                            godkjentStudieAvLånekassen = true,
                            avbruttPgaSykdomEllerSkade = true,
                            harBehovForBehandling = true,
                            avbruttStudieDato = avbruttStudieDato,
                            avbruddMerEnn6Måneder = true
                        )
                    )
                )
            )
            .medKontekst {
                val vilkår = repositoryProvider.provide<VilkårsresultatRepository>().hent(this.behandling.id)
                val studentVilkår = vilkår.finnVilkår(Vilkårtype.STUDENT)

                studentVilkår.tidslinje().assertTidslinje(
                    Segment(Periode(fom, avbruttStudieDato.plusMonths(6).minusDays(1))) { vurdering ->
                        assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
                    },
                    Segment(Periode(forventetVarighetSluttStudent, Tid.MAKS)) { vurdering ->
                        assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                        assertThat(vurdering.avslagsårsak).isEqualTo(Avslagsårsak.VARIGHET_OVERSKREDET_STUDENT)
                    }
                )
            }
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsSykestipend(listOf(sykestipendPeriode))
            .medKontekst {
                val vilkår = repositoryProvider.provide<VilkårsresultatRepository>().hent(this.behandling.id)
                val sykestipendVilkår = vilkår.finnVilkår(Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING)
                assertThat(sykestipendVilkår.harPerioderMedIkkeOppfylt()).isTrue
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
    }

    @Test
    fun `revurdering med tom løsningerForPerioder skal ikke kaste NoSuchElementException`() {
        val fom = 24 november 2025
        val sykestipendPeriode = Periode(fom, fom.plusDays(14))
        val avbruttStudieDato = 1 oktober 2025

        val person = TestPersoner.STANDARD_PERSON()

        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            søknad = TestSøknader.SØKNAD_STUDENT
        )

        behandling
            .løsSykdomSomPotensieltOppfyltStudent(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsAvklaringsBehov(
                AvklarStudentLøsningV2(
                    løsningerForPerioder = listOf(
                        PeriodisertStudentDto(
                            fom = fom,
                            begrunnelse = "...",
                            harAvbruttStudie = true,
                            godkjentStudieAvLånekassen = true,
                            avbruttPgaSykdomEllerSkade = true,
                            harBehovForBehandling = true,
                            avbruttStudieDato = avbruttStudieDato,
                            avbruddMerEnn6Måneder = true
                        )
                    )
                )
            )
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsSykestipend(listOf(sykestipendPeriode))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()

        sak.opprettManuellRevurdering(listOf(Vurderingsbehov.REVURDER_STUDENT))
            .løsAvklaringsBehov(
                AvklarStudentLøsningV2(løsningerForPerioder = emptyList())
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .doesNotContain(Definisjon.AVKLAR_STUDENT_V2)
            }
    }

    @Test
    fun `Periodisering - skal kunne avslutte student i samme behandling`() {
        val fom = 24 november 2025
        val sykestipendPeriode = Periode(fom, fom.plusDays(14))
        val person = TestPersoner.STANDARD_PERSON()

        var (_, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            søknad = TestSøknader.SØKNAD_STUDENT
        )

        val avbruttStudieDato = 1 oktober 2025

        behandling = behandling
            .løsSykdomSomPotensieltOppfyltStudent(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsAvklaringsBehov(
                AvklarStudentLøsningV2(
                    løsningerForPerioder = listOf(
                        PeriodisertStudentDto(
                            fom = fom,
                            begrunnelse = "...",
                            harAvbruttStudie = true,
                            godkjentStudieAvLånekassen = true,
                            avbruttPgaSykdomEllerSkade = true,
                            harBehovForBehandling = true,
                            avbruttStudieDato = avbruttStudieDato,
                            avbruddMerEnn6Måneder = true
                        ),
                        PeriodisertStudentDto(
                            fom = 1 januar 2026,
                            begrunnelse = "Ikke lenger",
                            harAvbruttStudie = false,
                            godkjentStudieAvLånekassen = null,
                            avbruttPgaSykdomEllerSkade = null,
                            harBehovForBehandling = null,
                            avbruttStudieDato = null,
                            avbruddMerEnn6Måneder = null
                        ),
                    )
                )
            )
            .medKontekst {
                val vilkår = repositoryProvider.provide<VilkårsresultatRepository>().hent(this.behandling.id)

                val studentVilkår = vilkår.finnVilkår(Vilkårtype.STUDENT)
                studentVilkår.tidslinje().assertTidslinje(
                    Segment(Periode(fom, 31 desember 2025)) { vurdering ->
                        assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
                    },
                    Segment(Periode(1 januar 2026, Tid.MAKS)) { vurdering ->
                        assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                        assertThat(vurdering.avslagsårsak).isEqualTo(Avslagsårsak.IKKE_RETT_PA_STUDENT)
                    }
                )
            }
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsSykestipend(listOf(sykestipendPeriode))
            .medKontekst {
                val vilkår = repositoryProvider.provide<VilkårsresultatRepository>().hent(this.behandling.id)
                val sykestipendVilkår = vilkår.finnVilkår(Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING)
                assertThat(sykestipendVilkår.harPerioderMedIkkeOppfylt()).isTrue
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
                    31 desember 2025
                ) to RettighetsType.STUDENT
            )
    }
}
