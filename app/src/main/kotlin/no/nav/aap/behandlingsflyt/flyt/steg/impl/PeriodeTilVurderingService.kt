package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.vilkår.Vilkårtype
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.Periode

class PeriodeTilVurderingService(private val sakService: SakService) {

    fun utled(kontekst: FlytKontekst, vilkår: Vilkårtype): Set<Periode> {
        if (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling) {
            // ved førstegangsbehandling skal hele perioden alltid vurderes for alle vilkår?
            val sak = sakService.hent(kontekst.sakId)

            return setOf(sak.rettighetsperiode)
        }

        TODO(" Sjekk vilkår mot årsaker til vurdering (ligger på behandling)")
        //behandling.årsaker()
    }
}