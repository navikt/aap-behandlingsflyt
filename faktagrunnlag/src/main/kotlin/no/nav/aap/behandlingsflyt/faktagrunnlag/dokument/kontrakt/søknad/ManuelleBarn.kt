package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad

data class ManuelleBarn(
    val relasjon: Relasjon,
    val fnr: String
)

enum class Relasjon {
    FORELDER, FOSTERFORELDER
}

/**
type ManuelleBarn = {
    navn: Navn;
    internId: string;
    fødseldato: Date;
    relasjon: Relasjon;
    vedlegg?: Vedlegg[];
    fnr?: string;
};

type Navn = {
fornavn?: string;
mellomnavn?: string;
etternavn?: string;
};

enum Relasjon {
FORELDER = 'FORELDER',
FOSTERFORELDER = 'FOSTERFORELDER',
}
 **/