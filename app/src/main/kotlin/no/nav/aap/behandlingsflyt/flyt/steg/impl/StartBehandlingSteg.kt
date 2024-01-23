package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.StegType

class StartBehandlingSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sakService: SakService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        if (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling) {
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

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return StartBehandlingSteg(
                VilkårsresultatRepository(connection),
                SakService(connection)
            )
        }

        override fun type(): StegType {
            return StegType.START_BEHANDLING
        }
    }
}
