package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Avslått
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class KlagebehandlingKontorSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlendeEnhetRepository: BehandlendeEnhetRepository,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val trekkKlageService: TrekkKlageService,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val resultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        val behandlendeEnhetVurdering = behandlendeEnhetRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurdering
        val skalBehandlesAvKontor = behandlendeEnhetVurdering?.skalBehandlesAvKontor == true

        val klageErAvslått = resultat is Avslått
        val klageErTrukket = trekkKlageService.klageErTrukket(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehov,
            definisjon = Definisjon.VURDER_KLAGE_KONTOR,
            vedtakBehøverVurdering = {
                !klageErTrukket && !klageErAvslått && skalBehandlesAvKontor
            },
            erTilstrekkeligVurdert = { !avklaringsbehov.harIkkeBlittLøst(Definisjon.VURDER_KLAGE_KONTOR) },
            tilbakestillGrunnlag = { /* TODO: Eventuell utnulling av vurdering kan skje i senere steg. Vil kanskje ta vare på vurderingen "så lenge som mulig" i tilfelle man ombestemmer seg */ },
            kontekst = kontekst,
        )

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return KlagebehandlingKontorSteg(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                KlageresultatUtleder(repositoryProvider),
                TrekkKlageService(repositoryProvider),
                AvklaringsbehovService(repositoryProvider),
            )
        }

        override fun type(): StegType {
            return StegType.KLAGEBEHANDLING_KONTOR
        }
    }

    private fun Avklaringsbehovene.harIkkeBlittLøst(definisjon: Definisjon): Boolean {
        return this.alle()
            .filter { it.definisjon == definisjon }
            .none { it.status() == Status.AVSLUTTET }
    }
}