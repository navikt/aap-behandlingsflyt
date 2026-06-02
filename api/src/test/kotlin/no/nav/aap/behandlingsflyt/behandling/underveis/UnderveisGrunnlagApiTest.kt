package no.nav.aap.behandlingsflyt.behandling.underveis

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.behandlingsflyt.utils.diff.Endret
import no.nav.aap.behandlingsflyt.utils.diff.Fjernet
import no.nav.aap.behandlingsflyt.utils.diff.LagtTil
import no.nav.aap.behandlingsflyt.utils.diff.Uendret
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Fakes
class UnderveisGrunnlagApiTest : BaseApiTest() {

    @Test
    fun `henter underveisperioder for behandling`() {
        val ds = MockDataSource()
        val sak = nySak(LocalDate.parse("2025-01-01"))
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        val periode1 = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-14"))
        val periode2 = Periode(LocalDate.parse("2025-01-15"), LocalDate.parse("2025-01-28"))

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(
                underveisperiode(
                    periode = periode1,
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    avslagsarsak = null,
                    trekkPerDag = 2,
                    brukerAvKvoter = setOf(Kvote.ORDINÆR),
                ),
                underveisperiode(
                    periode = periode2,
                    utfall = Utfall.IKKE_OPPFYLT,
                    rettighetsType = null,
                    avslagsarsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
                    trekkPerDag = 3,
                    brukerAvKvoter = emptySet(),
                ),
            ),
            input = object : Faktagrunnlag {},
        )

        testApplication {
            installApplication {
                underveisVurderingerApi(ds, inMemoryRepositoryRegistry)
            }

            val respons = sendGetRequest("/api/behandling/underveis/${behandling.referanse.referanse}")
            assertThat(respons.status).isEqualTo(HttpStatusCode.OK)

            val actual = respons.body<List<UnderveisperiodeDto>>()
            val expected = listOf(
                expectedDto(
                    periode = periode1,
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    avslagsarsak = null,
                    trekkPerDag = 2,
                    brukerAvKvoter = listOf(Kvote.ORDINÆR),
                ),
                expectedDto(
                    periode = periode2,
                    utfall = Utfall.IKKE_OPPFYLT,
                    rettighetsType = null,
                    avslagsarsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
                    trekkPerDag = 3,
                    brukerAvKvoter = emptyList(),
                ),
            )

            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        }
    }

    @Test
    fun `underveis med diff skal gi lagt til for første behandling`() {
        val ds = MockDataSource()
        val sak = nySak(LocalDate.parse("2025-01-01"))
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        val periode1 = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-14"))
        val periode2 = Periode(LocalDate.parse("2025-01-15"), LocalDate.parse("2025-01-28"))

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(
                underveisperiode(
                    periode = periode1,
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    avslagsarsak = null,
                    trekkPerDag = 4,
                    brukerAvKvoter = setOf(Kvote.ORDINÆR),
                ),
                underveisperiode(
                    periode = periode2,
                    utfall = Utfall.IKKE_OPPFYLT,
                    rettighetsType = null,
                    avslagsarsak = UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP,
                    trekkPerDag = 5,
                    brukerAvKvoter = setOf(Kvote.SYKEPENGEERSTATNING),
                ),
            ),
            input = object : Faktagrunnlag {},
        )

        testApplication {
            installApplication {
                underveisVurderingerApi(ds, inMemoryRepositoryRegistry)
            }

            val response = sendGetRequest("/api/behandling/underveis-med-diff/${behandling.referanse.referanse}")
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            val actual = response.body<UnderveisGrunnlagMedDiffDto>()
            val expected = UnderveisGrunnlagMedDiffDto(
                listOf(
                    LagtTil(
                        expectedDto(
                            periode = periode1,
                            utfall = Utfall.OPPFYLT,
                            rettighetsType = RettighetsType.BISTANDSBEHOV,
                            avslagsarsak = null,
                            trekkPerDag = 4,
                            brukerAvKvoter = listOf(Kvote.ORDINÆR),
                        )
                    ),
                    LagtTil(
                        expectedDto(
                            periode = periode2,
                            utfall = Utfall.IKKE_OPPFYLT,
                            rettighetsType = null,
                            avslagsarsak = UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP,
                            trekkPerDag = 5,
                            brukerAvKvoter = listOf(Kvote.SYKEPENGEERSTATNING),
                        )
                    ),
                )
            )

            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        }
    }

    @Test
    fun `underveis med diff skal generere lagt til uendret endret og fjernet`() {
        val ds = MockDataSource()
        val sak = nySak(LocalDate.parse("2025-01-01"))
        val forrigeBehandling = opprettBehandling(sak, TypeBehandling.Revurdering)
        val gjeldendeBehandling = opprettBehandling(
            sak,
            TypeBehandling.Revurdering,
            forrigeBehandlingId = forrigeBehandling.id,
        )

        val lagtTilPeriode = Periode(LocalDate.parse("2024-12-18"), LocalDate.parse("2024-12-31"))
        val uendretPeriode = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-14"))
        val endretPeriode = Periode(LocalDate.parse("2025-01-15"), LocalDate.parse("2025-01-28"))
        val fjernetPeriode = Periode(LocalDate.parse("2025-01-29"), LocalDate.parse("2025-02-11"))

        InMemoryUnderveisRepository.lagre(
            behandlingId = forrigeBehandling.id,
            underveisperioder = listOf(
                underveisperiode(
                    periode = uendretPeriode,
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    avslagsarsak = null,
                    trekkPerDag = 4,
                    brukerAvKvoter = setOf(Kvote.ORDINÆR),
                ),
                underveisperiode(
                    periode = endretPeriode,
                    utfall = Utfall.IKKE_OPPFYLT,
                    rettighetsType = null,
                    avslagsarsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
                    trekkPerDag = 6,
                    brukerAvKvoter = emptySet(),
                ),
                underveisperiode(
                    periode = fjernetPeriode,
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.STUDENT,
                    avslagsarsak = null,
                    trekkPerDag = 2,
                    brukerAvKvoter = setOf(Kvote.ORDINÆR),
                ),
            ),
            input = object : Faktagrunnlag {},
        )

        InMemoryUnderveisRepository.lagre(
            behandlingId = gjeldendeBehandling.id,
            underveisperioder = listOf(
                underveisperiode(
                    periode = lagtTilPeriode,
                    utfall = Utfall.IKKE_OPPFYLT,
                    rettighetsType = null,
                    avslagsarsak = UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP,
                    trekkPerDag = 3,
                    brukerAvKvoter = setOf(Kvote.SYKEPENGEERSTATNING),
                ),
                underveisperiode(
                    periode = uendretPeriode,
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    avslagsarsak = null,
                    trekkPerDag = 4,
                    brukerAvKvoter = setOf(Kvote.ORDINÆR),
                ),
                underveisperiode(
                    periode = endretPeriode,
                    utfall = Utfall.IKKE_OPPFYLT,
                    rettighetsType = null,
                    avslagsarsak = UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP,
                    trekkPerDag = 8,
                    brukerAvKvoter = setOf(Kvote.SYKEPENGEERSTATNING),
                ),
            ),
            input = object : Faktagrunnlag {},
        )

        testApplication {
            installApplication {
                underveisVurderingerApi(ds, inMemoryRepositoryRegistry)
            }

            val response =
                sendGetRequest("/api/behandling/underveis-med-diff/${gjeldendeBehandling.referanse.referanse}")
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            val actual = response.body<UnderveisGrunnlagMedDiffDto>()
            val expected = UnderveisGrunnlagMedDiffDto(
                listOf(
                    LagtTil(
                        expectedDto(
                            periode = lagtTilPeriode,
                            utfall = Utfall.IKKE_OPPFYLT,
                            rettighetsType = null,
                            avslagsarsak = UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP,
                            trekkPerDag = 3,
                            brukerAvKvoter = listOf(Kvote.SYKEPENGEERSTATNING),
                        )
                    ),
                    Uendret(
                        expectedDto(
                            periode = uendretPeriode,
                            utfall = Utfall.OPPFYLT,
                            rettighetsType = RettighetsType.BISTANDSBEHOV,
                            avslagsarsak = null,
                            trekkPerDag = 4,
                            brukerAvKvoter = listOf(Kvote.ORDINÆR),
                        )
                    ),
                    Endret(
                        fra = expectedDto(
                            periode = endretPeriode,
                            utfall = Utfall.IKKE_OPPFYLT,
                            rettighetsType = null,
                            avslagsarsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
                            trekkPerDag = 6,
                            brukerAvKvoter = emptyList(),
                        ),
                        til = expectedDto(
                            periode = endretPeriode,
                            utfall = Utfall.IKKE_OPPFYLT,
                            rettighetsType = null,
                            avslagsarsak = UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP,
                            trekkPerDag = 8,
                            brukerAvKvoter = listOf(Kvote.SYKEPENGEERSTATNING),
                        ),
                    ),
                    Fjernet(
                        expectedDto(
                            periode = fjernetPeriode,
                            utfall = Utfall.OPPFYLT,
                            rettighetsType = RettighetsType.STUDENT,
                            avslagsarsak = null,
                            trekkPerDag = 2,
                            brukerAvKvoter = listOf(Kvote.ORDINÆR),
                        )
                    ),
                )
            )

            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        }
    }

    private fun expectedDto(
        periode: Periode,
        utfall: Utfall,
        rettighetsType: RettighetsType?,
        avslagsarsak: UnderveisÅrsak?,
        trekkPerDag: Int,
        brukerAvKvoter: List<Kvote>,
    ) = UnderveisperiodeDto(
        periode = periode,
        meldePeriode = periode,
        utfall = utfall,
        rettighetsType = rettighetsType?.let { RettighetsTypeDto(it) },
        avslagsårsak = avslagsarsak,
        gradering = GraderingDto(
            gradering = 70,
            andelArbeid = 30,
            fastsattArbeidsevne = 60,
            grenseverdi = 80,
        ),
        trekk = Dagsatser(trekkPerDag * periode.antallDager()),
        brukerAvKvoter = brukerAvKvoter,
    )

    private fun underveisperiode(
        periode: Periode,
        utfall: Utfall,
        rettighetsType: RettighetsType?,
        avslagsarsak: UnderveisÅrsak?,
        trekkPerDag: Int,
        brukerAvKvoter: Set<Kvote>,
    ) = Underveisperiode(
        periode = periode,
        meldePeriode = periode,
        utfall = utfall,
        rettighetsType = rettighetsType,
        avslagsårsak = avslagsarsak,
        grenseverdi = Prosent(80),
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal("14.0")),
            andelArbeid = Prosent(30),
            fastsattArbeidsevne = Prosent(60),
            gradering = Prosent(70),
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(trekkPerDag),
        brukerAvKvoter = brukerAvKvoter,
        institusjonsoppholdReduksjon = Prosent(0),
        meldepliktStatus = MeldepliktStatus.MELDT_SEG,
        meldepliktGradering = Prosent(0),
    )

    private suspend fun ApplicationTestBuilder.sendGetRequest(path: String): HttpResponse {
        val client = createClient()
        return client.get(path) {
            header("Authorization", "Bearer ${getToken().token()}")
            contentType(ContentType.Application.Json)
        }
    }
}