package no.nav.aap.behandlingsflyt.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontor
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNay
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Avslått
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Omgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Opprettholdes
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Trukket
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Ufullstendig
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.ÅrsakTilAvslag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.ÅrsakTilUfullstendigResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

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
            false, LocalDate.now().plusWeeks(1), formkravVurdering, null, null, null
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

        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            false, LocalDate.now().minusWeeks(1), formkravVurdering, null, null, null
        )

        assertThat(klageresultat).isEqualTo(
            Avslått(årsak = ÅrsakTilAvslag.IKKE_OVERHOLDT_FORMKRAV)
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
            false, null, formkravVurdering, null, null, null
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
            false, null,oppfylteFormkrav, behandlendeEnhetVurdering, nayVurdering, null
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
            false, null,oppfylteFormkrav, behandlendeEnhetVurdering, nayVurdering, null
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
            false, null,oppfylteFormkrav, behandlendeEnhetVurdering, nayVurdering, null
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
            false, null, oppfylteFormkrav, behandlendeEnhetVurdering, nayVurdering, kontorVurdering
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
            false,  null,oppfylteFormkrav, behandlendeEnhetVurdering, nayVurdering, kontorVurdering
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
        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(false, null, null, null, null, null)
        assertThat(klageresultat).isEqualTo(Ufullstendig(ÅrsakTilUfullstendigResultat.MANGLER_VURDERING))
    }
    
    @Test
    fun `Skal gi Trukket ved klage trukket`() {
        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(true, null, null, null, null, null)
        assertThat(klageresultat).isEqualTo(Trukket)
    }

    @Test
    fun `Skal gi Ufullstendig dersom formkrav er oppfylt, men behandlendeEnhetVurdering mangler`() {
        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(false, null, oppfylteFormkrav, null, null, null)
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
            false, null, oppfylteFormkrav, behandlendeEnhetVurdering, nayVurdering, null
        )
        assertThat(klageresultat).isEqualTo(Ufullstendig(ÅrsakTilUfullstendigResultat.MANGLER_VURDERING))
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