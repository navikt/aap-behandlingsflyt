package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarHelseinstitusjonLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AvklarHelseinstitusjonLøser(
    private val behandlingRepository: BehandlingRepository,
    private val helseinstitusjonRepository: InstitusjonsoppholdRepository,
    private val unleashGateway: UnleashGateway
) : AvklaringsbehovsLøser<AvklarHelseinstitusjonLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        helseinstitusjonRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarHelseinstitusjonLøsning
    ): LøsningsResultat {
        return if (unleashGateway.isEnabled(BehandlingsflytFeature.PeriodiseringHelseinstitusjonOpphold)) {
            løsNy(løsning, kontekst)
        } else {
            løsGammel(løsning, kontekst)
        }
    }

    private fun løsGammel(løsning: AvklarHelseinstitusjonLøsning, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId())

        val vedtatteVurderinger =
            behandling.forrigeBehandlingId?.let { helseinstitusjonRepository.hentHvisEksisterer(it) }

        val oppdaterteVurderinger =
            slåSammenMedNyeVurderinger(
                vedtatteVurderinger,
                løsning.helseinstitusjonVurdering.vurderinger,
                kontekst.bruker.ident,
                behandling.id
            )
        helseinstitusjonRepository.lagreHelseVurdering(
            kontekst.kontekst.behandlingId,
            kontekst.bruker.ident,
            oppdaterteVurderinger
        )

        return LøsningsResultat(løsning.helseinstitusjonVurdering.vurderinger.joinToString(" ") { it.begrunnelse })
    }

    private fun løsNy(løsning: AvklarHelseinstitusjonLøsning, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
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

        val oppdaterteVurderinger =
            slåSammenMedNyeVurderingerNy(
                vedtatteVurderinger,
                løsning.helseinstitusjonVurdering.vurderinger,
                behandling,
                vurdertAv
            )

        helseinstitusjonRepository.lagreHelseVurdering(
            kontekst.kontekst.behandlingId,
            vurdertAv,
            oppdaterteVurderinger
        )

        return LøsningsResultat(løsning.helseinstitusjonVurdering.vurderinger.joinToString(" ") { it.begrunnelse })
    }

    private fun slåSammenMedNyeVurderinger(
        grunnlag: InstitusjonsoppholdGrunnlag?,
        nyeVurderinger: List<HelseinstitusjonVurderingDto>,
        vurdertAv: String,
        behandlingId: BehandlingId,
    ): List<HelseinstitusjonVurdering> {
        val eksisterendeTidslinje = byggTidslinjeForHelseoppholdvurderinger(grunnlag)

        val nyeVurderingerTidslinje = Tidslinje(nyeVurderinger.sortedBy { it.periode }
            .map {
                Segment(
                    it.periode,
                    HelseoppholdVurderingData(
                        begrunnelse = it.begrunnelse,
                        faarFriKostOgLosji = it.faarFriKostOgLosji,
                        forsoergerEktefelle = it.forsoergerEktefelle,
                        harFasteUtgifter = it.harFasteUtgifter,
                        vurdertIBehandling = behandlingId,
                        vurdertAv = vurdertAv,
                        vurdertTidspunkt = LocalDateTime.now()
                    )
                )
            }).komprimer()

        return eksisterendeTidslinje.kombiner(
            nyeVurderingerTidslinje,
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        ).segmenter().map {
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

    private fun slåSammenMedNyeVurderingerNy(
        grunnlag: InstitusjonsoppholdGrunnlag?,
        nyeVurderinger: List<HelseinstitusjonVurderingDto>,
        behandling: Behandling,
        vurdertAv: String,
    ): List<HelseinstitusjonVurdering> {
        val eksisterendeTidslinje = byggTidslinjeForHelseoppholdvurderinger(grunnlag)

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

        // Valider første opphold: Ingen reduksjon første 4 måneder (innleggelsesmåned + 3 måneder)
        vurderingerPerOpphold.entries.firstOrNull()?.let { (opphold, vurderinger) ->
            val første = førsteReduksjonsvurdering(vurderinger)
            val tidligsteReduksjonsdato = opphold.periode.fom.withDayOfMonth(1).plusMonths(4)
            val resultat = validerReduksjonsdato(
                vurderinger, første, tidligsteReduksjonsdato,
                "Første reduksjonsvurdering starter for tidlig. Skal ikke starte før"
            )

            if (resultat != null) return resultat
        }

        // Valider påfølgende opphold
        if (vurderingerPerOpphold.size > 1) {
            vurderingerPerOpphold.entries.toList()
                .windowed(2)
                .forEach { (forrige, nåværende) ->
                    val forrigeOpphold = forrige.key
                    val nåværendeOpphold = nåværende.key
                    val vurderingerNåværende = nåværende.value

                    val treMånederEtterUtskrivelse = forrigeOpphold.periode.tom.plusMonths(3)
                    val erInnenTreMåneder = !nåværendeOpphold.periode.fom.isAfter(treMånederEtterUtskrivelse)
                    val første = førsteReduksjonsvurdering(vurderingerNåværende)

                    if (!erInnenTreMåneder) {
                        // Nytt opphold behandles som første opphold (4 måneders karantene)
                        // Dette gjelder hvis oppholdet er mer enn 3 måneder etter forrige.
                        val tidligsteReduksjonsdato = nåværendeOpphold.periode.fom.withDayOfMonth(1).plusMonths(4)
                        val resultat = validerReduksjonsdato(
                            vurderingerNåværende, første, tidligsteReduksjonsdato,
                            "Reduksjon ved nytt opphold starter for tidlig. Skal ikke starte før"
                        )

                        if (resultat != null) return resultat
                    }
                }
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
        tidligsteReduksjonsdato: LocalDate,
        feilmelding: String
    ): Validation<List<HelseinstitusjonVurderingDto>>? {
        if (førsteReduksjonsvurdering != null && førsteReduksjonsvurdering.periode.fom.isBefore(tidligsteReduksjonsdato)) {
            val tidligsteReduksjonsdatoFormatert =
                tidligsteReduksjonsdato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            return Validation.Invalid(
                vurderinger,
                "$feilmelding $tidligsteReduksjonsdatoFormatert"
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
        val vurdertIBehandling: BehandlingId? = null,
        val vurdertAv: String? = null,
        val vurdertTidspunkt: LocalDateTime? = null
    )
}