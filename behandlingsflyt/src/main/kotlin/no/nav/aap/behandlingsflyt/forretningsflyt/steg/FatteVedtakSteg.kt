package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.klage.andreinstans.AndreinstansGateway
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Omgjøres
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.TilbakeføresFraBeslutter
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.lookup.repository.RepositoryProvider

class FatteVedtakSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val trekkKlageService: TrekkKlageService,
) : BehandlingSteg {

    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
        trekkKlageService = TrekkKlageService(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type()) || trekkKlageService.klageErTrukket(kontekst.behandlingId)) {
            avklaringsbehov.avbrytForSteg(type())
            return Fullført
        }

        if (kontekst.behandlingType == TypeBehandling.Klage) {
            val klageresultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
            if (klageresultat is Omgjøres) {
                avklaringsbehov.avbrytForSteg(type())
                return Fullført
            }
        }

        if (avklaringsbehov.skalTilbakeføresEtterTotrinnsVurdering()) {
            return TilbakeføresFraBeslutter
        }
        if (avklaringsbehov.harHattAvklaringsbehovSomHarKrevdToTrinn()) {
            return FantAvklaringsbehov(Definisjon.FATTE_VEDTAK)
        }

        gosysGateway.opprettOppgaveHvisIkkeEksisterer
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return FatteVedtakSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.FATTE_VEDTAK
        }
    }
}
