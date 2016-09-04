package application

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import logger.ActorsLoggerHelper

object ClusterAddresses {
  final case class WhatMyAddress()
}

class ClusterAddresses extends Actor with ActorsLoggerHelper with ActorLogging {
  import ClusterAddresses._

  def findMyAddress: String = Cluster(context.system).system.provider.getDefaultAddress.toString

  override def receive: Receive = {
    case _: WhatMyAddress => sender ! findMyAddress
    case _                => log error "Unhandled message"
  }
}
