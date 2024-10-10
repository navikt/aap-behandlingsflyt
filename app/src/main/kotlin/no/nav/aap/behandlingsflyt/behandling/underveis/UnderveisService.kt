package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetSted
import no.nav.aap.behandlingsflyt.behandling.etannetsted.Institusjon
import no.nav.aap.behandlingsflyt.behandling.etannetsted.Soning
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivtBidragRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.GraderingArbeidRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.InstitusjonRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.RettTilRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.SoningRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UnderveisInput
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.PliktkortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.SoningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurdering
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class UnderveisService(
    private val behandlingService: SakOgBehandlingService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val pliktkortRepository: PliktkortRepository,
    private val underveisRepository: UnderveisRepository,
    private val bruddAktivitetspliktRepository: BruddAktivitetspliktRepository,
    private val barnetilleggRepository: BarnetilleggRepository,
    private val soningRepository: SoningRepository,
    private val helseInstitusjonRepository: HelseinstitusjonRepository
) {

    private val kvoteService = KvoteService()

    private val regelset = listOf(
        RettTilRegel(),
        InstitusjonRegel(),
        SoningRegel(),
        MeldepliktRegel(),
        AktivtBidragRegel(),
        ReduksjonAktivitetspliktRegel(),
        FraværFastsattAktivitetRegel(),
        GraderingArbeidRegel(),
        VarighetRegel(),
    )

    fun vurder(behandlingId: BehandlingId): Tidslinje<Vurdering> {
        val input = genererInput(behandlingId)

        val vurderRegler = vurderRegler(input)
        underveisRepository.lagre(
            behandlingId,
            vurderRegler.segmenter()
                .map {
                    Underveisperiode(
                        it.periode,
                        it.verdi.meldeperiode(),
                        it.verdi.utfall(),
                        it.verdi.avslagsårsak(),
                        it.verdi.grenseverdi(),
                        it.verdi.gradering(),
                    )
                })
        return vurderRegler
    }

    internal fun vurderRegler(input: UnderveisInput): Tidslinje<Vurdering> {
        return regelset.fold(Tidslinje()) { resultat, regel ->
            regel.vurder(input, resultat)
        }
    }

    private fun genererInput(behandlingId: BehandlingId): UnderveisInput {
        val sak = behandlingService.hentSakFor(behandlingId)
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val relevanteVilkår = vilkårsresultat
            .alle()
            .filter { v ->
                v.type in setOf(
                    Vilkårtype.ALDERSVILKÅRET,
                    Vilkårtype.MEDLEMSKAP,
                    Vilkårtype.SYKDOMSVILKÅRET,
                    Vilkårtype.BISTANDSVILKÅRET
                )
            }

        val pliktkortGrunnlag = pliktkortRepository.hentHvisEksisterer(behandlingId)
        val pliktkort = pliktkortGrunnlag?.pliktkort() ?: listOf()
        val innsendingsTidspunkt = pliktkortGrunnlag?.innsendingsdatoPerMelding() ?: mapOf()
        val kvote = kvoteService.beregn(behandlingId)

        val soningVurdering = soningRepository.hentAktivSoningsvurderingHvisEksisterer(behandlingId)
        val helseInstitusjonVurdering =
            helseInstitusjonRepository.hentAktivHelseinstitusjonVurderingHvisEksisterer(behandlingId)
        val etAnnetSted = mapTilEtAnnetSted(soningVurdering, helseInstitusjonVurdering)

        val barnetilleggGrunnlag = requireNotNull(barnetilleggRepository.hentHvisEksisterer(behandlingId))
        val bruddAktivitetspliktGrunnlag = bruddAktivitetspliktRepository.hentGrunnlagHvisEksisterer(behandlingId)
            ?: BruddAktivitetspliktGrunnlag(bruddene = setOf())

        return UnderveisInput(
            rettighetsperiode = sak.rettighetsperiode,
            relevanteVilkår = relevanteVilkår,
            opptrappingPerioder = listOf(),
            pliktkort = pliktkort,
            innsendingsTidspunkt = innsendingsTidspunkt,
            kvote = kvote,
            bruddAktivitetspliktGrunnlag = bruddAktivitetspliktGrunnlag,
            etAnnetSted = etAnnetSted,
            barnetillegg = barnetilleggGrunnlag
        )
    }

    private fun mapTilEtAnnetSted(
        soningVurdering: Soningsvurdering?,
        helseInstitusjon: HelseinstitusjonVurdering?
    ): List<EtAnnetSted> {
        //TODO: Kan foreløpig ikke vurdere om formueUnderForvaltning er sant, mangler data
        val etAnnetStedList = mutableListOf<EtAnnetSted>()
        soningVurdering?.let {
            etAnnetStedList.add(
                EtAnnetSted(
                    it.periode,
                    soning = Soning(
                        true,
                        false,
                        soningUtenforFengsel = it.soningUtenforFengsel,
                        arbeidUtenforAnstalt = it.arbeidUtenforAnstalt ?: false
                    ),
                    begrunnelse = it.begrunnelse ?: ""
                )
            )
        }

        helseInstitusjon?.let {
            etAnnetStedList.add(
                EtAnnetSted(
                    it.periode,
                    institusjon = Institusjon(true, it.forsoergerEktefelle ?: false, it.harFasteUtgifter ?: false),
                    begrunnelse = it.begrunnelse
                )
            )
        }

        return listOf()
    }
}