package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.klage.andreinstans.AndreinstansService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Opprettholdes
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class OpprettholdelseSteg private constructor(
    private val klageresultatUtleder: KlageresultatUtleder,
    private val andreinstansService: AndreinstansService,
    private val trekkKlageService: TrekkKlageService,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if(trekkKlageService.klageErTrukket(kontekst.behandlingId)) {
            return Fullført
        }

        val resultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
        return when (resultat) {
            is Opprettholdes, is DelvisOmgjøres -> {
                andreinstansService.oversendTilAndreinstans(kontekst.behandlingId)
                Fullført
            }

            else -> Fullført
        }
    }


    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return OpprettholdelseSteg(
                KlageresultatUtleder(repositoryProvider),
                AndreinstansService(repositoryProvider, gatewayProvider),
                TrekkKlageService(repositoryProvider),
            )
        }

        override fun type(): StegType {
            return StegType.OPPRETTHOLDELSE
        }
    }
}