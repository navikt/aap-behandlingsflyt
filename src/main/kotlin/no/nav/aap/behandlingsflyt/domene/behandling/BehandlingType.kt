package no.nav.aap.behandlingsflyt.domene.behandling

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.impl.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.ForeslåVedtakSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.GeneriskPlaceholderSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.InnhentPersonopplysningerSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.InnhentYrkesskadeSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.StartBehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderAlderSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderSykdomSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderYrkesskadeÅrsakssammenhengSteg

interface BehandlingType {
    fun flyt(): BehandlingFlyt
    fun identifikator(): String
}

object Førstegangsbehandling : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(StartBehandlingSteg())
            .medSteg(InnhentPersonopplysningerSteg())
            .medSteg(VurderAlderSteg())
            .medSteg(GeneriskPlaceholderSteg(StegType.VURDER_LOVVALG))
            .medSteg(InnhentYrkesskadeSteg())
            .medSteg(VurderYrkesskadeÅrsakssammenhengSteg())
            .medSteg(VurderSykdomSteg())
            .medSteg(GeneriskPlaceholderSteg(StegType.VURDER_BISTANDSBEHOV))
            .medSteg(GeneriskPlaceholderSteg(StegType.VURDER_SYKEPENGEERSTATNING))
            .medSteg(GeneriskPlaceholderSteg(StegType.FRITAK_MELDEPLIKT))
            .medSteg(GeneriskPlaceholderSteg(StegType.INNHENT_MEDLEMSKAP))
            .medSteg(GeneriskPlaceholderSteg(StegType.VURDER_MEDLEMSKAP))
            .medSteg(GeneriskPlaceholderSteg(StegType.INNHENT_INNTEKTSOPPLYSNINGER))
            .medSteg(GeneriskPlaceholderSteg(StegType.FASTSETT_GRUNNLAG))
            .medSteg(GeneriskPlaceholderSteg(StegType.FASTSETT_UTTAK))
            .medSteg(GeneriskPlaceholderSteg(StegType.BARNETILLEGG))
            .medSteg(GeneriskPlaceholderSteg(StegType.SAMORDNING))
            .medSteg(GeneriskPlaceholderSteg(StegType.SIMULERING))
            .medSteg(GeneriskPlaceholderSteg(StegType.BEREGN_TILKJENT_YTELSE))
            .medSteg(ForeslåVedtakSteg()) // en-trinn
            .medSteg(FatteVedtakSteg()) // to-trinn
            .medSteg(GeneriskPlaceholderSteg(StegType.IVERKSETT_VEDTAK))
            .build()
    }

    override fun identifikator(): String {
        return "ae0034"
    }
}

object Revurdering : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return Førstegangsbehandling.flyt() // Returnerer bare samme fly atm
    }

    override fun identifikator(): String {
        return "ae0028"
    }
}

object Klage : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        TODO("Not yet implemented")
    }

    override fun identifikator(): String {
        TODO("Not yet implemented")
    }

}

object Anke : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        TODO("Not yet implemented")
    }

    override fun identifikator(): String {
        TODO("Not yet implemented")
    }

}

object Tilbakekreving : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        TODO("Not yet implemented")
    }

    override fun identifikator(): String {
        TODO("Not yet implemented")
    }

}
