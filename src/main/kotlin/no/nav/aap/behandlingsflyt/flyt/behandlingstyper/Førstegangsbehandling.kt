package no.nav.aap.behandlingsflyt.flyt.behandlingstyper

import no.nav.aap.behandlingsflyt.behandling.BehandlingType
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersonopplysningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.impl.FatteVedtakFlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.ForeslåVedtakFlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.FritakMeldepliktFlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.GeneriskPlaceholderFlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.StartBehandlingFlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderAlderFlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderBistandsbehovFlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderStudentFlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderSykdomFlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderSykepengeErstatningFlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderYrkesskadeÅrsakssammenhengFlytSteg

object Førstegangsbehandling : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(steg = StartBehandlingFlytSteg)
            .medSteg(steg = VurderAlderFlytSteg, informasjonskrav = listOf(PersonopplysningService))
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.VURDER_LOVVALG))
            .medSteg(steg = VurderStudentFlytSteg)
            .medSteg(steg = VurderYrkesskadeÅrsakssammenhengFlytSteg, informasjonskrav = listOf(YrkesskadeService))
            .medSteg(steg = VurderSykdomFlytSteg)
            .medSteg(steg = FritakMeldepliktFlytSteg)
            .medSteg(steg = VurderBistandsbehovFlytSteg)
            .medSteg(steg = VurderSykepengeErstatningFlytSteg)
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.VURDER_MEDLEMSKAP))
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.FASTSETT_GRUNNLAG))
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.FASTSETT_UTTAK))
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.BARNETILLEGG))
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.SAMORDNING))
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.SIMULERING))
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.BEREGN_TILKJENT_YTELSE))
            .medSteg(steg = ForeslåVedtakFlytSteg) // en-trinn
            .sluttÅOppdatereFaktagrunnlag()
            .medSteg(steg = FatteVedtakFlytSteg) // to-trinn
            .medSteg(steg = GeneriskPlaceholderFlytSteg(StegType.IVERKSETT_VEDTAK))
            .build()
    }

    override fun identifikator(): String {
        return "ae0034"
    }
}