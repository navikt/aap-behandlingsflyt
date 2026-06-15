--
-- PostgreSQL database dump
--

-- Dumped from database version 16.13
-- Dumped by pg_dump version 17.5

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: btree_gist; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;


--
-- Name: EXTENSION btree_gist; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION btree_gist IS 'support for indexing common datatypes in GiST';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: vurderingsbehov; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vurderingsbehov (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    aarsak character varying(150) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    behandling_aarsak_id bigint,
    oppdatert_tid timestamp(3) without time zone
);


--
-- Name: aarsak_til_behandling_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.aarsak_til_behandling_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: aarsak_til_behandling_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.aarsak_til_behandling_id_seq OWNED BY public.vurderingsbehov.id;


--
-- Name: aktivitetsplikt_11_7_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aktivitetsplikt_11_7_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurderinger_id bigint
);


--
-- Name: aktivitetsplikt_11_7_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.aktivitetsplikt_11_7_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: aktivitetsplikt_11_7_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.aktivitetsplikt_11_7_grunnlag_id_seq OWNED BY public.aktivitetsplikt_11_7_grunnlag.id;


--
-- Name: aktivitetsplikt_11_7_varsel; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aktivitetsplikt_11_7_varsel (
    id bigint NOT NULL,
    dato_varslet date,
    frist date,
    brev_referanse uuid NOT NULL,
    behandling_id bigint,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: aktivitetsplikt_11_7_varsel_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.aktivitetsplikt_11_7_varsel_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: aktivitetsplikt_11_7_varsel_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.aktivitetsplikt_11_7_varsel_id_seq OWNED BY public.aktivitetsplikt_11_7_varsel.id;


--
-- Name: aktivitetsplikt_11_7_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aktivitetsplikt_11_7_vurdering (
    id integer NOT NULL,
    begrunnelse text NOT NULL,
    er_oppfylt boolean NOT NULL,
    utfall character varying(20),
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av character varying(50) NOT NULL,
    vurderingen_gjelder_fra date,
    vurderinger_id bigint,
    vurdert_i_behandling bigint,
    skal_ignorere_varsel_frist boolean DEFAULT false NOT NULL,
    tom date
);


--
-- Name: aktivitetsplikt_11_7_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.aktivitetsplikt_11_7_vurdering_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: aktivitetsplikt_11_7_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.aktivitetsplikt_11_7_vurdering_id_seq OWNED BY public.aktivitetsplikt_11_7_vurdering.id;


--
-- Name: aktivitetsplikt_11_7_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aktivitetsplikt_11_7_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: aktivitetsplikt_11_7_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.aktivitetsplikt_11_7_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: aktivitetsplikt_11_7_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.aktivitetsplikt_11_7_vurderinger_id_seq OWNED BY public.aktivitetsplikt_11_7_vurderinger.id;


--
-- Name: aktivitetsplikt_11_9_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aktivitetsplikt_11_9_grunnlag (
    id bigint NOT NULL,
    vurderinger_id bigint,
    behandling_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: aktivitetsplikt_11_9_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.aktivitetsplikt_11_9_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: aktivitetsplikt_11_9_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.aktivitetsplikt_11_9_grunnlag_id_seq OWNED BY public.aktivitetsplikt_11_9_grunnlag.id;


--
-- Name: aktivitetsplikt_11_9_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aktivitetsplikt_11_9_vurdering (
    id bigint NOT NULL,
    dato date NOT NULL,
    begrunnelse text NOT NULL,
    brudd character varying(50) NOT NULL,
    grunn character varying(50) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurderinger_id bigint NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    vurdert_av character varying(50) NOT NULL
);


--
-- Name: aktivitetsplikt_11_9_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.aktivitetsplikt_11_9_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: aktivitetsplikt_11_9_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.aktivitetsplikt_11_9_vurdering_id_seq OWNED BY public.aktivitetsplikt_11_9_vurdering.id;


--
-- Name: aktivitetsplikt_11_9_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aktivitetsplikt_11_9_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: aktivitetsplikt_11_9_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.aktivitetsplikt_11_9_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: aktivitetsplikt_11_9_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.aktivitetsplikt_11_9_vurderinger_id_seq OWNED BY public.aktivitetsplikt_11_9_vurderinger.id;


--
-- Name: andre_ytelser_oppgitt_i_soknad_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.andre_ytelser_oppgitt_i_soknad_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    andre_ytelser_id bigint,
    aktiv boolean DEFAULT true NOT NULL
);


--
-- Name: andre_ytelser_oppgitt_i_soknad_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.andre_ytelser_oppgitt_i_soknad_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: andre_ytelser_oppgitt_i_soknad_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.andre_ytelser_oppgitt_i_soknad_grunnlag_id_seq OWNED BY public.andre_ytelser_oppgitt_i_soknad_grunnlag.id;


--
-- Name: andre_ytelser_svar_i_soknad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.andre_ytelser_svar_i_soknad (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ekstralonn boolean NOT NULL,
    afpkilder text
);


--
-- Name: andre_ytelser_svar_i_soknad_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.andre_ytelser_svar_i_soknad_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: andre_ytelser_svar_i_soknad_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.andre_ytelser_svar_i_soknad_id_seq OWNED BY public.andre_ytelser_svar_i_soknad.id;


--
-- Name: annen_ytelse_oppgitt_i_soknad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.annen_ytelse_oppgitt_i_soknad (
    id bigint NOT NULL,
    andre_ytelser_id bigint NOT NULL,
    ytelse text NOT NULL
);


--
-- Name: annen_ytelse_oppgitt_i_soknad_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.annen_ytelse_oppgitt_i_soknad_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: annen_ytelse_oppgitt_i_soknad_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.annen_ytelse_oppgitt_i_soknad_id_seq OWNED BY public.annen_ytelse_oppgitt_i_soknad.id;


--
-- Name: arbeid; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeid (
    id bigint NOT NULL,
    identifikator character varying(30) NOT NULL,
    arbeidsforhold_kode character varying(50) NOT NULL,
    arbeider_id bigint NOT NULL,
    startdato date NOT NULL,
    sluttdato date,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: arbeid_detaljer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeid_detaljer (
    id bigint NOT NULL,
    arbeid_forutgaaende_id bigint NOT NULL,
    skipsregister_kode text,
    skipstype_kode text,
    fartsomraade_kode text,
    yrke_kode text,
    yrke_beskrivelse text,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: arbeid_detaljer_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arbeid_detaljer_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arbeid_detaljer_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.arbeid_detaljer_id_seq OWNED BY public.arbeid_detaljer.id;


--
-- Name: arbeid_forutgaaende; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeid_forutgaaende (
    id bigint NOT NULL,
    identifikator character varying(30) NOT NULL,
    arbeidsforhold_kode character varying(50) NOT NULL,
    arbeider_id bigint NOT NULL,
    startdato date NOT NULL,
    sluttdato date,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    organisasjonsnavn text
);


--
-- Name: arbeid_forutgaaende_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arbeid_forutgaaende_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arbeid_forutgaaende_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.arbeid_forutgaaende_id_seq OWNED BY public.arbeid_forutgaaende.id;


--
-- Name: arbeid_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arbeid_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arbeid_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.arbeid_id_seq OWNED BY public.arbeid.id;


--
-- Name: arbeider; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeider (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: arbeider_forutgaaende; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeider_forutgaaende (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: arbeider_forutgaaende_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arbeider_forutgaaende_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arbeider_forutgaaende_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.arbeider_forutgaaende_id_seq OWNED BY public.arbeider_forutgaaende.id;


--
-- Name: arbeider_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arbeider_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arbeider_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.arbeider_id_seq OWNED BY public.arbeider.id;


--
-- Name: arbeidsevne; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeidsevne (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: arbeidsevne_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeidsevne_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    arbeidsevne_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: arbeidsevne_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arbeidsevne_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arbeidsevne_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.arbeidsevne_grunnlag_id_seq OWNED BY public.arbeidsevne_grunnlag.id;


--
-- Name: arbeidsevne_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arbeidsevne_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arbeidsevne_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.arbeidsevne_id_seq OWNED BY public.arbeidsevne.id;


--
-- Name: arbeidsevne_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeidsevne_vurdering (
    id bigint NOT NULL,
    arbeidsevne_id bigint NOT NULL,
    fra_dato date NOT NULL,
    begrunnelse text NOT NULL,
    andel_arbeidsevne smallint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av text NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    til_dato date
);


--
-- Name: arbeidsevne_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arbeidsevne_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arbeidsevne_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.arbeidsevne_vurdering_id_seq OWNED BY public.arbeidsevne_vurdering.id;


--
-- Name: arbeidsopptrapping_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeidsopptrapping_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurderinger_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: arbeidsopptrapping_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arbeidsopptrapping_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arbeidsopptrapping_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.arbeidsopptrapping_grunnlag_id_seq OWNED BY public.arbeidsopptrapping_grunnlag.id;


--
-- Name: arbeidsopptrapping_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeidsopptrapping_vurdering (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    mulighet_til_opptrapping boolean NOT NULL,
    rett_paa_aap boolean NOT NULL,
    vurderinger_id bigint NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    vurdert_av character varying(50) NOT NULL,
    gjelder_fra date NOT NULL,
    gjelder_til date,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: arbeidsopptrapping_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arbeidsopptrapping_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arbeidsopptrapping_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.arbeidsopptrapping_vurdering_id_seq OWNED BY public.arbeidsopptrapping_vurdering.id;


--
-- Name: arbeidsopptrapping_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeidsopptrapping_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: arbeidsopptrapping_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arbeidsopptrapping_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arbeidsopptrapping_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.arbeidsopptrapping_vurderinger_id_seq OWNED BY public.arbeidsopptrapping_vurderinger.id;


--
-- Name: avbryt_aktivitetspliktbehandling_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.avbryt_aktivitetspliktbehandling_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: avbryt_aktivitetspliktbehandling_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.avbryt_aktivitetspliktbehandling_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: avbryt_aktivitetspliktbehandling_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.avbryt_aktivitetspliktbehandling_grunnlag_id_seq OWNED BY public.avbryt_aktivitetspliktbehandling_grunnlag.id;


--
-- Name: avbryt_aktivitetspliktbehandling_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.avbryt_aktivitetspliktbehandling_vurdering (
    id bigint NOT NULL,
    aarsak text NOT NULL,
    begrunnelse text NOT NULL,
    vurdert_av text NOT NULL,
    opprettet_tid timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: avbryt_aktivitetspliktbehandling_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.avbryt_aktivitetspliktbehandling_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: avbryt_aktivitetspliktbehandling_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.avbryt_aktivitetspliktbehandling_vurdering_id_seq OWNED BY public.avbryt_aktivitetspliktbehandling_vurdering.id;


--
-- Name: avbryt_revurdering_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.avbryt_revurdering_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL
);


--
-- Name: avbryt_revurdering_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.avbryt_revurdering_vurdering (
    id bigint NOT NULL,
    aarsak text,
    begrunnelse text NOT NULL,
    vurdert_av text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL
);


--
-- Name: avklaringsbehov; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.avklaringsbehov (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    definisjon character varying(50) NOT NULL,
    funnet_i_steg character varying(50) NOT NULL,
    krever_to_trinn boolean,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: avklaringsbehov_endring; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.avklaringsbehov_endring (
    id bigint NOT NULL,
    avklaringsbehov_id bigint NOT NULL,
    status character varying(50) NOT NULL,
    begrunnelse text,
    venteaarsak character varying(50),
    frist date,
    opprettet_av character varying(100) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    perioder_ugyldig_vurdering daterange[],
    perioder_krever_vurdering daterange[]
);


--
-- Name: avklaringsbehov_endring_aarsak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.avklaringsbehov_endring_aarsak (
    id bigint NOT NULL,
    endring_id bigint NOT NULL,
    aarsak_til_retur character varying(50) NOT NULL,
    aarsak_til_retur_fritekst text,
    opprettet_av character varying(100) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: avklaringsbehov_endring_aarsak_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.avklaringsbehov_endring_aarsak_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: avklaringsbehov_endring_aarsak_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.avklaringsbehov_endring_aarsak_id_seq OWNED BY public.avklaringsbehov_endring_aarsak.id;


--
-- Name: avklaringsbehov_endring_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.avklaringsbehov_endring_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: avklaringsbehov_endring_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.avklaringsbehov_endring_id_seq OWNED BY public.avklaringsbehov_endring.id;


--
-- Name: avklaringsbehov_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.avklaringsbehov_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: avklaringsbehov_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.avklaringsbehov_id_seq OWNED BY public.avklaringsbehov.id;


--
-- Name: avvist_formkrav_varsel; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.avvist_formkrav_varsel (
    id bigint NOT NULL,
    dato_varslet date,
    frist date,
    brev_referanse uuid NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    behandling_id bigint NOT NULL
);


--
-- Name: avvist_formkrav_varsel_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.avvist_formkrav_varsel_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: avvist_formkrav_varsel_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.avvist_formkrav_varsel_id_seq OWNED BY public.avvist_formkrav_varsel.id;


--
-- Name: barn_saksbehandler_oppgitt; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.barn_saksbehandler_oppgitt (
    id bigint NOT NULL,
    ident text,
    navn text NOT NULL,
    fodselsdato date NOT NULL,
    relasjon text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    saksbehandler_oppgitt_barn_id bigint NOT NULL
);


--
-- Name: barn_saksbehandler_oppgitt_barnopplysning; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.barn_saksbehandler_oppgitt_barnopplysning (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: barn_saksbehandler_oppgitt_barnopplysning_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.barn_saksbehandler_oppgitt_barnopplysning_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: barn_saksbehandler_oppgitt_barnopplysning_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.barn_saksbehandler_oppgitt_barnopplysning_id_seq OWNED BY public.barn_saksbehandler_oppgitt_barnopplysning.id;


--
-- Name: barn_saksbehandler_oppgitt_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.barn_saksbehandler_oppgitt_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: barn_saksbehandler_oppgitt_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.barn_saksbehandler_oppgitt_id_seq OWNED BY public.barn_saksbehandler_oppgitt.id;


--
-- Name: barn_tillegg; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.barn_tillegg (
    id bigint NOT NULL,
    ident character varying(19),
    barnetillegg_periode_id bigint NOT NULL,
    navn text,
    fodselsdato date
);


--
-- Name: barn_tillegg_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.barn_tillegg_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: barn_tillegg_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.barn_tillegg_id_seq OWNED BY public.barn_tillegg.id;


--
-- Name: barn_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.barn_vurdering (
    id bigint NOT NULL,
    ident character varying(19),
    barn_vurderinger_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    navn text,
    fodselsdato date
);


--
-- Name: barn_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.barn_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: barn_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.barn_vurdering_id_seq OWNED BY public.barn_vurdering.id;


--
-- Name: barn_vurdering_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.barn_vurdering_periode (
    id bigint NOT NULL,
    barn_vurdering_id bigint NOT NULL,
    periode daterange NOT NULL,
    begrunnelse text NOT NULL,
    har_foreldreansvar boolean NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    er_fosterforelder boolean
);


--
-- Name: barn_vurdering_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.barn_vurdering_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: barn_vurdering_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.barn_vurdering_periode_id_seq OWNED BY public.barn_vurdering_periode.id;


--
-- Name: barn_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.barn_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av text NOT NULL
);


--
-- Name: barn_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.barn_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: barn_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.barn_vurderinger_id_seq OWNED BY public.barn_vurderinger.id;


--
-- Name: barnetillegg_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.barnetillegg_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    perioder_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: barnetillegg_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.barnetillegg_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: barnetillegg_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.barnetillegg_grunnlag_id_seq OWNED BY public.barnetillegg_grunnlag.id;


--
-- Name: barnetillegg_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.barnetillegg_periode (
    id bigint NOT NULL,
    perioder_id bigint NOT NULL,
    periode daterange NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: barnetillegg_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.barnetillegg_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: barnetillegg_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.barnetillegg_periode_id_seq OWNED BY public.barnetillegg_periode.id;


--
-- Name: barnetillegg_perioder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.barnetillegg_perioder (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: barnetillegg_perioder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.barnetillegg_perioder_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: barnetillegg_perioder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.barnetillegg_perioder_id_seq OWNED BY public.barnetillegg_perioder.id;


--
-- Name: barnopplysning; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.barnopplysning (
    id bigint NOT NULL,
    ident character varying(11),
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    bgb_id bigint NOT NULL,
    fodselsdato date,
    dodsdato date,
    person_id integer,
    navn text
);


--
-- Name: barnopplysning_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.barnopplysning_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    register_barn_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    oppgitt_barn_id bigint,
    vurderte_barn_id bigint,
    saksbehandler_oppgitt_barn_id bigint
);


--
-- Name: barnopplysning_grunnlag_barnopplysning; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.barnopplysning_grunnlag_barnopplysning (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: barnopplysning_grunnlag_barnopplysning_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.barnopplysning_grunnlag_barnopplysning_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: barnopplysning_grunnlag_barnopplysning_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.barnopplysning_grunnlag_barnopplysning_id_seq OWNED BY public.barnopplysning_grunnlag_barnopplysning.id;


--
-- Name: barnopplysning_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.barnopplysning_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: barnopplysning_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.barnopplysning_grunnlag_id_seq OWNED BY public.barnopplysning_grunnlag.id;


--
-- Name: barnopplysning_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.barnopplysning_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: barnopplysning_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.barnopplysning_id_seq OWNED BY public.barnopplysning.id;


--
-- Name: behandlende_enhet_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.behandlende_enhet_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: behandlende_enhet_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.behandlende_enhet_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: behandlende_enhet_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.behandlende_enhet_grunnlag_id_seq OWNED BY public.behandlende_enhet_grunnlag.id;


--
-- Name: behandlende_enhet_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.behandlende_enhet_vurdering (
    id integer NOT NULL,
    skal_behandles_av_nay boolean NOT NULL,
    skal_behandles_av_kontor boolean NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av character varying(50) NOT NULL
);


--
-- Name: behandlende_enhet_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.behandlende_enhet_vurdering_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: behandlende_enhet_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.behandlende_enhet_vurdering_id_seq OWNED BY public.behandlende_enhet_vurdering.id;


--
-- Name: behandling; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.behandling (
    id bigint NOT NULL,
    sak_id bigint NOT NULL,
    referanse uuid NOT NULL,
    status character varying(100) NOT NULL,
    type character varying(100) NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    forrige_id bigint,
    aarsak_til_opprettelse character varying(50)
);


--
-- Name: behandling_aarsak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.behandling_aarsak (
    id bigint NOT NULL,
    behandling_id bigint,
    aarsak text NOT NULL,
    begrunnelse text,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: behandling_aarsak_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.behandling_aarsak_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: behandling_aarsak_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.behandling_aarsak_id_seq OWNED BY public.behandling_aarsak.id;


--
-- Name: behandling_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.behandling_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: behandling_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.behandling_id_seq OWNED BY public.behandling.id;


--
-- Name: beregning; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beregning (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    beregningstype character varying(50) NOT NULL
);


--
-- Name: beregning_hoved; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beregning_hoved (
    id bigint NOT NULL,
    beregning_id bigint NOT NULL,
    grunnlag numeric(21,10) NOT NULL,
    er_gjennomsnitt boolean NOT NULL,
    gjennomsnittlig_inntekt_i_g numeric(21,10) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: beregning_hoved_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.beregning_hoved_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: beregning_hoved_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.beregning_hoved_id_seq OWNED BY public.beregning_hoved.id;


--
-- Name: beregning_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.beregning_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: beregning_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.beregning_id_seq OWNED BY public.beregning.id;


--
-- Name: beregning_inntekt; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beregning_inntekt (
    id bigint NOT NULL,
    beregning_hoved_id bigint NOT NULL,
    arstall integer NOT NULL,
    inntekt_i_kroner numeric(19,2) NOT NULL,
    grunnbelop numeric(19,2) NOT NULL,
    inntekt_i_g numeric(21,10) NOT NULL,
    inntekt_6g_begrenset numeric(21,10) NOT NULL,
    er_6g_begrenset boolean NOT NULL
);


--
-- Name: beregning_inntekt_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.beregning_inntekt_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: beregning_inntekt_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.beregning_inntekt_id_seq OWNED BY public.beregning_inntekt.id;


--
-- Name: beregning_ufore; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beregning_ufore (
    id bigint NOT NULL,
    beregning_id bigint NOT NULL,
    beregning_hoved_id bigint NOT NULL,
    beregning_hoved_ytterligere_id bigint NOT NULL,
    type text NOT NULL,
    g_unit numeric(21,10) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    uforegrad smallint NOT NULL,
    ufore_ytterligere_nedsatt_arbeidsevne_ar smallint NOT NULL
);


--
-- Name: beregning_ufore_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.beregning_ufore_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: beregning_ufore_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.beregning_ufore_id_seq OWNED BY public.beregning_ufore.id;


--
-- Name: beregning_ufore_inntekt; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beregning_ufore_inntekt (
    id bigint NOT NULL,
    beregning_ufore_id bigint NOT NULL,
    arstall integer NOT NULL,
    inntekt_i_kroner numeric(19,2) NOT NULL,
    uforegrad smallint NOT NULL,
    arbeidsgrad smallint NOT NULL,
    inntekt_justert_for_uforegrad numeric(19,2) NOT NULL,
    inntekt_justert_ufore_g numeric(21,10) NOT NULL,
    grunnbelop numeric(19,2) NOT NULL,
    inntekt_i_g numeric(21,10) NOT NULL
);


--
-- Name: beregning_ufore_inntekt_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.beregning_ufore_inntekt_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: beregning_ufore_inntekt_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.beregning_ufore_inntekt_id_seq OWNED BY public.beregning_ufore_inntekt.id;


--
-- Name: beregning_ufore_tidsperiode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beregning_ufore_tidsperiode (
    id bigint NOT NULL,
    beregning_ufore_inntekt_id bigint,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    periode daterange,
    inntekt_i_kroner numeric(19,2),
    inntekt_justert_for_uforegrad numeric(19,2),
    uforegrad smallint
);


--
-- Name: beregning_ufore_tidsperiode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.beregning_ufore_tidsperiode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: beregning_ufore_tidsperiode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.beregning_ufore_tidsperiode_id_seq OWNED BY public.beregning_ufore_tidsperiode.id;


--
-- Name: beregning_ufore_uforegrader; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beregning_ufore_uforegrader (
    id bigint NOT NULL,
    beregning_ufore_id bigint,
    uforegrad smallint,
    virkningstidspunkt date,
    uforegrad_tom date,
    uforegrad_fom date
);


--
-- Name: beregning_ufore_uforegrader_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.beregning_ufore_uforegrader_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: beregning_ufore_uforegrader_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.beregning_ufore_uforegrader_id_seq OWNED BY public.beregning_ufore_uforegrader.id;


--
-- Name: beregning_yrkesskade; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beregning_yrkesskade (
    id bigint NOT NULL,
    beregning_id bigint NOT NULL,
    g_unit numeric(21,10) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    terskelverdi_for_yrkesskade smallint NOT NULL,
    andel_yrkesskade smallint NOT NULL,
    benyttet_andel_yrkesskade smallint NOT NULL,
    yrkesskade_tidspunkt smallint NOT NULL,
    grunnbelop numeric(19,2) NOT NULL,
    yrkesskade_inntekt_i_g numeric(21,10) NOT NULL,
    antatt_arlig_inntekt_yrkesskade_tidspunkt numeric(19,2) NOT NULL,
    andel_som_skyldes_yrkesskade numeric(21,10) NOT NULL,
    andel_som_ikke_skyldes_yrkesskade numeric(21,10) NOT NULL,
    grunnlag_etter_yrkesskade_fordel numeric(21,10) NOT NULL,
    grunnlag_for_beregning_av_yrkesskadeandel numeric(21,10) NOT NULL
);


--
-- Name: beregning_yrkesskade_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.beregning_yrkesskade_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: beregning_yrkesskade_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.beregning_yrkesskade_id_seq OWNED BY public.beregning_yrkesskade.id;


--
-- Name: beregningsfakta_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beregningsfakta_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    tidspunkt_vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    yrkesskade_vurdering_id bigint
);


--
-- Name: beregningsgrunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beregningsgrunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    beregning_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: beregningsgrunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.beregningsgrunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: beregningsgrunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.beregningsgrunnlag_id_seq OWNED BY public.beregningsgrunnlag.id;


--
-- Name: beregningstidspunkt_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.beregningstidspunkt_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: beregningstidspunkt_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.beregningstidspunkt_grunnlag_id_seq OWNED BY public.beregningsfakta_grunnlag.id;


--
-- Name: beregningstidspunkt_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beregningstidspunkt_vurdering (
    id bigint NOT NULL,
    nedsatt_begrunnelse text,
    ytterligere_nedsatt_arbeidsevne_dato date,
    nedsatt_arbeidsevne_dato date NOT NULL,
    ytterligere_nedsatt_begrunnelse text,
    vurdert_av text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: beregningstidspunkt_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.beregningstidspunkt_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: beregningstidspunkt_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.beregningstidspunkt_vurdering_id_seq OWNED BY public.beregningstidspunkt_vurdering.id;


--
-- Name: bistand; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bistand (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    behov_for_aktiv_behandling boolean NOT NULL,
    behov_for_arbeidsrettet_tiltak boolean NOT NULL,
    behov_for_annen_oppfoelging boolean,
    vurderingen_gjelder_fra date NOT NULL,
    vurdert_av character varying(50) NOT NULL,
    overgang_til_arbeid boolean,
    overgang_begrunnelse text,
    bistand_vurderinger_id bigint NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    tom date
);


--
-- Name: bistand_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bistand_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    bistand_vurderinger_id bigint NOT NULL
);


--
-- Name: bistand_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bistand_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bistand_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bistand_grunnlag_id_seq OWNED BY public.bistand_grunnlag.id;


--
-- Name: bistand_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bistand_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bistand_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bistand_id_seq OWNED BY public.bistand.id;


--
-- Name: bistand_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bistand_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: bistand_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bistand_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bistand_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bistand_vurderinger_id_seq OWNED BY public.bistand_vurderinger.id;


--
-- Name: brevbestilling; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brevbestilling (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    type_brev character varying(100) NOT NULL,
    referanse uuid NOT NULL,
    status character varying(100) NOT NULL,
    opprettet timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: brevbestilling_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brevbestilling_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brevbestilling_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brevbestilling_id_seq OWNED BY public.brevbestilling.id;


--
-- Name: brudd_aktivitetsplikt; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brudd_aktivitetsplikt (
    id bigint NOT NULL,
    sak_id bigint NOT NULL,
    nav_ident text NOT NULL,
    brudd character varying(60) NOT NULL,
    periode daterange NOT NULL,
    begrunnelse text NOT NULL,
    paragraf character varying(20) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    hendelse_id uuid NOT NULL,
    innsending_id uuid NOT NULL,
    dokument_type text NOT NULL,
    grunn text
);


--
-- Name: brudd_aktivitetsplikt_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brudd_aktivitetsplikt_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: brudd_aktivitetsplikt_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brudd_aktivitetsplikt_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brudd_aktivitetsplikt_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brudd_aktivitetsplikt_grunnlag_id_seq OWNED BY public.brudd_aktivitetsplikt_grunnlag.id;


--
-- Name: brudd_aktivitetsplikt_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brudd_aktivitetsplikt_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brudd_aktivitetsplikt_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brudd_aktivitetsplikt_id_seq OWNED BY public.brudd_aktivitetsplikt.id;


--
-- Name: brudd_aktivitetsplikter; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brudd_aktivitetsplikter (
    id bigint NOT NULL,
    brudd_aktivitetsplikt_grunnlag_id bigint NOT NULL,
    brudd_aktivitetsplikt_id bigint NOT NULL
);


--
-- Name: brudd_aktivitetsplikter_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brudd_aktivitetsplikter_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brudd_aktivitetsplikter_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brudd_aktivitetsplikter_id_seq OWNED BY public.brudd_aktivitetsplikter.id;


--
-- Name: bruker_land; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_land (
    id bigint NOT NULL,
    land character varying(50) DEFAULT 'XUK'::character varying NOT NULL,
    gyldigfraogmed date,
    gyldigtilogmed date,
    landkoder_id bigint,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: bruker_land_aggregat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_land_aggregat (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: bruker_land_aggregat_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bruker_land_aggregat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bruker_land_aggregat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bruker_land_aggregat_id_seq OWNED BY public.bruker_land_aggregat.id;


--
-- Name: bruker_land_forutgaaende; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_land_forutgaaende (
    id bigint NOT NULL,
    land character varying(50) DEFAULT 'XUK'::character varying NOT NULL,
    gyldigfraogmed date,
    gyldigtilogmed date,
    landkoder_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: bruker_land_forutgaaende_aggregat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_land_forutgaaende_aggregat (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: bruker_land_forutgaaende_aggregat_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bruker_land_forutgaaende_aggregat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bruker_land_forutgaaende_aggregat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bruker_land_forutgaaende_aggregat_id_seq OWNED BY public.bruker_land_forutgaaende_aggregat.id;


--
-- Name: bruker_land_forutgaaende_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bruker_land_forutgaaende_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bruker_land_forutgaaende_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bruker_land_forutgaaende_id_seq OWNED BY public.bruker_land_forutgaaende.id;


--
-- Name: bruker_land_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bruker_land_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bruker_land_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bruker_land_id_seq OWNED BY public.bruker_land.id;


--
-- Name: bruker_personopplysning; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_personopplysning (
    id bigint NOT NULL,
    fodselsdato date NOT NULL,
    dodsdato date,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    land character varying(50) DEFAULT 'XUK'::character varying NOT NULL,
    gyldigfraogmed date,
    gyldigtilogmed date,
    status character varying(50) DEFAULT 'inaktiv'::character varying NOT NULL,
    landkoder_id bigint,
    utenlandsadresser_id bigint
);


--
-- Name: bruker_personopplysning_forutgaaende; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_personopplysning_forutgaaende (
    id bigint NOT NULL,
    fodselsdato date NOT NULL,
    dodsdato date,
    landkoder_id bigint NOT NULL,
    statuser_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    utenlandsadresser_id bigint
);


--
-- Name: bruker_personopplysning_forutgaaende_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bruker_personopplysning_forutgaaende_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bruker_personopplysning_forutgaaende_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bruker_personopplysning_forutgaaende_id_seq OWNED BY public.bruker_personopplysning_forutgaaende.id;


--
-- Name: bruker_statuser_forutgaaende; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_statuser_forutgaaende (
    id bigint NOT NULL,
    status character varying(50) DEFAULT 'inaktiv'::character varying NOT NULL,
    statuser_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gyldighetstidspunkt date,
    opphoerstidspunkt date
);


--
-- Name: bruker_statuser_forutgaaende_aggregat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_statuser_forutgaaende_aggregat (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: bruker_statuser_forutgaaende_aggregat_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bruker_statuser_forutgaaende_aggregat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bruker_statuser_forutgaaende_aggregat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bruker_statuser_forutgaaende_aggregat_id_seq OWNED BY public.bruker_statuser_forutgaaende_aggregat.id;


--
-- Name: bruker_statuser_forutgaaende_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bruker_statuser_forutgaaende_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bruker_statuser_forutgaaende_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bruker_statuser_forutgaaende_id_seq OWNED BY public.bruker_statuser_forutgaaende.id;


--
-- Name: bruker_utenlandsadresse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_utenlandsadresse (
    id bigint NOT NULL,
    adressenavn character varying(150),
    postkode character varying(25),
    bysted character varying(50),
    landkode character varying(10),
    gyldigfraogmed date,
    gyldigtilogmed date,
    utenlandsadresser_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    adresse_type character varying(25)
);


--
-- Name: bruker_utenlandsadresse_forutgaaende; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_utenlandsadresse_forutgaaende (
    id bigint NOT NULL,
    adressenavn character varying(150),
    postkode character varying(25),
    bysted character varying(50),
    landkode character varying(10),
    gyldigfraogmed date,
    gyldigtilogmed date,
    utenlandsadresser_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    adresse_type character varying(25)
);


--
-- Name: bruker_utenlandsadresse_forutgaaende_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bruker_utenlandsadresse_forutgaaende_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bruker_utenlandsadresse_forutgaaende_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bruker_utenlandsadresse_forutgaaende_id_seq OWNED BY public.bruker_utenlandsadresse_forutgaaende.id;


--
-- Name: bruker_utenlandsadresse_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bruker_utenlandsadresse_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bruker_utenlandsadresse_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bruker_utenlandsadresse_id_seq OWNED BY public.bruker_utenlandsadresse.id;


--
-- Name: bruker_utenlandsadresser_aggregat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_utenlandsadresser_aggregat (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: bruker_utenlandsadresser_aggregat_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bruker_utenlandsadresser_aggregat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bruker_utenlandsadresser_aggregat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bruker_utenlandsadresser_aggregat_id_seq OWNED BY public.bruker_utenlandsadresser_aggregat.id;


--
-- Name: bruker_utenlandsadresser_forutgaaende_aggregat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_utenlandsadresser_forutgaaende_aggregat (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: bruker_utenlandsadresser_forutgaaende_aggregat_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bruker_utenlandsadresser_forutgaaende_aggregat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bruker_utenlandsadresser_forutgaaende_aggregat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bruker_utenlandsadresser_forutgaaende_aggregat_id_seq OWNED BY public.bruker_utenlandsadresser_forutgaaende_aggregat.id;


--
-- Name: dagpenger_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dagpenger_grunnlag (
    id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    behandling_id bigint NOT NULL,
    dagpenger_perioder_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: dagpenger_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dagpenger_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dagpenger_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dagpenger_grunnlag_id_seq OWNED BY public.dagpenger_grunnlag.id;


--
-- Name: dagpenger_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dagpenger_periode (
    id bigint NOT NULL,
    dagpenger_perioder_id bigint NOT NULL,
    periode daterange NOT NULL,
    ytelse_type text NOT NULL,
    kilde text NOT NULL
);


--
-- Name: dagpenger_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dagpenger_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dagpenger_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dagpenger_periode_id_seq OWNED BY public.dagpenger_periode.id;


--
-- Name: dagpenger_perioder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dagpenger_perioder (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: dagpenger_perioder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dagpenger_perioder_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dagpenger_perioder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dagpenger_perioder_id_seq OWNED BY public.dagpenger_perioder.id;


--
-- Name: egen_virksomhet_oppstart_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.egen_virksomhet_oppstart_periode (
    id bigint NOT NULL,
    perioder_id bigint NOT NULL,
    periode daterange NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: egen_virksomhet_oppstart_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.egen_virksomhet_oppstart_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: egen_virksomhet_oppstart_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.egen_virksomhet_oppstart_periode_id_seq OWNED BY public.egen_virksomhet_oppstart_periode.id;


--
-- Name: egen_virksomhet_oppstart_perioder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.egen_virksomhet_oppstart_perioder (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: egen_virksomhet_oppstart_perioder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.egen_virksomhet_oppstart_perioder_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: egen_virksomhet_oppstart_perioder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.egen_virksomhet_oppstart_perioder_id_seq OWNED BY public.egen_virksomhet_oppstart_perioder.id;


--
-- Name: egen_virksomhet_utvikling_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.egen_virksomhet_utvikling_periode (
    id bigint NOT NULL,
    perioder_id bigint NOT NULL,
    periode daterange NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: egen_virksomhet_utvikling_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.egen_virksomhet_utvikling_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: egen_virksomhet_utvikling_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.egen_virksomhet_utvikling_periode_id_seq OWNED BY public.egen_virksomhet_utvikling_periode.id;


--
-- Name: egen_virksomhet_utvikling_perioder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.egen_virksomhet_utvikling_perioder (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: egen_virksomhet_utvikling_perioder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.egen_virksomhet_utvikling_perioder_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: egen_virksomhet_utvikling_perioder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.egen_virksomhet_utvikling_perioder_id_seq OWNED BY public.egen_virksomhet_utvikling_perioder.id;


--
-- Name: etablering_egen_virksomhet_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.etablering_egen_virksomhet_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurderinger_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: etablering_egen_virksomhet_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.etablering_egen_virksomhet_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: etablering_egen_virksomhet_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.etablering_egen_virksomhet_grunnlag_id_seq OWNED BY public.etablering_egen_virksomhet_grunnlag.id;


--
-- Name: etablering_egen_virksomhet_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.etablering_egen_virksomhet_vurdering (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    foreligger_faglig_vurdering boolean NOT NULL,
    virksomhet_er_ny boolean,
    bruker_eier_virksomhet text,
    kan_bli_selvforsorget boolean,
    virksomhet_navn text NOT NULL,
    org_nr text,
    egen_virksomhet_utvikling_perioder_id bigint,
    egen_virksomhet_oppstart_perioder_id bigint,
    vurderinger_id bigint NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    vurdert_av character varying(50) NOT NULL,
    gjelder_fra date NOT NULL,
    gjelder_til date,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: etablering_egen_virksomhet_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.etablering_egen_virksomhet_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: etablering_egen_virksomhet_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.etablering_egen_virksomhet_vurdering_id_seq OWNED BY public.etablering_egen_virksomhet_vurdering.id;


--
-- Name: etablering_egen_virksomhet_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.etablering_egen_virksomhet_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: etablering_egen_virksomhet_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.etablering_egen_virksomhet_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: etablering_egen_virksomhet_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.etablering_egen_virksomhet_vurderinger_id_seq OWNED BY public.etablering_egen_virksomhet_vurderinger.id;


--
-- Name: formkrav_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.formkrav_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: formkrav_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.formkrav_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: formkrav_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.formkrav_grunnlag_id_seq OWNED BY public.formkrav_grunnlag.id;


--
-- Name: formkrav_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.formkrav_vurdering (
    id integer NOT NULL,
    begrunnelse text NOT NULL,
    er_bruker_part boolean NOT NULL,
    er_frist_overholdt boolean NOT NULL,
    er_konkret boolean NOT NULL,
    er_signert boolean NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av character varying(50) NOT NULL,
    likevel_behandles boolean
);


--
-- Name: formkrav_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.formkrav_vurdering_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: formkrav_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.formkrav_vurdering_id_seq OWNED BY public.formkrav_vurdering.id;


--
-- Name: forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    medlemskap_unntak_person_id bigint,
    inntekter_i_norge_id bigint,
    arbeider_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurderinger_id bigint
);


--
-- Name: forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnl_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnl_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnl_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnl_id_seq OWNED BY public.forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag.id;


--
-- Name: forutgaaende_medlemskap_manuell_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.forutgaaende_medlemskap_manuell_vurdering (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    har_forutgaaende_medlemskap boolean NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    var_medlem_med_nedsatt_arbeidsevne boolean,
    medlem_med_unntak_av_maks_fem_aar boolean,
    overstyrt boolean DEFAULT false NOT NULL,
    vurdert_av character varying(50) NOT NULL,
    vurderinger_id bigint NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    fom date NOT NULL,
    tom date
);


--
-- Name: forutgaaende_medlemskap_manuell_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.forutgaaende_medlemskap_manuell_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: forutgaaende_medlemskap_manuell_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.forutgaaende_medlemskap_manuell_vurdering_id_seq OWNED BY public.forutgaaende_medlemskap_manuell_vurdering.id;


--
-- Name: forutgaaende_medlemskap_manuell_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.forutgaaende_medlemskap_manuell_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: forutgaaende_medlemskap_manuell_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.forutgaaende_medlemskap_manuell_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: forutgaaende_medlemskap_manuell_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.forutgaaende_medlemskap_manuell_vurderinger_id_seq OWNED BY public.forutgaaende_medlemskap_manuell_vurderinger.id;


--
-- Name: fullmektig_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fullmektig_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: fullmektig_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fullmektig_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fullmektig_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.fullmektig_grunnlag_id_seq OWNED BY public.fullmektig_grunnlag.id;


--
-- Name: fullmektig_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fullmektig_vurdering (
    id bigint NOT NULL,
    har_fullmektig boolean NOT NULL,
    fullmektig_ident character varying(19),
    fullmektig_navn_og_adresse jsonb,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av character varying(50) NOT NULL,
    fullmektig_ident_type character varying(20)
);


--
-- Name: fullmektig_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fullmektig_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fullmektig_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.fullmektig_vurdering_id_seq OWNED BY public.fullmektig_vurdering.id;


--
-- Name: vedtak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vedtak (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vedtakstidspunkt timestamp(3) without time zone NOT NULL,
    virkningstidspunkt date
);


--
-- Name: gjeldende_vedtatte_behandlinger; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.gjeldende_vedtatte_behandlinger AS
 SELECT sak_id,
    rn,
    behandling_id
   FROM ( SELECT b.sak_id,
            row_number() OVER (PARTITION BY b.sak_id ORDER BY v.vedtakstidspunkt DESC) AS rn,
            b.id AS behandling_id
           FROM (public.behandling b
             JOIN public.vedtak v ON ((b.id = v.behandling_id)))
          WHERE ((b.status)::text = ANY ((ARRAY['AVSLUTTET'::character varying, 'IVERKSETTES'::character varying])::text[]))) q
  WHERE (rn = 1);


--
-- Name: helseopphold_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.helseopphold_vurdering (
    id bigint NOT NULL,
    helseopphold_vurderinger_id bigint NOT NULL,
    kost_og_losji boolean NOT NULL,
    forsorger_ektefelle boolean,
    faste_utgifter boolean,
    begrunnelse text NOT NULL,
    periode daterange NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    opphold_id bigint,
    vurdert_i_behandling bigint,
    vurdert_av character varying(50) NOT NULL
);


--
-- Name: helseopphold_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.helseopphold_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: helseopphold_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.helseopphold_vurdering_id_seq OWNED BY public.helseopphold_vurdering.id;


--
-- Name: helseopphold_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.helseopphold_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: helseopphold_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.helseopphold_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: helseopphold_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.helseopphold_vurderinger_id_seq OWNED BY public.helseopphold_vurderinger.id;


--
-- Name: informasjonskrav_oppdatert; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.informasjonskrav_oppdatert (
    id bigint NOT NULL,
    sak_id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    oppdatert timestamp(3) without time zone NOT NULL,
    informasjonskrav text NOT NULL,
    rettighetsperiode daterange,
    informasjonskrav_input jsonb
);


--
-- Name: informasjonskrav_oppdatert_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.informasjonskrav_oppdatert_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: informasjonskrav_oppdatert_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.informasjonskrav_oppdatert_id_seq OWNED BY public.informasjonskrav_oppdatert.id;


--
-- Name: inntekt; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inntekt (
    id bigint NOT NULL,
    inntekt_id bigint NOT NULL,
    ar smallint NOT NULL,
    belop numeric(19,2) NOT NULL
);


--
-- Name: inntekt_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inntekt_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    inntekt_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: inntekt_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inntekt_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inntekt_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inntekt_grunnlag_id_seq OWNED BY public.inntekt_grunnlag.id;


--
-- Name: inntekt_i_norge; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inntekt_i_norge (
    id bigint NOT NULL,
    identifikator character varying(30) NOT NULL,
    beloep numeric NOT NULL,
    skattemessig_bosatt_land character varying(3),
    opptjenings_land character varying(3),
    inntekt_type text,
    periode daterange NOT NULL,
    inntekter_i_norge_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    organisasjonsnavn character varying(150)
);


--
-- Name: inntekt_i_norge_forutgaaende; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inntekt_i_norge_forutgaaende (
    id bigint NOT NULL,
    identifikator character varying(30) NOT NULL,
    beloep numeric NOT NULL,
    skattemessig_bosatt_land character varying(3),
    opptjenings_land character varying(3),
    inntekt_type text,
    periode daterange NOT NULL,
    inntekter_i_norge_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    organisasjonsnavn character varying(150)
);


--
-- Name: inntekt_i_norge_forutgaaende_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inntekt_i_norge_forutgaaende_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inntekt_i_norge_forutgaaende_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inntekt_i_norge_forutgaaende_id_seq OWNED BY public.inntekt_i_norge_forutgaaende.id;


--
-- Name: inntekt_i_norge_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inntekt_i_norge_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inntekt_i_norge_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inntekt_i_norge_id_seq OWNED BY public.inntekt_i_norge.id;


--
-- Name: inntekt_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inntekt_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inntekt_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inntekt_id_seq OWNED BY public.inntekt.id;


--
-- Name: inntekt_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inntekt_periode (
    id bigint NOT NULL,
    inntekt_id bigint NOT NULL,
    periode daterange NOT NULL,
    belop numeric(19,2) NOT NULL
);


--
-- Name: inntekt_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inntekt_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inntekt_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inntekt_periode_id_seq OWNED BY public.inntekt_periode.id;


--
-- Name: inntekter; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inntekter (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: inntekter_i_norge; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inntekter_i_norge (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: inntekter_i_norge_forutgaaende; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inntekter_i_norge_forutgaaende (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: inntekter_i_norge_forutgaaende_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inntekter_i_norge_forutgaaende_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inntekter_i_norge_forutgaaende_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inntekter_i_norge_forutgaaende_id_seq OWNED BY public.inntekter_i_norge_forutgaaende.id;


--
-- Name: inntekter_i_norge_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inntekter_i_norge_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inntekter_i_norge_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inntekter_i_norge_id_seq OWNED BY public.inntekter_i_norge.id;


--
-- Name: inntekter_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inntekter_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inntekter_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inntekter_id_seq OWNED BY public.inntekter.id;


--
-- Name: inntektsbortfall_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inntektsbortfall_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurderinger_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: inntektsbortfall_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inntektsbortfall_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inntektsbortfall_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inntektsbortfall_grunnlag_id_seq OWNED BY public.inntektsbortfall_grunnlag.id;


--
-- Name: inntektsbortfall_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inntektsbortfall_vurdering (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    rett_til_alderspensjon_uttak boolean NOT NULL,
    vurderinger_id bigint NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    vurdert_av text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: inntektsbortfall_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inntektsbortfall_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inntektsbortfall_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inntektsbortfall_vurdering_id_seq OWNED BY public.inntektsbortfall_vurdering.id;


--
-- Name: inntektsbortfall_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inntektsbortfall_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: inntektsbortfall_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inntektsbortfall_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inntektsbortfall_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inntektsbortfall_vurderinger_id_seq OWNED BY public.inntektsbortfall_vurderinger.id;


--
-- Name: jobb; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jobb (
    id bigint NOT NULL,
    status character varying(50) DEFAULT 'KLAR'::character varying NOT NULL,
    type character varying(50) NOT NULL,
    sak_id bigint,
    behandling_id bigint,
    neste_kjoring timestamp(3) without time zone NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    parameters text,
    payload text
);


--
-- Name: jobb_arkiv; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jobb_arkiv (
    id bigint NOT NULL,
    status character varying(50) NOT NULL,
    type character varying(50) NOT NULL,
    sak_id bigint,
    behandling_id bigint,
    parameters text,
    payload text,
    neste_kjoring timestamp(3) without time zone NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: jobb_historikk; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jobb_historikk (
    id bigint NOT NULL,
    jobb_id bigint NOT NULL,
    status character varying(50) NOT NULL,
    feilmelding text,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: jobb_historikk_arkiv; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jobb_historikk_arkiv (
    id bigint NOT NULL,
    jobb_id bigint NOT NULL,
    status character varying(50) NOT NULL,
    feilmelding text,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: kanseller_revurdering_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.kanseller_revurdering_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: kanseller_revurdering_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.kanseller_revurdering_grunnlag_id_seq OWNED BY public.avbryt_revurdering_grunnlag.id;


--
-- Name: kanseller_revurdering_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.kanseller_revurdering_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: kanseller_revurdering_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.kanseller_revurdering_vurdering_id_seq OWNED BY public.avbryt_revurdering_vurdering.id;


--
-- Name: klage_kontor_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.klage_kontor_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: klage_kontor_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.klage_kontor_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: klage_kontor_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.klage_kontor_grunnlag_id_seq OWNED BY public.klage_kontor_grunnlag.id;


--
-- Name: klage_kontor_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.klage_kontor_vurdering (
    id integer NOT NULL,
    begrunnelse text NOT NULL,
    notat text,
    innstilling character varying(20) NOT NULL,
    vilkaar_som_skal_omgjoeres text[] DEFAULT ARRAY[]::text[] NOT NULL,
    vilkaar_som_skal_opprettholdes text[] DEFAULT ARRAY[]::text[] NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av character varying(50) NOT NULL
);


--
-- Name: klage_kontor_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.klage_kontor_vurdering_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: klage_kontor_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.klage_kontor_vurdering_id_seq OWNED BY public.klage_kontor_vurdering.id;


--
-- Name: klage_nay_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.klage_nay_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: klage_nay_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.klage_nay_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: klage_nay_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.klage_nay_grunnlag_id_seq OWNED BY public.klage_nay_grunnlag.id;


--
-- Name: klage_nay_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.klage_nay_vurdering (
    id integer NOT NULL,
    begrunnelse text NOT NULL,
    notat text,
    innstilling character varying(20) NOT NULL,
    vilkaar_som_skal_omgjoeres text[] DEFAULT ARRAY[]::text[] NOT NULL,
    vilkaar_som_skal_opprettholdes text[] DEFAULT ARRAY[]::text[] NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av character varying(50) NOT NULL
);


--
-- Name: klage_nay_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.klage_nay_vurdering_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: klage_nay_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.klage_nay_vurdering_id_seq OWNED BY public.klage_nay_vurdering.id;


--
-- Name: krav_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.krav_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    krav_vurderinger_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: krav_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.krav_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: krav_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.krav_grunnlag_id_seq OWNED BY public.krav_grunnlag.id;


--
-- Name: krav_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.krav_vurdering (
    id bigint NOT NULL,
    krav_vurderinger_id bigint NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    journalpost_id text NOT NULL,
    vurdert_av text NOT NULL,
    begrunnelse text NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL,
    krav_type text NOT NULL,
    kravdato date,
    soknadsdato date,
    soknadsdato_aarsak text,
    mulig_rett_fra date,
    mulig_rett_fra_aarsak text
);


--
-- Name: krav_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.krav_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: krav_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.krav_vurdering_id_seq OWNED BY public.krav_vurdering.id;


--
-- Name: krav_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.krav_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: krav_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.krav_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: krav_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.krav_vurderinger_id_seq OWNED BY public.krav_vurderinger.id;


--
-- Name: lovvalg_medlemskap_manuell_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lovvalg_medlemskap_manuell_vurdering (
    id bigint NOT NULL,
    tekstvurdering_lovvalg text NOT NULL,
    lovvalgs_land character varying(3) NOT NULL,
    tekstvurdering_medlemskap text,
    var_medlem_i_folketrygden boolean,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    overstyrt boolean DEFAULT false NOT NULL,
    vurdert_av character varying(50) NOT NULL,
    vurderinger_id bigint NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    fom date NOT NULL,
    tom date
);


--
-- Name: lovvalg_medlemskap_manuell_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.lovvalg_medlemskap_manuell_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: lovvalg_medlemskap_manuell_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.lovvalg_medlemskap_manuell_vurdering_id_seq OWNED BY public.lovvalg_medlemskap_manuell_vurdering.id;


--
-- Name: lovvalg_medlemskap_manuell_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lovvalg_medlemskap_manuell_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: lovvalg_medlemskap_manuell_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.lovvalg_medlemskap_manuell_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: lovvalg_medlemskap_manuell_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.lovvalg_medlemskap_manuell_vurderinger_id_seq OWNED BY public.lovvalg_medlemskap_manuell_vurderinger.id;


--
-- Name: manuell_inntekt_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.manuell_inntekt_vurdering (
    id bigint NOT NULL,
    ar smallint NOT NULL,
    begrunnelse text NOT NULL,
    belop numeric(19,2),
    vurdert_av text DEFAULT 'Ukjent'::text NOT NULL,
    manuell_inntekt_vurderinger_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    eos_belop numeric(19,2),
    ferdig_lignet_pgi numeric(19,2)
);


--
-- Name: manuell_inntekt_vurdering_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.manuell_inntekt_vurdering_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    manuell_inntekt_vurderinger_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: manuell_inntekt_vurdering_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.manuell_inntekt_vurdering_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: manuell_inntekt_vurdering_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.manuell_inntekt_vurdering_grunnlag_id_seq OWNED BY public.manuell_inntekt_vurdering_grunnlag.id;


--
-- Name: manuell_inntekt_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.manuell_inntekt_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: manuell_inntekt_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.manuell_inntekt_vurdering_id_seq OWNED BY public.manuell_inntekt_vurdering.id;


--
-- Name: manuell_inntekt_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.manuell_inntekt_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: manuell_inntekt_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.manuell_inntekt_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: manuell_inntekt_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.manuell_inntekt_vurderinger_id_seq OWNED BY public.manuell_inntekt_vurderinger.id;


--
-- Name: medlemskap_arbeid_og_inntekt_i_norge_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medlemskap_arbeid_og_inntekt_i_norge_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    medlemskap_unntak_person_id bigint,
    inntekter_i_norge_id bigint,
    arbeider_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurderinger_id bigint
);


--
-- Name: medlemskap_arbeid_og_inntekt_i_norge_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.medlemskap_arbeid_og_inntekt_i_norge_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: medlemskap_arbeid_og_inntekt_i_norge_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.medlemskap_arbeid_og_inntekt_i_norge_grunnlag_id_seq OWNED BY public.medlemskap_arbeid_og_inntekt_i_norge_grunnlag.id;


--
-- Name: medlemskap_forutgaaende_unntak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medlemskap_forutgaaende_unntak (
    id bigint NOT NULL,
    medlemskap_forutgaaende_unntak_person_id bigint NOT NULL,
    status text NOT NULL,
    status_arsak text,
    medlem boolean NOT NULL,
    periode daterange NOT NULL,
    grunnlag text NOT NULL,
    lovvalg text NOT NULL,
    helsedel boolean NOT NULL,
    lovvalgsland character varying(50),
    kildesystem character varying(50),
    kildenavn character varying(50)
);


--
-- Name: medlemskap_forutgaaende_unntak_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medlemskap_forutgaaende_unntak_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    medlemskap_forutgaaende_unntak_person_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: medlemskap_forutgaaende_unntak_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.medlemskap_forutgaaende_unntak_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: medlemskap_forutgaaende_unntak_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.medlemskap_forutgaaende_unntak_grunnlag_id_seq OWNED BY public.medlemskap_forutgaaende_unntak_grunnlag.id;


--
-- Name: medlemskap_forutgaaende_unntak_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.medlemskap_forutgaaende_unntak_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: medlemskap_forutgaaende_unntak_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.medlemskap_forutgaaende_unntak_id_seq OWNED BY public.medlemskap_forutgaaende_unntak.id;


--
-- Name: medlemskap_forutgaaende_unntak_person; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medlemskap_forutgaaende_unntak_person (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: medlemskap_forutgaaende_unntak_person_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.medlemskap_forutgaaende_unntak_person_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: medlemskap_forutgaaende_unntak_person_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.medlemskap_forutgaaende_unntak_person_id_seq OWNED BY public.medlemskap_forutgaaende_unntak_person.id;


--
-- Name: medlemskap_unntak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medlemskap_unntak (
    id bigint NOT NULL,
    medlemskap_unntak_person_id bigint NOT NULL,
    status text NOT NULL,
    status_arsak text,
    medlem boolean NOT NULL,
    periode daterange NOT NULL,
    grunnlag text NOT NULL,
    lovvalg text NOT NULL,
    helsedel boolean NOT NULL,
    lovvalgsland character varying(50),
    kildesystem character varying(50),
    kildenavn character varying(50)
);


--
-- Name: medlemskap_unntak_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medlemskap_unntak_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    medlemskap_unntak_person_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: medlemskap_unntak_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.medlemskap_unntak_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: medlemskap_unntak_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.medlemskap_unntak_grunnlag_id_seq OWNED BY public.medlemskap_unntak_grunnlag.id;


--
-- Name: medlemskap_unntak_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.medlemskap_unntak_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: medlemskap_unntak_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.medlemskap_unntak_id_seq OWNED BY public.medlemskap_unntak.id;


--
-- Name: medlemskap_unntak_person; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medlemskap_unntak_person (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: medlemskap_unntak_person_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.medlemskap_unntak_person_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: medlemskap_unntak_person_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.medlemskap_unntak_person_id_seq OWNED BY public.medlemskap_unntak_person.id;


--
-- Name: meldekort; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meldekort (
    id bigint NOT NULL,
    meldekortene_id bigint NOT NULL,
    journalpost character varying(25) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    mottatt_tidspunkt timestamp(3) without time zone NOT NULL
);


--
-- Name: meldekort_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meldekort_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    meldekortene_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: meldekort_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldekort_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldekort_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldekort_grunnlag_id_seq OWNED BY public.meldekort_grunnlag.id;


--
-- Name: meldekort_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldekort_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldekort_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldekort_id_seq OWNED BY public.meldekort.id;


--
-- Name: meldekort_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meldekort_periode (
    id bigint NOT NULL,
    meldekort_id bigint NOT NULL,
    periode daterange NOT NULL,
    timer_arbeid numeric(5,1) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: meldekort_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldekort_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldekort_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldekort_periode_id_seq OWNED BY public.meldekort_periode.id;


--
-- Name: meldekortene; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meldekortene (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: meldekortene_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldekortene_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldekortene_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldekortene_id_seq OWNED BY public.meldekortene.id;


--
-- Name: meldeperiode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meldeperiode (
    id bigint NOT NULL,
    meldeperiodegrunnlag_id bigint NOT NULL,
    opprettet timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    periode daterange NOT NULL
);


--
-- Name: meldeperiode_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meldeperiode_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    opprettet timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    aktiv boolean NOT NULL
);


--
-- Name: meldeperiode_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldeperiode_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldeperiode_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldeperiode_grunnlag_id_seq OWNED BY public.meldeperiode_grunnlag.id;


--
-- Name: meldeperiode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldeperiode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldeperiode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldeperiode_id_seq OWNED BY public.meldeperiode.id;


--
-- Name: meldeplikt_fritak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meldeplikt_fritak (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: meldeplikt_fritak_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meldeplikt_fritak_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    meldeplikt_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: meldeplikt_fritak_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldeplikt_fritak_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldeplikt_fritak_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldeplikt_fritak_grunnlag_id_seq OWNED BY public.meldeplikt_fritak_grunnlag.id;


--
-- Name: meldeplikt_fritak_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldeplikt_fritak_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldeplikt_fritak_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldeplikt_fritak_id_seq OWNED BY public.meldeplikt_fritak.id;


--
-- Name: meldeplikt_fritak_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meldeplikt_fritak_vurdering (
    id bigint NOT NULL,
    meldeplikt_id bigint NOT NULL,
    begrunnelse text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    har_fritak boolean NOT NULL,
    fra_dato date NOT NULL,
    vurdert_av text NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    til_dato date
);


--
-- Name: meldeplikt_fritak_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldeplikt_fritak_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldeplikt_fritak_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldeplikt_fritak_vurdering_id_seq OWNED BY public.meldeplikt_fritak_vurdering.id;


--
-- Name: meldeplikt_overstyring_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meldeplikt_overstyring_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    aktiv boolean NOT NULL
);


--
-- Name: meldeplikt_overstyring_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldeplikt_overstyring_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldeplikt_overstyring_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldeplikt_overstyring_grunnlag_id_seq OWNED BY public.meldeplikt_overstyring_grunnlag.id;


--
-- Name: meldeplikt_overstyring_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meldeplikt_overstyring_vurdering (
    id bigint NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    vurdert_av text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: meldeplikt_overstyring_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldeplikt_overstyring_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldeplikt_overstyring_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldeplikt_overstyring_vurdering_id_seq OWNED BY public.meldeplikt_overstyring_vurdering.id;


--
-- Name: meldeplikt_overstyring_vurdering_perioder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meldeplikt_overstyring_vurdering_perioder (
    id bigint NOT NULL,
    meldeplikt_overstyring_vurdering_id bigint NOT NULL,
    meldeplikt_overstyring_status text NOT NULL,
    periode daterange NOT NULL,
    begrunnelse text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: meldeplikt_overstyring_vurdering_perioder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldeplikt_overstyring_vurdering_perioder_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldeplikt_overstyring_vurdering_perioder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldeplikt_overstyring_vurdering_perioder_id_seq OWNED BY public.meldeplikt_overstyring_vurdering_perioder.id;


--
-- Name: meldeplikt_overstyring_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meldeplikt_overstyring_vurderinger (
    grunnlag_id bigint NOT NULL,
    vurdering_id bigint NOT NULL
);


--
-- Name: meldeplikt_overstyring_vurderinger_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldeplikt_overstyring_vurderinger_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldeplikt_overstyring_vurderinger_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldeplikt_overstyring_vurderinger_grunnlag_id_seq OWNED BY public.meldeplikt_overstyring_vurderinger.grunnlag_id;


--
-- Name: meldeplikt_overstyring_vurderinger_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meldeplikt_overstyring_vurderinger_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meldeplikt_overstyring_vurderinger_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meldeplikt_overstyring_vurderinger_vurdering_id_seq OWNED BY public.meldeplikt_overstyring_vurderinger.vurdering_id;


--
-- Name: mellomlagret_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mellomlagret_vurdering (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    avklaringsbehov_kode text NOT NULL,
    data jsonb NOT NULL,
    vurdert_av text NOT NULL,
    vurdert_dato timestamp(3) without time zone NOT NULL
);


--
-- Name: mellomlagret_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.mellomlagret_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: mellomlagret_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.mellomlagret_vurdering_id_seq OWNED BY public.mellomlagret_vurdering.id;


--
-- Name: mottatt_dokument; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mottatt_dokument (
    id bigint NOT NULL,
    sak_id bigint NOT NULL,
    behandling_id bigint,
    mottatt_tid timestamp(3) without time zone NOT NULL,
    type character varying(50) NOT NULL,
    status character varying(50) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    strukturert_dokument text,
    referanse text,
    referanse_type text,
    kanal character varying(50) NOT NULL,
    digitalisert_manuelt_postmottak boolean
);


--
-- Name: mottatt_dokument_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.mottatt_dokument_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: mottatt_dokument_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.mottatt_dokument_id_seq OWNED BY public.mottatt_dokument.id;


--
-- Name: oppfolgingsoppgave_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppfolgingsoppgave_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: oppfolgingsoppgave_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppfolgingsoppgave_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppfolgingsoppgave_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppfolgingsoppgave_grunnlag_id_seq OWNED BY public.oppfolgingsoppgave_grunnlag.id;


--
-- Name: oppfolgingsoppgave_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppfolgingsoppgave_vurdering (
    id bigint NOT NULL,
    konsekvens_av_oppfolging text,
    opplysninger_til_revurdering text[],
    aarsak_til_revurdering text,
    vurdert_av text
);


--
-- Name: oppfolgingsoppgave_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppfolgingsoppgave_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppfolgingsoppgave_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppfolgingsoppgave_vurdering_id_seq OWNED BY public.oppfolgingsoppgave_vurdering.id;


--
-- Name: oppgave_historikk_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppgave_historikk_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppgave_historikk_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppgave_historikk_id_seq OWNED BY public.jobb_historikk.id;


--
-- Name: oppgave_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppgave_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppgave_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppgave_id_seq OWNED BY public.jobb.id;


--
-- Name: oppgitt_barn; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppgitt_barn (
    id bigint NOT NULL,
    ident character varying(11),
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    oppgitt_barn_id bigint NOT NULL,
    navn text,
    fodselsdato date,
    relasjon text
);


--
-- Name: oppgitt_barn_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppgitt_barn_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppgitt_barn_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppgitt_barn_id_seq OWNED BY public.oppgitt_barn.id;


--
-- Name: oppgitt_barnopplysning; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppgitt_barnopplysning (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: oppgitt_barnopplysning_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppgitt_barnopplysning_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppgitt_barnopplysning_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppgitt_barnopplysning_id_seq OWNED BY public.oppgitt_barnopplysning.id;


--
-- Name: oppgitt_student; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppgitt_student (
    id bigint NOT NULL,
    avbrutt_dato date,
    er_student text NOT NULL,
    skal_gjenoppta_studie text
);


--
-- Name: oppgitt_student_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppgitt_student_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppgitt_student_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppgitt_student_id_seq OWNED BY public.oppgitt_student.id;


--
-- Name: oppgitt_utenlandsopphold; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppgitt_utenlandsopphold (
    id bigint NOT NULL,
    bodd_i_norge_siste_fem_aar boolean NOT NULL,
    arbeidet_i_norge_siste_fem_aar boolean NOT NULL,
    arbeidet_utenfor_norge_for_sykdom boolean NOT NULL,
    i_tillegg_arbeid_utenfor_norge boolean NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: oppgitt_utenlandsopphold_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppgitt_utenlandsopphold_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    journalpost_id character varying(50) NOT NULL,
    oppgitt_utenlandsopphold_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: oppgitt_utenlandsopphold_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppgitt_utenlandsopphold_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppgitt_utenlandsopphold_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppgitt_utenlandsopphold_grunnlag_id_seq OWNED BY public.oppgitt_utenlandsopphold_grunnlag.id;


--
-- Name: oppgitt_utenlandsopphold_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppgitt_utenlandsopphold_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppgitt_utenlandsopphold_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppgitt_utenlandsopphold_id_seq OWNED BY public.oppgitt_utenlandsopphold.id;


--
-- Name: opphold; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.opphold (
    id bigint NOT NULL,
    opphold_person_id bigint NOT NULL,
    institusjonstype text NOT NULL,
    kategori text NOT NULL,
    orgnr character varying(9) NOT NULL,
    periode daterange NOT NULL,
    institusjonsnavn character varying(255) NOT NULL,
    opprettet_tid timestamp(3) without time zone
);


--
-- Name: opphold_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.opphold_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    opphold_person_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    soning_vurderinger_id bigint,
    helseopphold_vurderinger_id bigint
);


--
-- Name: opphold_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.opphold_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: opphold_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.opphold_grunnlag_id_seq OWNED BY public.opphold_grunnlag.id;


--
-- Name: opphold_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.opphold_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: opphold_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.opphold_id_seq OWNED BY public.opphold.id;


--
-- Name: opphold_person; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.opphold_person (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: opphold_person_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.opphold_person_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: opphold_person_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.opphold_person_id_seq OWNED BY public.opphold_person.id;


--
-- Name: oppholdskrav_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppholdskrav_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    aktiv boolean NOT NULL
);


--
-- Name: oppholdskrav_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppholdskrav_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppholdskrav_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppholdskrav_grunnlag_id_seq OWNED BY public.oppholdskrav_grunnlag.id;


--
-- Name: oppholdskrav_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppholdskrav_vurdering (
    id bigint NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    vurdert_av text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: oppholdskrav_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppholdskrav_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppholdskrav_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppholdskrav_vurdering_id_seq OWNED BY public.oppholdskrav_vurdering.id;


--
-- Name: oppholdskrav_vurdering_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppholdskrav_vurdering_periode (
    id bigint NOT NULL,
    oppholdskrav_vurdering_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fom date NOT NULL,
    tom date,
    begrunnelse text NOT NULL,
    oppfylt boolean NOT NULL,
    land text
);


--
-- Name: oppholdskrav_vurdering_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppholdskrav_vurdering_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppholdskrav_vurdering_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppholdskrav_vurdering_periode_id_seq OWNED BY public.oppholdskrav_vurdering_periode.id;


--
-- Name: oppholdskrav_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppholdskrav_vurderinger (
    grunnlag_id bigint NOT NULL,
    vurdering_id bigint NOT NULL
);


--
-- Name: oppholdskrav_vurderinger_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppholdskrav_vurderinger_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppholdskrav_vurderinger_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppholdskrav_vurderinger_grunnlag_id_seq OWNED BY public.oppholdskrav_vurderinger.grunnlag_id;


--
-- Name: oppholdskrav_vurderinger_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppholdskrav_vurderinger_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppholdskrav_vurderinger_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.oppholdskrav_vurderinger_vurdering_id_seq OWNED BY public.oppholdskrav_vurderinger.vurdering_id;


--
-- Name: overgang_arbeid_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.overgang_arbeid_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurderinger_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: overgang_arbeid_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.overgang_arbeid_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: overgang_arbeid_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.overgang_arbeid_grunnlag_id_seq OWNED BY public.overgang_arbeid_grunnlag.id;


--
-- Name: overgang_arbeid_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.overgang_arbeid_vurdering (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    bruker_rett_paa_aap boolean NOT NULL,
    vurdert_av character varying(50) NOT NULL,
    vurderingen_gjelder_fra date NOT NULL,
    vurderingen_gjelder_til date,
    opprettet_tid timestamp(3) without time zone NOT NULL,
    vurderinger_id bigint NOT NULL,
    vurdert_i_behandling bigint NOT NULL
);


--
-- Name: overgang_arbeid_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.overgang_arbeid_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: overgang_arbeid_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.overgang_arbeid_vurdering_id_seq OWNED BY public.overgang_arbeid_vurdering.id;


--
-- Name: overgang_arbeid_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.overgang_arbeid_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: overgang_arbeid_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.overgang_arbeid_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: overgang_arbeid_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.overgang_arbeid_vurderinger_id_seq OWNED BY public.overgang_arbeid_vurderinger.id;


--
-- Name: overgang_ufore_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.overgang_ufore_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurderinger_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: overgang_ufore_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.overgang_ufore_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: overgang_ufore_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.overgang_ufore_grunnlag_id_seq OWNED BY public.overgang_ufore_grunnlag.id;


--
-- Name: overgang_ufore_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.overgang_ufore_vurdering (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    bruker_sokt_uforetrygd boolean DEFAULT false NOT NULL,
    bruker_vedtak_uforetrygd text,
    bruker_rett_paa_aap boolean DEFAULT false NOT NULL,
    vurdert_av character varying(50) DEFAULT 'Ukjent'::character varying NOT NULL,
    virkningsdato date NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurderinger_id bigint,
    vurdert_i_behandling bigint NOT NULL,
    tom date
);


--
-- Name: overgang_ufore_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.overgang_ufore_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: overgang_ufore_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.overgang_ufore_vurdering_id_seq OWNED BY public.overgang_ufore_vurdering.id;


--
-- Name: overgang_ufore_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.overgang_ufore_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: overgang_ufore_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.overgang_ufore_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: overgang_ufore_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.overgang_ufore_vurderinger_id_seq OWNED BY public.overgang_ufore_vurderinger.id;


--
-- Name: paaklaget_behandling_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.paaklaget_behandling_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: paaklaget_behandling_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.paaklaget_behandling_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: paaklaget_behandling_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.paaklaget_behandling_grunnlag_id_seq OWNED BY public.paaklaget_behandling_grunnlag.id;


--
-- Name: paaklaget_behandling_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.paaklaget_behandling_vurdering (
    id integer NOT NULL,
    type_vedtak character varying(100) NOT NULL,
    paaklaget_behandling_id bigint,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av character varying(50) NOT NULL
);


--
-- Name: paaklaget_behandling_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.paaklaget_behandling_vurdering_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: paaklaget_behandling_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.paaklaget_behandling_vurdering_id_seq OWNED BY public.paaklaget_behandling_vurdering.id;


--
-- Name: person; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.person (
    id bigint NOT NULL,
    referanse uuid NOT NULL
);


--
-- Name: person_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.person_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: person_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.person_id_seq OWNED BY public.person.id;


--
-- Name: person_ident; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.person_ident (
    id bigint NOT NULL,
    person_id bigint NOT NULL,
    ident character varying(19) NOT NULL,
    primaer boolean NOT NULL
);


--
-- Name: person_ident_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.person_ident_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: person_ident_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.person_ident_id_seq OWNED BY public.person_ident.id;


--
-- Name: personopplysning_forutgaaende_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.personopplysning_forutgaaende_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    bruker_personopplysning_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: personopplysning_forutgaaende_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.personopplysning_forutgaaende_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: personopplysning_forutgaaende_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.personopplysning_forutgaaende_grunnlag_id_seq OWNED BY public.personopplysning_forutgaaende_grunnlag.id;


--
-- Name: personopplysning_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.personopplysning_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    bruker_personopplysning_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: personopplysning_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.personopplysning_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: personopplysning_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.personopplysning_grunnlag_id_seq OWNED BY public.personopplysning_grunnlag.id;


--
-- Name: personopplysning_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.personopplysning_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: personopplysning_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.personopplysning_id_seq OWNED BY public.bruker_personopplysning.id;


--
-- Name: reduksjon_11_9; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reduksjon_11_9 (
    id bigint NOT NULL,
    dato date NOT NULL,
    dagsats numeric(21,0) NOT NULL,
    reduksjon_11_9_grunnlag_id bigint NOT NULL
);


--
-- Name: reduksjon_11_9_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reduksjon_11_9_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: reduksjon_11_9_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.reduksjon_11_9_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: reduksjon_11_9_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.reduksjon_11_9_grunnlag_id_seq OWNED BY public.reduksjon_11_9_grunnlag.id;


--
-- Name: reduksjon_11_9_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.reduksjon_11_9_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: reduksjon_11_9_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.reduksjon_11_9_id_seq OWNED BY public.reduksjon_11_9.id;


--
-- Name: refusjonkrav_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refusjonkrav_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    sak_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    refusjonkrav_vurderinger_id bigint
);


--
-- Name: refusjonkrav_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refusjonkrav_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refusjonkrav_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.refusjonkrav_grunnlag_id_seq OWNED BY public.refusjonkrav_grunnlag.id;


--
-- Name: refusjonkrav_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refusjonkrav_vurdering (
    id bigint NOT NULL,
    har_krav boolean NOT NULL,
    fom date,
    tom date,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av text NOT NULL,
    navkontor text DEFAULT ''::text NOT NULL,
    refusjonkrav_vurderinger_id bigint
);


--
-- Name: refusjonkrav_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refusjonkrav_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refusjonkrav_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.refusjonkrav_vurdering_id_seq OWNED BY public.refusjonkrav_vurdering.id;


--
-- Name: refusjonkrav_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refusjonkrav_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: refusjonkrav_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refusjonkrav_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refusjonkrav_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.refusjonkrav_vurderinger_id_seq OWNED BY public.refusjonkrav_vurderinger.id;


--
-- Name: rettighetsperiode_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rettighetsperiode_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurderinger_id bigint NOT NULL,
    opprettet timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    aktiv boolean DEFAULT true NOT NULL
);


--
-- Name: rettighetsperiode_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.rettighetsperiode_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rettighetsperiode_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.rettighetsperiode_grunnlag_id_seq OWNED BY public.rettighetsperiode_grunnlag.id;


--
-- Name: rettighetsperiode_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rettighetsperiode_vurdering (
    id bigint NOT NULL,
    vurderinger_id bigint NOT NULL,
    begrunnelse text NOT NULL,
    start_dato date,
    har_rett_utover_soknadsdato text NOT NULL,
    har_krav_paa_renter boolean,
    opprettet timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    vurdert_av character varying(50) NOT NULL
);


--
-- Name: rettighetsperiode_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.rettighetsperiode_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rettighetsperiode_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.rettighetsperiode_vurdering_id_seq OWNED BY public.rettighetsperiode_vurdering.id;


--
-- Name: rettighetsperiode_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rettighetsperiode_vurderinger (
    id bigint NOT NULL,
    opprettet timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL
);


--
-- Name: rettighetsperiode_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.rettighetsperiode_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rettighetsperiode_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.rettighetsperiode_vurderinger_id_seq OWNED BY public.rettighetsperiode_vurderinger.id;


--
-- Name: rettighetstype_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rettighetstype_grunnlag (
    id bigint NOT NULL,
    perioder_id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    opprettet_tid timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    aktiv boolean DEFAULT true NOT NULL
);


--
-- Name: rettighetstype_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.rettighetstype_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rettighetstype_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.rettighetstype_grunnlag_id_seq OWNED BY public.rettighetstype_grunnlag.id;


--
-- Name: rettighetstype_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rettighetstype_periode (
    id bigint NOT NULL,
    perioder_id bigint NOT NULL,
    periode daterange NOT NULL,
    rettighetstype text NOT NULL,
    opprettet_tid timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: rettighetstype_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.rettighetstype_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rettighetstype_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.rettighetstype_periode_id_seq OWNED BY public.rettighetstype_periode.id;


--
-- Name: rettighetstype_perioder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rettighetstype_perioder (
    id bigint NOT NULL,
    opprettet_tid timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: rettighetstype_perioder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.rettighetstype_perioder_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rettighetstype_perioder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.rettighetstype_perioder_id_seq OWNED BY public.rettighetstype_perioder.id;


--
-- Name: rettighetstype_sporing; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rettighetstype_sporing (
    id bigint NOT NULL,
    rettighetstype_grunnlag_id bigint NOT NULL,
    versjon text NOT NULL,
    faktagrunnlag text NOT NULL
);


--
-- Name: rettighetstype_sporing_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.rettighetstype_sporing_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rettighetstype_sporing_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.rettighetstype_sporing_id_seq OWNED BY public.rettighetstype_sporing.id;


--
-- Name: sak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sak (
    id bigint NOT NULL,
    saksnummer character varying(19) NOT NULL,
    person_id bigint NOT NULL,
    rettighetsperiode daterange NOT NULL,
    status character varying(100) NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: sak_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sak_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sak_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sak_id_seq OWNED BY public.sak.id;


--
-- Name: sam_id; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sam_id (
    id bigint NOT NULL,
    sam_id text NOT NULL,
    behandling_id bigint NOT NULL
);


--
-- Name: sam_id_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sam_id_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sam_id_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sam_id_id_seq OWNED BY public.sam_id.id;


--
-- Name: samordning_andre_statlige_ytelser_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_andre_statlige_ytelser_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: samordning_andre_statlige_ytelser_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_andre_statlige_ytelser_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_andre_statlige_ytelser_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_andre_statlige_ytelser_grunnlag_id_seq OWNED BY public.samordning_andre_statlige_ytelser_grunnlag.id;


--
-- Name: samordning_andre_statlige_ytelser_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_andre_statlige_ytelser_vurdering (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av character varying(50) NOT NULL
);


--
-- Name: samordning_andre_statlige_ytelser_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_andre_statlige_ytelser_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_andre_statlige_ytelser_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_andre_statlige_ytelser_vurdering_id_seq OWNED BY public.samordning_andre_statlige_ytelser_vurdering.id;


--
-- Name: samordning_andre_statlige_ytelser_vurdering_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_andre_statlige_ytelser_vurdering_periode (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdering_id bigint NOT NULL,
    periode daterange NOT NULL,
    ytelse_type character varying(50) NOT NULL
);


--
-- Name: samordning_andre_statlige_ytelser_vurdering_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_andre_statlige_ytelser_vurdering_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_andre_statlige_ytelser_vurdering_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_andre_statlige_ytelser_vurdering_periode_id_seq OWNED BY public.samordning_andre_statlige_ytelser_vurdering_periode.id;


--
-- Name: samordning_arbeidsgiver_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_arbeidsgiver_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    sak_id bigint NOT NULL,
    samordning_arbeidsgiver_vurdering_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: samordning_arbeidsgiver_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_arbeidsgiver_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_arbeidsgiver_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_arbeidsgiver_grunnlag_id_seq OWNED BY public.samordning_arbeidsgiver_grunnlag.id;


--
-- Name: samordning_arbeidsgiver_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_arbeidsgiver_vurdering (
    id bigint NOT NULL,
    begrunnelse text,
    fom date,
    tom date,
    vurdert_av character varying(50) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: samordning_arbeidsgiver_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_arbeidsgiver_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_arbeidsgiver_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_arbeidsgiver_vurdering_id_seq OWNED BY public.samordning_arbeidsgiver_vurdering.id;


--
-- Name: samordning_arbeidsgiver_vurdering_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_arbeidsgiver_vurdering_periode (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdering_id bigint NOT NULL,
    fom date NOT NULL,
    tom date NOT NULL
);


--
-- Name: samordning_arbeidsgiver_vurdering_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_arbeidsgiver_vurdering_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_arbeidsgiver_vurdering_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_arbeidsgiver_vurdering_periode_id_seq OWNED BY public.samordning_arbeidsgiver_vurdering_periode.id;


--
-- Name: samordning_barnepensjon_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_barnepensjon_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    opprettet timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    aktiv boolean DEFAULT true NOT NULL,
    vurdering_id bigint NOT NULL
);


--
-- Name: samordning_barnepensjon_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_barnepensjon_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_barnepensjon_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_barnepensjon_grunnlag_id_seq OWNED BY public.samordning_barnepensjon_grunnlag.id;


--
-- Name: samordning_barnepensjon_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_barnepensjon_vurdering (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    vurdert_av_ident text NOT NULL,
    opprettet timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: samordning_barnepensjon_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_barnepensjon_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_barnepensjon_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_barnepensjon_vurdering_id_seq OWNED BY public.samordning_barnepensjon_vurdering.id;


--
-- Name: samordning_barnepensjon_vurdering_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_barnepensjon_vurdering_periode (
    id bigint NOT NULL,
    vurdering_id bigint NOT NULL,
    fom text NOT NULL,
    tom text,
    maaned_beloep numeric NOT NULL
);


--
-- Name: samordning_barnepensjon_vurdering_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_barnepensjon_vurdering_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_barnepensjon_vurdering_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_barnepensjon_vurdering_periode_id_seq OWNED BY public.samordning_barnepensjon_vurdering_periode.id;


--
-- Name: samordning_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    perioder_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    faktagrunnlag text
);


--
-- Name: samordning_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_grunnlag_id_seq OWNED BY public.samordning_grunnlag.id;


--
-- Name: samordning_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_periode (
    id bigint NOT NULL,
    perioder_id bigint NOT NULL,
    periode daterange NOT NULL,
    gradering smallint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: samordning_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_periode_id_seq OWNED BY public.samordning_periode.id;


--
-- Name: samordning_perioder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_perioder (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: samordning_perioder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_perioder_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_perioder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_perioder_id_seq OWNED BY public.samordning_perioder.id;


--
-- Name: samordning_ufore_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_ufore_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: samordning_ufore_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_ufore_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_ufore_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_ufore_grunnlag_id_seq OWNED BY public.samordning_ufore_grunnlag.id;


--
-- Name: samordning_ufore_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_ufore_vurdering (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av text NOT NULL
);


--
-- Name: samordning_ufore_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_ufore_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_ufore_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_ufore_vurdering_id_seq OWNED BY public.samordning_ufore_vurdering.id;


--
-- Name: samordning_ufore_vurdering_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_ufore_vurdering_periode (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdering_id bigint NOT NULL,
    uforegrad smallint NOT NULL,
    virkningstidspunkt date NOT NULL
);


--
-- Name: samordning_ufore_vurdering_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_ufore_vurdering_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_ufore_vurdering_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_ufore_vurdering_periode_id_seq OWNED BY public.samordning_ufore_vurdering_periode.id;


--
-- Name: samordning_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_vurdering (
    id bigint NOT NULL,
    vurderinger_id bigint NOT NULL,
    ytelse_type character varying(50) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: samordning_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_vurdering_id_seq OWNED BY public.samordning_vurdering.id;


--
-- Name: samordning_vurdering_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_vurdering_periode (
    id bigint NOT NULL,
    periode daterange NOT NULL,
    vurdering_id bigint NOT NULL,
    gradering smallint,
    kronesum numeric(21,0),
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    manuell boolean
);


--
-- Name: samordning_vurdering_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_vurdering_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_vurdering_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_vurdering_periode_id_seq OWNED BY public.samordning_vurdering_periode.id;


--
-- Name: samordning_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    begrunnelse text,
    maksdato_endelig boolean,
    frist_ny_revurdering date,
    vurdert_av text NOT NULL
);


--
-- Name: samordning_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_vurderinger_id_seq OWNED BY public.samordning_vurderinger.id;


--
-- Name: samordning_ytelse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_ytelse (
    id bigint NOT NULL,
    ytelse_type character varying(50) NOT NULL,
    ytelser_id bigint NOT NULL,
    kilde character varying(30) NOT NULL,
    saks_ref character varying(40),
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: samordning_ytelse_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_ytelse_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    opprettet timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    aktiv boolean NOT NULL,
    samordning_ytelse_id bigint NOT NULL
);


--
-- Name: samordning_ytelse_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_ytelse_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_ytelse_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_ytelse_grunnlag_id_seq OWNED BY public.samordning_ytelse_grunnlag.id;


--
-- Name: samordning_ytelse_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_ytelse_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_ytelse_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_ytelse_id_seq OWNED BY public.samordning_ytelse.id;


--
-- Name: samordning_ytelse_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_ytelse_periode (
    id bigint NOT NULL,
    ytelse_id bigint NOT NULL,
    periode daterange NOT NULL,
    gradering smallint,
    kronesum numeric(21,0),
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: samordning_ytelse_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_ytelse_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_ytelse_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_ytelse_periode_id_seq OWNED BY public.samordning_ytelse_periode.id;


--
-- Name: samordning_ytelser; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_ytelser (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: samordning_ytelser_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_ytelser_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_ytelser_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_ytelser_id_seq OWNED BY public.samordning_ytelser.id;


--
-- Name: samordning_ytelsevurdering_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.samordning_ytelsevurdering_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurderinger_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: samordning_ytelsevurdering_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.samordning_ytelsevurdering_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: samordning_ytelsevurdering_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.samordning_ytelsevurdering_grunnlag_id_seq OWNED BY public.samordning_ytelsevurdering_grunnlag.id;


--
-- Name: seq_saksnummer; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.seq_saksnummer
    START WITH 10000000
    INCREMENT BY 50
    MINVALUE 10000000
    NO MAXVALUE
    CACHE 1;


--
-- Name: soning_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.soning_vurdering (
    id bigint NOT NULL,
    soning_vurderinger_id bigint NOT NULL,
    skal_opphore boolean NOT NULL,
    begrunnelse text NOT NULL,
    fra_dato date NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: soning_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.soning_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: soning_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.soning_vurdering_id_seq OWNED BY public.soning_vurdering.id;


--
-- Name: soning_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.soning_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av text NOT NULL
);


--
-- Name: soning_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.soning_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: soning_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.soning_vurderinger_id_seq OWNED BY public.soning_vurderinger.id;


--
-- Name: stans_opphor; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stans_opphor (
    id bigint NOT NULL,
    stans_opphor_set_id bigint,
    fom date NOT NULL,
    vurdering text NOT NULL,
    avslagsaarsaker text[] NOT NULL
);


--
-- Name: stans_opphor_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stans_opphor_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurderinger_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL,
    aktiv boolean NOT NULL,
    stans_opphor_set_id bigint,
    vurderinger_id_v2 bigint
);


--
-- Name: stans_opphor_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stans_opphor_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stans_opphor_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stans_opphor_grunnlag_id_seq OWNED BY public.stans_opphor_grunnlag.id;


--
-- Name: stans_opphor_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stans_opphor_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stans_opphor_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stans_opphor_id_seq OWNED BY public.stans_opphor.id;


--
-- Name: stans_opphor_set; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stans_opphor_set (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: stans_opphor_set_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stans_opphor_set_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stans_opphor_set_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stans_opphor_set_id_seq OWNED BY public.stans_opphor_set.id;


--
-- Name: stans_opphor_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stans_opphor_vurdering (
    id bigint NOT NULL,
    vurderinger_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL,
    vedtaksstatus text NOT NULL,
    fom date NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    vedtakstype text,
    avslagsaarsaker text[] NOT NULL,
    CONSTRAINT stans_opphor_vurdering_check CHECK (((vedtaksstatus <> 'GJELDENDE'::text) OR (vedtakstype = ANY (ARRAY['STANS'::text, 'OPPHØR'::text]))))
);


--
-- Name: stans_opphor_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stans_opphor_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stans_opphor_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stans_opphor_vurdering_id_seq OWNED BY public.stans_opphor_vurdering.id;


--
-- Name: stans_opphor_vurdering_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stans_opphor_vurdering_v2 (
    id bigint NOT NULL,
    vurderinger_id bigint NOT NULL,
    vurdert_tidspunkt timestamp(3) without time zone NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    fom date NOT NULL,
    vurdering text NOT NULL,
    avslagsaarsaker text[]
);


--
-- Name: stans_opphor_vurdering_v2_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stans_opphor_vurdering_v2_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stans_opphor_vurdering_v2_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stans_opphor_vurdering_v2_id_seq OWNED BY public.stans_opphor_vurdering_v2.id;


--
-- Name: stans_opphor_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stans_opphor_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: stans_opphor_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stans_opphor_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stans_opphor_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stans_opphor_vurderinger_id_seq OWNED BY public.stans_opphor_vurderinger.id;


--
-- Name: stans_opphor_vurderinger_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stans_opphor_vurderinger_v2 (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: stans_opphor_vurderinger_v2_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stans_opphor_vurderinger_v2_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stans_opphor_vurderinger_v2_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stans_opphor_vurderinger_v2_id_seq OWNED BY public.stans_opphor_vurderinger_v2.id;


--
-- Name: steg_historikk; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.steg_historikk (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    steg character varying(50) NOT NULL,
    status character varying(50) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: steg_historikk_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.steg_historikk_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: steg_historikk_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.steg_historikk_id_seq OWNED BY public.steg_historikk.id;


--
-- Name: student_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    oppgitt_student_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    student_vurderinger_id bigint
);


--
-- Name: student_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.student_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: student_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.student_grunnlag_id_seq OWNED BY public.student_grunnlag.id;


--
-- Name: student_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_vurdering (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    avbrutt_studie boolean NOT NULL,
    godkjent_studie_av_laanekassen boolean,
    avbrutt_pga_sykdom_eller_skade boolean,
    har_behov_for_behandling boolean,
    avbrutt_dato date,
    avbrudd_mer_enn_6_maaneder boolean,
    vurdert_av character varying(50) NOT NULL,
    vurdert_tidspunkt timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    student_vurderinger_id bigint NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    fom date NOT NULL,
    tom date,
    kodeverk text,
    hoveddiagnose text,
    bidiagnoser text[]
);


--
-- Name: student_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.student_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: student_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.student_vurdering_id_seq OWNED BY public.student_vurdering.id;


--
-- Name: student_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: student_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.student_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: student_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.student_vurderinger_id_seq OWNED BY public.student_vurderinger.id;


--
-- Name: svar_fra_andreinstans_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.svar_fra_andreinstans_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: svar_fra_andreinstans_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.svar_fra_andreinstans_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: svar_fra_andreinstans_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.svar_fra_andreinstans_grunnlag_id_seq OWNED BY public.svar_fra_andreinstans_grunnlag.id;


--
-- Name: svar_fra_andreinstans_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.svar_fra_andreinstans_vurdering (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    konsekvens character varying(20) NOT NULL,
    vilkaar_som_skal_omgjoeres text[] DEFAULT ARRAY[]::text[] NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av character varying(50) NOT NULL
);


--
-- Name: svar_fra_andreinstans_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.svar_fra_andreinstans_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: svar_fra_andreinstans_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.svar_fra_andreinstans_vurdering_id_seq OWNED BY public.svar_fra_andreinstans_vurdering.id;


--
-- Name: sykdom_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sykdom_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    yrkesskade_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    sykdom_vurderinger_id bigint NOT NULL
);


--
-- Name: sykdom_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sykdom_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sykdom_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sykdom_grunnlag_id_seq OWNED BY public.sykdom_grunnlag.id;


--
-- Name: sykdom_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sykdom_vurdering (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    er_arbeidsevne_nedsatt boolean,
    er_sykdom_skade_lyte_vesetling_del boolean,
    er_nedsettelse_mer_enn_halvparten boolean,
    har_sykdom_skade_lyte boolean NOT NULL,
    er_nedsettelse_mer_enn_yrkesskade_grense boolean,
    er_nedsettelse_av_en_viss_varighet boolean,
    yrkesskade_begrunnelse text,
    kodeverk text,
    diagnose text,
    vurderingen_gjelder_fra date NOT NULL,
    sykdom_vurderinger_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av_ident text DEFAULT ''::text NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    vurderingen_gjelder_til date,
    er_nedsettelse_minst_halvparten text,
    er_nedsettelse_mer_enn_yrkesskadegrense text,
    har_nedsatt_arbeidsevne text
);


--
-- Name: sykdom_vurdering_bidiagnoser; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sykdom_vurdering_bidiagnoser (
    id bigint NOT NULL,
    vurdering_id bigint NOT NULL,
    kode character varying(25) NOT NULL
);


--
-- Name: sykdom_vurdering_bidiagnoser_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sykdom_vurdering_bidiagnoser_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sykdom_vurdering_bidiagnoser_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sykdom_vurdering_bidiagnoser_id_seq OWNED BY public.sykdom_vurdering_bidiagnoser.id;


--
-- Name: sykdom_vurdering_brev; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sykdom_vurdering_brev (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    vurdering text,
    vurdert_av text DEFAULT 'Ukjent'::text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: sykdom_vurdering_brev_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sykdom_vurdering_brev_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sykdom_vurdering_brev_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sykdom_vurdering_brev_id_seq OWNED BY public.sykdom_vurdering_brev.id;


--
-- Name: sykdom_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sykdom_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sykdom_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sykdom_vurdering_id_seq OWNED BY public.sykdom_vurdering.id;


--
-- Name: sykdom_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sykdom_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: sykdom_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sykdom_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sykdom_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sykdom_vurderinger_id_seq OWNED BY public.sykdom_vurderinger.id;


--
-- Name: sykepenge_erstatning_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sykepenge_erstatning_grunnlag (
    id integer NOT NULL,
    behandling_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurderinger_id bigint
);


--
-- Name: sykepenge_erstatning_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sykepenge_erstatning_grunnlag_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sykepenge_erstatning_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sykepenge_erstatning_grunnlag_id_seq OWNED BY public.sykepenge_erstatning_grunnlag.id;


--
-- Name: sykepenge_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sykepenge_vurdering (
    id integer NOT NULL,
    begrunnelse text,
    oppfylt boolean NOT NULL,
    grunn character varying(50),
    vurdert_av text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurderinger_id bigint,
    gjelder_fra date NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    gjelder_tom date
);


--
-- Name: sykepenge_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sykepenge_vurdering_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sykepenge_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sykepenge_vurdering_id_seq OWNED BY public.sykepenge_vurdering.id;


--
-- Name: sykepenge_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sykepenge_vurderinger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: sykepenge_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sykepenge_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sykepenge_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sykepenge_vurderinger_id_seq OWNED BY public.sykepenge_vurderinger.id;


--
-- Name: sykestipend_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sykestipend_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    opprettet timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    aktiv boolean DEFAULT true NOT NULL,
    vurdering_id bigint NOT NULL
);


--
-- Name: sykestipend_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sykestipend_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sykestipend_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sykestipend_grunnlag_id_seq OWNED BY public.sykestipend_grunnlag.id;


--
-- Name: sykestipend_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sykestipend_vurdering (
    id bigint NOT NULL,
    begrunnelse text NOT NULL,
    perioder daterange[],
    vurdert_i_behandling bigint NOT NULL,
    vurdert_av_ident text NOT NULL,
    opprettet timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: sykestipend_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sykestipend_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sykestipend_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sykestipend_vurdering_id_seq OWNED BY public.sykestipend_vurdering.id;


--
-- Name: test_automatisk_meldekort_sak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.test_automatisk_meldekort_sak (
    sak_id bigint NOT NULL
);


--
-- Name: tilbakekrevingsbehandling; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tilbakekrevingsbehandling (
    id bigint NOT NULL,
    sak_id bigint NOT NULL,
    tilbakekreving_behandling_id uuid NOT NULL,
    ekstern_fagsak_id character varying(40) NOT NULL,
    hendelse_opprettet timestamp(3) without time zone NOT NULL,
    ekstern_behandling_id character varying(40),
    sak_opprettet timestamp(3) without time zone NOT NULL,
    varsel_sendt date,
    behandlingsstatus character varying(40) NOT NULL,
    totalt_feilutbetalt_belop numeric(9,2) NOT NULL,
    tilbakekreving_saksbehandling_url text NOT NULL,
    fullstendig_periode daterange NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    vente_grunn character varying(40),
    gjenopptas date
);


--
-- Name: tilbakekrevingsbehandling_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tilbakekrevingsbehandling_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tilbakekrevingsbehandling_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tilbakekrevingsbehandling_id_seq OWNED BY public.tilbakekrevingsbehandling.id;


--
-- Name: tilbakekrevingshendelse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tilbakekrevingshendelse (
    id bigint NOT NULL,
    sak_id bigint NOT NULL,
    tilbakekreving_behandling_id uuid NOT NULL,
    ekstern_fagsak_id character varying(40) NOT NULL,
    hendelse_opprettet timestamp(3) without time zone NOT NULL,
    ekstern_behandling_id character varying(40),
    sak_opprettet timestamp(3) without time zone NOT NULL,
    varsel_sendt date,
    behandlingsstatus character varying(40) NOT NULL,
    totalt_feilutbetalt_belop numeric(9,2) NOT NULL,
    tilbakekreving_saksbehandling_url text NOT NULL,
    fullstendig_periode daterange NOT NULL,
    versjon bigint NOT NULL,
    vente_grunn character varying(40),
    gjenopptas date
);


--
-- Name: tilbakekrevingshendelse_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tilbakekrevingshendelse_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tilbakekrevingshendelse_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tilbakekrevingshendelse_id_seq OWNED BY public.tilbakekrevingshendelse.id;


--
-- Name: tilkjent_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tilkjent_periode (
    id bigint NOT NULL,
    periode daterange NOT NULL,
    dagsats numeric(21,0) NOT NULL,
    grunnlag numeric(21,0) NOT NULL,
    gradering smallint NOT NULL,
    grunnbelop numeric(21,0) NOT NULL,
    antall_barn smallint NOT NULL,
    barnetillegg numeric(21,0) NOT NULL,
    grunnlagsfaktor numeric(21,10) NOT NULL,
    barnetilleggsats numeric NOT NULL,
    tilkjent_ytelse_id bigint NOT NULL,
    utbetalingsdato date NOT NULL,
    samordning_gradering smallint,
    institusjon_gradering smallint,
    arbeid_gradering smallint,
    samordning_ufore_gradering smallint,
    samordning_arbeidsgiver_gradering smallint,
    meldeplikt_gradering smallint DEFAULT 0,
    minstesats text,
    redusert_dagsats numeric(21,0),
    barnepensjon_dagsats numeric(21,0) DEFAULT 0 NOT NULL,
    CONSTRAINT tilkjent_periode_meldeplikt_gradering_check CHECK (((0 <= meldeplikt_gradering) AND (meldeplikt_gradering <= 100)))
);


--
-- Name: tilkjent_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tilkjent_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tilkjent_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tilkjent_periode_id_seq OWNED BY public.tilkjent_periode.id;


--
-- Name: tilkjent_ytelse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tilkjent_ytelse (
    id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    behandling_id bigint NOT NULL
);


--
-- Name: tilkjent_ytelse_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tilkjent_ytelse_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tilkjent_ytelse_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tilkjent_ytelse_id_seq OWNED BY public.tilkjent_ytelse.id;


--
-- Name: tilkjent_ytelse_sporing; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tilkjent_ytelse_sporing (
    id bigint NOT NULL,
    tilkjent_ytelse_id bigint NOT NULL,
    versjon text NOT NULL,
    faktagrunnlag text NOT NULL
);


--
-- Name: tilkjent_ytelse_sporing_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tilkjent_ytelse_sporing_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tilkjent_ytelse_sporing_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tilkjent_ytelse_sporing_id_seq OWNED BY public.tilkjent_ytelse_sporing.id;


--
-- Name: tiltakspenger_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tiltakspenger_grunnlag (
    id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    behandling_id bigint NOT NULL,
    perioder_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: tiltakspenger_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tiltakspenger_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tiltakspenger_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tiltakspenger_grunnlag_id_seq OWNED BY public.tiltakspenger_grunnlag.id;


--
-- Name: tiltakspenger_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tiltakspenger_periode (
    id bigint NOT NULL,
    perioder_id bigint NOT NULL,
    periode daterange NOT NULL,
    ytelse_type text NOT NULL,
    kilde text NOT NULL
);


--
-- Name: tiltakspenger_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tiltakspenger_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tiltakspenger_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tiltakspenger_periode_id_seq OWNED BY public.tiltakspenger_periode.id;


--
-- Name: tiltakspenger_perioder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tiltakspenger_perioder (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: tiltakspenger_perioder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tiltakspenger_perioder_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tiltakspenger_perioder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tiltakspenger_perioder_id_seq OWNED BY public.tiltakspenger_perioder.id;


--
-- Name: tjenestepensjon_forhold_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tjenestepensjon_forhold_grunnlag (
    id bigint NOT NULL,
    aktiv boolean NOT NULL,
    behandling_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tjenestepensjon_ordninger_id bigint NOT NULL
);


--
-- Name: tjenestepensjon_forhold_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tjenestepensjon_forhold_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tjenestepensjon_forhold_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tjenestepensjon_forhold_grunnlag_id_seq OWNED BY public.tjenestepensjon_forhold_grunnlag.id;


--
-- Name: tjenestepensjon_ordning; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tjenestepensjon_ordning (
    id bigint NOT NULL,
    tjenestepensjon_ordninger_id bigint NOT NULL,
    navn text NOT NULL,
    tpnr text NOT NULL,
    orgnr text NOT NULL
);


--
-- Name: tjenestepensjon_ordning_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tjenestepensjon_ordning_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tjenestepensjon_ordning_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tjenestepensjon_ordning_id_seq OWNED BY public.tjenestepensjon_ordning.id;


--
-- Name: tjenestepensjon_ordninger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tjenestepensjon_ordninger (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: tjenestepensjon_ordninger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tjenestepensjon_ordninger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tjenestepensjon_ordninger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tjenestepensjon_ordninger_id_seq OWNED BY public.tjenestepensjon_ordninger.id;


--
-- Name: tjenestepensjon_refusjonskrav_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tjenestepensjon_refusjonskrav_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    sak_id bigint NOT NULL,
    refusjonkrav_vurdering_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: tjenestepensjon_refusjonskrav_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tjenestepensjon_refusjonskrav_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tjenestepensjon_refusjonskrav_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tjenestepensjon_refusjonskrav_grunnlag_id_seq OWNED BY public.tjenestepensjon_refusjonskrav_grunnlag.id;


--
-- Name: tjenestepensjon_refusjonskrav_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tjenestepensjon_refusjonskrav_vurdering (
    id bigint NOT NULL,
    har_krav boolean NOT NULL,
    fom date,
    tom date,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    begrunnelse text NOT NULL
);


--
-- Name: tjenestepensjon_refusjonskrav_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tjenestepensjon_refusjonskrav_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tjenestepensjon_refusjonskrav_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tjenestepensjon_refusjonskrav_vurdering_id_seq OWNED BY public.tjenestepensjon_refusjonskrav_vurdering.id;


--
-- Name: tjenestepensjon_ytelse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tjenestepensjon_ytelse (
    id bigint NOT NULL,
    tjenestepensjon_ordning_id bigint NOT NULL,
    extern_id text NOT NULL,
    ytelse_type text NOT NULL,
    innmeldt_fom date,
    iverksatt_fom date NOT NULL,
    iverksatt_tom date
);


--
-- Name: tjenestepensjon_ytelse_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tjenestepensjon_ytelse_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tjenestepensjon_ytelse_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tjenestepensjon_ytelse_id_seq OWNED BY public.tjenestepensjon_ytelse.id;


--
-- Name: trekk_klage_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.trekk_klage_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurdering_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: trekk_klage_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.trekk_klage_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: trekk_klage_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.trekk_klage_grunnlag_id_seq OWNED BY public.trekk_klage_grunnlag.id;


--
-- Name: trekk_klage_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.trekk_klage_vurdering (
    id integer NOT NULL,
    skal_trekkes boolean NOT NULL,
    hvorfor_trekkes character varying(50),
    begrunnelse text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av character varying(50) NOT NULL
);


--
-- Name: trekk_klage_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.trekk_klage_vurdering_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: trekk_klage_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.trekk_klage_vurdering_id_seq OWNED BY public.trekk_klage_vurdering.id;


--
-- Name: trukket_soknad_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.trukket_soknad_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vurderinger_id bigint NOT NULL,
    opprettet timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    aktiv boolean DEFAULT true NOT NULL
);


--
-- Name: trukket_soknad_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.trukket_soknad_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: trukket_soknad_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.trukket_soknad_grunnlag_id_seq OWNED BY public.trukket_soknad_grunnlag.id;


--
-- Name: trukket_soknad_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.trukket_soknad_vurdering (
    id bigint NOT NULL,
    vurderinger_id bigint NOT NULL,
    begrunnelse text NOT NULL,
    journalpost_id text NOT NULL,
    vurdert_av text NOT NULL,
    vurdert timestamp(3) without time zone NOT NULL,
    opprettet timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    skal_trekkes boolean NOT NULL
);


--
-- Name: trukket_soknad_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.trukket_soknad_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: trukket_soknad_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.trukket_soknad_vurdering_id_seq OWNED BY public.trukket_soknad_vurdering.id;


--
-- Name: trukket_soknad_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.trukket_soknad_vurderinger (
    id bigint NOT NULL,
    opprettet timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL
);


--
-- Name: trukket_soknad_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.trukket_soknad_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: trukket_soknad_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.trukket_soknad_vurderinger_id_seq OWNED BY public.trukket_soknad_vurderinger.id;


--
-- Name: ufore; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ufore (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: ufore_gradering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ufore_gradering (
    id bigint NOT NULL,
    ufore_id bigint NOT NULL,
    uforegrad smallint NOT NULL,
    virkningstidspunkt date NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    uforegrad_tom date,
    uforegrad_fom date
);


--
-- Name: ufore_gradering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ufore_gradering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ufore_gradering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ufore_gradering_id_seq OWNED BY public.ufore_gradering.id;


--
-- Name: ufore_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ufore_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    ufore_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: ufore_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ufore_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ufore_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ufore_grunnlag_id_seq OWNED BY public.ufore_grunnlag.id;


--
-- Name: ufore_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ufore_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ufore_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ufore_id_seq OWNED BY public.ufore.id;


--
-- Name: ufore_soknad_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ufore_soknad_grunnlag (
    id bigint NOT NULL,
    ufore_sak_id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    soknadsdato date NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    aktiv boolean DEFAULT true NOT NULL
);


--
-- Name: ufore_soknad_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ufore_soknad_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ufore_soknad_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ufore_soknad_grunnlag_id_seq OWNED BY public.ufore_soknad_grunnlag.id;


--
-- Name: underveis_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.underveis_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    perioder_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    sporing_id bigint
);


--
-- Name: underveis_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.underveis_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: underveis_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.underveis_grunnlag_id_seq OWNED BY public.underveis_grunnlag.id;


--
-- Name: underveis_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.underveis_periode (
    id bigint NOT NULL,
    perioder_id bigint NOT NULL,
    periode daterange NOT NULL,
    meldeperiode daterange,
    timer_arbeid numeric(5,1) DEFAULT 0 NOT NULL,
    utfall character varying(50) NOT NULL,
    avslagsarsak character varying(50),
    grenseverdi smallint NOT NULL,
    gradering smallint DEFAULT 0 NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    trekk_dagsatser integer NOT NULL,
    andel_arbeidsevne smallint DEFAULT 0 NOT NULL,
    bruker_av_kvoter text[] NOT NULL,
    brudd_aktivitetsplikt_id bigint,
    rettighetstype text,
    institusjonsoppholdreduksjon integer NOT NULL,
    meldeplikt_status text,
    meldekort_mottatt date,
    meldeplikt_gradering smallint DEFAULT 0,
    CONSTRAINT underveis_periode_meldeplikt_gradering_check CHECK (((0 <= meldeplikt_gradering) AND (meldeplikt_gradering <= 100)))
);


--
-- Name: underveis_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.underveis_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: underveis_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.underveis_periode_id_seq OWNED BY public.underveis_periode.id;


--
-- Name: underveis_perioder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.underveis_perioder (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: underveis_perioder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.underveis_perioder_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: underveis_perioder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.underveis_perioder_id_seq OWNED BY public.underveis_perioder.id;


--
-- Name: underveis_sporing; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.underveis_sporing (
    id bigint NOT NULL,
    faktagrunnlag text,
    versjon character varying(100) NOT NULL
);


--
-- Name: underveis_sporing_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.underveis_sporing_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: underveis_sporing_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.underveis_sporing_id_seq OWNED BY public.underveis_sporing.id;


--
-- Name: utenlands_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.utenlands_periode (
    id bigint NOT NULL,
    land character varying(50),
    til_dato date,
    fra_dato date,
    i_arbeid boolean NOT NULL,
    utenlands_id text,
    oppgitt_utenlandsopphold_id bigint,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: utenlands_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.utenlands_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: utenlands_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.utenlands_periode_id_seq OWNED BY public.utenlands_periode.id;


--
-- Name: vedtak_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vedtak_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vedtak_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vedtak_id_seq OWNED BY public.vedtak.id;


--
-- Name: vedtakslengde_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vedtakslengde_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    opprettet timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    aktiv boolean DEFAULT true NOT NULL,
    vurderinger_id bigint NOT NULL
);


--
-- Name: vedtakslengde_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vedtakslengde_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vedtakslengde_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vedtakslengde_grunnlag_id_seq OWNED BY public.vedtakslengde_grunnlag.id;


--
-- Name: vedtakslengde_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vedtakslengde_vurdering (
    id bigint NOT NULL,
    sluttdato date NOT NULL,
    utvidet_med text NOT NULL,
    vurdert_av text NOT NULL,
    vurdert_i_behandling bigint NOT NULL,
    opprettet timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    vurderinger_id bigint NOT NULL,
    begrunnelse text NOT NULL
);


--
-- Name: vedtakslengde_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vedtakslengde_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vedtakslengde_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vedtakslengde_vurdering_id_seq OWNED BY public.vedtakslengde_vurdering.id;


--
-- Name: vedtakslengde_vurderinger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vedtakslengde_vurderinger (
    id bigint NOT NULL,
    opprettet timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: vedtakslengde_vurderinger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vedtakslengde_vurderinger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vedtakslengde_vurderinger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vedtakslengde_vurderinger_id_seq OWNED BY public.vedtakslengde_vurderinger.id;


--
-- Name: vilkar; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vilkar (
    id bigint NOT NULL,
    type character varying(50) NOT NULL,
    resultat_id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: vilkar_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vilkar_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vilkar_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vilkar_id_seq OWNED BY public.vilkar.id;


--
-- Name: vilkar_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vilkar_periode (
    id bigint NOT NULL,
    vilkar_id bigint NOT NULL,
    periode daterange NOT NULL,
    utfall character varying(50) NOT NULL,
    manuell_vurdering boolean NOT NULL,
    begrunnelse text,
    innvilgelsesarsak character varying(100),
    avslagsarsak character varying(100),
    faktagrunnlag text,
    versjon character varying(100) NOT NULL
);


--
-- Name: vilkar_periode_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vilkar_periode_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vilkar_periode_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vilkar_periode_id_seq OWNED BY public.vilkar_periode.id;


--
-- Name: vilkar_resultat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vilkar_resultat (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL
);


--
-- Name: vilkar_resultat_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vilkar_resultat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vilkar_resultat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vilkar_resultat_id_seq OWNED BY public.vilkar_resultat.id;


--
-- Name: yrkesskade; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.yrkesskade (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: yrkesskade_dato; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.yrkesskade_dato (
    id bigint NOT NULL,
    yrkesskade_id bigint NOT NULL,
    referanse text NOT NULL,
    skadedato date,
    yrkesskade_saksnummer integer,
    kildesystem character varying(50),
    vedtaksdato date,
    skadeart text,
    diagnose text,
    skadekombinasjoner text,
    skadekombinasjoner_tekst text,
    opprettet_tid timestamp(3) without time zone
);


--
-- Name: yrkesskade_dato_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.yrkesskade_dato_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: yrkesskade_dato_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.yrkesskade_dato_id_seq OWNED BY public.yrkesskade_dato.id;


--
-- Name: yrkesskade_grunnlag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.yrkesskade_grunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    yrkesskade_id bigint,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    oppgitt_yrkesskade_i_soknad boolean
);


--
-- Name: yrkesskade_grunnlag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.yrkesskade_grunnlag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: yrkesskade_grunnlag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.yrkesskade_grunnlag_id_seq OWNED BY public.yrkesskade_grunnlag.id;


--
-- Name: yrkesskade_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.yrkesskade_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: yrkesskade_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.yrkesskade_id_seq OWNED BY public.yrkesskade.id;


--
-- Name: yrkesskade_inntekt; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.yrkesskade_inntekt (
    id bigint NOT NULL,
    inntekter_id bigint NOT NULL,
    referanse character varying(50) NOT NULL,
    begrunnelse text NOT NULL,
    antatt_arlig_inntekt numeric(19,2) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av text NOT NULL
);


--
-- Name: yrkesskade_inntekt_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.yrkesskade_inntekt_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: yrkesskade_inntekt_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.yrkesskade_inntekt_id_seq OWNED BY public.yrkesskade_inntekt.id;


--
-- Name: yrkesskade_inntekter; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.yrkesskade_inntekter (
    id bigint NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: yrkesskade_inntekter_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.yrkesskade_inntekter_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: yrkesskade_inntekter_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.yrkesskade_inntekter_id_seq OWNED BY public.yrkesskade_inntekter.id;


--
-- Name: yrkesskade_relaterte_saker; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.yrkesskade_relaterte_saker (
    id bigint NOT NULL,
    vurdering_id bigint NOT NULL,
    referanse character varying(50) NOT NULL,
    manuell_yrkesskade_dato date
);


--
-- Name: yrkesskade_relaterte_saker_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.yrkesskade_relaterte_saker_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: yrkesskade_relaterte_saker_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.yrkesskade_relaterte_saker_id_seq OWNED BY public.yrkesskade_relaterte_saker.id;


--
-- Name: yrkesskade_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.yrkesskade_vurdering (
    id bigint NOT NULL,
    begrunnelse text,
    arsakssammenheng boolean NOT NULL,
    andel_av_nedsettelse smallint,
    vurdert_av text NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: yrkesskade_vurdering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.yrkesskade_vurdering_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: yrkesskade_vurdering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.yrkesskade_vurdering_id_seq OWNED BY public.yrkesskade_vurdering.id;


--
-- Name: aktivitetsplikt_11_7_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_7_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.aktivitetsplikt_11_7_grunnlag_id_seq'::regclass);


--
-- Name: aktivitetsplikt_11_7_varsel id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_7_varsel ALTER COLUMN id SET DEFAULT nextval('public.aktivitetsplikt_11_7_varsel_id_seq'::regclass);


--
-- Name: aktivitetsplikt_11_7_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_7_vurdering ALTER COLUMN id SET DEFAULT nextval('public.aktivitetsplikt_11_7_vurdering_id_seq'::regclass);


--
-- Name: aktivitetsplikt_11_7_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_7_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.aktivitetsplikt_11_7_vurderinger_id_seq'::regclass);


--
-- Name: aktivitetsplikt_11_9_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_9_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.aktivitetsplikt_11_9_grunnlag_id_seq'::regclass);


--
-- Name: aktivitetsplikt_11_9_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_9_vurdering ALTER COLUMN id SET DEFAULT nextval('public.aktivitetsplikt_11_9_vurdering_id_seq'::regclass);


--
-- Name: aktivitetsplikt_11_9_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_9_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.aktivitetsplikt_11_9_vurderinger_id_seq'::regclass);


--
-- Name: andre_ytelser_oppgitt_i_soknad_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.andre_ytelser_oppgitt_i_soknad_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.andre_ytelser_oppgitt_i_soknad_grunnlag_id_seq'::regclass);


--
-- Name: andre_ytelser_svar_i_soknad id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.andre_ytelser_svar_i_soknad ALTER COLUMN id SET DEFAULT nextval('public.andre_ytelser_svar_i_soknad_id_seq'::regclass);


--
-- Name: annen_ytelse_oppgitt_i_soknad id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.annen_ytelse_oppgitt_i_soknad ALTER COLUMN id SET DEFAULT nextval('public.annen_ytelse_oppgitt_i_soknad_id_seq'::regclass);


--
-- Name: arbeid id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeid ALTER COLUMN id SET DEFAULT nextval('public.arbeid_id_seq'::regclass);


--
-- Name: arbeid_detaljer id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeid_detaljer ALTER COLUMN id SET DEFAULT nextval('public.arbeid_detaljer_id_seq'::regclass);


--
-- Name: arbeid_forutgaaende id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeid_forutgaaende ALTER COLUMN id SET DEFAULT nextval('public.arbeid_forutgaaende_id_seq'::regclass);


--
-- Name: arbeider id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeider ALTER COLUMN id SET DEFAULT nextval('public.arbeider_id_seq'::regclass);


--
-- Name: arbeider_forutgaaende id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeider_forutgaaende ALTER COLUMN id SET DEFAULT nextval('public.arbeider_forutgaaende_id_seq'::regclass);


--
-- Name: arbeidsevne id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsevne ALTER COLUMN id SET DEFAULT nextval('public.arbeidsevne_id_seq'::regclass);


--
-- Name: arbeidsevne_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsevne_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.arbeidsevne_grunnlag_id_seq'::regclass);


--
-- Name: arbeidsevne_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsevne_vurdering ALTER COLUMN id SET DEFAULT nextval('public.arbeidsevne_vurdering_id_seq'::regclass);


--
-- Name: arbeidsopptrapping_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsopptrapping_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.arbeidsopptrapping_grunnlag_id_seq'::regclass);


--
-- Name: arbeidsopptrapping_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsopptrapping_vurdering ALTER COLUMN id SET DEFAULT nextval('public.arbeidsopptrapping_vurdering_id_seq'::regclass);


--
-- Name: arbeidsopptrapping_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsopptrapping_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.arbeidsopptrapping_vurderinger_id_seq'::regclass);


--
-- Name: avbryt_aktivitetspliktbehandling_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avbryt_aktivitetspliktbehandling_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.avbryt_aktivitetspliktbehandling_grunnlag_id_seq'::regclass);


--
-- Name: avbryt_aktivitetspliktbehandling_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avbryt_aktivitetspliktbehandling_vurdering ALTER COLUMN id SET DEFAULT nextval('public.avbryt_aktivitetspliktbehandling_vurdering_id_seq'::regclass);


--
-- Name: avbryt_revurdering_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avbryt_revurdering_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.kanseller_revurdering_grunnlag_id_seq'::regclass);


--
-- Name: avbryt_revurdering_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avbryt_revurdering_vurdering ALTER COLUMN id SET DEFAULT nextval('public.kanseller_revurdering_vurdering_id_seq'::regclass);


--
-- Name: avklaringsbehov id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avklaringsbehov ALTER COLUMN id SET DEFAULT nextval('public.avklaringsbehov_id_seq'::regclass);


--
-- Name: avklaringsbehov_endring id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avklaringsbehov_endring ALTER COLUMN id SET DEFAULT nextval('public.avklaringsbehov_endring_id_seq'::regclass);


--
-- Name: avklaringsbehov_endring_aarsak id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avklaringsbehov_endring_aarsak ALTER COLUMN id SET DEFAULT nextval('public.avklaringsbehov_endring_aarsak_id_seq'::regclass);


--
-- Name: avvist_formkrav_varsel id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avvist_formkrav_varsel ALTER COLUMN id SET DEFAULT nextval('public.avvist_formkrav_varsel_id_seq'::regclass);


--
-- Name: barn_saksbehandler_oppgitt id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_saksbehandler_oppgitt ALTER COLUMN id SET DEFAULT nextval('public.barn_saksbehandler_oppgitt_id_seq'::regclass);


--
-- Name: barn_saksbehandler_oppgitt_barnopplysning id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_saksbehandler_oppgitt_barnopplysning ALTER COLUMN id SET DEFAULT nextval('public.barn_saksbehandler_oppgitt_barnopplysning_id_seq'::regclass);


--
-- Name: barn_tillegg id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_tillegg ALTER COLUMN id SET DEFAULT nextval('public.barn_tillegg_id_seq'::regclass);


--
-- Name: barn_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_vurdering ALTER COLUMN id SET DEFAULT nextval('public.barn_vurdering_id_seq'::regclass);


--
-- Name: barn_vurdering_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_vurdering_periode ALTER COLUMN id SET DEFAULT nextval('public.barn_vurdering_periode_id_seq'::regclass);


--
-- Name: barn_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.barn_vurderinger_id_seq'::regclass);


--
-- Name: barnetillegg_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnetillegg_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.barnetillegg_grunnlag_id_seq'::regclass);


--
-- Name: barnetillegg_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnetillegg_periode ALTER COLUMN id SET DEFAULT nextval('public.barnetillegg_periode_id_seq'::regclass);


--
-- Name: barnetillegg_perioder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnetillegg_perioder ALTER COLUMN id SET DEFAULT nextval('public.barnetillegg_perioder_id_seq'::regclass);


--
-- Name: barnopplysning id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnopplysning ALTER COLUMN id SET DEFAULT nextval('public.barnopplysning_id_seq'::regclass);


--
-- Name: barnopplysning_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnopplysning_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.barnopplysning_grunnlag_id_seq'::regclass);


--
-- Name: barnopplysning_grunnlag_barnopplysning id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnopplysning_grunnlag_barnopplysning ALTER COLUMN id SET DEFAULT nextval('public.barnopplysning_grunnlag_barnopplysning_id_seq'::regclass);


--
-- Name: behandlende_enhet_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandlende_enhet_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.behandlende_enhet_grunnlag_id_seq'::regclass);


--
-- Name: behandlende_enhet_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandlende_enhet_vurdering ALTER COLUMN id SET DEFAULT nextval('public.behandlende_enhet_vurdering_id_seq'::regclass);


--
-- Name: behandling id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling ALTER COLUMN id SET DEFAULT nextval('public.behandling_id_seq'::regclass);


--
-- Name: behandling_aarsak id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling_aarsak ALTER COLUMN id SET DEFAULT nextval('public.behandling_aarsak_id_seq'::regclass);


--
-- Name: beregning id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning ALTER COLUMN id SET DEFAULT nextval('public.beregning_id_seq'::regclass);


--
-- Name: beregning_hoved id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_hoved ALTER COLUMN id SET DEFAULT nextval('public.beregning_hoved_id_seq'::regclass);


--
-- Name: beregning_inntekt id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_inntekt ALTER COLUMN id SET DEFAULT nextval('public.beregning_inntekt_id_seq'::regclass);


--
-- Name: beregning_ufore id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore ALTER COLUMN id SET DEFAULT nextval('public.beregning_ufore_id_seq'::regclass);


--
-- Name: beregning_ufore_inntekt id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore_inntekt ALTER COLUMN id SET DEFAULT nextval('public.beregning_ufore_inntekt_id_seq'::regclass);


--
-- Name: beregning_ufore_tidsperiode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore_tidsperiode ALTER COLUMN id SET DEFAULT nextval('public.beregning_ufore_tidsperiode_id_seq'::regclass);


--
-- Name: beregning_ufore_uforegrader id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore_uforegrader ALTER COLUMN id SET DEFAULT nextval('public.beregning_ufore_uforegrader_id_seq'::regclass);


--
-- Name: beregning_yrkesskade id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_yrkesskade ALTER COLUMN id SET DEFAULT nextval('public.beregning_yrkesskade_id_seq'::regclass);


--
-- Name: beregningsfakta_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregningsfakta_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.beregningstidspunkt_grunnlag_id_seq'::regclass);


--
-- Name: beregningsgrunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregningsgrunnlag ALTER COLUMN id SET DEFAULT nextval('public.beregningsgrunnlag_id_seq'::regclass);


--
-- Name: beregningstidspunkt_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregningstidspunkt_vurdering ALTER COLUMN id SET DEFAULT nextval('public.beregningstidspunkt_vurdering_id_seq'::regclass);


--
-- Name: bistand id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bistand ALTER COLUMN id SET DEFAULT nextval('public.bistand_id_seq'::regclass);


--
-- Name: bistand_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bistand_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.bistand_grunnlag_id_seq'::regclass);


--
-- Name: bistand_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bistand_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.bistand_vurderinger_id_seq'::regclass);


--
-- Name: brevbestilling id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brevbestilling ALTER COLUMN id SET DEFAULT nextval('public.brevbestilling_id_seq'::regclass);


--
-- Name: brudd_aktivitetsplikt id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brudd_aktivitetsplikt ALTER COLUMN id SET DEFAULT nextval('public.brudd_aktivitetsplikt_id_seq'::regclass);


--
-- Name: brudd_aktivitetsplikt_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brudd_aktivitetsplikt_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.brudd_aktivitetsplikt_grunnlag_id_seq'::regclass);


--
-- Name: brudd_aktivitetsplikter id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brudd_aktivitetsplikter ALTER COLUMN id SET DEFAULT nextval('public.brudd_aktivitetsplikter_id_seq'::regclass);


--
-- Name: bruker_land id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_land ALTER COLUMN id SET DEFAULT nextval('public.bruker_land_id_seq'::regclass);


--
-- Name: bruker_land_aggregat id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_land_aggregat ALTER COLUMN id SET DEFAULT nextval('public.bruker_land_aggregat_id_seq'::regclass);


--
-- Name: bruker_land_forutgaaende id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_land_forutgaaende ALTER COLUMN id SET DEFAULT nextval('public.bruker_land_forutgaaende_id_seq'::regclass);


--
-- Name: bruker_land_forutgaaende_aggregat id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_land_forutgaaende_aggregat ALTER COLUMN id SET DEFAULT nextval('public.bruker_land_forutgaaende_aggregat_id_seq'::regclass);


--
-- Name: bruker_personopplysning id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_personopplysning ALTER COLUMN id SET DEFAULT nextval('public.personopplysning_id_seq'::regclass);


--
-- Name: bruker_personopplysning_forutgaaende id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_personopplysning_forutgaaende ALTER COLUMN id SET DEFAULT nextval('public.bruker_personopplysning_forutgaaende_id_seq'::regclass);


--
-- Name: bruker_statuser_forutgaaende id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_statuser_forutgaaende ALTER COLUMN id SET DEFAULT nextval('public.bruker_statuser_forutgaaende_id_seq'::regclass);


--
-- Name: bruker_statuser_forutgaaende_aggregat id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_statuser_forutgaaende_aggregat ALTER COLUMN id SET DEFAULT nextval('public.bruker_statuser_forutgaaende_aggregat_id_seq'::regclass);


--
-- Name: bruker_utenlandsadresse id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_utenlandsadresse ALTER COLUMN id SET DEFAULT nextval('public.bruker_utenlandsadresse_id_seq'::regclass);


--
-- Name: bruker_utenlandsadresse_forutgaaende id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_utenlandsadresse_forutgaaende ALTER COLUMN id SET DEFAULT nextval('public.bruker_utenlandsadresse_forutgaaende_id_seq'::regclass);


--
-- Name: bruker_utenlandsadresser_aggregat id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_utenlandsadresser_aggregat ALTER COLUMN id SET DEFAULT nextval('public.bruker_utenlandsadresser_aggregat_id_seq'::regclass);


--
-- Name: bruker_utenlandsadresser_forutgaaende_aggregat id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_utenlandsadresser_forutgaaende_aggregat ALTER COLUMN id SET DEFAULT nextval('public.bruker_utenlandsadresser_forutgaaende_aggregat_id_seq'::regclass);


--
-- Name: dagpenger_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dagpenger_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.dagpenger_grunnlag_id_seq'::regclass);


--
-- Name: dagpenger_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dagpenger_periode ALTER COLUMN id SET DEFAULT nextval('public.dagpenger_periode_id_seq'::regclass);


--
-- Name: dagpenger_perioder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dagpenger_perioder ALTER COLUMN id SET DEFAULT nextval('public.dagpenger_perioder_id_seq'::regclass);


--
-- Name: egen_virksomhet_oppstart_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.egen_virksomhet_oppstart_periode ALTER COLUMN id SET DEFAULT nextval('public.egen_virksomhet_oppstart_periode_id_seq'::regclass);


--
-- Name: egen_virksomhet_oppstart_perioder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.egen_virksomhet_oppstart_perioder ALTER COLUMN id SET DEFAULT nextval('public.egen_virksomhet_oppstart_perioder_id_seq'::regclass);


--
-- Name: egen_virksomhet_utvikling_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.egen_virksomhet_utvikling_periode ALTER COLUMN id SET DEFAULT nextval('public.egen_virksomhet_utvikling_periode_id_seq'::regclass);


--
-- Name: egen_virksomhet_utvikling_perioder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.egen_virksomhet_utvikling_perioder ALTER COLUMN id SET DEFAULT nextval('public.egen_virksomhet_utvikling_perioder_id_seq'::regclass);


--
-- Name: etablering_egen_virksomhet_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etablering_egen_virksomhet_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.etablering_egen_virksomhet_grunnlag_id_seq'::regclass);


--
-- Name: etablering_egen_virksomhet_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etablering_egen_virksomhet_vurdering ALTER COLUMN id SET DEFAULT nextval('public.etablering_egen_virksomhet_vurdering_id_seq'::regclass);


--
-- Name: etablering_egen_virksomhet_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etablering_egen_virksomhet_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.etablering_egen_virksomhet_vurderinger_id_seq'::regclass);


--
-- Name: formkrav_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.formkrav_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.formkrav_grunnlag_id_seq'::regclass);


--
-- Name: formkrav_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.formkrav_vurdering ALTER COLUMN id SET DEFAULT nextval('public.formkrav_vurdering_id_seq'::regclass);


--
-- Name: forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnl_id_seq'::regclass);


--
-- Name: forutgaaende_medlemskap_manuell_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forutgaaende_medlemskap_manuell_vurdering ALTER COLUMN id SET DEFAULT nextval('public.forutgaaende_medlemskap_manuell_vurdering_id_seq'::regclass);


--
-- Name: forutgaaende_medlemskap_manuell_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forutgaaende_medlemskap_manuell_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.forutgaaende_medlemskap_manuell_vurderinger_id_seq'::regclass);


--
-- Name: fullmektig_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fullmektig_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.fullmektig_grunnlag_id_seq'::regclass);


--
-- Name: fullmektig_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fullmektig_vurdering ALTER COLUMN id SET DEFAULT nextval('public.fullmektig_vurdering_id_seq'::regclass);


--
-- Name: helseopphold_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.helseopphold_vurdering ALTER COLUMN id SET DEFAULT nextval('public.helseopphold_vurdering_id_seq'::regclass);


--
-- Name: helseopphold_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.helseopphold_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.helseopphold_vurderinger_id_seq'::regclass);


--
-- Name: informasjonskrav_oppdatert id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.informasjonskrav_oppdatert ALTER COLUMN id SET DEFAULT nextval('public.informasjonskrav_oppdatert_id_seq'::regclass);


--
-- Name: inntekt id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt ALTER COLUMN id SET DEFAULT nextval('public.inntekt_id_seq'::regclass);


--
-- Name: inntekt_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.inntekt_grunnlag_id_seq'::regclass);


--
-- Name: inntekt_i_norge id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt_i_norge ALTER COLUMN id SET DEFAULT nextval('public.inntekt_i_norge_id_seq'::regclass);


--
-- Name: inntekt_i_norge_forutgaaende id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt_i_norge_forutgaaende ALTER COLUMN id SET DEFAULT nextval('public.inntekt_i_norge_forutgaaende_id_seq'::regclass);


--
-- Name: inntekt_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt_periode ALTER COLUMN id SET DEFAULT nextval('public.inntekt_periode_id_seq'::regclass);


--
-- Name: inntekter id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekter ALTER COLUMN id SET DEFAULT nextval('public.inntekter_id_seq'::regclass);


--
-- Name: inntekter_i_norge id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekter_i_norge ALTER COLUMN id SET DEFAULT nextval('public.inntekter_i_norge_id_seq'::regclass);


--
-- Name: inntekter_i_norge_forutgaaende id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekter_i_norge_forutgaaende ALTER COLUMN id SET DEFAULT nextval('public.inntekter_i_norge_forutgaaende_id_seq'::regclass);


--
-- Name: inntektsbortfall_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntektsbortfall_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.inntektsbortfall_grunnlag_id_seq'::regclass);


--
-- Name: inntektsbortfall_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntektsbortfall_vurdering ALTER COLUMN id SET DEFAULT nextval('public.inntektsbortfall_vurdering_id_seq'::regclass);


--
-- Name: inntektsbortfall_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntektsbortfall_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.inntektsbortfall_vurderinger_id_seq'::regclass);


--
-- Name: jobb id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobb ALTER COLUMN id SET DEFAULT nextval('public.oppgave_id_seq'::regclass);


--
-- Name: jobb_historikk id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobb_historikk ALTER COLUMN id SET DEFAULT nextval('public.oppgave_historikk_id_seq'::regclass);


--
-- Name: klage_kontor_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.klage_kontor_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.klage_kontor_grunnlag_id_seq'::regclass);


--
-- Name: klage_kontor_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.klage_kontor_vurdering ALTER COLUMN id SET DEFAULT nextval('public.klage_kontor_vurdering_id_seq'::regclass);


--
-- Name: klage_nay_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.klage_nay_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.klage_nay_grunnlag_id_seq'::regclass);


--
-- Name: klage_nay_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.klage_nay_vurdering ALTER COLUMN id SET DEFAULT nextval('public.klage_nay_vurdering_id_seq'::regclass);


--
-- Name: krav_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.krav_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.krav_grunnlag_id_seq'::regclass);


--
-- Name: krav_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.krav_vurdering ALTER COLUMN id SET DEFAULT nextval('public.krav_vurdering_id_seq'::regclass);


--
-- Name: krav_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.krav_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.krav_vurderinger_id_seq'::regclass);


--
-- Name: lovvalg_medlemskap_manuell_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lovvalg_medlemskap_manuell_vurdering ALTER COLUMN id SET DEFAULT nextval('public.lovvalg_medlemskap_manuell_vurdering_id_seq'::regclass);


--
-- Name: lovvalg_medlemskap_manuell_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lovvalg_medlemskap_manuell_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.lovvalg_medlemskap_manuell_vurderinger_id_seq'::regclass);


--
-- Name: manuell_inntekt_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manuell_inntekt_vurdering ALTER COLUMN id SET DEFAULT nextval('public.manuell_inntekt_vurdering_id_seq'::regclass);


--
-- Name: manuell_inntekt_vurdering_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manuell_inntekt_vurdering_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.manuell_inntekt_vurdering_grunnlag_id_seq'::regclass);


--
-- Name: manuell_inntekt_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manuell_inntekt_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.manuell_inntekt_vurderinger_id_seq'::regclass);


--
-- Name: medlemskap_arbeid_og_inntekt_i_norge_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_arbeid_og_inntekt_i_norge_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.medlemskap_arbeid_og_inntekt_i_norge_grunnlag_id_seq'::regclass);


--
-- Name: medlemskap_forutgaaende_unntak id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_forutgaaende_unntak ALTER COLUMN id SET DEFAULT nextval('public.medlemskap_forutgaaende_unntak_id_seq'::regclass);


--
-- Name: medlemskap_forutgaaende_unntak_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_forutgaaende_unntak_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.medlemskap_forutgaaende_unntak_grunnlag_id_seq'::regclass);


--
-- Name: medlemskap_forutgaaende_unntak_person id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_forutgaaende_unntak_person ALTER COLUMN id SET DEFAULT nextval('public.medlemskap_forutgaaende_unntak_person_id_seq'::regclass);


--
-- Name: medlemskap_unntak id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_unntak ALTER COLUMN id SET DEFAULT nextval('public.medlemskap_unntak_id_seq'::regclass);


--
-- Name: medlemskap_unntak_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_unntak_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.medlemskap_unntak_grunnlag_id_seq'::regclass);


--
-- Name: medlemskap_unntak_person id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_unntak_person ALTER COLUMN id SET DEFAULT nextval('public.medlemskap_unntak_person_id_seq'::regclass);


--
-- Name: meldekort id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldekort ALTER COLUMN id SET DEFAULT nextval('public.meldekort_id_seq'::regclass);


--
-- Name: meldekort_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldekort_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.meldekort_grunnlag_id_seq'::regclass);


--
-- Name: meldekort_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldekort_periode ALTER COLUMN id SET DEFAULT nextval('public.meldekort_periode_id_seq'::regclass);


--
-- Name: meldekortene id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldekortene ALTER COLUMN id SET DEFAULT nextval('public.meldekortene_id_seq'::regclass);


--
-- Name: meldeperiode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeperiode ALTER COLUMN id SET DEFAULT nextval('public.meldeperiode_id_seq'::regclass);


--
-- Name: meldeperiode_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeperiode_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.meldeperiode_grunnlag_id_seq'::regclass);


--
-- Name: meldeplikt_fritak id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_fritak ALTER COLUMN id SET DEFAULT nextval('public.meldeplikt_fritak_id_seq'::regclass);


--
-- Name: meldeplikt_fritak_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_fritak_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.meldeplikt_fritak_grunnlag_id_seq'::regclass);


--
-- Name: meldeplikt_fritak_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_fritak_vurdering ALTER COLUMN id SET DEFAULT nextval('public.meldeplikt_fritak_vurdering_id_seq'::regclass);


--
-- Name: meldeplikt_overstyring_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_overstyring_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.meldeplikt_overstyring_grunnlag_id_seq'::regclass);


--
-- Name: meldeplikt_overstyring_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_overstyring_vurdering ALTER COLUMN id SET DEFAULT nextval('public.meldeplikt_overstyring_vurdering_id_seq'::regclass);


--
-- Name: meldeplikt_overstyring_vurdering_perioder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_overstyring_vurdering_perioder ALTER COLUMN id SET DEFAULT nextval('public.meldeplikt_overstyring_vurdering_perioder_id_seq'::regclass);


--
-- Name: meldeplikt_overstyring_vurderinger grunnlag_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_overstyring_vurderinger ALTER COLUMN grunnlag_id SET DEFAULT nextval('public.meldeplikt_overstyring_vurderinger_grunnlag_id_seq'::regclass);


--
-- Name: meldeplikt_overstyring_vurderinger vurdering_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_overstyring_vurderinger ALTER COLUMN vurdering_id SET DEFAULT nextval('public.meldeplikt_overstyring_vurderinger_vurdering_id_seq'::regclass);


--
-- Name: mellomlagret_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mellomlagret_vurdering ALTER COLUMN id SET DEFAULT nextval('public.mellomlagret_vurdering_id_seq'::regclass);


--
-- Name: mottatt_dokument id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mottatt_dokument ALTER COLUMN id SET DEFAULT nextval('public.mottatt_dokument_id_seq'::regclass);


--
-- Name: oppfolgingsoppgave_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppfolgingsoppgave_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.oppfolgingsoppgave_grunnlag_id_seq'::regclass);


--
-- Name: oppfolgingsoppgave_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppfolgingsoppgave_vurdering ALTER COLUMN id SET DEFAULT nextval('public.oppfolgingsoppgave_vurdering_id_seq'::regclass);


--
-- Name: oppgitt_barn id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgitt_barn ALTER COLUMN id SET DEFAULT nextval('public.oppgitt_barn_id_seq'::regclass);


--
-- Name: oppgitt_barnopplysning id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgitt_barnopplysning ALTER COLUMN id SET DEFAULT nextval('public.oppgitt_barnopplysning_id_seq'::regclass);


--
-- Name: oppgitt_student id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgitt_student ALTER COLUMN id SET DEFAULT nextval('public.oppgitt_student_id_seq'::regclass);


--
-- Name: oppgitt_utenlandsopphold id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgitt_utenlandsopphold ALTER COLUMN id SET DEFAULT nextval('public.oppgitt_utenlandsopphold_id_seq'::regclass);


--
-- Name: oppgitt_utenlandsopphold_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgitt_utenlandsopphold_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.oppgitt_utenlandsopphold_grunnlag_id_seq'::regclass);


--
-- Name: opphold id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opphold ALTER COLUMN id SET DEFAULT nextval('public.opphold_id_seq'::regclass);


--
-- Name: opphold_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opphold_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.opphold_grunnlag_id_seq'::regclass);


--
-- Name: opphold_person id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opphold_person ALTER COLUMN id SET DEFAULT nextval('public.opphold_person_id_seq'::regclass);


--
-- Name: oppholdskrav_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppholdskrav_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.oppholdskrav_grunnlag_id_seq'::regclass);


--
-- Name: oppholdskrav_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppholdskrav_vurdering ALTER COLUMN id SET DEFAULT nextval('public.oppholdskrav_vurdering_id_seq'::regclass);


--
-- Name: oppholdskrav_vurdering_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppholdskrav_vurdering_periode ALTER COLUMN id SET DEFAULT nextval('public.oppholdskrav_vurdering_periode_id_seq'::regclass);


--
-- Name: oppholdskrav_vurderinger grunnlag_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppholdskrav_vurderinger ALTER COLUMN grunnlag_id SET DEFAULT nextval('public.oppholdskrav_vurderinger_grunnlag_id_seq'::regclass);


--
-- Name: oppholdskrav_vurderinger vurdering_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppholdskrav_vurderinger ALTER COLUMN vurdering_id SET DEFAULT nextval('public.oppholdskrav_vurderinger_vurdering_id_seq'::regclass);


--
-- Name: overgang_arbeid_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_arbeid_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.overgang_arbeid_grunnlag_id_seq'::regclass);


--
-- Name: overgang_arbeid_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_arbeid_vurdering ALTER COLUMN id SET DEFAULT nextval('public.overgang_arbeid_vurdering_id_seq'::regclass);


--
-- Name: overgang_arbeid_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_arbeid_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.overgang_arbeid_vurderinger_id_seq'::regclass);


--
-- Name: overgang_ufore_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_ufore_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.overgang_ufore_grunnlag_id_seq'::regclass);


--
-- Name: overgang_ufore_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_ufore_vurdering ALTER COLUMN id SET DEFAULT nextval('public.overgang_ufore_vurdering_id_seq'::regclass);


--
-- Name: overgang_ufore_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_ufore_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.overgang_ufore_vurderinger_id_seq'::regclass);


--
-- Name: paaklaget_behandling_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paaklaget_behandling_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.paaklaget_behandling_grunnlag_id_seq'::regclass);


--
-- Name: paaklaget_behandling_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paaklaget_behandling_vurdering ALTER COLUMN id SET DEFAULT nextval('public.paaklaget_behandling_vurdering_id_seq'::regclass);


--
-- Name: person id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person ALTER COLUMN id SET DEFAULT nextval('public.person_id_seq'::regclass);


--
-- Name: person_ident id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_ident ALTER COLUMN id SET DEFAULT nextval('public.person_ident_id_seq'::regclass);


--
-- Name: personopplysning_forutgaaende_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personopplysning_forutgaaende_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.personopplysning_forutgaaende_grunnlag_id_seq'::regclass);


--
-- Name: personopplysning_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personopplysning_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.personopplysning_grunnlag_id_seq'::regclass);


--
-- Name: reduksjon_11_9 id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reduksjon_11_9 ALTER COLUMN id SET DEFAULT nextval('public.reduksjon_11_9_id_seq'::regclass);


--
-- Name: reduksjon_11_9_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reduksjon_11_9_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.reduksjon_11_9_grunnlag_id_seq'::regclass);


--
-- Name: refusjonkrav_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refusjonkrav_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.refusjonkrav_grunnlag_id_seq'::regclass);


--
-- Name: refusjonkrav_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refusjonkrav_vurdering ALTER COLUMN id SET DEFAULT nextval('public.refusjonkrav_vurdering_id_seq'::regclass);


--
-- Name: refusjonkrav_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refusjonkrav_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.refusjonkrav_vurderinger_id_seq'::regclass);


--
-- Name: rettighetsperiode_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetsperiode_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.rettighetsperiode_grunnlag_id_seq'::regclass);


--
-- Name: rettighetsperiode_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetsperiode_vurdering ALTER COLUMN id SET DEFAULT nextval('public.rettighetsperiode_vurdering_id_seq'::regclass);


--
-- Name: rettighetsperiode_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetsperiode_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.rettighetsperiode_vurderinger_id_seq'::regclass);


--
-- Name: rettighetstype_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetstype_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.rettighetstype_grunnlag_id_seq'::regclass);


--
-- Name: rettighetstype_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetstype_periode ALTER COLUMN id SET DEFAULT nextval('public.rettighetstype_periode_id_seq'::regclass);


--
-- Name: rettighetstype_perioder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetstype_perioder ALTER COLUMN id SET DEFAULT nextval('public.rettighetstype_perioder_id_seq'::regclass);


--
-- Name: rettighetstype_sporing id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetstype_sporing ALTER COLUMN id SET DEFAULT nextval('public.rettighetstype_sporing_id_seq'::regclass);


--
-- Name: sak id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sak ALTER COLUMN id SET DEFAULT nextval('public.sak_id_seq'::regclass);


--
-- Name: sam_id id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sam_id ALTER COLUMN id SET DEFAULT nextval('public.sam_id_id_seq'::regclass);


--
-- Name: samordning_andre_statlige_ytelser_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_andre_statlige_ytelser_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.samordning_andre_statlige_ytelser_grunnlag_id_seq'::regclass);


--
-- Name: samordning_andre_statlige_ytelser_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_andre_statlige_ytelser_vurdering ALTER COLUMN id SET DEFAULT nextval('public.samordning_andre_statlige_ytelser_vurdering_id_seq'::regclass);


--
-- Name: samordning_andre_statlige_ytelser_vurdering_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_andre_statlige_ytelser_vurdering_periode ALTER COLUMN id SET DEFAULT nextval('public.samordning_andre_statlige_ytelser_vurdering_periode_id_seq'::regclass);


--
-- Name: samordning_arbeidsgiver_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_arbeidsgiver_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.samordning_arbeidsgiver_grunnlag_id_seq'::regclass);


--
-- Name: samordning_arbeidsgiver_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_arbeidsgiver_vurdering ALTER COLUMN id SET DEFAULT nextval('public.samordning_arbeidsgiver_vurdering_id_seq'::regclass);


--
-- Name: samordning_arbeidsgiver_vurdering_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_arbeidsgiver_vurdering_periode ALTER COLUMN id SET DEFAULT nextval('public.samordning_arbeidsgiver_vurdering_periode_id_seq'::regclass);


--
-- Name: samordning_barnepensjon_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_barnepensjon_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.samordning_barnepensjon_grunnlag_id_seq'::regclass);


--
-- Name: samordning_barnepensjon_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_barnepensjon_vurdering ALTER COLUMN id SET DEFAULT nextval('public.samordning_barnepensjon_vurdering_id_seq'::regclass);


--
-- Name: samordning_barnepensjon_vurdering_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_barnepensjon_vurdering_periode ALTER COLUMN id SET DEFAULT nextval('public.samordning_barnepensjon_vurdering_periode_id_seq'::regclass);


--
-- Name: samordning_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.samordning_grunnlag_id_seq'::regclass);


--
-- Name: samordning_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_periode ALTER COLUMN id SET DEFAULT nextval('public.samordning_periode_id_seq'::regclass);


--
-- Name: samordning_perioder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_perioder ALTER COLUMN id SET DEFAULT nextval('public.samordning_perioder_id_seq'::regclass);


--
-- Name: samordning_ufore_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ufore_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.samordning_ufore_grunnlag_id_seq'::regclass);


--
-- Name: samordning_ufore_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ufore_vurdering ALTER COLUMN id SET DEFAULT nextval('public.samordning_ufore_vurdering_id_seq'::regclass);


--
-- Name: samordning_ufore_vurdering_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ufore_vurdering_periode ALTER COLUMN id SET DEFAULT nextval('public.samordning_ufore_vurdering_periode_id_seq'::regclass);


--
-- Name: samordning_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_vurdering ALTER COLUMN id SET DEFAULT nextval('public.samordning_vurdering_id_seq'::regclass);


--
-- Name: samordning_vurdering_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_vurdering_periode ALTER COLUMN id SET DEFAULT nextval('public.samordning_vurdering_periode_id_seq'::regclass);


--
-- Name: samordning_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.samordning_vurderinger_id_seq'::regclass);


--
-- Name: samordning_ytelse id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelse ALTER COLUMN id SET DEFAULT nextval('public.samordning_ytelse_id_seq'::regclass);


--
-- Name: samordning_ytelse_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelse_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.samordning_ytelse_grunnlag_id_seq'::regclass);


--
-- Name: samordning_ytelse_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelse_periode ALTER COLUMN id SET DEFAULT nextval('public.samordning_ytelse_periode_id_seq'::regclass);


--
-- Name: samordning_ytelser id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelser ALTER COLUMN id SET DEFAULT nextval('public.samordning_ytelser_id_seq'::regclass);


--
-- Name: samordning_ytelsevurdering_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelsevurdering_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.samordning_ytelsevurdering_grunnlag_id_seq'::regclass);


--
-- Name: soning_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.soning_vurdering ALTER COLUMN id SET DEFAULT nextval('public.soning_vurdering_id_seq'::regclass);


--
-- Name: soning_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.soning_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.soning_vurderinger_id_seq'::regclass);


--
-- Name: stans_opphor id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor ALTER COLUMN id SET DEFAULT nextval('public.stans_opphor_id_seq'::regclass);


--
-- Name: stans_opphor_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.stans_opphor_grunnlag_id_seq'::regclass);


--
-- Name: stans_opphor_set id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_set ALTER COLUMN id SET DEFAULT nextval('public.stans_opphor_set_id_seq'::regclass);


--
-- Name: stans_opphor_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_vurdering ALTER COLUMN id SET DEFAULT nextval('public.stans_opphor_vurdering_id_seq'::regclass);


--
-- Name: stans_opphor_vurdering_v2 id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_vurdering_v2 ALTER COLUMN id SET DEFAULT nextval('public.stans_opphor_vurdering_v2_id_seq'::regclass);


--
-- Name: stans_opphor_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.stans_opphor_vurderinger_id_seq'::regclass);


--
-- Name: stans_opphor_vurderinger_v2 id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_vurderinger_v2 ALTER COLUMN id SET DEFAULT nextval('public.stans_opphor_vurderinger_v2_id_seq'::regclass);


--
-- Name: steg_historikk id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.steg_historikk ALTER COLUMN id SET DEFAULT nextval('public.steg_historikk_id_seq'::regclass);


--
-- Name: student_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.student_grunnlag_id_seq'::regclass);


--
-- Name: student_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_vurdering ALTER COLUMN id SET DEFAULT nextval('public.student_vurdering_id_seq'::regclass);


--
-- Name: student_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.student_vurderinger_id_seq'::regclass);


--
-- Name: svar_fra_andreinstans_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.svar_fra_andreinstans_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.svar_fra_andreinstans_grunnlag_id_seq'::regclass);


--
-- Name: svar_fra_andreinstans_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.svar_fra_andreinstans_vurdering ALTER COLUMN id SET DEFAULT nextval('public.svar_fra_andreinstans_vurdering_id_seq'::regclass);


--
-- Name: sykdom_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.sykdom_grunnlag_id_seq'::regclass);


--
-- Name: sykdom_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_vurdering ALTER COLUMN id SET DEFAULT nextval('public.sykdom_vurdering_id_seq'::regclass);


--
-- Name: sykdom_vurdering_bidiagnoser id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_vurdering_bidiagnoser ALTER COLUMN id SET DEFAULT nextval('public.sykdom_vurdering_bidiagnoser_id_seq'::regclass);


--
-- Name: sykdom_vurdering_brev id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_vurdering_brev ALTER COLUMN id SET DEFAULT nextval('public.sykdom_vurdering_brev_id_seq'::regclass);


--
-- Name: sykdom_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.sykdom_vurderinger_id_seq'::regclass);


--
-- Name: sykepenge_erstatning_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykepenge_erstatning_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.sykepenge_erstatning_grunnlag_id_seq'::regclass);


--
-- Name: sykepenge_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykepenge_vurdering ALTER COLUMN id SET DEFAULT nextval('public.sykepenge_vurdering_id_seq'::regclass);


--
-- Name: sykepenge_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykepenge_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.sykepenge_vurderinger_id_seq'::regclass);


--
-- Name: sykestipend_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykestipend_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.sykestipend_grunnlag_id_seq'::regclass);


--
-- Name: sykestipend_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykestipend_vurdering ALTER COLUMN id SET DEFAULT nextval('public.sykestipend_vurdering_id_seq'::regclass);


--
-- Name: tilbakekrevingsbehandling id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilbakekrevingsbehandling ALTER COLUMN id SET DEFAULT nextval('public.tilbakekrevingsbehandling_id_seq'::regclass);


--
-- Name: tilbakekrevingshendelse id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilbakekrevingshendelse ALTER COLUMN id SET DEFAULT nextval('public.tilbakekrevingshendelse_id_seq'::regclass);


--
-- Name: tilkjent_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilkjent_periode ALTER COLUMN id SET DEFAULT nextval('public.tilkjent_periode_id_seq'::regclass);


--
-- Name: tilkjent_ytelse id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilkjent_ytelse ALTER COLUMN id SET DEFAULT nextval('public.tilkjent_ytelse_id_seq'::regclass);


--
-- Name: tilkjent_ytelse_sporing id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilkjent_ytelse_sporing ALTER COLUMN id SET DEFAULT nextval('public.tilkjent_ytelse_sporing_id_seq'::regclass);


--
-- Name: tiltakspenger_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tiltakspenger_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.tiltakspenger_grunnlag_id_seq'::regclass);


--
-- Name: tiltakspenger_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tiltakspenger_periode ALTER COLUMN id SET DEFAULT nextval('public.tiltakspenger_periode_id_seq'::regclass);


--
-- Name: tiltakspenger_perioder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tiltakspenger_perioder ALTER COLUMN id SET DEFAULT nextval('public.tiltakspenger_perioder_id_seq'::regclass);


--
-- Name: tjenestepensjon_forhold_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_forhold_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.tjenestepensjon_forhold_grunnlag_id_seq'::regclass);


--
-- Name: tjenestepensjon_ordning id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_ordning ALTER COLUMN id SET DEFAULT nextval('public.tjenestepensjon_ordning_id_seq'::regclass);


--
-- Name: tjenestepensjon_ordninger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_ordninger ALTER COLUMN id SET DEFAULT nextval('public.tjenestepensjon_ordninger_id_seq'::regclass);


--
-- Name: tjenestepensjon_refusjonskrav_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_refusjonskrav_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.tjenestepensjon_refusjonskrav_grunnlag_id_seq'::regclass);


--
-- Name: tjenestepensjon_refusjonskrav_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_refusjonskrav_vurdering ALTER COLUMN id SET DEFAULT nextval('public.tjenestepensjon_refusjonskrav_vurdering_id_seq'::regclass);


--
-- Name: tjenestepensjon_ytelse id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_ytelse ALTER COLUMN id SET DEFAULT nextval('public.tjenestepensjon_ytelse_id_seq'::regclass);


--
-- Name: trekk_klage_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trekk_klage_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.trekk_klage_grunnlag_id_seq'::regclass);


--
-- Name: trekk_klage_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trekk_klage_vurdering ALTER COLUMN id SET DEFAULT nextval('public.trekk_klage_vurdering_id_seq'::regclass);


--
-- Name: trukket_soknad_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trukket_soknad_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.trukket_soknad_grunnlag_id_seq'::regclass);


--
-- Name: trukket_soknad_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trukket_soknad_vurdering ALTER COLUMN id SET DEFAULT nextval('public.trukket_soknad_vurdering_id_seq'::regclass);


--
-- Name: trukket_soknad_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trukket_soknad_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.trukket_soknad_vurderinger_id_seq'::regclass);


--
-- Name: ufore id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ufore ALTER COLUMN id SET DEFAULT nextval('public.ufore_id_seq'::regclass);


--
-- Name: ufore_gradering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ufore_gradering ALTER COLUMN id SET DEFAULT nextval('public.ufore_gradering_id_seq'::regclass);


--
-- Name: ufore_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ufore_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.ufore_grunnlag_id_seq'::regclass);


--
-- Name: ufore_soknad_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ufore_soknad_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.ufore_soknad_grunnlag_id_seq'::regclass);


--
-- Name: underveis_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.underveis_grunnlag_id_seq'::regclass);


--
-- Name: underveis_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_periode ALTER COLUMN id SET DEFAULT nextval('public.underveis_periode_id_seq'::regclass);


--
-- Name: underveis_perioder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_perioder ALTER COLUMN id SET DEFAULT nextval('public.underveis_perioder_id_seq'::regclass);


--
-- Name: underveis_sporing id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_sporing ALTER COLUMN id SET DEFAULT nextval('public.underveis_sporing_id_seq'::regclass);


--
-- Name: utenlands_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utenlands_periode ALTER COLUMN id SET DEFAULT nextval('public.utenlands_periode_id_seq'::regclass);


--
-- Name: vedtak id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtak ALTER COLUMN id SET DEFAULT nextval('public.vedtak_id_seq'::regclass);


--
-- Name: vedtakslengde_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtakslengde_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.vedtakslengde_grunnlag_id_seq'::regclass);


--
-- Name: vedtakslengde_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtakslengde_vurdering ALTER COLUMN id SET DEFAULT nextval('public.vedtakslengde_vurdering_id_seq'::regclass);


--
-- Name: vedtakslengde_vurderinger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtakslengde_vurderinger ALTER COLUMN id SET DEFAULT nextval('public.vedtakslengde_vurderinger_id_seq'::regclass);


--
-- Name: vilkar id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkar ALTER COLUMN id SET DEFAULT nextval('public.vilkar_id_seq'::regclass);


--
-- Name: vilkar_periode id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkar_periode ALTER COLUMN id SET DEFAULT nextval('public.vilkar_periode_id_seq'::regclass);


--
-- Name: vilkar_resultat id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkar_resultat ALTER COLUMN id SET DEFAULT nextval('public.vilkar_resultat_id_seq'::regclass);


--
-- Name: vurderingsbehov id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vurderingsbehov ALTER COLUMN id SET DEFAULT nextval('public.aarsak_til_behandling_id_seq'::regclass);


--
-- Name: yrkesskade id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade ALTER COLUMN id SET DEFAULT nextval('public.yrkesskade_id_seq'::regclass);


--
-- Name: yrkesskade_dato id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_dato ALTER COLUMN id SET DEFAULT nextval('public.yrkesskade_dato_id_seq'::regclass);


--
-- Name: yrkesskade_grunnlag id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_grunnlag ALTER COLUMN id SET DEFAULT nextval('public.yrkesskade_grunnlag_id_seq'::regclass);


--
-- Name: yrkesskade_inntekt id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_inntekt ALTER COLUMN id SET DEFAULT nextval('public.yrkesskade_inntekt_id_seq'::regclass);


--
-- Name: yrkesskade_inntekter id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_inntekter ALTER COLUMN id SET DEFAULT nextval('public.yrkesskade_inntekter_id_seq'::regclass);


--
-- Name: yrkesskade_relaterte_saker id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_relaterte_saker ALTER COLUMN id SET DEFAULT nextval('public.yrkesskade_relaterte_saker_id_seq'::regclass);


--
-- Name: yrkesskade_vurdering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_vurdering ALTER COLUMN id SET DEFAULT nextval('public.yrkesskade_vurdering_id_seq'::regclass);


--
-- Name: vurderingsbehov aarsak_til_behandling_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vurderingsbehov
    ADD CONSTRAINT aarsak_til_behandling_pkey PRIMARY KEY (id);


--
-- Name: aktivitetsplikt_11_7_grunnlag aktivitetsplikt_11_7_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_7_grunnlag
    ADD CONSTRAINT aktivitetsplikt_11_7_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: aktivitetsplikt_11_7_varsel aktivitetsplikt_11_7_varsel_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_7_varsel
    ADD CONSTRAINT aktivitetsplikt_11_7_varsel_pkey PRIMARY KEY (id);


--
-- Name: aktivitetsplikt_11_7_vurdering aktivitetsplikt_11_7_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_7_vurdering
    ADD CONSTRAINT aktivitetsplikt_11_7_vurdering_pkey PRIMARY KEY (id);


--
-- Name: aktivitetsplikt_11_7_vurderinger aktivitetsplikt_11_7_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_7_vurderinger
    ADD CONSTRAINT aktivitetsplikt_11_7_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: aktivitetsplikt_11_9_grunnlag aktivitetsplikt_11_9_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_9_grunnlag
    ADD CONSTRAINT aktivitetsplikt_11_9_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: aktivitetsplikt_11_9_vurdering aktivitetsplikt_11_9_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_9_vurdering
    ADD CONSTRAINT aktivitetsplikt_11_9_vurdering_pkey PRIMARY KEY (id);


--
-- Name: aktivitetsplikt_11_9_vurderinger aktivitetsplikt_11_9_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_9_vurderinger
    ADD CONSTRAINT aktivitetsplikt_11_9_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: andre_ytelser_oppgitt_i_soknad_grunnlag andre_ytelser_oppgitt_i_soknad_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.andre_ytelser_oppgitt_i_soknad_grunnlag
    ADD CONSTRAINT andre_ytelser_oppgitt_i_soknad_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: andre_ytelser_svar_i_soknad andre_ytelser_svar_i_soknad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.andre_ytelser_svar_i_soknad
    ADD CONSTRAINT andre_ytelser_svar_i_soknad_pkey PRIMARY KEY (id);


--
-- Name: annen_ytelse_oppgitt_i_soknad annen_ytelse_oppgitt_i_soknad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.annen_ytelse_oppgitt_i_soknad
    ADD CONSTRAINT annen_ytelse_oppgitt_i_soknad_pkey PRIMARY KEY (id);


--
-- Name: arbeid_detaljer arbeid_detaljer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeid_detaljer
    ADD CONSTRAINT arbeid_detaljer_pkey PRIMARY KEY (id);


--
-- Name: arbeid_forutgaaende arbeid_forutgaaende_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeid_forutgaaende
    ADD CONSTRAINT arbeid_forutgaaende_pkey PRIMARY KEY (id);


--
-- Name: arbeid arbeid_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeid
    ADD CONSTRAINT arbeid_pkey PRIMARY KEY (id);


--
-- Name: arbeider_forutgaaende arbeider_forutgaaende_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeider_forutgaaende
    ADD CONSTRAINT arbeider_forutgaaende_pkey PRIMARY KEY (id);


--
-- Name: arbeider arbeider_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeider
    ADD CONSTRAINT arbeider_pkey PRIMARY KEY (id);


--
-- Name: arbeidsevne_grunnlag arbeidsevne_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsevne_grunnlag
    ADD CONSTRAINT arbeidsevne_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: arbeidsevne arbeidsevne_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsevne
    ADD CONSTRAINT arbeidsevne_pkey PRIMARY KEY (id);


--
-- Name: arbeidsevne_vurdering arbeidsevne_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsevne_vurdering
    ADD CONSTRAINT arbeidsevne_vurdering_pkey PRIMARY KEY (id);


--
-- Name: arbeidsopptrapping_grunnlag arbeidsopptrapping_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsopptrapping_grunnlag
    ADD CONSTRAINT arbeidsopptrapping_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: arbeidsopptrapping_vurdering arbeidsopptrapping_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsopptrapping_vurdering
    ADD CONSTRAINT arbeidsopptrapping_vurdering_pkey PRIMARY KEY (id);


--
-- Name: arbeidsopptrapping_vurderinger arbeidsopptrapping_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsopptrapping_vurderinger
    ADD CONSTRAINT arbeidsopptrapping_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: avbryt_aktivitetspliktbehandling_grunnlag avbryt_aktivitetspliktbehandling_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avbryt_aktivitetspliktbehandling_grunnlag
    ADD CONSTRAINT avbryt_aktivitetspliktbehandling_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: avbryt_aktivitetspliktbehandling_vurdering avbryt_aktivitetspliktbehandling_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avbryt_aktivitetspliktbehandling_vurdering
    ADD CONSTRAINT avbryt_aktivitetspliktbehandling_vurdering_pkey PRIMARY KEY (id);


--
-- Name: avklaringsbehov_endring_aarsak avklaringsbehov_endring_aarsak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avklaringsbehov_endring_aarsak
    ADD CONSTRAINT avklaringsbehov_endring_aarsak_pkey PRIMARY KEY (id);


--
-- Name: avklaringsbehov_endring avklaringsbehov_endring_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avklaringsbehov_endring
    ADD CONSTRAINT avklaringsbehov_endring_pkey PRIMARY KEY (id);


--
-- Name: avklaringsbehov avklaringsbehov_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avklaringsbehov
    ADD CONSTRAINT avklaringsbehov_pkey PRIMARY KEY (id);


--
-- Name: avvist_formkrav_varsel avvist_formkrav_varsel_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avvist_formkrav_varsel
    ADD CONSTRAINT avvist_formkrav_varsel_pkey PRIMARY KEY (id);


--
-- Name: barn_saksbehandler_oppgitt_barnopplysning barn_saksbehandler_oppgitt_barnopplysning_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_saksbehandler_oppgitt_barnopplysning
    ADD CONSTRAINT barn_saksbehandler_oppgitt_barnopplysning_pkey PRIMARY KEY (id);


--
-- Name: barn_saksbehandler_oppgitt barn_saksbehandler_oppgitt_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_saksbehandler_oppgitt
    ADD CONSTRAINT barn_saksbehandler_oppgitt_pkey PRIMARY KEY (id);


--
-- Name: barn_tillegg barn_tillegg_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_tillegg
    ADD CONSTRAINT barn_tillegg_pkey PRIMARY KEY (id);


--
-- Name: barn_vurdering_periode barn_vurdering_periode_ikke_overlapp_periode; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_vurdering_periode
    ADD CONSTRAINT barn_vurdering_periode_ikke_overlapp_periode EXCLUDE USING gist (barn_vurdering_id WITH =, periode WITH &&);


--
-- Name: barn_vurdering_periode barn_vurdering_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_vurdering_periode
    ADD CONSTRAINT barn_vurdering_periode_pkey PRIMARY KEY (id);


--
-- Name: barn_vurdering barn_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_vurdering
    ADD CONSTRAINT barn_vurdering_pkey PRIMARY KEY (id);


--
-- Name: barn_vurderinger barn_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_vurderinger
    ADD CONSTRAINT barn_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: barnetillegg_grunnlag barnetillegg_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnetillegg_grunnlag
    ADD CONSTRAINT barnetillegg_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: barnetillegg_periode barnetillegg_periode_ikke_overlapp_periode; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnetillegg_periode
    ADD CONSTRAINT barnetillegg_periode_ikke_overlapp_periode EXCLUDE USING gist (perioder_id WITH =, periode WITH &&);


--
-- Name: barnetillegg_periode barnetillegg_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnetillegg_periode
    ADD CONSTRAINT barnetillegg_periode_pkey PRIMARY KEY (id);


--
-- Name: barnetillegg_perioder barnetillegg_perioder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnetillegg_perioder
    ADD CONSTRAINT barnetillegg_perioder_pkey PRIMARY KEY (id);


--
-- Name: barnopplysning_grunnlag_barnopplysning barnopplysning_grunnlag_barnopplysning_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnopplysning_grunnlag_barnopplysning
    ADD CONSTRAINT barnopplysning_grunnlag_barnopplysning_pkey PRIMARY KEY (id);


--
-- Name: barnopplysning_grunnlag barnopplysning_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnopplysning_grunnlag
    ADD CONSTRAINT barnopplysning_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: barnopplysning barnopplysning_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnopplysning
    ADD CONSTRAINT barnopplysning_pkey PRIMARY KEY (id);


--
-- Name: behandlende_enhet_grunnlag behandlende_enhet_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandlende_enhet_grunnlag
    ADD CONSTRAINT behandlende_enhet_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: behandlende_enhet_vurdering behandlende_enhet_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandlende_enhet_vurdering
    ADD CONSTRAINT behandlende_enhet_vurdering_pkey PRIMARY KEY (id);


--
-- Name: behandling_aarsak behandling_aarsak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling_aarsak
    ADD CONSTRAINT behandling_aarsak_pkey PRIMARY KEY (id);


--
-- Name: behandling behandling_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling
    ADD CONSTRAINT behandling_pkey PRIMARY KEY (id);


--
-- Name: behandling behandling_referanse_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling
    ADD CONSTRAINT behandling_referanse_key UNIQUE (referanse);


--
-- Name: beregning_hoved beregning_hoved_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_hoved
    ADD CONSTRAINT beregning_hoved_pkey PRIMARY KEY (id);


--
-- Name: beregning_inntekt beregning_inntekt_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_inntekt
    ADD CONSTRAINT beregning_inntekt_pkey PRIMARY KEY (id);


--
-- Name: beregning beregning_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning
    ADD CONSTRAINT beregning_pkey PRIMARY KEY (id);


--
-- Name: beregning_ufore_inntekt beregning_ufore_inntekt_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore_inntekt
    ADD CONSTRAINT beregning_ufore_inntekt_pkey PRIMARY KEY (id);


--
-- Name: beregning_ufore beregning_ufore_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore
    ADD CONSTRAINT beregning_ufore_pkey PRIMARY KEY (id);


--
-- Name: beregning_ufore_tidsperiode beregning_ufore_tidsperiode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore_tidsperiode
    ADD CONSTRAINT beregning_ufore_tidsperiode_pkey PRIMARY KEY (id);


--
-- Name: beregning_ufore_uforegrader beregning_ufore_uforegrader_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore_uforegrader
    ADD CONSTRAINT beregning_ufore_uforegrader_pkey PRIMARY KEY (id);


--
-- Name: beregning_yrkesskade beregning_yrkesskade_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_yrkesskade
    ADD CONSTRAINT beregning_yrkesskade_pkey PRIMARY KEY (id);


--
-- Name: beregningsgrunnlag beregningsgrunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregningsgrunnlag
    ADD CONSTRAINT beregningsgrunnlag_pkey PRIMARY KEY (id);


--
-- Name: beregningsfakta_grunnlag beregningstidspunkt_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregningsfakta_grunnlag
    ADD CONSTRAINT beregningstidspunkt_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: beregningstidspunkt_vurdering beregningstidspunkt_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregningstidspunkt_vurdering
    ADD CONSTRAINT beregningstidspunkt_vurdering_pkey PRIMARY KEY (id);


--
-- Name: bistand_grunnlag bistand_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bistand_grunnlag
    ADD CONSTRAINT bistand_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: bistand bistand_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bistand
    ADD CONSTRAINT bistand_pkey PRIMARY KEY (id);


--
-- Name: bistand_vurderinger bistand_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bistand_vurderinger
    ADD CONSTRAINT bistand_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: brevbestilling brevbestilling_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brevbestilling
    ADD CONSTRAINT brevbestilling_pkey PRIMARY KEY (id);


--
-- Name: brevbestilling brevbestilling_referanse_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brevbestilling
    ADD CONSTRAINT brevbestilling_referanse_key UNIQUE (referanse);


--
-- Name: brudd_aktivitetsplikt_grunnlag brudd_aktivitetsplikt_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brudd_aktivitetsplikt_grunnlag
    ADD CONSTRAINT brudd_aktivitetsplikt_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: brudd_aktivitetsplikt brudd_aktivitetsplikt_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brudd_aktivitetsplikt
    ADD CONSTRAINT brudd_aktivitetsplikt_pkey PRIMARY KEY (id);


--
-- Name: brudd_aktivitetsplikter brudd_aktivitetsplikter_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brudd_aktivitetsplikter
    ADD CONSTRAINT brudd_aktivitetsplikter_pkey PRIMARY KEY (id);


--
-- Name: bruker_land_aggregat bruker_land_aggregat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_land_aggregat
    ADD CONSTRAINT bruker_land_aggregat_pkey PRIMARY KEY (id);


--
-- Name: bruker_land_forutgaaende_aggregat bruker_land_forutgaaende_aggregat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_land_forutgaaende_aggregat
    ADD CONSTRAINT bruker_land_forutgaaende_aggregat_pkey PRIMARY KEY (id);


--
-- Name: bruker_land_forutgaaende bruker_land_forutgaaende_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_land_forutgaaende
    ADD CONSTRAINT bruker_land_forutgaaende_pkey PRIMARY KEY (id);


--
-- Name: bruker_land bruker_land_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_land
    ADD CONSTRAINT bruker_land_pkey PRIMARY KEY (id);


--
-- Name: bruker_personopplysning_forutgaaende bruker_personopplysning_forutgaaende_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_personopplysning_forutgaaende
    ADD CONSTRAINT bruker_personopplysning_forutgaaende_pkey PRIMARY KEY (id);


--
-- Name: bruker_statuser_forutgaaende_aggregat bruker_statuser_forutgaaende_aggregat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_statuser_forutgaaende_aggregat
    ADD CONSTRAINT bruker_statuser_forutgaaende_aggregat_pkey PRIMARY KEY (id);


--
-- Name: bruker_statuser_forutgaaende bruker_statuser_forutgaaende_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_statuser_forutgaaende
    ADD CONSTRAINT bruker_statuser_forutgaaende_pkey PRIMARY KEY (id);


--
-- Name: bruker_utenlandsadresse_forutgaaende bruker_utenlandsadresse_forutgaaende_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_utenlandsadresse_forutgaaende
    ADD CONSTRAINT bruker_utenlandsadresse_forutgaaende_pkey PRIMARY KEY (id);


--
-- Name: bruker_utenlandsadresse bruker_utenlandsadresse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_utenlandsadresse
    ADD CONSTRAINT bruker_utenlandsadresse_pkey PRIMARY KEY (id);


--
-- Name: bruker_utenlandsadresser_aggregat bruker_utenlandsadresser_aggregat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_utenlandsadresser_aggregat
    ADD CONSTRAINT bruker_utenlandsadresser_aggregat_pkey PRIMARY KEY (id);


--
-- Name: bruker_utenlandsadresser_forutgaaende_aggregat bruker_utenlandsadresser_forutgaaende_aggregat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_utenlandsadresser_forutgaaende_aggregat
    ADD CONSTRAINT bruker_utenlandsadresser_forutgaaende_aggregat_pkey PRIMARY KEY (id);


--
-- Name: dagpenger_grunnlag dagpenger_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dagpenger_grunnlag
    ADD CONSTRAINT dagpenger_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: dagpenger_periode dagpenger_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dagpenger_periode
    ADD CONSTRAINT dagpenger_periode_pkey PRIMARY KEY (id);


--
-- Name: dagpenger_perioder dagpenger_perioder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dagpenger_perioder
    ADD CONSTRAINT dagpenger_perioder_pkey PRIMARY KEY (id);


--
-- Name: egen_virksomhet_oppstart_periode egen_virksomhet_oppstart_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.egen_virksomhet_oppstart_periode
    ADD CONSTRAINT egen_virksomhet_oppstart_periode_pkey PRIMARY KEY (id);


--
-- Name: egen_virksomhet_oppstart_perioder egen_virksomhet_oppstart_perioder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.egen_virksomhet_oppstart_perioder
    ADD CONSTRAINT egen_virksomhet_oppstart_perioder_pkey PRIMARY KEY (id);


--
-- Name: egen_virksomhet_utvikling_periode egen_virksomhet_utvikling_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.egen_virksomhet_utvikling_periode
    ADD CONSTRAINT egen_virksomhet_utvikling_periode_pkey PRIMARY KEY (id);


--
-- Name: egen_virksomhet_utvikling_perioder egen_virksomhet_utvikling_perioder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.egen_virksomhet_utvikling_perioder
    ADD CONSTRAINT egen_virksomhet_utvikling_perioder_pkey PRIMARY KEY (id);


--
-- Name: etablering_egen_virksomhet_grunnlag etablering_egen_virksomhet_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etablering_egen_virksomhet_grunnlag
    ADD CONSTRAINT etablering_egen_virksomhet_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: etablering_egen_virksomhet_vurdering etablering_egen_virksomhet_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etablering_egen_virksomhet_vurdering
    ADD CONSTRAINT etablering_egen_virksomhet_vurdering_pkey PRIMARY KEY (id);


--
-- Name: etablering_egen_virksomhet_vurderinger etablering_egen_virksomhet_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etablering_egen_virksomhet_vurderinger
    ADD CONSTRAINT etablering_egen_virksomhet_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: formkrav_grunnlag formkrav_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.formkrav_grunnlag
    ADD CONSTRAINT formkrav_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: formkrav_vurdering formkrav_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.formkrav_vurdering
    ADD CONSTRAINT formkrav_vurdering_pkey PRIMARY KEY (id);


--
-- Name: forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag
    ADD CONSTRAINT forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: forutgaaende_medlemskap_manuell_vurdering forutgaaende_medlemskap_manuell_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forutgaaende_medlemskap_manuell_vurdering
    ADD CONSTRAINT forutgaaende_medlemskap_manuell_vurdering_pkey PRIMARY KEY (id);


--
-- Name: forutgaaende_medlemskap_manuell_vurderinger forutgaaende_medlemskap_manuell_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forutgaaende_medlemskap_manuell_vurderinger
    ADD CONSTRAINT forutgaaende_medlemskap_manuell_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: fullmektig_grunnlag fullmektig_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fullmektig_grunnlag
    ADD CONSTRAINT fullmektig_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: fullmektig_vurdering fullmektig_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fullmektig_vurdering
    ADD CONSTRAINT fullmektig_vurdering_pkey PRIMARY KEY (id);


--
-- Name: helseopphold_vurdering helseopphold_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.helseopphold_vurdering
    ADD CONSTRAINT helseopphold_vurdering_pkey PRIMARY KEY (id);


--
-- Name: helseopphold_vurderinger helseopphold_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.helseopphold_vurderinger
    ADD CONSTRAINT helseopphold_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: informasjonskrav_oppdatert informasjonskrav_oppdatert_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.informasjonskrav_oppdatert
    ADD CONSTRAINT informasjonskrav_oppdatert_pkey PRIMARY KEY (id);


--
-- Name: inntekt_grunnlag inntekt_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt_grunnlag
    ADD CONSTRAINT inntekt_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: inntekt_i_norge_forutgaaende inntekt_i_norge_forutgaaende_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt_i_norge_forutgaaende
    ADD CONSTRAINT inntekt_i_norge_forutgaaende_pkey PRIMARY KEY (id);


--
-- Name: inntekt_i_norge inntekt_i_norge_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt_i_norge
    ADD CONSTRAINT inntekt_i_norge_pkey PRIMARY KEY (id);


--
-- Name: inntekt_periode inntekt_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt_periode
    ADD CONSTRAINT inntekt_periode_pkey PRIMARY KEY (id);


--
-- Name: inntekt inntekt_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt
    ADD CONSTRAINT inntekt_pkey PRIMARY KEY (id);


--
-- Name: inntekter_i_norge_forutgaaende inntekter_i_norge_forutgaaende_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekter_i_norge_forutgaaende
    ADD CONSTRAINT inntekter_i_norge_forutgaaende_pkey PRIMARY KEY (id);


--
-- Name: inntekter_i_norge inntekter_i_norge_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekter_i_norge
    ADD CONSTRAINT inntekter_i_norge_pkey PRIMARY KEY (id);


--
-- Name: inntekter inntekter_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekter
    ADD CONSTRAINT inntekter_pkey PRIMARY KEY (id);


--
-- Name: inntektsbortfall_grunnlag inntektsbortfall_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntektsbortfall_grunnlag
    ADD CONSTRAINT inntektsbortfall_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: inntektsbortfall_vurdering inntektsbortfall_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntektsbortfall_vurdering
    ADD CONSTRAINT inntektsbortfall_vurdering_pkey PRIMARY KEY (id);


--
-- Name: inntektsbortfall_vurderinger inntektsbortfall_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntektsbortfall_vurderinger
    ADD CONSTRAINT inntektsbortfall_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: jobb_arkiv jobb_arkiv_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobb_arkiv
    ADD CONSTRAINT jobb_arkiv_pkey PRIMARY KEY (id);


--
-- Name: jobb_historikk_arkiv jobb_historikk_arkiv_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobb_historikk_arkiv
    ADD CONSTRAINT jobb_historikk_arkiv_pkey PRIMARY KEY (id);


--
-- Name: avbryt_revurdering_grunnlag kanseller_revurdering_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avbryt_revurdering_grunnlag
    ADD CONSTRAINT kanseller_revurdering_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: avbryt_revurdering_vurdering kanseller_revurdering_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avbryt_revurdering_vurdering
    ADD CONSTRAINT kanseller_revurdering_vurdering_pkey PRIMARY KEY (id);


--
-- Name: klage_kontor_grunnlag klage_kontor_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.klage_kontor_grunnlag
    ADD CONSTRAINT klage_kontor_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: klage_kontor_vurdering klage_kontor_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.klage_kontor_vurdering
    ADD CONSTRAINT klage_kontor_vurdering_pkey PRIMARY KEY (id);


--
-- Name: klage_nay_grunnlag klage_nay_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.klage_nay_grunnlag
    ADD CONSTRAINT klage_nay_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: klage_nay_vurdering klage_nay_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.klage_nay_vurdering
    ADD CONSTRAINT klage_nay_vurdering_pkey PRIMARY KEY (id);


--
-- Name: krav_grunnlag krav_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.krav_grunnlag
    ADD CONSTRAINT krav_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: krav_vurdering krav_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.krav_vurdering
    ADD CONSTRAINT krav_vurdering_pkey PRIMARY KEY (id);


--
-- Name: krav_vurderinger krav_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.krav_vurderinger
    ADD CONSTRAINT krav_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: lovvalg_medlemskap_manuell_vurdering lovvalg_medlemskap_manuell_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lovvalg_medlemskap_manuell_vurdering
    ADD CONSTRAINT lovvalg_medlemskap_manuell_vurdering_pkey PRIMARY KEY (id);


--
-- Name: lovvalg_medlemskap_manuell_vurderinger lovvalg_medlemskap_manuell_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lovvalg_medlemskap_manuell_vurderinger
    ADD CONSTRAINT lovvalg_medlemskap_manuell_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: manuell_inntekt_vurdering_grunnlag manuell_inntekt_vurdering_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manuell_inntekt_vurdering_grunnlag
    ADD CONSTRAINT manuell_inntekt_vurdering_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: manuell_inntekt_vurdering manuell_inntekt_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manuell_inntekt_vurdering
    ADD CONSTRAINT manuell_inntekt_vurdering_pkey PRIMARY KEY (id);


--
-- Name: manuell_inntekt_vurderinger manuell_inntekt_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manuell_inntekt_vurderinger
    ADD CONSTRAINT manuell_inntekt_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: medlemskap_arbeid_og_inntekt_i_norge_grunnlag medlemskap_arbeid_og_inntekt_i_norge_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_arbeid_og_inntekt_i_norge_grunnlag
    ADD CONSTRAINT medlemskap_arbeid_og_inntekt_i_norge_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: medlemskap_forutgaaende_unntak_grunnlag medlemskap_forutgaaende_unntak_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_forutgaaende_unntak_grunnlag
    ADD CONSTRAINT medlemskap_forutgaaende_unntak_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: medlemskap_forutgaaende_unntak_person medlemskap_forutgaaende_unntak_person_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_forutgaaende_unntak_person
    ADD CONSTRAINT medlemskap_forutgaaende_unntak_person_pkey PRIMARY KEY (id);


--
-- Name: medlemskap_forutgaaende_unntak medlemskap_forutgaaende_unntak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_forutgaaende_unntak
    ADD CONSTRAINT medlemskap_forutgaaende_unntak_pkey PRIMARY KEY (id);


--
-- Name: medlemskap_unntak_grunnlag medlemskap_unntak_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_unntak_grunnlag
    ADD CONSTRAINT medlemskap_unntak_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: medlemskap_unntak_person medlemskap_unntak_person_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_unntak_person
    ADD CONSTRAINT medlemskap_unntak_person_pkey PRIMARY KEY (id);


--
-- Name: medlemskap_unntak medlemskap_unntak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_unntak
    ADD CONSTRAINT medlemskap_unntak_pkey PRIMARY KEY (id);


--
-- Name: meldekort_grunnlag meldekort_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldekort_grunnlag
    ADD CONSTRAINT meldekort_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: meldekort_periode meldekort_periode_ikke_overlapp_periode; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldekort_periode
    ADD CONSTRAINT meldekort_periode_ikke_overlapp_periode EXCLUDE USING gist (meldekort_id WITH =, periode WITH &&);


--
-- Name: meldekort_periode meldekort_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldekort_periode
    ADD CONSTRAINT meldekort_periode_pkey PRIMARY KEY (id);


--
-- Name: meldekort meldekort_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldekort
    ADD CONSTRAINT meldekort_pkey PRIMARY KEY (id);


--
-- Name: meldekortene meldekortene_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldekortene
    ADD CONSTRAINT meldekortene_pkey PRIMARY KEY (id);


--
-- Name: meldeperiode_grunnlag meldeperiode_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeperiode_grunnlag
    ADD CONSTRAINT meldeperiode_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: meldeperiode meldeperiode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeperiode
    ADD CONSTRAINT meldeperiode_pkey PRIMARY KEY (id);


--
-- Name: meldeplikt_fritak_grunnlag meldeplikt_fritak_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_fritak_grunnlag
    ADD CONSTRAINT meldeplikt_fritak_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: meldeplikt_fritak meldeplikt_fritak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_fritak
    ADD CONSTRAINT meldeplikt_fritak_pkey PRIMARY KEY (id);


--
-- Name: meldeplikt_fritak_vurdering meldeplikt_fritak_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_fritak_vurdering
    ADD CONSTRAINT meldeplikt_fritak_vurdering_pkey PRIMARY KEY (id);


--
-- Name: meldeplikt_overstyring_grunnlag meldeplikt_overstyring_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_overstyring_grunnlag
    ADD CONSTRAINT meldeplikt_overstyring_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: meldeplikt_overstyring_vurdering_perioder meldeplikt_overstyring_vurdering_perioder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_overstyring_vurdering_perioder
    ADD CONSTRAINT meldeplikt_overstyring_vurdering_perioder_pkey PRIMARY KEY (id);


--
-- Name: meldeplikt_overstyring_vurdering meldeplikt_overstyring_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_overstyring_vurdering
    ADD CONSTRAINT meldeplikt_overstyring_vurdering_pkey PRIMARY KEY (id);


--
-- Name: mellomlagret_vurdering mellomlagret_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mellomlagret_vurdering
    ADD CONSTRAINT mellomlagret_vurdering_pkey PRIMARY KEY (id);


--
-- Name: mellomlagret_vurdering mellomlagret_vurdering_unik_behandling_kode; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mellomlagret_vurdering
    ADD CONSTRAINT mellomlagret_vurdering_unik_behandling_kode UNIQUE (behandling_id, avklaringsbehov_kode);


--
-- Name: mottatt_dokument mottatt_dokument_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mottatt_dokument
    ADD CONSTRAINT mottatt_dokument_pkey PRIMARY KEY (id);


--
-- Name: oppfolgingsoppgave_grunnlag oppfolgingsoppgave_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppfolgingsoppgave_grunnlag
    ADD CONSTRAINT oppfolgingsoppgave_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: oppfolgingsoppgave_vurdering oppfolgingsoppgave_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppfolgingsoppgave_vurdering
    ADD CONSTRAINT oppfolgingsoppgave_vurdering_pkey PRIMARY KEY (id);


--
-- Name: jobb_historikk oppgave_historikk_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobb_historikk
    ADD CONSTRAINT oppgave_historikk_pkey PRIMARY KEY (id);


--
-- Name: jobb oppgave_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobb
    ADD CONSTRAINT oppgave_pkey PRIMARY KEY (id);


--
-- Name: oppgitt_barn oppgitt_barn_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgitt_barn
    ADD CONSTRAINT oppgitt_barn_pkey PRIMARY KEY (id);


--
-- Name: oppgitt_barnopplysning oppgitt_barnopplysning_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgitt_barnopplysning
    ADD CONSTRAINT oppgitt_barnopplysning_pkey PRIMARY KEY (id);


--
-- Name: oppgitt_student oppgitt_student_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgitt_student
    ADD CONSTRAINT oppgitt_student_pkey PRIMARY KEY (id);


--
-- Name: oppgitt_utenlandsopphold_grunnlag oppgitt_utenlandsopphold_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgitt_utenlandsopphold_grunnlag
    ADD CONSTRAINT oppgitt_utenlandsopphold_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: oppgitt_utenlandsopphold oppgitt_utenlandsopphold_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgitt_utenlandsopphold
    ADD CONSTRAINT oppgitt_utenlandsopphold_pkey PRIMARY KEY (id);


--
-- Name: opphold_grunnlag opphold_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opphold_grunnlag
    ADD CONSTRAINT opphold_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: opphold_person opphold_person_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opphold_person
    ADD CONSTRAINT opphold_person_pkey PRIMARY KEY (id);


--
-- Name: opphold opphold_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opphold
    ADD CONSTRAINT opphold_pkey PRIMARY KEY (id);


--
-- Name: oppholdskrav_grunnlag oppholdskrav_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppholdskrav_grunnlag
    ADD CONSTRAINT oppholdskrav_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: oppholdskrav_vurdering_periode oppholdskrav_vurdering_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppholdskrav_vurdering_periode
    ADD CONSTRAINT oppholdskrav_vurdering_periode_pkey PRIMARY KEY (id);


--
-- Name: oppholdskrav_vurdering oppholdskrav_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppholdskrav_vurdering
    ADD CONSTRAINT oppholdskrav_vurdering_pkey PRIMARY KEY (id);


--
-- Name: overgang_arbeid_grunnlag overgang_arbeid_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_arbeid_grunnlag
    ADD CONSTRAINT overgang_arbeid_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: overgang_arbeid_vurdering overgang_arbeid_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_arbeid_vurdering
    ADD CONSTRAINT overgang_arbeid_vurdering_pkey PRIMARY KEY (id);


--
-- Name: overgang_arbeid_vurderinger overgang_arbeid_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_arbeid_vurderinger
    ADD CONSTRAINT overgang_arbeid_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: overgang_ufore_grunnlag overgang_ufore_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_ufore_grunnlag
    ADD CONSTRAINT overgang_ufore_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: overgang_ufore_vurdering overgang_ufore_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_ufore_vurdering
    ADD CONSTRAINT overgang_ufore_vurdering_pkey PRIMARY KEY (id);


--
-- Name: overgang_ufore_vurderinger overgang_ufore_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_ufore_vurderinger
    ADD CONSTRAINT overgang_ufore_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: paaklaget_behandling_grunnlag paaklaget_behandling_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paaklaget_behandling_grunnlag
    ADD CONSTRAINT paaklaget_behandling_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: paaklaget_behandling_vurdering paaklaget_behandling_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paaklaget_behandling_vurdering
    ADD CONSTRAINT paaklaget_behandling_vurdering_pkey PRIMARY KEY (id);


--
-- Name: person_ident person_ident_ident_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_ident
    ADD CONSTRAINT person_ident_ident_key UNIQUE (ident);


--
-- Name: person_ident person_ident_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_ident
    ADD CONSTRAINT person_ident_pkey PRIMARY KEY (id);


--
-- Name: person person_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_pkey PRIMARY KEY (id);


--
-- Name: person person_referanse_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_referanse_key UNIQUE (referanse);


--
-- Name: personopplysning_forutgaaende_grunnlag personopplysning_forutgaaende_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personopplysning_forutgaaende_grunnlag
    ADD CONSTRAINT personopplysning_forutgaaende_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: personopplysning_grunnlag personopplysning_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personopplysning_grunnlag
    ADD CONSTRAINT personopplysning_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: bruker_personopplysning personopplysning_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_personopplysning
    ADD CONSTRAINT personopplysning_pkey PRIMARY KEY (id);


--
-- Name: reduksjon_11_9_grunnlag reduksjon_11_9_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reduksjon_11_9_grunnlag
    ADD CONSTRAINT reduksjon_11_9_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: reduksjon_11_9 reduksjon_11_9_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reduksjon_11_9
    ADD CONSTRAINT reduksjon_11_9_pkey PRIMARY KEY (id);


--
-- Name: refusjonkrav_grunnlag refusjonkrav_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refusjonkrav_grunnlag
    ADD CONSTRAINT refusjonkrav_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: refusjonkrav_vurdering refusjonkrav_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refusjonkrav_vurdering
    ADD CONSTRAINT refusjonkrav_vurdering_pkey PRIMARY KEY (id);


--
-- Name: refusjonkrav_vurderinger refusjonkrav_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refusjonkrav_vurderinger
    ADD CONSTRAINT refusjonkrav_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: rettighetsperiode_grunnlag rettighetsperiode_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetsperiode_grunnlag
    ADD CONSTRAINT rettighetsperiode_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: rettighetsperiode_vurdering rettighetsperiode_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetsperiode_vurdering
    ADD CONSTRAINT rettighetsperiode_vurdering_pkey PRIMARY KEY (id);


--
-- Name: rettighetsperiode_vurderinger rettighetsperiode_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetsperiode_vurderinger
    ADD CONSTRAINT rettighetsperiode_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: rettighetstype_grunnlag rettighetstype_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetstype_grunnlag
    ADD CONSTRAINT rettighetstype_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: rettighetstype_periode rettighetstype_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetstype_periode
    ADD CONSTRAINT rettighetstype_periode_pkey PRIMARY KEY (id);


--
-- Name: rettighetstype_perioder rettighetstype_perioder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetstype_perioder
    ADD CONSTRAINT rettighetstype_perioder_pkey PRIMARY KEY (id);


--
-- Name: rettighetstype_sporing rettighetstype_sporing_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetstype_sporing
    ADD CONSTRAINT rettighetstype_sporing_pkey PRIMARY KEY (id);


--
-- Name: sak sak_ikke_overlapp_periode; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sak
    ADD CONSTRAINT sak_ikke_overlapp_periode EXCLUDE USING gist (person_id WITH =, rettighetsperiode WITH &&);


--
-- Name: sak sak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sak
    ADD CONSTRAINT sak_pkey PRIMARY KEY (id);


--
-- Name: sam_id sam_id_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sam_id
    ADD CONSTRAINT sam_id_pkey PRIMARY KEY (id);


--
-- Name: samordning_andre_statlige_ytelser_grunnlag samordning_andre_statlige_ytelser_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_andre_statlige_ytelser_grunnlag
    ADD CONSTRAINT samordning_andre_statlige_ytelser_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: samordning_andre_statlige_ytelser_vurdering_periode samordning_andre_statlige_ytelser_vurdering_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_andre_statlige_ytelser_vurdering_periode
    ADD CONSTRAINT samordning_andre_statlige_ytelser_vurdering_periode_pkey PRIMARY KEY (id);


--
-- Name: samordning_andre_statlige_ytelser_vurdering samordning_andre_statlige_ytelser_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_andre_statlige_ytelser_vurdering
    ADD CONSTRAINT samordning_andre_statlige_ytelser_vurdering_pkey PRIMARY KEY (id);


--
-- Name: samordning_arbeidsgiver_grunnlag samordning_arbeidsgiver_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_arbeidsgiver_grunnlag
    ADD CONSTRAINT samordning_arbeidsgiver_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: samordning_arbeidsgiver_vurdering_periode samordning_arbeidsgiver_vurdering_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_arbeidsgiver_vurdering_periode
    ADD CONSTRAINT samordning_arbeidsgiver_vurdering_periode_pkey PRIMARY KEY (id);


--
-- Name: samordning_arbeidsgiver_vurdering samordning_arbeidsgiver_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_arbeidsgiver_vurdering
    ADD CONSTRAINT samordning_arbeidsgiver_vurdering_pkey PRIMARY KEY (id);


--
-- Name: samordning_barnepensjon_grunnlag samordning_barnepensjon_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_barnepensjon_grunnlag
    ADD CONSTRAINT samordning_barnepensjon_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: samordning_barnepensjon_vurdering_periode samordning_barnepensjon_vurdering_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_barnepensjon_vurdering_periode
    ADD CONSTRAINT samordning_barnepensjon_vurdering_periode_pkey PRIMARY KEY (id);


--
-- Name: samordning_barnepensjon_vurdering samordning_barnepensjon_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_barnepensjon_vurdering
    ADD CONSTRAINT samordning_barnepensjon_vurdering_pkey PRIMARY KEY (id);


--
-- Name: samordning_grunnlag samordning_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_grunnlag
    ADD CONSTRAINT samordning_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: samordning_periode samordning_periode_ikke_overlapp_periode; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_periode
    ADD CONSTRAINT samordning_periode_ikke_overlapp_periode EXCLUDE USING gist (perioder_id WITH =, periode WITH &&);


--
-- Name: samordning_periode samordning_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_periode
    ADD CONSTRAINT samordning_periode_pkey PRIMARY KEY (id);


--
-- Name: samordning_perioder samordning_perioder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_perioder
    ADD CONSTRAINT samordning_perioder_pkey PRIMARY KEY (id);


--
-- Name: samordning_ufore_grunnlag samordning_ufore_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ufore_grunnlag
    ADD CONSTRAINT samordning_ufore_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: samordning_ufore_vurdering_periode samordning_ufore_vurdering_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ufore_vurdering_periode
    ADD CONSTRAINT samordning_ufore_vurdering_periode_pkey PRIMARY KEY (id);


--
-- Name: samordning_ufore_vurdering samordning_ufore_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ufore_vurdering
    ADD CONSTRAINT samordning_ufore_vurdering_pkey PRIMARY KEY (id);


--
-- Name: samordning_vurdering_periode samordning_vurdering_periode_ikke_overlapp_periode; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_vurdering_periode
    ADD CONSTRAINT samordning_vurdering_periode_ikke_overlapp_periode EXCLUDE USING gist (vurdering_id WITH =, periode WITH &&);


--
-- Name: samordning_vurdering_periode samordning_vurdering_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_vurdering_periode
    ADD CONSTRAINT samordning_vurdering_periode_pkey PRIMARY KEY (id);


--
-- Name: samordning_vurdering samordning_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_vurdering
    ADD CONSTRAINT samordning_vurdering_pkey PRIMARY KEY (id);


--
-- Name: samordning_vurderinger samordning_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_vurderinger
    ADD CONSTRAINT samordning_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: samordning_ytelse_grunnlag samordning_ytelse_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelse_grunnlag
    ADD CONSTRAINT samordning_ytelse_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: samordning_ytelse_periode samordning_ytelse_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelse_periode
    ADD CONSTRAINT samordning_ytelse_periode_pkey PRIMARY KEY (id);


--
-- Name: samordning_ytelse samordning_ytelse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelse
    ADD CONSTRAINT samordning_ytelse_pkey PRIMARY KEY (id);


--
-- Name: samordning_ytelser samordning_ytelser_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelser
    ADD CONSTRAINT samordning_ytelser_pkey PRIMARY KEY (id);


--
-- Name: samordning_ytelsevurdering_grunnlag samordning_ytelsevurdering_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelsevurdering_grunnlag
    ADD CONSTRAINT samordning_ytelsevurdering_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: soning_vurdering soning_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.soning_vurdering
    ADD CONSTRAINT soning_vurdering_pkey PRIMARY KEY (id);


--
-- Name: soning_vurderinger soning_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.soning_vurderinger
    ADD CONSTRAINT soning_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: stans_opphor_grunnlag stans_opphor_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_grunnlag
    ADD CONSTRAINT stans_opphor_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: stans_opphor stans_opphor_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor
    ADD CONSTRAINT stans_opphor_pkey PRIMARY KEY (id);


--
-- Name: stans_opphor_set stans_opphor_set_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_set
    ADD CONSTRAINT stans_opphor_set_pkey PRIMARY KEY (id);


--
-- Name: stans_opphor_vurdering stans_opphor_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_vurdering
    ADD CONSTRAINT stans_opphor_vurdering_pkey PRIMARY KEY (id);


--
-- Name: stans_opphor_vurdering_v2 stans_opphor_vurdering_v2_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_vurdering_v2
    ADD CONSTRAINT stans_opphor_vurdering_v2_pkey PRIMARY KEY (id);


--
-- Name: stans_opphor_vurderinger stans_opphor_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_vurderinger
    ADD CONSTRAINT stans_opphor_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: stans_opphor_vurderinger_v2 stans_opphor_vurderinger_v2_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_vurderinger_v2
    ADD CONSTRAINT stans_opphor_vurderinger_v2_pkey PRIMARY KEY (id);


--
-- Name: steg_historikk steg_historikk_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.steg_historikk
    ADD CONSTRAINT steg_historikk_pkey PRIMARY KEY (id);


--
-- Name: student_grunnlag student_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_grunnlag
    ADD CONSTRAINT student_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: student_vurdering student_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_vurdering
    ADD CONSTRAINT student_vurdering_pkey PRIMARY KEY (id);


--
-- Name: student_vurderinger student_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_vurderinger
    ADD CONSTRAINT student_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: svar_fra_andreinstans_grunnlag svar_fra_andreinstans_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.svar_fra_andreinstans_grunnlag
    ADD CONSTRAINT svar_fra_andreinstans_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: svar_fra_andreinstans_vurdering svar_fra_andreinstans_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.svar_fra_andreinstans_vurdering
    ADD CONSTRAINT svar_fra_andreinstans_vurdering_pkey PRIMARY KEY (id);


--
-- Name: sykdom_grunnlag sykdom_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_grunnlag
    ADD CONSTRAINT sykdom_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: sykdom_vurdering_bidiagnoser sykdom_vurdering_bidiagnoser_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_vurdering_bidiagnoser
    ADD CONSTRAINT sykdom_vurdering_bidiagnoser_pkey PRIMARY KEY (id);


--
-- Name: sykdom_vurdering_brev sykdom_vurdering_brev_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_vurdering_brev
    ADD CONSTRAINT sykdom_vurdering_brev_pkey PRIMARY KEY (id);


--
-- Name: sykdom_vurdering sykdom_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_vurdering
    ADD CONSTRAINT sykdom_vurdering_pkey PRIMARY KEY (id);


--
-- Name: sykdom_vurderinger sykdom_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_vurderinger
    ADD CONSTRAINT sykdom_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: sykepenge_erstatning_grunnlag sykepenge_erstatning_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykepenge_erstatning_grunnlag
    ADD CONSTRAINT sykepenge_erstatning_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: sykepenge_vurdering sykepenge_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykepenge_vurdering
    ADD CONSTRAINT sykepenge_vurdering_pkey PRIMARY KEY (id);


--
-- Name: sykepenge_vurderinger sykepenge_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykepenge_vurderinger
    ADD CONSTRAINT sykepenge_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: sykestipend_grunnlag sykestipend_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykestipend_grunnlag
    ADD CONSTRAINT sykestipend_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: sykestipend_vurdering sykestipend_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykestipend_vurdering
    ADD CONSTRAINT sykestipend_vurdering_pkey PRIMARY KEY (id);


--
-- Name: test_automatisk_meldekort_sak test_automatisk_meldekort_sak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_automatisk_meldekort_sak
    ADD CONSTRAINT test_automatisk_meldekort_sak_pkey PRIMARY KEY (sak_id);


--
-- Name: tilbakekrevingsbehandling tilbakekrevingsbehandling_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilbakekrevingsbehandling
    ADD CONSTRAINT tilbakekrevingsbehandling_pkey PRIMARY KEY (id);


--
-- Name: tilbakekrevingshendelse tilbakekrevingshendelse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilbakekrevingshendelse
    ADD CONSTRAINT tilbakekrevingshendelse_pkey PRIMARY KEY (id);


--
-- Name: tilkjent_periode tilkjent_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilkjent_periode
    ADD CONSTRAINT tilkjent_periode_pkey PRIMARY KEY (id);


--
-- Name: tilkjent_ytelse tilkjent_ytelse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilkjent_ytelse
    ADD CONSTRAINT tilkjent_ytelse_pkey PRIMARY KEY (id);


--
-- Name: tilkjent_ytelse_sporing tilkjent_ytelse_sporing_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilkjent_ytelse_sporing
    ADD CONSTRAINT tilkjent_ytelse_sporing_pkey PRIMARY KEY (id);


--
-- Name: tiltakspenger_grunnlag tiltakspenger_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tiltakspenger_grunnlag
    ADD CONSTRAINT tiltakspenger_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: tiltakspenger_periode tiltakspenger_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tiltakspenger_periode
    ADD CONSTRAINT tiltakspenger_periode_pkey PRIMARY KEY (id);


--
-- Name: tiltakspenger_perioder tiltakspenger_perioder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tiltakspenger_perioder
    ADD CONSTRAINT tiltakspenger_perioder_pkey PRIMARY KEY (id);


--
-- Name: tjenestepensjon_forhold_grunnlag tjenestepensjon_forhold_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_forhold_grunnlag
    ADD CONSTRAINT tjenestepensjon_forhold_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: tjenestepensjon_ordning tjenestepensjon_ordning_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_ordning
    ADD CONSTRAINT tjenestepensjon_ordning_pkey PRIMARY KEY (id);


--
-- Name: tjenestepensjon_ordninger tjenestepensjon_ordninger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_ordninger
    ADD CONSTRAINT tjenestepensjon_ordninger_pkey PRIMARY KEY (id);


--
-- Name: tjenestepensjon_refusjonskrav_grunnlag tjenestepensjon_refusjonskrav_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_refusjonskrav_grunnlag
    ADD CONSTRAINT tjenestepensjon_refusjonskrav_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: tjenestepensjon_refusjonskrav_vurdering tjenestepensjon_refusjonskrav_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_refusjonskrav_vurdering
    ADD CONSTRAINT tjenestepensjon_refusjonskrav_vurdering_pkey PRIMARY KEY (id);


--
-- Name: tjenestepensjon_ytelse tjenestepensjon_ytelse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_ytelse
    ADD CONSTRAINT tjenestepensjon_ytelse_pkey PRIMARY KEY (id);


--
-- Name: trekk_klage_grunnlag trekk_klage_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trekk_klage_grunnlag
    ADD CONSTRAINT trekk_klage_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: trekk_klage_vurdering trekk_klage_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trekk_klage_vurdering
    ADD CONSTRAINT trekk_klage_vurdering_pkey PRIMARY KEY (id);


--
-- Name: trukket_soknad_grunnlag trukket_soknad_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trukket_soknad_grunnlag
    ADD CONSTRAINT trukket_soknad_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: trukket_soknad_vurdering trukket_soknad_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trukket_soknad_vurdering
    ADD CONSTRAINT trukket_soknad_vurdering_pkey PRIMARY KEY (id);


--
-- Name: trukket_soknad_vurderinger trukket_soknad_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trukket_soknad_vurderinger
    ADD CONSTRAINT trukket_soknad_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: ufore_gradering ufore_gradering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ufore_gradering
    ADD CONSTRAINT ufore_gradering_pkey PRIMARY KEY (id);


--
-- Name: ufore_grunnlag ufore_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ufore_grunnlag
    ADD CONSTRAINT ufore_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: ufore ufore_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ufore
    ADD CONSTRAINT ufore_pkey PRIMARY KEY (id);


--
-- Name: ufore_soknad_grunnlag ufore_soknad_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ufore_soknad_grunnlag
    ADD CONSTRAINT ufore_soknad_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: underveis_grunnlag underveis_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_grunnlag
    ADD CONSTRAINT underveis_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: underveis_periode underveis_periode_ikke_overlapp_periode; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_periode
    ADD CONSTRAINT underveis_periode_ikke_overlapp_periode EXCLUDE USING gist (perioder_id WITH =, periode WITH &&);


--
-- Name: underveis_periode underveis_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_periode
    ADD CONSTRAINT underveis_periode_pkey PRIMARY KEY (id);


--
-- Name: underveis_perioder underveis_perioder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_perioder
    ADD CONSTRAINT underveis_perioder_pkey PRIMARY KEY (id);


--
-- Name: underveis_sporing underveis_sporing_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_sporing
    ADD CONSTRAINT underveis_sporing_pkey PRIMARY KEY (id);


--
-- Name: avvist_formkrav_varsel uq_avvist_formkrav_varsel_behandling_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avvist_formkrav_varsel
    ADD CONSTRAINT uq_avvist_formkrav_varsel_behandling_id UNIQUE (behandling_id);


--
-- Name: utenlands_periode utenlands_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utenlands_periode
    ADD CONSTRAINT utenlands_periode_pkey PRIMARY KEY (id);


--
-- Name: vedtak vedtak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtak
    ADD CONSTRAINT vedtak_pkey PRIMARY KEY (id);


--
-- Name: vedtakslengde_grunnlag vedtakslengde_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtakslengde_grunnlag
    ADD CONSTRAINT vedtakslengde_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: vedtakslengde_vurdering vedtakslengde_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtakslengde_vurdering
    ADD CONSTRAINT vedtakslengde_vurdering_pkey PRIMARY KEY (id);


--
-- Name: vedtakslengde_vurderinger vedtakslengde_vurderinger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtakslengde_vurderinger
    ADD CONSTRAINT vedtakslengde_vurderinger_pkey PRIMARY KEY (id);


--
-- Name: vilkar_periode vilkar_periode_ikke_overlapp_periode; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkar_periode
    ADD CONSTRAINT vilkar_periode_ikke_overlapp_periode EXCLUDE USING gist (vilkar_id WITH =, periode WITH &&);


--
-- Name: vilkar_periode vilkar_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkar_periode
    ADD CONSTRAINT vilkar_periode_pkey PRIMARY KEY (id);


--
-- Name: vilkar vilkar_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkar
    ADD CONSTRAINT vilkar_pkey PRIMARY KEY (id);


--
-- Name: vilkar_resultat vilkar_resultat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkar_resultat
    ADD CONSTRAINT vilkar_resultat_pkey PRIMARY KEY (id);


--
-- Name: yrkesskade_dato yrkesskade_dato_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_dato
    ADD CONSTRAINT yrkesskade_dato_pkey PRIMARY KEY (id);


--
-- Name: yrkesskade_grunnlag yrkesskade_grunnlag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_grunnlag
    ADD CONSTRAINT yrkesskade_grunnlag_pkey PRIMARY KEY (id);


--
-- Name: yrkesskade_inntekt yrkesskade_inntekt_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_inntekt
    ADD CONSTRAINT yrkesskade_inntekt_pkey PRIMARY KEY (id);


--
-- Name: yrkesskade_inntekter yrkesskade_inntekter_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_inntekter
    ADD CONSTRAINT yrkesskade_inntekter_pkey PRIMARY KEY (id);


--
-- Name: yrkesskade yrkesskade_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade
    ADD CONSTRAINT yrkesskade_pkey PRIMARY KEY (id);


--
-- Name: yrkesskade_relaterte_saker yrkesskade_relaterte_saker_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_relaterte_saker
    ADD CONSTRAINT yrkesskade_relaterte_saker_pkey PRIMARY KEY (id);


--
-- Name: yrkesskade_vurdering yrkesskade_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_vurdering
    ADD CONSTRAINT yrkesskade_vurdering_pkey PRIMARY KEY (id);


--
-- Name: aktivitetsplikt_11_7_grunnlag_behandling_uindex; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX aktivitetsplikt_11_7_grunnlag_behandling_uindex ON public.aktivitetsplikt_11_7_grunnlag USING btree (behandling_id) WHERE aktiv;


--
-- Name: aktivitetsplikt_11_9_grunnlag_behandling_uindex; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX aktivitetsplikt_11_9_grunnlag_behandling_uindex ON public.aktivitetsplikt_11_9_grunnlag USING btree (behandling_id) WHERE aktiv;


--
-- Name: behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX behandling_id_idx ON public.meldeperiode_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_arbeid_arbeider_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_arbeid_arbeider_id ON public.arbeid USING btree (arbeider_id);


--
-- Name: idx_arbeid_detaljer_arbeid_forutgaaende_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_arbeid_detaljer_arbeid_forutgaaende_id ON public.arbeid_detaljer USING btree (arbeid_forutgaaende_id);


--
-- Name: idx_arbeid_forutgaaende_arbeider_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_arbeid_forutgaaende_arbeider_id ON public.arbeid_forutgaaende USING btree (arbeider_id);


--
-- Name: idx_arbeidsevne_vurdering_arbeidsevne_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_arbeidsevne_vurdering_arbeidsevne_id ON public.arbeidsevne_vurdering USING btree (arbeidsevne_id);


--
-- Name: idx_arbeidsopptrapping_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_arbeidsopptrapping_grunnlag_behandling_id ON public.arbeidsopptrapping_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_arbeidsopptrapping_vurdering_vurderinger_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_arbeidsopptrapping_vurdering_vurderinger_id ON public.arbeidsopptrapping_vurdering USING btree (vurderinger_id);


--
-- Name: idx_avklaringsbehov_behandling_definisjon; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_avklaringsbehov_behandling_definisjon ON public.avklaringsbehov USING btree (behandling_id, definisjon);


--
-- Name: idx_avklaringsbehov_definisjon; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_avklaringsbehov_definisjon ON public.avklaringsbehov USING btree (definisjon);


--
-- Name: idx_avklaringsbehov_endring_aarsak_tid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_avklaringsbehov_endring_aarsak_tid ON public.avklaringsbehov_endring_aarsak USING btree (opprettet_tid);


--
-- Name: idx_avklaringsbehov_endring_aarsak_tid_2; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_avklaringsbehov_endring_aarsak_tid_2 ON public.avklaringsbehov_endring_aarsak USING btree (endring_id, opprettet_tid);


--
-- Name: idx_avklaringsbehov_endring_avklaringsbehov_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_avklaringsbehov_endring_avklaringsbehov_id ON public.avklaringsbehov_endring USING btree (avklaringsbehov_id);


--
-- Name: idx_avklaringsbehov_endring_behov_tid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_avklaringsbehov_endring_behov_tid ON public.avklaringsbehov_endring USING btree (avklaringsbehov_id, opprettet_tid DESC);


--
-- Name: idx_avklaringsbehov_endring_tid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_avklaringsbehov_endring_tid ON public.avklaringsbehov_endring USING btree (opprettet_tid);


--
-- Name: idx_avklaringsbehov_tid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_avklaringsbehov_tid ON public.avklaringsbehov USING btree (opprettet_tid);


--
-- Name: idx_barnopplysing_bgb_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_barnopplysing_bgb_id ON public.barnopplysning USING btree (bgb_id);


--
-- Name: idx_barnopplysing_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_barnopplysing_grunnlag_behandling_id ON public.barnopplysning_grunnlag USING btree (behandling_id);


--
-- Name: idx_barnopplysing_grunnlag_oppgitt_barn; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_barnopplysing_grunnlag_oppgitt_barn ON public.barnopplysning_grunnlag USING btree (oppgitt_barn_id);


--
-- Name: idx_barnopplysing_grunnlag_register_barn_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_barnopplysing_grunnlag_register_barn_id ON public.barnopplysning_grunnlag USING btree (register_barn_id);


--
-- Name: idx_barnopplysing_grunnlag_vurderte_barn; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_barnopplysing_grunnlag_vurderte_barn ON public.barnopplysning_grunnlag USING btree (vurderte_barn_id);


--
-- Name: idx_barnopplysing_person_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_barnopplysing_person_id ON public.barnopplysning USING btree (person_id);


--
-- Name: idx_behandling_referanse; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_behandling_referanse ON public.behandling USING btree (referanse);


--
-- Name: idx_behandling_sak_tid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_behandling_sak_tid ON public.behandling USING btree (sak_id, opprettet_tid);


--
-- Name: idx_behandling_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_behandling_status ON public.behandling USING btree (status);


--
-- Name: idx_bistand_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_bistand_grunnlag_behandling_id ON public.bistand_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_bistand_vurdering_vurderinger_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bistand_vurdering_vurderinger_id ON public.bistand USING btree (bistand_vurderinger_id);


--
-- Name: idx_brevbestilling_referanse; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brevbestilling_referanse ON public.brevbestilling USING btree (referanse);


--
-- Name: idx_brudd_aktivitetsplikter_grunnlag_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_brudd_aktivitetsplikter_grunnlag_id ON public.brudd_aktivitetsplikter USING btree (brudd_aktivitetsplikt_grunnlag_id);


--
-- Name: idx_bruker_forutgaaende_utenlandsadresser_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bruker_forutgaaende_utenlandsadresser_id ON public.bruker_utenlandsadresse_forutgaaende USING btree (utenlandsadresser_id);


--
-- Name: idx_bruker_land_landkoder_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bruker_land_landkoder_id ON public.bruker_land_forutgaaende USING btree (landkoder_id);


--
-- Name: idx_bruker_land_lovvalg_landkoder_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bruker_land_lovvalg_landkoder_id ON public.bruker_land USING btree (landkoder_id);


--
-- Name: idx_bruker_personopplysning_landkoder_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bruker_personopplysning_landkoder_id ON public.bruker_personopplysning_forutgaaende USING btree (landkoder_id);


--
-- Name: idx_bruker_personopplysning_statuser_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bruker_personopplysning_statuser_id ON public.bruker_personopplysning_forutgaaende USING btree (statuser_id);


--
-- Name: idx_bruker_statuser_statuser_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bruker_statuser_statuser_id ON public.bruker_statuser_forutgaaende USING btree (statuser_id);


--
-- Name: idx_etablering_egen_virksomhet_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_etablering_egen_virksomhet_grunnlag_behandling_id ON public.etablering_egen_virksomhet_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_etablering_egen_virksomhet_vurdering_vurderinger_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_etablering_egen_virksomhet_vurdering_vurderinger_id ON public.etablering_egen_virksomhet_vurdering USING btree (vurderinger_id);


--
-- Name: idx_forutgaaende_medlemskap_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_forutgaaende_medlemskap_grunnlag_behandling_id ON public.forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag USING btree (behandling_id);


--
-- Name: idx_helseopphold_vurderinger_vurdering; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_helseopphold_vurderinger_vurdering ON public.helseopphold_vurdering USING btree (helseopphold_vurderinger_id);


--
-- Name: idx_informasjonskrav_oppdatert_behandling; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_informasjonskrav_oppdatert_behandling ON public.informasjonskrav_oppdatert USING btree (behandling_id);


--
-- Name: idx_informasjonskrav_oppdatert_sak_id_oppdatert_krav; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_informasjonskrav_oppdatert_sak_id_oppdatert_krav ON public.informasjonskrav_oppdatert USING btree (sak_id, oppdatert, informasjonskrav);


--
-- Name: idx_inntekt_i_norge_forutgaaende_inntekter_i_norge_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inntekt_i_norge_forutgaaende_inntekter_i_norge_id ON public.inntekt_i_norge_forutgaaende USING btree (inntekter_i_norge_id);


--
-- Name: idx_inntekt_i_norge_inntekter_i_norge_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inntekt_i_norge_inntekter_i_norge_id ON public.inntekt_i_norge USING btree (inntekter_i_norge_id);


--
-- Name: idx_inntekt_inntekter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inntekt_inntekter_id ON public.inntekt USING btree (inntekt_id);


--
-- Name: idx_inntektsbortfall_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_inntektsbortfall_grunnlag_behandling_id ON public.inntektsbortfall_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_inntektsbortfall_vurdering_vurderinger_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inntektsbortfall_vurdering_vurderinger_id ON public.inntektsbortfall_vurdering USING btree (vurderinger_id);


--
-- Name: idx_jobb_behandling; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jobb_behandling ON public.jobb USING btree (behandling_id);


--
-- Name: idx_jobb_historikk_jobb_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jobb_historikk_jobb_id ON public.jobb_historikk USING btree (jobb_id);


--
-- Name: idx_jobb_historikk_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jobb_historikk_status ON public.jobb_historikk USING btree (status);


--
-- Name: idx_jobb_historikk_tid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jobb_historikk_tid ON public.jobb_historikk USING btree (opprettet_tid);


--
-- Name: idx_jobb_neste_kjoring; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jobb_neste_kjoring ON public.jobb USING btree (neste_kjoring);


--
-- Name: idx_jobb_neste_kjoring_sak_behandling; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jobb_neste_kjoring_sak_behandling ON public.jobb USING btree (sak_id, behandling_id, neste_kjoring);


--
-- Name: idx_jobb_sak; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jobb_sak ON public.jobb USING btree (sak_id);


--
-- Name: idx_jobb_sak_behandling; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jobb_sak_behandling ON public.jobb USING btree (sak_id, behandling_id);


--
-- Name: idx_jobb_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jobb_status ON public.jobb USING btree (status);


--
-- Name: idx_jobb_status_neste_kjoring; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jobb_status_neste_kjoring ON public.jobb USING btree (status, neste_kjoring);


--
-- Name: idx_jobb_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jobb_type ON public.jobb USING btree (type);


--
-- Name: idx_krav_grunnlag_behandling_aktiv; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_krav_grunnlag_behandling_aktiv ON public.krav_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_krav_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_krav_grunnlag_behandling_id ON public.krav_grunnlag USING btree (behandling_id);


--
-- Name: idx_krav_vurdering_vurderinger_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_krav_vurdering_vurderinger_id ON public.krav_vurdering USING btree (krav_vurderinger_id);


--
-- Name: idx_lovvalg_utenlandsadresser_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lovvalg_utenlandsadresser_id ON public.bruker_utenlandsadresse USING btree (utenlandsadresser_id);


--
-- Name: idx_manuell_inntekt_vurderinger_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_manuell_inntekt_vurderinger_id ON public.manuell_inntekt_vurdering USING btree (manuell_inntekt_vurderinger_id);


--
-- Name: idx_medlemskap_arbeid_og_inntekt_i_norge_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_medlemskap_arbeid_og_inntekt_i_norge_grunnlag_behandling_id ON public.medlemskap_arbeid_og_inntekt_i_norge_grunnlag USING btree (behandling_id);


--
-- Name: idx_medlemskap_forutgaaende_unntak_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_medlemskap_forutgaaende_unntak_behandling_id ON public.medlemskap_forutgaaende_unntak_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_medlemskap_unntak_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_medlemskap_unntak_behandling_id ON public.medlemskap_unntak_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_meldekort; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meldekort ON public.meldekort USING btree (journalpost);


--
-- Name: idx_meldekort_meldekortene_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meldekort_meldekortene_id ON public.meldekort USING btree (meldekortene_id);


--
-- Name: idx_meldeplikt_fritak_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_meldeplikt_fritak_grunnlag_behandling_id ON public.meldeplikt_fritak_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_meldeplikt_fritak_vurdering_meldeplikt_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meldeplikt_fritak_vurdering_meldeplikt_id ON public.meldeplikt_fritak_vurdering USING btree (meldeplikt_id);


--
-- Name: idx_meldeplikt_overstyring_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_meldeplikt_overstyring_grunnlag_behandling_id ON public.meldeplikt_overstyring_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_meldeplikt_overstyring_vurdering_perioder_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meldeplikt_overstyring_vurdering_perioder_id ON public.meldeplikt_overstyring_vurdering_perioder USING btree (meldeplikt_overstyring_vurdering_id);


--
-- Name: idx_meldeplikt_overstyring_vurderinger_grunnlag_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meldeplikt_overstyring_vurderinger_grunnlag_id ON public.meldeplikt_overstyring_vurderinger USING btree (grunnlag_id);


--
-- Name: idx_mottatt_dokument_1; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mottatt_dokument_1 ON public.mottatt_dokument USING btree (sak_id, type, status);


--
-- Name: idx_mottatt_dokument_2; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mottatt_dokument_2 ON public.mottatt_dokument USING btree (sak_id, behandling_id);


--
-- Name: idx_mottatt_dokument_3; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mottatt_dokument_3 ON public.mottatt_dokument USING btree (sak_id, mottatt_tid);


--
-- Name: idx_mottatt_dokument_4; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mottatt_dokument_4 ON public.mottatt_dokument USING btree (sak_id, type);


--
-- Name: idx_oppgave_historikk_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oppgave_historikk_status ON public.jobb_historikk USING btree (jobb_id, status);


--
-- Name: idx_oppgave_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oppgave_status ON public.jobb USING btree (status, sak_id, behandling_id, neste_kjoring);


--
-- Name: idx_oppgitt_utenlandsopphold_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oppgitt_utenlandsopphold_grunnlag_behandling_id ON public.oppgitt_utenlandsopphold_grunnlag USING btree (behandling_id);


--
-- Name: idx_opphold_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_opphold_behandling_id ON public.opphold_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_oppholdskrav_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_oppholdskrav_grunnlag_behandling_id ON public.oppholdskrav_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_oppholdskrav_vurdering_periode_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oppholdskrav_vurdering_periode_id ON public.oppholdskrav_vurdering_periode USING btree (oppholdskrav_vurdering_id);


--
-- Name: idx_oppholdskrav_vurderinger_grunnlag_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oppholdskrav_vurderinger_grunnlag_id ON public.oppholdskrav_vurderinger USING btree (grunnlag_id);


--
-- Name: idx_person_ident_ident; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_person_ident_ident ON public.person_ident USING btree (ident);


--
-- Name: idx_person_ident_person_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_person_ident_person_id ON public.person_ident USING btree (person_id);


--
-- Name: idx_person_referanse; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_person_referanse ON public.person USING btree (referanse);


--
-- Name: idx_personopplysning_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_personopplysning_behandling_id ON public.personopplysning_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_personopplysning_forutgaaende_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_personopplysning_forutgaaende_behandling_id ON public.personopplysning_forutgaaende_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_refusjonkrav_vurdering_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refusjonkrav_vurdering_id ON public.refusjonkrav_vurdering USING btree (id);


--
-- Name: idx_sak_id_tilbakekrevingsbehandling; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sak_id_tilbakekrevingsbehandling ON public.tilbakekrevingsbehandling USING btree (sak_id);


--
-- Name: idx_sak_id_tilbakekrevingshendelse; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sak_id_tilbakekrevingshendelse ON public.tilbakekrevingshendelse USING btree (sak_id);


--
-- Name: idx_sak_person; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sak_person ON public.sak USING btree (person_id);


--
-- Name: idx_sak_saksnummer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sak_saksnummer ON public.sak USING btree (saksnummer);


--
-- Name: idx_samordning_arbeidsgiver_vurdering_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_samordning_arbeidsgiver_vurdering_id ON public.samordning_arbeidsgiver_vurdering USING btree (id);


--
-- Name: idx_soning_vurdering_vurderinger; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_soning_vurdering_vurderinger ON public.soning_vurdering USING btree (soning_vurderinger_id);


--
-- Name: idx_steg_historikk_kun_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_steg_historikk_kun_behandling_id ON public.steg_historikk USING btree (behandling_id);


--
-- Name: idx_sykdom_vurdering_bidiagnose_vurderinger_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sykdom_vurdering_bidiagnose_vurderinger_id ON public.sykdom_vurdering_bidiagnoser USING btree (vurdering_id);


--
-- Name: idx_sykdom_vurdering_brev_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_sykdom_vurdering_brev_behandling_id ON public.sykdom_vurdering_brev USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: idx_sykdom_vurdering_vurderinger_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sykdom_vurdering_vurderinger_id ON public.sykdom_vurdering USING btree (sykdom_vurderinger_id);


--
-- Name: idx_tilkjent_periode_tilkjent_ytelse_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tilkjent_periode_tilkjent_ytelse_id ON public.tilkjent_periode USING btree (tilkjent_ytelse_id);


--
-- Name: idx_tilkjent_ytelse_sporing; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tilkjent_ytelse_sporing ON public.tilkjent_ytelse_sporing USING btree (tilkjent_ytelse_id);


--
-- Name: idx_underveis_periode_perioder_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_underveis_periode_perioder_id ON public.underveis_periode USING btree (perioder_id) INCLUDE (periode);


--
-- Name: idx_utenlands_periode_oppgitt_utenlandsopphold_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_utenlands_periode_oppgitt_utenlandsopphold_id ON public.utenlands_periode USING btree (oppgitt_utenlandsopphold_id);


--
-- Name: idx_vilkar_periode_periode; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vilkar_periode_periode ON public.vilkar_periode USING btree (vilkar_id, periode);


--
-- Name: idx_vurderingsbehov_tid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vurderingsbehov_tid ON public.vurderingsbehov USING btree (behandling_id, opprettet_tid);


--
-- Name: idx_yrkesskade_dato_grunnlag_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_yrkesskade_dato_grunnlag_id ON public.yrkesskade_dato USING btree (yrkesskade_id);


--
-- Name: meldeperioder_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX meldeperioder_behandling_id_idx ON public.meldeperiode USING btree (meldeperiodegrunnlag_id);


--
-- Name: reduksjon_11_9_grunnlag_behandling_uindex; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX reduksjon_11_9_grunnlag_behandling_uindex ON public.reduksjon_11_9_grunnlag USING btree (behandling_id) WHERE aktiv;


--
-- Name: uidx_arbeidsevne_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_arbeidsevne_grunnlag_behandling_id ON public.arbeidsevne_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_avbryt_aktivitetspliktbehandling_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_avbryt_aktivitetspliktbehandling_grunnlag_behandling_id ON public.avbryt_aktivitetspliktbehandling_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_avbryt_revurdering_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_avbryt_revurdering_grunnlag_behandling_id ON public.avbryt_revurdering_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_barn_vurdering_ident; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_barn_vurdering_ident ON public.barn_vurdering USING btree (barn_vurderinger_id, ident);


--
-- Name: uidx_barnetillegg_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_barnetillegg_grunnlag_behandling_id ON public.barnetillegg_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_barnopplysning_grunnlag_historikk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_barnopplysning_grunnlag_historikk ON public.barnopplysning_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_behandlende_enhet_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_behandlende_enhet_grunnlag_behandling_id ON public.behandlende_enhet_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_beregning_ufore_type; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_beregning_ufore_type ON public.beregning_ufore USING btree (beregning_id, type);


--
-- Name: uidx_beregningsgrunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_beregningsgrunnlag_behandling_id ON public.beregningsgrunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_beregningstidspunkt_grunnlag_historikk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_beregningstidspunkt_grunnlag_historikk ON public.beregningsfakta_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_brudd_aktivitetsplikt_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_brudd_aktivitetsplikt_grunnlag_behandling_id ON public.brudd_aktivitetsplikt_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_formkrav_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_formkrav_grunnlag_behandling_id ON public.formkrav_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_fullmektig_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_fullmektig_grunnlag_behandling_id ON public.fullmektig_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_inntekt_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_inntekt_grunnlag_behandling_id ON public.inntekt_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_inntekt_vurdering_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_inntekt_vurdering_grunnlag_behandling_id ON public.manuell_inntekt_vurdering_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_klage_kontor_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_klage_kontor_grunnlag_behandling_id ON public.klage_kontor_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_klage_nay_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_klage_nay_grunnlag_behandling_id ON public.klage_nay_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_manuell_inntekt_vurdering; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_manuell_inntekt_vurdering ON public.manuell_inntekt_vurdering USING btree (ar, manuell_inntekt_vurderinger_id);


--
-- Name: uidx_meldekort_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_meldekort_grunnlag_behandling_id ON public.meldekort_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_mottatt_dokument_sak_referanse; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_mottatt_dokument_sak_referanse ON public.mottatt_dokument USING btree (sak_id, referanse_type, referanse);


--
-- Name: uidx_oppfolgingsoppgave_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_oppfolgingsoppgave_grunnlag_behandling_id ON public.oppfolgingsoppgave_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_oppgitt_barn_ident; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_oppgitt_barn_ident ON public.oppgitt_barn USING btree (oppgitt_barn_id, ident);


--
-- Name: uidx_paaklaget_behandling_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_paaklaget_behandling_grunnlag_behandling_id ON public.paaklaget_behandling_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_person_ident_duplikat; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_person_ident_duplikat ON public.person_ident USING btree (ident);


--
-- Name: uidx_person_ident_primary; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_person_ident_primary ON public.person_ident USING btree (person_id) WHERE (primaer = true);


--
-- Name: uidx_refusjonkrav_grunnlag_historikk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_refusjonkrav_grunnlag_historikk ON public.refusjonkrav_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_rettighetsperiode_grunnlag; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_rettighetsperiode_grunnlag ON public.rettighetsperiode_grunnlag USING btree (behandling_id) WHERE aktiv;


--
-- Name: uidx_rettighetstype_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_rettighetstype_grunnlag_behandling_id ON public.rettighetstype_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_saksbehandler_oppgitt_barn_ident; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_saksbehandler_oppgitt_barn_ident ON public.barn_saksbehandler_oppgitt USING btree (saksbehandler_oppgitt_barn_id, ident);


--
-- Name: uidx_samording_ytelse_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_samording_ytelse_grunnlag_behandling_id ON public.samordning_ytelse_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_samordning_andre_statlige_ytelser_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_samordning_andre_statlige_ytelser_grunnlag_behandling_id ON public.samordning_andre_statlige_ytelser_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_samordning_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX uidx_samordning_grunnlag_behandling_id ON public.samordning_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_samordning_ufore_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_samordning_ufore_grunnlag_behandling_id ON public.samordning_ufore_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_samordning_vurdering_vurderinger_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX uidx_samordning_vurdering_vurderinger_id ON public.samordning_vurdering USING btree (vurderinger_id);


--
-- Name: uidx_samordning_ytelse_ytelser_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX uidx_samordning_ytelse_ytelser_id ON public.samordning_ytelse USING btree (ytelser_id);


--
-- Name: uidx_samordning_ytelsevurdering_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX uidx_samordning_ytelsevurdering_grunnlag_behandling_id ON public.samordning_ytelsevurdering_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_stans_opphor_grunnlag_aktiv_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_stans_opphor_grunnlag_aktiv_behandling_id ON public.stans_opphor_grunnlag USING btree (behandling_id) WHERE aktiv;


--
-- Name: uidx_steg_historikk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_steg_historikk ON public.steg_historikk USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_student_grunnlag_historikk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_student_grunnlag_historikk ON public.student_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_svar_fra_andreinstans_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_svar_fra_andreinstans_grunnlag_behandling_id ON public.svar_fra_andreinstans_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_sykdom_grunnlag_historikk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_sykdom_grunnlag_historikk ON public.sykdom_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_sykepenge_vurdering_grunnlag_historikk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_sykepenge_vurdering_grunnlag_historikk ON public.sykepenge_erstatning_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_sykestipend_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_sykestipend_grunnlag_behandling_id ON public.sykestipend_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_tilbakekreving_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_tilbakekreving_behandling_id ON public.tilbakekrevingsbehandling USING btree (tilbakekreving_behandling_id);


--
-- Name: uidx_tilkjent_ytelse; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_tilkjent_ytelse ON public.tilkjent_ytelse USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_tjenestepensjon_forhold_grunnlag_historikk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_tjenestepensjon_forhold_grunnlag_historikk ON public.tjenestepensjon_forhold_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_tjenestepensjon_refusjonskrav_historikk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_tjenestepensjon_refusjonskrav_historikk ON public.tjenestepensjon_refusjonskrav_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_trukket_klage_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_trukket_klage_grunnlag_behandling_id ON public.trekk_klage_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_trukket_soknad_grunnlag; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_trukket_soknad_grunnlag ON public.trukket_soknad_grunnlag USING btree (behandling_id) WHERE aktiv;


--
-- Name: uidx_ufore_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_ufore_grunnlag_behandling_id ON public.ufore_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_underveis_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_underveis_grunnlag_behandling_id ON public.underveis_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_vedtak_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_vedtak_behandling_id ON public.vedtak USING btree (behandling_id);


--
-- Name: uidx_vedtakslengde_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_vedtakslengde_grunnlag_behandling_id ON public.vedtakslengde_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_vilkar; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_vilkar ON public.vilkar USING btree (resultat_id, type);


--
-- Name: uidx_vilkar_resultat_historikk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_vilkar_resultat_historikk ON public.vilkar_resultat USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_yrkesskade_grunnlag_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_yrkesskade_grunnlag_behandling_id ON public.yrkesskade_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_yrkesskade_inntekt_saker; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_yrkesskade_inntekt_saker ON public.yrkesskade_inntekt USING btree (referanse, inntekter_id);


--
-- Name: uidx_yrkesskade_relaterte_saker; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_yrkesskade_relaterte_saker ON public.yrkesskade_relaterte_saker USING btree (referanse, vurdering_id);


--
-- Name: uniq_active_grunnlag_per_behandling; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uniq_active_grunnlag_per_behandling ON public.dagpenger_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: uniq_active_tiltakspenger_grunnlag_per_behandling; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uniq_active_tiltakspenger_grunnlag_per_behandling ON public.tiltakspenger_grunnlag USING btree (behandling_id) WHERE (aktiv = true);


--
-- Name: vurderingsbehov aarsak_til_behandling_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vurderingsbehov
    ADD CONSTRAINT aarsak_til_behandling_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: aktivitetsplikt_11_7_grunnlag aktivitetsplikt_11_7_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_7_grunnlag
    ADD CONSTRAINT aktivitetsplikt_11_7_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: aktivitetsplikt_11_7_grunnlag aktivitetsplikt_11_7_grunnlag_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_7_grunnlag
    ADD CONSTRAINT aktivitetsplikt_11_7_grunnlag_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.aktivitetsplikt_11_7_vurderinger(id);


--
-- Name: aktivitetsplikt_11_7_varsel aktivitetsplikt_11_7_varsel_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_7_varsel
    ADD CONSTRAINT aktivitetsplikt_11_7_varsel_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: aktivitetsplikt_11_7_vurdering aktivitetsplikt_11_7_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_7_vurdering
    ADD CONSTRAINT aktivitetsplikt_11_7_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.aktivitetsplikt_11_7_vurderinger(id);


--
-- Name: aktivitetsplikt_11_7_vurdering aktivitetsplikt_11_7_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_7_vurdering
    ADD CONSTRAINT aktivitetsplikt_11_7_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: aktivitetsplikt_11_9_grunnlag aktivitetsplikt_11_9_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_9_grunnlag
    ADD CONSTRAINT aktivitetsplikt_11_9_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: aktivitetsplikt_11_9_grunnlag aktivitetsplikt_11_9_grunnlag_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_9_grunnlag
    ADD CONSTRAINT aktivitetsplikt_11_9_grunnlag_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.aktivitetsplikt_11_9_vurderinger(id);


--
-- Name: aktivitetsplikt_11_9_vurdering aktivitetsplikt_11_9_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_9_vurdering
    ADD CONSTRAINT aktivitetsplikt_11_9_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.aktivitetsplikt_11_9_vurderinger(id);


--
-- Name: aktivitetsplikt_11_9_vurdering aktivitetsplikt_11_9_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktivitetsplikt_11_9_vurdering
    ADD CONSTRAINT aktivitetsplikt_11_9_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: andre_ytelser_oppgitt_i_soknad_grunnlag andre_ytelser_oppgitt_i_soknad_grunnlag_andre_ytelser_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.andre_ytelser_oppgitt_i_soknad_grunnlag
    ADD CONSTRAINT andre_ytelser_oppgitt_i_soknad_grunnlag_andre_ytelser_id_fkey FOREIGN KEY (andre_ytelser_id) REFERENCES public.andre_ytelser_svar_i_soknad(id);


--
-- Name: andre_ytelser_oppgitt_i_soknad_grunnlag andre_ytelser_oppgitt_i_soknad_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.andre_ytelser_oppgitt_i_soknad_grunnlag
    ADD CONSTRAINT andre_ytelser_oppgitt_i_soknad_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: annen_ytelse_oppgitt_i_soknad annen_ytelse_oppgitt_i_soknad_andre_ytelser_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.annen_ytelse_oppgitt_i_soknad
    ADD CONSTRAINT annen_ytelse_oppgitt_i_soknad_andre_ytelser_id_fkey FOREIGN KEY (andre_ytelser_id) REFERENCES public.andre_ytelser_svar_i_soknad(id);


--
-- Name: arbeid arbeid_arbeider_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeid
    ADD CONSTRAINT arbeid_arbeider_id_fkey FOREIGN KEY (arbeider_id) REFERENCES public.arbeider(id);


--
-- Name: arbeid_detaljer arbeid_detaljer_arbeid_forutgaaende_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeid_detaljer
    ADD CONSTRAINT arbeid_detaljer_arbeid_forutgaaende_id_fkey FOREIGN KEY (arbeid_forutgaaende_id) REFERENCES public.arbeid_forutgaaende(id);


--
-- Name: arbeid_forutgaaende arbeid_forutgaaende_arbeider_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeid_forutgaaende
    ADD CONSTRAINT arbeid_forutgaaende_arbeider_id_fkey FOREIGN KEY (arbeider_id) REFERENCES public.arbeider_forutgaaende(id);


--
-- Name: arbeidsevne_grunnlag arbeidsevne_grunnlag_arbeidsevne_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsevne_grunnlag
    ADD CONSTRAINT arbeidsevne_grunnlag_arbeidsevne_id_fkey FOREIGN KEY (arbeidsevne_id) REFERENCES public.arbeidsevne(id);


--
-- Name: arbeidsevne_grunnlag arbeidsevne_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsevne_grunnlag
    ADD CONSTRAINT arbeidsevne_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: arbeidsevne_vurdering arbeidsevne_vurdering_arbeidsevne_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsevne_vurdering
    ADD CONSTRAINT arbeidsevne_vurdering_arbeidsevne_id_fkey FOREIGN KEY (arbeidsevne_id) REFERENCES public.arbeidsevne(id);


--
-- Name: arbeidsevne_vurdering arbeidsevne_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsevne_vurdering
    ADD CONSTRAINT arbeidsevne_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: arbeidsopptrapping_grunnlag arbeidsopptrapping_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsopptrapping_grunnlag
    ADD CONSTRAINT arbeidsopptrapping_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: arbeidsopptrapping_grunnlag arbeidsopptrapping_grunnlag_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsopptrapping_grunnlag
    ADD CONSTRAINT arbeidsopptrapping_grunnlag_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.arbeidsopptrapping_vurderinger(id);


--
-- Name: arbeidsopptrapping_vurdering arbeidsopptrapping_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsopptrapping_vurdering
    ADD CONSTRAINT arbeidsopptrapping_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.arbeidsopptrapping_vurderinger(id);


--
-- Name: arbeidsopptrapping_vurdering arbeidsopptrapping_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsopptrapping_vurdering
    ADD CONSTRAINT arbeidsopptrapping_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: avbryt_aktivitetspliktbehandling_grunnlag avbryt_aktivitetspliktbehandling_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avbryt_aktivitetspliktbehandling_grunnlag
    ADD CONSTRAINT avbryt_aktivitetspliktbehandling_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: avbryt_aktivitetspliktbehandling_grunnlag avbryt_aktivitetspliktbehandling_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avbryt_aktivitetspliktbehandling_grunnlag
    ADD CONSTRAINT avbryt_aktivitetspliktbehandling_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.avbryt_aktivitetspliktbehandling_vurdering(id);


--
-- Name: avbryt_revurdering_grunnlag avbryt_revurdering_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avbryt_revurdering_grunnlag
    ADD CONSTRAINT avbryt_revurdering_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.avbryt_revurdering_vurdering(id);


--
-- Name: avklaringsbehov avklaringsbehov_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avklaringsbehov
    ADD CONSTRAINT avklaringsbehov_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: avklaringsbehov_endring_aarsak avklaringsbehov_endring_aarsak_endring_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avklaringsbehov_endring_aarsak
    ADD CONSTRAINT avklaringsbehov_endring_aarsak_endring_id_fkey FOREIGN KEY (endring_id) REFERENCES public.avklaringsbehov_endring(id);


--
-- Name: avklaringsbehov_endring avklaringsbehov_endring_avklaringsbehov_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avklaringsbehov_endring
    ADD CONSTRAINT avklaringsbehov_endring_avklaringsbehov_id_fkey FOREIGN KEY (avklaringsbehov_id) REFERENCES public.avklaringsbehov(id);


--
-- Name: barn_saksbehandler_oppgitt barn_saksbehandler_oppgitt_saksbehandler_oppgitt_barn_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_saksbehandler_oppgitt
    ADD CONSTRAINT barn_saksbehandler_oppgitt_saksbehandler_oppgitt_barn_id_fkey FOREIGN KEY (saksbehandler_oppgitt_barn_id) REFERENCES public.barn_saksbehandler_oppgitt_barnopplysning(id);


--
-- Name: barn_tillegg barn_tillegg_barnetillegg_periode_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_tillegg
    ADD CONSTRAINT barn_tillegg_barnetillegg_periode_id_fkey FOREIGN KEY (barnetillegg_periode_id) REFERENCES public.barnetillegg_periode(id);


--
-- Name: barn_vurdering barn_vurdering_barn_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_vurdering
    ADD CONSTRAINT barn_vurdering_barn_vurderinger_id_fkey FOREIGN KEY (barn_vurderinger_id) REFERENCES public.barn_vurderinger(id);


--
-- Name: barn_vurdering_periode barn_vurdering_periode_barn_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barn_vurdering_periode
    ADD CONSTRAINT barn_vurdering_periode_barn_vurdering_id_fkey FOREIGN KEY (barn_vurdering_id) REFERENCES public.barn_vurdering(id);


--
-- Name: barnetillegg_grunnlag barnetillegg_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnetillegg_grunnlag
    ADD CONSTRAINT barnetillegg_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: barnetillegg_grunnlag barnetillegg_grunnlag_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnetillegg_grunnlag
    ADD CONSTRAINT barnetillegg_grunnlag_perioder_id_fkey FOREIGN KEY (perioder_id) REFERENCES public.barnetillegg_perioder(id);


--
-- Name: barnetillegg_periode barnetillegg_periode_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnetillegg_periode
    ADD CONSTRAINT barnetillegg_periode_perioder_id_fkey FOREIGN KEY (perioder_id) REFERENCES public.barnetillegg_perioder(id);


--
-- Name: barnopplysning barnopplysning_bgb_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnopplysning
    ADD CONSTRAINT barnopplysning_bgb_id_fkey FOREIGN KEY (bgb_id) REFERENCES public.barnopplysning_grunnlag_barnopplysning(id);


--
-- Name: barnopplysning_grunnlag barnopplysning_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnopplysning_grunnlag
    ADD CONSTRAINT barnopplysning_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: barnopplysning_grunnlag barnopplysning_grunnlag_bgb_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnopplysning_grunnlag
    ADD CONSTRAINT barnopplysning_grunnlag_bgb_id_fkey FOREIGN KEY (register_barn_id) REFERENCES public.barnopplysning_grunnlag_barnopplysning(id);


--
-- Name: barnopplysning_grunnlag barnopplysning_grunnlag_oppgitt_barn_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnopplysning_grunnlag
    ADD CONSTRAINT barnopplysning_grunnlag_oppgitt_barn_id_fkey FOREIGN KEY (oppgitt_barn_id) REFERENCES public.oppgitt_barnopplysning(id);


--
-- Name: barnopplysning_grunnlag barnopplysning_grunnlag_saksbehandler_oppgitt_barn_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnopplysning_grunnlag
    ADD CONSTRAINT barnopplysning_grunnlag_saksbehandler_oppgitt_barn_id_fkey FOREIGN KEY (saksbehandler_oppgitt_barn_id) REFERENCES public.barn_saksbehandler_oppgitt_barnopplysning(id);


--
-- Name: barnopplysning_grunnlag barnopplysning_grunnlag_vurderte_barn_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnopplysning_grunnlag
    ADD CONSTRAINT barnopplysning_grunnlag_vurderte_barn_id_fkey FOREIGN KEY (vurderte_barn_id) REFERENCES public.barn_vurderinger(id);


--
-- Name: barnopplysning barnopplysning_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.barnopplysning
    ADD CONSTRAINT barnopplysning_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.person(id);


--
-- Name: behandlende_enhet_grunnlag behandlende_enhet_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandlende_enhet_grunnlag
    ADD CONSTRAINT behandlende_enhet_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: behandlende_enhet_grunnlag behandlende_enhet_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandlende_enhet_grunnlag
    ADD CONSTRAINT behandlende_enhet_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.behandlende_enhet_vurdering(id);


--
-- Name: behandling_aarsak behandling_aarsak_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling_aarsak
    ADD CONSTRAINT behandling_aarsak_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: behandling behandling_forrige_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling
    ADD CONSTRAINT behandling_forrige_id_fkey FOREIGN KEY (forrige_id) REFERENCES public.behandling(id);


--
-- Name: behandling behandling_sak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling
    ADD CONSTRAINT behandling_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES public.sak(id);


--
-- Name: beregning_hoved beregning_hoved_beregning_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_hoved
    ADD CONSTRAINT beregning_hoved_beregning_id_fkey FOREIGN KEY (beregning_id) REFERENCES public.beregning(id);


--
-- Name: beregning_inntekt beregning_inntekt_beregning_hoved_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_inntekt
    ADD CONSTRAINT beregning_inntekt_beregning_hoved_id_fkey FOREIGN KEY (beregning_hoved_id) REFERENCES public.beregning_hoved(id);


--
-- Name: beregning_ufore beregning_ufore_beregning_hoved_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore
    ADD CONSTRAINT beregning_ufore_beregning_hoved_id_fkey FOREIGN KEY (beregning_hoved_id) REFERENCES public.beregning_hoved(id);


--
-- Name: beregning_ufore beregning_ufore_beregning_hoved_ytterligere_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore
    ADD CONSTRAINT beregning_ufore_beregning_hoved_ytterligere_id_fkey FOREIGN KEY (beregning_hoved_ytterligere_id) REFERENCES public.beregning_hoved(id);


--
-- Name: beregning_ufore beregning_ufore_beregning_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore
    ADD CONSTRAINT beregning_ufore_beregning_id_fkey FOREIGN KEY (beregning_id) REFERENCES public.beregning(id);


--
-- Name: beregning_ufore_inntekt beregning_ufore_inntekt_beregning_ufore_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore_inntekt
    ADD CONSTRAINT beregning_ufore_inntekt_beregning_ufore_id_fkey FOREIGN KEY (beregning_ufore_id) REFERENCES public.beregning_ufore(id);


--
-- Name: beregning_ufore_tidsperiode beregning_ufore_tidsperiode_beregning_ufore_inntekt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore_tidsperiode
    ADD CONSTRAINT beregning_ufore_tidsperiode_beregning_ufore_inntekt_id_fkey FOREIGN KEY (beregning_ufore_inntekt_id) REFERENCES public.beregning_ufore_inntekt(id);


--
-- Name: beregning_ufore_uforegrader beregning_ufore_uforegrader_beregning_ufore_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_ufore_uforegrader
    ADD CONSTRAINT beregning_ufore_uforegrader_beregning_ufore_id_fkey FOREIGN KEY (beregning_ufore_id) REFERENCES public.beregning_ufore(id);


--
-- Name: beregning_yrkesskade beregning_yrkesskade_beregning_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregning_yrkesskade
    ADD CONSTRAINT beregning_yrkesskade_beregning_id_fkey FOREIGN KEY (beregning_id) REFERENCES public.beregning(id);


--
-- Name: beregningsfakta_grunnlag beregningsfakta_grunnlag_yrkesskade_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregningsfakta_grunnlag
    ADD CONSTRAINT beregningsfakta_grunnlag_yrkesskade_vurdering_id_fkey FOREIGN KEY (yrkesskade_vurdering_id) REFERENCES public.yrkesskade_inntekter(id);


--
-- Name: beregningsgrunnlag beregningsgrunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregningsgrunnlag
    ADD CONSTRAINT beregningsgrunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: beregningsgrunnlag beregningsgrunnlag_beregning_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregningsgrunnlag
    ADD CONSTRAINT beregningsgrunnlag_beregning_id_fkey FOREIGN KEY (beregning_id) REFERENCES public.beregning(id);


--
-- Name: beregningsfakta_grunnlag beregningstidspunkt_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregningsfakta_grunnlag
    ADD CONSTRAINT beregningstidspunkt_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: beregningsfakta_grunnlag beregningstidspunkt_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beregningsfakta_grunnlag
    ADD CONSTRAINT beregningstidspunkt_grunnlag_vurdering_id_fkey FOREIGN KEY (tidspunkt_vurdering_id) REFERENCES public.beregningstidspunkt_vurdering(id);


--
-- Name: bistand bistand_bistand_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bistand
    ADD CONSTRAINT bistand_bistand_vurderinger_id_fkey FOREIGN KEY (bistand_vurderinger_id) REFERENCES public.bistand_vurderinger(id);


--
-- Name: bistand_grunnlag bistand_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bistand_grunnlag
    ADD CONSTRAINT bistand_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: bistand_grunnlag bistand_grunnlag_bistand_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bistand_grunnlag
    ADD CONSTRAINT bistand_grunnlag_bistand_vurderinger_id_fkey FOREIGN KEY (bistand_vurderinger_id) REFERENCES public.bistand_vurderinger(id);


--
-- Name: bistand bistand_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bistand
    ADD CONSTRAINT bistand_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: brevbestilling brevbestilling_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brevbestilling
    ADD CONSTRAINT brevbestilling_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: brudd_aktivitetsplikt_grunnlag brudd_aktivitetsplikt_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brudd_aktivitetsplikt_grunnlag
    ADD CONSTRAINT brudd_aktivitetsplikt_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: brudd_aktivitetsplikt brudd_aktivitetsplikt_sak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brudd_aktivitetsplikt
    ADD CONSTRAINT brudd_aktivitetsplikt_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES public.sak(id);


--
-- Name: brudd_aktivitetsplikter brudd_aktivitetsplikter_brudd_aktivitetsplikt_grunnlag_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brudd_aktivitetsplikter
    ADD CONSTRAINT brudd_aktivitetsplikter_brudd_aktivitetsplikt_grunnlag_id_fkey FOREIGN KEY (brudd_aktivitetsplikt_grunnlag_id) REFERENCES public.brudd_aktivitetsplikt_grunnlag(id);


--
-- Name: brudd_aktivitetsplikter brudd_aktivitetsplikter_brudd_aktivitetsplikt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brudd_aktivitetsplikter
    ADD CONSTRAINT brudd_aktivitetsplikter_brudd_aktivitetsplikt_id_fkey FOREIGN KEY (brudd_aktivitetsplikt_id) REFERENCES public.brudd_aktivitetsplikt(id);


--
-- Name: bruker_land_forutgaaende bruker_land_forutgaaende_landkoder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_land_forutgaaende
    ADD CONSTRAINT bruker_land_forutgaaende_landkoder_id_fkey FOREIGN KEY (landkoder_id) REFERENCES public.bruker_land_forutgaaende_aggregat(id);


--
-- Name: bruker_land bruker_land_landkoder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_land
    ADD CONSTRAINT bruker_land_landkoder_id_fkey FOREIGN KEY (landkoder_id) REFERENCES public.bruker_land_aggregat(id);


--
-- Name: bruker_personopplysning_forutgaaende bruker_personopplysning_forutgaaende_landkoder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_personopplysning_forutgaaende
    ADD CONSTRAINT bruker_personopplysning_forutgaaende_landkoder_id_fkey FOREIGN KEY (landkoder_id) REFERENCES public.bruker_land_forutgaaende_aggregat(id);


--
-- Name: bruker_personopplysning_forutgaaende bruker_personopplysning_forutgaaende_statuser_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_personopplysning_forutgaaende
    ADD CONSTRAINT bruker_personopplysning_forutgaaende_statuser_id_fkey FOREIGN KEY (statuser_id) REFERENCES public.bruker_statuser_forutgaaende_aggregat(id);


--
-- Name: bruker_personopplysning_forutgaaende bruker_personopplysning_forutgaaende_utenlandsadresser_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_personopplysning_forutgaaende
    ADD CONSTRAINT bruker_personopplysning_forutgaaende_utenlandsadresser_id_fkey FOREIGN KEY (utenlandsadresser_id) REFERENCES public.bruker_utenlandsadresser_forutgaaende_aggregat(id);


--
-- Name: bruker_personopplysning bruker_personopplysning_landkoder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_personopplysning
    ADD CONSTRAINT bruker_personopplysning_landkoder_id_fkey FOREIGN KEY (landkoder_id) REFERENCES public.bruker_land_aggregat(id);


--
-- Name: bruker_personopplysning bruker_personopplysning_utenlandsadresser_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_personopplysning
    ADD CONSTRAINT bruker_personopplysning_utenlandsadresser_id_fkey FOREIGN KEY (utenlandsadresser_id) REFERENCES public.bruker_utenlandsadresser_aggregat(id);


--
-- Name: bruker_statuser_forutgaaende bruker_statuser_forutgaaende_statuser_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_statuser_forutgaaende
    ADD CONSTRAINT bruker_statuser_forutgaaende_statuser_id_fkey FOREIGN KEY (statuser_id) REFERENCES public.bruker_statuser_forutgaaende_aggregat(id);


--
-- Name: bruker_utenlandsadresse_forutgaaende bruker_utenlandsadresse_forutgaaende_utenlandsadresser_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_utenlandsadresse_forutgaaende
    ADD CONSTRAINT bruker_utenlandsadresse_forutgaaende_utenlandsadresser_id_fkey FOREIGN KEY (utenlandsadresser_id) REFERENCES public.bruker_utenlandsadresser_forutgaaende_aggregat(id);


--
-- Name: bruker_utenlandsadresse bruker_utenlandsadresse_utenlandsadresser_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_utenlandsadresse
    ADD CONSTRAINT bruker_utenlandsadresse_utenlandsadresser_id_fkey FOREIGN KEY (utenlandsadresser_id) REFERENCES public.bruker_utenlandsadresser_aggregat(id);


--
-- Name: dagpenger_grunnlag dagpenger_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dagpenger_grunnlag
    ADD CONSTRAINT dagpenger_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: dagpenger_grunnlag dagpenger_grunnlag_dagpenger_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dagpenger_grunnlag
    ADD CONSTRAINT dagpenger_grunnlag_dagpenger_perioder_id_fkey FOREIGN KEY (dagpenger_perioder_id) REFERENCES public.dagpenger_perioder(id);


--
-- Name: dagpenger_periode dagpenger_periode_dagpenger_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dagpenger_periode
    ADD CONSTRAINT dagpenger_periode_dagpenger_perioder_id_fkey FOREIGN KEY (dagpenger_perioder_id) REFERENCES public.dagpenger_perioder(id);


--
-- Name: egen_virksomhet_oppstart_periode egen_virksomhet_oppstart_periode_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.egen_virksomhet_oppstart_periode
    ADD CONSTRAINT egen_virksomhet_oppstart_periode_perioder_id_fkey FOREIGN KEY (perioder_id) REFERENCES public.egen_virksomhet_oppstart_perioder(id);


--
-- Name: egen_virksomhet_utvikling_periode egen_virksomhet_utvikling_periode_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.egen_virksomhet_utvikling_periode
    ADD CONSTRAINT egen_virksomhet_utvikling_periode_perioder_id_fkey FOREIGN KEY (perioder_id) REFERENCES public.egen_virksomhet_utvikling_perioder(id);


--
-- Name: etablering_egen_virksomhet_grunnlag etablering_egen_virksomhet_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etablering_egen_virksomhet_grunnlag
    ADD CONSTRAINT etablering_egen_virksomhet_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: etablering_egen_virksomhet_grunnlag etablering_egen_virksomhet_grunnlag_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etablering_egen_virksomhet_grunnlag
    ADD CONSTRAINT etablering_egen_virksomhet_grunnlag_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.etablering_egen_virksomhet_vurderinger(id);


--
-- Name: etablering_egen_virksomhet_vurdering etablering_egen_virksomhet_vu_egen_virksomhet_oppstart_per_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etablering_egen_virksomhet_vurdering
    ADD CONSTRAINT etablering_egen_virksomhet_vu_egen_virksomhet_oppstart_per_fkey FOREIGN KEY (egen_virksomhet_oppstart_perioder_id) REFERENCES public.egen_virksomhet_oppstart_perioder(id);


--
-- Name: etablering_egen_virksomhet_vurdering etablering_egen_virksomhet_vu_egen_virksomhet_utvikling_pe_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etablering_egen_virksomhet_vurdering
    ADD CONSTRAINT etablering_egen_virksomhet_vu_egen_virksomhet_utvikling_pe_fkey FOREIGN KEY (egen_virksomhet_utvikling_perioder_id) REFERENCES public.egen_virksomhet_utvikling_perioder(id);


--
-- Name: etablering_egen_virksomhet_vurdering etablering_egen_virksomhet_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etablering_egen_virksomhet_vurdering
    ADD CONSTRAINT etablering_egen_virksomhet_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.etablering_egen_virksomhet_vurderinger(id);


--
-- Name: etablering_egen_virksomhet_vurdering etablering_egen_virksomhet_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etablering_egen_virksomhet_vurdering
    ADD CONSTRAINT etablering_egen_virksomhet_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: avvist_formkrav_varsel fk_avvist_formkrav_varsel_behandling; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avvist_formkrav_varsel
    ADD CONSTRAINT fk_avvist_formkrav_varsel_behandling FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: formkrav_grunnlag formkrav_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.formkrav_grunnlag
    ADD CONSTRAINT formkrav_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: formkrav_grunnlag formkrav_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.formkrav_grunnlag
    ADD CONSTRAINT formkrav_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.formkrav_vurdering(id);


--
-- Name: forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag forutgaaende_grunnlag_inntekter_i_norge_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag
    ADD CONSTRAINT forutgaaende_grunnlag_inntekter_i_norge_id_fkey FOREIGN KEY (inntekter_i_norge_id) REFERENCES public.inntekter_i_norge_forutgaaende(id);


--
-- Name: forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag forutgaaende_medlemskap_arbeid_medlemskap_unntak_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag
    ADD CONSTRAINT forutgaaende_medlemskap_arbeid_medlemskap_unntak_person_id_fkey FOREIGN KEY (medlemskap_unntak_person_id) REFERENCES public.medlemskap_forutgaaende_unntak_person(id);


--
-- Name: forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag forutgaaende_medlemskap_arbeid_og_inntekt_i__behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag
    ADD CONSTRAINT forutgaaende_medlemskap_arbeid_og_inntekt_i__behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag forutgaaende_medlemskap_arbeid_og_inntekt_i_no_arbeider_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag
    ADD CONSTRAINT forutgaaende_medlemskap_arbeid_og_inntekt_i_no_arbeider_id_fkey FOREIGN KEY (arbeider_id) REFERENCES public.arbeider_forutgaaende(id);


--
-- Name: forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag forutgaaende_medlemskap_arbeid_og_inntekt_i_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forutgaaende_medlemskap_arbeid_og_inntekt_i_norge_grunnlag
    ADD CONSTRAINT forutgaaende_medlemskap_arbeid_og_inntekt_i_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.forutgaaende_medlemskap_manuell_vurderinger(id);


--
-- Name: forutgaaende_medlemskap_manuell_vurdering forutgaaende_medlemskap_manuell_vurde_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forutgaaende_medlemskap_manuell_vurdering
    ADD CONSTRAINT forutgaaende_medlemskap_manuell_vurde_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: forutgaaende_medlemskap_manuell_vurdering forutgaaende_medlemskap_manuell_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forutgaaende_medlemskap_manuell_vurdering
    ADD CONSTRAINT forutgaaende_medlemskap_manuell_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.forutgaaende_medlemskap_manuell_vurderinger(id);


--
-- Name: fullmektig_grunnlag fullmektig_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fullmektig_grunnlag
    ADD CONSTRAINT fullmektig_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: fullmektig_grunnlag fullmektig_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fullmektig_grunnlag
    ADD CONSTRAINT fullmektig_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.fullmektig_vurdering(id);


--
-- Name: helseopphold_vurdering helseopphold_vurdering_helseopphold_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.helseopphold_vurdering
    ADD CONSTRAINT helseopphold_vurdering_helseopphold_vurderinger_id_fkey FOREIGN KEY (helseopphold_vurderinger_id) REFERENCES public.helseopphold_vurderinger(id);


--
-- Name: helseopphold_vurdering helseopphold_vurdering_opphold_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.helseopphold_vurdering
    ADD CONSTRAINT helseopphold_vurdering_opphold_id_fkey FOREIGN KEY (opphold_id) REFERENCES public.opphold(id);


--
-- Name: helseopphold_vurdering helseopphold_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.helseopphold_vurdering
    ADD CONSTRAINT helseopphold_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: informasjonskrav_oppdatert informasjonskrav_oppdatert_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.informasjonskrav_oppdatert
    ADD CONSTRAINT informasjonskrav_oppdatert_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: informasjonskrav_oppdatert informasjonskrav_oppdatert_sak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.informasjonskrav_oppdatert
    ADD CONSTRAINT informasjonskrav_oppdatert_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES public.sak(id);


--
-- Name: inntekt_grunnlag inntekt_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt_grunnlag
    ADD CONSTRAINT inntekt_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: inntekt_grunnlag inntekt_grunnlag_inntekt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt_grunnlag
    ADD CONSTRAINT inntekt_grunnlag_inntekt_id_fkey FOREIGN KEY (inntekt_id) REFERENCES public.inntekter(id);


--
-- Name: inntekt_i_norge_forutgaaende inntekt_i_norge_forutgaaende_inntekter_i_norge_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt_i_norge_forutgaaende
    ADD CONSTRAINT inntekt_i_norge_forutgaaende_inntekter_i_norge_id_fkey FOREIGN KEY (inntekter_i_norge_id) REFERENCES public.inntekter_i_norge_forutgaaende(id);


--
-- Name: inntekt_i_norge inntekt_i_norge_inntekter_i_norge_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt_i_norge
    ADD CONSTRAINT inntekt_i_norge_inntekter_i_norge_id_fkey FOREIGN KEY (inntekter_i_norge_id) REFERENCES public.inntekter_i_norge(id);


--
-- Name: inntekt inntekt_inntekt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt
    ADD CONSTRAINT inntekt_inntekt_id_fkey FOREIGN KEY (inntekt_id) REFERENCES public.inntekter(id);


--
-- Name: inntekt_periode inntekt_periode_inntekt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntekt_periode
    ADD CONSTRAINT inntekt_periode_inntekt_id_fkey FOREIGN KEY (inntekt_id) REFERENCES public.inntekter(id);


--
-- Name: inntektsbortfall_grunnlag inntektsbortfall_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntektsbortfall_grunnlag
    ADD CONSTRAINT inntektsbortfall_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: inntektsbortfall_grunnlag inntektsbortfall_grunnlag_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntektsbortfall_grunnlag
    ADD CONSTRAINT inntektsbortfall_grunnlag_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.inntektsbortfall_vurderinger(id);


--
-- Name: inntektsbortfall_vurdering inntektsbortfall_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntektsbortfall_vurdering
    ADD CONSTRAINT inntektsbortfall_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.inntektsbortfall_vurderinger(id);


--
-- Name: inntektsbortfall_vurdering inntektsbortfall_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inntektsbortfall_vurdering
    ADD CONSTRAINT inntektsbortfall_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: avbryt_revurdering_grunnlag kanseller_revurdering_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avbryt_revurdering_grunnlag
    ADD CONSTRAINT kanseller_revurdering_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: klage_kontor_grunnlag klage_kontor_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.klage_kontor_grunnlag
    ADD CONSTRAINT klage_kontor_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: klage_kontor_grunnlag klage_kontor_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.klage_kontor_grunnlag
    ADD CONSTRAINT klage_kontor_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.klage_kontor_vurdering(id);


--
-- Name: klage_nay_grunnlag klage_nay_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.klage_nay_grunnlag
    ADD CONSTRAINT klage_nay_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: klage_nay_grunnlag klage_nay_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.klage_nay_grunnlag
    ADD CONSTRAINT klage_nay_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.klage_nay_vurdering(id);


--
-- Name: krav_grunnlag krav_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.krav_grunnlag
    ADD CONSTRAINT krav_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: krav_grunnlag krav_grunnlag_krav_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.krav_grunnlag
    ADD CONSTRAINT krav_grunnlag_krav_vurderinger_id_fkey FOREIGN KEY (krav_vurderinger_id) REFERENCES public.krav_vurderinger(id);


--
-- Name: krav_vurdering krav_vurdering_krav_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.krav_vurdering
    ADD CONSTRAINT krav_vurdering_krav_vurderinger_id_fkey FOREIGN KEY (krav_vurderinger_id) REFERENCES public.krav_vurderinger(id);


--
-- Name: krav_vurdering krav_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.krav_vurdering
    ADD CONSTRAINT krav_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: lovvalg_medlemskap_manuell_vurdering lovvalg_medlemskap_manuell_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lovvalg_medlemskap_manuell_vurdering
    ADD CONSTRAINT lovvalg_medlemskap_manuell_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.lovvalg_medlemskap_manuell_vurderinger(id);


--
-- Name: lovvalg_medlemskap_manuell_vurdering lovvalg_medlemskap_manuell_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lovvalg_medlemskap_manuell_vurdering
    ADD CONSTRAINT lovvalg_medlemskap_manuell_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: manuell_inntekt_vurdering_grunnlag manuell_inntekt_vurdering_gru_manuell_inntekt_vurderinger__fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manuell_inntekt_vurdering_grunnlag
    ADD CONSTRAINT manuell_inntekt_vurdering_gru_manuell_inntekt_vurderinger__fkey FOREIGN KEY (manuell_inntekt_vurderinger_id) REFERENCES public.manuell_inntekt_vurderinger(id);


--
-- Name: manuell_inntekt_vurdering_grunnlag manuell_inntekt_vurdering_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manuell_inntekt_vurdering_grunnlag
    ADD CONSTRAINT manuell_inntekt_vurdering_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: manuell_inntekt_vurdering manuell_inntekt_vurdering_manuell_inntekt_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manuell_inntekt_vurdering
    ADD CONSTRAINT manuell_inntekt_vurdering_manuell_inntekt_vurderinger_id_fkey FOREIGN KEY (manuell_inntekt_vurderinger_id) REFERENCES public.manuell_inntekt_vurderinger(id);


--
-- Name: medlemskap_arbeid_og_inntekt_i_norge_grunnlag medlemskap_arbeid_og_inntekt_i_medlemskap_unntak_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_arbeid_og_inntekt_i_norge_grunnlag
    ADD CONSTRAINT medlemskap_arbeid_og_inntekt_i_medlemskap_unntak_person_id_fkey FOREIGN KEY (medlemskap_unntak_person_id) REFERENCES public.medlemskap_unntak_person(id);


--
-- Name: medlemskap_arbeid_og_inntekt_i_norge_grunnlag medlemskap_arbeid_og_inntekt_i_norge__inntekter_i_norge_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_arbeid_og_inntekt_i_norge_grunnlag
    ADD CONSTRAINT medlemskap_arbeid_og_inntekt_i_norge__inntekter_i_norge_id_fkey FOREIGN KEY (inntekter_i_norge_id) REFERENCES public.inntekter_i_norge(id);


--
-- Name: medlemskap_arbeid_og_inntekt_i_norge_grunnlag medlemskap_arbeid_og_inntekt_i_norge_grunnl_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_arbeid_og_inntekt_i_norge_grunnlag
    ADD CONSTRAINT medlemskap_arbeid_og_inntekt_i_norge_grunnl_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.lovvalg_medlemskap_manuell_vurderinger(id);


--
-- Name: medlemskap_arbeid_og_inntekt_i_norge_grunnlag medlemskap_arbeid_og_inntekt_i_norge_grunnla_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_arbeid_og_inntekt_i_norge_grunnlag
    ADD CONSTRAINT medlemskap_arbeid_og_inntekt_i_norge_grunnla_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: medlemskap_arbeid_og_inntekt_i_norge_grunnlag medlemskap_arbeid_og_inntekt_i_norge_grunnlag_arbeider_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_arbeid_og_inntekt_i_norge_grunnlag
    ADD CONSTRAINT medlemskap_arbeid_og_inntekt_i_norge_grunnlag_arbeider_id_fkey FOREIGN KEY (arbeider_id) REFERENCES public.arbeider(id);


--
-- Name: medlemskap_forutgaaende_unntak medlemskap_forutgaaende_unnt_medlemskap_forutgaaende_unnt_fkey1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_forutgaaende_unntak
    ADD CONSTRAINT medlemskap_forutgaaende_unnt_medlemskap_forutgaaende_unnt_fkey1 FOREIGN KEY (medlemskap_forutgaaende_unntak_person_id) REFERENCES public.medlemskap_forutgaaende_unntak_person(id);


--
-- Name: medlemskap_forutgaaende_unntak_grunnlag medlemskap_forutgaaende_unnta_medlemskap_forutgaaende_unnt_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_forutgaaende_unntak_grunnlag
    ADD CONSTRAINT medlemskap_forutgaaende_unnta_medlemskap_forutgaaende_unnt_fkey FOREIGN KEY (medlemskap_forutgaaende_unntak_person_id) REFERENCES public.medlemskap_forutgaaende_unntak_person(id);


--
-- Name: medlemskap_forutgaaende_unntak_grunnlag medlemskap_forutgaaende_unntak_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_forutgaaende_unntak_grunnlag
    ADD CONSTRAINT medlemskap_forutgaaende_unntak_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: medlemskap_unntak_grunnlag medlemskap_unntak_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_unntak_grunnlag
    ADD CONSTRAINT medlemskap_unntak_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: medlemskap_unntak_grunnlag medlemskap_unntak_grunnlag_medlemskap_unntak_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_unntak_grunnlag
    ADD CONSTRAINT medlemskap_unntak_grunnlag_medlemskap_unntak_person_id_fkey FOREIGN KEY (medlemskap_unntak_person_id) REFERENCES public.medlemskap_unntak_person(id);


--
-- Name: medlemskap_unntak medlemskap_unntak_medlemskap_unntak_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medlemskap_unntak
    ADD CONSTRAINT medlemskap_unntak_medlemskap_unntak_person_id_fkey FOREIGN KEY (medlemskap_unntak_person_id) REFERENCES public.medlemskap_unntak_person(id);


--
-- Name: meldekort_grunnlag meldekort_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldekort_grunnlag
    ADD CONSTRAINT meldekort_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: meldekort_grunnlag meldekort_grunnlag_meldekortene_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldekort_grunnlag
    ADD CONSTRAINT meldekort_grunnlag_meldekortene_id_fkey FOREIGN KEY (meldekortene_id) REFERENCES public.meldekortene(id);


--
-- Name: meldekort meldekort_meldekortene_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldekort
    ADD CONSTRAINT meldekort_meldekortene_id_fkey FOREIGN KEY (meldekortene_id) REFERENCES public.meldekortene(id);


--
-- Name: meldekort_periode meldekort_periode_meldekort_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldekort_periode
    ADD CONSTRAINT meldekort_periode_meldekort_id_fkey FOREIGN KEY (meldekort_id) REFERENCES public.meldekort(id);


--
-- Name: meldeperiode_grunnlag meldeperiode_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeperiode_grunnlag
    ADD CONSTRAINT meldeperiode_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: meldeperiode meldeperiode_meldeperiodegrunnlag_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeperiode
    ADD CONSTRAINT meldeperiode_meldeperiodegrunnlag_id_fkey FOREIGN KEY (meldeperiodegrunnlag_id) REFERENCES public.meldeperiode_grunnlag(id);


--
-- Name: meldeplikt_fritak_grunnlag meldeplikt_fritak_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_fritak_grunnlag
    ADD CONSTRAINT meldeplikt_fritak_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: meldeplikt_fritak_grunnlag meldeplikt_fritak_grunnlag_meldeplikt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_fritak_grunnlag
    ADD CONSTRAINT meldeplikt_fritak_grunnlag_meldeplikt_id_fkey FOREIGN KEY (meldeplikt_id) REFERENCES public.meldeplikt_fritak(id);


--
-- Name: meldeplikt_fritak_vurdering meldeplikt_fritak_vurdering_meldeplikt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_fritak_vurdering
    ADD CONSTRAINT meldeplikt_fritak_vurdering_meldeplikt_id_fkey FOREIGN KEY (meldeplikt_id) REFERENCES public.meldeplikt_fritak(id);


--
-- Name: meldeplikt_fritak_vurdering meldeplikt_fritak_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_fritak_vurdering
    ADD CONSTRAINT meldeplikt_fritak_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: meldeplikt_overstyring_grunnlag meldeplikt_overstyring_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_overstyring_grunnlag
    ADD CONSTRAINT meldeplikt_overstyring_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: meldeplikt_overstyring_vurdering_perioder meldeplikt_overstyring_vurder_meldeplikt_overstyring_vurde_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_overstyring_vurdering_perioder
    ADD CONSTRAINT meldeplikt_overstyring_vurder_meldeplikt_overstyring_vurde_fkey FOREIGN KEY (meldeplikt_overstyring_vurdering_id) REFERENCES public.meldeplikt_overstyring_vurdering(id);


--
-- Name: meldeplikt_overstyring_vurdering meldeplikt_overstyring_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_overstyring_vurdering
    ADD CONSTRAINT meldeplikt_overstyring_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: meldeplikt_overstyring_vurderinger meldeplikt_overstyring_vurderinger_grunnlag_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_overstyring_vurderinger
    ADD CONSTRAINT meldeplikt_overstyring_vurderinger_grunnlag_id_fkey FOREIGN KEY (grunnlag_id) REFERENCES public.meldeplikt_overstyring_grunnlag(id);


--
-- Name: meldeplikt_overstyring_vurderinger meldeplikt_overstyring_vurderinger_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meldeplikt_overstyring_vurderinger
    ADD CONSTRAINT meldeplikt_overstyring_vurderinger_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.meldeplikt_overstyring_vurdering(id);


--
-- Name: mellomlagret_vurdering mellomlagret_vurdering_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mellomlagret_vurdering
    ADD CONSTRAINT mellomlagret_vurdering_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: mottatt_dokument mottatt_dokument_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mottatt_dokument
    ADD CONSTRAINT mottatt_dokument_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: mottatt_dokument mottatt_dokument_sak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mottatt_dokument
    ADD CONSTRAINT mottatt_dokument_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES public.sak(id);


--
-- Name: oppfolgingsoppgave_grunnlag oppfolgingsoppgave_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppfolgingsoppgave_grunnlag
    ADD CONSTRAINT oppfolgingsoppgave_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: oppfolgingsoppgave_grunnlag oppfolgingsoppgave_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppfolgingsoppgave_grunnlag
    ADD CONSTRAINT oppfolgingsoppgave_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.oppfolgingsoppgave_vurdering(id);


--
-- Name: jobb oppgave_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobb
    ADD CONSTRAINT oppgave_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: jobb_historikk oppgave_historikk_oppgave_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobb_historikk
    ADD CONSTRAINT oppgave_historikk_oppgave_id_fkey FOREIGN KEY (jobb_id) REFERENCES public.jobb(id);


--
-- Name: jobb oppgave_sak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobb
    ADD CONSTRAINT oppgave_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES public.sak(id);


--
-- Name: oppgitt_barn oppgitt_barn_oppgitt_barn_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgitt_barn
    ADD CONSTRAINT oppgitt_barn_oppgitt_barn_id_fkey FOREIGN KEY (oppgitt_barn_id) REFERENCES public.oppgitt_barnopplysning(id);


--
-- Name: oppgitt_utenlandsopphold_grunnlag oppgitt_utenlandsopphold_grunn_oppgitt_utenlandsopphold_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgitt_utenlandsopphold_grunnlag
    ADD CONSTRAINT oppgitt_utenlandsopphold_grunn_oppgitt_utenlandsopphold_id_fkey FOREIGN KEY (oppgitt_utenlandsopphold_id) REFERENCES public.oppgitt_utenlandsopphold(id);


--
-- Name: oppgitt_utenlandsopphold_grunnlag oppgitt_utenlandsopphold_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgitt_utenlandsopphold_grunnlag
    ADD CONSTRAINT oppgitt_utenlandsopphold_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: opphold_grunnlag opphold_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opphold_grunnlag
    ADD CONSTRAINT opphold_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: opphold_grunnlag opphold_grunnlag_helseopphold_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opphold_grunnlag
    ADD CONSTRAINT opphold_grunnlag_helseopphold_vurderinger_id_fkey FOREIGN KEY (helseopphold_vurderinger_id) REFERENCES public.helseopphold_vurderinger(id);


--
-- Name: opphold_grunnlag opphold_grunnlag_opphold_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opphold_grunnlag
    ADD CONSTRAINT opphold_grunnlag_opphold_person_id_fkey FOREIGN KEY (opphold_person_id) REFERENCES public.opphold_person(id);


--
-- Name: opphold_grunnlag opphold_grunnlag_soning_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opphold_grunnlag
    ADD CONSTRAINT opphold_grunnlag_soning_vurderinger_id_fkey FOREIGN KEY (soning_vurderinger_id) REFERENCES public.soning_vurderinger(id);


--
-- Name: opphold opphold_opphold_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opphold
    ADD CONSTRAINT opphold_opphold_person_id_fkey FOREIGN KEY (opphold_person_id) REFERENCES public.opphold_person(id);


--
-- Name: oppholdskrav_grunnlag oppholdskrav_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppholdskrav_grunnlag
    ADD CONSTRAINT oppholdskrav_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: oppholdskrav_vurdering_periode oppholdskrav_vurdering_periode_oppholdskrav_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppholdskrav_vurdering_periode
    ADD CONSTRAINT oppholdskrav_vurdering_periode_oppholdskrav_vurdering_id_fkey FOREIGN KEY (oppholdskrav_vurdering_id) REFERENCES public.oppholdskrav_vurdering(id);


--
-- Name: oppholdskrav_vurdering oppholdskrav_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppholdskrav_vurdering
    ADD CONSTRAINT oppholdskrav_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: oppholdskrav_vurderinger oppholdskrav_vurderinger_grunnlag_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppholdskrav_vurderinger
    ADD CONSTRAINT oppholdskrav_vurderinger_grunnlag_id_fkey FOREIGN KEY (grunnlag_id) REFERENCES public.oppholdskrav_grunnlag(id);


--
-- Name: oppholdskrav_vurderinger oppholdskrav_vurderinger_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppholdskrav_vurderinger
    ADD CONSTRAINT oppholdskrav_vurderinger_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.oppholdskrav_vurdering(id);


--
-- Name: overgang_arbeid_grunnlag overgang_arbeid_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_arbeid_grunnlag
    ADD CONSTRAINT overgang_arbeid_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: overgang_arbeid_grunnlag overgang_arbeid_grunnlag_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_arbeid_grunnlag
    ADD CONSTRAINT overgang_arbeid_grunnlag_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.overgang_arbeid_vurderinger(id);


--
-- Name: overgang_arbeid_vurdering overgang_arbeid_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_arbeid_vurdering
    ADD CONSTRAINT overgang_arbeid_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.overgang_arbeid_vurderinger(id);


--
-- Name: overgang_arbeid_vurdering overgang_arbeid_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_arbeid_vurdering
    ADD CONSTRAINT overgang_arbeid_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: overgang_ufore_grunnlag overgang_ufore_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_ufore_grunnlag
    ADD CONSTRAINT overgang_ufore_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: overgang_ufore_grunnlag overgang_ufore_grunnlag_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_ufore_grunnlag
    ADD CONSTRAINT overgang_ufore_grunnlag_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.overgang_ufore_vurderinger(id);


--
-- Name: overgang_ufore_vurdering overgang_ufore_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_ufore_vurdering
    ADD CONSTRAINT overgang_ufore_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.overgang_ufore_vurderinger(id);


--
-- Name: overgang_ufore_vurdering overgang_ufore_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.overgang_ufore_vurdering
    ADD CONSTRAINT overgang_ufore_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: paaklaget_behandling_grunnlag paaklaget_behandling_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paaklaget_behandling_grunnlag
    ADD CONSTRAINT paaklaget_behandling_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: paaklaget_behandling_grunnlag paaklaget_behandling_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paaklaget_behandling_grunnlag
    ADD CONSTRAINT paaklaget_behandling_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.paaklaget_behandling_vurdering(id);


--
-- Name: paaklaget_behandling_vurdering paaklaget_behandling_vurdering_paaklaget_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paaklaget_behandling_vurdering
    ADD CONSTRAINT paaklaget_behandling_vurdering_paaklaget_behandling_id_fkey FOREIGN KEY (paaklaget_behandling_id) REFERENCES public.behandling(id);


--
-- Name: person_ident person_ident_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_ident
    ADD CONSTRAINT person_ident_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.person(id);


--
-- Name: personopplysning_forutgaaende_grunnlag personopplysning_forutgaaende_g_bruker_personopplysning_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personopplysning_forutgaaende_grunnlag
    ADD CONSTRAINT personopplysning_forutgaaende_g_bruker_personopplysning_id_fkey FOREIGN KEY (bruker_personopplysning_id) REFERENCES public.bruker_personopplysning_forutgaaende(id);


--
-- Name: personopplysning_forutgaaende_grunnlag personopplysning_forutgaaende_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personopplysning_forutgaaende_grunnlag
    ADD CONSTRAINT personopplysning_forutgaaende_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: personopplysning_grunnlag personopplysning_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personopplysning_grunnlag
    ADD CONSTRAINT personopplysning_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: personopplysning_grunnlag personopplysning_grunnlag_personopplysning_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personopplysning_grunnlag
    ADD CONSTRAINT personopplysning_grunnlag_personopplysning_id_fkey FOREIGN KEY (bruker_personopplysning_id) REFERENCES public.bruker_personopplysning(id);


--
-- Name: reduksjon_11_9_grunnlag reduksjon_11_9_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reduksjon_11_9_grunnlag
    ADD CONSTRAINT reduksjon_11_9_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: reduksjon_11_9 reduksjon_11_9_reduksjon_11_9_grunnlag_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reduksjon_11_9
    ADD CONSTRAINT reduksjon_11_9_reduksjon_11_9_grunnlag_id_fkey FOREIGN KEY (reduksjon_11_9_grunnlag_id) REFERENCES public.reduksjon_11_9_grunnlag(id);


--
-- Name: refusjonkrav_grunnlag refusjonkrav_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refusjonkrav_grunnlag
    ADD CONSTRAINT refusjonkrav_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: refusjonkrav_grunnlag refusjonkrav_grunnlag_refusjonkrav_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refusjonkrav_grunnlag
    ADD CONSTRAINT refusjonkrav_grunnlag_refusjonkrav_vurderinger_id_fkey FOREIGN KEY (refusjonkrav_vurderinger_id) REFERENCES public.refusjonkrav_vurderinger(id);


--
-- Name: refusjonkrav_grunnlag refusjonkrav_grunnlag_sak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refusjonkrav_grunnlag
    ADD CONSTRAINT refusjonkrav_grunnlag_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES public.sak(id);


--
-- Name: refusjonkrav_vurdering refusjonkrav_vurdering_refusjonkrav_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refusjonkrav_vurdering
    ADD CONSTRAINT refusjonkrav_vurdering_refusjonkrav_vurderinger_id_fkey FOREIGN KEY (refusjonkrav_vurderinger_id) REFERENCES public.refusjonkrav_vurderinger(id);


--
-- Name: rettighetsperiode_grunnlag rettighetsperiode_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetsperiode_grunnlag
    ADD CONSTRAINT rettighetsperiode_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: rettighetsperiode_grunnlag rettighetsperiode_grunnlag_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetsperiode_grunnlag
    ADD CONSTRAINT rettighetsperiode_grunnlag_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.rettighetsperiode_vurderinger(id);


--
-- Name: rettighetsperiode_vurdering rettighetsperiode_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetsperiode_vurdering
    ADD CONSTRAINT rettighetsperiode_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.rettighetsperiode_vurderinger(id);


--
-- Name: rettighetstype_grunnlag rettighetstype_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetstype_grunnlag
    ADD CONSTRAINT rettighetstype_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: rettighetstype_grunnlag rettighetstype_grunnlag_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetstype_grunnlag
    ADD CONSTRAINT rettighetstype_grunnlag_perioder_id_fkey FOREIGN KEY (perioder_id) REFERENCES public.rettighetstype_perioder(id);


--
-- Name: rettighetstype_periode rettighetstype_periode_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetstype_periode
    ADD CONSTRAINT rettighetstype_periode_perioder_id_fkey FOREIGN KEY (perioder_id) REFERENCES public.rettighetstype_perioder(id);


--
-- Name: rettighetstype_sporing rettighetstype_sporing_rettighetstype_grunnlag_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rettighetstype_sporing
    ADD CONSTRAINT rettighetstype_sporing_rettighetstype_grunnlag_id_fkey FOREIGN KEY (rettighetstype_grunnlag_id) REFERENCES public.rettighetstype_grunnlag(id);


--
-- Name: sak sak_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sak
    ADD CONSTRAINT sak_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.person(id);


--
-- Name: sam_id sam_id_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sam_id
    ADD CONSTRAINT sam_id_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: samordning_andre_statlige_ytelser_grunnlag samordning_andre_statlige_ytelser_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_andre_statlige_ytelser_grunnlag
    ADD CONSTRAINT samordning_andre_statlige_ytelser_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: samordning_andre_statlige_ytelser_grunnlag samordning_andre_statlige_ytelser_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_andre_statlige_ytelser_grunnlag
    ADD CONSTRAINT samordning_andre_statlige_ytelser_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.samordning_andre_statlige_ytelser_vurdering(id);


--
-- Name: samordning_andre_statlige_ytelser_vurdering_periode samordning_andre_statlige_ytelser_vurdering_p_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_andre_statlige_ytelser_vurdering_periode
    ADD CONSTRAINT samordning_andre_statlige_ytelser_vurdering_p_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.samordning_andre_statlige_ytelser_vurdering(id);


--
-- Name: samordning_arbeidsgiver_grunnlag samordning_arbeidsgiver_grunn_samordning_arbeidsgiver_vurd_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_arbeidsgiver_grunnlag
    ADD CONSTRAINT samordning_arbeidsgiver_grunn_samordning_arbeidsgiver_vurd_fkey FOREIGN KEY (samordning_arbeidsgiver_vurdering_id) REFERENCES public.samordning_arbeidsgiver_vurdering(id);


--
-- Name: samordning_arbeidsgiver_grunnlag samordning_arbeidsgiver_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_arbeidsgiver_grunnlag
    ADD CONSTRAINT samordning_arbeidsgiver_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: samordning_arbeidsgiver_grunnlag samordning_arbeidsgiver_grunnlag_sak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_arbeidsgiver_grunnlag
    ADD CONSTRAINT samordning_arbeidsgiver_grunnlag_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES public.sak(id);


--
-- Name: samordning_arbeidsgiver_vurdering_periode samordning_arbeidsgiver_vurdering_periode_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_arbeidsgiver_vurdering_periode
    ADD CONSTRAINT samordning_arbeidsgiver_vurdering_periode_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.samordning_arbeidsgiver_vurdering(id);


--
-- Name: samordning_barnepensjon_grunnlag samordning_barnepensjon_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_barnepensjon_grunnlag
    ADD CONSTRAINT samordning_barnepensjon_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: samordning_barnepensjon_grunnlag samordning_barnepensjon_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_barnepensjon_grunnlag
    ADD CONSTRAINT samordning_barnepensjon_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.samordning_barnepensjon_vurdering(id);


--
-- Name: samordning_barnepensjon_vurdering_periode samordning_barnepensjon_vurdering_periode_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_barnepensjon_vurdering_periode
    ADD CONSTRAINT samordning_barnepensjon_vurdering_periode_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.samordning_barnepensjon_vurdering(id);


--
-- Name: samordning_barnepensjon_vurdering samordning_barnepensjon_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_barnepensjon_vurdering
    ADD CONSTRAINT samordning_barnepensjon_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: samordning_grunnlag samordning_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_grunnlag
    ADD CONSTRAINT samordning_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: samordning_grunnlag samordning_grunnlag_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_grunnlag
    ADD CONSTRAINT samordning_grunnlag_perioder_id_fkey FOREIGN KEY (perioder_id) REFERENCES public.samordning_perioder(id);


--
-- Name: samordning_periode samordning_periode_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_periode
    ADD CONSTRAINT samordning_periode_perioder_id_fkey FOREIGN KEY (perioder_id) REFERENCES public.samordning_perioder(id);


--
-- Name: samordning_ufore_grunnlag samordning_ufore_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ufore_grunnlag
    ADD CONSTRAINT samordning_ufore_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: samordning_ufore_grunnlag samordning_ufore_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ufore_grunnlag
    ADD CONSTRAINT samordning_ufore_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.samordning_ufore_vurdering(id);


--
-- Name: samordning_ufore_vurdering_periode samordning_ufore_vurdering_periode_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ufore_vurdering_periode
    ADD CONSTRAINT samordning_ufore_vurdering_periode_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.samordning_ufore_vurdering(id);


--
-- Name: samordning_vurdering_periode samordning_vurdering_periode_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_vurdering_periode
    ADD CONSTRAINT samordning_vurdering_periode_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.samordning_vurdering(id);


--
-- Name: samordning_vurdering samordning_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_vurdering
    ADD CONSTRAINT samordning_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.samordning_vurderinger(id);


--
-- Name: samordning_ytelse_grunnlag samordning_ytelse_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelse_grunnlag
    ADD CONSTRAINT samordning_ytelse_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: samordning_ytelse_grunnlag samordning_ytelse_grunnlag_samordning_ytelse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelse_grunnlag
    ADD CONSTRAINT samordning_ytelse_grunnlag_samordning_ytelse_id_fkey FOREIGN KEY (samordning_ytelse_id) REFERENCES public.samordning_ytelser(id);


--
-- Name: samordning_ytelse_periode samordning_ytelse_periode_ytelse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelse_periode
    ADD CONSTRAINT samordning_ytelse_periode_ytelse_id_fkey FOREIGN KEY (ytelse_id) REFERENCES public.samordning_ytelse(id);


--
-- Name: samordning_ytelse samordning_ytelse_ytelser_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelse
    ADD CONSTRAINT samordning_ytelse_ytelser_id_fkey FOREIGN KEY (ytelser_id) REFERENCES public.samordning_ytelser(id);


--
-- Name: samordning_ytelsevurdering_grunnlag samordning_ytelsevurdering_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelsevurdering_grunnlag
    ADD CONSTRAINT samordning_ytelsevurdering_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: samordning_ytelsevurdering_grunnlag samordning_ytelsevurdering_grunnlag_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.samordning_ytelsevurdering_grunnlag
    ADD CONSTRAINT samordning_ytelsevurdering_grunnlag_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.samordning_vurderinger(id);


--
-- Name: soning_vurdering soning_vurdering_soning_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.soning_vurdering
    ADD CONSTRAINT soning_vurdering_soning_vurderinger_id_fkey FOREIGN KEY (soning_vurderinger_id) REFERENCES public.soning_vurderinger(id);


--
-- Name: stans_opphor_grunnlag stans_opphor_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_grunnlag
    ADD CONSTRAINT stans_opphor_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: stans_opphor_grunnlag stans_opphor_grunnlag_stans_opphor_set_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_grunnlag
    ADD CONSTRAINT stans_opphor_grunnlag_stans_opphor_set_id_fkey FOREIGN KEY (stans_opphor_set_id) REFERENCES public.stans_opphor_set(id);


--
-- Name: stans_opphor_grunnlag stans_opphor_grunnlag_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_grunnlag
    ADD CONSTRAINT stans_opphor_grunnlag_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.stans_opphor_vurderinger(id);


--
-- Name: stans_opphor_grunnlag stans_opphor_grunnlag_vurderinger_id_v2_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_grunnlag
    ADD CONSTRAINT stans_opphor_grunnlag_vurderinger_id_v2_fkey FOREIGN KEY (vurderinger_id_v2) REFERENCES public.stans_opphor_vurderinger_v2(id);


--
-- Name: stans_opphor stans_opphor_stans_opphor_set_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor
    ADD CONSTRAINT stans_opphor_stans_opphor_set_id_fkey FOREIGN KEY (stans_opphor_set_id) REFERENCES public.stans_opphor_set(id);


--
-- Name: stans_opphor_vurdering_v2 stans_opphor_vurdering_v2_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_vurdering_v2
    ADD CONSTRAINT stans_opphor_vurdering_v2_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.stans_opphor_vurderinger_v2(id);


--
-- Name: stans_opphor_vurdering_v2 stans_opphor_vurdering_v2_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_vurdering_v2
    ADD CONSTRAINT stans_opphor_vurdering_v2_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: stans_opphor_vurdering stans_opphor_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_vurdering
    ADD CONSTRAINT stans_opphor_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.stans_opphor_vurderinger(id);


--
-- Name: stans_opphor_vurdering stans_opphor_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stans_opphor_vurdering
    ADD CONSTRAINT stans_opphor_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: steg_historikk steg_historikk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.steg_historikk
    ADD CONSTRAINT steg_historikk_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: student_grunnlag student_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_grunnlag
    ADD CONSTRAINT student_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: student_grunnlag student_grunnlag_oppgitt_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_grunnlag
    ADD CONSTRAINT student_grunnlag_oppgitt_student_id_fkey FOREIGN KEY (oppgitt_student_id) REFERENCES public.oppgitt_student(id);


--
-- Name: student_grunnlag student_grunnlag_student_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_grunnlag
    ADD CONSTRAINT student_grunnlag_student_vurderinger_id_fkey FOREIGN KEY (student_vurderinger_id) REFERENCES public.student_vurderinger(id);


--
-- Name: student_vurdering student_vurdering_student_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_vurdering
    ADD CONSTRAINT student_vurdering_student_vurderinger_id_fkey FOREIGN KEY (student_vurderinger_id) REFERENCES public.student_vurderinger(id);


--
-- Name: student_vurdering student_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_vurdering
    ADD CONSTRAINT student_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: svar_fra_andreinstans_grunnlag svar_fra_andreinstans_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.svar_fra_andreinstans_grunnlag
    ADD CONSTRAINT svar_fra_andreinstans_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: svar_fra_andreinstans_grunnlag svar_fra_andreinstans_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.svar_fra_andreinstans_grunnlag
    ADD CONSTRAINT svar_fra_andreinstans_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.svar_fra_andreinstans_vurdering(id);


--
-- Name: sykdom_grunnlag sykdom_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_grunnlag
    ADD CONSTRAINT sykdom_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: sykdom_grunnlag sykdom_grunnlag_sykdom_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_grunnlag
    ADD CONSTRAINT sykdom_grunnlag_sykdom_vurderinger_id_fkey FOREIGN KEY (sykdom_vurderinger_id) REFERENCES public.sykdom_vurderinger(id);


--
-- Name: sykdom_grunnlag sykdom_grunnlag_yrkesskade_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_grunnlag
    ADD CONSTRAINT sykdom_grunnlag_yrkesskade_id_fkey FOREIGN KEY (yrkesskade_id) REFERENCES public.yrkesskade_vurdering(id);


--
-- Name: sykdom_vurdering_bidiagnoser sykdom_vurdering_bidiagnoser_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_vurdering_bidiagnoser
    ADD CONSTRAINT sykdom_vurdering_bidiagnoser_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.sykdom_vurdering(id);


--
-- Name: sykdom_vurdering_brev sykdom_vurdering_brev_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_vurdering_brev
    ADD CONSTRAINT sykdom_vurdering_brev_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: sykdom_vurdering sykdom_vurdering_sykdom_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_vurdering
    ADD CONSTRAINT sykdom_vurdering_sykdom_vurderinger_id_fkey FOREIGN KEY (sykdom_vurderinger_id) REFERENCES public.sykdom_vurderinger(id);


--
-- Name: sykdom_vurdering sykdom_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykdom_vurdering
    ADD CONSTRAINT sykdom_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: sykepenge_erstatning_grunnlag sykepenge_erstatning_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykepenge_erstatning_grunnlag
    ADD CONSTRAINT sykepenge_erstatning_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: sykepenge_erstatning_grunnlag sykepenge_erstatning_grunnlag_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykepenge_erstatning_grunnlag
    ADD CONSTRAINT sykepenge_erstatning_grunnlag_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.sykepenge_vurderinger(id);


--
-- Name: sykepenge_vurdering sykepenge_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykepenge_vurdering
    ADD CONSTRAINT sykepenge_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.sykepenge_vurderinger(id);


--
-- Name: sykepenge_vurdering sykepenge_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykepenge_vurdering
    ADD CONSTRAINT sykepenge_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: sykestipend_grunnlag sykestipend_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykestipend_grunnlag
    ADD CONSTRAINT sykestipend_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: sykestipend_grunnlag sykestipend_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykestipend_grunnlag
    ADD CONSTRAINT sykestipend_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.sykestipend_vurdering(id);


--
-- Name: sykestipend_vurdering sykestipend_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sykestipend_vurdering
    ADD CONSTRAINT sykestipend_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: test_automatisk_meldekort_sak test_automatisk_meldekort_sak_sak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_automatisk_meldekort_sak
    ADD CONSTRAINT test_automatisk_meldekort_sak_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES public.sak(id);


--
-- Name: tilbakekrevingsbehandling tilbakekrevingsbehandling_sak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilbakekrevingsbehandling
    ADD CONSTRAINT tilbakekrevingsbehandling_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES public.sak(id);


--
-- Name: tilbakekrevingshendelse tilbakekrevingshendelse_sak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilbakekrevingshendelse
    ADD CONSTRAINT tilbakekrevingshendelse_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES public.sak(id);


--
-- Name: tilkjent_periode tilkjent_periode_tilkjent_ytelse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilkjent_periode
    ADD CONSTRAINT tilkjent_periode_tilkjent_ytelse_id_fkey FOREIGN KEY (tilkjent_ytelse_id) REFERENCES public.tilkjent_ytelse(id);


--
-- Name: tilkjent_ytelse tilkjent_ytelse_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilkjent_ytelse
    ADD CONSTRAINT tilkjent_ytelse_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: tilkjent_ytelse_sporing tilkjent_ytelse_sporing_tilkjent_ytelse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilkjent_ytelse_sporing
    ADD CONSTRAINT tilkjent_ytelse_sporing_tilkjent_ytelse_id_fkey FOREIGN KEY (tilkjent_ytelse_id) REFERENCES public.tilkjent_ytelse(id);


--
-- Name: tiltakspenger_grunnlag tiltakspenger_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tiltakspenger_grunnlag
    ADD CONSTRAINT tiltakspenger_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: tiltakspenger_grunnlag tiltakspenger_grunnlag_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tiltakspenger_grunnlag
    ADD CONSTRAINT tiltakspenger_grunnlag_perioder_id_fkey FOREIGN KEY (perioder_id) REFERENCES public.tiltakspenger_perioder(id);


--
-- Name: tiltakspenger_periode tiltakspenger_periode_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tiltakspenger_periode
    ADD CONSTRAINT tiltakspenger_periode_perioder_id_fkey FOREIGN KEY (perioder_id) REFERENCES public.tiltakspenger_perioder(id);


--
-- Name: tjenestepensjon_forhold_grunnlag tjenestepensjon_forhold_grunn_tjenestepensjon_ordninger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_forhold_grunnlag
    ADD CONSTRAINT tjenestepensjon_forhold_grunn_tjenestepensjon_ordninger_id_fkey FOREIGN KEY (tjenestepensjon_ordninger_id) REFERENCES public.tjenestepensjon_ordninger(id);


--
-- Name: tjenestepensjon_forhold_grunnlag tjenestepensjon_forhold_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_forhold_grunnlag
    ADD CONSTRAINT tjenestepensjon_forhold_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: tjenestepensjon_ordning tjenestepensjon_ordning_tjenestepensjon_ordninger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_ordning
    ADD CONSTRAINT tjenestepensjon_ordning_tjenestepensjon_ordninger_id_fkey FOREIGN KEY (tjenestepensjon_ordninger_id) REFERENCES public.tjenestepensjon_ordninger(id);


--
-- Name: tjenestepensjon_refusjonskrav_grunnlag tjenestepensjon_refusjonskrav_gr_refusjonkrav_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_refusjonskrav_grunnlag
    ADD CONSTRAINT tjenestepensjon_refusjonskrav_gr_refusjonkrav_vurdering_id_fkey FOREIGN KEY (refusjonkrav_vurdering_id) REFERENCES public.tjenestepensjon_refusjonskrav_vurdering(id);


--
-- Name: tjenestepensjon_refusjonskrav_grunnlag tjenestepensjon_refusjonskrav_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_refusjonskrav_grunnlag
    ADD CONSTRAINT tjenestepensjon_refusjonskrav_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: tjenestepensjon_refusjonskrav_grunnlag tjenestepensjon_refusjonskrav_grunnlag_sak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_refusjonskrav_grunnlag
    ADD CONSTRAINT tjenestepensjon_refusjonskrav_grunnlag_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES public.sak(id);


--
-- Name: tjenestepensjon_ytelse tjenestepensjon_ytelse_tjenestepensjon_ordning_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tjenestepensjon_ytelse
    ADD CONSTRAINT tjenestepensjon_ytelse_tjenestepensjon_ordning_id_fkey FOREIGN KEY (tjenestepensjon_ordning_id) REFERENCES public.tjenestepensjon_ordning(id);


--
-- Name: trekk_klage_grunnlag trekk_klage_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trekk_klage_grunnlag
    ADD CONSTRAINT trekk_klage_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: trekk_klage_grunnlag trekk_klage_grunnlag_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trekk_klage_grunnlag
    ADD CONSTRAINT trekk_klage_grunnlag_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.trekk_klage_vurdering(id);


--
-- Name: trukket_soknad_grunnlag trukket_soknad_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trukket_soknad_grunnlag
    ADD CONSTRAINT trukket_soknad_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: trukket_soknad_grunnlag trukket_soknad_grunnlag_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trukket_soknad_grunnlag
    ADD CONSTRAINT trukket_soknad_grunnlag_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.trukket_soknad_vurderinger(id);


--
-- Name: trukket_soknad_vurdering trukket_soknad_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trukket_soknad_vurdering
    ADD CONSTRAINT trukket_soknad_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.trukket_soknad_vurderinger(id);


--
-- Name: ufore_gradering ufore_gradering_ufore_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ufore_gradering
    ADD CONSTRAINT ufore_gradering_ufore_id_fkey FOREIGN KEY (ufore_id) REFERENCES public.ufore(id);


--
-- Name: ufore_grunnlag ufore_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ufore_grunnlag
    ADD CONSTRAINT ufore_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: ufore_grunnlag ufore_grunnlag_ufore_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ufore_grunnlag
    ADD CONSTRAINT ufore_grunnlag_ufore_id_fkey FOREIGN KEY (ufore_id) REFERENCES public.ufore(id);


--
-- Name: ufore_soknad_grunnlag ufore_soknad_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ufore_soknad_grunnlag
    ADD CONSTRAINT ufore_soknad_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: underveis_grunnlag underveis_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_grunnlag
    ADD CONSTRAINT underveis_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: underveis_grunnlag underveis_grunnlag_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_grunnlag
    ADD CONSTRAINT underveis_grunnlag_perioder_id_fkey FOREIGN KEY (perioder_id) REFERENCES public.underveis_perioder(id);


--
-- Name: underveis_grunnlag underveis_grunnlag_sporing_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_grunnlag
    ADD CONSTRAINT underveis_grunnlag_sporing_id_fkey FOREIGN KEY (sporing_id) REFERENCES public.underveis_sporing(id);


--
-- Name: underveis_periode underveis_periode_brudd_aktivitetsplikt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_periode
    ADD CONSTRAINT underveis_periode_brudd_aktivitetsplikt_id_fkey FOREIGN KEY (brudd_aktivitetsplikt_id) REFERENCES public.brudd_aktivitetsplikt(id);


--
-- Name: underveis_periode underveis_periode_perioder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.underveis_periode
    ADD CONSTRAINT underveis_periode_perioder_id_fkey FOREIGN KEY (perioder_id) REFERENCES public.underveis_perioder(id);


--
-- Name: utenlands_periode utenlands_periode_oppgitt_utenlandsopphold_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utenlands_periode
    ADD CONSTRAINT utenlands_periode_oppgitt_utenlandsopphold_id_fkey FOREIGN KEY (oppgitt_utenlandsopphold_id) REFERENCES public.oppgitt_utenlandsopphold(id);


--
-- Name: vedtak vedtak_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtak
    ADD CONSTRAINT vedtak_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: vedtakslengde_grunnlag vedtakslengde_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtakslengde_grunnlag
    ADD CONSTRAINT vedtakslengde_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: vedtakslengde_grunnlag vedtakslengde_grunnlag_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtakslengde_grunnlag
    ADD CONSTRAINT vedtakslengde_grunnlag_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.vedtakslengde_vurderinger(id);


--
-- Name: vedtakslengde_vurdering vedtakslengde_vurdering_vurderinger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtakslengde_vurdering
    ADD CONSTRAINT vedtakslengde_vurdering_vurderinger_id_fkey FOREIGN KEY (vurderinger_id) REFERENCES public.vedtakslengde_vurderinger(id);


--
-- Name: vedtakslengde_vurdering vedtakslengde_vurdering_vurdert_i_behandling_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtakslengde_vurdering
    ADD CONSTRAINT vedtakslengde_vurdering_vurdert_i_behandling_fkey FOREIGN KEY (vurdert_i_behandling) REFERENCES public.behandling(id);


--
-- Name: vilkar_periode vilkar_periode_vilkar_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkar_periode
    ADD CONSTRAINT vilkar_periode_vilkar_id_fkey FOREIGN KEY (vilkar_id) REFERENCES public.vilkar(id);


--
-- Name: vilkar_resultat vilkar_resultat_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkar_resultat
    ADD CONSTRAINT vilkar_resultat_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: vilkar vilkar_resultat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkar
    ADD CONSTRAINT vilkar_resultat_id_fkey FOREIGN KEY (resultat_id) REFERENCES public.vilkar_resultat(id);


--
-- Name: vurderingsbehov vurderingsbehov_behandling_aarsak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vurderingsbehov
    ADD CONSTRAINT vurderingsbehov_behandling_aarsak_id_fkey FOREIGN KEY (behandling_aarsak_id) REFERENCES public.behandling_aarsak(id);


--
-- Name: yrkesskade_dato yrkesskade_dato_yrkesskade_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_dato
    ADD CONSTRAINT yrkesskade_dato_yrkesskade_id_fkey FOREIGN KEY (yrkesskade_id) REFERENCES public.yrkesskade(id);


--
-- Name: yrkesskade_grunnlag yrkesskade_grunnlag_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_grunnlag
    ADD CONSTRAINT yrkesskade_grunnlag_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES public.behandling(id);


--
-- Name: yrkesskade_grunnlag yrkesskade_grunnlag_yrkesskade_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_grunnlag
    ADD CONSTRAINT yrkesskade_grunnlag_yrkesskade_id_fkey FOREIGN KEY (yrkesskade_id) REFERENCES public.yrkesskade(id);


--
-- Name: yrkesskade_inntekt yrkesskade_inntekt_inntekter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_inntekt
    ADD CONSTRAINT yrkesskade_inntekt_inntekter_id_fkey FOREIGN KEY (inntekter_id) REFERENCES public.yrkesskade_inntekter(id);


--
-- Name: yrkesskade_relaterte_saker yrkesskade_relaterte_saker_vurdering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.yrkesskade_relaterte_saker
    ADD CONSTRAINT yrkesskade_relaterte_saker_vurdering_id_fkey FOREIGN KEY (vurdering_id) REFERENCES public.yrkesskade_vurdering(id);


--
-- PostgreSQL database dump complete
--
