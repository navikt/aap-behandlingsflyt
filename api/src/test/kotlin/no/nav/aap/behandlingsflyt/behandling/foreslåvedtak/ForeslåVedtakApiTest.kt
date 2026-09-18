package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@Fakes
class ForeslåVedtakApiTest : BaseApiTest() {

    private fun gatewayProvider() = createGatewayProvider {
        register<AlleAvskruddUnleash>()
    }

    @Test
    fun `returnerer tom liste av perioder når det ikke finnes underveisgrunnlag`() {
        val sak = opprettInMemorySak(LocalDate.of(2026, 1, 1))
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)
        InMemoryVilkårsresultatRepository.slett(behandling.id)

        testApplication {
            installApplication {
                foreslaaVedtakApi(MockDataSource(), inMemoryRepositoryRegistry, gatewayProvider())
            }

            val response =
                createClient().get("/api/behandling/${behandling.referanse.referanse}/grunnlag/foreslaa-vedtak") {
                    header("Authorization", "Bearer ${getToken().token()}")
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<ForeslåVedtakResponse>()
            assertThat(body.perioder).isEmpty()
            assertThat(body.stansOpphør).isEmpty()
            assertThat(body.harTilgangTilÅSaksbehandle).isTrue()
        }
    }

    @Test
    fun `returnerer foreslått vedtak-periode uten vilkårsavslag når underveisgrunnlag finnes og vilkår er oppfylt`() {
        val sak = opprettInMemorySak(LocalDate.of(2026, 1, 1))
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)
        val periode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15))

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(
                Underveisperiode(
                    periode = periode,
                    meldePeriode = periode,
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`100_PROSENT`,
                    institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
                    arbeidsgradering = ArbeidsGradering(
                        totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
                        andelArbeid = Prosent.`0_PROSENT`,
                        fastsattArbeidsevne = Prosent.`100_PROSENT`,
                        gradering = Prosent.`100_PROSENT`,
                        opplysningerMottatt = null,
                    ),
                    trekk = Dagsatser(0),
                    brukerAvKvoter = emptySet(),
                    meldepliktStatus = null,
                    meldepliktGradering = null,
                )
            ),
            input = object : Faktagrunnlag {},
        )

        // Alle vilkårene som TidligereVurderingerImpl sjekker før FORESLÅ_VEDTAK må være OPPFYLT
        // for at ingen sjekk skal returnere UunngåeligAvslag pga. manglende vurdering.
        val alleOppfylteVilkårtyper = listOf(
            Vilkårtype.LOVVALG,
            Vilkårtype.ALDERSVILKÅRET,
            Vilkårtype.STUDENT,
            Vilkårtype.SYKDOMSVILKÅRET,
            Vilkårtype.OVERGANGUFØREVILKÅRET,
            Vilkårtype.OVERGANGARBEIDVILKÅRET,
            Vilkårtype.SYKEPENGEERSTATNING,
            Vilkårtype.GRUNNLAGET,
            Vilkårtype.INNTEKTSBORTFALL,
            Vilkårtype.MEDLEMSKAP,
            Vilkårtype.SAMORDNING,
            Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING,
        )
        val vilkårsresultat = Vilkårsresultat(
            vilkår = alleOppfylteVilkårtyper.map { type ->
                Vilkår(
                    type,
                    setOf(
                        Vilkårsperiode(
                            periode = periode,
                            utfall = Utfall.OPPFYLT,
                            manuellVurdering = false,
                            begrunnelse = null,
                        )
                    )
                )
            }
        )
        InMemoryVilkårsresultatRepository.lagre(behandling.id, vilkårsresultat)

        testApplication {
            installApplication {
                foreslaaVedtakApi(MockDataSource(), inMemoryRepositoryRegistry, gatewayProvider())
            }

            val response = createClient().get("/api/behandling/${behandling.referanse.referanse}/grunnlag/foreslaa-vedtak") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<ForeslåVedtakResponse>()
            assertThat(body.perioder).hasSize(1)
            val foreslåttPeriode = body.perioder.first()
            assertThat(foreslåttPeriode.periode).isEqualTo(periode)
            assertThat(foreslåttPeriode.utfall).isEqualTo(Utfall.OPPFYLT)
            assertThat(foreslåttPeriode.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)
            assertThat(foreslåttPeriode.avslagsårsak.vilkårsavslag).isEmpty()
            assertThat(foreslåttPeriode.avslagsårsak.underveisavslag).isNull()
        }
    }

    @Test
    fun `inkluderer vilkårsavslag i respons når tidligereVurderinger gir UunngåeligAvslag`() {
        val sak = opprettInMemorySak(LocalDate.of(2026, 1, 1))
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)
        val periode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15))

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(
                Underveisperiode(
                    periode = periode,
                    meldePeriode = periode,
                    utfall = Utfall.IKKE_OPPFYLT,
                    rettighetsType = null,
                    avslagsårsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
                    grenseverdi = Prosent.`100_PROSENT`,
                    institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
                    arbeidsgradering = ArbeidsGradering(
                        totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
                        andelArbeid = Prosent.`0_PROSENT`,
                        fastsattArbeidsevne = Prosent.`100_PROSENT`,
                        gradering = Prosent.`100_PROSENT`,
                        opplysningerMottatt = null,
                    ),
                    trekk = Dagsatser(0),
                    brukerAvKvoter = emptySet(),
                    meldepliktStatus = null,
                    meldepliktGradering = null,
                )
            ),
            input = object : Faktagrunnlag {},
        )

        // Alle andre vilkår enn ALDERSVILKÅRET settes OPPFYLT, slik at kun ALDERSVILKÅRET
        // gir opphav til UunngåeligAvslag i tidslinjen som TidligereVurderingerImpl produserer.
        val øvrigeOppfylteVilkårtyper = listOf(
            Vilkårtype.LOVVALG,
            Vilkårtype.STUDENT,
            Vilkårtype.SYKDOMSVILKÅRET,
            Vilkårtype.OVERGANGUFØREVILKÅRET,
            Vilkårtype.OVERGANGARBEIDVILKÅRET,
            Vilkårtype.SYKEPENGEERSTATNING,
            Vilkårtype.GRUNNLAGET,
            Vilkårtype.INNTEKTSBORTFALL,
            Vilkårtype.MEDLEMSKAP,
            Vilkårtype.SAMORDNING,
            Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING,
        )

        val aldersvilkåretIkkeOppfylt = Vilkår(
            Vilkårtype.ALDERSVILKÅRET,
            setOf(
                Vilkårsperiode(
                    periode = periode,
                    utfall = Utfall.IKKE_OPPFYLT,
                    manuellVurdering = false,
                    begrunnelse = null,
                    avslagsårsak = Avslagsårsak.BRUKER_OVER_67,
                )
            )
        )

        val vilkårsresultat = Vilkårsresultat(
            vilkår = listOf(aldersvilkåretIkkeOppfylt) + øvrigeOppfylteVilkårtyper.map { type ->
                Vilkår(
                    type,
                    setOf(
                        Vilkårsperiode(
                            periode = periode,
                            utfall = Utfall.OPPFYLT,
                            manuellVurdering = false,
                            begrunnelse = null,
                        )
                    )
                )
            }
        )
        InMemoryVilkårsresultatRepository.lagre(behandling.id, vilkårsresultat)

        testApplication {
            installApplication {
                foreslaaVedtakApi(MockDataSource(), inMemoryRepositoryRegistry, gatewayProvider())
            }

            val response = createClient().get("/api/behandling/${behandling.referanse.referanse}/grunnlag/foreslaa-vedtak") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<ForeslåVedtakResponse>()
            assertThat(body.perioder).hasSize(1)
            val foreslåttPeriode = body.perioder.first()
            assertThat(foreslåttPeriode.avslagsårsak.vilkårsavslag)
                .extracting("vilkår")
                .containsExactly(Vilkårtype.ALDERSVILKÅRET.hjemmel)
        }
    }
}