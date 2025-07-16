package no.nav.aap.behandlingsflyt.forretningsflyt.steg.oppfølgingsbehandling

import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.KonsekvensAvOppfølging
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.HvemSkalFølgeOpp
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class AvklarOppfølgingSteg(
    private val oppfølgingsBehandlingRepository: OppfølgingsBehandlingRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val låsRepository: TaSkriveLåsRepository,
    private val prosesserBehandling: ProsesserBehandlingService,
    private val mottaDokumentService: MottaDokumentService
) :
    BehandlingSteg {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val grunnlag = oppfølgingsBehandlingRepository.hent(kontekst.behandlingId)
        val oppfølgingsoppgavedokument = mottaDokumentService.hentOppfølgingsBehandlingDokument(kontekst.behandlingId)

        if (grunnlag == null) {
            return FantAvklaringsbehov(
                when (oppfølgingsoppgavedokument.hvemSkalFølgeOpp) {
                    HvemSkalFølgeOpp.Lokalkontor   -> Definisjon.AVKLAR_OPPFØLGINGSBEHOV_LOKALKONTOR
                    HvemSkalFølgeOpp.NasjonalEnhet -> Definisjon.AVKLAR_OPPFØLGINGSBEHOV_NAY
                }
            )
        }

        when (grunnlag.konsekvensAvOppfølging) {
            KonsekvensAvOppfølging.INGEN -> return Fullført
            KonsekvensAvOppfølging.OPPRETT_VURDERINGSBEHOV -> {
                val årsaker = grunnlag.opplysningerTilRevurdering
                val behandling = sakOgBehandlingService.finnEllerOpprettBehandling(
                    sakId = kontekst.sakId,
                    årsaker = årsaker.map { Årsak(it) }
                )

                val behandlingSkrivelås =
                    låsRepository.låsBehandling(behandling.id)

                prosesserBehandling.triggProsesserBehandling(behandling.sakId, behandling.id)
                låsRepository.verifiserSkrivelås(behandlingSkrivelås)
            }
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return AvklarOppfølgingSteg(
                repositoryProvider.provide(),
                SakOgBehandlingService(repositoryProvider),
                repositoryProvider.provide(),
                ProsesserBehandlingService(repositoryProvider),
                MottaDokumentService(repositoryProvider.provide()),
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_OPPFØLGING
        }
    }
}