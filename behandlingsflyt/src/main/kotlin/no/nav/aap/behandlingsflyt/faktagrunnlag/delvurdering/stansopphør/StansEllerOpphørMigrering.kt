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
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class StansEllerOpphørMigrering(
    val dataSource: DataSource,
    val repositoryRegistry: RepositoryRegistry,
    val gatewayProvider: GatewayProvider
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun migrer() {
        log.info("starter migrering av stans og opphør")
        var ferdig = false
        var antallMigreringer = 0
        val sakIdSett = HashSet<Long>()

        while (!ferdig) {
            dataSource.transaction { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val stansEllerOpphørMigreringService = StansEllerOpphørMigreringService(repositoryProvider, gatewayProvider)

                val sakId = connection.queryFirstOrNull(
                    """
                    SELECT SAK_ID FROM BEHANDLING
                    WHERE NOT EXISTS(
                        SELECT *
                        FROM stans_opphor_grunnlag
                        WHERE behandling_id = behandling.id
                    )
                      AND behandling.type IN ('ae0034', 'ae0028')
                      AND NOT EXISTS(
                        SELECT *
                        FROM trukket_soknad_grunnlag
                                 JOIN trukket_soknad_vurderinger ON trukket_soknad_grunnlag.vurderinger_id = trukket_soknad_vurderinger.id
                                 JOIN trukket_Soknad_vurdering ON trukket_soknad_vurderinger.id = trukket_Soknad_vurdering.vurderinger_id
                        WHERE
                                   trukket_soknad_grunnlag.behandling_id = behandling.id
                          AND trukket_soknad_grunnlag.aktiv
                          AND trukket_soknad_vurdering.skal_trekkes
                    )
                    LIMIT 1
                """.trimIndent()
                ) {
                    setRowMapper {
                        SakId(it.getLong("sak_id"))
                    }
                }

                if (sakId == null) {
                    ferdig = true
                    log.info("Ferdig med $antallMigreringer migrering av stans og opphør")
                    return@transaction
                }
                if (sakId.id in sakIdSett) {
                    log.info("Trukket samme sak id etter den er migrert, så migrering / utplukk feilet")
                    ferdig = true
                    return@transaction
                }

                sakIdSett += sakId.id

                stansEllerOpphørMigreringService.migrerSak(sakId)
                antallMigreringer += 1

                if (antallMigreringer % 10 == 0) {
                    log.info("Pågående migrering, $antallMigreringer utført")
                }
            }
        }
    }
}

class StansEllerOpphørMigreringService(
    val stansOpphørRepository: StansOpphørRepository,
    val vilkårsresultatRepository: VilkårsresultatRepository,
    val behandlingRepository: BehandlingRepository,
    val rettighetstypeSteg: RettighetstypeSteg,
    val behandlingService: BehandlingService,
    val taSkriveLåsRepository: TaSkriveLåsRepository,
    val sakRepository: SakRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        stansOpphørRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        rettighetstypeSteg = RettighetstypeSteg(repositoryProvider, gatewayProvider),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        taSkriveLåsRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
    )

    fun migrerSak(sakId: SakId) {
        val sakLås = taSkriveLåsRepository.låsSak(sakId)
        val sak = sakRepository.hent(sakId)

        val behandlinger = behandlingRepository.hentAlleFor(
            sak.id,
            TypeBehandling.ytelseBehandlingstyper()
        )
        val sortedBehandlinger = behandlinger.sortedWith(behandlingService.comparator(behandlinger))

        for (behandling in sortedBehandlinger) {
            val behandlingLås = taSkriveLåsRepository.låsBehandling(behandling.id)
            migrerBehandling(behandling)
            taSkriveLåsRepository.verifiserSkrivelås(behandlingLås)
        }

        taSkriveLåsRepository.verifiserSkrivelås(sakLås)
    }

    fun migrerBehandling(behandling: Behandling) {
        if (stansOpphørRepository.hentHvisEksisterer(behandling.id) != null) {
            return
        }

        val rettighetsPeriode = vilkårsresultatRepository
            .hent(behandling.id)
            .optionalVilkår(Vilkårtype.ALDERSVILKÅRET)
            ?.tidslinje()
            ?.helePerioden()
            ?: return /* mangler aldersvilkåret, så dette må være førstegangsbehandling som er stanset før aldersvilkåret. Trenger derfor ikke å gjøre noe. */


        val forrigeBehandlingId = behandlingRepository
            .hent(behandling.id)
            .forrigeBehandlingId

        if (forrigeBehandlingId != null && behandling.flyt()
                .erStegFør(behandling.aktivtSteg(), StegType.FASTSETT_RETTIGHETSTYPE)
        ) {
            stansOpphørRepository.kopier(
                fraBehandling = forrigeBehandlingId,
                tilBehandling = behandling.id,
            )
        } else {
            rettighetstypeSteg
                .lagreStansOgOpphør(
                    behandling.id,
                    forrigeBehandlingId,
                    behandling.typeBehandling(),
                    rettighetsPeriode
                )
        }
    }
}