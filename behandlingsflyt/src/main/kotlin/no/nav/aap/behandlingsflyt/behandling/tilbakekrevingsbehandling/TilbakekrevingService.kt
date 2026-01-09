package no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling

import no.nav.aap.behandlingsflyt.prosessering.tilbakekreving.FagsysteminfoSvarHendelse
import no.nav.aap.behandlingsflyt.prosessering.tilbakekreving.SendFagsysteminfoBehovTilTilbakekrevingUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput

class TilbakekrevingService(
    private val tilbakekrevingsbehandlingRepository: TilbakekrevingRepository,
    private val flytJobbRepository: FlytJobbRepository,
) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        tilbakekrevingsbehandlingRepository = repositoryProvider.provide(),
        flytJobbRepository = repositoryProvider.provide(),
    )

    fun håndter(sakId: SakId, tilbakekrevingshendelse: Tilbakekrevingshendelse) {
        tilbakekrevingsbehandlingRepository.lagre(sakId, tilbakekrevingshendelse)
/*        flytJobbRepository.leggTil(
            jobbInput = JobbInput(jobb = OppdaterOppgaveMedTilbakekrevingHendelseUtfører)
                .medPayload(tilbakekrevingshendelse)
                .forSak(sakId = sakId.toLong())
         )
*/
    }

    fun håndter(sakId: SakId, hendelse: FagsysteminfoSvarHendelse) {
        flytJobbRepository.leggTil(
            jobbInput = JobbInput(jobb = SendFagsysteminfoBehovTilTilbakekrevingUtfører)
                .medPayload(hendelse)
                .forSak(sakId = sakId.toLong())
        )
    }

}