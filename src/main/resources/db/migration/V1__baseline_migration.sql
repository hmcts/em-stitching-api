CREATE TABLE public.batch_job_execution (
    job_execution_id bigint NOT NULL,
    version bigint,
    job_instance_id bigint NOT NULL,
    create_time timestamp without time zone NOT NULL,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10),
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone,
    job_configuration_location character varying(2500)
);

CREATE TABLE public.batch_job_execution_context (
    job_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);

CREATE TABLE public.batch_job_execution_params (
    job_execution_id bigint NOT NULL,
    type_cd character varying(6) NOT NULL,
    key_name character varying(100) NOT NULL,
    string_val character varying(250),
    date_val timestamp without time zone,
    long_val bigint,
    double_val double precision,
    identifying character(1) NOT NULL
);

CREATE SEQUENCE public.batch_job_execution_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.batch_job_instance (
    job_instance_id bigint NOT NULL,
    version bigint,
    job_name character varying(100) NOT NULL,
    job_key character varying(32) NOT NULL
);

CREATE SEQUENCE public.batch_job_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE public.batch_step_execution (
    step_execution_id bigint NOT NULL,
    version bigint NOT NULL,
    step_name character varying(100) NOT NULL,
    job_execution_id bigint NOT NULL,
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone,
    status character varying(10),
    commit_count bigint,
    read_count bigint,
    filter_count bigint,
    write_count bigint,
    read_skip_count bigint,
    write_skip_count bigint,
    process_skip_count bigint,
    rollback_count bigint,
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone
);

CREATE TABLE public.batch_step_execution_context (
    step_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);

CREATE SEQUENCE public.batch_step_execution_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.bundle (
    id bigint NOT NULL,
    created_by character varying(50) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    last_modified_by character varying(50),
    last_modified_date timestamp without time zone,
    bundle_title character varying(255),
    description character varying(1000),
    stitch_status character varying(255),
    stitched_documenturi character varying(255),
    file_name character varying(255),
    has_coversheets boolean DEFAULT true NOT NULL,
    has_table_of_contents boolean DEFAULT true NOT NULL,
    has_folder_coversheets boolean DEFAULT false NOT NULL,
    coverpage_template character varying(255),
    pagination_style integer,
    page_number_format integer,
    coverpage_template_data jsonb,
    enable_email_notification boolean,
    document_image jsonb DEFAULT null::jsonb,
    file_name_identifier character varying(255),
    hash_token character varying(5000)
);

CREATE TABLE public.bundle_document (
    id bigint NOT NULL,
    created_by character varying(50) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    last_modified_by character varying(50),
    last_modified_date timestamp without time zone,
    doc_description character varying(1000),
    doc_title character varying(255),
    documenturi character varying(255),
    sort_index integer NOT NULL
);

CREATE TABLE public.bundle_documents (
    bundle_id bigint NOT NULL,
    documents_id bigint NOT NULL
);

CREATE TABLE public.bundle_folder (
    id bigint NOT NULL,
    created_by character varying(50) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    last_modified_by character varying(50),
    last_modified_date timestamp without time zone,
    description character varying(255),
    folder_name character varying(255),
    sort_index integer NOT NULL
);

CREATE TABLE public.bundle_folder_documents (
    bundle_folder_id bigint NOT NULL,
    documents_id bigint NOT NULL
);

CREATE TABLE public.bundle_folder_folders (
    bundle_folder_id bigint NOT NULL,
    folders_id bigint NOT NULL
);

CREATE TABLE public.bundle_folders (
    bundle_id bigint NOT NULL,
    folders_id bigint NOT NULL
);

CREATE TABLE public.callback (
    id bigint NOT NULL,
    created_by character varying(50) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    last_modified_by character varying(50),
    last_modified_date timestamp without time zone,
    callback_state character varying(255) DEFAULT 'NEW'::character varying,
    callback_url character varying(5000),
    failure_description character varying(5000),
    version integer NOT NULL,
    attempts integer DEFAULT 0
);

CREATE TABLE public.databasechangelog (
    id character varying(255) NOT NULL,
    author character varying(255) NOT NULL,
    filename character varying(255) NOT NULL,
    dateexecuted timestamp without time zone NOT NULL,
    orderexecuted integer NOT NULL,
    exectype character varying(10) NOT NULL,
    md5sum character varying(35),
    description character varying(255),
    comments character varying(255),
    tag character varying(255),
    liquibase character varying(20),
    contexts character varying(255),
    labels character varying(255),
    deployment_id character varying(10)
);

CREATE TABLE public.databasechangeloglock (
    id integer NOT NULL,
    locked boolean NOT NULL,
    lockgranted timestamp without time zone,
    lockedby character varying(255)
);

CREATE TABLE public.document_task (
    id bigint NOT NULL,
    created_by character varying(50) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    last_modified_by character varying(50),
    last_modified_date timestamp without time zone,
    failure_description character varying(255),
    jwt character varying(5000),
    task_state character varying(255),
    bundle_id bigint
);

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.jhi_entity_audit_event (
    id bigint NOT NULL,
    action character varying(20) NOT NULL,
    commit_version integer,
    entity_id bigint NOT NULL,
    entity_type character varying(255) NOT NULL,
    entity_value text,
    modified_by character varying(100),
    modified_date timestamp without time zone NOT NULL
);

CREATE TABLE public.jhi_persistent_audit_event (
    event_id bigint NOT NULL,
    event_date timestamp without time zone,
    event_type character varying(255),
    principal character varying(5000) NOT NULL
);

CREATE TABLE public.jhi_persistent_audit_evt_data (
    event_id bigint NOT NULL,
    audit_data character varying(255),
    name character varying(255) NOT NULL
);

CREATE TABLE public.shedlock (
    name character varying(64) NOT NULL,
    lock_until timestamp(3) without time zone,
    locked_at timestamp(3) without time zone,
    locked_by character varying(255)
);

CREATE TABLE public.versioned_document_task (
    id bigint NOT NULL,
    created_by character varying(50) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    last_modified_by character varying(50),
    last_modified_date timestamp without time zone,
    failure_description character varying(5000),
    jwt character varying(5000),
    task_state character varying(255),
    version integer NOT NULL,
    bundle_id bigint,
    callback_id bigint,
    case_type_id character varying(255),
    jurisdiction_id character varying(255),
    service_auth character varying(5000),
    case_id character varying(255)
);

ALTER TABLE public.bundle_folders
    ADD CONSTRAINT "UK_4ppyrnm7h4a8apqi6w5wwta60" UNIQUE (folders_id);

ALTER TABLE public.bundle_documents
    ADD CONSTRAINT "UK_n3imi6spqlq3ngx6111erwjml" UNIQUE (documents_id);

ALTER TABLE public.bundle_folder_folders
    ADD CONSTRAINT "UK_qgkkmomjdlesf4xupw7ttsjiy" UNIQUE (folders_id);

ALTER TABLE public.bundle_folder_documents
    ADD CONSTRAINT "UK_qhi8jp1q243d61ie7hm17iio7" UNIQUE (documents_id);

ALTER TABLE public.batch_job_execution_context
    ADD CONSTRAINT batch_job_execution_context_pkey PRIMARY KEY (job_execution_id);

ALTER TABLE public.batch_job_execution
    ADD CONSTRAINT batch_job_execution_pkey PRIMARY KEY (job_execution_id);

ALTER TABLE public.batch_job_instance
    ADD CONSTRAINT batch_job_instance_pkey PRIMARY KEY (job_instance_id);

ALTER TABLE public.batch_step_execution_context
    ADD CONSTRAINT batch_step_execution_context_pkey PRIMARY KEY (step_execution_id);

ALTER TABLE public.batch_step_execution
    ADD CONSTRAINT batch_step_execution_pkey PRIMARY KEY (step_execution_id);

ALTER TABLE public.bundle
    ADD CONSTRAINT "bundlePK" PRIMARY KEY (id);

ALTER TABLE public.bundle_document
    ADD CONSTRAINT "bundle_documentPK" PRIMARY KEY (id);

ALTER TABLE public.bundle_folder
    ADD CONSTRAINT "bundle_folderPK" PRIMARY KEY (id);

ALTER TABLE public.callback
    ADD CONSTRAINT "callbackPK" PRIMARY KEY (id);

ALTER TABLE public.databasechangeloglock
    ADD CONSTRAINT databasechangeloglock_pkey PRIMARY KEY (id);

ALTER TABLE public.document_task
    ADD CONSTRAINT "document_taskPK" PRIMARY KEY (id);

ALTER TABLE public.jhi_entity_audit_event
    ADD CONSTRAINT "jhi_entity_audit_eventPK" PRIMARY KEY (id);

ALTER TABLE public.jhi_persistent_audit_event
    ADD CONSTRAINT "jhi_persistent_audit_eventPK" PRIMARY KEY (event_id);

ALTER TABLE public.jhi_persistent_audit_evt_data
    ADD CONSTRAINT jhi_persistent_audit_evt_data_pkey PRIMARY KEY (event_id, name);

ALTER TABLE public.batch_job_instance
    ADD CONSTRAINT job_inst_un UNIQUE (job_name, job_key);

ALTER TABLE public.shedlock
    ADD CONSTRAINT shedlock_pkey PRIMARY KEY (name);

ALTER TABLE public.versioned_document_task
    ADD CONSTRAINT "versioned_document_taskPK" PRIMARY KEY (id);

ALTER TABLE public.jhi_persistent_audit_evt_data
    ADD CONSTRAINT "FK2ehnyx2si4tjd2nt4q7y40v8m" FOREIGN KEY (event_id) REFERENCES public.jhi_persistent_audit_event(event_id);

ALTER TABLE public.bundle_folders
    ADD CONSTRAINT "FK6kqfyrhicyteuexidrkaevbf0" FOREIGN KEY (folders_id) REFERENCES public.bundle_folder(id);

ALTER TABLE public.bundle_folder_folders
    ADD CONSTRAINT "FK7gg0w68p2f26kjkt3f0w6er8i" FOREIGN KEY (folders_id) REFERENCES public.bundle_folder(id);

ALTER TABLE public.bundle_documents
    ADD CONSTRAINT "FK98uvetyev6186oanertlvllle" FOREIGN KEY (bundle_id) REFERENCES public.bundle(id);

ALTER TABLE public.bundle_documents
    ADD CONSTRAINT "FKa3kmx8n3ns2wr9l7tqyswaht3" FOREIGN KEY (documents_id) REFERENCES public.bundle_document(id);

ALTER TABLE public.bundle_folder_documents
    ADD CONSTRAINT "FKc07yhxgs4v87ivcl5balgk36q" FOREIGN KEY (bundle_folder_id) REFERENCES public.bundle_folder(id);

ALTER TABLE public.versioned_document_task
    ADD CONSTRAINT "FKh58jkv39h3yi38u9x2yx9efbn" FOREIGN KEY (callback_id) REFERENCES public.callback(id);

ALTER TABLE public.versioned_document_task
    ADD CONSTRAINT "FKnt0iy5u9v1r35jpjqtghhs31s" FOREIGN KEY (bundle_id) REFERENCES public.bundle(id);

ALTER TABLE public.bundle_folders
    ADD CONSTRAINT "FKou62qgy15phpqte2j8shpm3bc" FOREIGN KEY (bundle_id) REFERENCES public.bundle(id);

ALTER TABLE public.bundle_folder_folders
    ADD CONSTRAINT "FKrqte5ksaowlvc8fgd4hkv60h8" FOREIGN KEY (bundle_folder_id) REFERENCES public.bundle_folder(id);

ALTER TABLE public.bundle_folder_documents
    ADD CONSTRAINT "FKyhn0o144ru23c7gvxax3gpy0" FOREIGN KEY (documents_id) REFERENCES public.bundle_document(id);

ALTER TABLE public.batch_job_execution_context
    ADD CONSTRAINT job_exec_ctx_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id);

ALTER TABLE public.batch_job_execution_params
    ADD CONSTRAINT job_exec_params_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id);

ALTER TABLE public.batch_step_execution
    ADD CONSTRAINT job_exec_step_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id);

ALTER TABLE public.batch_job_execution
    ADD CONSTRAINT job_inst_exec_fk FOREIGN KEY (job_instance_id) REFERENCES public.batch_job_instance(job_instance_id);

ALTER TABLE public.batch_step_execution_context
    ADD CONSTRAINT step_exec_ctx_fk FOREIGN KEY (step_execution_id) REFERENCES public.batch_step_execution(step_execution_id);