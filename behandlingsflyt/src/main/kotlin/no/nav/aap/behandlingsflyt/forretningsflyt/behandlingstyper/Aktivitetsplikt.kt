package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt.IverksettBruddSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt.VurderAktivitetsplikt11_7Steg

object Aktivitetsplikt : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(steg = VurderAktivitetsplikt11_7Steg)
            .medSteg(steg = FatteVedtakSteg)
            .medSteg(steg = IverksettBruddSteg)
            .build()
    }
}