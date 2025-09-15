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
            apiInternGateway.sendDetaljertMeldekortListe(kontraktObjekter)
        } catch (e: Exception) {
            log.error(
                "Feil ved sending av meldekort til API-intern for sak=${sakId}, behandling=${behandlingId}", e
            )
            throw e
        }
    }

    companion object : ProvidersJobbSpesifikasjon {
        override val navn = "Meldekort til API-intern"
        override val type = "kelvin.meldekort.til.api.intern"
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


}