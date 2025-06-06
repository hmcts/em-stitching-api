provider "azurerm" {
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "cft_vnet"
  subscription_id            = var.aks_subscription_id
}

locals {
  app_full_name     = "${var.product}-${var.component}"
  local_env         = (var.env == "preview" || var.env == "spreview") ? (var.env == "preview") ? "aat" : "saat" : var.env
  shared_vault_name = "${var.shared_product_name}-${local.local_env}"

  previewVaultName    = "${local.app_full_name}-aat"
  nonPreviewVaultName = "${local.app_full_name}-${var.env}"
  vaultName           = (var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName
  db_name             = "${local.app_full_name}-postgres-db-v15"
}

resource "azurerm_resource_group" "rg" {
  name     = "${local.app_full_name}-${var.env}"
  location = var.location
  tags     = var.common_tags
}

data "azurerm_subnet" "postgres" {
  name                 = "core-infra-subnet-0-${var.env}"
  resource_group_name  = "core-infra-${var.env}"
  virtual_network_name = "core-infra-vnet-${var.env}"
}

module "local_key_vault" {
  source                               = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  product                              = local.app_full_name
  env                                  = var.env
  tenant_id                            = var.tenant_id
  object_id                            = var.jenkins_AAD_objectId
  resource_group_name                  = "${local.app_full_name}-${var.env}"
  product_group_object_id              = "5d9cd025-a293-4b97-a0e5-6f43efce02c0"
  common_tags                          = var.common_tags
  managed_identity_object_ids          = [data.azurerm_user_assigned_identity.rpa-shared-identity.principal_id]
  additional_managed_identities_access = var.additional_managed_identities_access
}

data "azurerm_user_assigned_identity" "rpa-shared-identity" {
  name                = "rpa-${var.env}-mi"
  resource_group_name = "managed-identities-${var.env}-rg"
}

provider "vault" {
  address = "https://vault.reform.hmcts.net:6200"
}

data "azurerm_key_vault" "s2s_vault" {
  name                = "s2s-${local.local_env}"
  resource_group_name = "rpe-service-auth-provider-${local.local_env}"
}

data "azurerm_key_vault_secret" "s2s_key" {
  name         = "microservicekey-em-stitching-api"
  key_vault_id = data.azurerm_key_vault.s2s_vault.id
}


data "azurerm_key_vault_secret" "docmosis_access_key" {
  name         = "docmosis-access-key"
  key_vault_id = data.azurerm_key_vault.shared_key_vault.id
}

data "azurerm_key_vault" "shared_key_vault" {
  name                = local.shared_vault_name
  resource_group_name = local.shared_vault_name
}

data "azurerm_key_vault" "product" {
  name                = "${var.shared_product_name}-${var.env}"
  resource_group_name = "${var.shared_product_name}-${var.env}"
}

# Copy s2s key from shared to local vault
data "azurerm_key_vault" "local_key_vault" {
  name                = module.local_key_vault.key_vault_name
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
  name         = "${var.component}-POSTGRES-USER"
  value        = module.db-v15.username
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.component}-POSTGRES-PASS"
  value        = module.db-v15.password
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "${var.component}-POSTGRES-HOST"
  value        = module.db-v15.fqdn
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = "${var.component}-POSTGRES-PORT"
  value        = "5432"
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = "emstitch"
  key_vault_id = data.azurerm_key_vault.local_key_vault.id
}

# FlexiServer v15
module "db-v15" {
  providers = {
    azurerm.postgres_network = azurerm.cft_vnet
  }
  source                      = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  env                         = var.env
  product                     = var.product
  component                   = var.component
  common_tags                 = var.common_tags
  name                        = local.db_name
  pgsql_version               = "15"
  admin_user_object_id        = var.jenkins_AAD_objectId
  business_area               = "CFT"
  action_group_name           = join("-", [local.db_name, var.action_group_name, var.env])
  email_address_key           = var.email_address_key
  email_address_key_vault_id  = data.azurerm_key_vault.shared_key_vault.id
  # The original subnet is full, this is required to use the new subnet for new databases
  subnet_suffix = "expanded"
  pgsql_databases = [
    {
      name : "emstitch"
    }
  ]
  pgsql_server_configuration = [
    {
      name  = "azure.extensions"
      value = "pg_stat_statements,pg_buffercache,hypopg"
    }
  ]
  //Below attributes needs to be overridden for Perftest & Prod
  pgsql_sku                      = var.pgsql_sku
  pgsql_storage_mb               = var.pgsql_storage_mb
  force_user_permissions_trigger = "4"
}
