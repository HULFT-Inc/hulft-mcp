# Get Route53 hosted zone (optional)
data "aws_route53_zone" "main" {
  count        = var.create_dns_record ? 1 : 0
  name         = "hulftincpredev.net"
  private_zone = false
}

# Route53 Record (optional)
resource "aws_route53_record" "main" {
  count   = var.create_dns_record ? 1 : 0
  zone_id = data.aws_route53_zone.main[0].zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = aws_lb.main.dns_name
    zone_id                = aws_lb.main.zone_id
    evaluate_target_health = true
  }
}
