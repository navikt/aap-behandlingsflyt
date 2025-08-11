package no.nav.aap.behandlingsflyt.forretningsflyt.steg.oppfølgingsbehandling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class StartOppfølgingsBehandlingSteg(
    private val mottattDokumentRepository: MottattDokumentRepository,
    val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.behandlingType != TypeBehandling.OppfølgingsBehandling) {
            log.info("Dette steget skal bare kalles for behandlingstype ${TypeBehandling.OppfølgingsBehandling}")
            return Fullført
        }

        val hentAvklaringsbehovene =
            avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (hentAvklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.VENT_PÅ_OPPFØLGING)) {
            return Fullført
        }

        val oppfølgingsDokument = MottaDokumentService(mottattDokumentRepository).hentOppfølgingsBehandlingDokument(kontekst.behandlingId)
        requireNotNull(oppfølgingsDokument)
        

        return FantVentebehov(
            Ventebehov(
                definisjon = Definisjon.VENT_PÅ_OPPFØLGING,
                grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER,
                frist = oppfølgingsDokument.datoForOppfølging
            )
        )

    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return StartOppfølgingsBehandlingSteg(
                mottattDokumentRepository = repositoryProvider.provide(),
                avklaringsbehovRepository = repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.START_OPPFØLGINGSBEHANDLING
        }

        override fun toString(): String {
            return "FlytSteg(type:${type()})"
        }
    }
}