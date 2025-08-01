package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektTyper
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.komponenter.type.Periode

class MedlemskapLovvalgVurderingService {
    fun vurderTilhørighet(
        grunnlag: MedlemskapLovvalgGrunnlag,
        rettighetsPeriode: Periode
    ): KanBehandlesAutomatiskVurdering {
        val førsteDelVurderinger = vurderFørsteDelKriteier(grunnlag)
        val andreDelVurdering = vurderAndreDelKriterier(grunnlag, rettighetsPeriode)

        val oppfyltMinstEttKrav = førsteDelVurderinger.any { it.resultat }
        val ingenInntruffet = andreDelVurdering.all { !it.resultat }

        return KanBehandlesAutomatiskVurdering(
            oppfyltMinstEttKrav && ingenInntruffet,
            førsteDelVurderinger + andreDelVurdering
        )
    }

    // Minst én må oppfylles
    private fun vurderFørsteDelKriteier(grunnlag: MedlemskapLovvalgGrunnlag): List<TilhørighetVurdering> {
        val mottarSykepengerVurdering = mottarSykepenger(grunnlag.medlemskapArbeidInntektGrunnlag)
        val arbeidInntektINorgeVurdering = harArbeidInntektINorge(grunnlag.medlemskapArbeidInntektGrunnlag)
        val vedtakIMedl = harVedtakIMEDL(grunnlag.medlemskapArbeidInntektGrunnlag?.medlemskapGrunnlag)

        return listOf(mottarSykepengerVurdering, arbeidInntektINorgeVurdering, vedtakIMedl)
    }

    // Ingen kan inntreffe
    private fun vurderAndreDelKriterier(
        grunnlag: MedlemskapLovvalgGrunnlag,
        rettighetsPeriode: Periode
    ): List<TilhørighetVurdering> {
        val harJobbetIUtland = oppgittJobbetIUtland(grunnlag.nyeSoknadGrunnlag, rettighetsPeriode)
        val harHattUtenlandsOpphold = oppgittUtenlandsOpphold(grunnlag.nyeSoknadGrunnlag, rettighetsPeriode)
        val harUtenlandsAdresse = utenlandskAdresse(grunnlag.personopplysning, rettighetsPeriode)
        val annetLovvalgsland = lovvalgslandIkkeErNorge(grunnlag.medlemskapArbeidInntektGrunnlag?.medlemskapGrunnlag)
        val utenforEØS = manglerStatsborgerskapIEØS(grunnlag.personopplysning, rettighetsPeriode)

        return listOf(harJobbetIUtland, harHattUtenlandsOpphold, harUtenlandsAdresse, annetLovvalgsland, utenforEØS)
    }

    private fun oppgittJobbetIUtland(
        grunnlag: UtenlandsOppholdData?,
        rettighetsPeriode: Periode
    ): TilhørighetVurdering {
        if (grunnlag == null) {
            return TilhørighetVurdering(
                kilde = listOf(Kilde.SØKNAD),
                indikasjon = Indikasjon.UTENFOR_NORGE,
                opplysning = "Mangler utenlandsdata fra søknad",
                resultat = true,
                vurdertPeriode = VurdertPeriode.SØKNADSTIDSPUNKT.beskrivelse
            )
        }
        val relevantePerioder = grunnlag.utenlandsOpphold?.filter {
            (it.tilDato != null && rettighetsPeriode.inneholder(it.tilDato)) || (it.fraDato != null && rettighetsPeriode.inneholder(
                it.fraDato
            ))
        }

        val arbeidUtlandPerioder: MutableList<OppgittJobbetIUtlandGrunnlag> = mutableListOf()

        val jobbUtenforNorge = when {
            !grunnlag.harBoddINorgeSiste5År -> {
                val harRelevanteUtlandsPerioderIJobb = relevantePerioder?.any { it.iArbeid } == true
                        || (grunnlag.harArbeidetINorgeSiste5År && grunnlag.iTilleggArbeidUtenforNorge && relevantePerioder?.isNotEmpty() == true)

                if (harRelevanteUtlandsPerioderIJobb) {
                    val mappedArbeidUtland =
                        relevantePerioder.map { OppgittJobbetIUtlandGrunnlag(it.land, it.fraDato, it.tilDato) }
                    arbeidUtlandPerioder.addAll(mappedArbeidUtland)
                    true
                } else {
                    false
                }
            }

            grunnlag.arbeidetUtenforNorgeFørSykdom -> {
                val mappedArbeidUtland =
                    relevantePerioder?.map { OppgittJobbetIUtlandGrunnlag(it.land, it.fraDato, it.tilDato) }
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
            opplysning = "Arbeid i utland",
            resultat = jobbUtenforNorge,
            oppgittJobbetIUtlandGrunnlag = arbeidUtlandPerioder,
            vurdertPeriode = VurdertPeriode.SØKNADSTIDSPUNKT.beskrivelse
        )
    }

    private fun oppgittUtenlandsOpphold(
        grunnlag: UtenlandsOppholdData?,
        rettighetsPeriode: Periode
    ): TilhørighetVurdering {
        if (grunnlag == null) {
            return TilhørighetVurdering(
                listOf(Kilde.SØKNAD),
                Indikasjon.UTENFOR_NORGE,
                "Mangler utenlandsdata fra søknad",
                true,
                VurdertPeriode.SØKNADSTIDSPUNKT.beskrivelse
            )
        }

        val relevantePerioder = grunnlag.utenlandsOpphold?.filter {
            (it.tilDato != null && rettighetsPeriode.inneholder(it.tilDato)) || (it.fraDato != null && rettighetsPeriode.inneholder(
                it.fraDato
            )) && !it.iArbeid
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
            opplysning = "Opphold i utland",
            resultat = !grunnlag.harBoddINorgeSiste5År,
            oppgittUtenlandsOppholdGrunnlag = oppholdUtlandPerioder,
            vurdertPeriode = VurdertPeriode.SØKNADSTIDSPUNKT.beskrivelse
        )
    }

    private fun utenlandskAdresse(grunnlag: Personopplysning, rettighetsPeriode: Periode): TilhørighetVurdering {
        val bosattUtenforNorge = grunnlag.status != PersonStatus.bosatt

        val utenlandsAddresserGrunnlag = grunnlag.utenlandsAddresser?.map {
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
                    || rettighetsPeriode.inneholder(it.gyldigTilOgMed)
                    || (it.gyldigFraOgMed != null && rettighetsPeriode.inneholder(it.gyldigFraOgMed))
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.PDL),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Utenlandsk adresse",
            resultat = bosattUtenforNorge || !utenlandsAddresserGrunnlag.isNullOrEmpty(),
            utenlandsAddresserGrunnlag = utenlandsAddresserGrunnlag,
            vurdertPeriode = VurdertPeriode.SØKNADSTIDSPUNKT.beskrivelse
        )
    }

    private fun lovvalgslandIkkeErNorge(grunnlag: MedlemskapUnntakGrunnlag?): TilhørighetVurdering {
        val lovvalgslandErIkkeNorge = grunnlag?.unntak?.firstOrNull { it.verdi.lovvalgsland != "NOR" }
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
            vurdertPeriode = VurdertPeriode.SØKNADSTIDSPUNKT.beskrivelse
        )
    }

    private fun manglerStatsborgerskapIEØS(
        grunnlag: Personopplysning,
        rettighetsPeriode: Periode
    ): TilhørighetVurdering {
        val manglerEØS =
            grunnlag.statsborgerskap.none { it.land in enumValues<EØSLand>().map { eøsLand -> eøsLand.name } }
        val manglerStatsborgerskapGrunnlag = grunnlag.statsborgerskap.map {
            ManglerStatsborgerskapGrunnlag(
                land = it.land,
                gyldigFraOgMed = it.gyldigFraOgMed,
                gyldigTilOgMed = it.gyldigTilOgMed
            )
        }.filter {
            (it.gyldigTilOgMed == null)
                    || rettighetsPeriode.inneholder(it.gyldigTilOgMed)
                    || (it.gyldigFraOgMed != null && rettighetsPeriode.inneholder(it.gyldigFraOgMed))
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.PDL),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Mangler statsborgerskap i EØS",
            resultat = manglerEØS,
            manglerStatsborgerskapGrunnlag = manglerStatsborgerskapGrunnlag,
            vurdertPeriode = VurdertPeriode.SØKNADSTIDSPUNKT.beskrivelse
        )
    }

    private fun mottarSykepenger(grunnlag: MedlemskapArbeidInntektGrunnlag?): TilhørighetVurdering {
        val sykepengerInntektGrunnlag = grunnlag?.inntekterINorgeGrunnlag?.filter { inntekt ->
            inntekt.inntektType?.uppercase() in enumValues<InntektTyper>().map { it.name }
        }

        val mottarSykepengerGrunnlag = sykepengerInntektGrunnlag?.map {
            MottarSykepengerGrunnlag(
                identifikator = it.identifikator,
                inntektType = it.inntektType,
                periode = Periode(it.periode.fom, it.periode.tom),
            )
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.A_INNTEKT),
            indikasjon = Indikasjon.I_NORGE,
            opplysning = "Mottar sykepenger",
            resultat = !sykepengerInntektGrunnlag.isNullOrEmpty(),
            mottarSykepengerGrunnlag = mottarSykepengerGrunnlag,
            vurdertPeriode = VurdertPeriode.INNEVÆRENDE_OG_FORRIGE_MND.beskrivelse
        )
    }

    private fun harArbeidInntektINorge(grunnlag: MedlemskapArbeidInntektGrunnlag?): TilhørighetVurdering {
        //val eksistererArbeidsforhold = grunnlag?.arbeiderINorgeGrunnlag?.any() ?: false
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
            opplysning = "Arbeid og inntekt i Norge",
            resultat = !arbeidInntektINorgeGrunnlag.isNullOrEmpty(),
            arbeidInntektINorgeGrunnlag = arbeidInntektINorgeGrunnlag,
            vurdertPeriode = VurdertPeriode.INNEVÆRENDE_OG_FORRIGE_MND.beskrivelse
        )
    }

    private fun harVedtakIMEDL(grunnlag: MedlemskapUnntakGrunnlag?): TilhørighetVurdering {
        val erMedlem = grunnlag?.unntak?.firstOrNull { it.verdi.medlem }
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
            opplysning = "Vedtak om pliktig eller frivillig medlemskap finnes i MEDL",
            resultat = erMedlem != null,
            vedtakImedlGrunnlag = medlGrunnlag,
            vurdertPeriode = VurdertPeriode.SØKNADSTIDSPUNKT.beskrivelse
        )
    }
}