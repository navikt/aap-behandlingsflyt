package no.nav.aap.behandlingsflyt.flyt.behandlingstyper

import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingType
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersonopplysningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.impl.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.ForeslåVedtakSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.FritakMeldepliktSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.GeneriskPlaceholderSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.StartBehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderAlderSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderBistandsbehovSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderStudentSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderSykdomSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderSykepengeErstatningSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.VurderYrkesskadeÅrsakssammenhengSteg
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.db.InMemoryStudentRepository

object Førstegangsbehandling : BehandlingType {
    override fun flyt(): BehandlingFlyt {

        val behandlingTjeneste = BehandlingTjeneste
        val studentRepository = InMemoryStudentRepository

        return BehandlingFlytBuilder()
            .medSteg(steg = StartBehandlingSteg(behandlingTjeneste))
            .medSteg(steg = VurderAlderSteg(behandlingTjeneste), informasjonskrav = listOf(PersonopplysningService()))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.VURDER_LOVVALG))
            .medSteg(steg = VurderStudentSteg(behandlingTjeneste, studentRepository))
            .medSteg(steg = VurderYrkesskadeÅrsakssammenhengSteg(behandlingTjeneste, studentRepository), informasjonskrav = listOf(YrkesskadeService()))
            .medSteg(steg = VurderSykdomSteg(behandlingTjeneste, studentRepository))
            .medSteg(steg = FritakMeldepliktSteg(behandlingTjeneste))
            .medSteg(steg = VurderBistandsbehovSteg(behandlingTjeneste, studentRepository))
            .medSteg(steg = VurderSykepengeErstatningSteg(behandlingTjeneste))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.VURDER_MEDLEMSKAP))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.FASTSETT_GRUNNLAG))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.FASTSETT_UTTAK))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.BARNETILLEGG))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.SAMORDNING))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.SIMULERING))
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.BEREGN_TILKJENT_YTELSE))
            .medSteg(steg = ForeslåVedtakSteg(behandlingTjeneste)) // en-trinn
            .medSteg(steg = FatteVedtakSteg(behandlingTjeneste)) // to-trinn
            .medSteg(steg = GeneriskPlaceholderSteg(StegType.IVERKSETT_VEDTAK))
            .build()
    }

    override fun identifikator(): String {
        return "ae0034"
    }
}