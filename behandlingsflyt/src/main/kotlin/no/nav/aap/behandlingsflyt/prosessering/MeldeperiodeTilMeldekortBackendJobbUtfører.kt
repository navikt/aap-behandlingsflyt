package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.meldekort.kontrakt.Periode
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class MeldeperiodeTilMeldekortBackendJobbUtfører(
    private val sakService: SakService,
    private val meldeperiodeService: MeldeperiodeService,
    private val meldekortGateway: MeldekortGateway,
): JobbUtfører {
    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val behandlingId = BehandlingId(input.behandlingId())

        val sak = sakService.hent(sakId)
        val meldeperioder = meldeperiodeService.meldeperioder(sak, behandlingId)

        meldekortGateway.oppdaterMeldeperioder(
            MeldeperioderV0(
                saksnummer = sak.saksnummer.toString(),
                sakenGjelderFor = Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.tom),
                meldeperioder = meldeperioder.map { Periode(it.fom, it.tom) },
            )
        )
    }

    companion object: Jobb {
        override fun beskrivelse(): String {
            return """
                Push informasjon til meldekort-backend slik at vi kan åpne for
                innsending av meldekort før vedtak er fattet.
                """.trimIndent()
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()

            return MeldeperiodeTilMeldekortBackendJobbUtfører(
                sakService = SakService(
                    sakRepository = sakRepository,
                ),
                meldeperiodeService = MeldeperiodeService(
                    meldeperiodeRepository = repositoryProvider.provide(),
                ),
                meldekortGateway = GatewayProvider.provide(),
            )
        }

        override fun navn(): String {
            return "MeldeperiodeTilMeldekortBackend"
        }

        override fun type(): String {
            return "flyt.meldeperiodeTilMeldekortBackend"
        }

        fun nyJobb(
            sakId: SakId,
            behandlingId: BehandlingId,
        ) = JobbInput(MeldeperiodeTilMeldekortBackendJobbUtfører)
            .apply {
                forBehandling(sakId.toLong(), behandlingId.toLong())
            }
    }
}