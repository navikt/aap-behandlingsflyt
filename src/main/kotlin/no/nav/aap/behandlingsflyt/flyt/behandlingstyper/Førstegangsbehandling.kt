package no.nav.aap.behandlingsflyt.domene.behandling

import no.nav.aap.behandlingsflyt.faktagrunnlag.legeerklæring.Legeerklæring
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.impl.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.ForeslåVedtakSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.FritakMeldepliktSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.GeneriskPlaceholderSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.InnhentPersonopplysningerSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.InnhentYrkesskadeOpplysningerSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.StartBehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderAlderSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderBistandsbehovSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderStudentSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderSykdomSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderSykepengeErstatningSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderYrkesskadeÅrsakssammenhengSteg
import no.nav.aap.behandlingsflyt.grunnlag.student.db.InMemoryStudentRepository

object Førstegangsbehandling : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(steg = StartBehandlingSteg())
            .medSteg(steg = InnhentPersonopplysningerSteg())
            .medSteg(steg = VurderAlderSteg())
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.VURDER_LOVVALG))
            .medSteg(steg = VurderStudentSteg(InMemoryStudentRepository))
            .medSteg(steg = InnhentYrkesskadeOpplysningerSteg())
            .medSteg(steg = VurderYrkesskadeÅrsakssammenhengSteg(), informasjonskrav = listOf(Yrkesskade))
            .medSteg(steg = VurderSykdomSteg(), informasjonskrav = listOf(Legeerklæring))
            .medSteg(steg = FritakMeldepliktSteg())
            .medSteg(steg = VurderBistandsbehovSteg())
            .medSteg(steg = VurderSykepengeErstatningSteg())
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.INNHENT_MEDLEMSKAP))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.VURDER_MEDLEMSKAP))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.INNHENT_INNTEKTSOPPLYSNINGER))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.FASTSETT_GRUNNLAG))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.FASTSETT_UTTAK))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.BARNETILLEGG))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.SAMORDNING))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.SIMULERING))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.BEREGN_TILKJENT_YTELSE))
            .medSteg(steg = ForeslåVedtakSteg()) // en-trinn
            .medSteg(steg = FatteVedtakSteg()) // to-trinn
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.IVERKSETT_VEDTAK))
            .build()
    }

    override fun identifikator(): String {
        return "ae0034"
    }
}