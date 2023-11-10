package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.sak.SakService

class PeriodeTilVurderingService(private val sakService: SakService) {

    fun utled(kontekst: FlytKontekst, vilkår: Vilkårtype): Set<Periode> {
        if (kontekst.behandlingType == Førstegangsbehandling) {
            // ved førstegangsbehandling skal hele perioden alltid vurderes for alle vilkår?
            val sak = sakService.hent(kontekst.sakId)

            return setOf(sak.rettighetsperiode)
        }
        //behandling.årsaker()
        TODO(" Sjekk vilkår mot årsaker til vurdering (ligger på behandling)")
    }
}