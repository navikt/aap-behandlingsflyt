package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.IngenInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.IngenRegisterData
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class VedtakslengdeInformasjonskrav(
    private val vedtakslengdeRepository: VedtakslengdeRepository,
) : Informasjonskrav<IngenInput, IngenRegisterData> {
    
    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.VEDTAKSLENGDE

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): VedtakslengdeInformasjonskrav {
            return VedtakslengdeInformasjonskrav(
                repositoryProvider.provide(),
            )
        }
    }

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return false // Kun interessert i kopiering fra atomær behandling til åpen
    }

    override fun klargjør(kontekst: FlytKontekstMedPerioder) = IngenInput

    override fun hentData(input: IngenInput) = IngenRegisterData

    override fun oppdater(
        input: IngenInput,
        registerdata: IngenRegisterData,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        // Kun interessert i kopiering fra atomær behandling til åpen
        return IKKE_ENDRET
    }

    override fun flettOpplysningerFraAtomærBehandling(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val grunnlag = vedtakslengdeRepository.hentHvisEksisterer(kontekst.behandlingId)
        val forrigeGrunnlag =
            kontekst.forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(it) }
        if (grunnlag != forrigeGrunnlag) {
            vedtakslengdeRepository.kopier(kontekst.forrigeBehandlingId!!, kontekst.behandlingId)
            return ENDRET
        }
        return IKKE_ENDRET
    }
}
