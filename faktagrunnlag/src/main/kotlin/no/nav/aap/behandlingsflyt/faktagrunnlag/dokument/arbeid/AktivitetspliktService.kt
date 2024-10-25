package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

class AktivitetspliktService(
    private val repository: AktivitetspliktRepository,
) {
    fun registrerBrudd(
        bruddAktivitetsplikt: List<AktivitetspliktRepository.DokumentInput>
    ): InnsendingId {
        for (dokument in bruddAktivitetsplikt) {
            if (dokument is AktivitetspliktRepository.FeilregistreringInput) {
                val eksisterendeBrudd = repository.hentBrudd(dokument.brudd)
                require(eksisterendeBrudd.any { it is AktivitetspliktRegistrering}) {
                    """
                        |Kan ikke feilregistrere brudd som ikke overlapper nøyaktig med eksisterende brudd.
                        |Ved behov kan dette støttes.
                    """.trimMargin()
                }
            }
        }

        return repository.lagreBrudd(bruddAktivitetsplikt)
    }
}