package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.utledStansEllerOpphør
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.RettighetstypeSteg
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.IVERKSETTES
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.OPPRETTET
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.UTREDES
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling.Revurdering
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.lås.TaSkriveLåsRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import org.slf4j.LoggerFactory
import java.time.Duration
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("no.nav.aap.behandlingsflyt.BackfillStansOpphør")
private val teamLogs = LoggerFactory.getLogger("team-logs")

class BackfillStansOpphør(
    private val dataSource: DataSource,
    private val gatewayProvider: GatewayProvider,
) {
    val unleashGateway = gatewayProvider.provide<UnleashGateway>()


    fun kjør() {
        Thread.ofVirtual()
            .name("backfillStansOpphor")
            .uncaughtExceptionHandler { _, throwable ->
                log.warn("BackfillStansOpphør: uncaughtException {}, se secure / team log", throwable.javaClass.name)
                teamLogs.warn(
                    "BackfillStansOpphør: uncaughtException {}: {}",
                    throwable.javaClass.name,
                    throwable.message,
                    throwable
                )
            }.start {
                var forrigeFraTil: List<Long>? = null
                while (true) {
                    if (isLeader(log) && unleashGateway.isEnabled(BehandlingsflytFeature.BackfillStansOpphor)) {
                        val fraTil = unleashGateway.getVariantValue(
                            BehandlingsflytFeature.BackfillStansOpphor,
                            "backfill-behandling-ider"
                        ).split(",").map(String::toLong)

                        if (forrigeFraTil != fraTil) {
                            backfillStansOpphørLoop(dataSource, fraTil[0], fraTil[1])
                        }
                        forrigeFraTil = fraTil
                    }
                    Thread.sleep(Duration.ofMinutes(5))
                }
            }
    }

    private var antallBackfillUtført = 0
    private fun backfillStansOpphørLoop(dataSource: DataSource, fra: Long, til: Long) {
        log.info("Begynner backfill for $fra – $til")
        antallBackfillUtført = 0

        for (behandlingId in fra..til) {
            dataSource.transaction { connection ->
                val behandlingRepository = BehandlingRepositoryImpl(connection)
                val behandling = behandlingRepository.hentKandidatForStansOpphørBackfill(behandlingId)
                    ?: return@transaction

                if (behandling.typeBehandling() == Revurdering) {
                    backfillBehandling(connection, behandling)
                } else {
                    require(behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling)

                    when (behandling.status()) {
                        OPPRETTET -> {
                            error("kandidaten skal være ekskludert av hentKandidatForStansOpphørBackfill")
                        }

                        UTREDES if behandling.flyt().erStegFør(behandling.aktivtSteg(), StegType.FASTSETT_RETTIGHETSTYPE)  -> {
                            /* noop, vi har ikke kommet til steget enda, så det er ikke forventet å finne en verdi */
                        }

                        UTREDES,
                        IVERKSETTES,
                        AVSLUTTET -> {
                            backfillBehandling(connection, behandling)
                        }
                    }
                }
            }
        }
        log.info("Backfill stans/opphør av {} behandlinger. Ingen fler behandlinger for $til – $fra", antallBackfillUtført)
        Thread.sleep(Duration.ofMinutes(5))
    }

    private fun backfillBehandling(connection: DBConnection, behandling: Behandling) {
        val stansOpphørGrunnlagRepository = StansOpphørRepositoryImpl(connection)
        val vilkårsresultatRepository = VilkårsresultatRepositoryImpl(connection)
        val taSkriveLåsRepository = TaSkriveLåsRepositoryImpl(connection)
        val sakRepository = SakRepositoryImpl(connection)
        val rettighetstypeService = RettighetstypeService(postgresRepositoryRegistry.provider(connection), gatewayProvider)
        val grunnlag = stansOpphørGrunnlagRepository.hentHvisEksisterer(behandling.id)


        if (grunnlag?.stansOpphørV2 != null) {
            /* verdi satt, ingen behov for backfill. */
            return
        }

        taSkriveLåsRepository.withLåstBehandling(behandling.id) {
            val grunnlag = stansOpphørGrunnlagRepository.hentHvisEksisterer(behandling.id)

            /* sjekk igjen nå som vi har låst behandlignen. */
            if (grunnlag?.stansOpphørV2 != null) {
                /* verdi satt, ingen behov for backfill. */
                return@withLåstBehandling
            }

            val vilkårsresultat = vilkårsresultatRepository.hent(behandling.id)
            val rettighetsperiode = vilkårsresultat.optionalVilkår(Vilkårtype.ALDERSVILKÅRET)
                ?.tidslinje()
                ?.takeIf { it.isNotEmpty() }
                ?.helePerioden()
                ?: sakRepository.hent(behandling.sakId).rettighetsperiode

            val stansOpphør = utledStansEllerOpphør(vilkårsresultat, rettighetsperiode = rettighetsperiode)
            val nyttGrunnlag = (grunnlag ?: StansOpphørGrunnlag().utledNyttGrunnlag(stansOpphør, behandling.id))
                .copy(stansOpphørV2 = stansOpphør)

            check(RettighetstypeSteg.validerStansOpphør(
                nyttGrunnlag,
                rettighetstyper = rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(behandling.id)
            )) {
                "validering etter backfill av stans/opphør feilet for ${behandling.id}"
            }

            stansOpphørGrunnlagRepository.lagre(
                behandling.id,
                nyttGrunnlag
            )
            antallBackfillUtført += 1
            if (antallBackfillUtført % 1000 == 0) {
                log.info("Backfillet {} behandlinger med stans/opphør", antallBackfillUtført)
            }
        }
    }
}