package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektTyper
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.erGyldigIPeriode
import no.nav.aap.behandlingsflyt.lovvalgAutomatiskGjennomslipp
import no.nav.aap.behandlingsflyt.lovvalgBosattOgIngenAndreDel1
import no.nav.aap.behandlingsflyt.lovvalgBosattOgIngenAndreDel1IngenDel2
import no.nav.aap.behandlingsflyt.lovvalgBosattOgPotensielleAndreDel1
import no.nav.aap.behandlingsflyt.lovvalgBosattOgPotensielleAndreDel1IngenDel2
import no.nav.aap.behandlingsflyt.lovvalgÅrsakTilManuellVurderingIkkeOppfyltDel1
import no.nav.aap.behandlingsflyt.lovvalgÅrsakTilManuellVurderingOppfyltDel1
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import kotlin.enums.enumEntries

class MedlemskapLovvalgVurderingService {
    fun vurderTilhørighet(
        grunnlag: MedlemskapLovvalgGrunnlag,
        rettighetsPeriode: Periode,
        type: VurderingType? = null,
        unleashGateway: UnleashGateway
    ): KanBehandlesAutomatiskVurdering {
        val førsteDelVurderinger = vurderFørsteDelKriteier(grunnlag, rettighetsPeriode, unleashGateway)
        val andreDelVurdering = vurderAndreDelKriterier(grunnlag, rettighetsPeriode)

        val oppfyltMinstEttKrav = førsteDelVurderinger.any { it.resultat }
        val ingenInntruffet = andreDelVurdering.all { !it.resultat }
        val kanBehandlesAutomatisk = oppfyltMinstEttKrav && ingenInntruffet

        // No-op: Metrikker for videre utviklingsplan
        if (type == VurderingType.FØRSTEGANGSBEHANDLING) {
            // Målinger fra 12.08.26
            val bosatt = grunnlag.personopplysning?.status == PersonStatus.bosatt

            // Har bosatt-status
            prometheus.lovvalgBosattOgPotensielleAndreDel1(bosatt).increment()

            // Har bosatt-status og ingen oppfyllende del 1 kriterier
            prometheus.lovvalgBosattOgIngenAndreDel1(bosatt && !oppfyltMinstEttKrav).increment()

            // Har bosatt-status + potensielle andre del 1 + ingenInntruffet del 2 -> kanBehandlesAutomatisk
            prometheus.lovvalgBosattOgPotensielleAndreDel1IngenDel2(bosatt && ingenInntruffet).increment()

            // Har bosatt-status og ingen oppfyllende del 1 kriterier + ingenInntruffet del 2 -> kanBehandlesAutomatisk
            prometheus.lovvalgBosattOgIngenAndreDel1IngenDel2(bosatt && !oppfyltMinstEttKrav && ingenInntruffet)
                .increment()

            prometheus.lovvalgAutomatiskGjennomslipp(kanBehandlesAutomatisk).increment()

            if (!oppfyltMinstEttKrav) {
                prometheus.lovvalgÅrsakTilManuellVurderingIkkeOppfyltDel1("del1_ikke_oppfylt").increment()
                andreDelVurdering.filter { it.resultat }.forEach { vurdering ->
                    prometheus.lovvalgÅrsakTilManuellVurderingIkkeOppfyltDel1(lovvalgÅrsakNavn(vurdering.opplysning))
                        .increment()
                }
            } else {
                andreDelVurdering.filter { it.resultat }.forEach { vurdering ->
                    prometheus.lovvalgÅrsakTilManuellVurderingOppfyltDel1(lovvalgÅrsakNavn(vurdering.opplysning))
                        .increment()
                }
            }
        }

        return KanBehandlesAutomatiskVurdering(
            kanBehandlesAutomatisk,
            førsteDelVurderinger + andreDelVurdering
        )
    }

    private fun lovvalgÅrsakNavn(opplysning: String): String = when (opplysning) {
        "Arbeid i utland" -> "arbeid_i_utland"
        "Opphold i utland" -> "opphold_i_utland"
        "Utenlandsk adresse" -> "utenlandsk_adresse"
        "Vedtak om annet lovvalgsland finnes" -> "annet_lovvalgsland"
        "Mangler statsborgerskap i EØS" -> "mangler_statsborgerskap_eos"
        else -> opplysning.replace(" ", "_").lowercase()
    }

    // Minst én må oppfylles
    private fun vurderFørsteDelKriteier(
        grunnlag: MedlemskapLovvalgGrunnlag,
        rettighetsPeriode: Periode,
        unleashGateway: UnleashGateway,
    ): List<TilhørighetVurdering> {
        val mottarSykepengerVurdering = mottarSykepenger(grunnlag.medlemskapArbeidInntektGrunnlag)
        val arbeidInntektINorgeVurdering = harArbeidInntektINorge(grunnlag.medlemskapArbeidInntektGrunnlag)
        val vedtakIMedl = harVedtakIMEDL(grunnlag.medlemskapArbeidInntektGrunnlag?.medlemskapGrunnlag)

        return if (unleashGateway.isEnabled(BehandlingsflytFeature.BosattStatsborgerskapGjennomslipp)) {
            val bosattOgNorskStatsborger =
                harBosattStatusOgNorskStatsborgerskap(grunnlag.personopplysning, rettighetsPeriode)
            listOf(mottarSykepengerVurdering, arbeidInntektINorgeVurdering, vedtakIMedl, bosattOgNorskStatsborger)
        } else {
            listOf(mottarSykepengerVurdering, arbeidInntektINorgeVurdering, vedtakIMedl)
        }
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
                        relevantePerioder.map {
                            OppgittJobbetIUtlandGrunnlag(
                                land = it.land,
                                fraDato = it.fraDato,
                                tilDato = it.tilDato
                            )
                        }
                    arbeidUtlandPerioder.addAll(mappedArbeidUtland)
                    true
                } else {
                    false
                }
            }

            grunnlag.arbeidetUtenforNorgeFørSykdom -> {
                val mappedArbeidUtland =
                    relevantePerioder?.map {
                        OppgittJobbetIUtlandGrunnlag(
                            land = it.land,
                            fraDato = it.fraDato,
                            tilDato = it.tilDato
                        )
                    }
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
                fraDato = it.fraDato,
                tilDato = it.tilDato,
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

    private fun utenlandskAdresse(grunnlag: Personopplysning?, rettighetsPeriode: Periode): TilhørighetVurdering {
        val bosattUtenforNorge = grunnlag?.status != PersonStatus.bosatt && grunnlag?.status != PersonStatus.doed

        val adresser = grunnlag?.utenlandsAddresser?.filter { it.erGyldigIPeriode(rettighetsPeriode) }?.map {
            UtenlandskAdresseDto(
                gyldigFraOgMed = it.gyldigFraOgMed,
                gyldigTilOgMed = it.gyldigTilOgMed,
                adresseNavn = it.adresseNavn,
                postkode = it.postkode,
                bySted = it.bySted,
                landkode = it.landkode,
                adresseType = it.adresseType
            )
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.PDL),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Utenlandsk adresse",
            resultat = bosattUtenforNorge || !adresser.isNullOrEmpty(),
            utenlandsAddresserGrunnlag = UtenlandsAdresserGrunnlag(
                adresser,
                listOf(FolkeregisterStatusDto(grunnlag?.status, null, null))
            ),
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
        grunnlag: Personopplysning?,
        rettighetsPeriode: Periode
    ): TilhørighetVurdering {

        val manglerEØS =
            grunnlag?.statsborgerskap
                ?.none { it.land in EØSLandEllerLandMedAvtale.gyldigeEØSLand.map { it.name } }

        val manglerStatsborgerskapGrunnlag =
            grunnlag?.statsborgerskap?.filter { it.erGyldigIPeriode(rettighetsPeriode) }?.map {
                ManglerStatsborgerskapGrunnlag(
                    land = it.land,
                    gyldigFraOgMed = it.gyldigFraOgMed,
                    gyldigTilOgMed = it.gyldigTilOgMed
                )
            }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.PDL),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Mangler statsborgerskap i EØS",
            resultat = manglerEØS == true,
            manglerStatsborgerskapGrunnlag = manglerStatsborgerskapGrunnlag,
            vurdertPeriode = VurdertPeriode.SØKNADSTIDSPUNKT.beskrivelse
        )
    }

    private fun mottarSykepenger(grunnlag: MedlemskapArbeidInntektGrunnlag?): TilhørighetVurdering {
        val sykepengerInntektGrunnlag = grunnlag?.inntekterINorgeGrunnlag?.filter { inntekt ->
            inntekt.inntektType?.uppercase() in enumEntries<InntektTyper>().map { it.name } && inntekt.beloep != 0.0
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
        val arbeidInntektINorgeGrunnlag =
            grunnlag?.inntekterINorgeGrunnlag?.filter { it.beloep != 0.0 }?.map {
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

    private fun harBosattStatusOgNorskStatsborgerskap(
        grunnlag: Personopplysning?,
        rettighetsPeriode: Periode
    ): TilhørighetVurdering {
        val harNorskStatsborgerskap =
            grunnlag?.statsborgerskap
                ?.any { it.land == EØSLandEllerLandMedAvtale.NOR.toString() && it.erGyldigIPeriode(rettighetsPeriode) }

        val gyldigeStatsborgerskap =
            grunnlag?.statsborgerskap?.filter { it.erGyldigIPeriode(rettighetsPeriode) }?.map {
                GyldigStatsborgerskap(
                    land = it.land,
                    gyldigFraOgMed = it.gyldigFraOgMed,
                    gyldigTilOgMed = it.gyldigTilOgMed
                )
            } ?: emptyList()

        val bosattOgNorskStatsborgerskapGrunnlag =
            BosattOgNorskStatsborgerskapGrunnlag(grunnlag?.status, gyldigeStatsborgerskap)

        return TilhørighetVurdering(
            kilde = listOf(Kilde.PDL),
            indikasjon = Indikasjon.I_NORGE,
            opplysning = "Bosatt i Norge med norsk statsborgerskap",
            resultat = harNorskStatsborgerskap == true && grunnlag.status == PersonStatus.bosatt,
            bosattStatusOgNorskStatsborgerskap = bosattOgNorskStatsborgerskapGrunnlag,
            vurdertPeriode = VurdertPeriode.SØKNADSTIDSPUNKT.beskrivelse
        )
    }
}