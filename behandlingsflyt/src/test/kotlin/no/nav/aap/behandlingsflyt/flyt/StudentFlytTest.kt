package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentEnkelLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plusHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurderingDTO
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.FakeUnleashBase
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass

@ParameterizedClass
@MethodSource("testData")
class StudentFlytTest(val unleashGateway: KClass<UnleashGateway>) : AbstraktFlytOrkestratorTest(unleashGateway) {
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
            .medKontekst { 
                if(unleashGateway.objectInstance!!.isEnabled(BehandlingsflytFeature.Sykestipend)) {
                    this.behandling.løsSykestipend()
                }
            }
            .løsRefusjonskrav()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                val vilkår = repositoryProvider.provide<VilkårsresultatRepository>().hent(behandling.id)
                val v = vilkår.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
                assertThat(v.harPerioderSomErOppfylt()).isTrue
            }
            .assertRettighetstype(
                Periode(fom, fom.plusHverdager(Hverdager(130)).minusDays(1)) to RettighetsType.STUDENT,
            )

        // Revurdering
        val revurdering = sak.opprettManuellRevurdering(
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
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs { "Skal vurderes for ordinær dersom ikke oppfylt student" }
                    .containsExactlyInAnyOrder(Definisjon.AVKLAR_BISTANDSBEHOV)
            }
            .løsBistand(sak.rettighetsperiode.fom)
            .løsSykdomsvurderingBrev()
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
                    fom.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)
                ) to RettighetsType.BISTANDSBEHOV,
            )

    }

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun testData(): List<Arguments> {
            return listOf(
                Arguments.of(FakeUnleash::class),
                Arguments.of(SykestipendAktivert::class),
            )
        }
    }

}

object SykestipendAktivert: FakeUnleashBase(
    mapOf(
        BehandlingsflytFeature.IngenValidering to false,
        BehandlingsflytFeature.NyBrevtype11_17 to true,
        BehandlingsflytFeature.OverforingsdatoNullForAvregning to true,
        BehandlingsflytFeature.OvergangArbeid to true,
        BehandlingsflytFeature.KvalitetssikringsSteg to true,
        BehandlingsflytFeature.EOSBeregning to true,
        BehandlingsflytFeature.NyBrevbyggerV3 to false,
        BehandlingsflytFeature.BedreUttrekkAvSakerMedFritakMeldeplikt to false,
        BehandlingsflytFeature.LagreVedtakIFatteVedtak to true,
        BehandlingsflytFeature.PeriodisertSykepengeErstatningNyAvklaringsbehovService to true,
        BehandlingsflytFeature.ValiderOvergangUfore to true,
        BehandlingsflytFeature.KravOmInntektsbortfall to true,
        BehandlingsflytFeature.Under18 to true,
        BehandlingsflytFeature.MigrerArbeidsevne to true,
        BehandlingsflytFeature.SosialRefusjon to true,
        BehandlingsflytFeature.HentSykepengerVedOverlapp to true,
        BehandlingsflytFeature.KanSendeBrevOmBarnetilleggSatsRegulering to true,
        BehandlingsflytFeature.MigrerRettighetsperiode to true,
        BehandlingsflytFeature.PeriodisertSykdom to true,
        BehandlingsflytFeature.Sykestipend to true,
    )
)