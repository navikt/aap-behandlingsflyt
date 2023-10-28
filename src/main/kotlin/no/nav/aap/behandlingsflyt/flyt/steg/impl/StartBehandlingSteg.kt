package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.sak.SakService

class StartBehandlingSteg(
    private val behandlingService: BehandlingService,
    private val sakService: SakService
) : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {
        val behandling = behandlingService.hent(input.kontekst.behandlingId)

        if (behandling.type == Førstegangsbehandling) {
            val vilkårsresultat = VilkårsresultatRepository.hent(behandling.id)
            val rettighetsperiode = sakService.hent(behandling.sakId).rettighetsperiode
            Vilkårtype
                .entries
                .filter { it.obligatorisk }
                .forEach { vilkårstype ->
                    vilkårsresultat.leggTilHvisIkkeEksisterer(vilkårstype).leggTilIkkeVurdertPeriode(rettighetsperiode)
                }
        }

        return StegResultat()
    }
}
