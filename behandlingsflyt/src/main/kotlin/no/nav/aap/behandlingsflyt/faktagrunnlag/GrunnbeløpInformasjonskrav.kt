package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.gregulering.GReguleringService
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.FØRSTEGANGSBEHANDLING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.G_REGULERING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.REVURDERING
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class GrunnbeløpInformasjonskrav(
    private val gReguleringService: GReguleringService,
) : Informasjonskrav<IngenInput, IngenRegisterData> {

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.GRUNNBELØP

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): GrunnbeløpInformasjonskrav {
            return GrunnbeløpInformasjonskrav(
                gReguleringService = GReguleringService(repositoryProvider, gatewayProvider),
            )
        }
    }

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.vurderingType in setOf(FØRSTEGANGSBEHANDLING, REVURDERING, G_REGULERING)
    }

    override fun klargjør(kontekst: FlytKontekstMedPerioder) = IngenInput

    override fun hentData(input: IngenInput) = IngenRegisterData

    override fun oppdater(
        input: IngenInput,
        registerdata: IngenRegisterData,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        return if (gReguleringService.erGrunnbeløpEndretForBehandling(kontekst.behandlingId)) ENDRET else IKKE_ENDRET
    }

    override fun flettOpplysningerFraAtomærBehandling(kontekst: FlytKontekst): Informasjonskrav.Endret {
        /*
         * Denne vil føre til at behandlingen tilbakeføres til steget tilkjent ytelse. I dette tilfellet er det ikke
         * aktuelt å flette inn nye opplysninger så i prinsippet er dette strengt tatt ikke nødvendig da den åpne
         * behandlingen vil prosesseres og dermed gå gjennom alle informasjonskravene på nytt.
         */
        return if (gReguleringService.erGrunnbeløpEndretForBehandling(kontekst.behandlingId)) ENDRET else IKKE_ENDRET
    }
}
