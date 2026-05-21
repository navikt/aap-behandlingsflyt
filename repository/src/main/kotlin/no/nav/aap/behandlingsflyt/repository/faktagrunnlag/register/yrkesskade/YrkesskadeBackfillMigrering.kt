package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeRegisterGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.system.measureTimeMillis

class YrkesskadeBackfillMigrering(
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun migrer() {
        val yrkesskadeGateway = gatewayProvider.provide<YrkesskadeRegisterGateway>()

        val kandidater = dataSource.transaction(readOnly = true) { connection ->
            repositoryRegistry.provider(connection)
                .provide<YrkesskadeRepository>()
                .hentKandidaterForBackfill()
        }

        log.info("Starter backfill av yrkesskade-felter. Antall kandidater: ${kandidater.size}")
        val totalTid = measureTimeMillis {
            kandidater.forEach { kandidat ->
                runCatching {
                    val person = dataSource.transaction(readOnly = true) { connection ->
                        val sakRepo = repositoryRegistry.provider(connection).provide<SakRepository>()
                        val behandlingRepo = repositoryRegistry.provider(connection).provide<BehandlingRepository>()
                        val behandling = behandlingRepo.hent(kandidat.behandlingId)
                        sakRepo.hent(behandling.sakId).person
                    }
                    val fødselsdato = dataSource.transaction(readOnly = true) { connection ->
                        repositoryRegistry.provider(connection)
                            .provide<PersonopplysningRepository>()
                            .hentBrukerPersonOpplysningHvisEksisterer(kandidat.behandlingId)
                            ?.fødselsdato
                    } ?: return@forEach

                    val yrkesskader = yrkesskadeGateway.innhent(person, fødselsdato)
                    val match = yrkesskader.firstOrNull { it.ref == kandidat.ref }

                    if (match != null) {
                        dataSource.transaction { connection ->
                            repositoryRegistry.provider(connection)
                                .provide<YrkesskadeRepository>()
                                .backfillYrkesskadeDato(kandidat.yrkesskadeDatoId, match)
                        }
                        log.info("Backfillet yrkesskade ${kandidat.ref} for behandling ${kandidat.behandlingId.id}")
                    } else {
                        log.warn("Fant ikke match i registeret for yrkesskade ref=${kandidat.ref}")
                    }
                }.onFailure { e ->
                    log.error("Feil ved backfill av yrkesskade dato_id=${kandidat.yrkesskadeDatoId}", e)
                }
            }
        }
        log.info("Fullført backfill av yrkesskade-felter på $totalTid ms")
    }
}