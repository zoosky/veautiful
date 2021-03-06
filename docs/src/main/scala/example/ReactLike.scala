package example

import com.wbillingsley.veautiful._
import example.Model.Asteroid
import org.scalajs.dom.raw.HTMLInputElement

/**
  * A version of the UI that works most closely to a React-style interface. The UI is
  * made up of elements, and the virtual DOM is diffed at each step.
  */
object ReactLike {

  /**
    * Model contains code simulating the position of asteroids -- ie, the model here is
    * the M in the MVC. And this page is the VC.
    */
  import Model._

  /**
    * The SVG that will contain the asteroid field
    */
  def svg:DElement = <.svg.attrs(
    ^.attr("width") := "640",
    ^.attr("height") := "480"
  )

  /** Turns an asteroid into an SVG DElement */
  def svgAsteroid(a:Asteroid):VNode = {

    /** Just defines the shape of an asteroid */
    def polyPoints:Seq[(Int, Int)] = Seq((-10, -2), (-5, 8), (0, 10), (4, 7), (10, -1), (0, -10))

    /** Useful for turning a point into a string suitable for a polygon's points attribute */
    def pointToString(p:(Int,Int)) = s"${p._1},${p._2} "

    /** Formats a polygon's SVG point string */
    def polyString(s:Seq[(Int, Int)]):String = {
      val sb = new StringBuilder()
      for { point <- s } sb.append(pointToString(point))
      sb.mkString
    }

    val (x, y) = a.pos
    val points = for {
      p <- polyPoints
    } yield (p._1 + a.pos._1.toInt, p._2 + a.pos._2.toInt)

    // Once we've worked out what to put into it, the asteroid is just a polygon node
    <.polygon.attrs(^.attr("points") := polyString(points), ^.cls := "asteroid")

  }

  /** Creates an SVG for a gravity well */
  def svgWell(w:Well):VNode = {
    val (x, y) = w.pos

    // This one's just a circle node
    <.circle(
      ^.cls := "well",
      ^.attr("cx") := x, ^.attr("cy") := y, ^.attr("r") := w.radius
    )
  }

  /**
    * The is is the view code that the router directs to (puts into the page) when
    * you click on this page of the docs.
    */
  def page:VNode = Common.layout(
    <.div(
      <.h1("Example -- asteroids rendering into an SVG"),
      <.p(
        <.a(^.href := "https://github.com/wbillingsley/veautiful/blob/master/docs/src/main/scala/example/ReactLike.scala",
          ^.attr("target") := "_blank", "Source code"
        )
      ),
      <.p(
        """
          | In this version, the simulation is rendered with SVG elements. The UI is mostly
          | functional and declarative -- functions returning VNodes, and being diffed as in
          | React. For example, this is the code for rendering the gravity wells:
        """.stripMargin
      ),
      <("pre")(
        """
          |  /** Creates an SVG for a gravity well */
          |  def svgWell(w:Well):VNode = {
          |    val (x, y) = w.pos
          |
          |    <.circle(
          |      ^.cls := "well",
          |      ^.attr("cx") := x, ^.attr("cy") := y, ^.attr("r") := w.radius
          |    )
          |  }
          |
        """.stripMargin
      ),
      <.p(
        """
          | There is a single stateful component, SimulationView, that controls
          | starting, stopping, and editing the simulation. Unlike React, that component can
          | have its own `rerender()` method, so it is not necessary to regenerate the whole
          | UI on every tick. Though that could be done by calling `rerender()` on the router.
        """.stripMargin
      ),
      <.p(
        """
          | On Chrome, it seems to cope with around 250 asteroids before the framerate slows
          | below 60fps. Above 1,000 asteroids it judders a bit.
        """.stripMargin
      ),

      // SimulationView is the stateful component below
      SimulationView,

      <.p(
        "etc"
      )
    )
  )

  /**
    * This is the stateful view component. It registers itself as a listener on the model,
    * so that each tick just the simulation is updated (we don't do a full page re-render
    * for this example).
    */
  case object SimulationView extends ElementComponent(<.div()) {

    /**
      * afterAttach is called when we're being asked to attach ourselves to a DOM node.
      * It's a good time to register the listener. And to call rerender once to set this
      * component's initial contents.
      */
    override def afterAttach() = {
      super.afterAttach()
      Model.addListener(rerender)
      rerender()
    }

    /** beforeDetach is called just before we're removed from the DOM. Let's clear the listener */
    override def beforeDetach() = Model.removeListener(rerender)

    // When we click reset, these parameters will be set on the model
    // We've made them mutable state in the view component, so we only set them on the
    // model when you click the reset button, not just whenever you type
    var asteroidCount = Model.count
    def reset(): Unit = {
      Model.count = asteroidCount
      Model.reset()
    }

    // And these variables are used to keep track of how long it took us to render ourselves
    var last:Long = System.currentTimeMillis()
    var dt:Long = 0

    /** The function we're calling on every tick to re-render this bit of UI */
    def rerender():Unit = {
      val now = System.currentTimeMillis()
      dt = now - last
      last = now

      // We do our rendering just by telling our component's local root node
      // (the <.div() up in the constructor) to update itself so it has the children that
      // are returned by card(asteroids). ie, we're updating a local virtual DOM.
      renderElements(card(Model.asteroids))
    }

    /** A function to work out what the local VDOM should look like for the current asteroids */
    def card(asteroids:Seq[Asteroid]) = {
      <.div(^.cls := "card",
        svg(
          Model.wells.map(svgWell) ++ Model.asteroids.map(svgAsteroid)
        ),
        <.div(^.cls := "card-footer",
          <.p(s"${asteroids.length} asteroids rendering in ${dt}ms"),
          <.div(^.cls := "btn-group",
            <("button")(
              ^.cls := "btn btn-sm btn-secondary", ^.onClick --> Model.stopTicking(),
              <("i")(^.cls := "fa fa-pause")
            ),
            <("button")(
              ^.cls := "btn btn-sm btn-secondary", ^.onClick --> Model.startTicking(),
              <("i")(^.cls := "fa fa-play")
            )
          ),
          <.div(^.cls := "input-group",
            <.span(^.cls := "input-group-addon", "Asteroids"),
            <("input")(^.attr("type") := "number", ^.cls := "form-control",
              ^.attr("value") := asteroidCount,
              ^.on("change") ==> { event => event.target match {
                case i:HTMLInputElement => asteroidCount = i.valueAsNumber
                case _ => // do nothing
              }}
            ),
            <.span(^.cls := "input-group-btn",
              <("button")(
                ^.cls := "btn btn-sm btn-secondary", ^.onClick --> reset, "Reset"
              )
            )
          )

        )
      )
    }
  }

}
