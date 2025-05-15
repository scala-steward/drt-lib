package uk.gov.homeoffice.drt.models

sealed trait DocumentType

object DocumentType {
  object Visa extends DocumentType {
    override def toString: String = "V"
  }

  object Passport extends DocumentType {
    override def toString: String = "P"
  }
  object InvalidDocument extends DocumentType {
    override def toString: String = "_"
  }

  def apply(docTypeCode: String): DocumentType = docTypeCode.toUpperCase() match {
    case "V" => Visa
    case "P" | "PASSPORT" => Passport
    case _ => InvalidDocument
  }
}
