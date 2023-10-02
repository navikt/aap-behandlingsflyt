package no.nav.aap.behandlingsflyt.flate.behandling.periode

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.FunksjonellGruppe
import no.nav.aap.behandlingsflyt.domene.behandling.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårstype
import no.nav.aap.behandlingsflyt.domene.sak.Sakslager
import no.nav.aap.behandlingsflyt.domene.Periode

object PeriodeTilVurderingTjeneste {

    fun utled(behandling: Behandling, vilkår: Vilkårstype): Set<no.nav.aap.behandlingsflyt.domene.Periode> {
        if (behandling.type == Førstegangsbehandling) {
            // ved førstegangsbehandling skal hele perioden alltid vurderes for alle vilkår?
            val sak = Sakslager.hent(behandling.sakId)

            return setOf(sak.rettighetsperiode)
        }
        behandling.årsaker()
        TODO(" Sjekk vilkår mot årsaker til vurdering (ligger på behandling)")
    }

    /**
     * Er du i kontekst av et vilkår benytt metoden for vilkår
     *
     * Denne er for UTTAK osv? Usikker på om dette er en god ide?
     */
    fun utled(behandling: Behandling, gruppe: FunksjonellGruppe): Set<no.nav.aap.behandlingsflyt.domene.Periode> {
        if (behandling.type == Førstegangsbehandling) {
            // ved førstegangsbehandling skal hele perioden alltid vurderes for alle vilkår?
            val sak = Sakslager.hent(behandling.sakId)

            return setOf(sak.rettighetsperiode)
        } else if (gruppe in setOf(FunksjonellGruppe.UTTAK, FunksjonellGruppe.TILKJENT_YTELSE)) {
            // Skal alltid innom UTTAK / TILKJENT_YTELSE men kan avgrensenses på et tidspunkt?
            val sak = Sakslager.hent(behandling.sakId)

            return setOf(sak.rettighetsperiode)
        }
        behandling.årsaker()
        TODO(" Sjekk vilkår mot årsaker til vurdering (ligger på behandling)")
    }
}