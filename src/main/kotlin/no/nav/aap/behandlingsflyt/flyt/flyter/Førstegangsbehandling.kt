package no.nav.aap.behandlingsflyt.domene.behandling

import no.nav.aap.behandlingsflyt.faktagrunnlag.legeerklæring.Legeerklæring
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.impl.*
import no.nav.aap.behandlingsflyt.grunnlag.student.db.InMemoryStudentRepository

object Førstegangsbehandling : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(StartBehandlingSteg())
            .medSteg(InnhentPersonopplysningerSteg())
            .medSteg(VurderAlderSteg())
            .medSteg(GeneriskPlaceholderSteg(StegType.VURDER_LOVVALG))
            .medSteg(VurderStudentSteg(InMemoryStudentRepository))
            .medSteg(InnhentYrkesskadeOpplysningerSteg())
            .medSteg(VurderYrkesskadeÅrsakssammenhengSteg())
            .medSteg(VurderSykdomSteg(), informasjonskrav = listOf(Yrkesskade, Legeerklæring))
            .medSteg(FritakMeldepliktSteg())
            .medSteg(VurderBistandsbehovSteg())
            .medSteg(VurderSykepengeErstatningSteg())
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