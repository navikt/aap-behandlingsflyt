package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.steg.lovvalg.LovvalgInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnbeløpInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Aktivitetsplikt11_7Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Aktivitetsplikt11_9Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.SøknadInformasjonskrav
import no.nav.aap.behandlingsflyt.steg.barnetillegg.BarnInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektInformasjonskrav
import no.nav.aap.behandlingsflyt.steg.institusjon.InstitusjonsoppholdInformasjonskrav
import no.nav.aap.behandlingsflyt.steg.medlemskap.ForutgåendeMedlemskapInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningForutgåendeInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreSøknadInformasjonskrav
import no.nav.aap.behandlingsflyt.steg.yrkesskade.YrkesskadeInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeInformasjonskrav
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.steg.arbeidsopptrapping.ArbeidsopptrappingSteg
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.steg.avbrytrevurdering.AvbrytRevurderingSteg
import no.nav.aap.behandlingsflyt.steg.krav.AvklarStønadsperiodeSteg
import no.nav.aap.behandlingsflyt.steg.BekreftVurderingerOppfølgingSteg
import no.nav.aap.behandlingsflyt.steg.tilkjentytelse.BeregnTilkjentYtelseSteg
import no.nav.aap.behandlingsflyt.steg.beregning.BeregningAvklarFaktaSteg
import no.nav.aap.behandlingsflyt.steg.tilkjentytelse.Effektuer11_7Steg
import no.nav.aap.behandlingsflyt.steg.egenvirksomhet.EtableringEgenVirksomhetSteg
import no.nav.aap.behandlingsflyt.steg.FastsettArbeidsevneSteg
import no.nav.aap.behandlingsflyt.steg.beregning.FastsettGrunnlagSteg
import no.nav.aap.behandlingsflyt.steg.meldeperiode.FastsettMeldeperiodeSteg
import no.nav.aap.behandlingsflyt.steg.fattevedtak.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.steg.foreslåvedtak.ForeslåVedtakSteg
import no.nav.aap.behandlingsflyt.steg.ForeslåVedtakVedtakslengdeSteg
import no.nav.aap.behandlingsflyt.steg.meldeplikt.FritakMeldepliktSteg
import no.nav.aap.behandlingsflyt.steg.meldeplikt.IkkeOppfyltMeldepliktSteg
import no.nav.aap.behandlingsflyt.steg.institusjon.InstitusjonsoppholdSteg
import no.nav.aap.behandlingsflyt.steg.iverksett.IverksettVedtakSteg
import no.nav.aap.behandlingsflyt.steg.krav.KravSteg
import no.nav.aap.behandlingsflyt.steg.kvalitetssikring.KvalitetssikringsSteg
import no.nav.aap.behandlingsflyt.steg.beregning.ManglendeLigningGrunnlagSteg
import no.nav.aap.behandlingsflyt.steg.MeldingOmVedtakBrevSteg
import no.nav.aap.behandlingsflyt.steg.OpprettRevurderingSteg
import no.nav.aap.behandlingsflyt.steg.overgangarbeid.OvergangArbeidSteg
import no.nav.aap.behandlingsflyt.steg.overgangufore.OvergangUføreSteg
import no.nav.aap.behandlingsflyt.steg.RefusjonkravSteg
import no.nav.aap.behandlingsflyt.steg.krav.RettighetsperiodeSteg
import no.nav.aap.behandlingsflyt.steg.rettighetstype.RettighetstypeSteg
import no.nav.aap.behandlingsflyt.steg.samordning.SamordningAndreStatligeYtelserSteg
import no.nav.aap.behandlingsflyt.steg.samordning.SamordningArbeidsgiverSteg
import no.nav.aap.behandlingsflyt.steg.samordning.SamordningAvslagSteg
import no.nav.aap.behandlingsflyt.steg.samordning.SamordningBarnepensjonSteg
import no.nav.aap.behandlingsflyt.steg.samordning.SamordningSteg
import no.nav.aap.behandlingsflyt.steg.samordning.SamordningUføreSteg
import no.nav.aap.behandlingsflyt.steg.SendForvaltningsmeldingSteg
import no.nav.aap.behandlingsflyt.steg.SimulerUtbetalingSteg
import no.nav.aap.behandlingsflyt.steg.StartBehandlingSteg
import no.nav.aap.behandlingsflyt.steg.sykdom.SykdomsvurderingBrevSteg
import no.nav.aap.behandlingsflyt.steg.samordning.SykestipendSteg
import no.nav.aap.behandlingsflyt.steg.krav.SøknadSteg
import no.nav.aap.behandlingsflyt.steg.TjenestepensjonRefusjonskravSteg
import no.nav.aap.behandlingsflyt.steg.underveis.UnderveisSteg
import no.nav.aap.behandlingsflyt.steg.vedtakslengde.VedtakslengdeSteg
import no.nav.aap.behandlingsflyt.steg.VisGrunnlagSteg
import no.nav.aap.behandlingsflyt.steg.alder.VurderAlderSteg
import no.nav.aap.behandlingsflyt.steg.VurderAvslag11_27Steg
import no.nav.aap.behandlingsflyt.steg.bistand.VurderBistandsbehovSteg
import no.nav.aap.behandlingsflyt.steg.medlemskap.VurderForutgåendeMedlemskapSteg
import no.nav.aap.behandlingsflyt.steg.lovvalg.VurderLovvalgSteg
import no.nav.aap.behandlingsflyt.steg.oppholdskrav.VurderOppholdskravSteg
import no.nav.aap.behandlingsflyt.steg.sykepengeerstatning.VurderSykepengeErstatningSteg
import no.nav.aap.behandlingsflyt.steg.yrkesskade.VurderYrkesskadeSteg
import no.nav.aap.behandlingsflyt.steg.barnetillegg.BarnetilleggSteg
import no.nav.aap.behandlingsflyt.steg.inntektsbortfall.InntektsbortfallSteg
import no.nav.aap.behandlingsflyt.steg.samordning.andrestatligeytelservurdering.DagpengerInformasjonskrav
import no.nav.aap.behandlingsflyt.steg.samordning.andrestatligeytelservurdering.TiltakspengerInformasjonskrav
import no.nav.aap.behandlingsflyt.steg.samordning.tjenestepensjon.TjenestePensjonInformasjonskrav
import no.nav.aap.behandlingsflyt.steg.samordning.ytelsevurdering.SamordningYtelseVurderingInformasjonskrav
import no.nav.aap.behandlingsflyt.steg.student.AvklarStudentStegV2
import no.nav.aap.behandlingsflyt.steg.student.VurderStudentSteg
import no.nav.aap.behandlingsflyt.steg.sykdom.FastsettSykdomsvilkåretSteg
import no.nav.aap.behandlingsflyt.steg.sykdom.VurderSykdomSteg

object Revurdering : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(
                steg = StartBehandlingSteg,
                informasjonskrav = listOf(SøknadInformasjonskrav, BarnInformasjonskrav),  // TODO: Mulig vi ønsker å endre disse ifb krav?
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle()
            )
            .medSteg(
                steg = KravSteg,
                informasjonskrav = emptyList(),
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.MOTTATT_SØKNAD, Vurderingsbehov.MIGRERING_FRA_ARENA, Vurderingsbehov.VURDER_KRAV)
            )
            .medSteg(
                steg = SendForvaltningsmeldingSteg,
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.MOTTATT_SØKNAD, Vurderingsbehov.MIGRERING_FRA_ARENA),
                informasjonskrav = emptyList()
            )
            .medSteg(
                steg = AvklarStønadsperiodeSteg,
                informasjonskrav = emptyList(),
                vurderingsbehovRelevanteForSteg = emptyList()
            )
            .medSteg(
                steg = AvbrytRevurderingSteg,
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.REVURDERING_AVBRUTT),
            )
            .medSteg(
                steg = SøknadSteg,
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.SØKNAD_TRUKKET),
            )
            .medSteg(
                steg = RettighetsperiodeSteg,
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = VurderLovvalgSteg,
                informasjonskrav = listOf(PersonopplysningInformasjonskrav, LovvalgInformasjonskrav),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.REVURDER_LOVVALG,
                    Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(steg = FastsettMeldeperiodeSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = VurderAlderSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = VurderAvslag11_27Steg, vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.VURDER_AVSLAG_11_27))
            .medSteg(
                steg = VurderStudentSteg,
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.REVURDER_STUDENT,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = VurderSykdomSteg,
                // UføreService trengs her for å trigge ytterligere nedsatt arbeidsevne-vurdering
                informasjonskrav = listOf(
                    YrkesskadeInformasjonskrav,
                    LegeerklæringInformasjonskrav,
                    UføreInformasjonskrav
                ),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.BRUKER_TILBAKE_I_ARBEID,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.DØDSFALL_BRUKER,
                    Vurderingsbehov.OVERGANG_ARBEID,
                    Vurderingsbehov.REVURDER_STUDENT,
                )
            )
            .medSteg(
                steg = VurderBistandsbehovSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.BRUKER_TILBAKE_I_ARBEID,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.DØDSFALL_BRUKER,
                    Vurderingsbehov.OVERGANG_UFORE,
                )
            )
            .medSteg(
                steg = EtableringEgenVirksomhetSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.ETABLERING_EGEN_VIRKSOMHET,
                )
            )
            .medSteg(
                steg = ArbeidsopptrappingSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.BRUKER_TILBAKE_I_ARBEID,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.VURDER_ARBEIDSOPPTRAPPING,
                )
            )
            .medSteg(
                steg = FritakMeldepliktSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.BRUKER_TILBAKE_I_ARBEID,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.VURDER_FRITAK_MELDEPLIKT,
                )
            )
            .medSteg(
                steg = FastsettArbeidsevneSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.BRUKER_TILBAKE_I_ARBEID,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.FASTSETT_ARBEIDSEVNE,
                )
            )
            .medSteg(
                steg = OvergangUføreSteg,
                informasjonskrav = listOf(
                    UføreSøknadInformasjonskrav,
                    UføreInformasjonskrav
                ),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.BRUKER_TILBAKE_I_ARBEID,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.OVERGANG_UFORE,
                    Vurderingsbehov.OVERGANG_UFORE_AUTOMATISK_STANS,
                )
            )
            .medSteg(
                steg = OvergangArbeidSteg,
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.BRUKER_TILBAKE_I_ARBEID,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.OVERGANG_ARBEID,
                )
            )
            .medSteg(
                steg = RefusjonkravSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.REFUSJONSKRAV,
                )
            )
            .medSteg(
                steg = SykdomsvurderingBrevSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.BRUKER_TILBAKE_I_ARBEID,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = BekreftVurderingerOppfølgingSteg,
                vurderingsbehovRelevanteForSteg = emptyList()
            )
            .medSteg(steg = KvalitetssikringsSteg, vurderingsbehovRelevanteForSteg = emptyList())
            .medSteg(
                steg = VurderYrkesskadeSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.REVURDER_YRKESSKADE,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = AvklarStudentStegV2, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.REVURDER_STUDENT,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = VurderSykepengeErstatningSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.MOTTATT_DIALOGMELDING,
                    Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
                    Vurderingsbehov.REVURDER_SYKEPENGEERSTATNING,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(steg = FastsettSykdomsvilkåretSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(
                steg = BeregningAvklarFaktaSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.REVURDER_BEREGNING,
                    Vurderingsbehov.REVURDER_YRKESSKADE,
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP,
                )
            )
            .medSteg(steg = VisGrunnlagSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(
                steg = ManglendeLigningGrunnlagSteg,
                informasjonskrav = listOf(InntektInformasjonskrav),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.REVURDER_BEREGNING,
                    Vurderingsbehov.REVURDER_YRKESSKADE,
                    Vurderingsbehov.REVURDER_MANUELL_INNTEKT,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = FastsettGrunnlagSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.REVURDER_BEREGNING,
                    Vurderingsbehov.REVURDER_YRKESSKADE,
                    Vurderingsbehov.REVURDER_MANUELL_INNTEKT,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = InntektsbortfallSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.REVURDER_INNTEKTSBORTFALL,
                )
            )
            .medSteg(
                steg = VurderForutgåendeMedlemskapSteg,
                informasjonskrav = listOf(
                    PersonopplysningForutgåendeInformasjonskrav,
                    ForutgåendeMedlemskapInformasjonskrav
                ),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.REVURDER_MEDLEMSKAP,
                    Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP,
                    Vurderingsbehov.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                steg = VurderOppholdskravSteg,
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.OPPHOLDSKRAV,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                )
            )
            .medSteg(
                // TODO: Midlertidig duplikat av BarnService, skal på sikt kun være i StartBehandlingSteg
                informasjonskrav = listOf(BarnInformasjonskrav),
                steg = BarnetilleggSteg,
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.BARNETILLEGG,
                    Vurderingsbehov.DØDSFALL_BARN,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                )
            )
            .medSteg(
                steg = InstitusjonsoppholdSteg,
                informasjonskrav = listOf(InstitusjonsoppholdInformasjonskrav),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.INSTITUSJONSOPPHOLD_SONING,
                    Vurderingsbehov.INSTITUSJONSOPPHOLD_HELSEINSTITUSJON,
                    Vurderingsbehov.INSTITUSJONSOPPHOLD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.MOTTATT_SØKNAD
                )
            )
            .medSteg(
                steg = SamordningSteg,
                informasjonskrav = listOf(SamordningYtelseVurderingInformasjonskrav),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.SAMORDNING_OG_AVREGNING,
                    Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER,
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                ),
            )
            .medSteg(
                steg = SamordningUføreSteg,
                informasjonskrav = listOf(UføreInformasjonskrav),
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.REVURDER_SAMORDNING_UFØRE)
            )
            .medSteg(
                steg = TjenestepensjonRefusjonskravSteg,
                informasjonskrav = listOf(TjenestePensjonInformasjonskrav),
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON)
            )
            .medSteg(
                steg = SamordningArbeidsgiverSteg,
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.REVURDER_SAMORDNING_ARBEIDSGIVER)
            )
            .medSteg(steg = SamordningAvslagSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(
                steg = SamordningBarnepensjonSteg,
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.REVURDER_SAMORDNING_BARNEPENSJON)
            )
            .medSteg(
                steg = SykestipendSteg,
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.REVURDER_STUDENT,
                    Vurderingsbehov.REVURDER_SYKESTIPEND
                )
            )
            .medSteg(
                steg = SamordningAndreStatligeYtelserSteg,
                informasjonskrav = listOf(DagpengerInformasjonskrav, TiltakspengerInformasjonskrav),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.MOTTATT_SØKNAD,
                    Vurderingsbehov.MIGRERING_FRA_ARENA,
                    Vurderingsbehov.SAMORDNING_OG_AVREGNING,
                    Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_STATLIGE_YTELSER,
                    Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                    Vurderingsbehov.HELHETLIG_VURDERING,
                    Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP,
                )
            )
            .medSteg(
                steg = Effektuer11_7Steg,
                informasjonskrav = listOf(Aktivitetsplikt11_7Informasjonskrav),
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle()
            )
            .medSteg(steg = RettighetstypeSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(
                steg = VedtakslengdeSteg,
                informasjonskrav = listOf(VedtakslengdeInformasjonskrav),
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.VEDTAKSLENGDE_MANUELT
                )
            )
            .medSteg(
                steg = ForeslåVedtakVedtakslengdeSteg,
                vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.VEDTAKSLENGDE_MANUELT
                )
            )
            .medSteg(
                steg = IkkeOppfyltMeldepliktSteg,
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering(),
                informasjonskrav = listOf(MeldekortInformasjonskrav)
            )
            .medSteg(steg = UnderveisSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(
                steg = BeregnTilkjentYtelseSteg,
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering(),
                informasjonskrav = listOf(Aktivitetsplikt11_9Informasjonskrav, GrunnbeløpInformasjonskrav)
            )
            .medSteg(
                steg = SimulerUtbetalingSteg,
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering()
            )
            .medSteg(
                steg = ForeslåVedtakSteg,
                vurderingsbehovRelevanteForSteg = emptyList()
            ) // en-trinn
            .sluttÅOppdatereFaktagrunnlag()
            .medSteg(
                steg = FatteVedtakSteg,
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering()
            ) // to-trinn
            .medSteg(
                steg = IverksettVedtakSteg,
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alleInklusivGRegulering()
            )
            .medSteg(
                steg = MeldingOmVedtakBrevSteg,
                vurderingsbehovRelevanteForSteg = Vurderingsbehov.alleInklusivGReguleringUnntattMigrering()
            )
            .medSteg(
                steg = OpprettRevurderingSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle()
            )
            .build()
    }

}

