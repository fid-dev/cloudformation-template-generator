package com.monsanto.arch.cloudformation.model.resource

import com.monsanto.arch.cloudformation.model._
import org.scalatest.{FunSpec, Matchers}
import spray.json.{JsString, _}

class EC2_UT extends FunSpec with Matchers {

  describe("CidrBlock"){

    val cidr = CidrBlock(192,168,1,2,32)

    it("should write valid CidrBlock"){
      cidr.toJson shouldEqual JsString("192.168.1.2/32")
    }

    it("should read valid CidrBlock") {
      JsString("192.168.1.2/32").convertTo[CidrBlock] shouldEqual cidr
    }

  }

  describe("IPAddress"){
    val ipAddress = IPAddress(192,168,1,2)

    it("should write valid IPAddress"){
      ipAddress.toJson shouldEqual JsString("192.168.1.2")
    }

    it("should read valid IPAddress"){
      JsString("192.168.1.2").convertTo[IPAddress] shouldEqual ipAddress
    }

  }

  describe("VPN"){
    val vpc = `AWS::EC2::VPC`(name = "vpc", CidrBlock(1,1,1,1,16),Seq())
    val vgwName = "vgw"
    val vgw = `AWS::EC2::VPNGateway`(
      vgwName,
      Tags = Seq()
    )
    it("should write valid VPN gateway") {
      val expected = JsObject(
        vgwName -> JsObject(
          "Type" -> JsString("AWS::EC2::VPNGateway"),
          "Properties" -> JsObject(
            "Type" -> JsString("ipsec.1"),
            "Tags" -> JsArray()
          )
        )
      )
      Seq[Resource[_]](vgw).toJson should be(expected)
    }

    val vgaName = "vpnAttachment"
    val vpnGwyAttch = `AWS::EC2::VPCGatewayAttachment`(
      vgaName,
      vpc,
      vgw
    )
    it("should write valid VPN attachment") {
      val expected = JsObject(
        vgaName -> JsObject(
          "Type" -> JsString("AWS::EC2::VPCGatewayAttachment"),
          "Properties" -> JsObject(
            "VpcId" -> JsObject("Ref" -> JsString("vpc")),
            "VpnGatewayId" -> JsObject("Ref" -> JsString(vgwName))
          )
        )
      )
      Seq[Resource[_]](vpnGwyAttch).toJson should be(expected)
    }

    val cgwName = "cgw"
    val bpgAsn = 6000
    val cgw = `AWS::EC2::CustomerGateway`(
      cgwName,
      bpgAsn,
      IPAddress(1,1,1,1),
      Seq()
    )
    it("should create a valid new Customer Gateway") {
      val expected = JsObject(
        "cgw" -> JsObject(
          "Type" -> JsString("AWS::EC2::CustomerGateway"),
          "Properties" -> JsObject(
            "BgpAsn" -> JsNumber(bpgAsn),
            "IpAddress" -> JsString("1.1.1.1"),
            "Tags" -> JsArray(),
            "Type" -> JsString("ipsec.1")
          )
        )
      )
      Seq[Resource[_]](cgw).toJson should be(expected)
    }

    val vConnName = "vpnConnection"
    val vpnConn = `AWS::EC2::VPNConnection`(
      vConnName,
      cgw,
      false,
      vgw,
      Seq()
    )
    it("should write valid VPN Connection") {
      val expected = JsObject(
        vConnName -> JsObject(
          "Type" -> JsString("AWS::EC2::VPNConnection"),
          "Properties" -> JsObject(
            "CustomerGatewayId" -> JsObject("Ref" -> JsString(cgwName)),
            "StaticRoutesOnly" -> JsBoolean(false),
            "VpnGatewayId" -> JsObject("Ref" -> JsString(vgwName)),
            "Tags" -> JsArray()
          )
        )
      )
      Seq[Resource[_]](vpnConn).toJson should be(expected)
    }
    val vConnRouteName = "vpnConnectionRoute"
    val vConnRoute = `AWS::EC2::VPNConnectionRoute`(
      vConnRouteName,
      CidrBlock(1,1,1,1,16),
      vpnConn
    )
    it("should write valid VPNConnectionRoute") {
      val expected = JsObject(
        vConnRouteName -> JsObject(
          "Type" -> JsString("AWS::EC2::VPNConnectionRoute"),
          "Properties" -> JsObject(
            "DestinationCidrBlock" -> JsString("1.1.1.1/16"),
            "VpnConnectionId" -> JsObject("Ref" -> JsString(vConnName))
          )
        )
      )
      Seq[Resource[_]](vConnRoute).toJson should be(expected)
    }
    val nAclName = "nACL"
    val nAcl = `AWS::EC2::NetworkAcl`(
      nAclName,
      vpc,
      Seq()
    )
    it("should write a valid network acl"){
      val expected = JsObject(
        nAclName -> JsObject(
          "Type" -> JsString("AWS::EC2::NetworkAcl"),
          "Properties" -> JsObject(
            "VpcId" -> JsObject("Ref" -> JsString("vpc")),
            "Tags" -> JsArray()
          )
        )
      )
      Seq[Resource[_]](nAcl).toJson should be(expected)
    }
    val nAclEntryName = "nAclEntry"
    val nAclEntry = `AWS::EC2::NetworkAclEntry`(
      nAclEntryName,
      CidrBlock(1,1,1,1,16),
      false,
      EC2IcmpProperty(1, 1),
      nAcl,
      PortRange(2, 6),
      Protocol(-1),
      RuleAction("allow"),
      RuleNumber(2)
    )
    it("should write a valid network acl entry"){
      val expected = JsObject(
        nAclEntryName -> JsObject(
          "Type" -> JsString("AWS::EC2::NetworkAclEntry"),
          "Properties" -> JsObject(
            "CidrBlock" -> JsString("1.1.1.1/16"),
            "Egress" -> JsBoolean(false),
            "Icmp" -> JsObject("Code" -> JsNumber(1), "Type" -> JsNumber(1)),
            "NetworkAclId" -> JsObject("Ref" -> JsString(nAclName)),
            "PortRange" -> JsObject("From" -> JsNumber(2), "To" -> JsNumber(6)),
            "Protocol" -> JsNumber(-1),
            "RuleAction" -> JsString("allow"),
            "RuleNumber" -> JsNumber(2)
          )
        )
      )
      Seq[Resource[_]](nAclEntry).toJson should be(expected)
    }
  }

  describe("AWS::EC2::SecurityGroupIngress") {
    val securityGroupParam = `AWS::EC2::SecurityGroup_Parameter`("testSGParam", "testSGParam")
    val ingres = `AWS::EC2::SecurityGroupIngress`(
      name = "Test Ingress",
      GroupId = ParameterRef(securityGroupParam),
      IpProtocol = "TCP",
      FromPort = "80",
      ToPort = "80",
      CidrIp = Some(CidrBlock(192, 168, 1, 2, 32))
    )

    it("should write valid SecurityGroupIngress"){
      ingres.toJson shouldEqual JsObject(
        "name" -> JsString("Test Ingress"),
        "CidrIp" -> JsString("192.168.1.2/32"),
        "GroupId" -> JsObject("Ref" -> JsString("testSGParam")),
        "ToPort" -> JsString("80"),
        "FromPort" -> JsString("80"),
        "IpProtocol" -> JsString("TCP")
      )
    }

    it("should write valid SecurityGroupIngress when parameterized"){
      val portParam = StringParameter("portParam")
      val ingresParameterized = ingres.copy(
        FromPort = ParameterRef(portParam),
        ToPort = ParameterRef(portParam)
      )
      ingresParameterized.toJson shouldEqual JsObject(
        "name" -> JsString("Test Ingress"),
        "CidrIp" -> JsString("192.168.1.2/32"),
        "GroupId" -> JsObject("Ref" -> JsString("testSGParam")),
        "ToPort" -> JsObject("Ref" -> JsString("portParam")),
        "FromPort" -> JsObject("Ref" -> JsString("portParam")),
        "IpProtocol" -> JsString("TCP")
      )
    }
  }
}
