package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.RettighetstypeSteg
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.lookup.repository.RepositoryProvider
import javax.sql.DataSource

class StansEllerOpphørMigrering(
    val dataSource: DataSource,
    val repositoryRegistry: RepositoryRegistry,
    val gatewayProvider: GatewayProvider
) {
    fun migrer() {
        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val behandlingService = BehandlingService(repositoryProvider, gatewayProvider)
            val sakId = connection.queryFirstOrNull(
                """
                SELECT SAK_ID FROM BEHANDLING
                WHERE NOT EXISTS(
                    SELECT * FROM stans_opphor_grunnlag WHERE behandling_id = behandling.id
                ) LIMIT 1
                """.trimIndent()
            ) {
                setRowMapper {
                    SakId(it.getLong("sak_id"))
                }
            }

            if (sakId == null) {
                return@transaction
            }

            val taSkriveLåsRepository = repositoryProvider.provide<TaSkriveLåsRepository>()
            val sakLås = taSkriveLåsRepository.låsSak(sakId)
            val sak = repositoryProvider.provide<SakRepository>().hent(sakId)

            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val behandlinger = behandlingRepository.hentAlleFor(
                sak.id,
                TypeBehandling.ytelseBehandlingstyper()
            )
            val sortedBehandlinger = behandlinger.sortedWith(behandlingService.comparator(behandlinger))

            for (behandling in sortedBehandlinger) {
                val behandlingLås = taSkriveLåsRepository.låsBehandling(behandling.id)
                migrerBehandling(behandling, repositoryProvider)
                taSkriveLåsRepository.verifiserSkrivelås(behandlingLås)
            }

            taSkriveLåsRepository.verifiserSkrivelås(sakLås)
        }
    }


    fun migrerBehandling(behandling: Behandling, repositoryProvider: RepositoryProvider) {
        val stansOpphørRepository = repositoryProvider.provide<StansOpphørRepository>()
        if (stansOpphørRepository.hentHvisEksisterer(behandling.id) != null) {
            return
        }

        val rettighetsPeriode = repositoryProvider.provide<VilkårsresultatRepository>()
            .hent(behandling.id)
            .optionalVilkår(Vilkårtype.ALDERSVILKÅRET)
            ?.tidslinje()
            ?.helePerioden()
            ?: return /* mangler aldersvilkåret, så dette må være førstegangsbehandling som er stanset før aldersvilkåret. Trenger derfor ikke å gjøre noe. */

        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

        val forrigeBehandlingId = behandlingRepository
            .hent(behandling.id)
            .forrigeBehandlingId

        if (forrigeBehandlingId != null && behandling.flyt().erStegFør(behandling.aktivtSteg(), StegType.FASTSETT_RETTIGHETSTYPE)) {
            stansOpphørRepository.kopier(
                fraBehandling = forrigeBehandlingId,
                tilBehandling = behandling.id,
            )
        } else {
            RettighetstypeSteg(repositoryProvider, gatewayProvider)
                .lagreStansOgOpphør(
                    behandling.id,
                    forrigeBehandlingId,
                    behandling.typeBehandling(),
                    rettighetsPeriode
                )
        }
    }
}