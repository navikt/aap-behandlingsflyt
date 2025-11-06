package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.IngenInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.IngenRegisterData
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7RepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset

class AktivitetspliktInformasjonskravTest {
        companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    @Test
    fun `Revurdering med vurderingstype 'EFFEKTUER_AKTIVITETSPLIKT' skal kopiere grunnlag fra nyeste iverksatte aktvitetspliktbehandling`() {
        val sak = dataSource.transaction { connection -> nySak(connection) }

        val aktivitetspliktBehandling1 = opprettAktivitetspliktBehandlingMedVurdering(
            sak, status = Status.AVSLUTTET
        ) { behandlingId ->
            Aktivitetsplikt11_7Vurdering(
                begrunnelse = "Begrunnelse 1",
                erOppfylt = false,
                utfall = Utfall.STANS,
                gjelderFra = sak.rettighetsperiode.fom.plusMonths(2),
                vurdertAv = "Saksbehandler",
                opprettet = sak.rettighetsperiode.fom.plusMonths(2).plusWeeks(1).atStartOfDay()
                    .toInstant(ZoneOffset.UTC),
                vurdertIBehandling = behandlingId,
                skalIgnorereVarselFrist = false
            )
        }

        val aktivitetspliktBehandling2 = opprettAktivitetspliktBehandlingMedVurdering(
            sak, status = Status.IVERKSETTES, forrige = aktivitetspliktBehandling1.id
        ) { behandlingId ->
            Aktivitetsplikt11_7Vurdering(
                begrunnelse = "Begrunnelse 2",
                erOppfylt = false,
                utfall = Utfall.OPPHØR,
                gjelderFra = sak.rettighetsperiode.fom.plusMonths(3),
                vurdertAv = "Saksbehandler",
                opprettet = sak.rettighetsperiode.fom.plusMonths(3).plusWeeks(1).atStartOfDay()
                    .toInstant(ZoneOffset.UTC),
                vurdertIBehandling = behandlingId,
                skalIgnorereVarselFrist = false
            )
        }


        opprettAktivitetspliktBehandlingMedVurdering(
            sak, status = Status.UTREDES,
            forrige = aktivitetspliktBehandling2.id
        ) { behandlingId ->
            Aktivitetsplikt11_7Vurdering(
                begrunnelse = "Begrunnelse 3",
                erOppfylt = true,
                gjelderFra = sak.rettighetsperiode.fom.plusMonths(3),
                vurdertAv = "Saksbehandler",
                opprettet = sak.rettighetsperiode.fom.plusMonths(3).plusWeeks(1).atStartOfDay()
                    .toInstant(ZoneOffset.UTC),
                vurdertIBehandling = behandlingId,
                skalIgnorereVarselFrist = false
            )
        }


        dataSource.transaction { connection ->
            val effektueringsbehandling = BehandlingRepositoryImpl(connection).opprettBehandling(
                sak.id, TypeBehandling.Revurdering,
                forrigeBehandlingId = null,
                VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT)),
                    årsak = ÅrsakTilOpprettelse.AKTIVITETSPLIKT,
                )
            )

            val aktivitetsplikt11_7Informasjonskrav = Aktivitetsplikt11_7Informasjonskrav.konstruer(
                postgresRepositoryRegistry.provider(connection),
                createGatewayProvider { register<FakeUnleash>() },
            )
            val flytKontekstMedPerioder = flytKontekstMedPerioder(effektueringsbehandling, sak)


            assertThat(
                aktivitetsplikt11_7Informasjonskrav.erRelevant(
                    flytKontekstMedPerioder,
                    StegType.AVKLAR_SYKDOM,
                    null
                )
            ).isTrue

            aktivitetsplikt11_7Informasjonskrav.oppdater(IngenInput, IngenRegisterData, flytKontekstMedPerioder)
                .let {
                    assertThat(it)
                        .describedAs { "Skal ikke returnere endret ved oppdatering i effektueringsbehandling" }
                        .isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
                }

            assertThat(
                Aktivitetsplikt11_7RepositoryImpl(connection).hentHvisEksisterer(effektueringsbehandling.id)
            )
                .describedAs { "Skal kopiere grunnlag fra nyeste iverksatte aktivitetspliktbehandling" }
                .isEqualTo(
                    Aktivitetsplikt11_7Grunnlag(
                        listOf(
                            Aktivitetsplikt11_7Vurdering(
                                begrunnelse = "Begrunnelse 2",
                                erOppfylt = false,
                                utfall = Utfall.OPPHØR,
                                gjelderFra = sak.rettighetsperiode.fom.plusMonths(3),
                                vurdertAv = "Saksbehandler",
                                opprettet = sak.rettighetsperiode.fom.plusMonths(3).plusWeeks(1).atStartOfDay()
                                    .toInstant(ZoneOffset.UTC),
                                vurdertIBehandling = aktivitetspliktBehandling2.id,
                                skalIgnorereVarselFrist = false
                            )
                        )
                    )
                )
        }
    }

    private fun flytKontekstMedPerioder(behandling: Behandling, sak: Sak) =
        FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            forrigeBehandlingId = behandling.forrigeBehandlingId,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT),
            rettighetsperiode = sak.rettighetsperiode
        )

    private fun opprettAktivitetspliktBehandlingMedVurdering(
        sak: Sak,
        status: Status,
        forrige: BehandlingId? = null,
        vurdering: (behandlingId: BehandlingId) -> Aktivitetsplikt11_7Vurdering,
    ): Behandling {
        return dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val Aktivitetsplikt11_7Repository = repositoryProvider.provide<Aktivitetsplikt11_7Repository>()

            val behandling = opprettAktivitetspliktBehandling(repositoryProvider, sak, forrige)

            Aktivitetsplikt11_7Repository.lagre(
                behandling.id, listOf(vurdering(behandling.id))
            )
            behandlingRepository.oppdaterBehandlingStatus(behandling.id, status)
            behandling
        }
    }

    private fun opprettAktivitetspliktBehandling(
        repositoryProvider: RepositoryProvider,
        sak: Sak,
        forrige: BehandlingId? = null
    ): Behandling {
        return repositoryProvider.provide<BehandlingRepository>().opprettBehandling(
            sak.id, TypeBehandling.Aktivitetsplikt,
            forrigeBehandlingId = forrige,
            VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.AKTIVITETSPLIKT_11_7)),
                årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
            )
        )
    }

    fun nySak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(
            ident(),
            Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 2, 2))
        )
    }

}
