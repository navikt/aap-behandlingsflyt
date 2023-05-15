package no.nav.aap.domene.behandling

import no.nav.aap.flyt.BehandlingFlyt
import no.nav.aap.flyt.BehandlingFlytBuilder
import no.nav.aap.flyt.StegType
import no.nav.aap.steg.AvsluttBehandlingSteg
import no.nav.aap.steg.GeneriskPlaceholderSteg
import no.nav.aap.steg.StartBehandlingSteg
import no.nav.aap.steg.VurderYrkesskadeSteg

interface BehandlingType {
    fun flyt(): BehandlingFlyt
    fun identifikator(): String
}

object Førstegangsbehandling : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(StartBehandlingSteg())
            .medSteg(GeneriskPlaceholderSteg(StegType.INNHENT_REGISTERDATA))
            .medSteg(VurderYrkesskadeSteg())
            .medSteg(GeneriskPlaceholderSteg(StegType.INNGANGSVILKÅR))
            .medSteg(GeneriskPlaceholderSteg(StegType.FASTSETT_GRUNNLAG))
            .medSteg(GeneriskPlaceholderSteg(StegType.FASTSETT_UTTAK))
            .medSteg(GeneriskPlaceholderSteg(StegType.SIMULERING))
            .medSteg(GeneriskPlaceholderSteg(StegType.BEREGN_TILKJENT_YTELSE))
            .medSteg(GeneriskPlaceholderSteg(StegType.FORESLÅ_VEDTAK)) // en-trinn
            .medSteg(GeneriskPlaceholderSteg(StegType.FATTE_VEDTAK)) // to-trinn
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
