package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.RettighetstypeSteg
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
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


            val låsRepo = repositoryProvider.provide<TaSkriveLåsRepository>()
            val lås = låsRepo.låsSak(sakId)
            val sak = repositoryProvider.provide<SakRepository>().hent(sakId)

            val behandlingRepo = repositoryProvider.provide<BehandlingRepository>()
            val behandlinger = behandlingRepo.hentAlleMedVedtakFor(
                sak.person,
                TypeBehandling.ytelseBehandlingstyper()
            ).sortedBy { it.vedtakstidspunkt }

            behandlinger.map { behandling ->
                val behandlingLås = låsRepo.låsBehandling(behandling.id)
                migrerBehandling(behandling, repositoryProvider)
                låsRepo.verifiserSkrivelås(behandlingLås)
            }

            låsRepo.verifiserSkrivelås(lås)
        }
    }


    fun migrerBehandling(behandling: BehandlingMedVedtak, repositoryProvider: RepositoryProvider) {
        val stansOpphørRepo = repositoryProvider.provide<StansOpphørRepository>()
        if (stansOpphørRepo.hentHvisEksisterer(behandling.id) != null) {
            return
        }

        val rettighetsPeriode = repositoryProvider.provide<VilkårsresultatRepository>()
            .hent(behandling.id)
            .optionalVilkår(Vilkårtype.ALDERSVILKÅRET)!!
            .tidslinje()
            .helePerioden()

        val forrigeBehandlingId = repositoryProvider.provide<BehandlingRepository>()
            .hent(behandling.id)
            .forrigeBehandlingId

        RettighetstypeSteg(repositoryProvider, gatewayProvider)
            .lagreStansOgOpphør(
                behandling.id,
                forrigeBehandlingId,
                behandling.typeBehandling,
                rettighetsPeriode
            )
    }
}