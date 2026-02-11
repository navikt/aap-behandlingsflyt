package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Avslag
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktVurdering
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Oppfylt
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.tomUnderveisInput
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.OrdinærKvoteFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.OrdinærKvoteVilkår
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.SykepengeerstatningKvoteFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.SykepengeerstatningKvoteVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.help.genererVilkårsresultat
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.sql.DataSource

class UnderveisServiceTest {
    private val dataSource: DataSource = MockDataSource()
    private val kvoter = tomKvoter

    @Test
    fun `skal vurdere alle reglene`() {
        dataSource.transaction { connection ->
            val underveisService = UnderveisService(
                postgresRepositoryRegistry.provider(connection),
                createGatewayProvider {
                    register<AlleAvskruddUnleash>()
                }
            )
            val søknadsdato = LocalDate.now().minusDays(29)
            val periode = Periode(søknadsdato, søknadsdato.plusYears(3))
            val vilkårsresultat = genererVilkårsresultat(
                periode,
                studentVilkår = Vilkår(
                    Vilkårtype.STUDENT,
                    setOf(
                        Vilkårsperiode(
                            periode,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null,
                            avslagsårsak = null
                        )
                    )
                ),
                bistandVilkåret = Vilkår(
                    Vilkårtype.BISTANDSVILKÅRET,
                    setOf(
                        Vilkårsperiode(
                            periode,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null,
                            avslagsårsak = null
                        )
                    )
                ),
                sykepengeerstatningVilkåret = Vilkår(
                    Vilkårtype.SYKEPENGEERSTATNING, setOf(

                        Vilkårsperiode(
                            periode,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null,
                            avslagsårsak = null
                        )
                    )
                ),
            )
            val input = tomUnderveisInput(
                rettighetsperiode = periode,
                vilkårsresultat = vilkårsresultat,
                opptrappingPerioder = listOf(Periode(søknadsdato.plusYears(2), søknadsdato.plusYears(3))),
                kvoter = kvoter,
                forenkletKvoteFeature = true
            )

            val vurderingTidslinje = underveisService.vurderRegler(input)

            assertThat(vurderingTidslinje.segmenter()).isNotEmpty()
        }
    }

    @Test
    fun `Persistert rettighetstype med kvotevilkår skal gi samme svar som utledet bortsett fra rett der det er avslag på kvote`() {
        dataSource.transaction { connection ->
            val underveisService = UnderveisService(
                postgresRepositoryRegistry.provider(connection),
                createGatewayProvider {
                    register<AlleAvskruddUnleash>()
                }
            )
            val søknadsdato = 1 januar 2024

            val studentOppfylt = Periode(søknadsdato, 7 januar 2024) // Bruker 5 dager av ordinær kvote
            val speOppfylt =
                Periode(8 januar 2024, 14 januar 2024) // Bruker 5 dager av sykepengeerstatning-kvote
            val periodeBistandOppfylt = Periode(15 januar 2024, Tid.MAKS) // Bruker gjenværende dager av ordinær kvote


            val rettighetsperiode = Periode(søknadsdato, Tid.MAKS)
            val vilkårsresultat = genererVilkårsresultat(
                rettighetsperiode,
                studentVilkår = Vilkår(
                    Vilkårtype.STUDENT,
                    setOf(
                        Vilkårsperiode(
                            studentOppfylt,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null,
                            avslagsårsak = null
                        )
                    )
                ),
                bistandVilkåret = Vilkår(
                    Vilkårtype.BISTANDSVILKÅRET,
                    setOf(
                        Vilkårsperiode(
                            periodeBistandOppfylt,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null,
                            avslagsårsak = null
                        )
                    )
                ),
                sykepengeerstatningVilkåret = Vilkår(
                    Vilkårtype.SYKEPENGEERSTATNING, setOf(

                        Vilkårsperiode(
                            speOppfylt,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null,
                            avslagsårsak = null
                        )
                    )
                ),
            )

            val fiktiveKvoter = Kvoter(
                ordinærkvote = Hverdager(10),
                sykepengeerstatningkvote = Hverdager(5)
            )

            // Gjøres i RettighetstypeSteg
            val vilkårsresultat2 = Vilkårsresultat(vilkår = vilkårsresultat.alle())
            val kvotevurdering = vurderRettighetstypeOgKvoter(vilkårsresultat2, fiktiveKvoter)
            val rettighetstypeTidslinje =
                kvotevurdering
                    .filter { it.verdi is KvoteOk }
                    .mapNotNull { it.rettighetsType }.komprimer()
            OrdinærKvoteVilkår(vilkårsresultat2).vurder(OrdinærKvoteFaktagrunnlag(kvotevurdering, fiktiveKvoter))
            SykepengeerstatningKvoteVilkår(vilkårsresultat2).vurder(
                SykepengeerstatningKvoteFaktagrunnlag(
                    kvotevurdering,
                    fiktiveKvoter
                )
            )

            // Generer bare underveisperioder én måned frem i tid
            val underveisPeriode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusMonths(1))

            val input1 = tomUnderveisInput(
                rettighetsperiode = underveisPeriode,
                vilkårsresultat = vilkårsresultat,
                opptrappingPerioder = listOf(Periode(søknadsdato.plusYears(2), søknadsdato.plusYears(3))),
                kvoter = fiktiveKvoter,
                forenkletKvoteFeature = true,
                rettighetstypeGrunnlag = null
            )

            val input2 = tomUnderveisInput(
                rettighetsperiode = underveisPeriode,
                vilkårsresultat = vilkårsresultat2,
                opptrappingPerioder = listOf(Periode(søknadsdato.plusYears(2), søknadsdato.plusYears(3))),
                kvoter = fiktiveKvoter,
                forenkletKvoteFeature = true,
                rettighetstypeGrunnlag = RettighetstypeGrunnlag(rettighetstypeTidslinje)
            )

            val vurderingTidslinje1 = underveisService.vurderRegler(input1)
            val vurderingTidslinje2 = underveisService.vurderRegler(input2)


            val forventetPeriodeOppfyltOrdinær = Periode(
                periodeBistandOppfylt.fom,
                21 januar 2024 // Siste dag med kvote
            )

            val forventetPeriodeOrdinærKvoteBruktOpp = Periode(
                22 januar 2024,
                underveisPeriode.tom
            )

            assertTidslinje(
                vurderingTidslinje1.outerJoin(vurderingTidslinje2) { v1, v2 ->
                    Pair(v1, v2)
                },
                studentOppfylt to { assertThat(it.first).isEqualTo(it.second) },
                speOppfylt to { assertThat(it.first).isEqualTo(it.second) },
                forventetPeriodeOppfyltOrdinær to {
                    assertThat(it.first!!.copy(meldepliktVurdering = null)).isEqualTo(
                        it.second!!.copy(
                            meldepliktVurdering = null
                        )
                    )
                    assertTrue(it.first!!.meldepliktVurdering is MeldepliktVurdering.IkkeMeldtSeg)
                    assertThat(it.second!!.meldepliktVurdering is MeldepliktVurdering.UtenRett)
                        .describedAs { "Overlapper med meldeperiode uten rett, så forventer UtenRett" }
                        .isTrue()
                },
                forventetPeriodeOrdinærKvoteBruktOpp to {
                    assertThat(it.first!!.fårAapEtter).isEqualTo(RettighetsType.BISTANDSBEHOV)
                    assertThat(it.second!!.fårAapEtter)
                        .describedAs { "Forventer at persistert rettighetstype er justert for kvote" }
                        .isNull()
                    assertThat(it.first!!.meldepliktVurdering is MeldepliktVurdering.IkkeMeldtSeg)
                    assertThat(it.second!!.meldepliktVurdering is MeldepliktVurdering.UtenRett)
                    assertThat(it.first!!.utfall()).isEqualTo(it.second!!.utfall())
                    assertThat(it.first!!.avslagsårsak()).isEqualTo(it.second!!.avslagsårsak())
                },
            )
        }
    }

    @Test
    fun `oppfylt kvotevilkår bruker av kvoten hvis hverdag, ikke oppfylt er avslag på kvote, og ikke relevant bruker ikke av kvoten`() {
        dataSource.transaction { connection ->
            val underveisService = UnderveisService(
                postgresRepositoryRegistry.provider(connection),
                createGatewayProvider {
                    register<AlleAvskruddUnleash>()
                }
            )
            val søknadsdato = 1 januar 2024

            val fiktiveKvoter = Kvoter(
                ordinærkvote = Hverdager(10),
                sykepengeerstatningkvote = Hverdager(4)
            )


            val studentOppfylt = Periode(søknadsdato, 7 januar 2024) // Bruker 5 dager av ordinær kvote
            val speOppfylt =
                Periode(8 januar 2024, 14 januar 2024) // Bruker alle 4 dager av sykepengeerstatning-kvote
            val periodeBistandOppfylt = Periode(15 januar 2024, Tid.MAKS) // Bruker gjenværende dager av ordinær kvote
            val samordningAvslag =
                Periode(15 januar 2024, 17 januar 2024) // Tre dager med avskag skal ikke bruke av kvote


            val rettighetsperiode = Periode(søknadsdato, Tid.MAKS)
            val vilkårsresultat = genererVilkårsresultat(
                rettighetsperiode,
                samordningVilkår = Vilkår(
                    Vilkårtype.SAMORDNING,
                    setOf(
                        Vilkårsperiode(
                            samordningAvslag,
                            Utfall.IKKE_OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null,
                            avslagsårsak = Avslagsårsak.ANNEN_FULL_YTELSE
                        )
                    )
                ),
                studentVilkår = Vilkår(
                    Vilkårtype.STUDENT,
                    setOf(
                        Vilkårsperiode(
                            studentOppfylt,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null,
                            avslagsårsak = null
                        )
                    )
                ),
                bistandVilkåret = Vilkår(
                    Vilkårtype.BISTANDSVILKÅRET,
                    setOf(
                        Vilkårsperiode(
                            periodeBistandOppfylt,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null,
                            avslagsårsak = null
                        )
                    )
                ),
                sykepengeerstatningVilkåret = Vilkår(
                    Vilkårtype.SYKEPENGEERSTATNING, setOf(

                        Vilkårsperiode(
                            speOppfylt,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null,
                            avslagsårsak = null
                        )
                    )
                ),
            )

            // Gjøres i RettighetstypeSteg
            val kvotevurdering = vurderRettighetstypeOgKvoter(vilkårsresultat, fiktiveKvoter)
            val rettighetstypeTidslinje =
                kvotevurdering
                    .filter { it.verdi is KvoteOk }
                    .mapNotNull { it.rettighetsType }.komprimer()
            OrdinærKvoteVilkår(vilkårsresultat).vurder(OrdinærKvoteFaktagrunnlag(kvotevurdering, fiktiveKvoter))
            SykepengeerstatningKvoteVilkår(vilkårsresultat).vurder(
                SykepengeerstatningKvoteFaktagrunnlag(
                    kvotevurdering,
                    fiktiveKvoter
                )
            )

            val underveisPeriode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusYears(1))

            val input = tomUnderveisInput(
                rettighetsperiode = underveisPeriode,
                vilkårsresultat = vilkårsresultat,
                kvoter = fiktiveKvoter,
                forenkletKvoteFeature = true,
                rettighetstypeGrunnlag = RettighetstypeGrunnlag(rettighetstypeTidslinje)
            )

            val vurderingTidslinje = underveisService.vurderRegler(input)

            val forventetSpeOppfylt = Periode(
                speOppfylt.fom,
                speOppfylt.fom.plusDays(3)
            )
            val forventetPeriodeSpeKvoteBruktOpp = Periode(
                forventetSpeOppfylt.tom.plusDays(1),
                speOppfylt.tom
            )

            val forventetPeriodeOppfyltOrdinær = Periode(
                samordningAvslag.tom.plusDays(1),
                24 januar 2024 // Siste dag med kvote
            )

            val forventetPeriodeOrdinærKvoteBruktOpp = Periode(
                25 januar 2024, // Samordning skal ikke bruke av kvoten. Derfor 22 januar + tre dager
                underveisPeriode.tom
            )

            assertTidslinje(
                vurderingTidslinje,
                studentOppfylt to { assertThat(it.fårAapEtter).isEqualTo(RettighetsType.STUDENT) },
                forventetSpeOppfylt to {
                    assertThat(it.fårAapEtter).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                    assertThat(it.varighetVurdering)
                        .isEqualTo(Oppfylt(brukerAvKvoter = setOf(Kvote.SYKEPENGEERSTATNING)))
                },
                forventetPeriodeSpeKvoteBruktOpp to {
                    assertThat(it.fårAapEtter).isNull()
                    assertThat(it.varighetVurdering)
                        .isEqualTo(
                            Avslag(
                                brukerAvKvoter = setOf(Kvote.SYKEPENGEERSTATNING), avslagsårsaker = setOf(
                                    VarighetVurdering.Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP
                                )
                            )
                        )
                },
                samordningAvslag to {
                    assertThat(it.fårAapEtter).isEqualTo(null)
                    assertThat(it.varighetVurdering?.brukerAvKvoter)
                        .describedAs { "Skal ikke bruke av kvote ved avslag" }
                        .isEmpty()
                },
                forventetPeriodeOppfyltOrdinær to {
                    assertThat(it.fårAapEtter).isEqualTo(RettighetsType.BISTANDSBEHOV)
                    assertThat(it.varighetVurdering)
                        .isEqualTo(Oppfylt(brukerAvKvoter = setOf(Kvote.ORDINÆR)))
                },
                forventetPeriodeOrdinærKvoteBruktOpp to {
                    assertThat(it.fårAapEtter).isEqualTo(null)
                    assertThat(it.varighetVurdering)
                        .isEqualTo(
                            Avslag(
                                brukerAvKvoter = setOf(Kvote.ORDINÆR), avslagsårsaker = setOf(
                                    VarighetVurdering.Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP
                                )
                            )
                        )
                },
            )
        }
    }
}
