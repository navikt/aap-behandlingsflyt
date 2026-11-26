package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.MeldingOmVedtakBrevSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.StartBehandlingSteg
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.utenlandsopphold.VurderSøknadUtenlandsoppholdSteg

/** Behandling av søknad om å beholde AAP under opphold i utlandet. Lokalkontor vurderer søknaden, og vedtaksbrev sendes. */
object Utenlandsopphold : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(StartBehandlingSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(VurderSøknadUtenlandsoppholdSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(MeldingOmVedtakBrevSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .build()
    }
}
