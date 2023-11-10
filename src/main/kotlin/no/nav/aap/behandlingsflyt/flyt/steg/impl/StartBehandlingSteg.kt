package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.sak.SakService

class StartBehandlingSteg(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sakService: SakService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        if (kontekst.behandlingType == Førstegangsbehandling) {
            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            val rettighetsperiode = sakService.hent(kontekst.sakId).rettighetsperiode
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

        return StegResultat()
    }
}
