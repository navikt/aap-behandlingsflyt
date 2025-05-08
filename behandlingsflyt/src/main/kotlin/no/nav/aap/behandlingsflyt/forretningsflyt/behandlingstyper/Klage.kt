package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.StartBehandlingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.FormkravSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.PåklagetBehandlingSteg

object Klage : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(steg = StartBehandlingSteg)
            .medSteg(steg = PåklagetBehandlingSteg)
            .medSteg(steg = FormkravSteg)
            .medSteg(steg = FatteVedtakSteg)
            .build()
    }
}