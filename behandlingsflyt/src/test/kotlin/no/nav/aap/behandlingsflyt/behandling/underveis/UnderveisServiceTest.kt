package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.tomUnderveisInput
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.OrdinærKvoteFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.OrdinærKvoteVilkår
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.SykepengeerstatningKvoteFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.kvote.SykepengeerstatningKvoteVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.help.assertTidslinjeEquals
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
            
            // Genrer bare underveisperioder ett år frem i tid
            val underveisPeriode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusYears(1))

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

            assertThat(vurderingTidslinje1.segmenter()).isNotEmpty()
            assertTidslinjeEquals(vurderingTidslinje1.map{it.fårAapEtter}, vurderingTidslinje2.map{it.fårAapEtter})
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

            val studentOppfylt = Periode(søknadsdato, 7 januar 2024) // Bruker 5 dager av ordinær kvote
            val speOppfylt =
                Periode(8 januar 2024, 14 januar 2024) // Bruker 5 dager av sykepengeerstatning-kvote
            val periodeBistandOppfylt = Periode(15 januar 2024, Tid.MAKS) // Bruker gjenværende dager av ordinær kvote


            val periode = Periode(søknadsdato, Tid.MAKS)
            val vilkårsresultat = genererVilkårsresultat(
                periode,
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

            val underveisPeriode = Periode(periode.fom, periode.fom.plusYears(1))
            
            val input = tomUnderveisInput(
                rettighetsperiode = underveisPeriode,
                vilkårsresultat = vilkårsresultat,
                kvoter = fiktiveKvoter,
                forenkletKvoteFeature = true,
                rettighetstypeGrunnlag = RettighetstypeGrunnlag(rettighetstypeTidslinje)
            )

            val vurderingTidslinje = underveisService.vurderRegler(input)


            val forventetPeriodeOppfyltOrdinær = Periode(
                periodeBistandOppfylt.fom,
                21 januar 2024 // Siste dag med kvote
            )

            val forventetPeriodeOrdinærKvoteBruktOpp = Periode(
                22 januar 2024,
                underveisPeriode.tom
            )


            assertTidslinje(
                vurderingTidslinje,
                studentOppfylt to { assertThat(it.fårAapEtter).isEqualTo(RettighetsType.STUDENT) },
                speOppfylt to { assertThat(it.fårAapEtter).isEqualTo(RettighetsType.SYKEPENGEERSTATNING) },
                forventetPeriodeOppfyltOrdinær to { assertThat(it.fårAapEtter).isEqualTo(RettighetsType.BISTANDSBEHOV) },
                forventetPeriodeOrdinærKvoteBruktOpp to { assertThat(it.fårAapEtter).isEqualTo(null) },
            )
        }
    }
}
