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

variable ilbIp{}

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

variable dns_server {}