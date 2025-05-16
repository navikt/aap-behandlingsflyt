package no.nav.aap.behandlingsflyt.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontor
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KlageresultatUtlederTest {

    @Test
    fun `Skal gi avslag ved formkrav ikke oppfylt`() {
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
            formkravVurdering, null, null, null
        )

        assertThat(klageresultat).isEqualTo(
            Avslått(avslagsÅrsak = AvslagsÅrsak.IKKE_OVERHOLDT_FORMKRAV)
        )
    }
    
    @Test
    fun `Skal gi null ved manglende vurdering`() {
        val klageresultat = KlageresultatUtleder.utledKlagebehandlingResultat(
            null, null, null, null
        )

        assertThat(klageresultat).isNull()
    }
    
    @Test
    fun `Skal gi opprettholdes ved kun NAY-vurdering dersom NAY-vurdering er opprettholdes`() {
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
            formkravVurdering, behandlendeEnhetVurdering, nayVurdering, null
        )

        assertThat(klageresultat).isEqualTo(
            Opprettholdes(vilkårSomSkalOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5))
        )
    }
    
    @Test
    fun `Skal gi delvis opprettholdelse hvis ett av kontorene har omgjøring, og det andre ikke`() {
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
            vurdertAv = "Saksbehandler",
            skalBehandlesAvNay = false,
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
            formkravVurdering, behandlendeEnhetVurdering, nayVurdering, kontorVurdering
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
            vurdertAv = "Saksbehandler",
            skalBehandlesAvNay = false,
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
            formkravVurdering, behandlendeEnhetVurdering, nayVurdering, kontorVurdering
        )
        assertThat(klageresultat).isEqualTo(
            DelvisOmgjøres(
                vilkårSomSkalOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6),
                vilkårSomSkalOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
            )
        )
    }
}