package no.nav.aap.verdityper.dokument

public enum class Kanal {
    DIGITAL, PAPIR;

    public fun fraKanal(kanal: KanalFraKodeverk): Kanal {
        return when (kanal) {
            KanalFraKodeverk.SKAN_NETS -> PAPIR
            KanalFraKodeverk.SKAN_PEN -> PAPIR
            KanalFraKodeverk.SKAN_IM -> PAPIR
            else -> DIGITAL
        }
    }
}

/**
 * Kopiert fra https://github.com/navikt/saf/blob/master/app/src/main/resources/schemas/saf.graphqls#L675
 * Ikke til bruk internt i koden, kun for oversettelse til [Kanal].
 */
public enum class KanalFraKodeverk {
    /**
     * Forsendelsen er sendt inn via et Altinn-skjema.
     * Brukes for inngående journalposter.
     */
    ALTINN,

    /**
     * Forsendelsen er mottatt eller distribuert via applikasjoner som EU har levert og som benyttes for utveksling av informasjon med andre EU-land.
     * Brukes for inngående og utgående journalposter.
     */
    EESSI,

    /**
     * Forsendelsen er arkivert av applikasjonen EIA.
     * Brukes for inngående journalposter.
     */
    EIA,

    /**
     * Dokumentene i journalposten er hentet fra en ekstern kilde, for eksempel informasjon om oppholdstillatelse fra Utlendingsdirektoratet.
     * Brukes for inngående journalposter.
     */
    EKST_OPPS,

    /**
     * Brevet er skrevet ut lokalt og kan være sendt i posten på papir.
     * Brukes for utgående journalposter og notater.
     */
    LOKAL_UTSKRIFT,

    /**
     * Forsendelsen er sendt inn digitalt via selvbetjeningsløsninger på nav.no, eller distribuert digitalt til brukers meldingsboks på nav.no.
     * Brukes for inngående og utgående journalposter.
     */
    NAV_NO,

    /**
     * Brevet er overført til sentral distribusjon og sendt i posten på papir.
     * Brukes for utgående journalposter.
     */
    SENTRAL_UTSKRIFT,

    /**
     * Brevet er sendt via digital post til innbyggere.
     * Brukes for utgående journalposter.
     */
    SDP,

    /**
     * Forsendelsen er sendt inn på papir og skannet hos NETS.
     * Brukes for inngående, utgående journalposter og notater.
     */
    SKAN_NETS,

    /**
     * Forsendelsen er sendt inn på papir og skannet på NAVs skanningsenter for pensjon og bidrag.
     * Brukes for inngående journalposter.
     */
    SKAN_PEN,

    /**
     * Forsendelsen er sendt inn på papir og skannet hos Iron Mountain.
     * Brukes for inngående, utgående journalposter og notater.
     */
    SKAN_IM,

    /**
     * Forsendelsen er distribuert via integrasjonspunkt for eFormidling til Trygderetten.
     * Brukes for utgående journalposter.
     */
    TRYGDERETTEN,

    /**
     * Forsendelsen er mottatt eller distribuert via Norsk Helsenett, helsesektorens løsning for elektronisk meldingsutveksling.
     * Brukes for inngående og utgående journalposter.
     */
    HELSENETTET,

    /**
     * Forsendelsen skal ikke distribueres ut av NAV.
     * Brukes for alle notater og noen utgående journalposter.
     */
    INGEN_DISTRIBUSJON,

    /**
     * Forsendelsen er sendt inn digitalt via selvbetjeningsløsninger på nav.no, uten at avsenderen ble digitalt autentisert.
     * Brukes for inngående journalposter.
     */
    NAV_NO_UINNLOGGET,

    /**
     * Bruker har fylt ut og sendt inn dokumentet sammen med en NAV-ansatt. Det er den NAV-ansatte som var pålogget innsendingsløsningen.
     */
    INNSENDT_NAV_ANSATT,

    /**
     * Forsendelsen inneholder en komplett chatdialog (inngående og utgående meldinger) mellom en bruker og en veileder i NAV.
     */
    NAV_NO_CHAT,

    /**
     * Brevet er sendt til virksomhet som Taushetsbelagt Post via Altinn.
     * Brukes for utgående journalposter.
     */
    DPVT,

    /**
     * Forsendelsen er mottatt på e-post.
     * Brukes for inngående journalposter.
     */
    E_POST,

    /**
     * Forsendelsen er mottatt i en av NAVs meldingsbokser i Altinn.
     * Brukes for inngående journalposter.
     */
    ALTINN_INNBOKS,

    /**
     * Forsendelsen har ingen kjent kanal.
     */
    UKJENT
}