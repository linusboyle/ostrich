/*
 * This file is part of Ostrich, an SMT solver for strings.
 * Copyright (C) 2019-2020  Matthew Hague, Philipp Ruemmer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ostrich

import ap.theories.strings.{StringTheory, StringTheoryBuilder, SeqStringTheory}
import ap.util.CmdlParser

import scala.collection.mutable.ArrayBuffer

/**
 * The entry class of the Ostrich string solver.
 */
class OstrichStringTheoryBuilder extends StringTheoryBuilder {

  val name = "OSTRICH"

  Console.withOut(Console.err) {
    println
    println("Loading " + name + ", a solver for string constraints")
    println("(c) Matthew Hague, Philipp Rümmer, 2018-2020")
    println("For more information, see https://github.com/uuverifiers/ostrich")
    println
  }

  def setAlphabetSize(w : Int) : Unit = ()

  private var eager, forward = false
  private var useLen : OFlags.LengthOptions.Value = OFlags.LengthOptions.Auto

  override def parseParameter(str : String) : Unit = str match {
    case CmdlParser.Opt("eager", value) =>
      eager = value
    case CmdlParser.ValueOpt("length", "off") =>
      useLen = OFlags.LengthOptions.Off
    case CmdlParser.ValueOpt("length", "on") =>
      useLen = OFlags.LengthOptions.On
    case CmdlParser.ValueOpt("length", "auto") =>
      useLen = OFlags.LengthOptions.Auto
    case CmdlParser.Opt("forward", value) =>
      forward = value
    case str =>
      super.parseParameter(str)
  }

  import StringTheoryBuilder._
  import ap.parser._
  import IExpression._

  lazy val getTransducerTheory : Option[StringTheory] =
    Some(SeqStringTheory(OstrichStringTheory.alphabetSize))

  private val transducers = new ArrayBuffer[(String, SymTransducer)]

  def addTransducer(name : String, transducer : SymTransducer) : Unit = {
    assert(!createdTheory)
    transducers += ((name, transducer))
  }

  private var createdTheory = false

  lazy val theory = {
    createdTheory = true

    val symTransducers =
      for ((name, transducer) <- transducers) yield {
        Console.err.println("Translating transducer " + name + " ...")
        val aut = TransducerTranslator.toBricsTransducer(
                    transducer, OstrichStringTheory.alphabetSize,
                    getTransducerTheory.get)
        (name, aut)
      }

    new OstrichStringTheory (symTransducers,
                             OFlags(eagerAutomataOperations = eager,
                                    useLength = useLen,
                                    forwardApprox = forward))
  }

}
