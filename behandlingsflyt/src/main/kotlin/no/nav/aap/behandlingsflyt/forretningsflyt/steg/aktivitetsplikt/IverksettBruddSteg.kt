package no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt

import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.OmgjøringSteg
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class IverksettBruddSteg private constructor() : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        // TODO: Opprett fasttrack-behandling for å effektuere brudd på aktivitetsplikten

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return IverksettBruddSteg()
        }

        override fun type(): StegType {
            return StegType.IVERKSETT_BRUDD
        }
    }

}