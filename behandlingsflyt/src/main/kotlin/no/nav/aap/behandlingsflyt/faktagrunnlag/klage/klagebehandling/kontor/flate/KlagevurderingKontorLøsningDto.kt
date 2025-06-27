package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontor
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException

data class KlagevurderingKontorLøsningDto(
    val begrunnelse: String,
    val notat: String?,
    val innstilling: KlageInnstilling,
    val vilkårSomOpprettholdes: List<Hjemmel>,
    val vilkårSomOmgjøres: List<Hjemmel>,
) {
    init {
        when (innstilling) {
            KlageInnstilling.OPPRETTHOLD -> {
                if (vilkårSomOpprettholdes.isEmpty()) throw UgyldigForespørselException("Må sette vilkår som skal opprettholdes dersom innstilling er 'OPPRETTHOLD' ")
                if (vilkårSomOmgjøres.isNotEmpty()) throw UgyldigForespørselException("Kan ikke sette vilkår som skal omgjøres dersom innstilling er 'OPPRETTHOLD' ")
            }

            KlageInnstilling.OMGJØR -> {
                if (vilkårSomOmgjøres.isEmpty()) throw UgyldigForespørselException("Må sette vilkår som skal omgjøres dersom innstilling er 'OMGJØR' ")
                if (vilkårSomOpprettholdes.isNotEmpty()) throw UgyldigForespørselException("Kan ikke sette vilkår som skal opprettholdes dersom innstilling er 'OMGJØR' ")
            }

            KlageInnstilling.DELVIS_OMGJØR -> {
                if (vilkårSomOmgjøres.isEmpty()) throw UgyldigForespørselException("Må sette vilkår som skal omgjøres dersom innstilling er 'DELVIS_OMGJØR' ")
                if (vilkårSomOpprettholdes.isEmpty()) throw UgyldigForespørselException("Må sette vilkår som skal opprettholdes dersom innstilling er 'DELVIS_OMGJØR' ")
            }
        }
    }

    fun tilVurdering(vurdertAv: Bruker) = KlagevurderingKontor(
        begrunnelse = begrunnelse,
        notat = notat,
        innstilling = innstilling,
        vilkårSomOpprettholdes = vilkårSomOpprettholdes,
        vilkårSomOmgjøres = vilkårSomOmgjøres,
        vurdertAv = vurdertAv.ident
    )
}