package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MigreringFraArenaV0
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

sealed interface MigrerFraArenaResultat {
    data class MigreringStartet(val sak: Sak) : MigrerFraArenaResultat
    data object SakFinnesAllerede : MigrerFraArenaResultat
    data class ArenasakIkkeMigrerbar(val årsak: String) : MigrerFraArenaResultat
}

class ArenaMigreringService(
    private val apiInternGateway: ApiInternGateway,
    private val arenaMigreringRepository: ArenaMigreringRepository,
    private val personOgSakService: PersonOgSakService,
    private val mottattHendelseService: MottattHendelseService,
    private val unleashGateway: UnleashGateway,
) {
    constructor(
        gatewayProvider: GatewayProvider,
        repositoryProvider: RepositoryProvider
    ) : this(
        gatewayProvider.provide<ApiInternGateway>(),
        repositoryProvider.provide<ArenaMigreringRepository>(),
        PersonOgSakService(gatewayProvider, repositoryProvider),
        MottattHendelseService(repositoryProvider),
        gatewayProvider.provide<UnleashGateway>()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Migrerer en aktiv Arena-sak til Kelvin: oppretter sak med arenamigrering, lagrer Arena-saken med
     * vedtak som JSON og registrerer en migreringshendelse som setter igang behandlingsflyten.
     */
    fun migrerFraArena(ident: Ident, saksnummerArena: String): MigrerFraArenaResultat {
        if (personOgSakService.finnSakerFor(ident).isNotEmpty()) {
            return MigrerFraArenaResultat.SakFinnesAllerede
        }

        val arenaSak = personOgSakService.finnArenasakForBruker(ident, saksnummerArena)
            ?: return MigrerFraArenaResultat.ArenasakIkkeMigrerbar("Fant ikke arenasak $saksnummerArena for bruker")

        if (arenaSak.statuskode != "AKTIV") {
            return MigrerFraArenaResultat.ArenasakIkkeMigrerbar(
                "Arenasak $saksnummerArena har statuskode ${arenaSak.statuskode}, forventet AKTIV"
            )
        }

        val sak = personOgSakService.finnEllerOpprett(ident = ident, søknadsdato = LocalDate.now()) // TODO må hentes fra arenasaken?
        val arenaMigrering = ArenaMigrering(
            sakId = sak.id,
            saksnummerArena = saksnummerArena,
            ident = ident.identifikator,
            migrertTidspunkt = LocalDateTime.now(),
        )

        if (unleashGateway.isEnabled(BehandlingsflytFeature.MigreringHentArenaGrunnlag)) {
            val arenaSakMedVedtak = apiInternGateway.hentArenaSakMedVedtak(saksnummerArena)
            // TODO se litt mer på hvordan denne dataen representeres i datamodellen
            arenaMigreringRepository.lagre(arenaMigrering.copy(arenaSakData = arenaSakMedVedtak))
        } else {
            arenaMigreringRepository.lagre(arenaMigrering)
        }

        val referanse = UUID.randomUUID().toString()
        mottattHendelseService.registrerMottattHendelse(
            Innsending(
                saksnummer = sak.saksnummer,
                referanse = InnsendingReferanse(
                    type = InnsendingReferanse.Type.MIGRERING_FRA_ARENA,
                    verdi = referanse
                ),
                type = InnsendingType.MIGRERING_FRA_ARENA,
                mottattTidspunkt = LocalDateTime.now(),
                melding = MigreringFraArenaV0("Migrering av Arenasak $saksnummerArena"),
            )
        )

        log.info("Startet migrering av $saksnummerArena til Kelvin-sak ${sak.saksnummer} med referanse $referanse")
        return MigrerFraArenaResultat.MigreringStartet(sak)
    }
}