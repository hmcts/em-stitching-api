provider "azurerm" {
  features {}
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "cft_vnet"
  subscription_id            = var.aks_subscription_id
}

locals {
  app_full_name = "${var.product}-${var.component}"
  local_env = (var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env
  shared_vault_name = "${var.shared_product_name}-${local.local_env}"

  previewVaultName = "${local.app_full_name}-aat"
  nonPreviewVaultName = "${local.app_full_name}-${var.env}"
  vaultName = (var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName
}

resource "azurerm_resource_group" "rg" {
  name     = "${local.app_full_name}-${var.env}"
  location = var.location
  tags = var.common_tags
}

data "azurerm_subnet" "postgres" {
  name                 = "core-infra-subnet-0-${var.env}"
  resource_group_name  = "core-infra-${var.env}"
  virtual_network_name = "core-infra-vnet-${var.env}"
}

module "db-v11" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=postgresql_tf"
  product            = var.product
  component          = var.component
  name               = join("-", [var.product,var.component,"postgres-db-v11"])
  location           = var.location
  env                = var.env
  postgresql_user    = var.postgresql_user_v11
  database_name      = var.database_name_v11
  postgresql_version = "11"
  subnet_id          = data.azurerm_subnet.postgres.id
  sku_name           = var.sku_name
  sku_capacity       = var.sku_capacity
  sku_tier           = "GeneralPurpose"
  storage_mb         = var.database_storage_mb
  common_tags        = var.common_tags
  subscription       = var.subscription
}

module "local_key_vault" {
  source = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  product = local.app_full_name
  env = var.env
  tenant_id = var.tenant_id
  object_id = var.jenkins_AAD_objectId
  resource_group_name = "${local.app_full_name}-${var.env}"
  product_group_object_id = "5d9cd025-a293-4b97-a0e5-6f43efce02c0"
  common_tags = var.common_tags
  managed_identity_object_ids = [data.azurerm_user_assigned_identity.rpa-shared-identity.principal_id]
}

data "azurerm_user_assigned_identity" "rpa-shared-identity" {
  name                = "rpa-${var.env}-mi"
  resource_group_name = "managed-identities-${var.env}-rg"
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
  key_vault_id = data.azurerm_key_vault.s2s_vault.id
}


data "azurerm_key_vault_secret" "docmosis_access_key" {
  name      = "docmosis-access-key"
  key_vault_id = data.azurerm_key_vault.shared_key_vault.id
}

data "azurerm_key_vault" "shared_key_vault" {
  name = local.shared_vault_name
  resource_group_name = local.shared_vault_name
}

data "azurerm_key_vault" "product" {
  name = "${var.shared_product_name}-${var.env}"
  resource_group_name = "${var.shared_product_name}-${var.env}"
}

# Copy s2s key from shared to local vault
data "azurerm_key_vault" "local_key_vault" {
  name = module.local_key_vault.key_vault_name
  resource_group_name = module.local_key_vault.key_vault_name
}

resource "azurerm_key_vault_secret" "local_s2s_key" {
  name         = "microservicekey-em-stitching-api"
  value        = data.azurerm_key_vault_secret.s2s_key.value
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

# Copy docmosis keys to local
resource "azurerm_key_vault_secret" "local_docmosis_access_key" {
  name         = "docmosis-access-key"
  value        = data.azurerm_key_vault_secret.docmosis_access_key.value
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

# Load AppInsights key from rpa vault
data "azurerm_key_vault_secret" "app_insights_key" {
  name         = "EmAppInsightsInstrumentationKey"
  key_vault_id = data.azurerm_key_vault.product.id
}

resource "azurerm_key_vault_secret" "local_app_insights_key" {
  name         = "AppInsightsInstrumentationKey"
  value        = data.azurerm_key_vault_secret.app_insights_key.value
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

data "azurerm_key_vault_secret" "app_insights_connection_string" {
  name         = "em-app-insights-connection-string"
  key_vault_id = data.azurerm_key_vault.product.id
}

resource "azurerm_key_vault_secret" "local_app_insights_connection_string" {
  name         = "app-insights-connection-string"
  value        = data.azurerm_key_vault_secret.app_insights_connection_string.value
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name = "${var.component}-POSTGRES-USER"
  value = module.db-v11.user_name
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name = "${var.component}-POSTGRES-PASS"
  value = module.db-v11.postgresql_password
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name = "${var.component}-POSTGRES-HOST"
  value = module.db-v11.host_name
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name = "${var.component}-POSTGRES-PORT"
  value = module.db-v11.postgresql_listen_port
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name = "${var.component}-POSTGRES-DATABASE"
  value = module.db-v11.postgresql_database
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

# FlexiServer v14
module "db-v14" {
  providers = {
    azurerm.postgres_network = azurerm.cft_vnet
  }
  source               = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  env                  = var.env
  product              = var.product
  component            = var.component
  common_tags          = var.common_tags
  name                 = "${local.app_full_name}-postgres-db-v14"
  pgsql_version        = "14"
  admin_user_object_id = var.jenkins_AAD_objectId
  business_area        = "CFT"
  pgsql_databases      = [
    {
      name : "emstitch"
    }
  ]
  pgsql_server_configuration = [
    {
      name  = "azure.extensions"
      value = "plpgsql,pg_stat_statements,pg_buffercache"
    }
  ]
  //Below attributes needs to be overridden for Perftest & Prod
  pgsql_sku            = var.pgsql_sku
  pgsql_storage_mb     = var.pgsql_storage_mb
}

resource "azurerm_key_vault_secret" "POSTGRES-USER-V14" {
  name         = "${var.component}-POSTGRES-USER-V14"
  value        = module.db-v14.username
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS-V14" {
  name         = "${var.component}-POSTGRES-PASS-V14"
  value        = module.db-v14.password
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST-V14" {
  name         = "${var.component}-POSTGRES-HOST-V14"
  value        = module.db-v14.fqdn
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT-V14" {
  name         = "${var.component}-POSTGRES-PORT-V14"
  value        = "5432"
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE-V14" {
  name         = "${var.component}-POSTGRES-DATABASE-V14"
  value        = "emstitch"
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}
