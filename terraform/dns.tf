# Get Route53 hosted zone
data "aws_route53_zone" "main" {
  name         = "hulft.predev.aws"
  private_zone = false
}

# Route53 Record
resource "aws_route53_record" "main" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = aws_lb.main.dns_name
    zone_id                = aws_lb.main.zone_id
    evaluate_target_health = true
  }
}
