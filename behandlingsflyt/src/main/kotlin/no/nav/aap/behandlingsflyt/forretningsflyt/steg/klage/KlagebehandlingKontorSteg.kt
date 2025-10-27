package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Avslått
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagebehandlingKontorRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
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
    private val kontorRepository: KlagebehandlingKontorRepository
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
            erTilstrekkeligVurdert = { kontorRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurdering != null },
            tilbakestillGrunnlag = {
                kontorRepository.deaktiverEksisterende(kontekst.behandlingId)
            },
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
                repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.KLAGEBEHANDLING_KONTOR
        }
    }
}