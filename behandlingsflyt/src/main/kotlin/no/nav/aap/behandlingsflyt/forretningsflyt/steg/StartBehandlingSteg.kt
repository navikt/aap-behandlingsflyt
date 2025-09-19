package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class StartBehandlingSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling) {
            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            val rettighetsperiode = kontekst.rettighetsperiode
            Vilkårtype
                .entries
                .filter { it.obligatorisk }
                .forEach { vilkårstype ->
                    vilkårsresultat
                        .leggTilHvisIkkeEksisterer(vilkårstype)
                        .leggTilIkkeVurdertPeriode(rettighetsperiode)
                }

            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        if (kontekst.behandlingType == TypeBehandling.SvarFraAndreinstans) return Fullført

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return StartBehandlingSteg(
                repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.START_BEHANDLING
        }

        override fun toString(): String {
            return "FlytSteg(type:${type()})"
        }
    }
}
