package no.nav.aap.behandlingsflyt.steg.krav

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.krav.KravVurdering
import no.nav.aap.krav.RelevantKrav
import no.nav.aap.krav.SøknadsdatoÅrsak

object KravValidering {
    fun erKravVurderingTilstrekkeligVurdert(søknaderIBehandling: Set<MottattDokument>, kravVurderinger: Set<KravVurdering>): Boolean {
        val erAlleSøknaderIBehandlingVurdert =
            søknaderIBehandling.all { søknad -> kravVurderinger.any { it.journalpostId == søknad.referanse.asJournalpostId } }

        return erAlleSøknaderIBehandlingVurdert
    }

    fun validerRelevantKrav(
        vurdering: RelevantKrav,
        søknadForVurdering: MottattDokument?
    ) {
        if (søknadForVurdering != null) {
            if (vurdering.søknadsdato.årsak == SøknadsdatoÅrsak.SøknadMottatt && søknadForVurdering.mottattTidspunkt.toLocalDate() != vurdering.søknadsdato.dato) {
                throw UgyldigForespørselException("Søknadsdato for krav må være lik mottatt dato for den digitaliserte søknaden.")
            }
        }
        val overstyrMuligRettFra = vurdering.overstyrMuligRettFra
        if (overstyrMuligRettFra != null && overstyrMuligRettFra.dato > vurdering.søknadsdato.dato) {
            throw UgyldigForespørselException("Med rett fra annen dato enn søknadsdato må den nye rettighetsdatoen være tidligere enn søknadsdatoen.")
        }
    }
}