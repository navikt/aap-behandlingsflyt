package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class LegeerklæringInformasjonskrav private constructor(
    private val mottaDokumentService: MottaDokumentService,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav {
    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.LEGEERKLÆRING

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): LegeerklæringInformasjonskrav {
            return LegeerklæringInformasjonskrav(
                MottaDokumentService(repositoryProvider),
                TidligereVurderingerImpl(repositoryProvider)
            )
        }
    }

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val ubehandledeLegeerklæringer = mottaDokumentService.legeerklæringerSomIkkeHarBlittBehandlet(kontekst.sakId)
        val ubehandledeDialogmeldinger = mottaDokumentService.dialogmeldingerSomIkkeHarBlittBehandlet(kontekst.sakId)

        if (ubehandledeLegeerklæringer.isEmpty() && ubehandledeDialogmeldinger.isEmpty()) {
            return IKKE_ENDRET
        }

        for (dokument in ubehandledeLegeerklæringer) {
            mottaDokumentService.markerSomBehandlet(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                referanse = InnsendingReferanse(dokument.journalpostId)
            )
        }

        for (dokument in ubehandledeDialogmeldinger) {
            mottaDokumentService.markerSomBehandlet(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                referanse = InnsendingReferanse(dokument.journalpostId)
            )
        }

        return ENDRET // Antar her at alle nye legeerklæringer gir en endring vi må ta hensyn til
    }
}