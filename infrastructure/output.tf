// used for db migrations
output "microserviceName" {
  value = "${local.app_full_name}"
}

// used for db migrations
output "vaultName" {
  value = "${module.local_key_vault.key_vault_name}"
}

output "enable_idam_health_check" {
  value = "${var.enable_idam_healthcheck}"
}

output "enable_idam_healthcheck" {
  value = "${var.enable_idam_healthcheck}"
}
