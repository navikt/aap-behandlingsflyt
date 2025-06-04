package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst

interface VentebehovEvaluererService {
    fun løsVentebehov(kontekst: FlytKontekst, avklaringsbehovene: Avklaringsbehovene): List<Avklaringsbehov>
}