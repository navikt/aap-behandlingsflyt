package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.sak.SakService

class StartBehandlingSteg(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sakService: SakService
) : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {
        if (input.kontekst.behandlingType == Førstegangsbehandling) {
            val vilkårsresultat = vilkårsresultatRepository.hent(input.kontekst.behandlingId)
            val rettighetsperiode = sakService.hent(input.kontekst.sakId).rettighetsperiode
            Vilkårtype
                .entries
                .filter { it.obligatorisk }
                .forEach { vilkårstype ->
                    vilkårsresultat.leggTilHvisIkkeEksisterer(vilkårstype).leggTilIkkeVurdertPeriode(rettighetsperiode)
                }

            vilkårsresultatRepository.lagre(input.kontekst.behandlingId, vilkårsresultat)
        }

        return StegResultat()
    }
}
