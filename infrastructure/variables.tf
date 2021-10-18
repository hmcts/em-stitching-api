variable product {
  default = "em"
}

variable shared_product_name {
  default = "rpa"
}

variable component {
  default = "stitching"
}

variable team_name {
  default = "evidence"
}

variable app_language {
  default = "java"
}

variable location {
  default = "UK South"
}

variable env {}

variable subscription {}

variable ilbIp {}

variable tenant_id {}

variable jenkins_AAD_objectId {
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable common_tags {
  type = map(string)
}
////////////////////////////////////////////////
//Addtional Vars ///////////////////////////////
////////////////////////////////////////////////
variable capacity {
  default = "1"
}

variable java_opts {
  default = ""
}
////////////////////////////////////////////////
// Endpoints
////////////////////////////////////////////////
variable idam_api_base_uri {
  default = "http://betaDevBccidamAppLB.reform.hmcts.net:80"
}

variable open_id_api_base_uri {
  default = "idam-api"
}

variable s2s_url {
  default = "rpe-service-auth-provider"
}

variable dm_store_app_url {
  default = "dm-store"
}

variable postgresql_user {
  default = "annotation"
}

variable database_name {
  default = "emstitch"
}

variable postgresql_user_v11 {
  default = "emstitch"
}

variable database_name_v11 {
  default = "emstitch"
}
////////////////////////////////////////////////
// Logging
////////////////////////////////////////////////
variable json_console_pretty_print {
  default = "false"
}

variable log_output {
  default = "single"
}

variable root_logging_level {
  default = "INFO"
}

variable show_sql {
  default = "true"
}

variable endpoints_health_sensitive {
  default = "true"
}

variable endpoints_info_sensitive {
  default = "true"
}
////////////////////////////////////////////////
// Toggle Features
////////////////////////////////////////////////
variable enable_idam_healthcheck {
    default = "false"
}

variable enable_s2s_healthcheck {
    default = "false"
}

////////////////////////////////////////////////
// Whitelists
////////////////////////////////////////////////

variable case_worker_roles {
  default = "caseworker-probate,caseworker-cmc,caseworker-sscs,caseworker-divorce"
}
////////////////////////////////////////////////
// Addtional
////////////////////////////////////////////////

variable docmosis_uri {
  default = "https://docmosis-development.platform.hmcts.net/rs/convert"
}

variable docmosis_render_uri {
  default = "https://docmosis-development.platform.hmcts.net/rs/render"
}

variable dns_server {}

