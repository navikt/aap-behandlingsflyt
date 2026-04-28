package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plusHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMinstHalvpartenValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import kotlin.reflect.KClass

@ParameterizedClass
@MethodSource("unleashTestDataSource")
class SykepengeerstatningFlytTest(val unleashGateway: KClass<UnleashGateway>) :
    AbstraktFlytOrkestratorTest(unleashGateway) {
    @Test
    fun `Sykepengeerstatning med yrkesskade`() {
        val søknadsdato = 1 april 2025
        val person = TestPersoner.PERSON_MED_YRKESSKADE()
        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = søknadsdato.atStartOfDay(),
        )

        behandling = behandling.medKontekst {
            assertThat(åpneAvklaringsbehov).isNotEmpty()
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }
            .løsSykdom(
                sak.rettighetsperiode.fom,
                vissVarighet = false,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).describedAs(
                    "Bistandsbehov skal ikke vurderes hvis viss varighet er nei"
                ).doesNotContain(Definisjon.AVKLAR_BISTANDSBEHOV)
            }
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).describedAs(
                    "Person med yrkesskade skal gi avklaringsbehov for yrkesskade"
                ).containsExactly(Definisjon.AVKLAR_YRKESSKADE)
            }
            .løsYrkesskade(person)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).describedAs(
                    "Viss varighet false skal gi avklaringsbehov for sykepengeerstatning"
                ).containsExactly(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
            .løsSykepengeerstatning(søknadsdato to true)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).describedAs(
                    "Forutgående medlemskap skal ikke vurderes for yrkesskade"
                ).doesNotContain(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            }
            .løsBeregningstidspunkt(søknadsdato)
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsOppholdskrav(søknadsdato)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .assertRettighetstype(
                Periode(
                    sak.rettighetsperiode.fom,
                    sak.rettighetsperiode.fom.plusHverdager(Hverdager(131)).minusDays(1),
                ) to RettighetsType.SYKEPENGEERSTATNING,
            )
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)

                val resultat = ResultatUtleder(repositoryProvider, gatewayProvider).utledResultatFørstegangsBehandling(behandling.id)
                assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)
            }

    }

    @Test
    fun `revurdere sykepengeerstatning - skal ikke trigge 11-13 om gjelderFra ikke er kravdato`() {
        val sak = happyCaseFørstegangsbehandling(sendMeldekort = false)
        val gjelderFra = sak.rettighetsperiode.fom.plusMonths(1)

        revurdereFramTilOgMedSykdom(sak, gjelderFra)
            .løsBistand(gjelderFra)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs("Siden vurderingenGjelderFra ikke er lik kravdato (rettighetsperiode.fom), så skal man ikke vurdere 11-13")
                    .containsExactlyInAnyOrder(Definisjon.FATTE_VEDTAK)  // ingen avklaringsbehov løst av NAY, gå rett til fatte vedtak
            }
            .fattVedtak()
            .medKontekst {
                val underveisGrunnlag = repositoryProvider.provide<UnderveisRepository>().hent(this.behandling.id)

                assertThat(underveisGrunnlag.perioder).isNotEmpty
                assertThat(underveisGrunnlag.perioder).extracting<RettighetsType>(Underveisperiode::rettighetsType)
                    .allSatisfy { rettighetsType ->
                        assertThat(rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)
                    }
            }
    }

    @Test
    fun `ikke sykdom viss varighet, men skal få innvilget 11-13 sykepengererstatning`() {
        val søknadsdato = LocalDate.now()

        // Sender inn en søknad
        var (sak, behandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadsdato.atStartOfDay())

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = behandling.løsAvklaringsBehov(
            AvklarSykdomLøsning(
                løsningerForPerioder = listOf(
                    SykdomsvurderingLøsningDto(
                        begrunnelse = "Er syk nok",
                        dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                        harSkadeSykdomEllerLyte = true,
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                        // Nei på denne gir mulighet til å innvilge på 11-13
                        erNedsettelseIArbeidsevneAvEnVissVarighet = false,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                        erArbeidsevnenNedsatt = true,
                        yrkesskadeBegrunnelse = null,
                        fom = sak.rettighetsperiode.fom,
                        tom = null,
                        erNedsettelseMinstHalvparten = null,
                        erNedsettelseMerEnnYrkesskadegrense = null,
                    )
                )
            ),
        )
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .contains(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
            .løsSykepengeerstatning(søknadsdato to true)
            .løsBeregningstidspunkt()
            .løsOppholdskrav(søknadsdato)
            .løsAndreStatligeYtelser()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { avklaringsbehov ->
                    assertThat(avklaringsbehov.definisjon).isEqualTo(
                        Definisjon.FORESLÅ_VEDTAK
                    )
                }
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning()).medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)

                val resultat = ResultatUtleder(repositoryProvider, gatewayProvider).utledResultatFørstegangsBehandling(behandling.id)
                assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)
            }
            .løsVedtaksbrev()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)

                val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
                val sykepengeerstatningvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKEPENGEERSTATNING)

                assertThat(sykepengeerstatningvilkåret.vilkårsperioder()).hasSize(1).first()
                    .extracting(Vilkårsperiode::erOppfylt, Vilkårsperiode::innvilgelsesårsak)
                    .containsExactly(true, null)

                val resultat = ResultatUtleder(repositoryProvider, gatewayProvider).utledResultatFørstegangsBehandling(behandling.id)

                assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)
            }
            .assertRettighetstype(
                Periode(
                    sak.rettighetsperiode.fom,
                    sak.rettighetsperiode.fom.plusHverdager(Hverdager(131)).minusDays(1)
                ) to RettighetsType.SYKEPENGEERSTATNING
            )

        // Verifisere at det går an å kun 1 mnd med sykepengeerstatning
        val revurderingFom = søknadsdato.plusMonths(1)
        val revurdering = sak.opprettManuellRevurdering(
            listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
        )
            .løsSykdom(vurderingGjelderFra = revurderingFom)
            .løsBistand(revurderingFom)
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov.map { it.definisjon }).containsOnly(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)
            }
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov.map { it.definisjon }).containsOnly(Definisjon.FATTE_VEDTAK)

                val underveisTidslinje =
                    repositoryProvider.provide<UnderveisRepository>().hent(this.behandling.id).perioder
                        .map { Segment(it.periode, it) }.let(::Tidslinje)

                val oppfyltPeriode = underveisTidslinje.filter { it.verdi.rettighetsType != null }.helePerioden()
                val vilkårsresultat = hentVilkårsresultat(behandlingId = this.behandling.id)
                val rettighetstypeTidslinje = vilkårsresultat.rettighetstypeTidslinje()

                assertThat(oppfyltPeriode.fom).isEqualTo(søknadsdato)
                // Oppfylt ut rettighetsperioden
                assertThat(oppfyltPeriode.tom).isEqualTo(søknadsdato.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR))
                assertThat(underveisTidslinje.helePerioden().fom).isEqualTo(rettighetstypeTidslinje.helePerioden().fom)
            }
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)
            .assertRettighetstype(
                Periode(
                    sak.rettighetsperiode.fom,
                    sak.rettighetsperiode.fom.plusMonths(1).minusDays(1)
                ) to RettighetsType.SYKEPENGEERSTATNING,
                Periode(
                    revurderingFom,
                    sak.rettighetsperiode.fom.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)
                ) to RettighetsType.BISTANDSBEHOV
            )

        // Revurdering nr 2, innvilger sp-erstatning på nytt
        val førstePeriodeSykepengeerstatning = Periode(søknadsdato, søknadsdato.plusMonths(1).minusDays(1))

        val revurdering2Fom = søknadsdato.plusMonths(2)
        sak.opprettManuellRevurdering(
            listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
        )
            .medKontekst {
                assertThat(this.behandling.id).isNotEqualTo(revurdering.id)
            }
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    løsningerForPerioder = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = null,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            fom = revurdering2Fom,
                            tom = null,
                            erNedsettelseMinstHalvparten = ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER,
                            erNedsettelseMerEnnYrkesskadegrense = null,
                        )
                    )
                )
            )
            .løsBistand(revurdering2Fom, true)
            .løsOvergangArbeid(Utfall.IKKE_OPPFYLT, søknadsdato)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .løsSykepengeerstatning(søknadsdato.plusMonths(2) to true)
            .assertRettighetstype(
                førstePeriodeSykepengeerstatning to
                        RettighetsType.SYKEPENGEERSTATNING,
                Periode(søknadsdato.plusMonths(1), søknadsdato.plusMonths(2).minusDays(1)) to
                        RettighetsType.BISTANDSBEHOV,
                Periode(
                    søknadsdato.plusMonths(2),
                    søknadsdato.plusMonths(2).plusHverdager(
                        Hverdager(131) - førstePeriodeSykepengeerstatning.antallHverdager()
                    ).minusDays(1)
                ) to
                        RettighetsType.SYKEPENGEERSTATNING,
            )
    }


    @Test
    fun `ikke sykdom viss varighet, endrer rettighetsperiode etter 11-5 - skal ikke få spørsmål om 11-6`() {
        val søknadsdato = LocalDate.now()

        // Sender inn en søknad
        var (sak, behandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadsdato.atStartOfDay())

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        val nyStartDato = søknadsdato.minusDays(7)
        behandling.løsAvklaringsBehov(
            AvklarSykdomLøsning(
                løsningerForPerioder = listOf(
                    SykdomsvurderingLøsningDto(
                        begrunnelse = "Er syk nok",
                        dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                        harSkadeSykdomEllerLyte = true,
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                        // Nei på denne gir mulighet til å innvilge på 11-13
                        erNedsettelseIArbeidsevneAvEnVissVarighet = false,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                        erArbeidsevnenNedsatt = true,
                        yrkesskadeBegrunnelse = null,
                        fom = sak.rettighetsperiode.fom,
                        tom = null,
                        erNedsettelseMinstHalvparten = null,
                        erNedsettelseMerEnnYrkesskadegrense = null,
                    )
                )
            ),
        )
        behandling = sak.opprettManuellRevurdering(
            vurderingsbehov = listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE),
        )
        behandling.løsRettighetsperiode(nyStartDato)
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    løsningerForPerioder = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            // Nei på denne gir mulighet til å innvilge på 11-13
                            erNedsettelseIArbeidsevneAvEnVissVarighet = false,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            fom = nyStartDato,
                            tom = null,
                            erNedsettelseMinstHalvparten = null,
                            erNedsettelseMerEnnYrkesskadegrense = null,
                        )
                    )
                ),
            ).medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .doesNotContain(Definisjon.AVKLAR_BISTANDSBEHOV)
            }

    }

    @Test
    fun `happy case førstegangsbehandling + revurder førstegangssøknad, gi sykepengererstatning hele perioden`() {
        val sak = happyCaseFørstegangsbehandling(sendMeldekort = false)

        revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom, vissVarighet = false)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs("Siden vurderingenGjelderFra er lik kravdato (rettighetsperiode.fom), så kan man revurdere 11-13")
                    .containsExactlyInAnyOrder(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
            .løsSykepengeerstatning(sak.rettighetsperiode.fom to true)
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FORESLÅ_VEDTAK)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .assertRettighetstype(
                Periode(
                    sak.rettighetsperiode.fom,
                    sak.rettighetsperiode.fom.plusHverdager(Hverdager(131)).minusDays(1)
                ) to
                        RettighetsType.SYKEPENGEERSTATNING
            )
    }


}