package no.nav.aap.domene.behandling

import no.nav.aap.flyt.BehandlingFlyt
import no.nav.aap.flyt.BehandlingFlytBuilder
import no.nav.aap.flyt.StegType
import no.nav.aap.flyt.steg.AvsluttBehandlingSteg
import no.nav.aap.flyt.steg.FatteVedtakSteg
import no.nav.aap.flyt.steg.ForeslåVedtakSteg
import no.nav.aap.flyt.steg.GeneriskPlaceholderSteg
import no.nav.aap.flyt.steg.InnhentRegisterdataSteg
import no.nav.aap.flyt.steg.StartBehandlingSteg
import no.nav.aap.flyt.steg.VurderYrkesskadeSteg

interface BehandlingType {
    fun flyt(): BehandlingFlyt
    fun identifikator(): String
}

object Førstegangsbehandling : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(StartBehandlingSteg())
            .medSteg(InnhentRegisterdataSteg())
            .medSteg(VurderYrkesskadeSteg())
            .medSteg(GeneriskPlaceholderSteg(StegType.INNGANGSVILKÅR))
            .medSteg(GeneriskPlaceholderSteg(StegType.FASTSETT_GRUNNLAG))
            .medSteg(GeneriskPlaceholderSteg(StegType.FASTSETT_UTTAK))
            .medSteg(GeneriskPlaceholderSteg(StegType.SIMULERING))
            .medSteg(GeneriskPlaceholderSteg(StegType.BEREGN_TILKJENT_YTELSE))
            .medSteg(ForeslåVedtakSteg()) // en-trinn
            .medSteg(FatteVedtakSteg()) // to-trinn
            .medSteg(GeneriskPlaceholderSteg(StegType.IVERKSETT_VEDTAK))
            .medSteg(AvsluttBehandlingSteg())
            .build()
    }

    override fun identifikator(): String {
        return "ae0034"
    }
}

object Revurdering : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        TODO("Not yet implemented")
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
