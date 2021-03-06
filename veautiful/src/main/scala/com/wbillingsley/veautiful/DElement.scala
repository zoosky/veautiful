package com.wbillingsley.veautiful

import org.scalajs.dom
import org.scalajs.dom.{Event, html}

case class Lstnr(`type`:String, func:Event => _, usCapture:Boolean=false)
case class AttrVal(name:String, value:String)
case class InlineStyle(name:String, value:String)
case class EvtListener[T](`type`:String, f:Function[T, _], capture:Boolean)


object DElement {
  val htmlNS = "http://www.w3.org/1999/xhtml"
  val svgNS = "http://www.w3.org/2000/svg"
}
case class DElement(name:String, uniqEl:Any = "", ns:String = DElement.htmlNS) extends DiffNode {

  var children:Seq[VNode] = Seq.empty

  var attributes:Map[String, AttrVal] = Map.empty

  var listeners:Map[String, Lstnr] = Map.empty

  var styles:Seq[InlineStyle] = Seq.empty

  override var domNode:Option[dom.Element] = None

  def domEl = domNode.collect({ case e:dom.Element => e })

  /**
    * In DOM Events, it is very difficult to de-register an anonymous listener.
    * And in our code, it is very easy to end up with an event listener being an anonymous
    * function.
    * So, in order to make changing event listeners practical, instead of registering the
    * event listener, we always register this dispatcher function as the event listener.
    */
  def eventDispatch(e:Event):Unit = {
    for {
      h <- listeners.get(e.`type`)
    } h.func.apply(e)
  }

  val updateSelf = {
    case el:DElement =>
      val removeA = attributes.values.filter({ a => !el.attributes.contains(a.name)})
      val updateA = el.attributes.values.filter({ a => !attributes.get(a.name).map(_.value).contains(a.value)})
      //removeAttrsFromNode(attributes.filter({ x => !el.attributes.contains(x._1) }).values)
      removeAttrsFromNode(removeA)
      attributes = el.attributes
      //applyAttrsToNode(attributes.values)
      applyAttrsToNode(updateA)

      removeStylesFromNode(styles)
      styles = el.styles
      applyStylesToNode(styles)

      if (listeners.keys != el.listeners.keys) {
        println("Listeners differ")
        removeLsntrsFromNode(listeners.values)
        applyLsntrsToNode(listeners.values)
      }
      listeners = el.listeners

  }

  def applyAttrsToNode(as:Iterable[AttrVal]):Unit = {
    for { n <- domEl; a <- as } {
      n.setAttribute(a.name, a.value)
    }
  }

  def removeAttrsFromNode(as:Iterable[AttrVal]):Unit = {
    for { n <- domEl; a <- as } {
      n.removeAttribute(a.name)
    }
  }

  def applyLsntrsToNode(as:Iterable[Lstnr]):Unit = {
    for { n <- domEl; a <- as } {
      n.addEventListener(a.`type`, eventDispatch, false)
    }
  }

  def removeLsntrsFromNode(as:Iterable[Lstnr]):Unit = {
    for { n <- domEl; a <- as } {
      n.removeEventListener(a.`type`, eventDispatch, false)
    }
  }

  def style(s:InlineStyle*) = {
    styles ++= s
    this
  }

  def applyStylesToNode(as:Iterable[InlineStyle]):Unit = {
    domEl match {
      case Some(h:html.Element) => as.foreach({x => h.style.setProperty(x.name, x.value) })
      case _ => // nothing
    }
  }

  def removeStylesFromNode(as:Iterable[InlineStyle]):Unit = {
    domEl match {
      case Some(h:html.Element) => as.foreach({x => h.style.removeProperty(x.name) })
      case _ => // nothing
    }
  }

  def attrs(attrs:AttrVal*) = {
    attributes ++= attrs.map({ x => x.name -> x }).toMap
    this
  }

  def children(ac:VNode*):DElement = {
    children ++= ac
    this
  }

  def applyAppliable(a: <.DElAppliable) = a match {
    case attr: <.DEAAttr => attrs(attr.a)
    case l: <.DEALstnr => on(l.l)
    case s: <.DEAStyle => style(s.s)
    case n: <.DEAVNode => children(n.vNode)
    case nodes: <.DEAIVNode => children(nodes.nodes.toSeq : _*)
  }

  def apply(ac: <.DElAppliable *):DElement = {
    ac.foldLeft(this)({ case (x, y) => x.applyAppliable(y) })
  }

  def on(l: Lstnr *) = {
    listeners ++= l.map({x => x.`type` -> x }).toMap
    this
  }


  def create() = {
    val e = if (ns == DElement.htmlNS) {
      dom.document.createElement(name)
    } else {
      dom.document.createElementNS(ns, name)
    }

    for { AttrVal(a, value) <- attributes.values } {
      e.setAttribute(a, value)
    }

    for { Lstnr(t, f, cap) <- listeners.values } {
      e.addEventListener(t, f, cap)
    }

    applyStylesToNode(styles)

    e
  }


}

case class Text(text:String) extends VNode {

  var domNode:Option[dom.Node] = None

  def create() = {
    dom.document.createTextNode(text)
  }

  def attach() = {
    val n = create()
    domNode = Some(n)
    n
  }

  def detach() = {
    domNode = None
  }

}

object < {

  trait DElAppliable
  implicit class DEAVNode(val vNode: VNode) extends DElAppliable
  implicit class DEAIVNode(val nodes: Iterable[VNode]) extends DElAppliable
  implicit class DEAAttr(val a: AttrVal) extends DElAppliable
  implicit class DEALstnr(val l: Lstnr) extends DElAppliable
  implicit class DEAStyle(val s:InlineStyle) extends DElAppliable
  implicit def DEAText(t: String):DElAppliable = new DEAVNode(Text(t))


  def p = apply("p")
  def div = apply("div")
  def img = apply("img")
  def a = apply("a")
  def span = apply("span")
  def h1 = apply("h1")
  def h2 = apply("h2")
  def h3 = apply("h3")
  def h4 = apply("h4")

  def button = apply("button")
  def input = apply("input")

  def ol = apply("ol")
  def ul = apply("ul")
  def li = apply("li")

  def table = apply("table")
  def thead = apply("thead")
  def tbody = apply("tbody")
  def tr = apply("tr")
  def th = apply("th")
  def td = apply("td")

  def svg = apply("svg", ns=DElement.svgNS)
  def circle = apply("circle", ns=DElement.svgNS)
  def polygon = apply("polygon", ns=DElement.svgNS)


  def apply(n:String, u:String = "", ns:String = DElement.htmlNS) = DElement(n, u, ns)

}

object ^ {

  case class Attrable(n:String) {
    def :=(s:String) = AttrVal(n, s)

    def :=(i:Int) = AttrVal(n, i.toString)

    def :=(d:Double) = AttrVal(n, d.toString)
  }

  def attr(x:String) = Attrable(x)

  def alt = Attrable("alt")
  def src = Attrable("src")
  def `class` = Attrable("class")
  def cls = `class`
  def role = Attrable("role")
  def href = Attrable("href")

  case class Lsntrable(n:String) {
    def -->(e: => Unit ) = Lstnr(n, (x:Event) => e, false)

    def ==>(f: (Event) => Unit) = Lstnr(n, (x:Event) => f(x))
  }

  def onClick = Lsntrable("click")
  def on(s:String) = Lsntrable(s)

  case class InlineStylable(n:String) {
    def :=(v:String) = InlineStyle(n, v)
  }

  def minHeight = InlineStylable("min-height")
  def backgroundImage = InlineStylable("background-image")

}
