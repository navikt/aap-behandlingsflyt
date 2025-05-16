@file:JvmName("MeldingOmVedtakBrevStegKt")

package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.brev.BrevUtlederService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.BESTILL_BREV
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("BrevSteg")

class MeldingOmVedtakBrevSteg private constructor(
    private val brevUtlederService: BrevUtlederService,
    private val brevbestillingService: BrevbestillingService,
    private val behandlingRepository: BehandlingRepository
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        brevUtlederService = BrevUtlederService(repositoryProvider),
        brevbestillingService = BrevbestillingService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val brevBehov = brevUtlederService.utledBehovForMeldingOmVedtak(kontekst.behandlingId)
        if (brevBehov.harBehovForBrev()) {
            val typeBrev = brevBehov.typeBrev!!
            val bestillingFinnes =
                brevbestillingService.harBestillingOmVedtak(kontekst.behandlingId)
            if (!bestillingFinnes) {
                val behandling = behandlingRepository.hent(kontekst.behandlingId)
                log.info("Bestiller brev for sak ${kontekst.sakId}.")
                val unikReferanse = "${behandling.referanse}-$typeBrev"
                if (Miljø.erDev()) {
                    brevbestillingService.bestillV2(
                        behandlingId = kontekst.behandlingId,
                        typeBrev = typeBrev,
                        unikReferanse = unikReferanse,
                        ferdigstillAutomatisk = false,
                        faktagrunnlag = emptySet()
                    )
                    return FantAvklaringsbehov(Definisjon.SKRIV_VEDTAKSBREV)
                } else {
                    brevbestillingService.bestill(
                        behandlingId = kontekst.behandlingId,
                        typeBrev = typeBrev,
                        unikReferanse = unikReferanse
                    )
                    return FantVentebehov(Ventebehov(BESTILL_BREV, ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING))
                }
            }
        }
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return MeldingOmVedtakBrevSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.BREV
        }
    }
}