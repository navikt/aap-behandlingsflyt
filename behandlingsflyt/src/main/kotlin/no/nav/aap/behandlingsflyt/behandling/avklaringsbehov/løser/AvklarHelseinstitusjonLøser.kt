package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarHelseinstitusjonLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarHelseinstitusjonLøser(
    private val behandlingRepository: BehandlingRepository,
    private val helseinstitusjonRepository: InstitusjonsoppholdRepository,
) : AvklaringsbehovsLøser<AvklarHelseinstitusjonLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        helseinstitusjonRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarHelseinstitusjonLøsning
    ): LøsningsResultat {
        // FIXME Thao validerGyldigVurderinger. Valider tidligste reduksjonsdato og periode er riktig etter hverandre. Se eksempel i AvklarForutgåendeMedlemskapLøser

        val behandling = behandlingRepository.hent(kontekst.behandlingId())

        val vedtatteVurderinger = //TODO Thao: Tar denne hensyn til avbryt revurdering? Forrige behandling kan være avbrutt og vurderinger fra den skal ikke være med.
            behandling.forrigeBehandlingId?.let { helseinstitusjonRepository.hentHvisEksisterer(it) }

        val oppdaterteVurderinger =
            slåSammenMedNyeVurderinger(
                vedtatteVurderinger,
                løsning.helseinstitusjonVurdering.vurderinger,
                behandling
            )
        helseinstitusjonRepository.lagreHelseVurdering(
            kontekst.kontekst.behandlingId,
            kontekst.bruker.ident,
            oppdaterteVurderinger
        )

        return LøsningsResultat(løsning.helseinstitusjonVurdering.vurderinger.joinToString(" ") { it.begrunnelse })
    }

    private fun slåSammenMedNyeVurderinger(
        grunnlag: InstitusjonsoppholdGrunnlag?,
        nyeVurderinger: List<HelseinstitusjonVurderingDto>,
        behandling: Behandling
    ): List<HelseinstitusjonVurdering> {
        val eksisterendeTidslinje = byggTidslinjeForHelseoppholdvurderinger(grunnlag, behandling.forrigeBehandlingId)

        val nyeVurderingerTidslinje = Tidslinje(nyeVurderinger.sortedBy { it.periode }
            .map {
                Segment(
                    it.periode,
                    HelseoppholdVurderingData(
                        begrunnelse = it.begrunnelse,
                        faarFriKostOgLosji = it.faarFriKostOgLosji,
                        forsoergerEktefelle = it.forsoergerEktefelle,
                        harFasteUtgifter = it.harFasteUtgifter,
                        behandlingId = behandling.id
                    )
                )
            }).komprimer()

        val kombinert = eksisterendeTidslinje.kombiner(
            nyeVurderingerTidslinje,
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        ).segmenter().map {
            HelseinstitusjonVurdering(
                begrunnelse = it.verdi.begrunnelse,
                faarFriKostOgLosji = it.verdi.faarFriKostOgLosji,
                forsoergerEktefelle = it.verdi.forsoergerEktefelle,
                harFasteUtgifter = it.verdi.harFasteUtgifter,
                periode = it.periode,
                vurdertIBehandling = it.verdi.behandlingId
            )
        }.toMutableList()

        // Legg til manglende segmenter fra eksisterendeTidslinje
        eksisterendeTidslinje.segmenter().forEach { eksisterende ->
            val finnes = kombinert.any {
                it.periode == eksisterende.periode &&
                it.vurdertIBehandling == eksisterende.verdi.behandlingId
            }
            if (!finnes) {
                kombinert.add(
                    HelseinstitusjonVurdering(
                        begrunnelse = eksisterende.verdi.begrunnelse,
                        faarFriKostOgLosji = eksisterende.verdi.faarFriKostOgLosji,
                        forsoergerEktefelle = eksisterende.verdi.forsoergerEktefelle,
                        harFasteUtgifter = eksisterende.verdi.harFasteUtgifter,
                        periode = eksisterende.periode,
                        vurdertIBehandling = eksisterende.verdi.behandlingId
                    )
                )
            }
        }
        return kombinert
    }

    private fun byggTidslinjeForHelseoppholdvurderinger(grunnlag: InstitusjonsoppholdGrunnlag?, behandlingId: BehandlingId?): Tidslinje<HelseoppholdVurderingData> {
        if (grunnlag == null || behandlingId == null) {
            return Tidslinje()
        }
        return grunnlag.helseoppholdvurderinger?.tilTidslinje()
            ?.mapValue {
                HelseoppholdVurderingData(
                    begrunnelse = it.begrunnelse,
                    faarFriKostOgLosji = it.faarFriKostOgLosji,
                    forsoergerEktefelle = it.forsoergerEktefelle,
                    harFasteUtgifter = it.harFasteUtgifter,
                    behandlingId = behandlingId
                )
            }.orEmpty()
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_HELSEINSTITUSJON
    }

    internal data class HelseoppholdVurderingData(
        val begrunnelse: String,
        val faarFriKostOgLosji: Boolean,
        val forsoergerEktefelle: Boolean? = null,
        val harFasteUtgifter: Boolean? = null,
        val behandlingId: BehandlingId,
    )
}