package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.GjenopptakKravLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KlageKravLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravMedDato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravValidering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKravLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.TilleggsopplysningKravLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.TrukketSøknadKravLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class VurderKravLøser(
    private val kravRepository: KravRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
) :
    AvklaringsbehovsLøser<VurderKravLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(
        kravRepository = repositoryProvider.provide(),
        mottattDokumentRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderKravLøsning): LøsningsResultat {
        val søknaderIBehandling =
            mottattDokumentRepository.hentDokumenterAvType(kontekst.behandlingId(), InnsendingType.SØKNAD)

        val nyeVurderinger = løsning.kravVurderinger.map { vurderingDto ->
            vurderingDto.tilVurdering(kontekst.behandlingId(), kontekst.bruker, Instant.now())
        }.toSet()

        validerGyldighet(nyeVurderinger, søknaderIBehandling)

        val iverksatteVurderinger = kontekst.kontekst.forrigeBehandlingId?.let {
            kravRepository.hentHvisEksisterer(it)
        }?.vurderinger ?: emptySet()

        val alleVurderinger = nyeVurderinger + iverksatteVurderinger

        kravRepository.lagre(kontekst.behandlingId(), alleVurderinger)

        return LøsningsResultat("Fullført")
    }

    private fun validerKravMedDato(
        vurdering: KravMedDato,
        søknadForVurdering: MottattDokument?
    ) {
        if (søknadForVurdering != null) {
            if (vurdering.søknadsdato.årsak == SøknadsdatoÅrsak.SøknadMottatt && søknadForVurdering.mottattTidspunkt.toLocalDate() != vurdering.søknadsdato.dato) {
                throw UgyldigForespørselException("Søknadsdato for krav må være lik mottatt dato for den digitaliserte søknaden.")
            }
        }
        val muligRettFra = vurdering.muligRettFra
        if (muligRettFra != null && muligRettFra.dato > vurdering.søknadsdato.dato) {
            throw UgyldigForespørselException("Med rett fra annen dato enn søknadsdato må den nye rettighetsdatoen være tidligere enn søknadsdatoen.")
        }
    }

    private fun validerGyldighet(vurderinger: Set<KravVurdering>, søknaderIBehandling: Set<MottattDokument>) {
        if (!KravValidering.erKravVurderingTilstrekkeligVurdert(søknaderIBehandling, vurderinger)) {
            throw UgyldigForespørselException("Mangler vurdering av krav for innsendt søknad.")
        }

        vurderinger.forEach { vurdering ->
            if (vurdering is KravMedDato) {
                validerKravMedDato(
                    vurdering,
                    søknaderIBehandling.find { it.referanse.asJournalpostId == vurdering.journalpostId })
            }

        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_KRAV
    }

}

private fun KravVurderingLøsningDto.tilVurdering(
    behandlingId: BehandlingId,
    bruker: Bruker,
    opprettetTid: Instant,
): KravVurdering {
    return when (this) {
        is NyttKravLøsningDto -> NyttKrav(
            journalpostId = journalpostId,
            vurdertAv = bruker,
            begrunnelse = begrunnelse,
            vurdertIBehandling = behandlingId,
            opprettet = opprettetTid,
            søknadsdato = søknadsdato,
            muligRettFra = muligRettFra,
            kravdato = kravDato()
        )

        is TrukketSøknadKravLøsningDto, is GjenopptakKravLøsningDto, is KlageKravLøsningDto, is TilleggsopplysningKravLøsningDto -> throw UgyldigForespørselException(
            "Kelvin støtter foreløpig ikke ${this.kravType}."
        )
    }

}
