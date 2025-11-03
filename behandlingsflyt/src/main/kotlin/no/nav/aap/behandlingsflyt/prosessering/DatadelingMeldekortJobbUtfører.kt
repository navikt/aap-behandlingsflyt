package no.nav.aap.behandlingsflyt.prosessering


import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class DatadelingMeldekortJobbUtfører(
    private val saksRepository: SakRepository,
    private val underveisRepository: UnderveisRepository,
    private val meldekortRepository: MeldekortRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository,
    private val apiInternGateway: ApiInternGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)
    private val service = DatadelingMeldekortService(
        saksRepository,
        underveisRepository,
        meldekortRepository,
        meldeperiodeRepository,
    )

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val behandlingId = BehandlingId(input.behandlingId())

        val kontraktObjekter = service.opprettKontraktObjekter(sakId, behandlingId)

        try {
            if (kontraktObjekter.isEmpty()) {
                log.info("Ingen meldekort-detaljer å sende, tom liste mottatt for sakId=${sakId}, behandlingId=${behandlingId}")
                return
            }
            apiInternGateway.sendDetaljertMeldekortListe(kontraktObjekter, sakId, behandlingId)
        } catch (e: Exception) {
            log.error("Feil ved sending av meldekort til API-intern for sak=${sakId}, behandling=${behandlingId}", e)
            throw e
        }
    }

    companion object : ProvidersJobbSpesifikasjon {
        override val navn = "Meldekort til API-intern"
        override val type = "flyt.Datadeling.Meldekortdetaljer"
        override val beskrivelse = """
                Push informasjon om meldekort til API-intern slik at andre kan hente den derfra.
                """.trimIndent()

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            val sakRepository = repositoryProvider.provide<SakRepository>()

            return DatadelingMeldekortJobbUtfører(
                apiInternGateway = gatewayProvider.provide(ApiInternGateway::class),
                saksRepository = sakRepository,
                underveisRepository = repositoryProvider.provide<UnderveisRepository>(),
                meldekortRepository = repositoryProvider.provide<MeldekortRepository>(),
                meldeperiodeRepository = repositoryProvider.provide<MeldeperiodeRepository>(),
            )
        }

        fun nyJobb(
            sakId: SakId,
            behandlingId: BehandlingId,
        ) = JobbInput(DatadelingMeldekortJobbUtfører).apply {
            forBehandling(sakId.toLong(), behandlingId.toLong())
        }
    }

    /*
    For å sette inn jobber for alle meldekort, inkludert de som er opprettet før vi begynte
    å lytte på hendelser, kan vi bruke denne koden i databasen:

    SELECT 'INSERT INTO jobb (type, behandling_id, sak_id, neste_kjoring) VALUES ('''
           || 'flyt.Datadeling.Meldekortdetaljer' || ''', ' || behandling.id || ', ' || behandling.sak_id ||
       ', NOW()' || ');'
    FROM MELDEKORT_GRUNNLAG
             LEFT JOIN behandling
                       ON meldekort_grunnlag.behandling_id = behandling.id
    WHERE aktiv = true
      AND behandling.id IN (SELECT MAX(b2.id)
                            FROM behandling b2
                            GROUP BY b2.sak_id)
    ORDER BY sak_id asc, behandling_id asc;
     */

}