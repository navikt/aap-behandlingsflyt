package no.nav.aap.behandlingsflyt.behandling.samordning

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning.SamordningYtelseDTO
import no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning.SamordningYtelseVurderingGrunnlagDTO
import no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning.samordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykepengerOgFerieOppgittISøknad.SykepengerOgFerieSøknad
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomInfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningVurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVedtakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykepengerOgFerieOppgittISøknadRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak


@Fakes
class SamordningApiKtTest : BaseApiTest() {

    @Test
    fun `hente ut samordningsgrunnlag fra API`() {
        val ds = MockDataSource()
        val sak = opprettInMemorySak(LocalDate.now())
        val forrigeBehandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)
        val behandling = opprettBehandling(sak, TypeBehandling.Revurdering, forrigeBehandling.id)

        InMemorySamordningYtelseRepository.lagre(
            behandling.id,
            setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.PLEIEPENGER,
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1).minusDays(1)),
                            gradering = Prosent(50),
                        )
                    ),
                    kilde = "kilde",
                    saksRef = "saksref",
                ),
            ),
        )

        InMemorySykepengerOgFerieOppgittISøknadRepository.lagre(
            behandling.id,
            SykepengerOgFerieSøknad(
                mottarSykepenger = true,
                feriePerioder = listOf(Periode(LocalDate.now(), LocalDate.now().plusDays(6))),
                ferieDager = null,
            ),
        )

        testApplication {
            installApplication {
                samordningGrunnlag(ds, inMemoryRepositoryRegistry, createGatewayProvider {
                    register<NomInfoGateway>()
                    register<NorgGateway>()
                })
            }

            val response = createClient().get("/api/behandling/${behandling.referanse.referanse}/grunnlag/samordning") {
                header("Authorization", "Bearer ${getToken().token()}")
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            assertThat(response.body<SamordningYtelseVurderingGrunnlagDTO>()).isEqualTo(
                SamordningYtelseVurderingGrunnlagDTO(
                    harTilgangTilÅSaksbehandle = true,
                    ytelser = listOf(
                        SamordningYtelseDTO(
                            ytelseType = Ytelse.PLEIEPENGER,
                            kilde = "kilde",
                            saksRef = "saksref",
                            periode =
                                Periode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusYears(1).minusDays(1),
                                ),
                            gradering = 50,
                            endringStatus = EndringStatus.NY
                        )
                    ),
                    vurdering = null,
                    tpYtelser = null,
                    historiskeVurderinger = emptyList(),
                    mottarSykepenger = true,
                    feriePerioder = listOf(Periode(LocalDate.now(), LocalDate.now().plusDays(6))),
                    ferieDager = null,
                )
            )

            val vedtattPeriode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
            val nyPeriode = Periode(LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(3))
            val vurdering = SamordningVurderingGrunnlag(
                begrunnelse = "Samordning med pleiepenger",
                vurderinger = setOf(
                    SamordningVurdering(
                        Ytelse.PLEIEPENGER,
                        setOf(SamordningVurderingPeriode(vedtattPeriode, Prosent(50), manuell = true))
                    )
                ),
                vurdertAv = Bruker("Saksbehandler"),
                vurdertTidspunkt = LocalDateTime.now(),
            )
            InMemorySamordningVurderingRepository.lagreVurderinger(forrigeBehandling.id, vurdering)
            InMemorySamordningVurderingRepository.lagreVurderinger(
                behandling.id,
                vurdering.copy(
                    vurderinger = setOf(
                        SamordningVurdering(
                            Ytelse.PLEIEPENGER,
                            setOf(
                                SamordningVurderingPeriode(vedtattPeriode, Prosent(50), manuell = true),
                                SamordningVurderingPeriode(nyPeriode, Prosent(50), manuell = true)
                            )
                        )
                    )
                )
            )
            InMemoryVedtakRepository.lagre(forrigeBehandling.id, LocalDateTime.now(), LocalDate.now())

            val url = "/api/behandling/${behandling.referanse.referanse}/grunnlag/samordning"
            val førVedtak = createClient().get(url) {
                header("Authorization", "Bearer ${getToken().token()}")
            }.body<SamordningYtelseVurderingGrunnlagDTO>()
            assertThat(førVedtak.vedtattePerioder).isEmpty()

            InMemoryBehandlingRepository.oppdaterBehandlingStatus(forrigeBehandling.id, Status.AVSLUTTET)
            repeat(2) {
                val grunnlag = createClient().get(url) {
                    header("Authorization", "Bearer ${getToken().token()}")
                }.body<SamordningYtelseVurderingGrunnlagDTO>()
                assertThat(grunnlag.vedtattePerioder.map { it.periode }).containsExactly(vedtattPeriode)
                assertThat(grunnlag.vurdering?.vurderinger?.map { it.periode })
                    .containsExactlyInAnyOrder(vedtattPeriode, nyPeriode)
            }
        }
    }
}
