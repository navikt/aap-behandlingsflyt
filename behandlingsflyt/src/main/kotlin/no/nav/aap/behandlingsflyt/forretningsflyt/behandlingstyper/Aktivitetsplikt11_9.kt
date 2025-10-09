package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.MeldingOmVedtakBrevSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt.IverksettBruddSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt.VurderAktivitetsplikt11_9Steg

object Aktivitetsplikt11_9 : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(steg = VurderAktivitetsplikt11_9Steg)
            .sluttÅOppdatereFaktagrunnlag()
            .medSteg(steg = FatteVedtakSteg)
            .medSteg(steg = IverksettBruddSteg)
            .medSteg(steg = MeldingOmVedtakBrevSteg)
            .build()
    }
}