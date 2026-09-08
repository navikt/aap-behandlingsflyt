package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException

interface Klagevurdering {
    val innstilling: KlageInnstilling
    val vilkårSomOpprettholdes: List<Hjemmel>
    val vilkårSomOmgjøres: List<Hjemmel>
    
    fun validerHjemler() {
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

        if (vilkårSomOmgjøres.contains(Hjemmel.FOLKETRYGDLOVEN_21_12)) throw UgyldigForespørselException("Støtter ikke § 21-12 for omgjøring; behandle opprinnelig klage i stedet")
        if (vilkårSomOmgjøres.contains(Hjemmel.FVL_31)) throw UgyldigForespørselException("Støtter ikke Fvl. § 31 for omgjøring; behandle opprinnelig klage i stedet")
    }
}