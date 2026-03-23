package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarHelseinstitusjonLøsning
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.beregnTidligsteReduksjonsdatoPerOpphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AvklarHelseinstitusjonLøser(
    private val behandlingRepository: BehandlingRepository,
    private val helseinstitusjonRepository: InstitusjonsoppholdRepository
) : AvklaringsbehovsLøser<AvklarHelseinstitusjonLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        helseinstitusjonRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarHelseinstitusjonLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId())
        val vurdertAv = kontekst.bruker.ident

        validerReduksjonsdatoForInstitusjonsopphold(
            behandling,
            løsning.helseinstitusjonVurdering.vurderinger
        ).throwOnInvalid {
            UgyldigForespørselException(it.errorMessage)
        }

        val vedtatteVurderinger =
            behandling.forrigeBehandlingId?.let { helseinstitusjonRepository.hentHvisEksisterer(it) }

        val nåværendeGrunnlag = helseinstitusjonRepository.hentHvisEksisterer(behandling.id)

        val oppdaterteVurderinger =
            slåSammenMedNyeVurderingerNy(
                vedtatteVurderinger,
                nåværendeGrunnlag,
                løsning.helseinstitusjonVurdering.vurderinger,
                behandling,
                vurdertAv
            )

        helseinstitusjonRepository.lagreHelseVurdering(
            kontekst.kontekst.behandlingId,
            oppdaterteVurderinger
        )

        return LøsningsResultat(løsning.helseinstitusjonVurdering.vurderinger.joinToString(" ") { it.begrunnelse })
    }

    private fun slåSammenMedNyeVurderingerNy(
        vedtatteVurderinger: InstitusjonsoppholdGrunnlag?,
        nåværendeGrunnlag: InstitusjonsoppholdGrunnlag?,
        nyeVurderinger: List<HelseinstitusjonVurderingDto>,
        behandling: Behandling,
        vurdertAv: String,
    ): List<HelseinstitusjonVurdering> {

        val eksisterendeTidslinje = byggTidslinjeForHelseoppholdvurderinger(vedtatteVurderinger)
            .justerTilNåværendeOpphold(vedtatteVurderinger, nåværendeGrunnlag, behandling.id)

        val nyeVurderingerTidslinje = Tidslinje(nyeVurderinger.sortedBy { it.periode }
            .map {
                Segment(
                    it.periode,
                    HelseoppholdVurderingData(
                        begrunnelse = it.begrunnelse,
                        faarFriKostOgLosji = it.faarFriKostOgLosji,
                        forsoergerEktefelle = it.forsoergerEktefelle,
                        harFasteUtgifter = it.harFasteUtgifter,
                        vurdertIBehandling = behandling.id,
                        vurdertAv = vurdertAv,
                        vurdertTidspunkt = LocalDateTime.now()
                    )
                )
            }).komprimer()

        return eksisterendeTidslinje.kombiner(
            nyeVurderingerTidslinje,
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        ).komprimer().segmenter().map {
            HelseinstitusjonVurdering(
                begrunnelse = it.verdi.begrunnelse,
                faarFriKostOgLosji = it.verdi.faarFriKostOgLosji,
                forsoergerEktefelle = it.verdi.forsoergerEktefelle,
                harFasteUtgifter = it.verdi.harFasteUtgifter,
                periode = it.periode,
                vurdertIBehandling = it.verdi.vurdertIBehandling,
                vurdertAv = vurdertAv,
                vurdertTidspunkt = it.verdi.vurdertTidspunkt
            )
        }
    }

    private fun Tidslinje<HelseoppholdVurderingData>.justerTilNåværendeOpphold(
        forrigeGrunnlag: InstitusjonsoppholdGrunnlag?,
        nåværendeGrunnlag: InstitusjonsoppholdGrunnlag?,
        nåværendeBehandlingId: BehandlingId
    ): Tidslinje<HelseoppholdVurderingData> {
        if (forrigeGrunnlag == null || nåværendeGrunnlag == null) {
            return this
        }

        val forrigeOpphold = forrigeGrunnlag.oppholdene?.opphold
            ?.filter { it.verdi.type == Institusjonstype.HS }
            .orEmpty()
        val nåværendeOpphold = nåværendeGrunnlag.oppholdene?.opphold
            ?.filter { it.verdi.type == Institusjonstype.HS }
            .orEmpty()

        // Match opphold på fom-dato — samme fom = samme opphold, men kan ha endret tom
        val oppholdEndringer: Map<LocalDate, LocalDate> = forrigeOpphold
            .mapNotNull { forrige ->
                val nåværende = nåværendeOpphold.firstOrNull { it.periode.fom.isEqual(forrige.periode.fom) }
                if (nåværende != null && !nåværende.periode.tom.isEqual(forrige.periode.tom)) {
                    forrige.periode.fom to nåværende.periode.tom
                } else null
            }.toMap()

        val justerteSegmenter = if (oppholdEndringer.isEmpty()) {
            this.segmenter()
        } else {
            segmenter().map { segment ->
                // Finn hvilket forrige opphold denne vurderingen tilhører
                val tilhørendeOppholdFom = forrigeOpphold
                    .firstOrNull { segment.periode.fom >= it.periode.fom && segment.periode.tom <= it.periode.tom }
                    ?.periode?.fom

                val nyOppholdTom = tilhørendeOppholdFom?.let { oppholdEndringer[it] }

                if (nyOppholdTom != null) {
                    Segment(
                        Periode(segment.periode.fom, nyOppholdTom),
                        segment.verdi.copy(
                            vurdertIBehandling = nåværendeBehandlingId,
                            vurdertTidspunkt = LocalDateTime.now()
                        )
                    )
                } else {
                    segment
                }
            }
        }

        return Tidslinje(justerteSegmenter)
    }

    private fun byggTidslinjeForHelseoppholdvurderinger(grunnlag: InstitusjonsoppholdGrunnlag?): Tidslinje<HelseoppholdVurderingData> {
        return grunnlag?.helseoppholdvurderinger?.tilTidslinje()
            ?.mapValue {
                HelseoppholdVurderingData(
                    begrunnelse = it.begrunnelse,
                    faarFriKostOgLosji = it.faarFriKostOgLosji,
                    forsoergerEktefelle = it.forsoergerEktefelle,
                    harFasteUtgifter = it.harFasteUtgifter,
                    vurdertIBehandling = it.vurdertIBehandling,
                    vurdertAv = it.vurdertAv,
                    vurdertTidspunkt = it.vurdertTidspunkt
                )
            }.orEmpty()
    }

    private fun validerReduksjonsdatoForInstitusjonsopphold(
        behandling: Behandling,
        nyeVurderinger: List<HelseinstitusjonVurderingDto>
    ): Validation<List<HelseinstitusjonVurderingDto>> {
        val grunnlag = helseinstitusjonRepository.hentHvisEksisterer(behandling.id)
        val opphold = grunnlag?.oppholdene?.opphold ?: emptyList()
        if (opphold.isEmpty() || nyeVurderinger.isEmpty()) return Validation.Valid(nyeVurderinger)

        // Håndterer når vedtatte vurderinger finnes. Dette skjer i revurdering
        val vurderingerPerOpphold: Map<Segment<Institusjon>, List<HelseinstitusjonVurderingDto>> =
            // Finn vurderinger per opphold ved å matche oppholdets periode med vurderingenes periode.
            opphold.associateWith { o ->
                nyeVurderinger
                    .filter { v -> v.periode.fom >= o.periode.fom && v.periode.tom <= o.periode.tom }
                    .sortedBy { it.periode }
            }

        // Henter forhåndsberegnet tidligste reduksjonsdato per opphold fra util
        val tidligsteReduksjonsdatoPerOpphold = beregnTidligsteReduksjonsdatoPerOpphold(opphold)

        vurderingerPerOpphold.entries.forEach { (oppholdSegment, vurderinger) ->
            val tidligsteReduksjonsdato = tidligsteReduksjonsdatoPerOpphold[oppholdSegment] ?: return@forEach
            val første = førsteReduksjonsvurdering(vurderinger)
            val resultat = validerReduksjonsdato(
                vurderinger,
                første,
                tidligsteReduksjonsdato
            )
            if (resultat != null) return resultat
        }

        return Validation.Valid(nyeVurderinger)
    }

    private fun førsteReduksjonsvurdering(vurderinger: List<HelseinstitusjonVurderingDto>): HelseinstitusjonVurderingDto? {
        return vurderinger.firstOrNull {
            it.faarFriKostOgLosji && it.forsoergerEktefelle == false && it.harFasteUtgifter == false
        }
    }

    private fun validerReduksjonsdato(
        vurderinger: List<HelseinstitusjonVurderingDto>,
        førsteReduksjonsvurdering: HelseinstitusjonVurderingDto?,
        tidligsteReduksjonsdato: LocalDate
    ): Validation<List<HelseinstitusjonVurderingDto>>? {
        if (førsteReduksjonsvurdering != null && førsteReduksjonsvurdering.periode.fom.isBefore(tidligsteReduksjonsdato)) {
            val tidligsteReduksjonsdatoFormatert =
                tidligsteReduksjonsdato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            return Validation.Invalid(
                vurderinger,
                "Reduksjonsvurdering starter for tidlig. Skal ikke starte før $tidligsteReduksjonsdatoFormatert"
            )
        }
        return null
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_HELSEINSTITUSJON
    }

    internal data class HelseoppholdVurderingData(
        val begrunnelse: String,
        val faarFriKostOgLosji: Boolean,
        val forsoergerEktefelle: Boolean? = null,
        val harFasteUtgifter: Boolean? = null,
        val vurdertIBehandling: BehandlingId,
        val vurdertAv: String? = null,
        val vurdertTidspunkt: LocalDateTime? = null
    )
}