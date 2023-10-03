package no.nav.aap.behandlingsflyt.domene.behandling

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.flyt.steg.ForeslåVedtakSteg
import no.nav.aap.behandlingsflyt.flyt.steg.GeneriskPlaceholderSteg
import no.nav.aap.behandlingsflyt.flyt.steg.InnhentPersonopplysningerSteg
import no.nav.aap.behandlingsflyt.flyt.steg.InnhentYrkesskadeSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StartBehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.VurderAlderSteg
import no.nav.aap.behandlingsflyt.flyt.steg.VurderSykdomSteg
import no.nav.aap.behandlingsflyt.flyt.steg.VurderYrkesskadeÅrsakssammenhengSteg

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
            .medSteg(GeneriskPlaceholderSteg(StegType.VURDER_MEDLEMSKAP))
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
