package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.integrasjon.defaultGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusForeldrepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusSykepengerGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering.SamordningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@Fakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SamordningYtelseVurderingServiceTest {

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `krever avklaring når endringer kommer`() {
        dataSource.transaction { connection ->
            val ytelseRepo = SamordningYtelseRepositoryImpl(connection)
            val repo = SamordningVurderingRepositoryImpl(connection)
            val samordningYtelseVurderingInformasjonskrav = SamordningYtelseVurderingInformasjonskrav(
                SamordningYtelseRepositoryImpl(connection),
                FakeTidligereVurderinger(),
                AbakusForeldrepengerGateway(),
                AbakusSykepengerGateway(),
                SakOgBehandlingService(postgresRepositoryRegistry.provider(connection), defaultGatewayProvider()),
            )
            val kontekst = opprettSakdata(connection)

            // Når vi for første gang får opplysninger er det en endring
            val ingenData = klargjørOgOppdater(samordningYtelseVurderingInformasjonskrav, kontekst)
            assertThat(ingenData).isEqualTo(Informasjonskrav.Endret.ENDRET)

            // Data er uforandret
            val sammeData = klargjørOgOppdater(samordningYtelseVurderingInformasjonskrav, kontekst)
            assertThat(sammeData).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)

            // Ny data har kommet inn
            opprettYtelseData(ytelseRepo, kontekst.behandlingId)
            opprettVurderingData(repo, kontekst.behandlingId)
            val nyData = klargjørOgOppdater(samordningYtelseVurderingInformasjonskrav, kontekst)
            assertThat(nyData).isEqualTo(Informasjonskrav.Endret.ENDRET)
        }
    }

    private fun klargjørOgOppdater(
        samordningYtelseVurderingInformasjonskrav: SamordningYtelseVurderingInformasjonskrav,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        val input = samordningYtelseVurderingInformasjonskrav.klargjør(kontekst)
        val data = samordningYtelseVurderingInformasjonskrav.hentData(input)
        val ingenData = samordningYtelseVurderingInformasjonskrav.oppdater(input, data, kontekst)
        return ingenData
    }

    @Test
    fun `Rekkefølge i lister skal ikke gi endring`() {
        val nå = LocalDate.now()

        val eksisterendeGrunnlag = SamordningYtelseGrunnlag(
            1, listOf(
                SamordningYtelse(
                    Ytelse.SYKEPENGER,
                    setOf(
                        SamordningYtelsePeriode(
                            Periode(nå.plusDays(5), nå.plusDays(10)),
                            Prosent(50),
                            0
                        ),
                        SamordningYtelsePeriode(
                            Periode(nå.plusDays(5), nå.plusDays(10)),
                            Prosent(40),
                            0
                        ),
                    ),
                    "kilde",
                    "ref"
                ),
                SamordningYtelse(
                    Ytelse.SYKEPENGER,
                    setOf(
                        SamordningYtelsePeriode(
                            Periode(nå.plusDays(5), nå.plusDays(10)),
                            Prosent(50),
                            0
                        ),
                    ),
                    "kilde",
                    "ref"
                ),
            )
        )

        val ny = listOf(
            SamordningYtelse(
                Ytelse.SYKEPENGER,
                setOf(
                    SamordningYtelsePeriode(
                        Periode(nå.plusDays(5), nå.plusDays(10)),
                        Prosent(50),
                        0
                    ),
                ),
                "kilde",
                "ref"
            ),
            SamordningYtelse(
                Ytelse.SYKEPENGER,
                setOf(
                    SamordningYtelsePeriode(
                        Periode(nå.plusDays(5), nå.plusDays(10)),
                        Prosent(40),
                        0
                    ),
                    SamordningYtelsePeriode(
                        Periode(nå.plusDays(5), nå.plusDays(10)),
                        Prosent(50),
                        0
                    ),
                ),
                "kilde",
                "ref"
            ),
        )

        assertThat(
            SamordningYtelseVurderingInformasjonskrav.harEndringerIYtelser(
                eksisterendeGrunnlag, ny
            )
        ).isFalse()
    }

    private fun opprettVurderingData(samordningVurderingRepo: SamordningVurderingRepositoryImpl, behandlingId: BehandlingId) {
        samordningVurderingRepo.lagreVurderinger(
            behandlingId,
            SamordningVurderingGrunnlag(
                begrunnelse = "En god begrunnelse",
                maksDatoEndelig = false,
                fristNyRevurdering = LocalDate.now().plusYears(1),
                vurderinger = listOf(
                    SamordningVurdering(
                        Ytelse.SYKEPENGER,

                        listOf(
                            SamordningVurderingPeriode(
                                Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                                Prosent(50),
                                0,
                                false
                            )
                        )
                    )
                ),
                vurdertAv = "ident"
            )
        )
    }

    private fun opprettYtelseData(samordningYtelseRepo: SamordningYtelseRepositoryImpl, behandlingId: BehandlingId) {
        samordningYtelseRepo.lagre(
            behandlingId,
            listOf(
                SamordningYtelse(
                    Ytelse.SYKEPENGER,
                    setOf(
                        SamordningYtelsePeriode(
                            Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                            Prosent(50),
                            0
                        )
                    ),
                    "kilde",
                    "ref"
                )
            )
        )
    }

    private fun opprettSakdata(connection: DBConnection): FlytKontekstMedPerioder {
        val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("ident", true)))
        val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusDays(5))
        val sakId = SakRepositoryImpl(connection).finnEllerOpprett(
            person,
            rettighetsperiode
        ).id
        val behandlingId = BehandlingRepositoryImpl(connection).opprettBehandling(
            sakId,
            TypeBehandling.Førstegangsbehandling,
            null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            ),
        ).id
        return FlytKontekstMedPerioder(
            sakId, behandlingId, null, TypeBehandling.Førstegangsbehandling,
            VurderingType.FØRSTEGANGSBEHANDLING, rettighetsperiode, emptySet()
        )
    }
}