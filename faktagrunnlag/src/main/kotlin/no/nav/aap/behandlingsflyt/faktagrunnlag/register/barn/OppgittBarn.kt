package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.verdityper.sakogbehandling.Ident

class OppgittBarn(val id: Long? = null, var identer: Set<Ident>?, fnr: String?=null)  {
    init {
        if (identer.isNullOrEmpty()){
            identer = if (fnr != null) setOf(Ident(fnr)) else emptySet()
        } else {
            identer!!.plus(if (fnr != null) setOf(Ident(fnr)) else emptySet())
        }
    }
}

/*
export type ManuelleBarn = {
  navn: Navn;
  internId: string;
  fødseldato: Date;
  relasjon: Relasjon;
  vedlegg?: Vedlegg[];
  fnr?: string; // Det er kun dette feltet vi bryr oss om
};
 */