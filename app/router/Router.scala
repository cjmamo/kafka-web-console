package router

import akka.actor.{ActorRef, Actor}

class Router(val routees: List[ActorRef]) extends Actor {

  override def receive: Actor.Receive = {
    case message => routees.foreach(routee => {
      routee.forward(message)
    })
  }

}