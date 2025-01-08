package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.time.LocalDate

class Effektuer11_7VentebehovEvaluerer : SpesifikkVentebehovEvaluerer {
    override fun definisjon(): Definisjon {
        return Definisjon.EFFEKTUER_11_7
    }

    override fun ansesSomLøst(behandlingId: BehandlingId, avklaringsbehov: Avklaringsbehov, sakId: SakId): Boolean {
        val frist = LocalDate.of(2026, 12, 1) // TODO
        val fåttSvar = false

        return LocalDate.now() > frist || fåttSvar
    }
}