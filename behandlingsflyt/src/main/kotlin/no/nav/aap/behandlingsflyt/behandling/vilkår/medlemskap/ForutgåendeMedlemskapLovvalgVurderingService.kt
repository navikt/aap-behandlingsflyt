package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikkGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PersonStatus
import no.nav.aap.komponenter.type.Periode
import java.time.YearMonth

class ForutgåendeMedlemskapLovvalgVurderingService {
    fun vurderTilhørighet(grunnlag: ForutgåendeMedlemskapGrunnlag, rettighetsPeriode: Periode): KanBehandlesAutomatiskVurdering{
        val forutgåendePeriode = Periode(rettighetsPeriode.fom.minusYears(5), rettighetsPeriode.tom)
        val førsteDelVurderinger = vurderFørsteDelKriteier(grunnlag, forutgåendePeriode)
        val andreDelVurdering = vurderAndreDelKriterier(grunnlag, forutgåendePeriode)

        val oppfyltMinstEttKrav = førsteDelVurderinger.any{it.resultat}
        val ingenInntruffet = andreDelVurdering.all{!it.resultat}

        return KanBehandlesAutomatiskVurdering(
            oppfyltMinstEttKrav && ingenInntruffet,
            førsteDelVurderinger + andreDelVurdering
        )
    }

    // Minst én må oppfylles
    private fun vurderFørsteDelKriteier(grunnlag: ForutgåendeMedlemskapGrunnlag, forutgåendePeriode: Periode): List<TilhørighetVurdering> {
        val arbeidInntektINorgeVurdering = harArbeidInntektINorge(grunnlag.medlemskapArbeidInntektGrunnlag, forutgåendePeriode)
        val vedtakIMedl = harVedtakIMEDL(grunnlag.medlemskapArbeidInntektGrunnlag?.medlemskapGrunnlag, forutgåendePeriode)

        return listOf(arbeidInntektINorgeVurdering, vedtakIMedl)
    }

    // Ingen kan inntreffe
    private fun vurderAndreDelKriterier(grunnlag: ForutgåendeMedlemskapGrunnlag, forutgåendePeriode: Periode): List<TilhørighetVurdering> {
        val harJobbetIUtland = oppgittJobbetIUtland(grunnlag.nyeSoknadGrunnlag, forutgåendePeriode )
        val harHattUtenlandsOpphold = oppgittUtenlandsOpphold(grunnlag.nyeSoknadGrunnlag, forutgåendePeriode)
        val harUtenlandsAdresse = utenlandskAdresse(grunnlag.personopplysningGrunnlag, forutgåendePeriode)
        val annetLovvalgsland = lovvalgslandIkkeErNorge(grunnlag.medlemskapArbeidInntektGrunnlag?.medlemskapGrunnlag)
        val utenforEØS = manglerStatsborgerskapIEØSiPerioden(grunnlag.personopplysningGrunnlag, forutgåendePeriode)

        return listOf(harJobbetIUtland, harHattUtenlandsOpphold, harUtenlandsAdresse, annetLovvalgsland, utenforEØS)
    }

    private fun oppgittJobbetIUtland(grunnlag: UtenlandsOppholdData?, forutgåendePeriode: Periode): TilhørighetVurdering {
        if (grunnlag == null) {
            return TilhørighetVurdering(listOf(Kilde.SØKNAD), Indikasjon.UTENFOR_NORGE, "Mangler utenlandsdata fra søknad", true, VurdertPeriode.SISTE_5_ÅR.beskrivelse)
        }
        val relevantePerioder = grunnlag.utenlandsOpphold?.filter {
            (it.tilDato != null && forutgåendePeriode.inneholder(it.tilDato)) || (it.fraDato != null && forutgåendePeriode.inneholder(it.fraDato))
        }
        val arbeidUtlandPerioder: MutableList<OppgittJobbetIUtlandGrunnlag> = mutableListOf()

        val jobbUtenforNorge = when {
            !grunnlag.harBoddINorgeSiste5År -> {
                val harRelevanteUtlandsPerioderIJobb = relevantePerioder?.any { it.iArbeid } == true
                    || (grunnlag.harArbeidetINorgeSiste5År && grunnlag.iTilleggArbeidUtenforNorge && relevantePerioder?.isNotEmpty() == true)

                if (harRelevanteUtlandsPerioderIJobb) {
                    val mappedArbeidUtland = relevantePerioder!!.map { OppgittJobbetIUtlandGrunnlag(it.land, it.fraDato, it.tilDato) }
                    arbeidUtlandPerioder.addAll(mappedArbeidUtland)
                    true
                } else {
                    false
                }
            }

            grunnlag.arbeidetUtenforNorgeFørSykdom -> {
                val mappedArbeidUtland = relevantePerioder?.map { OppgittJobbetIUtlandGrunnlag(it.land, it.fraDato, it.tilDato) }
                if (mappedArbeidUtland != null) {
                    arbeidUtlandPerioder.addAll(mappedArbeidUtland)
                }
                relevantePerioder?.isNotEmpty() == true
            }

            else -> false
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.SØKNAD),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Arbeidet i utlandet siste 5 år",
            resultat = jobbUtenforNorge,
            oppgittJobbetIUtlandGrunnlag = arbeidUtlandPerioder,
            vurdertPeriode = VurdertPeriode.SISTE_5_ÅR.beskrivelse
        )
    }

    private fun oppgittUtenlandsOpphold(grunnlag: UtenlandsOppholdData?, forutgåendePeriode: Periode): TilhørighetVurdering {
        if (grunnlag == null) {
            return TilhørighetVurdering(listOf(Kilde.SØKNAD), Indikasjon.UTENFOR_NORGE, "Mangler utenlandsdata fra søknad", true, VurdertPeriode.SISTE_5_ÅR.beskrivelse)
        }

        val relevantePerioder = grunnlag.utenlandsOpphold?.filter {
            (it.tilDato != null && forutgåendePeriode.inneholder(it.tilDato)) || (it.fraDato != null && forutgåendePeriode.inneholder(it.fraDato)) && !it.iArbeid
        }

        val oppholdUtlandPerioder = relevantePerioder?.map {
            OppgittUtenlandsOppholdGrunnlag(
                land = it.land,
                tilDato = it.tilDato,
                fraDato = it.fraDato,
            )
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.SØKNAD),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Opphold i utlandet siste 5 år",
            resultat = !grunnlag.harBoddINorgeSiste5År,
            oppgittUtenlandsOppholdGrunnlag = oppholdUtlandPerioder,
            vurdertPeriode = VurdertPeriode.SISTE_5_ÅR.beskrivelse
        )
    }

    private fun utenlandskAdresse(grunnlag: PersonopplysningMedHistorikkGrunnlag, forutgåendePeriode: Periode): TilhørighetVurdering {
        val bosattUtenforNorge = grunnlag.brukerPersonopplysning.folkeregisterStatuser.any{it.status != PersonStatus.bosatt}

        val utenlandsAddresserGrunnlag = grunnlag.brukerPersonopplysning.utenlandsAddresser?.map {
            UtenlandsAdresseGrunnlag(
                gyldigFraOgMed = it.gyldigFraOgMed,
                gyldigTilOgMed = it.gyldigTilOgMed,
                adresseNavn = it.adresseNavn,
                postkode = it.postkode,
                bySted = it.bySted,
                landkode = it.landkode,
                adresseType = it.adresseType
            )
        }?.filter {
            (it.gyldigTilOgMed == null)
                || forutgåendePeriode.inneholder(it.gyldigTilOgMed.toLocalDate())
                || (it.gyldigFraOgMed != null && forutgåendePeriode.inneholder(it.gyldigFraOgMed.toLocalDate()))
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.PDL),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Har hatt utenlandsk adresse i perioden",
            resultat = bosattUtenforNorge || !utenlandsAddresserGrunnlag.isNullOrEmpty(),
            utenlandsAddresserGrunnlag = utenlandsAddresserGrunnlag,
            vurdertPeriode = VurdertPeriode.SISTE_5_ÅR.beskrivelse
        )
    }

    private fun lovvalgslandIkkeErNorge(grunnlag: MedlemskapUnntakGrunnlag?): TilhørighetVurdering {
        val lovvalgslandErIkkeNorge = grunnlag?.unntak?.firstOrNull{it.verdi.lovvalgsland != "NOR"}

        val medlGrunnlag = grunnlag?.unntak?.map {
            VedtakIMEDLGrunnlag(
                periode = Periode(it.periode.fom, it.periode.tom),
                lovvalgsland = it.verdi.lovvalgsland,
                grunnlag = it.verdi.grunnlag,
                kilde = it.verdi.kilde
            )
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.MEDL),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Vedtak om annet lovvalgsland finnes",
            resultat = lovvalgslandErIkkeNorge != null,
            vedtakImedlGrunnlag = medlGrunnlag,
            vurdertPeriode = VurdertPeriode.SISTE_5_ÅR.beskrivelse
        )
    }

    private fun manglerStatsborgerskapIEØSiPerioden(grunnlag: PersonopplysningMedHistorikkGrunnlag, forutgåendePeriode: Periode): TilhørighetVurdering {
        val fantStatsborgerskapUtenforEØSiPerioden = grunnlag.brukerPersonopplysning.statsborgerskap.any{it.land !in enumValues<EØSLand>().map { eøsLand -> eøsLand.name }}

        val manglerStatsborgerskapGrunnlag = grunnlag.brukerPersonopplysning.statsborgerskap.map {
            ManglerStatsborgerskapGrunnlag(
                land = it.land,
                gyldigFraOgMed = it.gyldigFraOgMed,
                gyldigTilOgMed = it.gyldigTilOgMed
            )
        }.filter {
            (it.gyldigTilOgMed == null)
                || forutgåendePeriode.inneholder(it.gyldigTilOgMed)
                || (it.gyldigFraOgMed != null && forutgåendePeriode.inneholder(it.gyldigFraOgMed))
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.PDL),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Har statsborgerskap utenfor EØS i perioden",
            resultat = fantStatsborgerskapUtenforEØSiPerioden,
            manglerStatsborgerskapGrunnlag = manglerStatsborgerskapGrunnlag,
            vurdertPeriode = VurdertPeriode.SISTE_5_ÅR.beskrivelse
        )
    }

    private fun harArbeidInntektINorge(grunnlag: ForutgåendeMedlemskapArbeidInntektGrunnlag?, forutgåendePeriode: Periode): TilhørighetVurdering {
        val inntekterINorgePerioder = grunnlag?.inntekterINorgeGrunnlag?.filter {
            EØSLand.erNorge(it.opptjeningsLand) || EØSLand.erNorge(it.skattemessigBosattLand)
        }?.map { it.periode }

        val sammenhengendeInntektSiste5År = sammenhengendePerioderAlleMndSiste5år(inntekterINorgePerioder, forutgåendePeriode)

        val arbeidInntektINorgeGrunnlag =
            grunnlag?.inntekterINorgeGrunnlag?.map {
                ArbeidInntektINorgeGrunnlag(
                    virksomhetId = it.identifikator,
                    virksomhetNavn = it.organisasjonsNavn,
                    beloep = it.beloep,
                    periode = Periode(it.periode.fom, it.periode.tom),
                )
            }


        return TilhørighetVurdering(
            kilde = listOf(Kilde.A_INNTEKT, Kilde.AA_REGISTERET, Kilde.EREG),
            indikasjon = Indikasjon.I_NORGE,
            opplysning = "Sammenhengende arbeid og inntekt i Norge siste 5 år",
            resultat = sammenhengendeInntektSiste5År,
            arbeidInntektINorgeGrunnlag = arbeidInntektINorgeGrunnlag,
            vurdertPeriode = VurdertPeriode.SISTE_5_ÅR.beskrivelse
        )
    }

    private fun sammenhengendePerioderAlleMndSiste5år(perioder: List<Periode>?, forutgåendePeriode: Periode): Boolean {
        if (perioder.isNullOrEmpty()) return false

        val startMnd = YearMonth.from(forutgåendePeriode.fom)
        val sluttMnd = YearMonth.from(forutgåendePeriode.fom.plusYears(5))

        var nåMnd = startMnd
        while (!nåMnd.isAfter(sluttMnd)) {
            val førsteDagIMnd = nåMnd.atDay(1)
            val sisteDagIMnd = nåMnd.atEndOfMonth()

            val mndPeriode = Periode(førsteDagIMnd, sisteDagIMnd)

            if (!perioder.any { it.overlapper(mndPeriode) }) {
                return false
            }
            nåMnd = nåMnd.plusMonths(1)
        }
        return true
    }

    private fun harVedtakIMEDL(grunnlag: MedlemskapUnntakGrunnlag?, forutgåendePeriode: Periode): TilhørighetVurdering {
        val erMedlemPerioder = grunnlag?.unntak?.filter{it.verdi.medlem}?.map { it.periode }
        val sammenhengendePeriodeIMEDL = sammenhengendePerioderAlleMndSiste5år(erMedlemPerioder, forutgåendePeriode)

        val medlGrunnlag = grunnlag?.unntak?.map {
            VedtakIMEDLGrunnlag(
                periode = Periode(it.periode.fom, it.periode.tom),
                lovvalgsland = it.verdi.lovvalgsland,
                grunnlag = it.verdi.grunnlag,
                kilde = it.verdi.kilde
            )
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.MEDL),
            indikasjon = Indikasjon.I_NORGE,
            opplysning = "Vedtak om pliktig eller frivillig medlemskap finnes i MEDL for perioden",
            resultat = sammenhengendePeriodeIMEDL,
            vedtakImedlGrunnlag = medlGrunnlag,
            vurdertPeriode = VurdertPeriode.SISTE_5_ÅR.beskrivelse
        )
    }
}