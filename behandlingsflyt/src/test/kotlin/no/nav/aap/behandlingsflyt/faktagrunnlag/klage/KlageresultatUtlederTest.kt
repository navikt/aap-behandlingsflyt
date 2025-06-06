package no.nav.aap.behandlingsflyt.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontor
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KlageresultatUtlederTest {

    @Test
    fun `Skal gi ufullstendig resultat dersom man venter på svar på forhåndsvarsel`() {
        val formkravVurdering = FormkravVurdering(
            begrunnelse = "Ikke oppfylt",
            erBrukerPart = false,
            erFristOverholdt = true,
            erKonkret = true,
            erSignert = true,
            vurdertAv = "Saksbehandler",
            likevelBehandles = null
        )

        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            formkravVurdering, null, null, null, null
        )

        assertThat(klageresultat).isEqualTo(
            Ufullstendig(ÅrsakTilUfullstendigResultat.VENTER_PÅ_SVAR_FRA_BRUKER)
        )
    }

    @Test
    fun `Skal gi avslag ved formkrav ikke oppfylt, dersom forhåndsvarsel har løpt ut`() {
        val formkravVurdering = FormkravVurdering(
            begrunnelse = "Ikke oppfylt",
            erBrukerPart = false,
            erFristOverholdt = true,
            erKonkret = true,
            erSignert = true,
            vurdertAv = "Saksbehandler",
            likevelBehandles = null
        )
        val effektuerAvvistPåFormkravVurdering = EffektuerAvvistPåFormkravVurdering(skalEndeligAvvises = true)

        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            formkravVurdering, null, null, null, effektuerAvvistPåFormkravVurdering
        )

        assertThat(klageresultat).isEqualTo(
            Avslått(årsak = ÅrsakTilAvslag.IKKE_OVERHOLDT_FORMKRAV)
        )
    }

    @Test
    fun `Skal gi inkonsistent resultat dersom formkrav ikke er oppfylt, men ikke skal effektueres`() {
        val formkravVurdering = FormkravVurdering(
            begrunnelse = "Ikke oppfylt",
            erBrukerPart = false,
            erFristOverholdt = true,
            erKonkret = true,
            erSignert = true,
            vurdertAv = "Saksbehandler",
            likevelBehandles = null
        )
        val effektuerAvvistPåFormkravVurdering = EffektuerAvvistPåFormkravVurdering(skalEndeligAvvises = false)

        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            formkravVurdering, null, null, null, effektuerAvvistPåFormkravVurdering
        )

        assertThat(klageresultat).isEqualTo(
            Ufullstendig(årsak = ÅrsakTilUfullstendigResultat.INKONSISTENT_FORMKRAV_VURDERING)
        )
    }

    @Test
    fun `Skal gi inkonsistent resultat dersom formkrav er oppfylt, men avslag på forkrav skal effektueres`() {
        val formkravVurdering = FormkravVurdering(
            begrunnelse = "Oppfylt",
            erBrukerPart = true,
            erFristOverholdt = true,
            erKonkret = true,
            erSignert = true,
            vurdertAv = "Saksbehandler",
            likevelBehandles = null
        )
        val behandlendeEnhetVurdering = BehandlendeEnhetVurdering(
            skalBehandlesAvKontor = false, skalBehandlesAvNay = true, vurdertAv = "Saksbehandler",
        )
        val nayVurdering = KlagevurderingNay(
            innstilling = KlageInnstilling.OPPRETTHOLD,
            vurdertAv = "Saksbehandler",
            begrunnelse = "Opprettholdes",
            vilkårSomOmgjøres = emptyList(),
            vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5),
            notat = null
        )
        
        val effektuerAvvistPåFormkravVurdering = EffektuerAvvistPåFormkravVurdering(skalEndeligAvvises = true)

        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            formkravVurdering, behandlendeEnhetVurdering, nayVurdering, null, effektuerAvvistPåFormkravVurdering
        )

        assertThat(klageresultat).isEqualTo(
            Ufullstendig(årsak = ÅrsakTilUfullstendigResultat.INKONSISTENT_FORMKRAV_VURDERING)
        )
    }

    @Test
    fun `Skal gi avslag ved frist ikke overholdt`() {
        val formkravVurdering = FormkravVurdering(
            begrunnelse = "Ikke oppfylt",
            erBrukerPart = false,
            erFristOverholdt = false,
            erKonkret = true,
            erSignert = true,
            vurdertAv = "Saksbehandler",
            likevelBehandles = false
        )

        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            formkravVurdering, null, null, null, null
        )

        assertThat(klageresultat).isEqualTo(
            Avslått(årsak = ÅrsakTilAvslag.IKKE_OVERHOLDT_FRIST)
        )
    }

    @Test
    fun `Skal gi opprettholdes ved kun NAY-vurdering dersom NAY-vurdering er opprettholdes`() {
        val behandlendeEnhetVurdering = BehandlendeEnhetVurdering(
            vurdertAv = "Saksbehandler",
            skalBehandlesAvNay = true,
            skalBehandlesAvKontor = false
        )

        val nayVurdering = KlagevurderingNay(
            innstilling = KlageInnstilling.OPPRETTHOLD,
            vurdertAv = "Saksbehandler",
            begrunnelse = "Opprettholdes",
            vilkårSomOmgjøres = emptyList(),
            vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5),
            notat = null
        )

        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            oppfylteFormkrav, behandlendeEnhetVurdering, nayVurdering, null, null
        )

        assertThat(klageresultat).isEqualTo(
            Opprettholdes(vilkårSomSkalOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5))
        )
    }

    @Test
    fun `Skal gi delvis omgjør ved kun NAY-vurdering dersom NAY-vurdering er delvis omgjør`() {
        val behandlendeEnhetVurdering = BehandlendeEnhetVurdering(
            vurdertAv = "Saksbehandler",
            skalBehandlesAvNay = true,
            skalBehandlesAvKontor = false
        )

        val nayVurdering = KlagevurderingNay(
            innstilling = KlageInnstilling.DELVIS_OMGJØR,
            vurdertAv = "Saksbehandler",
            begrunnelse = "Opprettholdes",
            vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6),
            vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5),
            notat = null
        )

        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            oppfylteFormkrav, behandlendeEnhetVurdering, nayVurdering, null, null
        )

        assertThat(klageresultat).isEqualTo(
            DelvisOmgjøres(
                vilkårSomSkalOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5),
                vilkårSomSkalOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6)
            )
        )
    }

    @Test
    fun `Skal gi omgjør ved kun NAY-vurdering dersom NAY-vurdering er omgjør`() {
        val behandlendeEnhetVurdering = BehandlendeEnhetVurdering(
            vurdertAv = "Saksbehandler",
            skalBehandlesAvNay = true,
            skalBehandlesAvKontor = false
        )

        val nayVurdering = KlagevurderingNay(
            innstilling = KlageInnstilling.OMGJØR,
            vurdertAv = "Saksbehandler",
            begrunnelse = "Opprettholdes",
            vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6),
            vilkårSomOpprettholdes = emptyList(),
            notat = null
        )

        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            oppfylteFormkrav, behandlendeEnhetVurdering, nayVurdering, null, null
        )

        assertThat(klageresultat).isEqualTo(
            Omgjøres(
                vilkårSomSkalOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6)
            )
        )
    }

    @Test
    fun `Skal gi delvis opprettholdelse hvis ett av kontorene har omgjøring, og det andre ikke`() {
        val behandlendeEnhetVurdering = BehandlendeEnhetVurdering(
            vurdertAv = "Saksbehandler",
            skalBehandlesAvNay = true,
            skalBehandlesAvKontor = true
        )

        val nayVurdering = KlagevurderingNay(
            innstilling = KlageInnstilling.OPPRETTHOLD,
            vurdertAv = "Saksbehandler",
            begrunnelse = "Opprettholdes",
            vilkårSomOmgjøres = emptyList(),
            vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5),
            notat = null
        )

        val kontorVurdering = KlagevurderingKontor(
            innstilling = KlageInnstilling.OMGJØR,
            vurdertAv = "Saksbehandler",
            begrunnelse = "Omgjøres",
            vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6),
            vilkårSomOpprettholdes = emptyList(),
            notat = null
        )

        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            oppfylteFormkrav, behandlendeEnhetVurdering, nayVurdering, kontorVurdering, null
        )
        assertThat(klageresultat).isEqualTo(
            DelvisOmgjøres(
                vilkårSomSkalOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6),
                vilkårSomSkalOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
            )
        )

    }

    @Test
    fun `Skal gi delvis omgjøring dersom resultatet fra ett er delvis omgjøring`() {
        val behandlendeEnhetVurdering = BehandlendeEnhetVurdering(
            vurdertAv = "Saksbehandler",
            skalBehandlesAvNay = true,
            skalBehandlesAvKontor = true
        )

        val nayVurdering = KlagevurderingNay(
            innstilling = KlageInnstilling.OPPRETTHOLD,
            vurdertAv = "Saksbehandler",
            begrunnelse = "Opprettholdes",
            vilkårSomOmgjøres = emptyList(),
            vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5),
            notat = null
        )

        val kontorVurdering = KlagevurderingKontor(
            innstilling = KlageInnstilling.DELVIS_OMGJØR,
            vurdertAv = "Saksbehandler",
            begrunnelse = "Omgjøres",
            vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6),
            vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5),
            notat = null
        )

        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            oppfylteFormkrav, behandlendeEnhetVurdering, nayVurdering, kontorVurdering, null
        )
        assertThat(klageresultat).isEqualTo(
            DelvisOmgjøres(
                vilkårSomSkalOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6),
                vilkårSomSkalOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
            )
        )
    }


    @Test
    fun `Skal gi Ufullstendig ved manglende vurdering`() {
        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(null, null, null, null, null)
        assertThat(klageresultat).isEqualTo(Ufullstendig(ÅrsakTilUfullstendigResultat.MANGLER_VURDERING))
    }


    @Test
    fun `Skal gi Ufullstendig dersom formkrav er oppfylt, men behandlendeEnhetVurdering mangler`() {
        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(oppfylteFormkrav, null, null, null, null)
        assertThat(klageresultat).isEqualTo(Ufullstendig(ÅrsakTilUfullstendigResultat.MANGLER_VURDERING))
    }

    @Test
    fun `Skal gi Ufullstendig dersom det mangler vurdering fra minst ett av kontorene som skal vurdere`() {

        val behandlendeEnhetVurdering = BehandlendeEnhetVurdering(
            vurdertAv = "Saksbehandler",
            skalBehandlesAvNay = true,
            skalBehandlesAvKontor = true
        )

        val nayVurdering = KlagevurderingNay(
            innstilling = KlageInnstilling.OMGJØR,
            vurdertAv = "Saksbehandler",
            begrunnelse = "Omgjøres",
            vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6),
            vilkårSomOpprettholdes = emptyList(),
            notat = null
        )

        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            oppfylteFormkrav, behandlendeEnhetVurdering, nayVurdering, null, null
        )
        assertThat(klageresultat).isEqualTo(Ufullstendig(ÅrsakTilUfullstendigResultat.MANGLER_VURDERING))

    }

    @Test
    fun `Skal gi Ufullstendig dersom vurderingene mellom Nay og Kontor er inkonsistente`() {
        val behandlendeEnhetVurdering = BehandlendeEnhetVurdering(
            vurdertAv = "Saksbehandler",
            skalBehandlesAvNay = true,
            skalBehandlesAvKontor = true
        )
        val nayVurdering = KlagevurderingNay(
            innstilling = KlageInnstilling.OMGJØR,
            vurdertAv = "Saksbehandler",
            begrunnelse = "Omgjøres",
            vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6),
            vilkårSomOpprettholdes = emptyList(),
            notat = null
        )
        val kontorVurdering = KlagevurderingKontor(
            innstilling = KlageInnstilling.OPPRETTHOLD,
            vurdertAv = "Saksbehandler",
            begrunnelse = "Opprettholdes",
            vilkårSomOmgjøres = emptyList(),
            vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6),
            notat = null,
        )
        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            oppfylteFormkrav, behandlendeEnhetVurdering, nayVurdering, kontorVurdering, null
        )
        assertThat(klageresultat).isEqualTo(Ufullstendig(ÅrsakTilUfullstendigResultat.INKONSISTENT_KLAGE_VURDERING))
    }

    companion object {
        val oppfylteFormkrav = FormkravVurdering(
            begrunnelse = "Oppfylt",
            erBrukerPart = true,
            erFristOverholdt = true,
            erKonkret = true,
            erSignert = true,
            vurdertAv = "Saksbehandler",
            likevelBehandles = null
        )
    }
}