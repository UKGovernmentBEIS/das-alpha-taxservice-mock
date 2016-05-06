package uk.gov.bis.controllers

import java.security.SecureRandom

import org.apache.commons.codec.binary.Hex

package object auth {

  private val random = new SecureRandom()
  random.nextBytes(new Array[Byte](55))

  def generateToken: String = {
    val bytes = new Array[Byte](12)
    random.nextBytes(bytes)
    new String(Hex.encodeHex(bytes))
  }
}
