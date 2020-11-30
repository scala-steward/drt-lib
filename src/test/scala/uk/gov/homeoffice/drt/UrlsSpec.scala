package uk.gov.homeoffice.drt

import com.typesafe.config.{Config, ConfigFactory}
import org.specs2.mutable.Specification

class UrlsSpec extends Specification {
  val config: Config = ConfigFactory.load

  val useHttps: Boolean = true
  val rootDomain: String = "some-domain.com"

  val urls: Urls = Urls(rootDomain, useHttps)

  "Given a LHR DRT port url" >> {
    "When I ask for the port" >> {
      "I should get LHR" >> {
        val lhrUrl = "https://lhr." + rootDomain + "/"
        val portCode = urls.portCodeFromUrl(lhrUrl)

        val expected = Option("LHR")

        portCode === expected
      }
    }
  }

  "Given a url without a port" >> {
    "When I ask for the port" >> {
      "I should get None" >> {
        val noPortUrl = "https://" + rootDomain + "/"
        val portCode = urls.portCodeFromUrl(noPortUrl)

        val expected = None

        portCode === expected
      }
    }
  }

  "Given a port code and an application domain" >> {
    "When I ask for the logout url" >> {
      "I should get port url with the logout path appended" >> {
        val logoutUrl = urls.logoutUrlForPort("lhr")

        val expected = "https://lhr." + rootDomain + "/oauth/logout?redirect=https://lhr." + rootDomain

        logoutUrl === expected
      }
    }
  }
}
