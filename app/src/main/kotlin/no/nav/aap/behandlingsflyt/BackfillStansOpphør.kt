package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.utledStansEllerOpphør
import no.nav.aap.behandlingsflyt.steg.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.steg.rettighetstype.StansOpphørGrunnlag
import no.nav.aap.underveis.UnderveisÅrsak
import no.nav.aap.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.steg.rettighetstype.RettighetstypeSteg
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.lås.TaSkriveLåsRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
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
            .start {
                var forrigeFraTil: List<Long>? = null
                while (true) {
                    if (isLeader(log) && unleashGateway.isEnabled(BehandlingsflytFeature.BackfillStansOpphor)) {
                        val fraTil = unleashGateway.getVariantValue(
                            BehandlingsflytFeature.BackfillStansOpphor,
                            "backfill-saker-ider"
                        ).split(",").map(String::toLong)

                        if (forrigeFraTil != fraTil) {
                            try {
                                backfillStansOpphørLoop(dataSource, fraTil[0], fraTil[1])
                                forrigeFraTil = fraTil /* anser fra/til som forrige kun hvis vi fullfører backfill */
                            } catch (e: Exception) {
                                log.warn(
                                    "BackfillStansOpphør: uncaughtException {}, se secure / team log",
                                    e.javaClass.name
                                )
                                teamLogs.warn(
                                    "BackfillStansOpphør: uncaughtException {}: {}",
                                    e.javaClass.name,
                                    e.message,
                                    e
                                )
                            }
                        }
                    }
                    Thread.sleep(Duration.ofMinutes(5))
                }
            }
    }

    private var antallBackfillUtført = 0
    private fun backfillStansOpphørLoop(dataSource: DataSource, fra: Long, til: Long) {
        log.info("Begynner backfill for sak-ider $fra – $til")
        antallBackfillUtført = 0

        for (sakId in fra..til) {
            dataSource.transaction { connection ->
                val sakRepository = SakRepositoryImpl(connection)
                val sak = sakRepository.backfillStansOpphørHentHvisFinnes(sakId)
                    ?: return@transaction

                if (Miljø.erDev() && sak.opprettetTidspunkt <= LocalDate.parse("2025-04-01").atStartOfDay()) {
                    return@transaction
                }

                val behandlingService = BehandlingService(postgresRepositoryRegistry.provider(connection), gatewayProvider)
                for (behandling in behandlingService.alleYtelsesbehandlinger(sak.id)) {
                    try {
                        backfillBehandling(connection, behandling)
                    } catch (e: Exception) {
                        throw RuntimeException("sak id=${sak.id.toLong()} nummer=${sak.saksnummer}, behanding id=${behandling.id.toLong()} ref=${behandling.referanse}", e)
                    }
                }
            }
        }
        log.info(
            "Backfill stans/opphør av {} behandlinger. Ingen fler behandlinger for sak-ider $fra – $til",
            antallBackfillUtført
        )
        Thread.sleep(Duration.ofMinutes(5))
    }

    private fun backfillBehandling(connection: DBConnection, behandling: Behandling) {
        val stansOpphørGrunnlagRepository = StansOpphørRepositoryImpl(connection)
        val vilkårsresultatRepository = VilkårsresultatRepositoryImpl(connection)
        val taSkriveLåsRepository = TaSkriveLåsRepositoryImpl(connection)
        val sakRepository = SakRepositoryImpl(connection)
        val underveisRepository = UnderveisRepositoryImpl(connection)
        val rettighetstypeService =
            RettighetstypeService(postgresRepositoryRegistry.provider(connection), gatewayProvider)
        val grunnlag = stansOpphørGrunnlagRepository.hentHvisEksisterer(behandling.id)

        if (behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling
            && behandling.flyt().erStegFør(behandling.aktivtSteg(), StegType.FASTSETT_RETTIGHETSTYPE)
        ) {
            log.info(
                "Ingen backfill for behandling {}: førstegangsbehandling og ikke nådd steget",
                behandling.id.toLong()
            )
            return
        }

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


            if (behandling.typeBehandling() == TypeBehandling.Revurdering
                && behandling.flyt().erStegFør(behandling.aktivtSteg(), StegType.FASTSETT_RETTIGHETSTYPE)
            ) {
                val forrigeGrunnlag =
                    requireNotNull(stansOpphørGrunnlagRepository.hentHvisEksisterer(behandling.forrigeBehandlingId!!)) {
                        "Finner ikke forrige grunnlag, selv om vi backfiller behandlinger i rekkefølge."
                    }
                requireNotNull(forrigeGrunnlag.stansOpphørV2) {
                    "Finner ikke forrige stansOpphørV2, selv om vi backfiller behandlinger i rekkefølge."
                }
                val nyttGrunnlag = grunnlag?.copy(stansOpphørV2 = forrigeGrunnlag.stansOpphørV2) ?: forrigeGrunnlag
                stansOpphørGrunnlagRepository.lagre(behandling.id, nyttGrunnlag)
                return@withLåstBehandling
            }

            if (underveisRepository.hentHvisEksisterer(behandling.id)?.perioder.orEmpty()
                    .any { it.avslagsårsak == UnderveisÅrsak.SONER_STRAFF }
            ) {
                /* Det finnes ingen eksempler på avslagsårsak = SONER_STRAFF i produksjon. I produksjon er dette et eget vilkår.  Vi kommer til å
                 * regne ut feil rettighetstype her, siden nåværende kode ikke håndterer dette caset som bare finnes i test-miljøet. Hopper derfor over.
                 */
                val nyttGrunnlag = (grunnlag ?: StansOpphørGrunnlag()).copy(stansOpphørV2 = emptyMap())
                stansOpphørGrunnlagRepository.lagre(behandling.id, nyttGrunnlag)
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

            check(
                RettighetstypeSteg.validerStansOpphør(
                    nyttGrunnlag,
                    rettighetstyper = rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(behandling.id)
                )
            ) {
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