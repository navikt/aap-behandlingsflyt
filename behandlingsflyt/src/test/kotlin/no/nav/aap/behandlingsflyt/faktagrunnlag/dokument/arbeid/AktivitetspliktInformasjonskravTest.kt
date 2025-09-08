package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7RepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleashFasttrackAktivitetsplikt
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDatabase
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.lookup.repository.RepositoryProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import javax.sql.DataSource

class AktivitetspliktInformasjonskravTest {
    private val repositoryRegistry = RepositoryRegistry()
        .register<TrukketSøknadRepositoryImpl>()
        .register<AvklaringsbehovRepositoryImpl>()
        .register<Aktivitetsplikt11_7RepositoryImpl>()
        .register<VilkårsresultatRepositoryImpl>()
        .register<BehandlingRepositoryImpl>()

    @TestDatabase
    lateinit var dataSource: DataSource

    @Test
    fun `Revurdering med vurderingstype 'EFFEKTUER_AKTIVITETSPLIKT' skal kopiere grunnlag fra nyeste iverksatte aktvitetspliktbehandling`() {
        val sak = dataSource.transaction { connection -> nySak(connection) }

        val aktivitetspliktBehandling1 = opprettAktivitetspliktBehandlingMedVurdering(
            sak, status = Status.AVSLUTTET,
            Aktivitetsplikt11_7Vurdering(
                begrunnelse = "Begrunnelse 1",
                erOppfylt = false,
                utfall = Utfall.STANS,
                gjelderFra = sak.rettighetsperiode.fom.plusMonths(2),
                vurdertAv = "Saksbehandler",
                opprettet = sak.rettighetsperiode.fom.plusMonths(2).plusWeeks(1).atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
            )
        )

        val aktivitetspliktBehandling2 = opprettAktivitetspliktBehandlingMedVurdering(
            sak, status = Status.IVERKSETTES,
            Aktivitetsplikt11_7Vurdering(
                begrunnelse = "Begrunnelse 2",
                erOppfylt = false,
                utfall = Utfall.OPPHØR,
                gjelderFra = sak.rettighetsperiode.fom.plusMonths(3),
                vurdertAv = "Saksbehandler",
                opprettet = sak.rettighetsperiode.fom.plusMonths(3).plusWeeks(1).atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
            ),
            forrige = aktivitetspliktBehandling1.id
        )

        opprettAktivitetspliktBehandlingMedVurdering(
            sak, status = Status.UTREDES,
            Aktivitetsplikt11_7Vurdering(
                begrunnelse = "Begrunnelse 3",
                erOppfylt = true,
                gjelderFra = sak.rettighetsperiode.fom.plusMonths(3),
                vurdertAv = "Saksbehandler",
                opprettet = sak.rettighetsperiode.fom.plusMonths(3).plusWeeks(1).atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
            ),
            forrige = aktivitetspliktBehandling2.id
        )

        dataSource.transaction { connection ->
            val effektueringsbehandling = BehandlingRepositoryImpl(connection).opprettBehandling(
                sak.id, TypeBehandling.Revurdering,
                forrigeBehandlingId = null,
                VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT)),
                    årsak = ÅrsakTilOpprettelse.AKTIVITETSPLIKT,
                )
            )

            val aktivitetspliktInformasjonskrav = AktivitetspliktInformasjonskrav.konstruer(
                repositoryRegistry.provider(connection),
                createGatewayProvider { register<FakeUnleashFasttrackAktivitetsplikt>() },
            )
            val flytKontekstMedPerioder = flytKontekstMedPerioder(effektueringsbehandling, sak)


            assertThat(
                aktivitetspliktInformasjonskrav.erRelevant(
                    flytKontekstMedPerioder,
                    StegType.AVKLAR_SYKDOM,
                    null
                )
            ).isTrue

            aktivitetspliktInformasjonskrav.oppdater(flytKontekstMedPerioder)
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
                                    .toInstant(ZoneOffset.UTC)
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
        vurdering: Aktivitetsplikt11_7Vurdering,
        forrige: BehandlingId? = null,
    ): Behandling {
        return dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val Aktivitetsplikt11_7Repository = repositoryProvider.provide<Aktivitetsplikt11_7Repository>()

            val behandling = opprettAktivitetspliktBehandling(repositoryProvider, sak, forrige)

            Aktivitetsplikt11_7Repository.lagre(
                behandling.id, listOf(vurdering)
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

}