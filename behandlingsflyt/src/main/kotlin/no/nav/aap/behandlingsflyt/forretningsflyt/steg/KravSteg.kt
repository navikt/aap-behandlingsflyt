package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class KravSteg(
    private val unleashGateway: UnleashGateway,
    private val kravRepository: KravRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.KravSteg)) {
            return Fullført
        }

        when (kontekst.behandlingType) {
            TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering -> {
                vurderAutomatiskHvisMulig(kontekst)
                
                avklaringsbehovService.oppdaterAvklaringsbehov(
                    definisjon = Definisjon.VURDER_KRAV,
                    vedtakBehøverVurdering = TODO(),
                    erTilstrekkeligVurdert = TODO(),
                    tilbakestillGrunnlag = TODO(),
                    kontekst = TODO()
                )
            }

            else -> {}
        }

        return Fullført
    }

    private fun vurderAutomatiskHvisMulig(kontekst: FlytKontekstMedPerioder) {
        if (erFørstegangsbehandlingUtenEksisterendeKrav(kontekst)) {
            val søknaderMottattIBehandling =
                mottattDokumentRepository.hentDokumenterAvType(kontekst.behandlingId, InnsendingType.SØKNAD)

            if (søknaderMottattIBehandling.size == 1) {
                val søknad = søknaderMottattIBehandling.first()
                kravRepository.lagre(
                    kontekst.behandlingId, vurderinger = setOf(
                        NyttKrav(
                            journalpostId = søknad.referanse.asJournalpostId,
                            vurdertAv = SYSTEMBRUKER.ident,
                            begrunnelse = "Automatisk vurdert",
                            vurdertIBehandling = kontekst.behandlingId,
                            opprettet = Instant.now(),
                            søknadsdato = Søknadsdato(
                                søknad.mottattTidspunkt.toLocalDate(),
                                SøknadsdatoÅrsak.SøknadMottatt
                            ),
                            muligRettFra = null,
                            kravdato = søknad.mottattTidspunkt.toLocalDate()
                        )
                    )
                )
            }
        }
    }

    private fun erFørstegangsbehandlingUtenEksisterendeKrav(kontekst: FlytKontekstMedPerioder): Boolean {
        val erFørstegangsbehandling =
            Vurderingsbehov.MOTTATT_SØKNAD in kontekst.vurderingsbehovRelevanteForSteg && kontekst.behandlingType == TypeBehandling.Førstegangsbehandling
        return erFørstegangsbehandling 
                && kravRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger.isNullOrEmpty()
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return KravSteg(
                unleashGateway = gatewayProvider.provide(),
                kravRepository = repositoryProvider.provide(),
                mottattDokumentRepository = repositoryProvider.provide(),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.KRAV
        }
    }
}
