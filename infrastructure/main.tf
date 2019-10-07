provider "azurerm" {
  version = "1.23.0"
}

locals {
  app_full_name = "${var.product}-${var.component}"
  ase_name = "core-compute-${var.env}"
  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"
  shared_vault_name = "${var.shared_product_name}-${local.local_env}"

  previewVaultName = "${local.app_full_name}-aat"
  nonPreviewVaultName = "${local.app_full_name}-${var.env}"
  vaultName = "${(var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName}"
}

module "app" {
  source = "git@github.com:hmcts/cnp-module-webapp?ref=master"
  product = "${local.app_full_name}"
  location = "${var.location}"
  env = "${var.env}"
  ilbIp = "${var.ilbIp}"
  subscription = "${var.subscription}"
  capacity     = "${var.capacity}"
  is_frontend = false
  additional_host_name = "${local.app_full_name}-${var.env}.service.${var.env}.platform.hmcts.net"
  https_only="false"
  common_tags  = "${var.common_tags}"
  asp_rg = "${var.shared_product_name}-${var.env}"
  asp_name = "${var.shared_product_name}-bundling-${var.env}"
  appinsights_instrumentation_key = "${data.azurerm_key_vault_secret.app_insights_key.value}"
  enable_ase                      = "${var.enable_ase}"

  app_settings = {
    POSTGRES_HOST = "${module.db.host_name}"
    POSTGRES_PORT = "${module.db.postgresql_listen_port}"
    POSTGRES_DATABASE = "${module.db.postgresql_database}"
    POSTGRES_USER = "${module.db.user_name}"
    POSTGRES_PASSWORD = "${module.db.postgresql_password}"
    MAX_ACTIVE_DB_CONNECTIONS = 70

    # JAVA_OPTS = "${var.java_opts}"
    # SERVER_PORT = "8080"

    # db
    SPRING_DATASOURCE_URL = "jdbc:postgresql://${module.db.host_name}:${module.db.postgresql_listen_port}/${module.db.postgresql_database}?sslmode=require"
    SPRING_DATASOURCE_USERNAME = "${module.db.user_name}"
    SPRING_DATASOURCE_PASSWORD = "${module.db.postgresql_password}"

    ENABLE_DB_MIGRATE="false"

    # idam
    IDAM_API_BASE_URI = "${var.idam_api_base_uri}"
    S2S_BASE_URI = "http://${var.s2s_url}-${local.local_env}.service.core-compute-${local.local_env}.internal"
    S2S_KEY = "${data.azurerm_key_vault_secret.s2s_key.value}"
    DOCMOSIS_ACCESS_KEY = "${data.azurerm_key_vault_secret.docmosis_access_key.value}"

    #DM STORE
    DM_STORE_APP_URL = "http://${var.dm_store_app_url}-${local.local_env}.service.core-compute-${local.local_env}.internal"

    # logging vars & healthcheck
    REFORM_SERVICE_NAME = "${local.app_full_name}"
    REFORM_TEAM = "${var.team_name}"
    REFORM_SERVICE_TYPE = "${var.app_language}"
    REFORM_ENVIRONMENT = "${var.env}"

    PACKAGES_NAME = "${local.app_full_name}"
    PACKAGES_PROJECT = "${var.team_name}"
    PACKAGES_ENVIRONMENT = "${var.env}"

    JSON_CONSOLE_PRETTY_PRINT = "${var.json_console_pretty_print}"
    LOG_OUTPUT = "${var.log_output}"

    # addtional log
    ROOT_LOGGING_LEVEL = "${var.root_logging_level}"
    SHOW_SQL = "${var.show_sql}"

    ENDPOINTS_HEALTH_SENSITIVE = "${var.endpoints_health_sensitive}"
    ENDPOINTS_INFO_SENSITIVE = "${var.endpoints_info_sensitive}"
    CASE_WORKER_ROLES = "${var.case_worker_roles}"

    # Toggles
    ENABLE_IDAM_HEALTH_CHECK = "${var.enable_idam_healthcheck}"
    ENABLE_S2S_HEALTH_CHECK = "${var.enable_s2s_healthcheck}"

    DOCMOSIS_ENDPOINT = "${var.docmosis_uri}"

    WEBSITE_DNS_SERVER = "${var.dns_server}"
    managed_identity_object_id = "${var.managed_identity_object_id}"
  }
}

module "db" {
  source = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product = "${local.app_full_name}-postgres-db"
  location = "${var.location}"
  env = "${var.env}"
  postgresql_user = "${var.postgresql_user}"
  database_name = "${var.database_name}"
  sku_name = "GP_Gen5_2"
  sku_tier = "GeneralPurpose"
  storage_mb = "51200"
  common_tags  = "${var.common_tags}"
  subscription = "${var.subscription}"
}

module "local_key_vault" {
  source = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  product = "${local.app_full_name}"
  env = "${var.env}"
  tenant_id = "${var.tenant_id}"
  object_id = "${var.jenkins_AAD_objectId}"
  resource_group_name = "${module.app.resource_group_name}"
  product_group_object_id = "5d9cd025-a293-4b97-a0e5-6f43efce02c0"
  common_tags = "${var.common_tags}"
  managed_identity_object_id = "${var.managed_identity_object_id}"
}

provider "vault" {
  address = "https://vault.reform.hmcts.net:6200"
}

data "azurerm_key_vault" "s2s_vault" {
  name = "s2s-${local.local_env}"
  resource_group_name = "rpe-service-auth-provider-${local.local_env}"
}

data "azurerm_key_vault_secret" "s2s_key" {
  name      = "microservicekey-em-stitching-api"
  key_vault_id = "${data.azurerm_key_vault.s2s_vault.id}"
}


data "azurerm_key_vault_secret" "docmosis_access_key" {
  name      = "docmosis-access-key"
  key_vault_id = "${data.azurerm_key_vault.shared_key_vault.id}"
}

data "azurerm_key_vault" "shared_key_vault" {
  name = "${local.shared_vault_name}"
  resource_group_name = "${local.shared_vault_name}"
}

data "azurerm_key_vault" "product" {
  name = "${var.shared_product_name}-${var.env}"
  resource_group_name = "${var.shared_product_name}-${var.env}"
}

# Copy s2s key from shared to local vault
data "azurerm_key_vault" "local_key_vault" {
  name = "${module.local_key_vault.key_vault_name}"
  resource_group_name = "${module.local_key_vault.key_vault_name}"
}

resource "azurerm_key_vault_secret" "local_s2s_key" {
  name         = "microservicekey-em-stitching-api"
  value        = "${data.azurerm_key_vault_secret.s2s_key.value}"
  key_vault_id = "${data.azurerm_key_vault.local_key_vault.id}"
}

# Copy docmosis keys to local
resource "azurerm_key_vault_secret" "local_docmosis_access_key" {
  name         = "docmosis-access-key"
  value        = "${data.azurerm_key_vault_secret.docmosis_access_key.value}"
  key_vault_id = "${data.azurerm_key_vault.local_key_vault.id}"
}

# Load AppInsights key from rpa vault
data "azurerm_key_vault_secret" "app_insights_key" {
  name         = "AppInsightsInstrumentationKey"
  key_vault_id = "${data.azurerm_key_vault.product.id}"
}

resource "azurerm_key_vault_secret" "local_app_insights_key" {
  name         = "AppInsightsInstrumentationKey"
  value        = "${data.azurerm_key_vault_secret.app_insights_key.value}"
  key_vault_id = "${data.azurerm_key_vault.local_key_vault.id}"
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name = "${var.component}-POSTGRES-USER"
  value = "${module.db.user_name}"
  key_vault_id = "${data.azurerm_key_vault.local_key_vault.id}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name = "${var.component}-POSTGRES-PASS"
  value = "${module.db.postgresql_password}"
  key_vault_id = "${data.azurerm_key_vault.local_key_vault.id}"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name = "${var.component}-POSTGRES-HOST"
  value = "${module.db.host_name}"
  key_vault_id = "${data.azurerm_key_vault.local_key_vault.id}"
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name = "${var.component}-POSTGRES-PORT"
  value = "${module.db.postgresql_listen_port}"
  key_vault_id = "${data.azurerm_key_vault.local_key_vault.id}"
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name = "${var.component}-POSTGRES-DATABASE"
  value = "${module.db.postgresql_database}"
  key_vault_id = "${data.azurerm_key_vault.local_key_vault.id}"
}
