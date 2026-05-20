package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.orEmpty
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

// antall behandlinger i prod: 2347
class MigreringManglendeDokumentasjon(
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider,
    private val dataSource: DataSource,
) {
    private val unleashGateway = gatewayProvider.provide<UnleashGateway>()
    private val log = LoggerFactory.getLogger(javaClass)


    fun migrer(): ScheduledExecutorService {
        val scheduler = Executors.newScheduledThreadPool(1)
        scheduler.scheduleWithFixedDelay(Runnable {
            val isLeader = isLeader(log)
            log.info("isLeader = $isLeader")

            if (unleashGateway.isEnabled(BehandlingsflytFeature.MigrerManglendeDokumentasjon) && isLeader) {
                try {
                    utførMigrering()
                } catch (e: Exception) {
                    log.warn("feil ved migrering av manglende dokumentasjon: {}", e.javaClass.name, e)
                }
            }

        }, 1, 9, TimeUnit.MINUTES)
        return scheduler
    }

    fun utførMigrering() {
        var ferdig = false
        var minimumBehandlingId = BehandlingId(0)
        while (!ferdig) {
            dataSource.transaction { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val taSkriveLåsRepository = repositoryProvider.provide<TaSkriveLåsRepository>()
                val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
                val sykdomsRepository = repositoryProvider.provide<SykdomRepository>()

                val behandlingId = connection.queryFirstOrNull(
                    """
                    select behandling.id
                    from behandling
                    join vilkar_resultat on behandling.id = vilkar_resultat.behandling_id
                    join vilkar on vilkar_resultat.id = vilkar.resultat_id
                    join vilkar_periode on vilkar.id = vilkar_periode.vilkar_id
                    where
                        vilkar_resultat.aktiv
                        and vilkar_periode.avslagsarsak = 'MANGLENDE_DOKUMENTASJON'
                        and behandling.id > ?
                    order by behandling.id
                    limit 1
                """
                ) {
                    setParams { setLong(1, minimumBehandlingId.id) }
                    setRowMapper { BehandlingId(it.getLong("id")) }
                }

                if (behandlingId == null) {
                    ferdig = true
                    log.info("MigreringManglendeDokumentasjon fant ingen flere behandlinger å migrere")
                    return@transaction
                }

                taSkriveLåsRepository.withLåstBehandling(behandlingId) { _ ->
                    val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
                    val sykdomsvilkaret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
                    val vilkårsvurderinger = sykdomsvilkaret.tidslinje()
                        .filter { it.verdi.avslagsårsak == Avslagsårsak.MANGLENDE_DOKUMENTASJON }

                    val vurderinger = sykdomsRepository.hentHvisEksisterer(behandlingId)
                        ?.somSykdomsvurderingstidslinje()
                        .orEmpty()

                    /* sjekk om avslagsårsak fortsatt finnes nå som vi har låst behandlingen. */
                    if (vilkårsvurderinger.isEmpty()) {
                        return@withLåstBehandling
                    }

                    val oppdatertePerioder =
                        vilkårsvurderinger.leftJoin(vurderinger) { vilkårsvurdering, sykdomsvurdering ->
                            require(vilkårsvurdering.avslagsårsak == Avslagsårsak.MANGLENDE_DOKUMENTASJON) {
                                "forventet MANGLENDE_DOKUMENTASJON, fikk ${vilkårsvurdering.avslagsårsak} for $behandlingId"
                            }
                            Vilkårsvurdering(
                                utfall = vilkårsvurdering.utfall,
                                manuellVurdering = vilkårsvurdering.manuellVurdering,
                                begrunnelse = vilkårsvurdering.begrunnelse,
                                innvilgelsesårsak = vilkårsvurdering.innvilgelsesårsak,
                                avslagsårsak = when {
                                    sykdomsvurdering?.harSkadeSykdomEllerLyte == false -> Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE
                                    else -> Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE
                                },
                                faktagrunnlag = vilkårsvurdering.faktagrunnlag(),
                            )
                        }
                    sykdomsvilkaret.leggTilVurderinger(oppdatertePerioder)
                    vilkårsresultatRepository.lagre(behandlingId, vilkårsresultat)
                    minimumBehandlingId = behandlingId
                    log.info("migrerte $behandlingId")
                }
            }
            Thread.sleep(Duration.ofSeconds(2))
        }
    }
}
