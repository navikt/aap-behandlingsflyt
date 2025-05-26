package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.KvalitetssikringsSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.MeldingOmVedtakBrevSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.StartBehandlingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.BehandlendeEnhetSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.FormkravSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.KlagebehandlingKontorSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.KlagebehandlingNaySteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.OmgjøringSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.OpprettholdelseSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.PåklagetBehandlingSteg

object Klage : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(steg = StartBehandlingSteg)
            .medSteg(steg = PåklagetBehandlingSteg)
            .medSteg(steg = FormkravSteg)
            .medSteg(steg = BehandlendeEnhetSteg)
            .medSteg(steg = KlagebehandlingKontorSteg)
            .medSteg(steg = KvalitetssikringsSteg)
            .medSteg(steg = KlagebehandlingNaySteg)
            .medSteg(steg = FatteVedtakSteg)
            .medSteg(steg = OmgjøringSteg)
            .medSteg(steg = OpprettholdelseSteg)
            .medSteg(steg = MeldingOmVedtakBrevSteg)
            .build()
    }
}