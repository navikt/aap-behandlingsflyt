package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.komponenter.verdityper.Bruker

sealed interface DokumentInput {
    val brudd: Brudd
    val innsender: Bruker
    val dokumentType: DokumentType
    val begrunnelse: String
}

class RegistreringInput(
    override val brudd: Brudd,
    override val innsender: Bruker,
    override val begrunnelse: String,
    val grunn: Grunn
): DokumentInput {
    override val dokumentType = DokumentType.REGISTRERING
}

class FeilregistreringInput(
    override val brudd: Brudd,
    override val innsender: Bruker,
    override val begrunnelse: String,
): DokumentInput {
    override val dokumentType = DokumentType.FEILREGISTRERING
}